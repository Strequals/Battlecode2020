package rw5;

import battlecode.common.*;

public strictfp class LandscaperRobot extends Robot {

	private enum LandscaperState {
		TURTLING,
		RUSHING,
		TERRAFORMING
	}

	private boolean isEnemyRushing;
	// TODO: This probably shouldn't matter. Eliminate it.
	private MapLocation homeDsLocation;
	private MapLocation targetCorner;
	private LandscaperState state;
	private boolean navigatingCorner;
	private boolean reachedCorner;
	private boolean preferNorth;
	private boolean preferEast;
	private boolean isExpectedLandscaperCount;
	private boolean navigatingHq;
	private int robotElevation;
	private MapLocation pitLocation;
	private MapLocation enemyHqLocation;
	private boolean isDroneThreat;

	LandscaperRobot(RobotController rc) throws GameActionException {
		super(rc);

		// HQ location comes from processing first message

		// Process nearby robots to get probable home design school
		RobotInfo[] ri = rc.senseNearbyRobots(2, rc.getTeam());
		RobotInfo r;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			// Friendly Units
			if (r.getType() == RobotType.DESIGN_SCHOOL && rc.getLocation().isAdjacentTo(r.getLocation())) {
				homeDsLocation = r.getLocation();
				break;
			}
		}

		state = LandscaperState.TERRAFORMING;
		navigatingCorner = false;
		isExpectedLandscaperCount = false;
		reachedCorner = false;
		navigatingHq = false;
	}

	@Override
	public void run() throws GameActionException {
		isEnemyRushing = false;
		robotElevation = rc.senseElevation(location);
		isDroneThreat = false;
		// Process nearby robots
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		int nearbyLandscapers = 0;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				// Friendly Units
				switch (r.getType()) {
				case HQ:
					hqLocation = r.location;
					break;
				case LANDSCAPER:
					if (r.location.isWithinDistanceSquared(location, 2)) {
						nearbyLandscapers++;
					}
					break;
				case DESIGN_SCHOOL:
					if (homeDsLocation == null) homeDsLocation = r.location;
					break;
				}
			} else if (r.getTeam() != Team.NEUTRAL) {
				// Enemy Units
				switch (r.getType()) {
				case DELIVERY_DRONE:
					isDroneThreat = true;
					break;
				case MINER:
					// TODO: Block or bury
					break;
				case LANDSCAPER:
					isEnemyRushing = true;
					break;
				case NET_GUN:
					// TODO: Bury
					break;
				case REFINERY:
					// TODO: Bury
					break;
				case DESIGN_SCHOOL:
					// TODO: Bury
					break;
				case FULFILLMENT_CENTER:
					// TODO: Bury
					break;
				case VAPORATOR:
					// TODO: Bury
				case HQ:
					// We found it!
					enemyHqLocation = r.location;
				default:
					//Probably some structure, bury it if possible but low priority
					//Communications.sendMessage(rc);
					break;
				}
			}
		}
		
		Direction[] dirs = Utility.directions;
		MapLocation ml;
		Direction d;
		
		pitLocation = null;
		for (int i = 8; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			if (pitTile(ml)) {
				if (rc.canDigDirt(d)) {
					pitLocation = ml;
					break;
				}
			}
		}

		if (!rc.isReady()) return;

		System.out.println(preferNorth);
		System.out.println(preferEast);

		if (location.isAdjacentTo(hqLocation)) {
			RobotInfo hqInfo = rc.senseRobotAtLocation(hqLocation);
			if (hqInfo.dirtCarrying > 0) {
				rc.digDirt(location.directionTo(hqLocation));
				return;
			}
		}
		
		if (enemyHqLocation != null && round < TURTLE_END) {
			state = LandscaperState.RUSHING;
		} else if (state != LandscaperState.TURTLING) {
			state = LandscaperState.TERRAFORMING;
		}

		switch (state) {
		
		case TURTLING:
			doTurtling(nearbyLandscapers, false);
			break;
		case RUSHING:
			doRushing();
		case TERRAFORMING:
			doTerraforming();
		}
		
	}

	/**
	 * Assign the Landscaper a role.
	 */
	private void assignRole() {
		if (hqLocation.isWithinDistanceSquared(location, 25)) {
			state = LandscaperState.TURTLING;
		}
	}

	private void doTurtling(int nearbyLandscapers, boolean movedThisTurn) throws GameActionException {
		if (location.distanceSquaredTo(hqLocation)>2) {
			//TODO: check if can tunnel/bridge
			
			if (!navigatingHq) {
				Nav.beginNav(rc, this, hqLocation);
				navigatingHq = true;
			}
			Nav.nav(rc, this);
			return;
		}
		
		
		Direction[] dirs = Utility.directions;

		if (dirtCarrying < RobotType.LANDSCAPER.dirtLimit) {
		MapLocation ml;
		Direction d;
		for (int i = 8; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			if (pitTile(ml)) {
				if (rc.canDigDirt(d)) {
					rc.digDirt(d);
				}
			}
		}
		} else {
			if (round < TURTLE_END) {
				if (rc.canDepositDirt(Direction.CENTER)) {
					rc.depositDirt(Direction.CENTER);
				}
			} else {
				MapLocation ml;
				Direction d = null;
				Direction lowDir = null;
				int lowElev = 1000000;
				int elev;
				for (int i = 8; i-->0;) {
					d = dirs[i];
					ml = location.add(d);
					if (!(Utility.chebyshev(ml, hqLocation) == 1)) {
						continue;
					}
					if (pitTile(ml)) {
						if (rc.canDigDirt(d)) {
							if (rc.canSenseLocation(ml)) {
								elev = rc.senseElevation(ml);
								if (elev < lowElev) {
									lowDir = d;
									lowElev = elev;
								}
							}
						}
					}
				}
				
				if (lowDir != null) {
					rc.depositDirt(lowDir);
				}
			}
			
		}
		
		
	}
	
	private void moveLandscaper(MapLocation ml) throws GameActionException {
		//If there is a nearby path location that is not traversable, make it traversable
		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation m;
		int elev;
		int elevDiff;
		for (int i = 8; i-->0; ) {
			d = dirs[i];
			m = location.add(d);
			if (rc.canSenseLocation(m)) {
				elev = rc.senseElevation(m);
				elevDiff = robotElevation - elev;
				if (elevDiff < 0) elevDiff = -elevDiff;
				if (elevDiff > GameConstants.MAX_DIRT_DIFFERENCE) {
					tunnelOrBridge(m, d);
					return;
				}
			}
		}
		
		//Move towards location
		if (!ml.equals(Nav.target)) {
			Nav.beginNav(rc, this, ml);
		}
		Nav.nav(rc, this);
	}
	
	private void tunnelOrBridge(MapLocation ml, Direction d) throws GameActionException {
		int elTarget = rc.senseElevation(ml);
		int elDistance = elTarget - robotElevation;
		if (rc.senseFlooding(ml)) {
			if (-dirtCarrying <= elDistance+GameConstants.MAX_DIRT_DIFFERENCE || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(d);
			} else {
				if (pitLocation != null) rc.digDirt(location.directionTo(pitLocation));
			}
		} else if (elDistance > GameConstants.MAX_DIRT_DIFFERENCE) {
			if (dirtCarrying >= elDistance - GameConstants.MAX_DIRT_DIFFERENCE || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(Direction.CENTER);
			} else {
				if (pitLocation != null) rc.digDirt(location.directionTo(pitLocation));
			}
		} else if (elDistance < -GameConstants.MAX_DIRT_DIFFERENCE) {
			if (-dirtCarrying <= elDistance+GameConstants.MAX_DIRT_DIFFERENCE || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(d);
			} else {
				if (pitLocation != null) rc.digDirt(location.directionTo(pitLocation));
			}
		}
	}

	private void moveOrTunnelOrBridge(MapLocation ml, Direction d) throws GameActionException {
		int elTarget = rc.senseElevation(ml);
		int elDistance = elTarget - robotElevation;
		if (rc.senseFlooding(ml)) {
			if (-dirtCarrying <= elDistance+GameConstants.MAX_DIRT_DIFFERENCE || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(d);
			} else {
				if (pitLocation != null) rc.digDirt(location.directionTo(pitLocation));
			}
		} else if (elDistance > GameConstants.MAX_DIRT_DIFFERENCE) {
			if (dirtCarrying >= elDistance - GameConstants.MAX_DIRT_DIFFERENCE || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(Direction.CENTER);
			} else {
				if (pitLocation != null) rc.digDirt(location.directionTo(pitLocation));
			}
		} else if (elDistance < -GameConstants.MAX_DIRT_DIFFERENCE) {
			if (-dirtCarrying <= elDistance+GameConstants.MAX_DIRT_DIFFERENCE || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(d);
			} else {
				if (pitLocation != null) rc.digDirt(location.directionTo(pitLocation));
			}
		} else {
			rc.move(d);
		}
	}
	
	public void doTerraforming() {
		
	}
	
	public void doRushing() throws GameActionException {
		if (location.distanceSquaredTo(enemyHqLocation) > 2) {
			if (!Nav.target.equals(enemyHqLocation)) {
				Nav.beginNav(rc, this, enemyHqLocation);
			}
			Nav.nav(rc, this);
			return;
		}
		
		if (dirtCarrying < RobotType.LANDSCAPER.dirtLimit && (!isDroneThreat || dirtCarrying == 0)) {
			rc.digDirt(Direction.CENTER);
		} else {
			rc.depositDirt(location.directionTo(enemyHqLocation));
		}
		
	}

	@Override
	public void processMessage(int m, int x, int y) {
		switch (m) {
		case 1:
			hqLocation = new MapLocation(x,y);
			System.out.println("Received HQ location: " + x + ", " + y);
			break;
		case 19:
			if (x == homeDsLocation.x && y == homeDsLocation.y) {
				state = LandscaperState.TURTLING;
			}
			break;
		}

	}

}

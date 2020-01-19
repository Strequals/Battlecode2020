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
	private MapLocation pitLocation;
	private Direction pitDirection;
 	private MapLocation enemyHqLocation;
	private boolean isDroneThreat;
	private MapLocation targetBuildingLocation;
	
	private static final int TERRAFORM_THRESHOLD = 100; //If change in elevation is greater, do not terraform this tile

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
		
		isDroneThreat = false;
		// Process nearby robots
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		int nearbyLandscapers = 0;
		int targetBuildingDistance = 1000000;
		targetBuildingLocation = null;
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
				int distance;
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
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
					}
					break;
				case REFINERY:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
					}
					break;
				case DESIGN_SCHOOL:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
					}
					break;
				case FULFILLMENT_CENTER:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
					}
					break;
				case VAPORATOR:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
					}
					break;
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
		
		Direction[] dirs = Utility.directionsC;
		MapLocation ml;
		Direction d;
		
		pitLocation = null;
		for (int i = 9; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			if (pitTile(ml)) {
				if (rc.canDigDirt(d)) {
					pitLocation = ml;
					pitDirection = d;
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
		
		if (state != LandscaperState.TURTLING) {
			if (enemyHqLocation != null && round < TURTLE_END) {
				state = LandscaperState.RUSHING;
			} else {
				state = LandscaperState.TERRAFORMING;
			}
		}
		

		switch (state) {
		
		case TURTLING:
			doTurtling(nearbyLandscapers, false);
			break;
		case RUSHING:
			doRushing();
			break;
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
		
		//TODO: check if can move or bridge to lower wall location

		Direction[] dirs = Utility.directions;

		if (dirtCarrying == 0) {
			if (pitDirection != null) rc.digDirt(pitDirection);
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
							if (rc.canSenseLocation(ml)) {
								elev = rc.senseElevation(ml);
								if (elev < lowElev) {
									lowDir = d;
									lowElev = elev;
								}
							}
				}
				
				if (lowDir != null) {
					rc.depositDirt(lowDir);
				}
			}
			
		}
		
		
	}
	
	private void moveTurtle(MapLocation ml) throws GameActionException {
		//If there is a nearby path location that is not traversable, make it traversable
				Direction[] dirs = Utility.directions;
				Direction d;
				MapLocation m;
				int elev;
				int elevDiff;
				for (int i = 8; i-->0; ) {
					d = dirs[i];
					m = location.add(d);
					if (pitTile(m) || Utility.chebyshev(m, hqLocation) != 1) continue;
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
				if (Nav.target == null || !ml.equals(Nav.target)) {
					Nav.beginNav(rc, this, ml);
				}
				Nav.nav(rc, this);
	}

	private void moveTerraform(MapLocation ml) throws GameActionException {
		//If there is a nearby path location that is not traversable, make it traversable
		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation m;
		int elev;
		int elevDiff;
		RobotInfo botInfo;
		for (int i = 8; i-->0; ) {
			d = dirs[i];
			m = location.add(d);
			if (pitTile(m) || Utility.chebyshev(m, hqLocation) == 1) continue;
			if (rc.canSenseLocation(m)) {
				botInfo = rc.senseRobotAtLocation(m);
				if (botInfo != null && botInfo.team == team && botInfo.type.isBuilding()) continue;
				elev = rc.senseElevation(m);
				elevDiff = robotElevation - elev;
				if (elevDiff < 0) elevDiff = -elevDiff;
				if (elevDiff > 0 && elevDiff <= TERRAFORM_THRESHOLD) {
					tunnelOrBridge(m, d);
					return;
				}
			}
		}
		
		//Move towards location
		if (Nav.target == null || !ml.equals(Nav.target)) {
			Nav.beginNav(rc, this, ml);
		}
		Nav.nav(rc, this);
	}
	
	private void tunnelOrBridge(MapLocation ml, Direction d) throws GameActionException {
		int elTarget = rc.senseElevation(ml);
		int elDistance = elTarget - robotElevation;
		if (rc.senseFlooding(ml)) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else if (elDistance > 0) {
			if (dirtCarrying > 0) {
				rc.depositDirt(Direction.CENTER);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else if (elDistance < 0) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		}
	}

	private void moveOrTunnelOrBridge(MapLocation ml, Direction d) throws GameActionException {
		int elTarget = rc.senseElevation(ml);
		int elDistance = elTarget - robotElevation;
		if (rc.senseFlooding(ml)) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else if (elDistance > GameConstants.MAX_DIRT_DIFFERENCE) {
			if (dirtCarrying > 0) {
				rc.depositDirt(Direction.CENTER);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else if (elDistance < -GameConstants.MAX_DIRT_DIFFERENCE) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else {
			rc.move(d);
		}
	}
	
	public void doTerraforming() throws GameActionException {
		
		//Destroy enemy building
		if (targetBuildingLocation != null) {
			System.out.println("targeting Building at " + targetBuildingLocation);
			rc.setIndicatorLine(location, targetBuildingLocation, 255, 50, 50);
			int csDist = Utility.chebyshev(location, targetBuildingLocation);
			if (csDist <= 1) {
				if (dirtCarrying == 0) {
					if (pitDirection != null) {
						rc.digDirt(pitDirection);
						return;
					}
				} else {
					rc.depositDirt(location.directionTo(targetBuildingLocation));
					return;
				}

			} else {
				moveTerraform(targetBuildingLocation);
				return;
			}
		}
		
		//Move off of pit
		if (pitTile(location)) {
			System.out.println("Moving off pit");
			Direction[] dirs = Utility.directions;
			Direction d;
			for (int i = 8; i-->0;) {
				d=dirs[i];
				if (rc.canMove(d)) {
					rc.move(d);
					return;
				}
			}
		}

		//Find tile to fill
		MapLocation ml;
		int rSq = senseRadiusSq;
		int radius = (int)(Math.sqrt(rSq));
		ml = null;
		int dx;
		int dy;
		int rad;
		int elev;
		int dElev;
		int bestPriority = 100000;
		int priority;
		int rank;
		RobotInfo ri;
		MapLocation nearestFillTile = null;
		for (int x = Math.max(0, location.x - radius); x <= Math.min(mapWidth - 1, location.x + radius); x++) {
			for (int y = Math.max(0, location.y - radius); y <= Math.min(mapHeight - 1, location.y + radius); y++) {
				dx = x - location.x;
				dy = y - location.y;
				rad = dx * dx + dy * dy;
				if (rad > rSq) continue;
				ml = new MapLocation(x, y);
				if (pitTile(ml)) continue;
				elev = rc.senseElevation(ml);
				ri = rc.senseRobotAtLocation(ml);
				if (ri != null && (ri.type.isBuilding() || (ri.team == team && ri.type == RobotType.LANDSCAPER))) continue;
				dElev = robotElevation - elev;
				rank = Utility.chebyshev(ml, hqLocation);
				if (rank == 1) continue;
				priority = Utility.chebyshev(location, ml) + rank;
				if (enemyHqLocation != null) {
					priority += Utility.chebyshev(ml, enemyHqLocation);
				}
				if (dElev < 0) {
					dElev = -dElev;
					priority -= 100; //Prioritize filling in lower tiles rather than digging higher ones
				}
				if (dElev > TERRAFORM_THRESHOLD) rad += 10000;
				if (dElev > GameConstants.MAX_DIRT_DIFFERENCE) {
					if (priority < bestPriority) {
						bestPriority = priority;
						nearestFillTile = ml;
					}

				}
			}
		}
		
		if (nearestFillTile != null) {
			System.out.println("Filling in...");
			rc.setIndicatorLine(location, nearestFillTile, 50, 255, 100);
			moveTerraform(nearestFillTile);
			return;
		}

		//Start increasing height of terraform
		System.out.println("Increase terraform height");
		if (dirtCarrying == 0) {
			rc.digDirt(pitDirection);
		} else {
			if (!pitTile(location)) {
				rc.depositDirt(Direction.CENTER);
			} else {
				Direction[] dirs = Utility.directions;
				Direction d;
				MapLocation m;
				RobotInfo botInfo;
				for (int i = 8; i-->0; ) {
					d = dirs[i];
					m = location.add(d);
					if (pitTile(m) || Utility.chebyshev(m, hqLocation) == 1) continue;
					if (rc.canSenseLocation(m)) {
						botInfo = rc.senseRobotAtLocation(m);
						if (botInfo != null && botInfo.team == team && botInfo.type.isBuilding()) continue;
						
						rc.depositDirt(d);
						return;
					}
				}
			}
		}
	}
	
	public void doRushing() throws GameActionException {
		if (location.distanceSquaredTo(enemyHqLocation) > 2) {
			if (Nav.target == null || !Nav.target.equals(enemyHqLocation)) {
				Nav.beginNav(rc, this, enemyHqLocation);
			}
			Nav.nav(rc, this);
			return;
		}
		
		if (dirtCarrying == 0) {
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
		case 3:
			enemyHqLocation = new MapLocation(x,y);
			break;
		case 19:
			if (x == homeDsLocation.x && y == homeDsLocation.y) {
				state = LandscaperState.TURTLING;
			}
			break;
		}

	}

}

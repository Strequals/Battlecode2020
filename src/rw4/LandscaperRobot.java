package rw4;

import battlecode.common.*;

public strictfp class LandscaperRobot extends Robot {

	private enum LandscaperState {
		MOVING_TO_TURTLE_POSITION,
		TURTLING,
		RUSHING
	}

	private boolean isEnemyRushing;
	private MapLocation hqLocation;
	// TODO: This probably shouldn't matter. Eliminate it.
	private MapLocation homeDsLocation;
	private MapLocation targetCorner;
	private LandscaperState state;
	private boolean navigatingCorner;
	private boolean reachedCorner;
	private boolean preferNorth;
	private boolean preferEast;
	private boolean isExpectedLandscaperCount;
	private int robotElevation;

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

		state = LandscaperState.MOVING_TO_TURTLE_POSITION;
		navigatingCorner = false;
		isExpectedLandscaperCount = false;
		reachedCorner = false;
	}

	@Override
	public void run() throws GameActionException {
		isEnemyRushing = false;
		robotElevation = rc.senseElevation(location);

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
					// We don't care
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
				default:
					//Probably some structure, bury it if possible but low priority
					//Communications.sendMessage(rc);
					break;
				}
			}
		}

		if (targetCorner == null && homeDsLocation != null && hqLocation != null) {
			if (hqLocation.x > homeDsLocation.x) {
				preferEast = true;
			} else {
				preferEast = false;
			}
			if (hqLocation.y > homeDsLocation.y) {
				preferNorth = true;
			} else {
				preferNorth = false;
			}

			if (preferEast) {
				if (preferNorth) {
					targetCorner = hqLocation.add(Direction.SOUTHWEST).add(Direction.SOUTHWEST);
				} else {
					targetCorner = hqLocation.add(Direction.NORTHWEST).add(Direction.NORTHWEST);
				}
			} else {
				if (preferNorth) {
					targetCorner = hqLocation.add(Direction.SOUTHEAST).add(Direction.SOUTHEAST);
				} else {
					targetCorner = hqLocation.add(Direction.NORTHEAST).add(Direction.NORTHEAST);
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
			}
		}

		switch (state) {
		case MOVING_TO_TURTLE_POSITION:
			doMoving(nearbyLandscapers, false);
			break;
		case TURTLING:
			doTurtling(nearbyLandscapers, false);
			break;
		}
	}

	/**
	 * Assign the Landscaper a role.
	 */
	private void assignRole() {
		if (hqLocation.isWithinDistanceSquared(location, 25)) {
			state = LandscaperState.MOVING_TO_TURTLE_POSITION;
		}
	}

	private void doMoving(int nearbyLandscapers, boolean turtledThisTurn) throws GameActionException {
		if (targetCorner == null) {
			System.out.println("NO TARGET CORNER");
			return;
		}
		System.out.println("doMoving");
		if (!reachedCorner) {
			int csdist = Utility.chebyshev(location, targetCorner);
			
			/*if (csdist == 2 && rc.canSenseLocation(targetCorner)) {
				//If there is a moat or obstacle, remove it
				Direction d = location.directionTo(targetCorner);
				MapLocation ml = location.add(d);
				int dElev = rc.senseElevation(ml);
				if (Math.abs(robotElevation - dElev) > 3 || rc.senseFlooding(ml)) {
					if (moveOrTunnelOrBridge(ml,d)) return;
				}
			}*/
			if (csdist > 1) {
				if (!navigatingCorner) {
					Nav.beginNav(rc, this, targetCorner);
					navigatingCorner = true;
				}
				System.out.println("NavHQ");
				Nav.nav(rc, this);
				return;
			} else {
				reachedCorner = true;
			}
		}

		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation ml;
		int locRank; // Each concentric square around HQ has a rank. HQ is 0, around that is 1, around that is 2 (wall)
		int robotRank = Utility.chebyshev(location, hqLocation);

		MapLocation moveLocation = null;
		Direction moveDirection = null;
		int moveRank = 100;

		for (int i = 8; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			locRank = Utility.chebyshev(ml, hqLocation);
			if (rc.canSenseLocation(ml) && rc.onTheMap(ml) && !rc.isLocationOccupied(ml) && !rc.senseFlooding(ml)) {
				// Move only if we can move to the tile, ignoring height issues

				if (locRank < robotRank) {
					// Move down in rank (toward HQ) whenever possible
					//TODO: prioritize moving lower in rank over moving at the same rank
					if (moveRank > locRank) {
						moveLocation = ml;
						moveDirection = d;
						moveRank = locRank;
					}
				} else if (locRank == robotRank) {
					// If we can't move down in rank, stay at rank
					if (!isEnemyRushing) {
						if (ml.x == location.x) {
							if (preferNorth == ml.y > location.y) {
								if (moveRank > locRank) {
									moveLocation = ml;
									moveDirection = d;
									moveRank = locRank;
								}
							}
						} else {
							if (ml.y == location.y) {
								if (preferEast == ml.x > location.x) {
									if (moveRank > locRank) {
										moveLocation = ml;
										moveDirection = d;
										moveRank = locRank;
									}
								}
							} else {
								if ((preferNorth == ml.y > location.y) && (preferEast == ml.x > location.x)) {
									if (moveRank > locRank) {
										moveLocation = ml;
										moveDirection = d;
										moveRank = locRank;
									}
								}
							}
						}
					} else {
						// TODO: Handle enemy rushing
					}
				}
			}
		}


		if (moveLocation != null) {
			moveOrTunnelOrBridge(moveLocation, moveDirection);
			return;
		}

		// Switch to turtling or reset to turtling, either way set new state
		state = LandscaperState.TURTLING;

		// Don't actually turtle again if we already tried
		if (!turtledThisTurn) {
			this.doTurtling(nearbyLandscapers, true);
		}
	}

	private void doTurtling(int nearbyLandscapers, boolean movedThisTurn) throws GameActionException {
		Direction[] dirs = Utility.directionsC;
		Direction d;
		MapLocation ml;
		int locRank; // Each concentric square around HQ has a rank. HQ is 0, around that is 1, around that is 2 (wall)
		// Will either dig from highest place it should dig from, or fill the lowest place it fills to
		int expectedLandscapers = 0; // How many landscapers should be nearby. Shouldn't mine if this doesn't match reality
		int elev;
		int rank = Utility.chebyshev(location, hqLocation);
		MapLocation high = null; // Highest location to mine from
		Direction dHigh = null;
		int hd = Integer.MIN_VALUE;
		MapLocation low = null; // Lowest location to place at
		Direction dLow = null;
		int ld = Integer.MAX_VALUE;
		//RobotInfo ri;
		for (int i = 9; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			locRank = Utility.chebyshev(ml, hqLocation);
			if (!rc.canSenseLocation(ml)) continue;
			elev = rc.senseElevation(ml);

			if (locRank == 2 && (isExpectedLandscaperCount || d == Direction.CENTER || round >= TURTLE_END)) {
				// Location is on wall

				if (elev < ld && rc.canDepositDirt(d)) {
					low = ml;
					ld = elev;
					dLow = d;
				}
			} else if (locRank != 0 && (isExpectedLandscaperCount || locRank < rank || round >= TURTLE_END)) {
				// Location is not HQ or on wall

				// TODO: Do this better
				// Prioritize mining outside outer wall
				if (locRank > 2) elev += 10000;

				System.out.println("Location: " + ml + ", elev:" + elev + ", hd:"+hd);

				// Dig from here if: we can, and it's the highest spot we've seen
				if (elev > hd && rc.canDigDirt(d)) {
					high = ml;
					hd = elev;
					dHigh = d;
					System.out.println("HIGH:"+high);
				}
			} else {
				//check if robot can dig at HQ location
			}

			if (d == Direction.CENTER) continue;

			if (locRank <= 2 && locRank != 0 && !ml.equals(homeDsLocation)) {
				// If the location is within or on top of the walls (but not HQ or the design school), expect to see a
				// landscaper there.
				expectedLandscapers++;
			}

		}

		isExpectedLandscaperCount = nearbyLandscapers >= expectedLandscapers;

		//        if (nearbyLandscapers < expectedLandscapers && round < 2 * TURTLE_ROUND + 50) return;
		if (!isExpectedLandscaperCount && round < TURTLE_END) {
			// Set or reset state, either way, set variable
			System.out.println("nearbyLS:"+nearbyLandscapers);
			state = LandscaperState.MOVING_TO_TURTLE_POSITION;

			// Don't move if we already tried
			if (!movedThisTurn) doMoving(nearbyLandscapers, true);
			return;
		}

		if (high != null) System.out.println("HIGH: " + high.x + ", " + high.y);
		else System.out.println("ERR CANNOT FIND HIGH");
		if (low != null) System.out.println("LOW: " + low.x + ", " + low.y);
		else System.out.println("ERR CANNOT FIND LOW");
		if (high != null && dirtCarrying <= RobotType.LANDSCAPER.dirtLimit) {
			if (rc.canDigDirt(dHigh)) {
				rc.digDirt(dHigh);
				System.out.println("DIGGING: " + high.x + ", " + high.y);
				return;
			}
		}
		if (low != null && rc.canDepositDirt(dLow)) {
			rc.depositDirt(dLow);
			System.out.println("DEPOSITING: " + low.x + ", " + low.y);
		}
	}

	private boolean moveOrTunnelOrBridge(MapLocation ml, Direction d) throws GameActionException {
		int elTarget = rc.senseElevation(ml);
		int elDistance = elTarget - robotElevation;
		if (rc.senseFlooding(ml)) {
			if (-dirtCarrying <= elDistance+3 || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(d);
			} else {
				Direction[] dirs = Utility.directionsC;
				Direction dir;
				int high = elTarget;
				Direction highDir = null;
				int elev;
				MapLocation loc;
				for (int i = 8; i-->0;) {
					dir = dirs[i];
					loc = location.add(dir);
					if (rc.canSenseLocation(loc)) {
						elev = rc.senseElevation(loc);
						if (elev > high) {
							high = elev;
							highDir = dir;
						}
					}
				}
				if (highDir != null) rc.digDirt(highDir);
				else return false;
			}
		} else if (elDistance > 3) {
			if (dirtCarrying >= elDistance - 3 || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(Direction.CENTER);
			} else {
				rc.digDirt(d);
			}
		} else if (elDistance < -3) {
			if (-dirtCarrying <= elDistance+3 || dirtCarrying == RobotType.LANDSCAPER.dirtLimit) {
				rc.depositDirt(d);
			} else {
				rc.digDirt(Direction.CENTER);
			}
		} else {
			rc.move(d);
		}
		return true;
	}

	@Override
	public void processMessage(int m, int x, int y) {
		switch (m) {
		case 1:
			hqLocation = new MapLocation(x,y);
			System.out.println("Received HQ location: " + x + ", " + y);
			break;
		}

	}

}

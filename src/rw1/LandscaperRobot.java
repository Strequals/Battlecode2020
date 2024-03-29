package rw1;

import battlecode.common.*;

public strictfp class LandscaperRobot extends Robot {

	private enum LandscaperState {
		MOVING_TO_TURTLE_POSITION,
		TURTLING
	}

	private boolean isEnemyRushing;
	private MapLocation hqLocation;
	// TODO: This probably shouldn't matter. Eliminate it.
	private MapLocation homeDsLocation;
	private MapLocation scaledDsLocation;
	private MapLocation inverseDsLocation;
	private LandscaperState state;

	LandscaperRobot(RobotController rc) throws GameActionException {
		super(rc);

		// HQ location comes from processing first message

		// Process nearby robots to get probable home design school
		RobotInfo[] ri = rc.senseNearbyRobots(-1, rc.getTeam());
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
	}

	@Override
	public void run() throws GameActionException {
		isEnemyRushing = false;

		// Process nearby robots
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		int nearbyLandscapers = 0;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				// Friendly Units
				switch (r.getType()) {
				case LANDSCAPER:
					if (r.location.isWithinDistanceSquared(location, 2)) {
						nearbyLandscapers++;
					}
					break;
				case HQ:
					hqLocation = r.location;
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

		if (inverseDsLocation == null && hqLocation != null && homeDsLocation != null) {
			Direction dir = homeDsLocation.directionTo(hqLocation);
			inverseDsLocation = hqLocation.add(dir).add(dir);
			dir = hqLocation.directionTo(homeDsLocation);
			scaledDsLocation = homeDsLocation.add(dir);
		}

		if (!rc.isReady()) return;

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
		Direction[] dirs = Utility.directionsC;
		Direction d;
		MapLocation ml;
		int locRank; // Each concentric square around HQ has a rank. HQ is 0, around that is 1, around that is 2 (wall)
		int robotRank = Utility.chebyshev(location, hqLocation);
		int robotElevation = rc.senseElevation(location);

		for (int i = 9; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			locRank = Utility.chebyshev(ml, hqLocation);
			if (rc.onTheMap(ml) && !rc.isLocationOccupied(ml) && !rc.senseFlooding(ml)) {
				// Move only if we can move to the tile, ignoring height issues

				if (locRank < robotRank) {
					// Move down in rank (toward HQ) whenever possible
					moveOrTunnelOrBridge(ml, d, robotElevation);
					return;
				} else if (locRank == robotRank) {
					// If we can't move down in rank, stay at rank
					if (!isEnemyRushing) {
						if (inverseDsLocation != null) {
							// Move away from the design school and toward the opposite side of HQ from the school
							int dsDist = location.distanceSquaredTo(scaledDsLocation);
							int iDsDist = location.distanceSquaredTo(inverseDsLocation);

							if (dsDist <= iDsDist) {
								if (dsDist < ml.distanceSquaredTo(scaledDsLocation)) {
									// Move there if it's farther from the school than we are now
									moveOrTunnelOrBridge(ml, d, robotElevation);
									return;
								}
							} else {
								if (iDsDist > ml.distanceSquaredTo(inverseDsLocation)) {
									// Move there if it's closer to opposite the school than we are now
									moveOrTunnelOrBridge(ml, d, robotElevation);
									return;
								}
							}
						}
					} else {
						// TODO: Handle enemy rushing
					}
				}
			}
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
		MapLocation high = null; // Highest location to mine from
		Direction dHigh = null;
		int hd = Integer.MIN_VALUE;
		MapLocation low = null; // Lowest location to place at
		Direction dLow = null;
		int ld = Integer.MAX_VALUE;
		for (int i = 9; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			locRank = Utility.chebyshev(ml, hqLocation);
			if (!rc.canSenseLocation(ml)) continue;
			elev = rc.senseElevation(ml);

			if (locRank == 2) {
				// Location is on wall

				if (elev < ld && rc.canDepositDirt(d)) {
					low = ml;
					ld = elev;
					dLow = d;
				}
			} else if (locRank != 0) {
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
			}

			if (d == Direction.CENTER) continue;

			if (locRank <= 2 && locRank != 0 && !ml.equals(homeDsLocation)) {
				// If the location is within or on top of the walls (but not HQ or the design school), expect to see a
				// landscaper there.
				expectedLandscapers++;
			}

		}

		//        if (nearbyLandscapers < expectedLandscapers && round < 2 * TURTLE_ROUND + 50) return;
		if (nearbyLandscapers < expectedLandscapers && round < 10*TURTLE_ROUND) {
			// Set or reset state, either way, set variable
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

	private void moveOrTunnelOrBridge(MapLocation ml, Direction d, int robotElevation) throws GameActionException {
		int elDistance = rc.senseElevation(ml) - robotElevation;
		if (elDistance > 3) {
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

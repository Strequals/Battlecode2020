package rw5;

import battlecode.common.*;

public strictfp class DesignSchoolRobot extends Robot {
	private MapLocation hqLocation;
	private MapLocation enemyHqLocation;

	private int numHQRequested = 0;

	private int nearbyAlliedLandscapers = 0;

	private DesignSchoolState designSchoolState = DesignSchoolState.TERRAFORMING;

	enum DesignSchoolState {
		BUILDING_TURTLES, RUSHING, TERRAFORMING
	}

	DesignSchoolRobot(RobotController rc) throws GameActionException {
		super(rc);

		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2, rc.getTeam().opponent());
		RobotInfo robot;
		for (int i = nearbyRobots.length; i-->0;) {
			robot = nearbyRobots[i];
			if (robot.type == RobotType.HQ) {
				enemyHqLocation = robot.location;
				designSchoolState = DesignSchoolState.RUSHING;
			}
		}
	}

	@Override
	public void run() throws GameActionException {
		nearbyAlliedLandscapers = 0;

		if (round == roundCreated) {
			Communications.queueMessage(rc, 2, 6, location.x, location.y);
		}

		// Process nearby robots
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				//Friendly Units
				switch (r.getType()) {
				case HQ:
					hqLocation = r.getLocation();
					break;
				case LANDSCAPER:
					nearbyAlliedLandscapers++;
					break;

				}
			} else if (r.getTeam() == Team.NEUTRAL) {
				//It's a cow, yeet it from our base
				if (round > 100) {
					//Call the drones
					//Communications.sendMessage(rc);
				}
			} else {
				//Enemy Units
				switch (r.getType()) {
				case MINER:
					//Call the drones
					//Communications.sendMessage(rc);
					break;
				case LANDSCAPER:
					//Call the drones
					//Communications.sendMessage(rc);

					break;
				case DELIVERY_DRONE:
					//pew pew pew
					return;
				case NET_GUN:
					//Direct units to bury the net gun
					//Communications.sendMessage(rc);
					break;
				case REFINERY:
					//Direct units to bury the refinery
					//Communications.sendMessage(rc);
					break;
				default:
					//Probably some structure, bury it if possible but low priority
					//Communications.sendMessage(rc);
					break;
				}
			}
		}

		// Switch between terraforming and building turtles
		if (numHQRequested > 0 && designSchoolState == DesignSchoolState.TERRAFORMING) {
			designSchoolState = DesignSchoolState.BUILDING_TURTLES;
		} else if (numHQRequested == 0 && designSchoolState == DesignSchoolState.BUILDING_TURTLES) {
			designSchoolState = DesignSchoolState.TERRAFORMING;
		}

		switch (designSchoolState) {
			case BUILDING_TURTLES:
				buildTurtles();
				break;
			case RUSHING:
				rush();
				break;
			case TERRAFORMING:
				terraform();
				break;
		}
	}

	private void buildTurtles() throws GameActionException {
		if (numHQRequested > 0) {
			Direction hqDirection = location.directionTo(hqLocation);
			if (rc.canBuildRobot(RobotType.LANDSCAPER, hqDirection)) {
				rc.buildRobot(RobotType.LANDSCAPER, hqDirection);
				return;
			}
			Direction left = hqDirection;
			Direction right = hqDirection;
			for (int i = 4; i-->0;) {
				left = left.rotateLeft();
				right = right.rotateRight();
				if (rc.canBuildRobot(RobotType.LANDSCAPER, left)) {
					rc.buildRobot(RobotType.LANDSCAPER, left);
					return;
				}
				if (rc.canBuildRobot(RobotType.LANDSCAPER, right)) {
					rc.buildRobot(RobotType.LANDSCAPER, right);
					return;
				}

			}
			numHQRequested--;
			if (numHQRequested == 0) {
				Communications.queueMessage(rc, 1, 19, location.x, location.y);
			}

		}
	}

	private void rush() throws GameActionException {
	    if (nearbyAlliedLandscapers < Utility.MAX_NEARBY_LANDSCAPERS_RUSH_DESIGN_SCHOOL) {
            Direction[] dirs = Utility.directions;

            Direction d;
            MapLocation ml;

            Direction potentialBuildDirection = null;

            for (int i = dirs.length; --i >= 0; ) {
                d = dirs[i];
                if (rc.canBuildRobot(RobotType.LANDSCAPER, d) && !rc.senseFlooding(location.add(d))) {
                    ml = location.add(d);
                    if (ml.isAdjacentTo(enemyHqLocation)) {
                        rc.buildRobot(RobotType.LANDSCAPER, d);
                        return;
                    } else {
                        potentialBuildDirection = d;
                    }
                }
            }

            if (potentialBuildDirection != null) {
                rc.buildRobot(RobotType.LANDSCAPER, potentialBuildDirection);
            }
        }
	}

	private void terraform() throws GameActionException {
		if (soup >= Utility.MIN_GLOBAL_SOUP_TO_BUILD_TERRAFORMER) {
			Direction[] dirs = Utility.directions;
			Direction d;

			for (int i = dirs.length; --i >= 0; ) {
				d = dirs[i];
				if (rc.canBuildRobot(RobotType.LANDSCAPER, d) && !rc.senseFlooding(location.add(d))) {
					rc.buildRobot(RobotType.LANDSCAPER, d);
					return;
				}
			}
		}
	}

	@Override
	public void processMessage(int m, int x, int y) {
		switch (m) {
		case 1:
			hqLocation = new MapLocation(x,y);
			System.out.println("Recieved HQ location: " + x + ", " + y);
			break;
		case 11:
			if (x == location.x && y == location.y) numHQRequested = 1;
			break;
		case 12:
			if (x == location.x && y == location.y) numHQRequested = 2;
			break;
		case 13:
			if (x == location.x && y == location.y) numHQRequested = 3;
			break;
		case 14:
			if (x == location.x && y == location.y) numHQRequested = 4;
			break;
		case 15:
			if (x == location.x && y == location.y) numHQRequested = 5;
			break;
		case 16:
			if (x == location.x && y == location.y) numHQRequested = 6;
			break;
		case 17:
			if (x == location.x && y == location.y) numHQRequested = 7;
			break;
		case 18:
			if (x == location.x && y == location.y) numHQRequested = 8;
			break;
		}

	}

}

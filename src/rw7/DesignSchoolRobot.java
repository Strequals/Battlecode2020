package rw7;

import battlecode.common.*;

public strictfp class DesignSchoolRobot extends Robot {
	private MapLocation enemyHqLocation;

	private int numTurtles = 0;

	private int nearbyAlliedLandscapers = 0;
	private int nearbyAlliedVaporators = 0;
	private boolean isRefinery;
	private boolean rushDetected;
	private boolean fcBuilt;
	private boolean isAlliedDrone;

	private DesignSchoolState designSchoolState = DesignSchoolState.TERRAFORMING;
	
	static final int MAX_NEARBY_LANDSCAPERS_RUSH_DESIGN_SCHOOL = 5;
	static final int MIN_GLOBAL_SOUP_TO_BUILD_TERRAFORMER = 300;
	
	static final int WEIGHT = 150; //need 150 extra soup per nearby landscaper to build
	static final int VAPORATOR_WEIGHT = 150; //need 150 less soup per nearby vaporator to build

	enum DesignSchoolState {
		BUILDING_TURTLES, RUSHING, TERRAFORMING
	}

	DesignSchoolRobot(RobotController rc) throws GameActionException {
		super(rc);

		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2, rc.getTeam().opponent());
		RobotInfo robot;
		/*for (int i = nearbyRobots.length; i-->0;) {
			robot = nearbyRobots[i];
			if (robot.type == RobotType.HQ) {
				enemyHqLocation = robot.location;
				designSchoolState = DesignSchoolState.RUSHING;
			}
		}*/
	}

	@Override
	public void run() throws GameActionException {
		nearbyAlliedLandscapers = 0;
		nearbyAlliedVaporators = 0;
		rushDetected = false;
		fcBuilt = false;
		isAlliedDrone = false;
		if (round == roundCreated) {
			if (initialBuildingTile(location)) {
				numTurtles = 8;
			}
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
					if (Utility.chebyshev(r.location, hqLocation) != 1) nearbyAlliedLandscapers++;
					break;
				case REFINERY:
					isRefinery = true;
					break;
				case FULFILLMENT_CENTER:
					fcBuilt = true;
					break;
				case DELIVERY_DRONE:
					isAlliedDrone = true;
					break;
				case VAPORATOR:
					nearbyAlliedVaporators++;
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
					rushDetected = true;
					break;
				case LANDSCAPER:
					//Call the drones
					//Communications.sendMessage(rc);
					rushDetected = true;
					break;
				case DELIVERY_DRONE:
					//pew pew pew
					
					return;
				case DESIGN_SCHOOL:
					rushDetected = true;
					break;
				case NET_GUN:
					//Direct units to bury the net gun
					//Communications.sendMessage(rc);
					rushDetected = true;
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
		if (numTurtles > 0 && designSchoolState == DesignSchoolState.TERRAFORMING) {
			designSchoolState = DesignSchoolState.BUILDING_TURTLES;
		} else if (numTurtles == 0 && designSchoolState == DesignSchoolState.BUILDING_TURTLES) {
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
		if (numTurtles > 0) {
			
			//if there is no refinery, do not finish the wall
			//if the fulfillment center has not been built and two or more landscapers exist, do not build more
			if ((numTurtles == 1 && (soup < RobotType.REFINERY.cost + RobotType.LANDSCAPER.cost || !isRefinery)) || (!fcBuilt && numTurtles <= 7 && soup < RobotType.LANDSCAPER.cost + RobotType.FULFILLMENT_CENTER.cost + RobotType.DELIVERY_DRONE.cost) || (!isAlliedDrone && numTurtles <= 6 && soup < RobotType.LANDSCAPER.cost + RobotType.DELIVERY_DRONE.cost)) {
				return;
			}
			Direction hqDirection = location.directionTo(hqLocation);
			if (rc.canBuildRobot(RobotType.LANDSCAPER, hqDirection)) {
				rc.buildRobot(RobotType.LANDSCAPER, hqDirection);
				numTurtles--;
				return;
			}
			Direction left = hqDirection;
			Direction right = hqDirection;
			for (int i = 4; i-->0;) {
				left = left.rotateLeft();
				right = right.rotateRight();
				if (rc.canBuildRobot(RobotType.LANDSCAPER, left)) {
					rc.buildRobot(RobotType.LANDSCAPER, left);
					numTurtles--;
					
					
					return;
				}
				if (rc.canBuildRobot(RobotType.LANDSCAPER, right)) {
					rc.buildRobot(RobotType.LANDSCAPER, right);
					numTurtles--;
					
					
					return;
				}

			}
			

		}
	}

	private void rush() throws GameActionException {
	    if (nearbyAlliedLandscapers < MAX_NEARBY_LANDSCAPERS_RUSH_DESIGN_SCHOOL) {
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
		if (soup >= MIN_GLOBAL_SOUP_TO_BUILD_TERRAFORMER + WEIGHT * nearbyAlliedLandscapers - VAPORATOR_WEIGHT * nearbyAlliedVaporators) {
			Direction[] dirs = Utility.directions;
			Direction d;

			for (int i = dirs.length; --i >= 0; ) {
				d = dirs[i];
				if (rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
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
			//System.out.println("Recieved HQ location: " + x + ", " + y);
			break;
		case 5:
			isRefinery = true;
			break;
		
		}

	}

}

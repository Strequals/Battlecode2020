package rw4;

import battlecode.common.*;

public strictfp class DesignSchoolRobot extends Robot {
	MapLocation hqLocation;

	public DesignSchoolRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws GameActionException {
		
		//Process nearby robots
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
						case DESIGN_SCHOOL:
							break;
						case LANDSCAPER:
							break;
						default:
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
		/*Direction[] dirs = Utility.directions;
		for (int i = dirs.length; --i >= 0;) {
			if (rc.canBuildRobot(RobotType.LANDSCAPER, dirs[i])) {
				rc.buildRobot(RobotType.LANDSCAPER, dirs[i]);
			}
		}*/
		
		
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
		
	}

	@Override
	public void processMessage(int m, int x, int y) {
		switch (m) {
		case 1:
			hqLocation = new MapLocation(x,y);
			System.out.println("Recieved HQ location: " + x + ", " + y);
			break;
		}
		
	}

}

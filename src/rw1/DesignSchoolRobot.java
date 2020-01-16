package rw1;

import battlecode.common.*;

public strictfp class DesignSchoolRobot extends Robot {
	MapLocation hqLocation;
	MapLocation sourceLocation;

	public DesignSchoolRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws GameActionException {
		System.out.println("HQCOOL:"+RobotType.HQ.actionCooldown);
		
		boolean sourceOccupied = false;
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
							sourceLocation = location.add(hqLocation.directionTo(location));
							break;
						case DESIGN_SCHOOL:
							break;
						case LANDSCAPER:
							if (r.location.equals(sourceLocation)) {
								sourceOccupied = true;
							}
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
		
		
		System.out.println(sourceOccupied);
		Direction hqDirection = location.directionTo(hqLocation);
		Direction left = hqDirection;
		Direction right = hqDirection;
		for (int i = 4; i-->0;) {
			if (sourceOccupied && i >= 3) break;
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
		// TODO Auto-generated method stub
		
	}

}

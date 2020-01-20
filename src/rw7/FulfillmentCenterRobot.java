package rw7;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public strictfp class FulfillmentCenterRobot extends Robot {

	public FulfillmentCenterRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws GameActionException {
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		int friendlyDrones = 0;
		int enemies = 0;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				//Friendly Units
				switch (r.getType()) {
				case DELIVERY_DRONE:
					friendlyDrones++;
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
					enemies++;
					break;
				case LANDSCAPER:
					//Call the drones
					//Communications.sendMessage(rc);
					enemies++;
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
		
		if (!rc.isReady()) return;
		
		if (soup > RobotType.DELIVERY_DRONE.cost && enemies>friendlyDrones) {
			Direction hqDirection = location.directionTo(hqLocation);
			if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, hqDirection)) {
				rc.buildRobot(RobotType.DELIVERY_DRONE, hqDirection);
				return;
			}
			Direction left = hqDirection;
			Direction right = hqDirection;
			for (int i = 4; i-->0;) {
				left = left.rotateLeft();
				right = right.rotateRight();
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, left)) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, left);
					return;
				}
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, right)) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, right);
					return;
				}

			}
		}

		if (soup > 700) {
			if (enemyHqLocation != null) {
				Direction enemyHqDirection = location.directionTo(enemyHqLocation);
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, enemyHqDirection)) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, enemyHqDirection);
					return;
				}
				Direction left = enemyHqDirection;
				Direction right = enemyHqDirection;
				for (int i = 4; i-->0;) {
					left = left.rotateLeft();
					right = right.rotateRight();
					if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, left)) {
						rc.buildRobot(RobotType.DELIVERY_DRONE, left);
						return;
					}
					if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, right)) {
						rc.buildRobot(RobotType.DELIVERY_DRONE, right);
						return;
					}

				}
			} else {
				Direction[] directions = Utility.directions;
				Direction d;
				for (int i = 8; i-->0;) {
					d = directions[i];
					if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, d)) {
						rc.buildRobot(RobotType.DELIVERY_DRONE, d);
					}
				}
			}
			


		}

	}

	@Override
	public void processMessage(int m, int x, int y) {
		// TODO Auto-generated method stub

	}

}

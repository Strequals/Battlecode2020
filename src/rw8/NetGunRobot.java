package rw8;

import battlecode.common.*;

public strictfp class NetGunRobot extends Robot {

	public NetGunRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws GameActionException {
		// TODO Auto-generated method stub
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;

		boolean isTarget = false;
		int bestTargetID = 0;
		int bestTargetPriority = 10000; // lower priority better
		int id;
		int dist;
		RobotInfo botCarrying;
		int priority;

		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				//Friendly Units
				switch (r.getType()) {
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
					if (rc.canShootUnit(r.ID)) {
						isTarget = true;
						dist = Utility.chebyshev(location, r.location);
						id = r.getHeldUnitID();
						botCarrying = null;
						if (rc.canSenseRobot(id)) {
							botCarrying = rc.senseRobot(id);
						}
						priority = shootPriority(dist, botCarrying);

						if (priority < bestTargetPriority) {
							bestTargetPriority = priority;
							bestTargetID = r.ID;
						}
					}
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
		
		if (isTarget) {
			rc.shootUnit(bestTargetID);
		}


	}

	@Override
	public void processMessage(int m, int x, int y) {
		// TODO Auto-generated method stub

	}

	public int shootPriority(int dist, RobotInfo botCarrying) throws GameActionException {
		int priority = dist;
		if (botCarrying != null) {
			if (botCarrying.team == team) {
				if (rc.senseFlooding(botCarrying.location)) {
					priority += 100;
				} else {
					priority -= 100;
				}
			} else {
				if (rc.senseFlooding(botCarrying.location)) {
					priority -= 100;
				} else {
					priority += 100;
				}
			}
		}
		return priority;
	}

}

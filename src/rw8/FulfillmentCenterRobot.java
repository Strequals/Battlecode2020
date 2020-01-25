package rw8;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public strictfp class FulfillmentCenterRobot extends Robot {
	
	public boolean makeOne;
	public ArrayList<MapLocation> enemyNetGuns;
	
	public int friendlyDrones;
	public int nearbyLandscapers;
	public int nearbyVaporators;
	public boolean isFreeDrone;
	public boolean rushDetected = false;
	
	public static final int WEIGHT = 100;
	public static final int BASE_WEIGHT = 600;
	public static final int LS_WEIGHT = 20;
	public static final int VAPORATOR_WEIGHT = 10;
	public static final int DIST_HQ = 64; // emergency drone range

	public FulfillmentCenterRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
		makeOne = false;
		enemyNetGuns = new ArrayList<MapLocation>();
	}

	@Override
	public void run() throws GameActionException {
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		int enemies = 0;
		enemyNetGuns.clear();
		friendlyDrones = 0;
		nearbyLandscapers = 0;
		nearbyVaporators = 0;
		if (round == roundCreated) {
			if (location.distanceSquaredTo(hqLocation)<=2) {
				makeOne = true;
			}
		}
		isFreeDrone = false;
		
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				//Friendly Units
				switch (r.getType()) {
				case DELIVERY_DRONE:
					friendlyDrones++;
					if (!r.currentlyHoldingUnit) {
						isFreeDrone = true;
					}
					break;
				case LANDSCAPER:
					nearbyLandscapers++;
					break;
				case VAPORATOR:
					nearbyVaporators++;
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
					enemyNetGuns.add(r.location);
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
		
		System.out.println("RD:"+rushDetected+",IFD:"+isFreeDrone);
		if (soup > RobotType.DELIVERY_DRONE.cost && (((enemies/2>friendlyDrones || (rushDetected && !isFreeDrone)) && location.distanceSquaredTo(hqLocation) < DIST_HQ) || makeOne || (round < TURTLE_END && soup > RobotType.DELIVERY_DRONE.cost + BASE_WEIGHT + friendlyDrones * WEIGHT - nearbyLandscapers * LS_WEIGHT - nearbyVaporators * VAPORATOR_WEIGHT))) {
			Direction hqDirection = location.directionTo(hqLocation);
			if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, hqDirection) && isSafe(location.add(hqDirection))) {
				rc.buildRobot(RobotType.DELIVERY_DRONE, hqDirection);
				makeOne = false;
				return;
			}
			Direction left = hqDirection;
			Direction right = hqDirection;
			for (int i = 4; i-->0;) {
				left = left.rotateLeft();
				right = right.rotateRight();
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, left) && isSafe(location.add(left))) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, left);
					makeOne = false;
					return;
				}
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, right) && isSafe(location.add(right))) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, right);
					makeOne = false;
					return;
				}

			}
		}
		
		//After turtle end
		if (soup > RobotType.DELIVERY_DRONE.cost && soup > RobotType.DELIVERY_DRONE.cost + BASE_WEIGHT + friendlyDrones * WEIGHT- nearbyLandscapers * LS_WEIGHT - nearbyVaporators * VAPORATOR_WEIGHT) {
			if (enemyHqLocation != null) {
				Direction enemyHqDirection = location.directionTo(enemyHqLocation);
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, enemyHqDirection) && isSafe(location.add(enemyHqDirection))) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, enemyHqDirection);
					makeOne = false;
					return;
				}
				Direction left = enemyHqDirection;
				Direction right = enemyHqDirection;
				for (int i = 4; i-->0;) {
					left = left.rotateLeft();
					right = right.rotateRight();
					if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, left) && isSafe(location.add(left))) {
						rc.buildRobot(RobotType.DELIVERY_DRONE, left);
						makeOne = false;
						return;
					}
					if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, right) && isSafe(location.add(right))) {
						rc.buildRobot(RobotType.DELIVERY_DRONE, right);
						makeOne = false;
						return;
					}

				}
			} else {
				Direction[] directions = Utility.directions;
				Direction d;
				for (int i = 8; i-->0;) {
					d = directions[i];
					if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, d) && isSafe(location.add(d))) {
						makeOne = false;
						rc.buildRobot(RobotType.DELIVERY_DRONE, d);
					}
				}
			}
			


		}

	}
	
	public boolean isSafe(MapLocation ml) {
		ArrayList<MapLocation> ngs = enemyNetGuns;
		MapLocation netGunLoc;
		for (int i = ngs.size(); i-->0;) {
			netGunLoc = ngs.get(i);
			if (netGunLoc.isWithinDistanceSquared(ml, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void processMessage(Communications.Message m, int x, int y) {
		switch (m) {
			case HQ_LOCATION:
				hqLocation = new MapLocation(x, y);
//				System.out.println("Received HQ location: " + x + ", " + y);
				break;
			case HQ_UNDER_ATTACK:
				if (location.distanceSquaredTo(hqLocation) <= 2) {
					rushDetected = true;
				}
				break;
		}

	}

}

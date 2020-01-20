package rw7;

import battlecode.common.*;

public abstract strictfp class Robot {
	public final RobotController rc;
	public static MapLocation hqLocation;
	public final Team team;
	public final int id;
	public final int mapWidth;
	public final int mapHeight;
	public final RobotType type;
	public final int TURTLE_ROUND = 150;
	public final int TURTLE_END = 700;

	public RobotInfo[] nearbyRobots;
	public int round;
	public int soup;
	public MapLocation location;
	public int senseRadiusSq;
	public int dirtCarrying;
	public int soupCarrying;
	public float cooldownTurns;
	public int robotElevation;
	
	public int roundCreated;
	public MapLocation enemyHqLocation;
	
	
	public static int hqX3;
	public static int hqY3;
	
	public Robot(RobotController rc) throws GameActionException {
		this.rc = rc;
		team = rc.getTeam();
		id = rc.getID();
		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
		type = rc.getType();
		round = 0;
	}
	
	public abstract void run() throws GameActionException;
	
	public abstract void processMessage(int m, int x, int y);
	
	public void loop() {
		while (true) {
			try {
				//put code common to all units here, such as communication processing
				
				
				//Read messages from last round
				if (round == 0 && type != RobotType.HQ) {
					//The robot has just been created, find the HQ location
					Communications.processFirstBlock(rc, this);
					roundCreated = rc.getRoundNum();
					System.out.println("hqLoc:"+hqLocation);
					if (hqLocation != null) {
						hqX3 = hqLocation.x%3;
						hqY3 = hqLocation.y%3;
					}
					System.out.println("hqX2 "+hqX3+", hqY2 "+hqY3);
				}if (round > 1) {
					Communications.processLastBlock(rc, this);
				}
				
				//Update Game State
				nearbyRobots = rc.senseNearbyRobots();
				round = rc.getRoundNum();
				location = rc.getLocation();
				senseRadiusSq = rc.getCurrentSensorRadiusSquared();
				dirtCarrying = rc.getDirtCarrying();
				soupCarrying = rc.getSoupCarrying();
				cooldownTurns = rc.getCooldownTurns();
				robotElevation = rc.senseElevation(location);
				soup = rc.getTeamSoup();
				
				
				
				//Communications
				Communications.calculateSecret(round);
				
				
				
				
				
				run();
				
				
				Communications.sendMessages(rc);
				System.out.println("BC left: " + Clock.getBytecodesLeft());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Clock.yield();
		}
	}
	
	public static boolean pathTile(MapLocation ml) {
		return (ml.x%3 != hqX3) || (ml.y%3 != hqY3) && (ml.distanceSquaredTo(hqLocation) != 4);
	}
	
	public static boolean pitTile(MapLocation ml) {
		return ((ml.x%3 == hqX3) && (ml.y%3 == hqY3)) || (ml.distanceSquaredTo(hqLocation) == 4);
	}
	
	public static boolean buildingTile(MapLocation ml) {
		return ((ml.x%3 != hqX3) && (ml.y%3 != hqY3)) && (ml.distanceSquaredTo(hqLocation) != 4);
	}
	
	public static boolean initialBuildingTile(MapLocation ml) {
		return !pitTile(ml) && Utility.chebyshev(ml, hqLocation) == 3;
	}

}

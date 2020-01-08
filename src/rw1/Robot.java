package rw1;

import battlecode.common.*;

public abstract strictfp class Robot {
	public final RobotController rc;
	public MapLocation hqLocation;
	public final Team team;
	public final int id;
	public final int mapWidth;
	public final int mapHeight;
	public final RobotType type;

	public RobotInfo[] nearbyRobots;
	public int round;
	public MapLocation location;
	public int senseRadiusSq;
	public int dirtCarrying;
	public int soupCarrying;
	
	
	public Robot(RobotController rc) throws GameActionException {
		this.rc = rc;
		team = rc.getTeam();
		id = rc.getID();
		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
		type = rc.getType();
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while (true) {
			try {
				//put code common to all units here, such as communication processing
				//Update Game State
				nearbyRobots = rc.senseNearbyRobots();
				round = rc.getRoundNum();
				location = rc.getLocation();
				senseRadiusSq = rc.getCurrentSensorRadiusSquared();
				dirtCarrying = rc.getDirtCarrying();
				soupCarrying = rc.getSoupCarrying();
				
				run();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

}

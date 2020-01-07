package rw1;

import battlecode.common.*;

public abstract strictfp class Robot {
	public final RobotController rc;
	public MapLocation hqLocation;
	public final Team team;
	public final int id;
	
	public Robot(RobotController rc) throws GameActionException {
		this.rc = rc;
		team = rc.getTeam();
		id = rc.getID();
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while (true) {
			try {
				//put code common to all units here, such as communication processing
				run();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

}

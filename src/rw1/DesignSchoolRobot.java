package rw1;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public strictfp class DesignSchoolRobot extends Robot {

	public DesignSchoolRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws GameActionException {
		Direction[] dirs = Utility.directions;
		for (int i = dirs.length; --i >= 0;) {
			if (rc.canBuildRobot(RobotType.LANDSCAPER, dirs[i])) {
				rc.buildRobot(RobotType.LANDSCAPER, dirs[i]);
			}
		}
		
	}

	@Override
	public void processMessage(int m, int x, int y) {
		// TODO Auto-generated method stub
		
	}

}

package rw8;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public strictfp class RefineryRobot extends Robot {

	public RefineryRobot(RobotController rc) throws GameActionException {
		super(rc);
	}

	@Override
	public void run() throws GameActionException {}

	@Override
	public void processMessage(Communications.Message m, int x, int y) {}
}

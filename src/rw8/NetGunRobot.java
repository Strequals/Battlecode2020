package rw8;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public strictfp class NetGunRobot extends Robot {

    public NetGunRobot(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        RobotInfo closestDrone = null;
        int droneDistSquared = Integer.MAX_VALUE;

        RobotInfo[] ri = nearbyRobots;
        RobotInfo r;
        for (int i = ri.length; --i >= 0; ) {
            r = ri[i];
            if (r.getTeam() == team.opponent() && r.getType() == RobotType.DELIVERY_DRONE) {
                int distance = r.location.distanceSquaredTo(location);
                if (distance < droneDistSquared) {
                    droneDistSquared = distance;
                    closestDrone = r;
                }
            }
        }

        if (closestDrone != null) {
            // pew pew pew
            rc.shootUnit(closestDrone.getID());
        }
    }

    @Override
    public void processMessage(Communications.Message m, int x, int y) {
    }
}

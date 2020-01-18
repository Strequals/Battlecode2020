package rw1;

import battlecode.common.*;

import static rw1.Utility.MAX_MINERS;

public strictfp class HQRobot extends Robot {

    public HQState hqState;
    public int numMiners = 0;

    enum HQState {
        NORMAL
    }

    public HQRobot(RobotController rc) throws GameActionException {
        super(rc);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void run() throws GameActionException {
        propaganda();

        //Process nearby robots
        RobotInfo[] ri = nearbyRobots;
        RobotInfo r;
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
                        rc.shootUnit(r.getID());
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

        //Broadcast HQ location on round 1
        if (round == 1) {
            Communications.queueMessage(rc, 20, 1, location.x, location.y);
        }

        if ((round < TURTLE_ROUND && numMiners < MAX_MINERS) || soup > RobotType.DESIGN_SCHOOL.cost + 20 * RobotType.LANDSCAPER.cost) {
            //Try building miner
            Direction[] dirs = Utility.directions;
            for (int i = dirs.length; --i >= 0;) {
                if (rc.canBuildRobot(RobotType.MINER, dirs[i])) {
                    rc.buildRobot(RobotType.MINER, dirs[i]);
                    numMiners++;
                }
            }
        }
    }

    /**
     * Teh devs have not been impartial. We must ensure that this travesty is not forgotten.
     *
     * Uses debug lines and dots to put "No bias" onto the field. Prints a longer message to the debug console.
     */
    private void propaganda() {
        // N
        rc.setIndicatorLine(new MapLocation(0, 0), new MapLocation(0, 4), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(0, 4), new MapLocation(2, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(2, 0), new MapLocation(2, 4), 0, 0, 0);

        // o
        rc.setIndicatorLine(new MapLocation(3, 2), new MapLocation(5, 2), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(5, 2), new MapLocation(5, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(5, 0), new MapLocation(3, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(3, 0), new MapLocation(3, 2), 0, 0, 0);

        // Space

        // b
        rc.setIndicatorLine(new MapLocation(7, 0), new MapLocation(7, 4), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(9, 2), new MapLocation(7, 2), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(9, 2), new MapLocation(9, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(9, 0), new MapLocation(7, 0), 0, 0, 0);

        // i
        rc.setIndicatorLine(new MapLocation(10, 0), new MapLocation(10, 2), 0, 0, 0);
        rc.setIndicatorDot(new MapLocation(10, 3), 0, 0, 0);

        // a
        rc.setIndicatorLine(new MapLocation(13, 1), new MapLocation(11, 1), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(11, 1), new MapLocation(11, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(11, 0), new MapLocation(13, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(13, 0), new MapLocation(13, 2), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(13, 2), new MapLocation(11, 2), 0, 0, 0);

        // s
        rc.setIndicatorLine(new MapLocation(15, 2), new MapLocation(14, 2), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(14, 2), new MapLocation(15, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(15, 0), new MapLocation(14, 0), 0, 0, 0);
    }

    @Override
    public void processMessage(int m, int x, int y) {
        // TODO Auto-generated method stub

    }

}

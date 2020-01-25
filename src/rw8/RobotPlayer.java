package rw8;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public strictfp class RobotPlayer {

    public static void run(RobotController rc) throws GameActionException {
        Robot r = null;
        switch (rc.getType()) {
            case HQ:
                r = new HQRobot(rc);
                break;
            case MINER:
                r = new MinerRobot(rc);
                break;
            case REFINERY:
                r = new RefineryRobot(rc);
                break;
            case VAPORATOR:
                r = new VaporatorRobot(rc);
                break;
            case DESIGN_SCHOOL:
                r = new DesignSchoolRobot(rc);
                break;
            case FULFILLMENT_CENTER:
                r = new FulfillmentCenterRobot(rc);
                break;
            case LANDSCAPER:
                r = new LandscaperRobot(rc);
                break;
            case DELIVERY_DRONE:
                r = new DeliveryDroneRobot(rc);
                break;
            case NET_GUN:
                r = new NetGunRobot(rc);
        }

        r.loop();
    }

}

package rw8;

import battlecode.common.*;

import java.util.ArrayList;

public strictfp class FulfillmentCenterRobot extends Robot {

    private static final int WEIGHT = 100;
    private static final int BASE_WEIGHT = 600;
    private static final int LS_WEIGHT = 20;
    private static final int VAPORATOR_WEIGHT = 10;
    private static final int DIST_HQ = 64; // emergency drone range
    private boolean makeOne;
    private ArrayList<MapLocation> enemyNetGuns;
    private boolean rushDetected = false;

    public FulfillmentCenterRobot(RobotController rc) throws GameActionException {
        super(rc);

        makeOne = false;
        enemyNetGuns = new ArrayList<>();
    }

    @Override
    public void run() throws GameActionException {
        RobotInfo[] ri = nearbyRobots;
        RobotInfo r;
        int enemies = 0;
        enemyNetGuns.clear();
        int friendlyDrones = 0;
        int nearbyLandscapers = 0;
        int nearbyVaporators = 0;
        if (round == roundCreated) {
            if (location.distanceSquaredTo(hqLocation) <= 2) {
                makeOne = true;
            }
        }
        boolean isFreeDrone = false;

        for (int i = ri.length; --i >= 0; ) {
            r = ri[i];
            if (r.getTeam() == team) {
                // Friendly Units
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
                }
            } else if (r.getTeam() != Team.NEUTRAL) {
                // Enemy Units
                switch (r.getType()) {
                    case MINER:
                    case LANDSCAPER:
                        enemies++;
                        break;
                    case NET_GUN:
                        enemyNetGuns.add(r.location);
                        break;
                }
            }
        }

        if (!rc.isReady()) return;

        System.out.println("RD:" + rushDetected + ",IFD:" + isFreeDrone);
        int totalWeight = RobotType.DELIVERY_DRONE.cost + BASE_WEIGHT + friendlyDrones * WEIGHT - nearbyLandscapers * LS_WEIGHT - nearbyVaporators * VAPORATOR_WEIGHT;
        if (soup > RobotType.DELIVERY_DRONE.cost && (((enemies / 2 > friendlyDrones || (rushDetected && !isFreeDrone)) && location.distanceSquaredTo(hqLocation) < DIST_HQ) || makeOne || (round < TURTLE_END && soup > totalWeight))) {
            Direction hqDirection = location.directionTo(hqLocation);
            if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, hqDirection) && isSafe(location.add(hqDirection))) {
                rc.buildRobot(RobotType.DELIVERY_DRONE, hqDirection);
                makeOne = false;
                return;
            }
            Direction left = hqDirection;
            Direction right = hqDirection;
            for (int i = 4; i-- > 0; ) {
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

        // After turtle end
        if (soup > RobotType.DELIVERY_DRONE.cost && soup > totalWeight) {
            if (enemyHqLocation != null) {
                Direction enemyHqDirection = location.directionTo(enemyHqLocation);
                if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, enemyHqDirection) && isSafe(location.add(enemyHqDirection))) {
                    rc.buildRobot(RobotType.DELIVERY_DRONE, enemyHqDirection);
                    makeOne = false;
                    return;
                }
                Direction left = enemyHqDirection;
                Direction right = enemyHqDirection;
                for (int i = 4; i-- > 0; ) {
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
                for (int i = 8; i-- > 0; ) {
                    d = directions[i];
                    if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, d) && isSafe(location.add(d))) {
                        makeOne = false;
                        rc.buildRobot(RobotType.DELIVERY_DRONE, d);
                    }
                }
            }
        }
    }

    private boolean isSafe(MapLocation ml) {
        ArrayList<MapLocation> ngs = enemyNetGuns;
        MapLocation netGunLoc;
        for (int i = ngs.size(); i-- > 0; ) {
            netGunLoc = ngs.get(i);
            if (netGunLoc.isWithinDistanceSquared(ml, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void processMessage(Communications.Message m, MapLocation messageLocation) {
        switch (m) {
            case HQ_LOCATION:
                hqLocation = messageLocation;
//				System.out.println("Received HQ location: " + x + ", " + y);
                break;
            case HQ_UNDER_ATTACK:
                if (location.isWithinDistanceSquared(hqLocation, 2)) {
                    rushDetected = true;
                }
                break;
        }

    }

}

package rw8;

import battlecode.common.*;

import java.util.ArrayList;

public strictfp class HQRobot extends Robot {

    private static final int INITIAL_MINERS = 3;
    private static final int MIN_ROUND_BFS = 200;
    private static final int MINER_WEIGHT = 70;
    private static final int BASE_MINER = -140;
    private int numMiners;
    private ArrayList<MapLocation> designSchoolLocations;
    private boolean rushDetected = false;
    private int MAX_MINERS;
    private boolean isDesignSchool;
    private boolean isFulfillmentCenter;

    private boolean minerRequested = false;

    private LinkedQueue<MapLocation> open;
    private ArrayList<MapLocation> closed;

    public HQRobot(RobotController rc) throws GameActionException {
        super(rc);

        MAX_MINERS = INITIAL_MINERS;
        numMiners = 0;
        designSchoolLocations = new ArrayList<>();

        open = new LinkedQueue<>();
        closed = new ArrayList<>();

        hqLocation = location;
    }

    @Override
    public void run() throws GameActionException {
        propaganda();

        // Process nearby robots
        RobotInfo[] ri = nearbyRobots;
        RobotInfo r;
        for (int i = ri.length; --i >= 0; ) {
            r = ri[i];
            if (r.getTeam() == team) {
                // Friendly Units
                switch (r.getType()) {
                    case DESIGN_SCHOOL:
                        if (!isDesignSchool && r.location.isWithinDistanceSquared(location, 8)) {
                            isDesignSchool = true;
                            MAX_MINERS++;
                        }
                        break;
                    case FULFILLMENT_CENTER:
                        if (!isFulfillmentCenter && r.location.isWithinDistanceSquared(location, 8)) {
                            isFulfillmentCenter = true;
                            MAX_MINERS++;
                        }
                        break;
                    default:
                        break;
                }
            } else if (r.getTeam() == Team.NEUTRAL) {
//				 It's a cow, yeet it from our base
                if (round > 100) {
                    Communications.queueMessage(Communications.Message.COW_NEAR_HQ, r.location);
                }
            } else {
                //Enemy Units
                switch (r.getType()) {
                    case MINER:
                    case LANDSCAPER:
                        //Call the drones
                        //Communications.sendMessage(rc);
                        if (!rushDetected) {
                            rushDetected = true;
                            Communications.queueMessage(Communications.Message.HQ_UNDER_ATTACK, r.location);
                        }
                        break;
                    case DELIVERY_DRONE:
                        // pew pew pew
                        // TODO: Shoot closest drone first
                        if (rc.canShootUnit(r.ID)) {
                            rc.shootUnit(r.ID);
                        }
                        return;
                    default:
                        // TODO: Probably some structure, bury it if possible but low priority
                        break;
                }
            }
        }

        //Broadcast HQ location on round 1
        if (round == 1) {
            Communications.queueMessage(Communications.Message.HQ_LOCATION, location);
        }

        if (enemyHqLocation != null && round % 20 == 0) {
            if (soup >= 1)
                Communications.queueMessage(Communications.Message.ENEMY_HQ_LOCATION, enemyHqLocation);
        }

        if (round > MIN_ROUND_BFS && round % 20 == 0) {
            MapLocation fillLoc = doBFS();
            if (fillLoc != null) {
                Communications.queueMessage(Communications.Message.TERRAFORM_LOCATION, fillLoc);
            }
        }

        if (soup > RobotType.MINER.cost &&
                ((minerRequested || numMiners < MAX_MINERS) && soup > Math.min(600, BASE_MINER + numMiners * MINER_WEIGHT))) {
            //Try building miner
            Direction[] dirs = Utility.directions;
            for (int i = dirs.length; --i >= 0; ) {
                if (rc.canBuildRobot(RobotType.MINER, dirs[i])) {
                    rc.buildRobot(RobotType.MINER, dirs[i]);
                    numMiners++;
                }
            }
        }
    }

    /**
     * Searches for a low elevation or flooded tile near the HQ
     *
     * @return The MapLocation of the closest, by Chebyshev distance, low elevation or flooded tile
     */
    private MapLocation doBFS() throws GameActionException {
        open.clear();
        open.add(location);
        closed.clear();
        closed.add(location);
        MapLocation ml;
        Direction[] directions = Utility.directions;
        Direction d;
        MapLocation adj;
        while (open.hasNext()) {
            ml = open.poll();

            if ((Utility.chebyshev(location, ml) > 2 && rc.senseElevation(ml) < Utility.MAX_HEIGHT_THRESHOLD) || rc.senseFlooding(ml)) {
                return ml;
            }

            for (int i = 8; i-- > 0; ) {
                d = directions[i];
                adj = ml.add(d);

                if (!rc.onTheMap(adj) || !rc.canSenseLocation(adj) || pitTile(adj)) continue;
                if (rc.senseElevation(adj) >= Utility.MAX_HEIGHT_THRESHOLD) continue;

                if (!closed.contains(adj)) {
                    open.add(adj);
                    closed.add(adj);
                }
            }
        }

        return null;

    }

    /**
     * Teh devs have not been impartial. We must ensure that this travesty is not forgotten.
     * <p>
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

        // sz
        rc.setIndicatorLine(new MapLocation(15, 2), new MapLocation(14, 2), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(14, 2), new MapLocation(15, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(15, 0), new MapLocation(14, 0), 0, 0, 0);
    }

    @Override
    public void processMessage(Communications.Message m, MapLocation messageLocation) {
        switch (m) {
            case SOUP_LOCATION:
                minerRequested = true;
                break;
            case ENEMY_HQ_LOCATION:
            case DRONE_RUSH_ENEMY_HQ:
                enemyHqLocation = messageLocation;
                break;
            case DESIGN_SCHOOL_LOCATION:
                if (!designSchoolLocations.contains(messageLocation)) {
                    designSchoolLocations.add(messageLocation);
                }
                rc.setIndicatorLine(location, messageLocation, 0, 255, 255);
                break;
            case HQ_UNDER_ATTACK:
                rushDetected = true;
                break;
        }
    }
}

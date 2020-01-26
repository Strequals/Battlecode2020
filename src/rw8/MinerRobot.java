package rw8;

import battlecode.common.*;

import java.util.ArrayList;

public strictfp class MinerRobot extends Robot {

    private static final int DESIGN_SCHOOL_WEIGHT = 550;
    private static final int FULFILLMENT_CENTER_WEIGHT = 570;
    private static final int A = 623;
    private static final int B = 49;
    private static final int MOVE_TO_MATRIX_ROUND = 500;
    private static final int DISTANCE_REFINERY_THRESHOLD = 400; // minimum distance apart for refineries
    private static final int DISTANCE_SOUP_THRESHOLD = 25; //maximum distance from refinery to soup deposit upon creation
    private static final int MAX_VAPORATOR_BUILD_ROUND = 1400;
    private static final int FC_DIST = 8;
    private MinerState minerState;
    private MapLocation soupMine;
    private boolean navigatingReturn;
    private MapLocation returnLoc;
    private int random; // A random number from 0 to 255
    private Direction lastDirection;
    private int soupCommunicateCooldown;
    private int designSchoolBuildCooldown;
    private MinerState prevState; // Stores state when switching to move_matrix state
    private ArrayList<MapLocation> refineries;
    private boolean builderDS = false;
    private boolean builderFC = false;
    private boolean hqAvailable = true;
    private MapLocation nearestNetgun;
    private boolean isBuilder;
    private boolean isRefineryNearby = false;
    private boolean isFreeFriendlyDrone;
    private boolean dsBuilt = false;
    private boolean fcBuilt = false;
    private boolean ngBuilt = false;
    private boolean enemySpotted = false;
    private boolean enemyDroneSpotted = false;
    private MapLocation nearestRefinery;
    private MapLocation nearestSoup;
    private int hqDist;
    private RobotInfo nearestEDrone;
    private MapLocation frontLocation;
    private int maxWaitTurns = 10;
    private boolean rushDetected = false;

    public MinerRobot(RobotController rc) throws GameActionException {
        super(rc);

        minerState = MinerState.SEEKING;
        navigatingReturn = false;
        refineries = new ArrayList<>();
        random = round % 256;
        soupCommunicateCooldown = 0;
    }

    @Override
    public void run() throws GameActionException {
        RobotInfo[] ri = nearbyRobots;
        RobotInfo r;
        int nearbyMiners = 1;
        int netgunDistance = 10000;

        isFreeFriendlyDrone = false;

        MapLocation nearestTerraformer = null;
        int terraformerDistance = 1000;
        int droneDist = 1000;

        fcBuilt = false;
        dsBuilt = false;
        for (int i = ri.length; --i >= 0; ) {
            r = ri[i];
            if (r.getTeam() == team) {
                // Friendly Units
                switch (r.getType()) {
                    case HQ:
                        hqLocation = r.getLocation();
                        break;
                    case MINER:
                        nearbyMiners++;
                        break;
                    case DESIGN_SCHOOL:
                        dsBuilt = true;
                        if (r.location.isAdjacentTo(hqLocation)) {
                            builderDS = true;
                        }
                        break;
                    case NET_GUN:
                        ngBuilt = true;
                        int distance = location.distanceSquaredTo(r.location);
                        if (distance <= netgunDistance) {
                            // Won't update if the nearest netgun isn't in visual range, but not a problem
                            nearestNetgun = r.location;
                            netgunDistance = distance;
                        }
                        break;
                    case LANDSCAPER:
                        if (Utility.chebyshev(hqLocation, r.location) > 2) {
                            int dist = Utility.chebyshev(r.location, location);
                            if (dist < terraformerDistance) {
                                terraformerDistance = dist;
                                nearestTerraformer = r.location;
                            }
                        }
                        break;
                    case FULFILLMENT_CENTER:
                        fcBuilt = true;
                        if (r.location.isAdjacentTo(hqLocation)) {
                            builderFC = true;
                        }
                        break;
                    case DELIVERY_DRONE:
                        if (!r.currentlyHoldingUnit) {
                            isFreeFriendlyDrone = true;
                        }
                        break;
                    case REFINERY:
                        isRefineryNearby = true;
                        if (!refineries.contains(r.location)) {
                            refineries.add(r.location);
                        }
                        break;
                    case VAPORATOR:
                        break;
                }
            } else if (r.getTeam() != Team.NEUTRAL) {
                // Enemy Units
                switch (r.getType()) {
                    case DELIVERY_DRONE:
                        enemyDroneSpotted = true;
                        int distance = location.distanceSquaredTo(r.location);
                        if (distance < droneDist) {
                            droneDist = distance;
                        }
                        nearestEDrone = r;
                        break;
                    case MINER:
                    case LANDSCAPER:
                        enemySpotted = true;
                        if (round < TURTLE_ROUND && !rushDetected) {
                            rushDetected = true;
                            Communications.queueMessage(Communications.Message.HQ_UNDER_ATTACK, r.location);
                        }
                        break;
                    case HQ:
                        if (enemyHqLocation == null) {
                            // Notify other units of enemy HQ location
                            Communications.queueMessage(Communications.Message.ENEMY_HQ_LOCATION, r.location);
                            enemyHqLocation = r.location;
                        }
                        break;
                }
            }
        }

        // Calculate Random
        random = (A * random + B) % 256;

        if (netgunDistance > 5 && droneDist <= 3) { // Not needed, since if no drones, default drone distance is 100
            escape();
            return;
        } else if (droneDist <= 1) {
            escape();
            return;
        }

        MapLocation ml;
        int rSq = senseRadiusSq;
        int radius = (int) (Math.sqrt(rSq));
        int dx;
        int dy;
        int rad0 = -1;
        int rad;
        int totalSoup = 0;
        int s;
        MapLocation nearestSoup = null;
        for (int x = Math.max(0, location.x - radius); x <= Math.min(mapWidth - 1, location.x + radius); x++) {
            for (int y = Math.max(0, location.y - radius); y <= Math.min(mapHeight - 1, location.y + radius); y++) {
                dx = x - location.x;
                dy = y - location.y;
                rad = dx * dx + dy * dy;
                if (rad > rSq) continue;
                ml = new MapLocation(x, y);
                s = rc.senseSoup(ml);
                if (s > 0) {
                    if (rad0 == -1 || rad < rad0) {
                        rad0 = rad;
                        nearestSoup = ml;
                    }
                    totalSoup += s;
                }
            }
        }

        if (soupCommunicateCooldown > 0) {
            soupCommunicateCooldown--;
        }

        if (nearestSoup != null && totalSoup / nearbyMiners > 100 && nearbyMiners < 4) {
            if (soup > 1 && soupCommunicateCooldown == 0) {
                Communications.queueMessage(Communications.Message.SOUP_LOCATION, nearestSoup);
                soupCommunicateCooldown += 20; //don't communicate soup location for another 20 turns
            }
        }

        if (round >= TURTLE_ROUND && returnLoc == hqLocation && refineries.size() > 0) {
            returnLoc = null;
            navigatingReturn = false;
        }

        nearestRefinery = null;
        // Calculate return location
        if (refineries.size() > 0) {
            ArrayList<MapLocation> refs = refineries;
            int dist = 1000000;
            int rld;
            RobotInfo botInfo;
            MapLocation rl;
            for (int i = refs.size(); i-- > 0; ) {
                rl = refs.get(i);
                rld = location.distanceSquaredTo(rl);
                if (rc.canSenseLocation(rl)) {
                    botInfo = rc.senseRobotAtLocation(rl);
                    if (botInfo == null || botInfo.team != team || botInfo.type != RobotType.REFINERY) {
                        refineries.remove(rl);
                        if (returnLoc != null && returnLoc.equals(rl)) {
                            returnLoc = null;
                            navigatingReturn = false;
                        }
                        if (soup > 1) {
                            Communications.queueMessage(Communications.Message.REFINERY_REMOVED, rl);
                        }
                        continue;
                    }
                }
                if (rld < dist) {
                    nearestRefinery = rl;
                    dist = rld;
                }
            }
        } else if (round < TURTLE_ROUND && hqAvailable) {
            returnLoc = hqLocation;
        }

        if (!navigatingReturn) {
            if (nearestRefinery != null) {
                returnLoc = nearestRefinery;
            } else if (round < TURTLE_ROUND && hqAvailable) {
                returnLoc = hqLocation;
            }
        }

        // Move towards matrix or hq if far
        if (minerState != MinerState.MOVE_MATRIX && hqDist > 8 && round > MOVE_TO_MATRIX_ROUND && robotElevation < Utility.MAX_HEIGHT_THRESHOLD) {
            prevState = minerState;
            minerState = MinerState.MOVE_MATRIX;

        }

        if (minerState == MinerState.MOVE_MATRIX && (robotElevation >= Utility.MAX_HEIGHT_THRESHOLD)) {
            minerState = prevState;
        }

        if (cooldownTurns >= 1) return;

        hqDist = 100;
        if (hqLocation != null) hqDist = Utility.chebyshev(location, hqLocation);

        if (doBuilding()) return;

        // Switch to builder if this is among the first miners built
        if (roundCreated <= 2) {
            isBuilder = true;
        }

        System.out.println(minerState);
        switch (minerState) {
            case MINING:
//                System.out.println("MINING");
                MapLocation soupLoc = null;
                int soupLeft = 0;
                if (senseRadiusSq > 1) {
                    int sal;
                    for (int x = Math.max(0, location.x - 1); x <= Math.min(mapWidth - 1, location.x + 1); x++) {
                        for (int y = Math.max(0, location.y - 1); y <= Math.min(mapHeight - 1, location.y + 1); y++) {
                            ml = new MapLocation(x, y);
                            sal = rc.senseSoup(ml);
                            if (sal > soupLeft) {
                                soupLoc = ml;
                                soupLeft = sal;
                            }
                        }
                    }
                } else if (senseRadiusSq == 1) {
                    int sal;
                    for (int x = Math.max(0, location.x - 1); x <= Math.min(mapWidth - 1, location.x + 1); x++) {
                        for (int y = Math.max(0, location.y - 1); y <= Math.min(mapHeight - 1, location.y + 1); y++) {
                            if ((x < 0 ? -x : x) + (y < 0 ? -y : y) > 1) continue;
                            ml = new MapLocation(x, y);
                            sal = rc.senseSoup(ml);
                            if (sal > soupLeft) {
                                soupLoc = ml;
                                soupLeft = sal;
                            }
                        }
                    }
                } else {
                    System.out.println("cannot sense locations");
                    return;
                }
                if (soupLoc != null) {
//                    System.out.println("CARRYING " + soupCarrying +" / " + type.soupLimit + " SOUPS");
                    rc.mineSoup(location.directionTo(soupLoc));
                    if (soupCarrying + Math.min(soupLeft, GameConstants.SOUP_MINING_RATE) >= type.soupLimit) {
                        minerState = MinerState.RETURNING;
                    }
                    break;
                }
                //this code is executed if mining fails
                minerState = MinerState.SEEKING;
            case SEEKING:
                //System.out.println("SEEKING");

                if (soupMine != null && rc.canSenseLocation(soupMine) && rc.senseSoup(soupMine) == 0) {
                    soupMine = null;
                }

                // Search for a soup deposit, check optimal soup deposit within radius
                if (soupMine == null) {
                    if (nearestSoup != null) {
                        soupMine = nearestSoup;
                        Nav.beginNav(rc, this, soupMine);
                    } else {
                        if (round > TURTLE_END && frontLocation != null) {
                            if (Utility.chebyshev(location, frontLocation) <= 2) {
                                frontLocation = null;
                            } else {
                                if (Nav.target == null || !Nav.target.equals(frontLocation)) {
                                    Nav.beginNav(rc, this, frontLocation);
                                }
                                Nav.nav(rc, this);
                                return;
                            }
                        }
                        moveScout(rc);
                        return;
                    }

                }
                // rc.setIndicatorLine(location, soupMine, 100, 0, 255);

                // Check if can begin mining
                MapLocation adjacentSoup = null;
                int soups = 0;
                if (senseRadiusSq > 1) {
                    int sal;
                    for (int x = Math.max(0, location.x - 1); x <= Math.min(mapWidth - 1, location.x + 1); x++) {
                        for (int y = Math.max(0, location.y - 1); y <= Math.min(mapHeight - 1, location.y + 1); y++) {
                            ml = new MapLocation(x, y);
                            sal = rc.senseSoup(ml);
                            if (sal > soups) {
                                adjacentSoup = ml;
                                soups = sal;
                            }
                        }
                    }
                }

                if (adjacentSoup == null) {
                    if (soupMine != null && (Nav.target == null || !Nav.target.equals(soupMine))) {
                        Nav.beginNav(rc, this, soupMine);
                    }
                    Nav.nav(rc, this);
                } else {
                    rc.mineSoup(location.directionTo(adjacentSoup));
                    if (soupCarrying + Math.min(soups, GameConstants.SOUP_MINING_RATE) >= type.soupLimit) {
                        minerState = MinerState.RETURNING;
                    } else {
                        minerState = MinerState.MINING;
                    }
                }
                break;
            case RETURNING:
//                System.out.println("RETURNING");
                if (returnLoc != null) {
                    if (returnLoc.equals(hqLocation)) {
                        Direction[] dirs = Utility.directions;
                        MapLocation hqAdjacent;
                        boolean allFilled = true;
                        RobotInfo botInf;
                        int elev;
                        for (int i = 8; i-- > 0; ) {
                            hqAdjacent = hqLocation.add(dirs[i]);

                            if (!rc.onTheMap(hqAdjacent)) continue;

                            if (!rc.canSenseLocation(hqAdjacent)) {
                                allFilled = false;
                                break;
                            }

                            if (hqAdjacent.x == 0 && (hqLocation.y == hqAdjacent.y || (hqLocation.y == 1 && hqLocation.y > hqAdjacent.y) || (mapHeight - hqLocation.y == 2 && hqLocation.y < hqAdjacent.y))) {
                                continue;
                            }

                            if (hqAdjacent.y == 0 && (hqLocation.x == hqAdjacent.x || (hqLocation.x == 1 && hqLocation.x > hqAdjacent.x) || (mapHeight - hqLocation.x == 2 && hqLocation.x < hqAdjacent.x))) {
                                continue;
                            }

                            botInf = rc.senseRobotAtLocation(hqAdjacent);
                            if (botInf == null) {
                                allFilled = false;
                                break;
                            }

                            elev = rc.senseElevation(hqAdjacent);
                            if (elev - robotElevation <= GameConstants.MAX_DIRT_DIFFERENCE) {
                                allFilled = false;
                                break;
                            }
                        }
                        if (!allFilled) hqAvailable = false;
                    }

                    if (Nav.target == null || !Nav.target.equals(returnLoc)) {
                        Nav.beginNav(rc, this, returnLoc);
                    }
                    Direction dirToReturn = location.directionTo(returnLoc);
                    if (location.distanceSquaredTo(returnLoc) <= 2) {
                        rc.depositSoup(dirToReturn, rc.getSoupCarrying());
                        minerState = MinerState.SEEKING;
                        navigatingReturn = false;
                        if (soupMine != null) {
                            Nav.beginNav(rc, this, soupMine);
                        }
                        return;
                    }
                    Nav.nav(rc, this);
                    return;
                }

                moveScout(rc);
                break;
            case MOVE_MATRIX:
                if (nearestTerraformer != null) {
                    rc.setIndicatorLine(location, nearestTerraformer, 100, 255, 100);
                    if (location.distanceSquaredTo(nearestTerraformer) <= 2) {
                        // Sit and wait
                        maxWaitTurns--;
                        if (maxWaitTurns > 0) return;
                    }
                    if (maxWaitTurns < 10) maxWaitTurns++;
                    if (Nav.target == null || !Nav.target.equals(nearestTerraformer)) {
                        Nav.beginNav(rc, this, nearestTerraformer);
                    }
                    Nav.nav(rc, this);
                } else {
                    if (Nav.target == null || !Nav.target.equals(hqLocation)) {
                        Nav.beginNav(rc, this, hqLocation);
                    }
                    Nav.nav(rc, this);
                }
                break;
        }
    }

    /**
     * Run towards nearest netgun
     * Only trigger if dsquare distance to nearest netgun is greater than 5
     */
    private void escape() throws GameActionException {
        System.out.println("Running...");
        if (nearestNetgun != null) {
            if (location.add(location.directionTo(nearestNetgun)).distanceSquaredTo(nearestEDrone.location) >= 8) {
                if (Nav.target == null || !Nav.target.equals(nearestNetgun)) {
                    Nav.beginNav(rc, this, nearestNetgun);
                }
                Nav.nav(rc, this);
                return;
            }
        } else {
            if (location.add(location.directionTo(hqLocation)).distanceSquaredTo(nearestEDrone.location) >= 8) {
                if (Nav.target == null || !Nav.target.equals(hqLocation)) {
                    Nav.beginNav(rc, this, hqLocation);
                }
                Nav.nav(rc, this);
                return;
            }
        }
        fuzzy(nearestEDrone.location.directionTo(location));
        System.out.println("Running with fuzzy");
    }

    private void fuzzy(Direction d) throws GameActionException {
        if (rc.canMove(d) && !rc.senseFlooding(rc.adjacentLocation(d))) {
            rc.move(d);
//            return true;
            return;
        }
        Direction dr = d.rotateRight();
        if (rc.canMove(dr) && !rc.senseFlooding(rc.adjacentLocation(dr))) {
            rc.move(dr);
//            return true;
            return;
        }
        Direction dl = d.rotateRight();
        if (rc.canMove(dl) && !rc.senseFlooding(rc.adjacentLocation(dl))) {
            rc.move(dl);
//            return true;
        }
//        return false;
    }

    private void tryBuildFC() throws GameActionException {
        int hqDist = location.distanceSquaredTo(hqLocation);
        if (hqDist <= 4) {
            Direction d = location.directionTo(hqLocation);
            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, d)) {
                rc.buildRobot(RobotType.FULFILLMENT_CENTER, d);
                return;
            }
            Direction right = d.rotateRight();
            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, right)) {
                rc.buildRobot(RobotType.FULFILLMENT_CENTER, right);
                return;
            }
            Direction left = d.rotateLeft();
            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, left)) {
                rc.buildRobot(RobotType.FULFILLMENT_CENTER, left);
                return;
            }
        } else if (hqDist <= 8) {
            Direction d = location.directionTo(hqLocation);
            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, d)) {
                rc.buildRobot(RobotType.FULFILLMENT_CENTER, d);
                return;
            }
            Direction right = d.rotateRight();
            MapLocation rl = location.add(right);
            if (rl.distanceSquaredTo(hqLocation) <= 2 && rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, right)) {
                rc.buildRobot(RobotType.FULFILLMENT_CENTER, right);
                return;
            }
            Direction left = d.rotateLeft();
            MapLocation ll = location.add(left);
            if (ll.distanceSquaredTo(hqLocation) <= 2 && rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, left)) {
                rc.buildRobot(RobotType.FULFILLMENT_CENTER, left);
                return;
            }
        }

        if (Nav.target == null || !Nav.target.equals(hqLocation)) {
            Nav.beginNav(rc, this, hqLocation);
        }
        Nav.nav(rc, this);
    }

    private void tryBuildDS() throws GameActionException {
        int hqDist = location.distanceSquaredTo(hqLocation);
        if (hqDist <= 4) {
            Direction d = location.directionTo(hqLocation);
            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
                rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
                return;
            }
            Direction right = d.rotateRight();
            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, right)) {
                rc.buildRobot(RobotType.DESIGN_SCHOOL, right);
                return;
            }
            Direction left = d.rotateLeft();
            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, left)) {
                rc.buildRobot(RobotType.DESIGN_SCHOOL, left);
                return;
            }
        } else if (hqDist <= 8) {
            Direction d = location.directionTo(hqLocation);
            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
                rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
                return;
            }
            Direction right = d.rotateRight();
            MapLocation rl = location.add(right);
            if (rl.distanceSquaredTo(hqLocation) <= 2 && rc.canBuildRobot(RobotType.DESIGN_SCHOOL, right)) {
                rc.buildRobot(RobotType.DESIGN_SCHOOL, right);
                return;
            }
            Direction left = d.rotateLeft();
            MapLocation ll = location.add(left);
            if (ll.distanceSquaredTo(hqLocation) <= 2 && rc.canBuildRobot(RobotType.DESIGN_SCHOOL, left)) {
                rc.buildRobot(RobotType.DESIGN_SCHOOL, left);
                return;
            }
        }

        if (Nav.target == null || !Nav.target.equals(hqLocation)) {
            Nav.beginNav(rc, this, hqLocation);
        }
        Nav.nav(rc, this);
    }

    private void moveScout(RobotController rc) throws GameActionException {
        if (lastDirection == null) {
            if (hqLocation != null) {
                lastDirection = hqLocation.directionTo(location);
            } else {
                System.out.println("RANDOM WALK ERROR");
                lastDirection = Direction.NORTHEAST;
            }
        }

        int i = random % 100;

        if (i < 50 && tryMove(rc, lastDirection)) {
            return;
        }

        Direction left = lastDirection;
        Direction right = lastDirection;

        for (int j = 0; j < 3; j++) {
            left = left.rotateLeft();
            if (i < Utility.MOVE_CHANCE_BREAKPOINTS[j * 2] && tryMove(rc, left)) {
                return;
            }

            right = right.rotateRight();
            if (i < Utility.MOVE_CHANCE_BREAKPOINTS[(j * 2) + 1] && tryMove(rc, right)) {
                return;
            }
        }

        tryMove(rc, left.rotateLeft());
    }

    public boolean tryMove(RobotController rc, Direction d) throws GameActionException {
        if (rc.canMove(d) && !rc.senseFlooding(location.add(d))) {
            rc.move(d);
            lastDirection = d;
            return true;
        }
        return false;
    }

    @Override
    public void processMessage(Communications.Message m, MapLocation messageLocation) {
        switch (m) {
            case HQ_LOCATION:
                hqLocation = messageLocation;
//                System.out.println("Received HQ location: " + x + ", " + y);
                break;
            case SOUP_LOCATION:
                if (soupMine == null && !isBuilder) {
                    soupMine = messageLocation;
                    Nav.beginNav(rc, this, soupMine);
                }
//                System.out.println("Received soup location: " + x + ", " + y);
                break;
            case REFINERY_LOCATION:
//                System.out.println("Received Refinery location: " + x + ", " + y);
                if (!refineries.contains(messageLocation)) {
                    refineries.add(messageLocation);
                }
                break;
            case DESIGN_SCHOOL_LOCATION:
                designSchoolBuildCooldown = 50;
                break;
            case REFINERY_REMOVED:
                refineries.remove(messageLocation);
                if (returnLoc != null && returnLoc.equals(messageLocation)) {
                    returnLoc = null;
                    navigatingReturn = false;
                }
                break;
            case HQ_UNDER_ATTACK:
                rushDetected = true;
                break;
            case TERRAFORM_LOCATION:
                if (frontLocation == null || Utility.chebyshev(location, messageLocation) < Utility.chebyshev(location, frontLocation)) {
                    frontLocation = messageLocation;
                }
                break;
        }
    }

    @Override
    public boolean canMove(Direction d) throws GameActionException {
        MapLocation ml = rc.adjacentLocation(d);
        return rc.canMove(d) && !rc.senseFlooding(ml) && (minerState != MinerState.MOVE_MATRIX || pathTile(ml));
    }

    private boolean doBuilding() throws GameActionException {
        if (round < TURTLE_ROUND) {
            if (!builderFC && soup > RobotType.FULFILLMENT_CENTER.cost) {
                System.out.println("TryFC");
                tryBuildFC();
                return true;
            } else if (!builderDS && (rushDetected || isFreeFriendlyDrone) && soup > RobotType.DESIGN_SCHOOL.cost) {
                System.out.println("TryDS");
                tryBuildDS();
                return true;
            }
        }

        MapLocation ml;

        boolean canBuild = true;
        if (robotElevation < Utility.MAX_HEIGHT_THRESHOLD || location.isWithinDistanceSquared(hqLocation, 8))
            canBuild = false;


        if (minerState != MinerState.MOVE_MATRIX) {
            // Build Refinery
            if (!isRefineryNearby && soup > RobotType.REFINERY.cost && (round > CLOSE_TURTLE_END || !hqAvailable)) {
                // TODO: Nearest soup is always null
                if ((nearestRefinery == null || location.distanceSquaredTo(nearestRefinery) >= DISTANCE_REFINERY_THRESHOLD) && nearestSoup != null && location.distanceSquaredTo(nearestSoup) <= DISTANCE_SOUP_THRESHOLD) {
                    Direction[] dirs = Utility.directions;
                    Direction d;
                    MapLocation refLoc;
                    for (int i = dirs.length; --i >= 0; ) {
                        d = dirs[i];
                        refLoc = location.add(d);
                        if (rc.canBuildRobot(RobotType.REFINERY, d) && Utility.chebyshev(refLoc, hqLocation) > 3) {
                            rc.buildRobot(RobotType.REFINERY, d);
                            MapLocation refineryLoc = location.add(d);
                            if (soup > 5)
                                Communications.queueMessage(Communications.Message.REFINERY_LOCATION, refineryLoc);
                            refineries.add(refineryLoc);
                            return true;
                        }
                    }
                }
            }

            if (canBuild) {
                // Build Design School
                if (designSchoolBuildCooldown > 0) designSchoolBuildCooldown--;
                if (round > TURTLE_ROUND && !dsBuilt && designSchoolBuildCooldown == 0 && soup > DESIGN_SCHOOL_WEIGHT && hqLocation != null && hqDist >= 2) {
                    Direction[] dirs = Utility.directions;
                    Direction d;
                    for (int i = 8; i-- > 0; ) {
                        d = dirs[i];
                        ml = location.add(d);
                        if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
                            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
                                rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
                                dsBuilt = true;
                                return true;
                            }
                        }
                    }
                }

                // Build Vaporator
                // TODO: Change the lines below so the first part of the if statement isn't always true, or delete it
//                boolean builderVP = false;
//                if ((!builderVP || soup > RobotType.VAPORATOR.cost + VAPORATOR_WEIGHT * numVaporators) && round < MAX_VAPORATOR_BUILD_ROUND && hqDist >= 2) {
                if (round < MAX_VAPORATOR_BUILD_ROUND && hqDist >= 2) {
                    Direction[] dirs = Utility.directions;
                    Direction d;
                    ml = null;
                    for (int i = 8; i-- > 0; ) {
                        d = dirs[i];
                        ml = location.add(d);
                        if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
                            if (rc.canBuildRobot(RobotType.VAPORATOR, d)) {
                                rc.buildRobot(RobotType.VAPORATOR, d);
                                return true;
                            }
                        }
                    }
                }

                //Build Fulfillment Center
                if (round > TURTLE_ROUND && soup > FULFILLMENT_CENTER_WEIGHT && !fcBuilt && ((enemySpotted && hqDist < FC_DIST) || soup > 800) && hqDist >= 2) {
                    Direction[] dirs = Utility.directions;
                    Direction d;
                    for (int i = 8; i-- > 0; ) {
                        d = dirs[i];
                        ml = location.add(d);
                        if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
                            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, d)) {
                                rc.buildRobot(RobotType.FULFILLMENT_CENTER, d);
                                return true;
                            }
                        }
                    }
                }

                //Build Netgun
                if (soup > RobotType.NET_GUN.cost && (soup > 800 || enemyDroneSpotted) && !ngBuilt && hqDist >= 2) {
                    Direction[] dirs = Utility.directions;
                    Direction d;
                    for (int i = 8; i-- > 0; ) {
                        d = dirs[i];
                        ml = location.add(d);
                        if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
                            if (rc.canBuildRobot(RobotType.NET_GUN, d)) {
                                rc.buildRobot(RobotType.NET_GUN, d);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    enum MinerState {
        SEEKING, MINING, RETURNING, MOVE_MATRIX
    }
}

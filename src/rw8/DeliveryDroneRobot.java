package rw8;

import battlecode.common.*;

import java.util.ArrayList;

public strictfp class DeliveryDroneRobot extends Robot {

    private static final int RUSH_SAFE_TURNS = 50;
    private static final int DEFEND_RANGE = 15;
    private static final int DRONE_COUNT_RUSH = 16;
    private static final int A = 623;
    private static final int B = 49;
    private static final int RUSH_RANGE = 12; //Chebyshev range to regard an enemy as a rusher
    private static final int CRUNCH_RANGE = 8; //If within this distance when crunch signal detected, crunch
    private static final int ASSAULT_ROUND = 1300;
    private MapLocation homeLocation;
    private MapLocation targetLocation;  //building or location to clear enemy robots from
    private MapLocation targetLocationf; //friendly robot location
    private RobotInfo targetRobot;
    private RobotInfo targetFriendly; //robot to move to pathtile
    private MapLocation nearestWater;
    private MapLocation nearestSafe; //nearest path tile
    private DroneState state;
    private boolean rush = false; //avoid netguns + hq?
    private boolean sentEHQL = false;
    private Direction lastDirection;
    private int friendlyDrones;
    private MapLocation minerAssistLocation;  //where to put
    private RobotInfo minerToAssist;
    private EnemyHqPossiblePosition enemyHqScouting = EnemyHqPossiblePosition.X_FLIP;
    private MapLocation rushLocation;
    private int turnsSinceRush;
    private boolean rushDetected = true;
    private boolean carryingEnemy;
    private boolean carryingAssaulter;
    private int random; // A random number from 0 to 255
    private ArrayList<MapLocation> enemyNetguns;
    public DeliveryDroneRobot(RobotController rc) throws GameActionException {
        super(rc);

        RobotInfo[] ri = rc.senseNearbyRobots(2, rc.getTeam());
        RobotInfo r;
        for (int i = ri.length; --i >= 0; ) {
            r = ri[i];
            // Friendly Units
            if (r.getType() == RobotType.FULFILLMENT_CENTER && location.isAdjacentTo(r.getLocation())) {
                homeLocation = r.getLocation();
                break;
            }
        }

        enemyNetguns = new ArrayList<>();
        carryingEnemy = false;
        carryingAssaulter = false;
    }

    @Override
    public void run() throws GameActionException {
        DroneNav.beginNav(this, enemyHqScouting.getLocation(hqLocation, mapWidth, mapHeight));

        // Process nearby robots (may have to move this into the if statements below)
        RobotInfo[] ri = nearbyRobots;
        RobotInfo r;
        targetRobot = null;
        targetLocation = null;
        targetFriendly = null;
        targetLocationf = null;
        friendlyDrones = 0;
        int targetDistance = 10000;
        int minerDistance = 10000;
        int distance;

        // Calculate Random
        random = (A * random + B) % 256;
        MapLocation netGunLoc = null;

        ArrayList<MapLocation> enemyGuns = enemyNetguns;
        MapLocation enemyGun;
        for (int i = enemyGuns.size(); i-- > 0; ) {
            enemyGun = enemyGuns.get(i);
            if (rc.canSenseLocation(enemyGun)) {
                r = rc.senseRobotAtLocation(enemyGun);
                if (r == null || r.team == team || r.type != RobotType.NET_GUN) {
                    enemyGuns.remove(i);
                }
            }
        }

        for (int i = ri.length; --i >= 0; ) {
            r = ri[i];
            if (r.getTeam() == team) {
                // Friendly Units
                switch (r.getType()) {
                    case HQ:
                        hqLocation = r.location;
                        break;
                    case FULFILLMENT_CENTER:
                        if (homeLocation == null) {
                            homeLocation = r.location;
                        }
                        break;
                    case MINER:
                        if (state == DroneState.TRANSPORTING || state == DroneState.MINER_ASSIST) {
                            distance = Utility.chebyshev(location, r.location);
                            if (distance < minerDistance && (!pathTile(r.location) || state == DroneState.MINER_ASSIST)) { //kinda hacky, only pick up miner if its not on the path, unless in miner assist mode
                                minerToAssist = r;
                                minerDistance = distance;
                            }
                        }
                        break;
                    case LANDSCAPER:
                        if (state == DroneState.ASSAULTING && (enemyHqLocation == null || !r.location.isAdjacentTo(enemyHqLocation))) {
                            if (rc.canPickUpUnit(r.ID)) {
                                rc.pickUpUnit(r.ID);
                                carryingAssaulter = true;
                                return;
                            }

                            distance = Utility.chebyshev(location, r.location);
                            if (distance < targetDistance && Utility.chebyshev(r.location, hqLocation) > 2) {
                                if (enemyHqLocation == null || Utility.chebyshev(r.location, enemyHqLocation) > 1) {
                                    targetLocationf = r.location;
                                }
                                targetDistance = distance;
                                targetFriendly = r;
                            }
                        }
                        break;
                    case DELIVERY_DRONE:
                        friendlyDrones++;
                        break;
                }
            } else if (r.getTeam() != Team.NEUTRAL) {
                // Enemy Units
                switch (r.getType()) {
                    case MINER:
                    case LANDSCAPER:
                        if (rc.canPickUpUnit(r.ID)) {
                            rc.pickUpUnit(r.ID);
                            carryingEnemy = true;
                            return;
                        }

                        distance = location.distanceSquaredTo(r.location);
                        if (distance < targetDistance) {
                            targetLocation = r.location;
                            targetDistance = distance;
                            targetRobot = r;
                        }
                        if (Utility.chebyshev(r.location, hqLocation) < RUSH_RANGE) {
                            rushDetected = true;
                        }
                        break;
                    case NET_GUN:
                        // Avoid
                        netGunLoc = r.location;
                        if (!enemyNetguns.contains(r.location)) {
                            enemyNetguns.add(r.location);
                        }
                        break;
                    case DESIGN_SCHOOL:
                        if (Utility.chebyshev(r.location, hqLocation) < RUSH_RANGE) {
                            rushDetected = true;
                        }
                        break;
                    case HQ:
                        // We found it!
                        // also avoid
                        enemyHqLocation = r.location;
                        if (!enemyNetguns.contains(r.location)) {
                            enemyNetguns.add(r.location);
                        }
                        break;
                }
            } else {
                if (targetLocation == null) {
                    targetLocation = r.location;
                    targetRobot = r;
                }
            }
        }

        if (enemyHqLocation != null && !sentEHQL) {
            Communications.queueMessage(Communications.Message.ENEMY_HQ_LOCATION, enemyHqLocation);
            sentEHQL = true;
        }

        if (!rc.isCurrentlyHoldingUnit()) {
            carryingAssaulter = false;
            carryingEnemy = false;
        }

        if (netGunLoc != null && !rush && !rushDetected && netGunLoc.isWithinDistanceSquared(location, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
            if (rc.isReady()) {
                if (fuzzy(rc, netGunLoc.directionTo(location))) return;
            }
        }

        if (round < TURTLE_ROUND || rushDetected || (rushLocation != null)) {
            state = DroneState.DEFENDING;
        } else {
            state = DroneState.ATTACKING;
        }

        if (round > ASSAULT_ROUND && shouldAssault()) {
            state = DroneState.ASSAULTING;
        }

        if (!rc.isReady()) {
            return;
        }

        if (state != DroneState.ASSAULTING && enemyHqLocation != null) {
            if (location.distanceSquaredTo(enemyHqLocation) < 8 && targetRobot == null) {
                Communications.queueMessage(Communications.Message.SOUP_LOCATION, enemyHqLocation);
                state = DroneState.ASSAULTING;
            }
        }

        System.out.println(state);
        switch (state) {
            case DEFENDING:
                doDefense();
                break;
            case ATTACKING:
                doAttack();
                break;
            case TRANSPORTING:
                doTransport();
                break;
            case ASSAULTING:
                doAssault();
                break;
            case MINER_ASSIST:
                doAssist();
                break;
        }

        if (rushDetected) {
            turnsSinceRush++;
            if (turnsSinceRush > RUSH_SAFE_TURNS) {
                rushLocation = null;
                turnsSinceRush = 0;
                rushDetected = false;
            }
        }

//        if (DroneNav.target != null) {
//            rc.setIndicatorDot(DroneNav.target, 255, 0, 255);
//        }
        if (nearestWater != null) {
            rc.setIndicatorDot(nearestWater, 0, 255, 255);
        }
        if (targetLocation != null) {
            rc.setIndicatorDot(targetLocation, 255, 0, 0);
        }

    }

    private void doAssist() throws GameActionException {
        if (rc.isCurrentlyHoldingUnit()) {
            if (Utility.chebyshev(location, minerAssistLocation) <= 1) {
                if (rc.canDropUnit(location.directionTo(minerAssistLocation))) {
                    rc.dropUnit(location.directionTo(minerAssistLocation));
                }
            }
        } else if (minerToAssist != null) {
            if (Utility.chebyshev(location, minerToAssist.location) <= 1) {
                if (rc.canPickUpUnit(minerToAssist.ID)) {
                    rc.pickUpUnit(minerToAssist.ID);
                }
            } else {
                if (DroneNav.target == null || !DroneNav.target.equals(minerToAssist.location)) {
                    DroneNav.beginNav(this, minerToAssist.location);
                }
                DroneNav.nav(rc, this);
            }
        } else {
            if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
                DroneNav.beginNav(this, hqLocation);
            }
        }
    }

    private void doAssault() throws GameActionException {
        doInitiateRush();
        if (doDropEnemy()) {
            return;
        }
        if (enemyHqLocation == null) {
            state = DroneState.ATTACKING;
        }
        if (carryingAssaulter) {
            scanForSafe();

            if (nearestSafe == null) {
                if (enemyHqLocation != null) {
                    if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
                        DroneNav.beginNav(this, enemyHqLocation);
                    }
                    DroneNav.nav(rc, this);
                } else {
                    moveScout(rc);
                }
            } else {
                if (Utility.chebyshev(location, nearestSafe) <= 1) {
                    if (rc.canDropUnit(location.directionTo(nearestSafe))) {
                        rc.dropUnit(location.directionTo(nearestSafe));
                    }
                } else {
                    if (DroneNav.target == null || !DroneNav.target.equals(nearestSafe)) {
                        DroneNav.beginNav(this, nearestSafe);
                    }
                    DroneNav.nav(rc, this);
                }
            }
        } else {
            scanForWater();
            if (targetFriendly != null) {
                if (Utility.chebyshev(location, targetLocationf) <= 1) {
                    if (rc.canPickUpUnit(targetFriendly.ID)) {
                        rc.pickUpUnit(targetFriendly.ID);
                        carryingAssaulter = true;
                    }
                } else {
                    if (DroneNav.target == null || !DroneNav.target.equals(targetLocationf)) {
                        DroneNav.beginNav(this, targetLocationf);
                    }
                    DroneNav.nav(rc, this);
                }
            } else {
                if (enemyHqLocation != null) {
                    if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
                        DroneNav.beginNav(this, enemyHqLocation);
                    }
                    DroneNav.nav(rc, this);
                } else {
                    moveScout(rc);
                }
            }
        }
    }

    private boolean shouldAssault() {
        return id % 3 == 0;
    }

    private void doInitiateRush() {
        if (enemyHqLocation != null) {
            if (Utility.chebyshev(location, enemyHqLocation) <= CRUNCH_RANGE) {
                if (friendlyDrones >= DRONE_COUNT_RUSH) {
                    Communications.queueMessage(Communications.Message.DRONE_RUSH_ENEMY_HQ, enemyHqLocation);
                }
            }
        }
    }

    private boolean scanForSafe() throws GameActionException {
        // Scan for a safe spot next to enemy hq
        MapLocation ml;
        int rSq = senseRadiusSq;
        int radius = (int) (Math.sqrt(rSq));
        int dx;
        int dy;
        int rad0 = 10000;
        int rad;
        int csDist;
        boolean toRet = false;  // Found an appropriate tile?
        for (int x = Math.max(0, location.x - radius); x <= Math.min(mapWidth - 1, location.x + radius); x++) {
            for (int y = Math.max(0, location.y - radius); y <= Math.min(mapHeight - 1, location.y + radius); y++) {
                dx = x - location.x;
                dy = y - location.y;
                if (dx == 0 && dy == 0) continue;
                rad = dx * dx + dy * dy;
                if (rad > rSq) continue;
                ml = new MapLocation(x, y);
                csDist = Utility.chebyshev(location, ml);
                if (csDist < rad0 && enemyHqLocation != null && Utility.chebyshev(ml, enemyHqLocation) == 1) {
                    RobotInfo ri = rc.senseRobotAtLocation(ml);
                    if (ri != null) continue;
                    rad0 = csDist;
                    nearestSafe = ml;
                    toRet = true;
                }
            }
        }
        return toRet;
    }

    private boolean tryDrown() throws GameActionException {
        Direction[] directions = Utility.directions;
        Direction d;
        MapLocation ml;
        for (int i = 8; i-- > 0; ) {
            d = directions[i];
            ml = location.add(d);
            if (rc.canSenseLocation(ml) && rc.senseFlooding(ml) && rc.canDropUnit(d)) {
                rc.dropUnit(d);
                return true;
            }
        }
        return false;
    }

    private void doTransport() throws GameActionException {
        if (rc.isCurrentlyHoldingUnit()) {
            if (nearestSafe == null) {
                if (scanForSafe()) {
                    DroneNav.beginNav(this, nearestSafe);
                    DroneNav.nav(rc, this);
                } else {
                    if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
                        DroneNav.beginNav(this, hqLocation);
                    }
                    DroneNav.nav(rc, this);
                }
            } else {
                if (Utility.chebyshev(location, nearestSafe) <= 1 && rc.canDropUnit(location.directionTo(nearestSafe))) {
                    rc.dropUnit(location.directionTo(nearestSafe));
                    return;
                }

                if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
                    DroneNav.beginNav(this, hqLocation);
                }

                DroneNav.nav(rc, this);
            }
        } else if (minerToAssist != null) {
            if (Utility.chebyshev(location, minerToAssist.location) <= 1) {
                if (rc.canPickUpUnit(minerToAssist.ID)) {
                    rc.pickUpUnit(minerToAssist.ID);
                    scanForSafe();
                }
            } else {
                if (DroneNav.target == null || !DroneNav.target.equals(minerToAssist.location)) {
                    DroneNav.beginNav(this, minerToAssist.location);
                }
                DroneNav.nav(rc, this);
            }
        } else {
            if (DroneNav.target == null || !DroneNav.target.equals(minerAssistLocation)) {
                DroneNav.beginNav(this, minerAssistLocation);
            }
        }
    }

    private void scanForWater() throws GameActionException {
        MapLocation ml;
        int rSq = senseRadiusSq;
        int radius = (int) (Math.sqrt(rSq));
        int dx;
        int dy;
        int rad0 = 10000;
        int rad;
        int csDist;
        RobotInfo ri;
        for (int x = Math.max(0, location.x - radius); x <= Math.min(mapWidth - 1, location.x + radius); x++) {
            for (int y = Math.max(0, location.y - radius); y <= Math.min(mapHeight - 1, location.y + radius); y++) {
                dx = x - location.x;
                dy = y - location.y;
                if (dx == 0 && dy == 0) continue;
                rad = dx * dx + dy * dy;
                if (rad > rSq) continue;
                ml = new MapLocation(x, y);
                csDist = Utility.chebyshev(location, ml);
                if (csDist < rad0 && rc.senseFlooding(ml)) {
                    ri = rc.senseRobotAtLocation(ml);
                    if (ri == null) {
                        rad0 = csDist;
                        nearestWater = ml;
                    }
                }
            }
        }
    }

    private boolean doDropEnemy() throws GameActionException {
        System.out.println("dde");
        if (carryingEnemy) {
            // Pathfind towards target (water, soup)
            // If any of 8 locations around are flooded, place robot into flood, update nearestWater

            if (nearestWater != null) {
//                rc.setIndicatorLine(location, nearestWater, 0, 100, 200);
                if (tryDrown()) return true;
                if (DroneNav.target == null || !DroneNav.target.equals(nearestWater)) {
                    DroneNav.beginNav(this, nearestWater);
                }
                System.out.println("Nav water:" + nearestWater);
                DroneNav.nav(rc, this);
                return true;
            } else {
                moveScout(rc);
                return true;
            }

        } else if (!rc.isCurrentlyHoldingUnit()) {
            if (targetRobot != null) {
                if (Utility.chebyshev(location, targetLocation) <= 1) {
                    if (rc.canPickUpUnit(targetRobot.ID)) {
                        rc.pickUpUnit(targetRobot.ID);
                        carryingEnemy = true;
                        return true;
                    }
                } else {
                    if (DroneNav.target == null || !DroneNav.target.equals(targetLocation)) {
                        DroneNav.beginNav(this, targetLocation);
                    }
                    DroneNav.nav(rc, this);
                    return true;
                }
            } else if (targetLocation != null) {
                if (DroneNav.target == null || !DroneNav.target.equals(targetLocation)) {
                    DroneNav.beginNav(this, targetLocation);
                }
                DroneNav.nav(rc, this);
                return true;
            }
        }
        return false;
    }

    private void doAttack() throws GameActionException {
        scanForWater();
        doInitiateRush();

        if (doDropEnemy()) return;

        if (enemyHqLocation != null) {
            if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
                DroneNav.beginNav(this, enemyHqLocation);
            }
            DroneNav.nav(rc, this);
            return;
        }

        moveScout(rc);
    }

    private void doDefense() throws GameActionException {
        scanForWater();

        if (rushLocation != null && targetLocation == null) {
            targetLocation = rushLocation;
        }

        if (doDropEnemy()) {
            return;
        }

        if (Utility.chebyshev(location, hqLocation) < 5) {
            moveScout(rc);
        } else {
            if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
                DroneNav.beginNav(this, hqLocation);
            }
            DroneNav.nav(rc, this);
        }
    }

    private void moveScout(RobotController rc) throws GameActionException {
        if (enemyHqLocation == null) {
            scoutEnemyHq();
        } else {
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
    }

    private void scoutEnemyHq() throws GameActionException {
        System.out.println("Scouting enemy HQ");

        if (rc.isCurrentlyHoldingUnit()) {
            Direction[] directions = Utility.directions;
            Direction direction;

            for (int i = 8; i-- > 0; ) {
                direction = directions[i];
                if (rc.senseFlooding(location.add(direction))) {
                    rc.dropUnit(direction);
                    return;
                }
            }
        }

        RobotInfo robot;
        RobotInfo[] nearbyRobots = super.nearbyRobots;

        for (int i = nearbyRobots.length; i-- > 0; ) {
            robot = nearbyRobots[i];
            if (robot.team == team.opponent() && rc.canPickUpUnit(robot.getID())) {
                rc.pickUpUnit(robot.getID());
                return;
            }
        }

        MapLocation enemyHqScoutingLocation = enemyHqScouting.getLocation(hqLocation, mapWidth, mapHeight);
        if (rc.canSenseLocation(enemyHqScoutingLocation)) {
            robot = rc.senseRobotAtLocation(enemyHqScoutingLocation);
            if (robot != null && robot.type == RobotType.HQ) {
                // Enemy
                foundEnemyHq(enemyHqScoutingLocation);
                return;
            } else {
                // Not enemy
                switch (enemyHqScouting) {
                    case X_FLIP:
                        // Checked our first position
                        enemyHqScouting = EnemyHqPossiblePosition.ROTATION;
                        DroneNav.beginNav(this, enemyHqScouting.getLocation(hqLocation, mapWidth, mapHeight));
                        break;
                    case ROTATION:
                        // Checked first and second position, so must be third
                        foundEnemyHq(EnemyHqPossiblePosition.Y_FLIP.getLocation(hqLocation, mapWidth, mapHeight));
                        return;
                    default:
                        throw new Error("Can't happen");
                }
            }
        }

        DroneNav.nav(rc, this);
    }

    private void foundEnemyHq(MapLocation location) {
        enemyHqLocation = location;
        state = DroneState.ASSAULTING;
    }

    public boolean tryMove(RobotController rc, Direction d) throws GameActionException {
        if (canMove(rc, d)) {
            rc.move(d);
            lastDirection = d;
            return true;
        }
        return false;
    }

    public boolean canMove(RobotController rc, Direction d) {
        MapLocation ml = location.add(d);
        if (!rush) {
            ArrayList<MapLocation> enemyGuns = enemyNetguns;
            MapLocation enemyGun;
            for (int i = enemyGuns.size(); i-- > 0; ) {
                enemyGun = enemyGuns.get(i);
                if (enemyGun.isWithinDistanceSquared(ml, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
                    return false;
                }
            }
        }
        return rc.canMove(d);
    }

    public boolean fuzzy(RobotController rc, Direction d) throws GameActionException {
        if (canMove(rc, d)) {
            rc.move(d);
            return true;
        }

        Direction dr = d.rotateRight();
        Direction dl = d.rotateLeft();
        if (canMove(rc, dr)) {
            rc.move(dr);
            return true;
        } else if (canMove(rc, dl)) {
            rc.move(dl);
            return true;
        }

        return false;
    }

    @Override
    public void processMessage(Communications.Message m, MapLocation messageLocation) {
        switch (m) {
            case HQ_LOCATION:
                hqLocation = messageLocation;
//                System.out.println("Received HQ location: " + messageLocation.x + ", " + messageLocation.y);
                break;
            case ENEMY_HQ_LOCATION:
                if (enemyHqLocation == null) {
                    enemyHqLocation = messageLocation;
                    enemyNetguns.add(enemyHqLocation);
                }
                sentEHQL = true; // If already received enemy hq location, don't rebroadcast
                break;
            case DRONE_RUSH_ENEMY_HQ:
                if (enemyHqLocation == null) {
                    enemyHqLocation = messageLocation;
                    enemyNetguns.add(enemyHqLocation);
                }
                if (Utility.chebyshev(enemyHqLocation, location) <= CRUNCH_RANGE) {
                    rush = true;
                }
                break;
            case HQ_UNDER_ATTACK:
                rushLocation = messageLocation;
                turnsSinceRush = 0;
                if (Utility.chebyshev(location, hqLocation) < DEFEND_RANGE) {
                    rushDetected = true;
                }
                break;
            case ENTER_TRANSPORT_MODE:
                state = DroneState.TRANSPORTING;
                minerAssistLocation = messageLocation;
                break;
            case ENTER_ASSAULT_MODE:
                enemyHqLocation = messageLocation;
                if (shouldAssault()) {
                    state = DroneState.ASSAULTING;
                }
                break;
            case ENTER_MINER_ASSIST_MODE:
                minerAssistLocation = messageLocation; // DOES NOT WORK, NEEDS LOCATION TO FIND MINER AND LOCATION TO PLACE MINER
                state = DroneState.MINER_ASSIST; //maybe move miner towards hq if it's stuck pathfinding?
                break;
        }
    }

    private enum DroneState {
        ATTACKING,
        ASSAULTING,
        DEFENDING,
        TRANSPORTING,
        MINER_ASSIST
    }

}

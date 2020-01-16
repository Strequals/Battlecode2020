package rw1;

import java.util.ArrayList;

import battlecode.common.*;

public strictfp class MinerRobot extends Robot {

    public MapLocation hqLocation;
    public MinerState minerState;
    public MapLocation soupMine;
    public boolean navgatingReturn;
    public boolean dsBuilt;
    public MapLocation returnLoc;

    public ArrayList<MapLocation> refineries;

    public static final int DISTANCE_THRESHOLD = 100;

    enum MinerState {
        SEEKING, MINING, RETURNING
    }

    public MinerRobot(RobotController rc) throws GameActionException {
        super(rc);
        // TODO Auto-generated constructor stub
        minerState = MinerState.SEEKING;
        navgatingReturn = false;
        dsBuilt = false;
        refineries = new ArrayList<MapLocation>();

    }

    @Override
    public void run() throws GameActionException {
        RobotInfo[] ri = nearbyRobots;
        RobotInfo r;
        int nearbyMiners = 1;
        boolean isRefineryNearby = false;
        for (int i = ri.length; --i >= 0;) {
            r = ri[i];
            if (r.getTeam() == team) {
                //Friendly Units
                switch (r.getType()) {
                    case HQ:
                        hqLocation = r.getLocation();
                        break;
                    case MINER:
                        nearbyMiners++;
                        break;
                    case DESIGN_SCHOOL:
                        dsBuilt = true;
                        break;
                    case REFINERY:
                        isRefineryNearby = true;
                        if (!refineries.contains(r.location)) refineries.add(r.location);
                    default:
                        break;

                }
            } else if (r.getTeam() == Team.NEUTRAL) {
                //yeet the cow
                if (round > 100) {
                    //Call the drones
                    //Communications.sendMessage(rc);
                }
            } else {
                //Enemy Units
                switch (r.getType()) {
                    case HQ:
                        //Notify other units of enemy HQ location
                    default:

                        break;
                }
            }
        }

        if (cooldownTurns >= 1) return;

        MapLocation ml;
        int rSq = senseRadiusSq;
        int radius = (int)(Math.sqrt(rSq));
        ml = null;
        int dx;
        int dy;
        int rad0 = -1;
        int rad;
        int bytecode1 = Clock.getBytecodesLeft();
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
                if (s > 0 && !rc.senseFlooding(ml)) {
                    if (rad0 == -1 || rad < rad0) {
                        rad0 = rad;
                        nearestSoup = ml;
                    }
                    totalSoup += s;
                }
            }
        }
        System.out.println("SEARCH bytecodes: " + (bytecode1 - Clock.getBytecodesLeft()));

        if (soupMine !=null && totalSoup / nearbyMiners > 100 && nearbyMiners < 4) {
            Communications.sendMessage(rc, 1, 2, soupMine.x, soupMine.y);
        }

        //Calculate return location
        if (refineries.size()>0) {
            ArrayList<MapLocation> refs = refineries;
            int dist = 1000000;
            int rld;
            MapLocation rl;
            for (int i = refs.size(); i-->0;) {
                rl = refs.get(i);
                rld = location.distanceSquaredTo(rl);
                if (rld < dist) {
                    returnLoc = rl;
                    dist = rld;
                }
            }
        } else if (round < TURTLE_ROUND) {
            returnLoc = hqLocation;
        }

        //Build Refinery
        if (hqLocation != null && totalSoup > 150 && !isRefineryNearby && soup > RobotType.REFINERY.cost && Utility.chebyshev(location, hqLocation) >= 5) {
            if (returnLoc == null || location.distanceSquaredTo(returnLoc) >= DISTANCE_THRESHOLD) {
                Direction[] dirs = Utility.directions;
                Direction d;
                for (int i = dirs.length; --i >= 0;) {
                    d = dirs[i];
                    if (rc.canBuildRobot(RobotType.REFINERY, d)) {
                        rc.buildRobot(RobotType.REFINERY, d);
                        MapLocation refineryLoc = location.add(d);
                        if (soup>5) Communications.sendMessage(rc, 5, 5, refineryLoc.x, refineryLoc.y);
                        refineries.add(refineryLoc);
                        return;
                    }
                }
            }
        }

        //Build Design School
        if (round > TURTLE_ROUND && !dsBuilt && soup > RobotType.DESIGN_SCHOOL.cost && hqLocation != null && (refineries.size() > 0 || round > 2*TURTLE_ROUND) && location.isWithinDistanceSquared(hqLocation, 8)) {
            Direction[] dirs = Utility.directions;
            ml = null;
            for (int i = 8; i-->0;) {
                ml = hqLocation.add(dirs[i]);
                if (location.distanceSquaredTo(ml)<=2) {
                    Direction d = location.directionTo(ml);
                    if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
                        rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
                        dsBuilt = true;
                        return;
                    }
                }
            }
        }
        if (soupMine != null)System.out.println("TARGETING: " + soupMine.x + ", " + soupMine.y);
        if (Nav.target != null) System.out.println("NAVTARGET: " + Nav.target.x + ", " + Nav.target.y);
        switch (minerState) {
            case MINING:
                System.out.println("MINING");
                MapLocation soupLoc = null;
                int soupLeft = 0;
                ml = null;
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
                    System.out.println("CARRYING " + soupCarrying +" / " + type.soupLimit + " SOUPS");
                    rc.mineSoup(location.directionTo(soupLoc));
                    if (soupCarrying + Math.min(soupLeft, GameConstants.SOUP_MINING_RATE) >= type.soupLimit) {
                        minerState = MinerState.RETURNING;
                    }
                    break;
                }
                //this code is executed if mining fails
                minerState = MinerState.SEEKING;
            case SEEKING:
                System.out.println("SEEKING");

                if (soupMine != null && rc.canSenseLocation(soupMine) && rc.senseSoup(soupMine) == 0) {
                    soupMine = null;
                }

                //search for a soup deposit, check optimal soup deposit within radius
                if (soupMine == null) {
                    if (nearestSoup != null) soupMine = nearestSoup;

                    if (soupMine == null) {
                        //random walk boi
                        fuzzy(rc, Utility.directions[(int) (Math.random() * 8)]);
                        return;
                    } else {
                        Nav.beginNav(rc, this, soupMine);
                    }
                }

                //Check if can begin mining
                MapLocation adjacentSoup = null;
                ml = null;
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
                System.out.println("RETURNING");

                if (!navgatingReturn) {
                    Nav.beginNav(rc, this, returnLoc);
                    navgatingReturn = true;
                }

                if (returnLoc != null) {
                    Direction dirToReturn = location.directionTo(returnLoc);
                    if (location.distanceSquaredTo(returnLoc) <= 2) {
                        rc.depositSoup(dirToReturn, rc.getSoupCarrying());
                        minerState = MinerState.SEEKING;
                        navgatingReturn = false;
                        if (soupMine != null) {
                            Nav.beginNav(rc, this, soupMine);
                        }
                        return;
                    }
                    Nav.nav(rc, this);
                    return;
                }

        }
    }

    public void mine(RobotController rc) {

    }

    public boolean fuzzy(RobotController rc, Direction d) throws GameActionException {
        if (rc.canMove(d) && !rc.senseFlooding(rc.adjacentLocation(d))) {
            rc.move(d);
            return true;
        }
        Direction dr = d.rotateRight();
        if (rc.canMove(dr) && !rc.senseFlooding(rc.adjacentLocation(dr))) {
            rc.move(dr);
            return true;
        }
        Direction dl = d.rotateRight();
        if (rc.canMove(dl) && !rc.senseFlooding(rc.adjacentLocation(dl))) {
            rc.move(dl);
            return true;
        }
        return false;
    }

    @Override
    public void processMessage(int m, int x, int y) {
        switch (m) {
            case 1:
                hqLocation = new MapLocation(x,y);
                System.out.println("Recieved HQ location: " + x + ", " + y);
                break;
            case 2:
                if (soupMine == null) {
                    soupMine = new MapLocation(x, y);
                    Nav.beginNav(rc, this, soupMine);
                }
                System.out.println("Recieved soup location: " + x + ", " + y);
                break;
            case 5:
                MapLocation ml = new MapLocation(x,y);
                System.out.println("Recieved Refinery location: " + x + ", " + y);
                if (!refineries.contains(ml)) refineries.add(ml);
        }

    }
}

package rw8;

import battlecode.common.*;

public abstract strictfp class Robot {
    public final RobotController rc;
    public final Team team;
    public final int id;
    public final int mapWidth;
    public final int mapHeight;
    public final RobotType type;
    public final int TURTLE_ROUND = 150; //1000 guaranteed soup / 8 speed
    public final int CLOSE_TURTLE_END = 500;
    public final int TURTLE_END = 700;
    public MapLocation hqLocation;
    public RobotInfo[] nearbyRobots;
    public int round;
    public int soup;
    public MapLocation location;
    public int senseRadiusSq;
    public int dirtCarrying;
    public int soupCarrying;
    public float cooldownTurns;
    public int robotElevation;

    public int roundCreated;
    public MapLocation enemyHqLocation;

    public int hqXTerraform;
    public int hqYTerraform;

    public Robot(RobotController rc) throws GameActionException {
        this.rc = rc;
        team = rc.getTeam();
        id = rc.getID();
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        type = rc.getType();
        round = 0;

        setVars();

        if (type != RobotType.HQ) {
            //The robot has just been created, find the HQ location
            Communications.processFirstBlock(rc, this);
            roundCreated = rc.getRoundNum();

            if (hqLocation != null) {
                hqXTerraform = hqLocation.x % Utility.TERRAFORM_HOLES_EVERY;
                hqYTerraform = hqLocation.y % Utility.TERRAFORM_HOLES_EVERY;
            }
        }
    }

    public abstract void run() throws GameActionException;

    public abstract void processMessage(Communications.Message m, MapLocation location);

    public void loop() {
        while (true) {
            try {
                // Update Game State
                setVars();

                // Read messages from last round
                if (round == 0 && type != RobotType.HQ) {
                    //The robot has just been created, find the HQ location
                    Communications.processFirstBlock(rc, this);
                    roundCreated = rc.getRoundNum();
                    //System.out.println("hqLoc:"+hqLocation);
                    if (hqLocation != null) {
                        hqXTerraform = hqLocation.x % 2;
                        hqYTerraform = hqLocation.y % 2;
                    }
                    //System.out.println("hqX2 "+hqX3+", hqY2 "+hqY3);
                }
                if (round > 1) {
                    Communications.processLastBlock(rc, this);
                }

                // Communications
                Communications.calculateSecret(round);

                // Set round
                round = rc.getRoundNum();

                run();

                Communications.sendMessages(rc);
                //System.out.println("BC left: " + Clock.getBytecodesLeft());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Clock.yield();
        }
    }

    //	public boolean pathTile(MapLocation ml) {
//		return (ml.x%3 != hqX3) || (ml.y%3 != hqY3) && (ml.distanceSquaredTo(hqLocation) != 4);
//	}
//
//	public boolean pitTile(MapLocation ml) {
//		return ((ml.x%3 == hqX3) && (ml.y%3 == hqY3)) || (ml.distanceSquaredTo(hqLocation) == 4);
//	}
//
//	public boolean buildingTile(MapLocation ml) {
//		return ((ml.x%3 != hqX3) && (ml.y%3 != hqY3)) && (ml.distanceSquaredTo(hqLocation) != 4);
//	}
//
//	public boolean initialBuildingTile(MapLocation ml) {
//		return !pitTile(ml) && Utility.chebyshev(ml, hqLocation) == 3;
//	}
    public boolean pathTile(MapLocation ml) {
        return ((ml.x % 2 != hqXTerraform) || (ml.y % 2 != hqYTerraform)) && !ml.isWithinDistanceSquared(hqLocation, 8);
    }

    public boolean pitTile(MapLocation ml) {
        return ((ml.x % 2 == hqXTerraform) && (ml.y % 2 == hqYTerraform)) && !ml.isWithinDistanceSquared(hqLocation, 8);
    }

    public boolean buildingTile(MapLocation ml) {
        return ((ml.x % 2 != hqXTerraform) && (ml.y % 2 != hqYTerraform)) && !ml.isWithinDistanceSquared(hqLocation, 8);

        //return (ml.x % Utility.TERRAFORM_HOLES_EVERY != hqXTerraform) || (ml.y % Utility.TERRAFORM_HOLES_EVERY != hqYTerraform) && (ml.distanceSquaredTo(hqLocation) != 4);
    }
	
	/*public boolean pitTile(MapLocation ml) {
		return ((ml.x % Utility.TERRAFORM_HOLES_EVERY == hqXTerraform) && (ml.y % Utility.TERRAFORM_HOLES_EVERY == hqYTerraform)) || (ml.distanceSquaredTo(hqLocation) == 4);
	}
	
	public boolean buildingTile(MapLocation ml) {
		return ((ml.x % Utility.TERRAFORM_HOLES_EVERY != hqXTerraform) && (ml.y % Utility.TERRAFORM_HOLES_EVERY != hqYTerraform)) && (ml.distanceSquaredTo(hqLocation) != 4);
	}*/

    public boolean canMove(Direction d) throws GameActionException {
        MapLocation ml = rc.adjacentLocation(d);
        return rc.canMove(d) && !rc.senseFlooding(ml);
    }

    private void setVars() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots();
        location = rc.getLocation();
        senseRadiusSq = rc.getCurrentSensorRadiusSquared();
        dirtCarrying = rc.getDirtCarrying();
        soupCarrying = rc.getSoupCarrying();
        cooldownTurns = rc.getCooldownTurns();
        robotElevation = rc.senseElevation(location);
        soup = rc.getTeamSoup();
    }
}

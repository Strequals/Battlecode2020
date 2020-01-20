package rw7;

import java.util.ArrayList;

import battlecode.common.*;

public strictfp class HQRobot extends Robot {

	public HQState hqState;
	public int numMiners;
	public boolean completedWall = false;
	public int numLandscapers;
	
	public int wallBoundLower;
	public int wallBoundUpper;
	public int wallBoundRight;
	public int wallBoundLeft;
	public int landscaperRequestCooldown = 0;
	public boolean dsAvailable = false;
	
	public ArrayList<MapLocation> designSchoolLocations;
	public MapLocation closestDesignSchool;
	
	public static final int ROUND_3x3 = 400; //If 3x3 wall not completed by round 400, do not try 5x5
	public static final int INITIAL_MINERS = 3;
	public int MAX_MINERS;
	
	
	public static final int MINER_WEIGHT = 0;
	
	public boolean isDesignSchool;
	public boolean isFulfillmentCenter;
	
	public boolean isEnemyRushing;
	
	public boolean minerRequested = false;
	
	enum HQState {
		NORMAL
	}

	public HQRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
		MAX_MINERS = INITIAL_MINERS;
		numMiners = 0;
		numLandscapers = 0;
		designSchoolLocations = new ArrayList<MapLocation>();
	}

    @Override
    public void run() throws GameActionException {
        propaganda();
        
        if (round == 1) {
        	wallBoundLower = Math.max(location.y - 1, 0);
    		wallBoundLeft = Math.max(location.x - 1, 0);
    		wallBoundUpper = Math.min(location.y + 1, mapHeight-1);
    		wallBoundRight = Math.min(location.x + 1, mapWidth-1);
    		hqLocation = location;
        }
        isEnemyRushing = false;
        //Process nearby robots
        RobotInfo[] ri = nearbyRobots;
        RobotInfo r;
        numLandscapers = 0;
        for (int i = ri.length; --i >= 0;) {
            r = ri[i];
            if (r.getTeam() == team) {
                //Friendly Units
                switch (r.getType()) {
                case LANDSCAPER:
                	if (Utility.chebyshev(r.location, location) <=2) {
                		numLandscapers++;
                	}
                case DESIGN_SCHOOL:
                	if (!isDesignSchool && initialBuildingTile(r.location)) {
                		isDesignSchool = true;
                		MAX_MINERS++;
                	}
                	break;
                case FULFILLMENT_CENTER:
                if (!isFulfillmentCenter && initialBuildingTile(r.location)) {
                	isFulfillmentCenter = true;
                	MAX_MINERS++;
                }
                break;
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
                    	isEnemyRushing = true;
                        break;
                    case LANDSCAPER:
                        //Call the drones
                        //Communications.sendMessage(rc);
                    	isEnemyRushing = true;
                        break;
                    case DELIVERY_DRONE:
                        //pew pew pew
                    	if (rc.canShootUnit(r.ID)) {
                    		rc.shootUnit(r.ID);
                    	}
                        
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
        
        if (enemyHqLocation != null && round % 100 == 0) {
        	if (soup > 2) Communications.queueMessage(rc, 2, 3, enemyHqLocation.x, enemyHqLocation.y);
        }

        if (((numMiners < INITIAL_MINERS) || (numMiners < MAX_MINERS && soup > 45 * numMiners)) || soup > RobotType.DESIGN_SCHOOL.cost + 8 * RobotType.VAPORATOR.cost + RobotType.VAPORATOR.cost) {
            //Try building miner
            Direction[] dirs = Utility.directions;
            for (int i = dirs.length; --i >= 0;) {
                if (rc.canBuildRobot(RobotType.MINER, dirs[i])) {
                    rc.buildRobot(RobotType.MINER, dirs[i]);
                    numMiners++;
                }
            }
        }
        
        
        
		/*MapLocation defenseLocation;
		RobotInfo botInfo;
		int rank;
		for (int i = wallBoundLeft; i <= wallBoundRight; i++) {
			for (int j = wallBoundLower; j <= wallBoundUpper; j++) {
				
				defenseLocation = new MapLocation(i,j);
				rank = Utility.chebyshev(location, defenseLocation);
				if (rank == 0) continue;
				if (rc.canSenseLocation(defenseLocation)) {
					botInfo = rc.senseRobotAtLocation(defenseLocation);
					if (botInfo == null || botInfo.team != team || botInfo.type != RobotType.LANDSCAPER) {
						if (rank == 1) {
							++;
							completedWall = false;
						}
					}
				}
				
			}
		}*/
        
        /*completedWall = numLandscapers >= 8;
		
		
		if (!completedWall && round < TURTLE_END && dsAvailable && landscaperRequestCooldown == 0) {
			MapLocation nearest = null;
			int d = 1000;
			ArrayList<MapLocation> locs = designSchoolLocations;
			MapLocation ml = null;
			int dl = 0;
			for (int i = locs.size(); i-->0;) {
				ml = locs.get(i);
				dl = Utility.chebyshev(ml, location);
				if (dl < d) {
					d = dl;
					nearest = ml;
				}
			}
			
			if (soup > 2 && ml != null) {
				rc.setIndicatorLine(location, nearest, 0, 255, 0);
				int amount = 8 - numLandscapers;
				Communications.queueMessage(rc, 2, 10+amount, nearest.x, nearest.y);
				landscaperRequestCooldown += amount + dl + 20;
				designSchoolLocations.remove(nearest);
				
			}
			landscaperRequestCooldown += 10;
		}
		
		if (landscaperRequestCooldown > 0) landscaperRequestCooldown--;*/
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

        // sz
        rc.setIndicatorLine(new MapLocation(15, 2), new MapLocation(14, 2), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(14, 2), new MapLocation(15, 0), 0, 0, 0);
        rc.setIndicatorLine(new MapLocation(15, 0), new MapLocation(14, 0), 0, 0, 0);
    }

    @Override
    public void processMessage(int m, int x, int y) {
        switch (m) {
        case 2:
        	minerRequested = true;
        	break;
        case 3:
        	enemyHqLocation = new MapLocation(x,y);
        	break;
        case 6:
        	MapLocation ml6 = new MapLocation(x, y);
        	if (!designSchoolLocations.contains(ml6)) designSchoolLocations.add(ml6);
        	rc.setIndicatorLine(location, ml6, 0, 255, 255);
        	dsAvailable = true;
        	break;
        }

    }

}

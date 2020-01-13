package rw1;

import battlecode.common.*;

public strictfp class LandscaperRobot extends Robot {

	boolean isEnemyRushing;
	MapLocation hqLocation;
	MapLocation dsLocation;
	MapLocation scaledDsLocation;
	MapLocation inverseDsLocation;

	public LandscaperRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws GameActionException {
		isEnemyRushing = false;
		//Process nearby robots
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		int nearbyLandscapers = 0;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				//Friendly Units
				switch (r.getType()) {
				case HQ:
					hqLocation = r.getLocation();
				case DESIGN_SCHOOL:
					dsLocation = r.getLocation();
				case LANDSCAPER:
					if (r.location.isWithinDistanceSquared(location, 2)) {
						nearbyLandscapers++;
					}
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
					isEnemyRushing = true;
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
		
		if (inverseDsLocation == null && hqLocation != null && dsLocation != null) {
			Direction dir = dsLocation.directionTo(hqLocation);
			inverseDsLocation = hqLocation.add(dir).add(dir);
			dir = hqLocation.directionTo(dsLocation);
			scaledDsLocation = dsLocation.add(dir);
		}
		
		if (cooldownTurns >= 1) return;
		
		int robotRank = Utility.chebyshev(location, hqLocation);
		int expectedLandscapers = 0;
		Direction[] dirs = Utility.directionsC;
		Direction d;
		MapLocation ml;
		int lrank;
		int elev;
		MapLocation high = null; //Highest location to mine from
		Direction dHigh = null;
		int hd = Integer.MIN_VALUE;
		MapLocation low = null; //Lowest location to place at
		Direction dLow = null;
		int ld = Integer.MAX_VALUE;
		for (int i = 9; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			lrank = Utility.chebyshev(ml,hqLocation);
			if (!rc.canSenseLocation(ml)) continue;
			elev = rc.senseElevation(ml);
			
			if (lrank == 2) {
				if (elev < ld && rc.canDepositDirt(d)) {
					low = ml;
					ld = elev;
					dLow = d;
				}
				
			} else if (lrank != 0) {
				if (lrank > 2) elev += 100000;
				if (elev > hd && rc.canDigDirt(d)) {
					high = ml;
					hd = elev;
					dHigh = d;
				}
			}
			if (d == Direction.CENTER) continue;
			if (lrank <= 2 && lrank != 0 && !ml.equals(dsLocation)) {
				expectedLandscapers++;
			}
			if (rc.canMove(d)) {
				if (lrank < robotRank) {
					rc.move(d);
					return;
				} else if (lrank == robotRank) {
					if (!isEnemyRushing) {
						if (inverseDsLocation != null) {
							int dsDist = location.distanceSquaredTo(scaledDsLocation);
							int iDsDist = location.distanceSquaredTo(inverseDsLocation);
							if (dsDist <= iDsDist) {
								if (dsDist < ml.distanceSquaredTo(scaledDsLocation)) {
									rc.move(d);
									return;
								}
							} else {
								if (iDsDist > ml.distanceSquaredTo(inverseDsLocation)) {
									rc.move(d);
									return;
								}
							}
						}
					}
				}
			}
			
		}
		
		if (nearbyLandscapers < expectedLandscapers && round < 2 * TURTLE_ROUND + 50) return;
		
		if (high != null) System.out.println("HIGH: " + high.x + ", " + high.y);
		else System.out.println("ERR CANNOT FIND HIGH");
		if (low != null) System.out.println("LOW: " + low.x + ", " + low.y);
		else System.out.println("ERR CANNOT FIND LOW");
		if (high != null && dirtCarrying <= RobotType.LANDSCAPER.dirtLimit) {
			if (rc.canDigDirt(dHigh)) {
				rc.digDirt(dHigh);
				System.out.println("DIGGING: " + high.x + ", " + high.y);
				return;
			}
		}
		if (low != null && rc.canDepositDirt(dLow)) {
			rc.depositDirt(dLow);
			System.out.println("DEPOSITING: " + low.x + ", " + low.y);
		}

	}
	
	

	@Override
	public void processMessage(int m, int x, int y) {
		switch (m) {
		case 1:
			hqLocation = new MapLocation(x,y);
			System.out.println("Recieved HQ location: " + x + ", " + y);
			break;
		}

	}

}

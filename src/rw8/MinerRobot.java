package rw8;

import java.util.ArrayList;

import battlecode.common.*;

public strictfp class MinerRobot extends Robot {

	public MinerState minerState;

	/**
	 * This is the possible position of the enemy HQ that the miner is currently scouting. Its value is undefined if the
	 * `minerState` is not `SCOUTING_ENEMY_HQ`. Otherwise, it must not be null.
	 */
	private EnemyHqPossiblePosition enemyHqScouting;
	/**
	 * While `minerState` is `SCOUTING_ENEMY_HQ`, this must be location of the possible HQ that the miner is currently
	 * scouting. While `minerState` is `RUSHING_ENEMY_HQ` and `DRONE_NETTING_ENEMY_HQ`, this must be the actual, known
	 * location of the enemy HQ.
	 */
	private MapLocation targetedEnemyHqLocation;

	public static final int DESIGN_SCHOOL_SEEN_TURNS = 50;
	public static final int DESIGN_SCHOOL_WEIGHT = 550;
	public static final int FULFILLMENT_CENTER_WEIGHT = 570;
	public static final int ENEMY_HQ_RANGE = 64;

	public MapLocation soupMine;
	public boolean navigatingReturn;
	public MapLocation returnLoc;
	public int random; // A random number from 0 to 255
	public static final int A = 623;
	public static final int B = 49;
	public Direction lastDirection;
	public int soupCommunicateCooldown;
	public int designSchoolBuildCooldown;
	public int turnsSinceDesignSchoolSeen = 100;
	private boolean enemyBuiltDrones = false;
	public boolean hqInRange = false;
	private boolean builderDS = false;
	private boolean builderFC = false;
	private boolean builderVP = false;
	private boolean hqAvailable = true;
	private int numVaporators;
	private int wallVaporators;
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
	private int vaporatorsInWall;
	private RobotInfo nearestEDrone;
	private MapLocation frontLocation;

	public MapLocation nearestTerraformer;
	private int maxWaitTurns = 10;
	private boolean rushDetected = false;
	public boolean isFreeHQSpot = true;

	public static final int MOVE_TO_MATRIX_ROUND = 500; 
	public static final int MOVE_TO_MATRIX_BUILDER = 100; //Builder should move faster to matrix
	public static final int VAPORATOR_WEIGHT = 20; // need VAPORATOR_WEIGHT more soup to build a vaporator with every vaporator in sight

	public MinerState prevState; //Stores state when switching to move_matrix state

	public ArrayList<MapLocation> refineries;

	public static final int DISTANCE_REFINERY_THRESHOLD = 400; // minimum distance apart for refineries
	public static final int DISTANCE_SOUP_THRESHOLD = 25; //maximum distance from refinery to soup deposit upon creation

	public static final int MAX_VAPORATOR_BUILD_ROUND = 1200;
	public static final int FC_DIST = 8;

	enum MinerState {
		SEEKING, MINING, RETURNING, MOVE_MATRIX, SCOUTING_ENEMY_HQ, RUSHING_ENEMY_HQ, DRONE_NETTING_ENEMY_HQ
	}

	enum EnemyHqPossiblePosition {
		X_FLIP,
		Y_FLIP,
		ROTATION
	}

	public MinerRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
		minerState = MinerState.SEEKING;
		navigatingReturn = false;
		refineries = new ArrayList<MapLocation>();
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

		hqInRange = false;
		nearestTerraformer = null;
		int terraformerDistance = 1000;
      int droneDist = 1000;
		numVaporators = 0;
		vaporatorsInWall = 0;

		fcBuilt = false;
		dsBuilt = false;
		isRefineryNearby = false;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				//Friendly Units
				switch (r.getType()) {
				case HQ:
					hqLocation = r.getLocation();
					hqInRange = true;
					break;
				case MINER:
					nearbyMiners++;
					break;
				case DESIGN_SCHOOL:
					//if (rc.senseElevation(r.location) >= robotElevation-3) {
						dsBuilt = true;
					//}
					if (r.location.isAdjacentTo(hqLocation)) {
						builderDS = true;
					}
					break;
				case NET_GUN:
					ngBuilt = true;
					int distance = location.distanceSquaredTo(r.location);
					if (distance <= netgunDistance) {
						nearestNetgun = r.location;
						netgunDistance = distance;       //won't update if the nearest netgun isnt in visual range, but not a problem
					}
					break;
				case LANDSCAPER:
					if (Utility.chebyshev(hqLocation, r.location) > 2) {
						int dist = Utility.chebyshev(r.location, location);
						if (dist<terraformerDistance) {
							terraformerDistance = dist;
							nearestTerraformer = r.location;
						}
					}
					break;
				case FULFILLMENT_CENTER:
					//if (rc.senseElevation(r.location) >= robotElevation-3) {
						fcBuilt = true;
					//}
					if (r.location.isAdjacentTo(hqLocation)) {
						builderFC = true;
					}
					break;
				case DELIVERY_DRONE:
					if (!r.currentlyHoldingUnit) isFreeFriendlyDrone = true;
					break;
				case REFINERY:
					isRefineryNearby = true;
					if (!refineries.contains(r.location)) refineries.add(r.location);
					break;
				case VAPORATOR:
					numVaporators++;
					if (r.location.isAdjacentTo(hqLocation)) wallVaporators++;
					break;
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
				case FULFILLMENT_CENTER:
					enemyBuiltDrones = true;
					break;
				case DELIVERY_DRONE:
					enemyBuiltDrones = true;
					enemyDroneSpotted = true;
               int distance = location.distanceSquaredTo(r.location);
               if(distance < droneDist) {
                  droneDist = distance;
               }
               nearestEDrone = r;
					break;
				case MINER:
					enemySpotted = true;
					if (round < TURTLE_ROUND) {
						if (!rushDetected) {
							rushDetected = true;
							Communications.queueMessage(rc,2,11,r.location.x, r.location.y);
						}

					}
					break;
				case LANDSCAPER:
					enemySpotted = true;
					if (round < TURTLE_ROUND) {
						if (!rushDetected) {
							rushDetected = true;
							Communications.queueMessage(rc,2,11,r.location.x, r.location.y);
						}

					}
					break;
				case HQ:
					//Notify other units of enemy HQ location
					break;
				default:

					break;
				}
			}
		}

		//Calculate Random
		random = (A*random+B)%256;

		if(/*droneDetected && */netgunDistance > 5 && droneDist <=13) {  //not needed, since if no drones, default drone distance is 100
			escape();
			return;
		}
		else if(droneDist <= 8) {
			escape();
			return;
		}

		MapLocation ml;
		int rSq = senseRadiusSq;
		int radius = (int)(Math.sqrt(rSq));
		ml = null;
		int dx;
		int dy;
		int rad0 = -1;
		int rad;
		//int bytecode1 = Clock.getBytecodesLeft();
		int totalSoup = 0;
		int s;
		nearestSoup = null;
		for (int x = Math.max(0, location.x - radius); x <= Math.min(mapWidth - 1, location.x + radius); x++) {
			for (int y = Math.max(0, location.y - radius); y <= Math.min(mapHeight - 1, location.y + radius); y++) {
				dx = x - location.x;
				dy = y - location.y;
				rad = dx * dx + dy * dy;
				if (rad > rSq) continue;
				ml = new MapLocation(x, y);
				s = rc.senseSoup(ml);
				if (s > 0) {// && !rc.senseFlooding(ml) Removed because some soup on edge is minable even when flooded
					if (rad0 == -1 || rad < rad0) {
						rad0 = rad;
						nearestSoup = ml;
					}
					totalSoup += s;
				}
			}
		}
		//System.out.println("SEARCH bytecodes: " + (bytecode1 - Clock.getBytecodesLeft()));
		if (soupCommunicateCooldown > 0) soupCommunicateCooldown--;

		if (nearestSoup !=null && totalSoup / nearbyMiners > 100 && nearbyMiners < 4) {
			if (soup>1 && soupCommunicateCooldown == 0) {
				Communications.queueMessage(rc, 1, 2, nearestSoup.x, nearestSoup.y);
				soupCommunicateCooldown += 35; //don't communicate soup location for another 20 turns
			}
		}

		if (round >= MOVE_TO_MATRIX_ROUND && returnLoc == hqLocation) {
			returnLoc = null;
			navigatingReturn = false;
		}

		nearestRefinery = null;
		//Calculate return location
		if (refineries.size()>0) {
			ArrayList<MapLocation> refs = refineries;
			int dist = 1000000;
			int rld;
			RobotInfo botInfo;
			MapLocation rl;
			for (int i = refs.size(); i-->0;) {
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
						if (soup>1)Communications.queueMessage(rc, 1, 8, rl.x, rl.y);
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


		/*if (isBuilder) {
			//move towards matrix or hq if far
			if (minerState != MinerState.MOVE_MATRIX && hqDist > 8 && ((robotElevation < round / MOVE_TO_MATRIX_BUILDER && round > TURTLE_ROUND && round < TURTLE_END) || (robotElevation < TURTLE_END / MOVE_TO_MATRIX_BUILDER && round > TURTLE_END))) {
				prevState = minerState;
				minerState = MinerState.MOVE_MATRIX;

			}

			if (minerState == MinerState.MOVE_MATRIX && (robotElevation > round/MOVE_TO_MATRIX_LEVEL || robotElevation > TURTLE_END/MOVE_TO_MATRIX_LEVEL)) {
				minerState = prevState;
			}
		} else {*/
		//move towards matrix or hq if far
		if (minerState != MinerState.MOVE_MATRIX && hqDist > 8 && round > MOVE_TO_MATRIX_ROUND && robotElevation < Utility.MAX_HEIGHT_THRESHOLD) {
			prevState = minerState;
			minerState = MinerState.MOVE_MATRIX;

		}

		if (minerState == MinerState.MOVE_MATRIX && (robotElevation >= Utility.MAX_HEIGHT_THRESHOLD)) {
			minerState = prevState;
		}
		//}

		if (cooldownTurns >= 1) return;

		hqDist = 100;
		if (hqLocation != null) hqDist = Utility.chebyshev(location, hqLocation);

		if (doBuilding()) return;

		//		if (isBuilder) {
		//			if (!builderDS && (rushDetected || (round > TURTLE_ROUND)) && soup > RobotType.DESIGN_SCHOOL.cost) {
		//				
		//				if (builderFC && hqDist > 1 && hqDist < 5) {
		//					//Build Design School
		//
		//					//if (rushDetected) {
		//						Direction[] dirs = Utility.directions;
		//						Direction d;
		//						ml = null;
		//						for (int i = 8; i-->0;) {
		//							d = dirs[i];
		//							ml = location.add(d);
		//							if (initialBuildingTile(ml)) {
		//								if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
		//									rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
		//									dsBuilt = true;
		//									builderDS = true;
		//									return;
		//								}
		//							}
		//						}
		//					//}
		//				} else {
		//					if (hqDist <=2) {
		//						fuzzy(rc, hqLocation.directionTo(location));
		//						return;
		//					} else {
		//						if (Nav.target == null || !Nav.target.equals(hqLocation)) {
		//							Nav.beginNav(rc, this, hqLocation);
		//						}
		//						Nav.nav(rc, this);
		//						return;
		//					}
		//				}
		//			} else {
		//				
		//				//Build Fulfillment Center
		//				if (!builderFC && soup > RobotType.FULFILLMENT_CENTER.cost) {
		//					Direction[] dirs = Utility.directions;
		//					Direction d;
		//					ml = null;
		//					for (int i = 8; i-->0;) {
		//						d = dirs[i];
		//						ml = location.add(d);
		//						if (initialBuildingTile(ml)) {
		//							if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, d)) {
		//								rc.buildRobot(RobotType.FULFILLMENT_CENTER, d);
		//								builderFC = true;
		//								return;
		//							}
		//						}
		//					}
		//				}
		//
		//
		//			//Build Vaporator
		//			if (round < MAX_VAPORATOR_BUILD_ROUND && soup > RobotType.VAPORATOR.cost + VAPORATOR_WEIGHT * numVaporators && hqDist >= 2) {
		//				Direction[] dirs = Utility.directions;
		//				Direction d;
		//				ml = null;
		//				for (int i = 8; i-->0;) {
		//					d = dirs[i];
		//					ml = location.add(d);
		//					if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
		//						if (rc.canBuildRobot(RobotType.VAPORATOR, d)) {
		//							rc.buildRobot(RobotType.VAPORATOR, d);
		//							return;
		//						}
		//					}
		//				}
		//			}
		//			
		//
		//			//Build Netgun
		//			if (builderDS && builderFC && soup > RobotType.NET_GUN.cost && !ngBuilt && enemyDroneSpotted) {
		//				Direction[] dirs = Utility.directions;
		//				Direction d;
		//				ml = null;
		//				for (int i = 8; i-->0;) {
		//					d = dirs[i];
		//					ml = location.add(d);
		//					if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
		//						if (rc.canBuildRobot(RobotType.NET_GUN, d)) {
		//							rc.buildRobot(RobotType.NET_GUN, d);
		//							return;
		//						}
		//					}
		//				}
		//			}
		//			}
		//		}



		//System.out.println("NEAREST REFINERY:"+nearestRefinery);


		//if (soupMine != null)System.out.println("TARGETING: " + soupMine.x + ", " + soupMine.y);
		//if (nearestSoup != null)System.out.println("NEARSOUP: " + nearestSoup.x + ", " + nearestSoup.y);
		//if (Nav.target != null) System.out.println("NAVTARGET: " + Nav.target.x + ", " + Nav.target.y);

		//System.out.println(round);
		//System.out.println(SCOUT_ROUND);

		// Possibly switch state to scouting if this is the first miner built
		if (roundCreated <= 2) { //round == SCOUT_ROUND && 
			/*minerState = MinerState.SCOUTING_ENEMY_HQ;
			isRush = true;
			EnemyHqPossiblePosition[] positions = EnemyHqPossiblePosition.values();
			setScoutEnemyHq(positions[random % positions.length]);*/
			isBuilder = true;
			//System.out.println("I'm the builder!");
		}
		System.out.println(minerState);
		switch (minerState) {
		case MINING:
			//System.out.println("MINING");
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
				//System.out.println("CARRYING " + soupCarrying +" / " + type.soupLimit + " SOUPS");
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

			//if (isBuilder && round < TURTLE_ROUND && hqDist < 2 && round > 20) {
			/*if (soupMine != null && soupMine.distanceSquaredTo(hqLocation) >= 2) {
				if (Nav.target == null || !Nav.target.equals(soupMine)) {
					Nav.beginNav(rc, this, soupMine);
				}
				Nav.nav(rc, this);
				System.out.println("BUILDER MOVING AWAY " + soupMine);
				return;
				} else {*/
			//moveScout(rc);
			//return;
			//}


			//}

			//search for a soup deposit, check optimal soup deposit within radius
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
			//rc.setIndicatorLine(location, soupMine, 100, 0, 255);


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
				if (soupMine != null && (Nav.target == null || !Nav.target.equals(soupMine))) {
					Nav.beginNav(rc, this,soupMine);
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
			//System.out.println("RETURNING");
			if (returnLoc != null ) {
				if (returnLoc.equals(hqLocation)) {
					Direction[] dirs = Utility.directions;
					MapLocation hqAdjacent;
					boolean allFilled = true;
					RobotInfo botInf;
					int elev;
					for (int i = 8; i-->0;) {
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
					//sit and wait
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


		}
	}

   private void escape() throws GameActionException {  //run towards nearest netgun (only trigger if dsquare distance to nearest netgun is greater than 5)
      //System.out.println("Running...");
      if(nearestNetgun != null) {
         if(location.add(location.directionTo(nearestNetgun)).distanceSquaredTo(nearestEDrone.location) >= 8) {
            if(Nav.target == null || !Nav.target.equals(nearestNetgun)) {
               Nav.beginNav(rc, this, nearestNetgun);
            }
            Nav.nav(rc, this);
            return;
         }
      }
      else {
         if(location.add(location.directionTo(hqLocation)).distanceSquaredTo(nearestEDrone.location) >= 8) {
            if(Nav.target == null || !Nav.target.equals(hqLocation)) {
               Nav.beginNav(rc, this, hqLocation);
            }
            Nav.nav(rc, this);
            return;
         }
      }
      fuzzy(rc, nearestEDrone.location.directionTo(location));
      System.out.println("Running with fuzzy");
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

   
	public void tryBuildFC() throws GameActionException {
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

	public void tryBuildDS() throws GameActionException {
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
	
	public void tryBuildVP() throws GameActionException {
		int hqDist = location.distanceSquaredTo(hqLocation);
		if (hqDist <= 4) {
			Direction d = location.directionTo(hqLocation);
			if (rc.canBuildRobot(RobotType.VAPORATOR, d)) {
				rc.buildRobot(RobotType.VAPORATOR, d);
				return;
			}
			Direction right = d.rotateRight();
			if (rc.canBuildRobot(RobotType.VAPORATOR, right)) {
				rc.buildRobot(RobotType.VAPORATOR, right);
				return;
			}
			Direction left = d.rotateLeft();
			if (rc.canBuildRobot(RobotType.VAPORATOR, left)) {
				rc.buildRobot(RobotType.VAPORATOR, left);
				return;
			}
		} else if (hqDist <= 8) {
			Direction d = location.directionTo(hqLocation);
			if (rc.canBuildRobot(RobotType.VAPORATOR, d)) {
				rc.buildRobot(RobotType.VAPORATOR, d);
				return;
			}
			Direction right = d.rotateRight();
			MapLocation rl = location.add(right);
			if (rl.distanceSquaredTo(hqLocation) <= 2 && rc.canBuildRobot(RobotType.VAPORATOR, right)) {
				rc.buildRobot(RobotType.VAPORATOR, right);
				return;
			}
			Direction left = d.rotateLeft();
			MapLocation ll = location.add(left);
			if (ll.distanceSquaredTo(hqLocation) <= 2 && rc.canBuildRobot(RobotType.VAPORATOR, left)) {
				rc.buildRobot(RobotType.VAPORATOR, left);
				return;
			}
		}

		if (Nav.target == null || !Nav.target.equals(hqLocation)) {
			Nav.beginNav(rc, this, hqLocation);
		}
		Nav.nav(rc, this);
	}

	public void moveScout(RobotController rc) throws GameActionException {
		if (lastDirection == null) {
			if (hqLocation != null) {
				lastDirection = hqLocation.directionTo(location);
			} else {
				System.out.println("RANDOM WALK ERROR");
				lastDirection = Direction.NORTHEAST;
			}
		}
		int i = random % 100;
		boolean last = false;
		if (i < 50) {
			if (last = tryMove(rc, lastDirection)) return;
		}
		if (i < 65 || last) {
			if (last = tryMove(rc, lastDirection.rotateLeft())) return;
		}
		if (i < 80 || last) {
			if (last = tryMove(rc, lastDirection.rotateRight())) return;
		}
		if (i < 87 || last) {
			if (last = tryMove(rc, lastDirection.rotateLeft().rotateLeft())) return;
		}
		if (i < 94 || last) {
			if (last = tryMove(rc, lastDirection.rotateRight().rotateRight())) return;
		}
		if (i < 97 || last) {
			if (last = tryMove(rc, lastDirection.rotateLeft().rotateLeft().rotateLeft())) return;
		}
		if (i < 100 || last) {
			if (last = tryMove(rc, lastDirection.rotateRight().rotateRight().rotateRight())) return;
		}
		tryMove(rc, lastDirection.rotateLeft().rotateLeft().rotateLeft().rotateLeft());
	}

	public boolean tryMove(RobotController rc, Direction d) throws GameActionException {
		if (canMove(d)) {
			rc.move(d);
			lastDirection = d;
			return true;
		}
		return false;
	}


	/*private void setScoutEnemyHq(EnemyHqPossiblePosition possiblePosition) {
		System.out.println("Set enemy HQ to scout");
		enemyHqScouting = possiblePosition;

		int enemyHqX;
		int enemyHqY;

		int maxXPosition;
		int maxYPosition;

		switch (possiblePosition) {
		case X_FLIP:
			maxXPosition = mapWidth - 1;
			enemyHqX = maxXPosition - hqLocation.x;
			enemyHqY = hqLocation.y;
			break;
		case Y_FLIP:
			maxYPosition = mapHeight - 1;
			enemyHqX = hqLocation.x;
			enemyHqY = maxYPosition - hqLocation.y;
			break;
		case ROTATION:
			maxXPosition = mapWidth - 1;
			maxYPosition = mapHeight - 1;
			enemyHqX = maxXPosition - hqLocation.x;
			enemyHqY = maxYPosition - hqLocation.y;
			break;
		default:
			// There's not even anything left
			// But this prevents an error in IDEA :/
			throw new Error("how did this even happen");
		}

		targetedEnemyHqLocation = new MapLocation(enemyHqX, enemyHqY);
		Nav.beginNav(rc, this, targetedEnemyHqLocation);
	}

	private void scoutEnemyHq() throws GameActionException {
		System.out.println("Scouting enemy HQ");
		Nav.nav(rc, this);

		if (rc.canSenseLocation(targetedEnemyHqLocation)) {
			RobotInfo potentialRobot = rc.senseRobotAtLocation(targetedEnemyHqLocation);
			// Will never sense our HQ because it's not one of the possibilities for enemyHqPossibleLocation
			// Therefore, if it sees any HQ, it's the enemy's
			if (potentialRobot != null && potentialRobot.type == RobotType.HQ) {
				// TODO: Do better setting the cost, ensuring that the message is sent, etc.
				if (soup>3) Communications.queueMessage(rc, 3, 3, targetedEnemyHqLocation.x, targetedEnemyHqLocation.y);

				setRushEnemyHq();
			} else {
				EnemyHqPossiblePosition[] values = EnemyHqPossiblePosition.values();
				setScoutEnemyHq(values[(enemyHqScouting.ordinal() + 1) % values.length]);
			}
		}
	}

	private void setRushEnemyHq() throws GameActionException {
		Nav.beginNav(rc, this, targetedEnemyHqLocation);
		minerState = MinerState.RUSHING_ENEMY_HQ;
	}*/

	/**
	 * Should place a design school adjacent to the enemy HQ, then switch to looking for soup.
	 */
	/*private void rushEnemyHq() throws GameActionException {
		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation ml;

		for (int i = 8; i-- > 0; ) {
			d = dirs[i];
			ml = location.add(d);

			if (!ml.equals(targetedEnemyHqLocation) && ml.isAdjacentTo(targetedEnemyHqLocation) && rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
				rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
				setDroneNettingEnemyHq();
				return;
			}
		}

		Nav.nav(rc, this);
	}

	private void setDroneNettingEnemyHq() {
		Nav.beginNav(rc, this, targetedEnemyHqLocation);
		minerState = MinerState.DRONE_NETTING_ENEMY_HQ;
	}

	private void droneNetEnemyHq() throws GameActionException {
		if (enemyBuiltDrones) {
			Direction[] dirs = Utility.directions;
			Direction d;
			MapLocation ml;

			for (int i = 8; i-- > 0; ) {
				d = dirs[i];
				ml = location.add(d);

				if (Utility.chebyshev(ml, targetedEnemyHqLocation) < 3 && rc.canBuildRobot(RobotType.NET_GUN, d)) {
					rc.buildRobot(RobotType.NET_GUN, d);
					minerState = MinerState.SEEKING;
					return;
				}

				//System.out.println("Can't build here: ");
			}
		}
		if (location.isWithinDistanceSquared(targetedEnemyHqLocation, 2)) {
		Nav.nav(rc, this);
		}
	}*/

	@Override
	public void processMessage(int m, int x, int y) {
		switch (m) {
		case 1:
			hqLocation = new MapLocation(x,y);
			//System.out.println("Recieved HQ location: " + x + ", " + y);
			break;
		case 2:
			if (soupMine == null) {

				if (!isBuilder) {
					soupMine = new MapLocation(x, y);
					Nav.beginNav(rc, this, soupMine);
				}
			}
			//System.out.println("Recieved soup location: " + x + ", " + y);
			break;
		case 5:
			MapLocation ml5 = new MapLocation(x,y);
			//System.out.println("Recieved Refinery location: " + x + ", " + y);
			if (!refineries.contains(ml5)) refineries.add(ml5);
			break;
		case 6:
			designSchoolBuildCooldown = 50;
			break;
		case 8:
			MapLocation ml8 = new MapLocation(x, y);
			refineries.remove(ml8);
			if (returnLoc != null && returnLoc.equals(ml8)) {
				returnLoc = null;
				navigatingReturn = false;
			}
			break;
		case 11:
			rushDetected = true;
			break;
		case 15:
			MapLocation ml15 = new MapLocation(x,y);
			if (frontLocation == null || Utility.chebyshev(location, ml15) < Utility.chebyshev(location, frontLocation)) {
				frontLocation = ml15;
			}
			break;
		}

	}

	@Override
	public boolean canMove(Direction d) throws GameActionException {
		MapLocation ml = rc.adjacentLocation(d);
		if (robotElevation >= Utility.MAX_HEIGHT_THRESHOLD && pathTile(location)) {
			return rc.canMove(d) && !rc.senseFlooding(ml) && pathTile(ml) && rc.senseElevation(ml) >= Utility.MAX_HEIGHT_THRESHOLD;
		}
		return rc.canMove(d) && !rc.senseFlooding(ml) && (minerState != MinerState.MOVE_MATRIX || pathTile(ml));
	}

	public boolean doBuilding() throws GameActionException {
		if (round < TURTLE_ROUND) {
			if (!builderFC && soup > RobotType.FULFILLMENT_CENTER.cost) {
				System.out.println("TryFC");
				tryBuildFC();
				return true;
			} else if (!builderDS && (rushDetected || round > TURTLE_ROUND || isFreeFriendlyDrone) && soup > RobotType.DESIGN_SCHOOL.cost) {
				System.out.println("TryDS");
				tryBuildDS();
				return true;
			} else if (builderFC && builderDS && isFreeHQSpot && soup > RobotType.VAPORATOR.cost) {
				//tryBuildVP();
				//return true;
				if (isFreeHQSpot) {
					int numWalls = 0;
					if (hqLocation.x == 0 || hqLocation.x == mapWidth - 1) {
						numWalls += 1;
					}
					if (hqLocation.y == 0 || hqLocation.y == mapHeight - 1) {
						numWalls += 1;
					}
					if (numWalls == 0) {
						if (wallVaporators < 5) {
							tryBuildVP();
							return true;
						}
					}
					if (numWalls == 1) {
						if (wallVaporators < 2) {
							tryBuildVP();
							return true;
						}
					}
					isFreeHQSpot = false;
				}
			}
		}

		MapLocation ml;

		boolean canBuild = true;
		if (robotElevation < Utility.MAX_HEIGHT_THRESHOLD || location.isWithinDistanceSquared(hqLocation, 8)) canBuild = false;


		if (minerState != MinerState.MOVE_MATRIX) {
			//Build Refinery
			//System.out.println("IRN:"+isRefineryNearby+", nearestRefinery:"+nearestRefinery+", nearestSoup:"+nearestSoup);
			if (!isRefineryNearby && soup > RobotType.REFINERY.cost) {// && (round > CLOSE_TURTLE_END || !hqAvailable)
				if ((nearestRefinery == null || location.distanceSquaredTo(nearestRefinery) >= DISTANCE_REFINERY_THRESHOLD) && nearestSoup != null && location.distanceSquaredTo(nearestSoup) <= DISTANCE_SOUP_THRESHOLD) {
					Direction[] dirs = Utility.directions;
					Direction d;
					MapLocation refLoc;
					for (int i = dirs.length; --i >= 0;) {
						d = dirs[i];
						refLoc = location.add(d);
						if (rc.canBuildRobot(RobotType.REFINERY, d) && Utility.chebyshev(refLoc, hqLocation) > 3) {
							rc.buildRobot(RobotType.REFINERY, d);
							MapLocation refineryLoc = location.add(d);
							if (soup>3) Communications.queueMessage(rc, 3, 5, refineryLoc.x, refineryLoc.y);
							refineries.add(refineryLoc);
							return true;
						}
					}
				}
			}

			if (canBuild) {
				if (enemyHqLocation != null && location.distanceSquaredTo(enemyHqLocation) <= ENEMY_HQ_RANGE) {
					//Build Netgun
					if (soup > RobotType.NET_GUN.cost && enemyDroneSpotted && hqDist >= 2) {
						Direction[] dirs = Utility.directions;
						Direction d;
						ml = null;
						for (int i = 8; i-->0;) {
							d = dirs[i];
							ml = location.add(d);
							
							if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
								if (rc.canBuildRobot(RobotType.NET_GUN, d)) {
									if (rc.senseElevation(ml) < Utility.MAX_HEIGHT_THRESHOLD && !ml.isWithinDistanceSquared(hqLocation, 8)) continue;
									rc.buildRobot(RobotType.NET_GUN, d);
									return true;
								}
							}
						}
					}
					return false;
				}
				//Build Design School
				if (designSchoolBuildCooldown > 0)designSchoolBuildCooldown--;
				if (dsBuilt) turnsSinceDesignSchoolSeen = 0;
				else turnsSinceDesignSchoolSeen++;
				if (round > TURTLE_ROUND && !dsBuilt && designSchoolBuildCooldown == 0 && soup > DESIGN_SCHOOL_WEIGHT && hqLocation != null && hqDist>=2) {
					Direction[] dirs = Utility.directions;
					Direction d;
					ml = null;
					for (int i = 8; i-->0;) {
						d = dirs[i];
						ml = location.add(d);
						
						if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
							if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
								if (rc.senseElevation(ml) < Utility.MAX_HEIGHT_THRESHOLD) continue;
								rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
								dsBuilt = true;
								return true;
							}
						}
					}
				}

				//Build Vaporator
				if ((!builderVP || soup > RobotType.VAPORATOR.cost + VAPORATOR_WEIGHT * numVaporators) && round < MAX_VAPORATOR_BUILD_ROUND && hqDist >= 2) {
					Direction[] dirs = Utility.directions;
					Direction d;
					ml = null;
					for (int i = 8; i-->0;) {
						d = dirs[i];
						ml = location.add(d);
						
						if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
							if (rc.canBuildRobot(RobotType.VAPORATOR, d)) {
								if (rc.senseElevation(ml) < Utility.MAX_HEIGHT_THRESHOLD) continue;
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
					ml = null;
					for (int i = 8; i-->0;) {
						d = dirs[i];
						ml = location.add(d);
						
						if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
							if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, d)) {
								if (rc.senseElevation(ml) < Utility.MAX_HEIGHT_THRESHOLD) continue;
								rc.buildRobot(RobotType.FULFILLMENT_CENTER, d);
								return true;
							}
						}
					}
				}
				
				if (soup > RobotType.NET_GUN.cost && (soup > 800 || enemyDroneSpotted) && !ngBuilt) {
					Direction[] dirs = Utility.directions;
					Direction d;
					ml = null;
					for (int i = 8; i-->0;) {
						d = dirs[i];
						ml = location.add(d);
						
						if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
							if (rc.canBuildRobot(RobotType.NET_GUN, d)) {
								if (rc.senseElevation(ml) < Utility.MAX_HEIGHT_THRESHOLD && !ml.isWithinDistanceSquared(hqLocation, 8)) continue;
								rc.buildRobot(RobotType.NET_GUN, d);
								return true;
							}
						}
					}
				}
				return false;
			}
		}
		return false;
	}



}

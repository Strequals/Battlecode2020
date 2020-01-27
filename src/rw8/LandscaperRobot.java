package rw8;

import battlecode.common.*;

import java.util.ArrayList;

import static rw8.Utility.TERRAFORM_THRESHOLD;

public strictfp class LandscaperRobot extends Robot {

	private enum LandscaperState {
		DEFENSE,
		ASSAULTING_HQ,
		TERRAFORMING,
		STATE_MACHINES_ARE_BAD
	}

	private LandscaperState state;

	// DEFENSE SECTION
	// Relevant only while state == DEFENSE
	private enum DefenseState {
		TURTLE,
		BURY_RUSH_BUILDINGS,
		UNBURYING_HQ
	}
	private DefenseState defenseState = DefenseState.TURTLE;

	// TERRAFORMING SECTION
	// Relevant only while state == TERRAFORMING
	private enum TerraformingState {
		NORMAL,
		HELP_MINER_UP,
		ESCAPE,
		TARGET_ENEMY_BUILDING
	}
	private TerraformingState terraformingState = TerraformingState.NORMAL;
	// When terraformingState == HELP_MINER_UP, this is the location of the miner to help
	private MapLocation minerToHelp;

	private MapLocation homeDsLocation;
	private Direction pitDirection;
	private MapLocation enemyHqLocation;
	private boolean isDroneThreat;
	private boolean rushDigging;
	private MapLocation targetBuildingLocation;
	private int hqElevation;
	private MapLocation nearestFillTile;
	private MapLocation backupFill;
	public WeightedMapLocation hqFill;
	private MapLocation nearestNetgun;
	//private int netgunDistance = 10000; needs to reset every loop
	private RobotInfo nearestEDrone;
	private boolean terraformedToEnemyHQ;

	private int turnsNavedHq;
	private int nearbyTerraformers = 0;
	private int communicationDelay;
	private MapLocation diagonalTarget;
	private MapLocation stuckLocation;
	private int turnsStuck = 0;

	private ArrayList<MapLocation> enemyDrones;

	public static final int TERRAFORM_TOWARDS_ENEMY_ROUND = 550;
	public static final int FINAL_HQ_DISTANCE = 64;
	public static final int RUN_AWAY_DISTANCE = 3;


	LandscaperRobot(RobotController rc) throws GameActionException {
		super(rc);

		// Process nearby robots to get probable home design school
		RobotInfo[] ri = rc.senseNearbyRobots(2, team);
		RobotInfo r;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getType() == RobotType.DESIGN_SCHOOL && location.isAdjacentTo(r.getLocation())) {
				homeDsLocation = r.getLocation();
				break;
			}
		}

		rushDigging = true;
		turnsNavedHq = 0;
		communicationDelay = 0;
		enemyDrones = new ArrayList<MapLocation>();

		state = getInitialState();
	}

	@Override
	public void run() throws GameActionException {
		state = getNextState();

		if (round == roundCreated) {
			diagonalTarget = new MapLocation(mapWidth-hqLocation.x-1,mapHeight-hqLocation.y-1);
		}

		System.out.println("State: " + state);

		if (rc.isReady()) {
			handleState();
		}
	}

	private LandscaperState getInitialState() {
		//        if (isInitialBuildingTile(homeDsLocation)) {
		//            return LandscaperState.DEFENSE;
		//        } else {
		return LandscaperState.TERRAFORMING;
		//        }
	}

	private LandscaperState getNextState() throws GameActionException {
		int droneDist = 100;
		int netgunDistance = 10000;

		Direction[] dirs = Utility.directionsC;
		MapLocation ml;
		Direction d;
		pitDirection = null;
		for (int i = 9; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			if (pitTile(ml)) {
				if (rc.canDigDirt(d)) {
					pitDirection = d;
					break;
				}
			}
		}

		isDroneThreat = false;

		int targetBuildingDistance = 1000000;
		targetBuildingLocation = null;
		nearbyTerraformers = 0;
		boolean rushDetected = false;

		terraformingState = TerraformingState.NORMAL;
		enemyDrones.clear();

		// Process nearby robots
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				// Friendly Units
				switch (r.getType()) {
				case HQ:
					hqLocation = r.location;
					hqElevation = rc.senseElevation(hqLocation);
					if (location.isAdjacentTo(hqLocation) && r.dirtCarrying > 0) {
						defenseState = DefenseState.UNBURYING_HQ;
						return LandscaperState.DEFENSE;
					}
					break;
				case LANDSCAPER:
					if (Utility.chebyshev(r.location, hqLocation) > 2) {
						nearbyTerraformers++;
					}
					break;
				case NET_GUN:
					int distance = location.distanceSquaredTo(r.location);
					if (distance <= netgunDistance) {
						// Won't update if the nearest net gun isn't in visual range, but not a problem
						nearestNetgun = r.location;
						netgunDistance = distance;
					}
					break;
				case MINER:
					// Build the miner up to the matrix
					// Doesn't do much if we don't ultimately decide to terraform this turn
					if (r.location.isWithinDistanceSquared(location, 2) && pathTile(r.location)) {
						int elev = rc.senseElevation(r.location);
						if (elev < Utility.MAX_HEIGHT_THRESHOLD) {
							if (pitDirection != null) {
								minerToHelp = r.location;
								terraformingState = TerraformingState.HELP_MINER_UP;

							}
						}
					}
					break;
				case DESIGN_SCHOOL:
					if (homeDsLocation == null) {
						homeDsLocation = r.location;
					}
					break;
				}
			} else if (r.getTeam() != Team.NEUTRAL) {
				// Enemy Units
				int distance;
				switch (r.getType()) {
				case DELIVERY_DRONE:
					isDroneThreat = true;
					distance = Utility.chebyshev(location, r.location); //distance squared has enough numbers to differentiate, and i can't get it to work on chebyevgfaevg distance for some reason
					if (distance < droneDist) {
						droneDist = distance;
						nearestEDrone = r;
					}
					enemyDrones.add(r.location);
					break;

				case NET_GUN:
				case FULFILLMENT_CENTER:
				case VAPORATOR:
				case REFINERY:
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						//if (!r.location.isWithinDistanceSquared(hqLocation, 8)) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						terraformingState = TerraformingState.TARGET_ENEMY_BUILDING;
						//}

					}
					break;
				case HQ:
					enemyHqLocation = r.location;
					int dist = Utility.chebyshev(location,  r.location);
					if (dist <= 1) {
						return LandscaperState.ASSAULTING_HQ;
					} else {
						if (round < TURTLE_END || !terraformedToEnemyHQ) {
							distance = Utility.chebyshev(r.location, location);
							if (distance < targetBuildingDistance) {
								targetBuildingDistance = distance;
								targetBuildingLocation = r.location;
								terraformingState = TerraformingState.TARGET_ENEMY_BUILDING;
							}
						}
					}
					
					break;
				case DESIGN_SCHOOL:
					distance = Utility.chebyshev(r.location, location);
					if (r.location.isWithinDistanceSquared(hqLocation, 8)) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						rushDetected = true;
						defenseState = DefenseState.BURY_RUSH_BUILDINGS;
						return LandscaperState.DEFENSE;
					}
					if (distance < targetBuildingDistance) {

						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						terraformingState = TerraformingState.TARGET_ENEMY_BUILDING;


					}

					break;
				case LANDSCAPER:
					if (Utility.chebyshev(r.location, hqLocation)<=4) {
						rushDetected = true;
					}
					break;
				}
			}
		}

		if (turnsNavedHq >= 50) {
			state = LandscaperState.TERRAFORMING;
		}
		else if (targetBuildingLocation == null && !rushDetected) {
			state = LandscaperState.TERRAFORMING;
		}

		switch (state) {
		case DEFENSE:
			if (targetBuildingLocation != null) {
				defenseState = DefenseState.BURY_RUSH_BUILDINGS;
			}
			//else {
			//    state = LandscaperState.TERRAFORMING;
			//}
			//break;
		case TERRAFORMING:

			// 			if ((netgunDistance > 5 && droneDist <= 3) || droneDist <= 1) {
			// 				//TODO: whatever goes here
			// 			} else 
			if (droneDist <= RUN_AWAY_DISTANCE) {//(netgunDistance > 5 && droneDist <= 13) || 
				if (escape()) return LandscaperState.STATE_MACHINES_ARE_BAD;
			}
		}

		return state;
	}

	private void handleState() throws GameActionException {
		System.out.println(state);
		switch (state) {
		case DEFENSE:
			System.out.println(defenseState);
			defend();
			break;
		case ASSAULTING_HQ:
			doAssault();
			break;
		case TERRAFORMING:
			System.out.println(terraformingState);
			doTerraforming();
			break;
		}
	}

	private void defend() throws GameActionException {

		int rank = Utility.chebyshev(location, hqLocation);

		switch (defenseState) {
		case BURY_RUSH_BUILDINGS:
			if (location.isAdjacentTo(targetBuildingLocation)) {
				// Next to target
				if (dirtCarrying > 0) {
					Direction d = location.directionTo(targetBuildingLocation);
					rc.depositDirt(d);
					break;
				} else if (pitDirection != null) {
					rc.digDirt(pitDirection);
					break;
				} else {
					rc.digDirt(Direction.CENTER);
				}
			}

			if (Nav.target == null || !Nav.target.equals(targetBuildingLocation)) {
				Nav.beginNav(rc, this, targetBuildingLocation);
			}
			Nav.nav(rc, this);
			break;
		case UNBURYING_HQ:
			if (dirtCarrying < RobotType.LANDSCAPER.dirtLimit) {
				rc.digDirt(location.directionTo(hqLocation));
			} else {
				rc.depositDirt(Direction.CENTER);
			}
			break;
		case TURTLE:
			Direction[] dirs = Utility.directions;
			boolean allFilled = true;
			RobotInfo ri;
			int elev;
			int lowElev = 10000;
			MapLocation lowLoc = null;
			boolean adjacentLow = false;
			MapLocation flooded = null;

			MapLocation hqAdjacent;
			for (int i = 8; i-->0;) {
				hqAdjacent = hqLocation.add(dirs[i]);

				if (!rc.onTheMap(hqAdjacent)) continue;

				if (!(rc.canSenseLocation(hqAdjacent))) {
					allFilled = false;
					continue;
				}

				if (hqAdjacent.x == 0 && (hqLocation.y == hqAdjacent.y || (hqLocation.y == 1 && hqLocation.y > hqAdjacent.y) || (mapHeight - hqLocation.y == 2 && hqLocation.y < hqAdjacent.y))) {
					continue;
				}
				if (hqAdjacent.y == 0 && (hqLocation.x == hqAdjacent.x || (hqLocation.x == 1 && hqLocation.x > hqAdjacent.x) || (mapHeight - hqLocation.x == 2 && hqLocation.x < hqAdjacent.x))) {
					continue;
				}
				if (rc.senseFlooding(hqAdjacent)) {
					flooded = hqAdjacent;
				}

				if (rank == 1) {
					elev = rc.senseElevation(hqAdjacent);
					if (elev < lowElev) {
						lowElev = elev;
						lowLoc = hqAdjacent;
						if (elev < hqElevation+1) {
							if (location.isAdjacentTo(hqAdjacent)) {
								adjacentLow = true;
							}
						}
					}
				}

				if (allFilled) {
					ri = rc.senseRobotAtLocation(hqAdjacent);
					if (ri == null || ri.team != team || ri.type != RobotType.LANDSCAPER) {
						allFilled = false;
					}
				}

			}

			if (location.distanceSquaredTo(hqLocation)>2) {
				if (!allFilled) {
					//TODO: check if can tunnel/bridge
					if (Nav.target == null || !Nav.target.equals(hqLocation)) {
						Nav.beginNav(rc, this, hqLocation);
					}
					Nav.nav(rc, this);
					turnsNavedHq++;
				}

				moveTerraform(hqLocation);
				return;

			}

			if (flooded != null) {
				Direction d = location.directionTo(flooded);
				if (dirtCarrying == 0) {
					if (pitDirection != null) {
						rc.digDirt(pitDirection);
						return;
					}
				} else if (rc.canDepositDirt(d)) {
					rc.depositDirt(d);
					return;
				}
			}

			if (round > TURTLE_END && !allFilled && (lowLoc == null || !lowLoc.isAdjacentTo(location))) {
				//System.out.println("All filled: " + allFilled);
				//check if can move or bridge to lower wall location
				if (lowLoc != null && robotElevation - lowElev > GameConstants.MAX_DIRT_DIFFERENCE) {
					//System.out.println("TURTLEMOVING to "+lowLoc);
					moveTurtle(lowLoc);
					return;
				}
			}

			if (dirtCarrying == 0) {
				if (pitDirection != null) rc.digDirt(pitDirection);
			} else {
				if (!allFilled && round < TURTLE_END && !adjacentLow) {// && (round < TURTLE_ROUND || !location.isAdjacentTo(lowLoc))
					//empty spots so leave them for our landscaperes
					//if the lowest is too low, add to it so landscapers can move onto it
					//System.out.println("CENTER FILLING");
					if (rc.canDepositDirt(Direction.CENTER)) {
						rc.depositDirt(Direction.CENTER);
					}
				} else {
					//System.out.println("LOW FILLING");
					dirs = Utility.directionsC;
					MapLocation ml;
					Direction d;
					Direction lowDir = null;
					int lowE = 1000000;
					int e;
					for (int i = 8; i-->0;) {
						d = dirs[i];
						ml = location.add(d);
						if (!(Utility.chebyshev(ml, hqLocation) == 1)) {
							continue;
						}
						if (rc.canSenseLocation(ml)) {
							e = rc.senseElevation(ml);
							if (e < lowE) {
								lowDir = d;
								lowE = e;
							}
						}
					}

					if (lowDir != null) {
						rc.depositDirt(lowDir);
					}
				}

			}
			break;
		}
	}

	private void moveTurtle(MapLocation ml) throws GameActionException {
		// If there is a nearby path location that is not traversable, make it traversable
		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation m;
		long elev;
		long elevDiff;
		for (int i = 8; i-->0; ) {
			d = dirs[i];
			m = location.add(d);
			if (Utility.chebyshev(m, hqLocation) != 1) continue;
			if (rc.canSenseLocation(m)) {
				elev = rc.senseElevation(m);
				elevDiff = (long)(robotElevation) - elev;
				if (elevDiff > TERRAFORM_THRESHOLD) continue;
				if (elevDiff < 0) elevDiff = -elevDiff;
				if (elevDiff > GameConstants.MAX_DIRT_DIFFERENCE) {
					tunnelOrBridge(m, d);
					return;
				}
			}
		}

		//Move towards location
		if (!ml.equals(Nav.target)) {
			Nav.beginNav(rc, this, ml);
		}
		Nav.nav(rc, this);
	}

	/**
	 * Moves towards the given MapLocation, filling in tiles along the way. 
	 * @param ml The location to terraform towards
	 * @return true if a tile is filled, false otherwise.
	 * @throws GameActionException
	 */
	private boolean moveTerraform(MapLocation ml) throws GameActionException {
		// If there is a nearby path location that is not traversable, make it traversable
		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation m;
		long elev;
		long elevDiff;
		RobotInfo botInfo;
		int rank;
		int buildTo = Math.max(robotElevation - GameConstants.MAX_DIRT_DIFFERENCE, Utility.MAX_HEIGHT_THRESHOLD);
		int digTo = Math.max(robotElevation + GameConstants.MAX_DIRT_DIFFERENCE, Utility.MAX_HEIGHT_THRESHOLD);
		MapLocation digLoc;
		for (int i = 8; i-->0; ) {
			d = dirs[i];
			m = location.add(d);
			rank = Utility.chebyshev(m, hqLocation);
			if (rc.canSenseLocation(m)) {
				if (rank <= 2 && rc.senseFlooding(m)) {
					if (pitDirection != null) tunnelOrBridge(m,d);
				}
				if (!pathTile(m) || rank <= 2) continue;

				elev = rc.senseElevation(m);
				elevDiff = (long)(robotElevation) - elev;
				if (elevDiff > TERRAFORM_THRESHOLD) continue;
				if (elev < buildTo) {
					//System.out.println("TUNNEL BRIDGE:" + m);
					botInfo = rc.senseRobotAtLocation(m);
					if (botInfo != null && botInfo.team == team && botInfo.type.isBuilding()) continue;
					if (tunnelOrBridge(m, d)) {
						return true;
					}
					//System.out.println("TBMT-"+d);
					
				}
				else if (elev > digTo) {
					digLoc = m;
				}
			}
		}
		//System.out.println("PATHINGMT");
		// Move towards location
		if (!ml.equals(GridNav.target)) {
			GridNav.beginNav(this, ml);
		}
		GridNav.nav(rc, this);
		return false;
	}

	private void moveWall(MapLocation ml) throws GameActionException {
		/*Direction[] dirs = Utility.directionsC;
        Direction d;
        MapLocation m;
        long elev;
        int rank;
        Direction low = null;
        long lowElev = 100;
        Direction high = null;
        long highElev = -100;
        RobotInfo botInfo;
        for (int i = 9; i-->0; ) {
            d = dirs[i];
            m = location.add(d);
            rank = Utility.chebyshev(m, hqLocation);
            if (rank <= 2 && m.isWithinDistanceSquared(m, senseRadiusSq)) {
            	botInfo = rc.senseRobotAtLocation(m);
            	if (botInfo != null && botInfo.type.isBuilding() && botInfo.team == team) continue;

            		elev = rc.senseElevation(m);

            		if (rc.senseFlooding(m)) elev -= 100;
            		//if (elev > hqElevation + GameConstants.MAX_DIRT_DIFFERENCE) {
            			//if (dirtCarrying < RobotType.LANDSCAPER.dirtLimit) {
            				if (elev > highElev && rc.canMove(d)) {
            					high = d;
            					highElev = elev;

            				}
            			//}
            		//} else if (elev < hqElevation - GameConstants.MAX_DIRT_DIFFERENCE || rc.senseFlooding(m)) {
            			//if (dirtCarrying > 0) {
            				if (elev < lowElev && rc.canMove(d)) {
            					low = d;
            					lowElev = elev;

            				}
            			//}
            		//}
            }
        }

        if (lowElev + GameConstants.MAX_DIRT_DIFFERENCE < hqElevation || lowElev + GameConstants.MAX_DIRT_DIFFERENCE < robotElevation) {
        	if (dirtCarrying > 0) {
        		rc.depositDirt(low);
        		return;
        	} else {
        		if (highElev > hqElevation) {
        			rc.digDirt(high);
        			return;
        		}
        	}

        }
        if (highElev - GameConstants.MAX_DIRT_DIFFERENCE > hqElevation || highElev - GameConstants.MAX_DIRT_DIFFERENCE > robotElevation) {
        	if (dirtCarrying < RobotType.LANDSCAPER.dirtLimit) {
        		rc.digDirt(high);
        		return;
        	} else {
        		if (lowElev < hqElevation) {
        			rc.depositDirt(low);
        			return;
        		}
        	}
        }*/


		// Move towards location
		if (!ml.equals(GridNav.target)) {
			GridNav.beginNav(this, ml);
		}
		GridNav.nav(rc, this);
	}

	private boolean tunnelOrBridge(MapLocation ml, Direction d) throws GameActionException {
		int elTarget = rc.senseElevation(ml);
		int elDistance = elTarget - robotElevation;
		if (rc.senseFlooding(ml)) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
				return true;
			} else {
				if (pitDirection != null) {
					rc.digDirt(pitDirection);
					return true;
				}
			}
		} else if (elDistance > 0) {
			if (dirtCarrying > 0) {
				if (pitDirection != null) {
					rc.depositDirt(pitDirection);
					return true;
				}
			} else {
				rc.digDirt(d);
				return true;
			}
		} else if (elDistance < 0) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
				return true;
			} else {
				if (pitDirection != null) {
					rc.digDirt(pitDirection);
					return true;
				}
			}
		}
		return false;
	}

	private void doTerraforming() throws GameActionException {
		System.out.println("Terraforming state: " + terraformingState);
		System.out.println("HQ Fill: " + hqFill);
		if(stuckLocation == null || !stuckLocation.equals(location)) {
			stuckLocation = location;
			turnsStuck = 0;
		}
		turnsStuck++;
		if(turnsStuck > 100 && enemyHqLocation != null && location.isWithinDistanceSquared(enemyHqLocation, 64)) {
			rc.disintegrate();
		}
		
		if (enemyHqLocation != null && enemyHqLocation.isWithinDistanceSquared(enemyHqLocation, 35)) {
			terraformedToEnemyHQ = true;
			
		}
		
		int csDist;
		switch (terraformingState) {
		case HELP_MINER_UP:
			if (dirtCarrying == 0) {
				rc.digDirt(pitDirection);
				return;
			} else {
				Direction dMiner = location.directionTo(minerToHelp);
				if (rc.canDepositDirt(dMiner)) {
					rc.depositDirt(dMiner);
					return;
				}
			}
			break;
		case ESCAPE:
			escape();
			break;
		case TARGET_ENEMY_BUILDING:
			//System.out.println("targeting Building at " + targetBuildingLocation);
			csDist = Utility.chebyshev(location, targetBuildingLocation);
			if (csDist <= 1) {
				if (dirtCarrying == 0) {
					if (pitDirection != null) {
						rc.digDirt(pitDirection);
					} else {
						rc.digDirt(Direction.CENTER);
					}
				} else {
					rc.depositDirt(location.directionTo(targetBuildingLocation));
				}

			} else {
				if (location.distanceSquaredTo(hqLocation) > 8) {
					moveTerraform(targetBuildingLocation);
				} else {
					moveWall(targetBuildingLocation);
				}

			}
			break;
		case NORMAL:
			if (nearestFillTile != null) {
				// Check if still needs filling
				if (rc.canSenseLocation(nearestFillTile)) {
					int elev = rc.senseElevation(nearestFillTile);
					if (elev >= Utility.MAX_HEIGHT_THRESHOLD) {
						nearestFillTile = null;
					} else {
						RobotInfo ri = rc.senseRobotAtLocation(nearestFillTile);
						if (ri != null && ri.team == team && ri.type.isBuilding()) {
							nearestFillTile = null;
						}
					}
				}
			}

			if (hqFill != null) {
				// Check if still needs filling
				MapLocation hqFillLoc = hqFill.mapLocation;
				if (rc.canSenseLocation(hqFillLoc)) {
					int elev = rc.senseElevation(hqFillLoc);
					if (elev >= Utility.MAX_HEIGHT_THRESHOLD) {
						hqFill = null;
					} else {
						RobotInfo ri = rc.senseRobotAtLocation(hqFillLoc);
						if (ri != null && ri.team == team && ri.type.isBuilding()) {
							hqFill = null;
						}
					}
				}
			}

			System.out.println("HQ fill at end: " + hqFill);

			// Move off of pit
			if (pitTile(location)) {
				Direction[] dirs = Utility.directions;
				Direction d;
				for (int i = 8; i-->0;) {
					d = dirs[i];
					if (rc.canMove(d)) {
						rc.move(d);
						return;
					}
				}
			}

			if (location.distanceSquaredTo(hqLocation) <= 8) {
				//moveTerraform(diagonalTarget);
				moveWall(diagonalTarget);
				return;
			}

			if (robotElevation < Utility.MAX_HEIGHT_THRESHOLD) {
				if (dirtCarrying == 0) {
					if (pitDirection != null) rc.digDirt(pitDirection);
				} else {
					if (pathTile(location) && robotElevation < Utility.MAX_HEIGHT_THRESHOLD) {
						rc.depositDirt(Direction.CENTER);
						return;
					} else {
						Direction[] dirs = Utility.directions;
						Direction d;
						MapLocation m;
						RobotInfo botInfo;
						int elev;
						for (int i = 8; i-->0; ) {
							d = dirs[i];
							m = location.add(d);

							if (!pathTile(m) || Utility.chebyshev(m, hqLocation) == 1) continue;
							if (rc.canSenseLocation(m)) {
								botInfo = rc.senseRobotAtLocation(m);
								elev = rc.senseElevation(m);
								if ((botInfo != null && botInfo.team == team && botInfo.type.isBuilding()) || elev >= Utility.MAX_HEIGHT_THRESHOLD) continue;
								rc.depositDirt(d);
								return;
							}
						}
					}
				}
			}


			//			if (backupFill != null && Utility.chebyshev(location, backupFill) <= 2) {
			//				backupFill = null;
			//			}
			//
			//			if (fillTarget != null) {
			//				if (rc.canSenseLocation(fillTarget)) {
			//					if (rc.senseElevation(fillTarget) >= Utility.MAX_HEIGHT_THRESHOLD) {
			//						fillTarget = null;
			//					}
			//				}
			//			}
			//
			//			if (fillTarget == null) {
			//				if (backupFill != null) {
			//					fillTarget = backupFill;
			//				}
			//			}
			//
			//			if (fillTarget == null) {
			//				if (enemyHqLocation != null) {
			//					if (moveTerraform(enemyHqLocation)) {
			//						if (communicationDelay == 0) {
			//							if (nearbyTerraformers < Utility.MAX_NEARBY_TERRAFORMERS) {
			//								Communications.queueMessage(rc, 1, 15, location.x, location.y);
			//								communicationDelay = 20*(nearbyTerraformers+1);
			//							}
			//						} else {
			//							if (communicationDelay > 0)communicationDelay--;
			//						}
			//					
			//					}
			//					return;
			//				} else {
			//					System.out.println("enemy hq location unknown, pathing around HQ");
			//					moveTerraform(hqLocation);
			//				}
			//			} else {
			//				if (moveTerraform(fillTarget)) {
			//					if (communicationDelay == 0 && Utility.chebyshev(location, fillTarget)==1) {
			//						if (nearbyTerraformers < Utility.MAX_NEARBY_TERRAFORMERS) {
			//							Communications.queueMessage(rc, 1, 15, fillTarget.x, fillTarget.y);
			//							communicationDelay = 20*(nearbyTerraformers+1);
			//						}
			//					} else {
			//						if (communicationDelay > 0)communicationDelay--;
			//					}
			//				}
			//				
			//				return;
			//			}








			if (backupFill != null && Utility.chebyshev(location, backupFill) <= 2) {
				backupFill = null;
			}

			if (nearestFillTile == null) {
				if (hqFill != null) {
					nearestFillTile = hqFill.mapLocation;
					hqFill = null;

				} else {
					// Find tile to fill
					MapLocation ml;
					int rSq = senseRadiusSq;
					int radius = Math.min((int)(Math.sqrt(rSq)), Utility.MAX_TERRAFORM_RANGE);
					int dx;
					int dy;
					int rad;
					long elev;
					long dElev;
					int bestPriority = 100000;
					int priority;
					int rank;
					RobotInfo ri;
					int lowerx=Math.max(0, location.x - radius);
					int upperx=Math.min(mapWidth - 1, location.x + radius);
					int lowery=Math.max(0, location.y - radius);
					int uppery=Math.min(mapHeight - 1, location.y + radius);
					for (int x = lowerx; x <= upperx; x++) {
						for (int y = lowery; y <= uppery; y++) {
							dx = x - location.x;
							dy = y - location.y;
							rad = dx * dx + dy * dy;
							if (rad > rSq) 
								continue;
							ml = new MapLocation(x, y);

							csDist = Utility.chebyshev(ml, location);
							if (!pathTile(ml)) continue;
							elev = rc.senseElevation(ml);

							dElev = (robotElevation) - elev;
							if (dElev > TERRAFORM_THRESHOLD) continue;
							rank = Utility.chebyshev(ml, hqLocation) * 4;
							// Don't create a huge bubble around HQ, just enough to keep floods out
							rank = rank < 7 ? rank : 0;
							if (rank == 1) continue;
							//heuristic
							priority = csDist + rank;
							if (enemyHqLocation != null) {
								priority += Utility.chebyshev(ml, enemyHqLocation) * 2;
							}
							if (dElev < 0) {
								dElev = -dElev;
								priority -= 100; //Prioritize filling in lower tiles rather than digging higher ones
							}
							if (elev >= Utility.MAX_HEIGHT_THRESHOLD) continue;
							if (dElev > 0) {
								if (priority < bestPriority) {
									ri = rc.senseRobotAtLocation(ml);
									if (ri != null && (ri.type.isBuilding() || (ri.team == team && ri.type == RobotType.LANDSCAPER))) continue;
									bestPriority = priority;
									nearestFillTile = ml;
								}

							}
						}
					}
				}
			}

			if (nearestFillTile != null) {
				if (location.isAdjacentTo(nearestFillTile) && Utility.chebyshev(nearestFillTile, hqLocation) == 2) {
					if (dirtCarrying == 0) {
						Direction direction = location.directionTo(hqLocation);
						// TODO: Find alternate direction if this doesn't work
						if (rc.canDigDirt(direction)) {
							rc.digDirt(direction);
							return;
						}
					} else {
						Direction direction = location.directionTo(nearestFillTile);
						if (rc.canDepositDirt(direction)) {
							rc.depositDirt(direction);
							return;
						}
					}
				}

				boolean filled = moveTerraform(nearestFillTile);
				if (filled && pitDirection != null && communicationDelay == 0 && Utility.chebyshev(location, nearestFillTile) == 1) {
					if (nearbyTerraformers < Utility.MAX_NEARBY_TERRAFORMERS) {
						Communications.queueMessage(rc, 1, 15, nearestFillTile.x, nearestFillTile.y);
						communicationDelay = 20 * (nearbyTerraformers + 1);
					}
					if (nearbyTerraformers > 5) {
						Communications.queueMessage(rc, 1, 16, 0, 0);
						communicationDelay = 20 * (nearbyTerraformers + 1);
					}
				} else {
					if (communicationDelay > 0)
						communicationDelay--;
				}

				return;
			}



			// Move towards terraform edge
			if (backupFill != null) {
				moveTerraform(backupFill);
				return;
			}

			//Move towards enemy HQ
			if (enemyHqLocation != null && !location.isWithinDistanceSquared(enemyHqLocation, FINAL_HQ_DISTANCE)) {
				moveTerraform(enemyHqLocation);
				return;
			}

			//pick up dirt so if an assaulter carries to enemy HQ, can begin placing immediately
			if (dirtCarrying < RobotType.LANDSCAPER.dirtLimit) {
				if (pitDirection != null) rc.digDirt(pitDirection);
				return;
			}
			moveTerraform(hqLocation);
			break;
		}
	}

	private boolean escape() throws GameActionException {  //run towards nearest netgun (only trigger if dsquare distance to nearest netgun is greater than 5)
		if(nearestNetgun != null) {
			if(location.add(location.directionTo(nearestNetgun)).distanceSquaredTo(nearestEDrone.location) >= 3) {
				if(Nav.target == null || !Nav.target.equals(nearestNetgun)) {
					Nav.beginNav(rc, this, nearestNetgun);
				}
				Nav.nav(rc, this);
				return true;
			}
		}
		else {
			if(location.add(location.directionTo(hqLocation)).distanceSquaredTo(nearestEDrone.location) >= 3) {
				if(Nav.target == null || !Nav.target.equals(hqLocation)) {
					Nav.beginNav(rc, this, hqLocation);
				}
				Nav.nav(rc, this);
				return true;
			}
		}
		return fuzzy(rc, nearestEDrone.location.directionTo(location));
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

	private int heuristic(MapLocation ml) {
		int k = Utility.chebyshev(ml, location)+Utility.chebyshev(ml, hqLocation);
		if (enemyHqLocation == null || round < TERRAFORM_TOWARDS_ENEMY_ROUND) return k;
		return k + Utility.chebyshev(ml, enemyHqLocation);
	}

	private void doAssault() throws GameActionException {
		if (location.distanceSquaredTo(enemyHqLocation) > 2) {
			if (Nav.target == null || !Nav.target.equals(enemyHqLocation)) {
				Nav.beginNav(rc, this, enemyHqLocation);
			}
			Nav.nav(rc, this);
			return;
		}

		//dig and place in alternating cycles
		if (((dirtCarrying < RobotType.LANDSCAPER.dirtLimit && rushDigging && !isDroneThreat) || dirtCarrying == 0)) {
			rushDigging = true;
			rc.digDirt(Direction.CENTER);
		} else {
			rushDigging = false;
			rc.depositDirt(location.directionTo(enemyHqLocation));
		}

	}

	@Override
	public boolean canMove(Direction d) throws GameActionException {
		MapLocation ml = location.add(d);
		ArrayList<MapLocation> eDrones = enemyDrones;
		MapLocation eDroneLoc;
		for (int i = eDrones.size(); i-->0;) {
			eDroneLoc = eDrones.get(i);
			if (Utility.chebyshev(location, eDroneLoc)<=RUN_AWAY_DISTANCE+1) return false;
		}

		if (location.isWithinDistanceSquared(hqLocation, 8)) {
			return rc.canMove(d) && !rc.senseFlooding(ml) && (hqFill == null || rc.senseElevation(ml) < Utility.MAX_HEIGHT_THRESHOLD);
		}
		else return rc.canMove(d) && pathTile(location.add(d)) && !rc.senseFlooding(ml);



	}

	@Override
	public void processMessage(int m, int x, int y) {
		System.out.println("Message: " + m + " (" + x + ", " + y + ")");
		switch (m) {
			case 1:
				hqLocation = new MapLocation(x, y);
				//System.out.println("Received HQ location: " + x + ", " + y);
				break;
			case 3:
				enemyHqLocation = new MapLocation(x, y);
				break;
			case 15:
				MapLocation ml15 = new MapLocation(x, y);
				System.out.println("Received location: " + ml15);
				//			if (Utility.chebyshev(hqLocation, ml15) <= 6) {
				if (Utility.chebyshev(location, hqLocation) <= 2) {
					diagonalTarget = ml15;
					hqFill = new WeightedMapLocation(ml15, 0);
				} else {

					hqFill = new WeightedMapLocation(ml15, 0);
				}
				//			}

				if (backupFill == null || heuristic(ml15) + Utility.BACKUP_THRESHOLD < heuristic(backupFill)) {
					backupFill = ml15;
				}
				if (Utility.chebyshev(ml15, location) <= 4) {
					communicationDelay += 10;
				}
				//System.out.println("recieved a fill location");
				break;
			case 10:
				MapLocation ml10 = new MapLocation(x, y);
				System.out.println("Received location: " + ml10);
				//			if (Utility.chebyshev(hqLocation, ml15) <= 6) {
				if (Utility.chebyshev(location, hqLocation) <= 2) {
					diagonalTarget = ml10;
					hqFill = new WeightedMapLocation(ml10, 1);
				} else {

					hqFill = new WeightedMapLocation(ml10, 1);
				}
				//			}

				if (backupFill == null || heuristic(ml10) + Utility.BACKUP_THRESHOLD < heuristic(backupFill)) {
					backupFill = ml10;
				}
				if (Utility.chebyshev(ml10, location) <= 4) {
					communicationDelay += 10;
				}
				//System.out.println("recieved a fill location");
				break;
		}
	}
}

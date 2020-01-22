package rw7;

import battlecode.common.*;

public strictfp class LandscaperRobot extends Robot {

	private enum LandscaperState {
		TURTLING,
		ASSAULTING_HQ,
		TERRAFORMING
	}

   private boolean isEnemyRushing;
	// TODO: This probably shouldn't matter. Eliminate it.
	private MapLocation homeDsLocation;
	private MapLocation targetCorner;
	private LandscaperState state;
	private boolean reachedCorner;
	private boolean preferNorth;
	private boolean preferEast;
	private boolean isExpectedLandscaperCount;
	private MapLocation pitLocation;
	private Direction pitDirection;
	private MapLocation enemyHqLocation;
	private boolean isDroneThreat;
	private boolean rushDigging;
	private MapLocation targetBuildingLocation;
	private RobotInfo targetRobot;
	private int hqElevation;
	private MapLocation nearestFillTile;
	private MapLocation backupFill;

	private int turnsNavvedHq;
	private int nearbyTerraformers = 0;
	private int communicationDelay;

	private static final int TERRAFORM_THRESHOLD = 100; //If change in elevation is greater, do not terraform this tile
	private static final int MAX_TERRAFORM_RANGE = 2; //Chebyshev distance to limit
	private static final int TERRAFORM_RANGE_SQ = 8; //2x^2
	private static final int MAX_HEIGHT_THRESHOLD = 12; //don't try to build up to unreachable heights
	private static final int BACKUP_THRESHOLD = 8;
	private static final int MAX_NEARBY_TERRAFORMERS = 5;//Don't communicate location if there are 5 or more terraformers nearby
	
	LandscaperRobot(RobotController rc) throws GameActionException {
		super(rc);

		// HQ location comes from processing first message

		// Process nearby robots to get probable home design school
		RobotInfo[] ri = rc.senseNearbyRobots(2, rc.getTeam());
		RobotInfo r;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			// Friendly Units
			if (r.getType() == RobotType.DESIGN_SCHOOL && rc.getLocation().isAdjacentTo(r.getLocation())) {
				homeDsLocation = r.getLocation();
				break;
			}
		}


		isExpectedLandscaperCount = false;
		reachedCorner = false;
		rushDigging = true;
		turnsNavvedHq = 0;
		communicationDelay = 0;
	}

	@Override
	public void run() throws GameActionException {


		if (round == roundCreated) {
			if (initialBuildingTile(homeDsLocation)) {
				state = LandscaperState.TURTLING;
			} else {
				state = LandscaperState.TERRAFORMING;
			}
		}
		Direction[] dirs = Utility.directionsC;
		MapLocation ml;
		Direction d;


		pitLocation = null;
		for (int i = 9; i-->0;) {
			d = dirs[i];
			ml = location.add(d);
			if (pitTile(ml)) {
				if (rc.canDigDirt(d)) {
					pitLocation = ml;
					pitDirection = d;
					break;
				}
			}
		}

		isEnemyRushing = false;

		isDroneThreat = false;
		// Process nearby robots
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;

		int targetBuildingDistance = 1000000;
		targetBuildingLocation = null;
		targetRobot = null;
		nearbyTerraformers = 0;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				// Friendly Units
				switch (r.getType()) {
				case HQ:
					hqLocation = r.location;
					hqElevation = rc.senseElevation(hqLocation);
					break;
				case LANDSCAPER:
					if (Utility.chebyshev(r.location, hqLocation) > 2) {
						nearbyTerraformers++;
					}
					break;
				case MINER:
					//Build the miner up to the matrix
					if (state == LandscaperState.TERRAFORMING) {
						if (r.location.isWithinDistanceSquared(location, 2) && pathTile(r.location)) {
							int elev = rc.senseElevation(r.location);
							if (elev < TERRAFORM_THRESHOLD) {
							if (elev < robotElevation) {
								if (dirtCarrying == 0 && pitDirection != null) {
									rc.digDirt(pitDirection);
									return;
								}
								if (dirtCarrying > 0) {
									Direction dMiner = location.directionTo(r.location);
									if (rc.canDepositDirt(dMiner)) {
										rc.depositDirt(dMiner);
										return;
									}
								}
							}
							}
						}
					}
					break;
				case DESIGN_SCHOOL:
					if (homeDsLocation == null) homeDsLocation = r.location;
					break;
				}
			} else if (r.getTeam() != Team.NEUTRAL) {
				// Enemy Units
				int distance;
				switch (r.getType()) {
				case DELIVERY_DRONE:
					isDroneThreat = true;

					break;
				case MINER:
					// TODO: Block or bury
					break;
				case LANDSCAPER:
					isEnemyRushing = true;
					break;
				case NET_GUN:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						targetRobot = r;
					}
					break;
				case REFINERY:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						targetRobot = r;
					}
					break;
				case DESIGN_SCHOOL:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						targetRobot = r;
					}
					break;
				case FULFILLMENT_CENTER:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						targetRobot = r;
					}
					break;
				case VAPORATOR:
					// TODO: Bury
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						targetRobot = r;
					}
					break;
				case HQ:
					// We found it!
					enemyHqLocation = r.location;
					if (round == roundCreated || Utility.chebyshev(location, r.location) <= 1) {
						state = LandscaperState.ASSAULTING_HQ;
					}
					distance = Utility.chebyshev(r.location, location);
					if (distance < targetBuildingDistance) {
						targetBuildingDistance = distance;
						targetBuildingLocation = r.location;
						targetRobot = r;
					}
					break;
				default:
					//Probably some structure, bury it if possible but low priority
					//Communications.sendMessage(rc);
					break;
				}
			}
		}



		if (!rc.isReady()) return;


		/*if (state == LandscaperState.ASSAULTING_HQ && round > TURTLE_END) {
			state = LandscaperState.TERRAFORMING;
		}*/


		switch (state) {

		case TURTLING:
			doTurtling();
			break;
		case ASSAULTING_HQ:
			doAssault();
			break;
		case TERRAFORMING:
			doTerraforming();
		}

	}

	/**
	 * Assign the Landscaper a role.
	 */
	private void assignRole() {
		if (hqLocation.isWithinDistanceSquared(location, 25)) {
			state = LandscaperState.TURTLING;
		}
	}

	private void doTurtling() throws GameActionException {
		int rank = Utility.chebyshev(location, hqLocation);

		if (targetBuildingLocation != null) {
			if (location.distanceSquaredTo(targetBuildingLocation)>2) {
				if (rank > 1) {
					if (Nav.target == null || !Nav.target.equals(targetBuildingLocation)) {
						Nav.beginNav(rc, this, targetBuildingLocation);
					}
					Nav.nav(rc, this);
					return;
				}

			} else {
				if (targetRobot.type.dirtLimit - targetRobot.dirtCarrying > dirtCarrying) {
					if (pitDirection != null) {
						rc.digDirt(pitDirection);
						return;
					}
				}
				if (dirtCarrying > 0) {
					Direction d = location.directionTo(targetBuildingLocation);
					if (rc.canDepositDirt(d)) {
						rc.depositDirt(d);
						return;
					}
				}

			}
		}
		
		if (location.isAdjacentTo(hqLocation)) {
			RobotInfo hqInfo = rc.senseRobotAtLocation(hqLocation);
			if (hqInfo.dirtCarrying > 0) {
				rc.digDirt(location.directionTo(hqLocation));
				return;
			}
		}



		Direction[] dirs = Utility.directions;
		MapLocation hqAdjacent;
		boolean allFilled = true;
		RobotInfo ri;
		int elev;
		int lowElev = 10000;
		MapLocation lowLoc = null;
		boolean adjacentLow = false;
		MapLocation flooded = null;
		for (int i = 8; i-->0;) {
			hqAdjacent = hqLocation.add(dirs[i]);


			if (!rc.onTheMap(hqAdjacent)) continue;


			if (!rc.canSenseLocation(hqAdjacent)) {
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

			if (turnsNavvedHq < 50) {
				if (!allFilled) {
					//TODO: check if can tunnel/bridge
					if (Nav.target == null || !Nav.target.equals(hqLocation)) {
						Nav.beginNav(rc, this, hqLocation);
					}
					Nav.nav(rc, this);
					turnsNavvedHq++;
					return;
				} else {
					state = LandscaperState.TERRAFORMING;
					return;
				}
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
				Direction d = null;
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


	}

	private void moveTurtle(MapLocation ml) throws GameActionException {
		//If there is a nearby path location that is not traversable, make it traversable
		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation m;
		int elev;
		int elevDiff;
		for (int i = 8; i-->0; ) {
			d = dirs[i];
			m = location.add(d);
			if (Utility.chebyshev(m, hqLocation) != 1) continue;
			if (rc.canSenseLocation(m)) {
				elev = rc.senseElevation(m);
				elevDiff = robotElevation - elev;
				if (elevDiff > TERRAFORM_THRESHOLD) continue;
				if (elevDiff < 0) elevDiff = -elevDiff;
				if (elevDiff > GameConstants.MAX_DIRT_DIFFERENCE) {
					tunnelOrBridge(m, d);
					return;
				}
			}
		}

		//Move towards location
		if (Nav.target == null || !ml.equals(Nav.target)) {
			Nav.beginNav(rc, this, ml);
		}
		Nav.nav(rc, this);
	}

	private void moveTerraform(MapLocation ml) throws GameActionException {
		//If there is a nearby path location that is not traversable, make it traversable
		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation m;
		int elev;
		int elevDiff;
		RobotInfo botInfo;
		for (int i = 8; i-->0; ) {
			d = dirs[i];
			m = location.add(d);
			if (pitTile(m) || Utility.chebyshev(m, hqLocation) == 1) continue;
			if (rc.canSenseLocation(m)) {
				botInfo = rc.senseRobotAtLocation(m);
				if (botInfo != null && botInfo.team == team && botInfo.type.isBuilding()) continue;
				elev = rc.senseElevation(m);
				elevDiff = robotElevation - elev;
				if (elevDiff > TERRAFORM_THRESHOLD) continue;
				if (elevDiff < 0) elevDiff = -elevDiff;
				if (elevDiff > 0 && elev < MAX_HEIGHT_THRESHOLD) {
					//System.out.println("TUNNEL BRIDGE:" + m);
					tunnelOrBridge(m, d);
					return;
				}
			}
		}

		//Move towards location
		if (Nav.target == null || !ml.equals(Nav.target)) {
			Nav.beginNav(rc, this, ml);
		}
		Nav.nav(rc, this);
	}

	private void tunnelOrBridge(MapLocation ml, Direction d) throws GameActionException {
		int elTarget = rc.senseElevation(ml);
		int elDistance = elTarget - robotElevation;
		if (rc.senseFlooding(ml)) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else if (elDistance > 0) {
			if (dirtCarrying > 0) {
				rc.depositDirt(Direction.CENTER);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else if (elDistance < 0) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		}
	}

	private void moveOrTunnelOrBridge(MapLocation ml, Direction d) throws GameActionException {
		int elTarget = rc.senseElevation(ml);
		int elDistance = elTarget - robotElevation;
		if (rc.senseFlooding(ml)) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else if (elDistance > GameConstants.MAX_DIRT_DIFFERENCE) {
			if (dirtCarrying > 0) {
				rc.depositDirt(Direction.CENTER);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else if (elDistance < -GameConstants.MAX_DIRT_DIFFERENCE) {
			if (dirtCarrying > 0) {
				rc.depositDirt(d);
			} else {
				if (pitDirection != null) rc.digDirt(pitDirection);
			}
		} else {
			rc.move(d);
		}
	}

	public void doTerraforming() throws GameActionException {

		//System.out.println("BACKUP:"+backupFill);

		//Destroy enemy building
		if (targetBuildingLocation != null) {
			//System.out.println("targeting Building at " + targetBuildingLocation);
			int csDist = Utility.chebyshev(location, targetBuildingLocation);
			if (csDist <= 1) {
				if (dirtCarrying == 0) {
					if (pitDirection != null) {
						rc.digDirt(pitDirection);
						return;
					}
				} else {
					rc.depositDirt(location.directionTo(targetBuildingLocation));
					return;
				}

			} else {
				moveTerraform(targetBuildingLocation);
				return;
			}
		}
		
		if (location.isAdjacentTo(hqLocation)) {
			RobotInfo hqInfo = rc.senseRobotAtLocation(hqLocation);
			if (hqInfo.dirtCarrying > 0) {
				rc.digDirt(location.directionTo(hqLocation));
				return;
			}
		}

		//start turtling if on rank 1
		if (Utility.chebyshev(location, hqLocation) == 1) {
			state = LandscaperState.TURTLING;
			doTurtling();
			return;
		}

		//Move off of pit
		if (pitTile(location)) {
			//System.out.println("Moving off pit");
			Direction[] dirs = Utility.directions;
			Direction d;
			for (int i = 8; i-->0;) {
				d=dirs[i];
				if (rc.canMove(d)) {
					rc.move(d);
					return;
				}
			}
		}

		if (nearestFillTile != null) {
			//check if still needs filling
			if (!rc.canSenseLocation(nearestFillTile)) {
				nearestFillTile = null;
			} else {

				int elev = rc.senseElevation(nearestFillTile);
				if (robotElevation - elev == 0) {
					nearestFillTile = null;
				} else {

					RobotInfo ri = rc.senseRobotAtLocation(nearestFillTile);
					if (ri != null && ri.team == team && ri.type.isBuilding()) {
						nearestFillTile = null;
					}
				}
			}
		}
		
		if (backupFill != null && Utility.chebyshev(location, backupFill) <= 2) {
			backupFill = null;
		}

		if (nearestFillTile == null) {
			//Find tile to fill
			MapLocation ml;
			int rSq = senseRadiusSq;
			int radius = Math.min((int)(Math.sqrt(rSq)), TERRAFORM_RANGE_SQ);
			ml = null;
			int dx;
			int dy;
			int rad;
			int elev;
			int dElev;
			int bestPriority = 100000;
			int priority;
			int rank;
			int csDist;
			RobotInfo ri;
			for (int x = Math.max(0, location.x - radius); x <= Math.min(mapWidth - 1, location.x + radius); x++) {
				for (int y = Math.max(0, location.y - radius); y <= Math.min(mapHeight - 1, location.y + radius); y++) {
					dx = x - location.x;
					dy = y - location.y;
					rad = dx * dx + dy * dy;
					if (rad > rSq) continue;
					ml = new MapLocation(x, y);

					csDist = Utility.chebyshev(ml, location);
					if (csDist > MAX_TERRAFORM_RANGE) continue;
					if (pitTile(ml)) continue;
					elev = rc.senseElevation(ml);
					ri = rc.senseRobotAtLocation(ml);
					if (ri != null && (ri.type.isBuilding() || (ri.team == team && ri.type == RobotType.LANDSCAPER))) continue;
					dElev = robotElevation - elev;
					if (dElev > TERRAFORM_THRESHOLD) continue;
					rank = Utility.chebyshev(ml, hqLocation);
					if (rank == 1) continue;
					//heuristic
					priority = csDist + rank;
					if (enemyHqLocation != null) {
						priority += Utility.chebyshev(ml, enemyHqLocation);
					}
					if (dElev < 0) {
						dElev = -dElev;
						priority -= 100; //Prioritize filling in lower tiles rather than digging higher ones
					}
					if (elev >= MAX_HEIGHT_THRESHOLD) continue;
					if (dElev > 0) {
						if (priority < bestPriority) {
							bestPriority = priority;
							nearestFillTile = ml;
						}

					}
				}
			}
		}

		if (nearestFillTile != null) {
			//System.out.println("Filling in...");
			moveTerraform(nearestFillTile);
			if (communicationDelay == 0) {
				if (nearbyTerraformers < MAX_NEARBY_TERRAFORMERS) {
					Communications.queueMessage(rc, 1, 15, nearestFillTile.x, nearestFillTile.y);
					communicationDelay = 20;
				}
			} else {
				communicationDelay--;
			}
			return;
		}

		//Start increasing height of terraform
		//System.out.println("Increase terraform height");
		if (dirtCarrying == 0) {
			rc.digDirt(pitDirection);
		} else {
			if (!pitTile(location) && robotElevation < MAX_HEIGHT_THRESHOLD) {
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
					
					if (pitTile(m) || Utility.chebyshev(m, hqLocation) == 1) continue;
					if (rc.canSenseLocation(m)) {
						botInfo = rc.senseRobotAtLocation(m);
						elev = rc.senseElevation(m);
						if ((botInfo != null && botInfo.team == team && botInfo.type.isBuilding()) || elev >= MAX_HEIGHT_THRESHOLD) continue;
						rc.depositDirt(d);
						return;
					}
				}
			}
		}
		
		//Move towards terraform edge
		if (backupFill != null) {
			moveTerraform(backupFill);
		}
	}
	
	public int heuristic(MapLocation ml) {
		int k = Utility.chebyshev(ml, location)+Utility.chebyshev(ml, hqLocation);
		if (enemyHqLocation == null) return k;
		return k + Utility.chebyshev(ml, enemyHqLocation);
	}

	public void doAssault() throws GameActionException {
		if (location.distanceSquaredTo(enemyHqLocation) > 2) {
			if (Nav.target == null || !Nav.target.equals(enemyHqLocation)) {
				Nav.beginNav(rc, this, enemyHqLocation);
			}
			Nav.nav(rc, this);
			return;
		}

		//dig and place in alternating cycles
		if (((dirtCarrying < RobotType.LANDSCAPER.dirtLimit && rushDigging) || dirtCarrying == 0) && !isDroneThreat) {
			rushDigging = true;
			rc.digDirt(Direction.CENTER);
		} else {
			rushDigging = false;
			rc.depositDirt(location.directionTo(enemyHqLocation));
		}

	}

	@Override
	public void processMessage(int m, int x, int y) {
		switch (m) {
		case 1:
			hqLocation = new MapLocation(x,y);
			//System.out.println("Received HQ location: " + x + ", " + y);
			break;
		case 3:
			enemyHqLocation = new MapLocation(x,y);
			break;
		case 15:
			MapLocation ml15 = new MapLocation(x,y);
			if (backupFill == null || heuristic(location) + BACKUP_THRESHOLD < heuristic(location)) {
				backupFill = ml15;
			}
			if (Utility.chebyshev(ml15, location) <= 4) {
				communicationDelay += 20;
			}
			//System.out.println("recieved a fill location");
			break;
		}

	}


}

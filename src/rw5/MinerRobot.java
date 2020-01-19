package rw5;

import java.util.ArrayList;

import battlecode.common.*;

import static rw5.Utility.SCOUT_ROUND;

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
	private boolean isRush = false;

	public ArrayList<MapLocation> refineries;

	public static final int DISTANCE_REFINERY_THRESHOLD = 400; // minimum distance apart for refineries
	public static final int DISTANCE_SOUP_THRESHOLD = 25; //maximum distance from refinery to soup deposit upon creation

	public static final int MAX_VAPORATOR_BUILD_ROUND = 1500;
	
	enum MinerState {
		SEEKING, MINING, RETURNING, SCOUTING_ENEMY_HQ, RUSHING_ENEMY_HQ, DRONE_NETTING_ENEMY_HQ
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
		boolean isRefineryNearby = false;
		boolean dsBuilt = false;
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
					if (rc.senseElevation(r.location) >= robotElevation-3) {
						dsBuilt = true;
					}
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
		
		//Calculate Random
		random = (A*random+B)%256;

		

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
				if (s > 0) {// && !rc.senseFlooding(ml) Removed because some soup on edge is minable even when flooded
					if (rad0 == -1 || rad < rad0) {
						rad0 = rad;
						nearestSoup = ml;
					}
					totalSoup += s;
				}
			}
		}
		System.out.println("SEARCH bytecodes: " + (bytecode1 - Clock.getBytecodesLeft()));
		if (soupCommunicateCooldown > 0) soupCommunicateCooldown--;
		
		if (nearestSoup !=null && totalSoup / nearbyMiners > 100 && nearbyMiners < 4) {
			if (soup>1 && soupCommunicateCooldown == 0) {
				Communications.queueMessage(rc, 1, 2, nearestSoup.x, nearestSoup.y);
				soupCommunicateCooldown += 20; //don't communicate soup location for another 20 turns
			}
		}
		
		if (round >= TURTLE_ROUND && returnLoc == hqLocation && refineries.size() > 0) {
			returnLoc = null;
			navigatingReturn = false;
		}
		
		MapLocation nearestRefinery = null;
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
		} else if (round < TURTLE_ROUND) {
			returnLoc = hqLocation;
		}
		
		if (!navigatingReturn) {
			if (nearestRefinery != null) {
				returnLoc = nearestRefinery;
			} else if (round < TURTLE_ROUND) {
				returnLoc = hqLocation;
			}
		}
		int hqDist = 100;
		if (hqLocation != null) hqDist = Utility.chebyshev(location, hqLocation);
		
		if (cooldownTurns >= 1) return;
		
		//Build Refinery
		if (!isRefineryNearby && !isRush && soup > RobotType.REFINERY.cost && (hqDist >= DISTANCE_REFINERY_THRESHOLD || (round > TURTLE_ROUND && hqDist > 2))) {
			if ((nearestRefinery == null || location.distanceSquaredTo(nearestRefinery) >= DISTANCE_REFINERY_THRESHOLD) && nearestSoup != null && location.distanceSquaredTo(nearestSoup) <= DISTANCE_SOUP_THRESHOLD) {
				Direction[] dirs = Utility.directions;
				Direction d;
				MapLocation refLoc;
				for (int i = dirs.length; --i >= 0;) {
					d = dirs[i];
					refLoc = location.add(d);
					if (rc.canBuildRobot(RobotType.REFINERY, d) && Utility.chebyshev(refLoc, hqLocation) > 2) {
						rc.buildRobot(RobotType.REFINERY, d);
						MapLocation refineryLoc = location.add(d);
						if (soup>5) Communications.queueMessage(rc, 5, 5, refineryLoc.x, refineryLoc.y);
						refineries.add(refineryLoc);
						return;
					}
				}
			}
		}
		
		if (designSchoolBuildCooldown > 0)designSchoolBuildCooldown--;
		if (dsBuilt) turnsSinceDesignSchoolSeen = 0;
		else turnsSinceDesignSchoolSeen++;
		//Build Design School
		if (round > TURTLE_ROUND && !isRush && (turnsSinceDesignSchoolSeen>DESIGN_SCHOOL_SEEN_TURNS || (soup > 1000 && !dsBuilt)) && designSchoolBuildCooldown == 0 && soup > RobotType.DESIGN_SCHOOL.cost && hqLocation != null && (refineries.size() > 0 || round > 2*TURTLE_ROUND) && hqDist>=2) {
			Direction[] dirs = Utility.directions;
			Direction d;
			ml = null;
			for (int i = 8; i-->0;) {
				d = dirs[i];
				ml = location.add(d);
				if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
					if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
						rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
						dsBuilt = true;
						return;
					}
				}
			}
		}
		
		//Build Vaporator
		if (round < MAX_VAPORATOR_BUILD_ROUND && !isRush && soup > RobotType.VAPORATOR.cost && designSchoolBuildCooldown > 0 && hqDist >= 2) {
			Direction[] dirs = Utility.directions;
			Direction d;
			ml = null;
			for (int i = 8; i-->0;) {
				d = dirs[i];
				ml = location.add(d);
				if (Utility.chebyshev(ml, hqLocation) > 2 && buildingTile(ml)) {
					if (rc.canBuildRobot(RobotType.VAPORATOR, d)) {
						rc.buildRobot(RobotType.VAPORATOR, d);
						return;
					}
				}
			}
		}
		
		System.out.println("NEAREST REFINERY:"+nearestRefinery);
		
		
		//if (soupMine != null)System.out.println("TARGETING: " + soupMine.x + ", " + soupMine.y);
		//if (nearestSoup != null)System.out.println("NEARSOUP: " + nearestSoup.x + ", " + nearestSoup.y);
		//if (Nav.target != null) System.out.println("NAVTARGET: " + Nav.target.x + ", " + Nav.target.y);

		//System.out.println(round);
		//System.out.println(SCOUT_ROUND);

		// Possibly switch state to scouting if this is the first miner built
		if (round == SCOUT_ROUND && roundCreated <= 2) {
			minerState = MinerState.SCOUTING_ENEMY_HQ;
			isRush = true;
			EnemyHqPossiblePosition[] positions = EnemyHqPossiblePosition.values();
			setScoutEnemyHq(positions[random % positions.length]);
		}

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
				if (nearestSoup != null) {
					soupMine = nearestSoup;
					Nav.beginNav(rc, this, soupMine);
				} else {
					moveScout(rc);
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
			if (returnLoc != null ) {
				if (!navigatingReturn) {
					Nav.beginNav(rc, this, returnLoc);
					navigatingReturn = true;
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
		case SCOUTING_ENEMY_HQ:
			scoutEnemyHq();
			break;
		case RUSHING_ENEMY_HQ:
			rushEnemyHq();
			break;
		case DRONE_NETTING_ENEMY_HQ:
			droneNetEnemyHq();
			break;
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
		if (rc.canMove(d) && !rc.senseFlooding(location.add(d))) {
			rc.move(d);
			lastDirection = d;
			return true;
		}
		return false;
	}

	private void setScoutEnemyHq(EnemyHqPossiblePosition possiblePosition) {
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
	}

	/**
	 * Should place a design school adjacent to the enemy HQ, then switch to looking for soup.
	 */
	private void rushEnemyHq() throws GameActionException {
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

			System.out.println("Can'b build here: ");
		}

		Nav.nav(rc, this);
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
				if (minerState != MinerState.SCOUTING_ENEMY_HQ) {
					Nav.beginNav(rc, this, soupMine);
				}
			}
			System.out.println("Recieved soup location: " + x + ", " + y);
			break;
		case 5:
			MapLocation ml5 = new MapLocation(x,y);
			System.out.println("Recieved Refinery location: " + x + ", " + y);
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
		}

	}
	
	
}

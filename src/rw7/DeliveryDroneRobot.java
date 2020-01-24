package rw7;

import java.util.ArrayList;

import battlecode.common.*;

public strictfp class DeliveryDroneRobot extends Robot {

	private enum DroneState {
		ATTACKING,
		ASSAULTING,
		DEFENDING,
		TRANSPORTING,
      MINER_ASSIST
	}

	private MapLocation homeLocation;
	private MapLocation targetLocation;  //building or location to clear enemy robots from
	private MapLocation targetLocationf; //friendly robot location
	private RobotInfo targetRobot;
	private RobotInfo targetFriendly; //robot to move to pathtile
	private MapLocation nearestWater;
	private MapLocation nearestSafe; //nearest path tile
	private DroneState state;
	public boolean rush = false; //avoid netguns + hq?
	private boolean sentEHQL = false;
	private Direction lastDirection;
	private int friendlyDrones;
	private MapLocation minerAssistLocation;  //where to put
	private MapLocation emptyWallLocation;
   private RobotInfo minerToAssist;
   
	private MapLocation rushLocation;
	private int turnsSinceRush;

	private static final int RUSH_SAFE_TURNS = 50;
	private static final int DEFEND_RANGE = 15;
	public static final int DRONE_COUNT_RUSH = 16;
	private boolean scouting = true;
	private boolean rushDetected = true;
	private int enemyLandscapers;

	private boolean carryingEnemy;
	private boolean carryingAssaulter;


	public int random; // A random number from 0 to 255
	public static final int A = 623;
	public static final int B = 49;
	public static final int RUSH_RANGE = 12; //Chebyshev range to regard an enemy as a rusher
	public static final int CRUNCH_RANGE = 8; //If within this distance when crunch signal detected, crunch
	public static final int FINAL_CRUNCH_ROUND = 1700;

	public ArrayList<MapLocation> enemyNetguns;
	private static final int ASSAULT_ROUND = 1300;

	public DeliveryDroneRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
		RobotInfo[] ri = rc.senseNearbyRobots(2, rc.getTeam());
		RobotInfo r;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			// Friendly Units
			if (r.getType() == RobotType.FULFILLMENT_CENTER && rc.getLocation().isAdjacentTo(r.getLocation())) {
				homeLocation = r.getLocation();
				break;
			}
		}
		enemyNetguns = new ArrayList<MapLocation>();
		carryingEnemy = false;
		carryingAssaulter = false;

	}

	@Override
	public void run() throws GameActionException {
		// TODO Auto-generated method stub
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
		rushDetected = false;

		//Calculate Random
		random = (A*random+B)%256;

		ArrayList<MapLocation> enemyGuns = enemyNetguns;
		MapLocation enemyGun;
		for (int i = enemyGuns.size(); i-->0;) {
			enemyGun = enemyGuns.get(i);
			if (rc.canSenseLocation(enemyGun)) {
				r = rc.senseRobotAtLocation(enemyGun);
				if (r == null || r.team == team || r.type != RobotType.NET_GUN) {
					enemyGuns.remove(i);
				}
			}
		}

		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			//System.out.println(r);
			if (r.getTeam() == team) {
				// Friendly Units
				switch (r.getType()) {
				case HQ:
					hqLocation = r.location;
					break;
				case FULFILLMENT_CENTER:
					if (homeLocation == null) homeLocation = r.location;
					break;
				case MINER:
					if(state == DroneState.TRANSPORTING || state == DroneState.MINER_ASSIST) {
   					distance = Utility.chebyshev(location, r.location);
   					if (distance < minerDistance && (!pathTile(r.location) || state == DroneState.MINER_ASSIST)) { //kinda hacky, only pick up miner if its not on the path, unless in miner assist mode
   						minerToAssist = r;
   						minerDistance = distance;
   					}
               }
					break;
				case LANDSCAPER:
					if(state == DroneState.ASSAULTING && (enemyHqLocation == null || !r.location.isAdjacentTo(enemyHqLocation))) {
						if (rc.canPickUpUnit(r.ID)) {
							rc.pickUpUnit(r.ID);
							carryingAssaulter = true;
							return;
						}

						distance = Utility.chebyshev(location, r.location);
						if (distance < targetDistance && Utility.chebyshev(r.location, hqLocation) > 2) {
							if (enemyHqLocation == null || Utility.chebyshev(r.location, enemyHqLocation) > 1)
								targetLocationf = r.location;
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
					// TODO: Block or bury
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
					if (!rush && r.location.isWithinDistanceSquared(location, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
						if (rc.isReady()) {
							DroneNav.fuzzy(rc, this, r.location.directionTo(location));
							return;
						}
					}
					if (!enemyNetguns.contains(r.location))enemyNetguns.add(r.location);
					break;
				case REFINERY:
					// TODO: target?
					break;
				case DESIGN_SCHOOL:
					// TODO: target?
					if (Utility.chebyshev(r.location, hqLocation) < RUSH_RANGE) {
						System.out.println("DESIGN SCHOOL RUSH");
						rushDetected = true;
					}
					break;
				case HQ:
					// We found it!
					//also avoid
					enemyHqLocation = r.location;
					if (!enemyNetguns.contains(r.location))enemyNetguns.add(r.location);

					/*if (state != DroneState.ASSAULTING) {
						Direction[] directions = Utility.directions;
						Direction d;
						RobotInfo botInf;
						MapLocation adj;
						boolean canAssault = true;
						for (int k = 8; k-->0;) {
							d = directions[k];
							adj = enemyHqLocation.add(d);
							if (!rc.canSenseLocation(adj)) {
								canAssault = false;
								break;
							}
							botInf = rc.senseRobotAtLocation(adj);
							/*if (botInf == null || botInf.team == team || (botInf.team != team && (botInf.type == RobotType.DELIVERY_DRONE || botInf.type.isBuilding()))) {

						}
							if (botInf != null && botInf.team != team && botInf.type != RobotType.DELIVERY_DRONE && !botInf.type.isBuilding()) {
								canAssault = false;
								break;
							}
						}
						if (canAssault) {
							state = DroneState.ASSAULTING;
							if (soup > 2)Communications.queueMessage(rc, 2, 13, enemyHqLocation.x, enemyHqLocation.y);

						}
					}*/
					break;
					//System.out.println(r.location);
				default:
					//Probably some structure, bury it if possible but low priority
					//Communications.sendMessage(rc);
					break;
				}
			} else {
				if (targetLocation == null) {
					targetLocation = r.location;
					targetRobot = r;
				}
			}
		}
		System.out.println("secret " + Communications.verySecretNumber + ", nearby drones: " + friendlyDrones);
		
		if(enemyHqLocation != null && !sentEHQL) {
			Communications.queueMessage(rc, 3, 3, enemyHqLocation.x, enemyHqLocation.y);
			sentEHQL = true;
		}

		if (!rc.isCurrentlyHoldingUnit()) {
			carryingAssaulter = false;
			carryingEnemy = false;
		}

		System.out.println("enemy:"+carryingEnemy);

		

		if (round < TURTLE_ROUND || rushDetected || (rushLocation != null)) {
			state = DroneState.DEFENDING;
		} else {
			state = DroneState.ATTACKING;
		}

		if (round > ASSAULT_ROUND && shouldAssault()) {
			state = DroneState.ASSAULTING;
		}

		if (!rc.isReady()) 
			return;

		//System.out.println(state);
		/*if (state == DroneState.ATTACKING || state == DroneState.ASSAULTING) {
			if (enemyHqLocation != null) {
				if (Utility.chebyshev(location, enemyHqLocation) < 5) {
					if (round % 100 == 49 && friendlyDrones >= 3) {
						if (soup > 2) Communications.queueMessage(rc, 2, 4, enemyHqLocation.x, enemyHqLocation.y);
					}
				}
			}
		}*/

		if (state != DroneState.ASSAULTING && enemyHqLocation != null) {
			if (location.distanceSquaredTo(enemyHqLocation) < 8 && targetRobot == null) {
				Communications.queueMessage(rc, 1, 2, enemyHqLocation.x, enemyHqLocation.y);
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
		if (rushLocation != null) {
			turnsSinceRush++;
			if (turnsSinceRush > RUSH_SAFE_TURNS) {
				rushLocation = null;
				turnsSinceRush = 0;
			}
		}
		//if (DroneNav.target != null) rc.setIndicatorDot(DroneNav.target, 255, 0, 255);
		if (nearestWater != null) rc.setIndicatorDot(nearestWater, 0, 255, 255);
		if (targetLocation != null) rc.setIndicatorDot(targetLocation, 255, 0, 0);

	}
   
   public void doAssist() {
		if(rc.isCurrentlyHoldingUnit()) {
			if(Utility.chebyshev(location, minerAssistLocation) <= 1) {
				if(rc.canDropUnit(location.directionTo(minerAssistLocation))) {
					rc.dropUnit(location.directionTo(minerAssistLocation));
					return;
				}
			}

			return;
		}
		else if(minerToAssist != null) {
			if (Utility.chebyshev(location, minerToAssist.location) <= 1) {
				if (rc.canPickUpUnit(minerToAssist.ID)) {
					rc.pickUpUnit(minerToAssist.ID);
					return;
				}
			} else {
				if (DroneNav.target == null || !DroneNav.target.equals(minerToAssist.location)) {
					DroneNav.beginNav(rc, this, minerToAssist.location);
				}
				DroneNav.nav(rc, this);
				return;
			}
		}
      else {
				if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
					DroneNav.beginNav(rc, this, hqLocation);
				}
      }

   }
   
	public void doAssault() throws GameActionException {

		doInitiateRush();
		
		//int ehqdist = location.distanceSquaredTo(enemyHqLocation);
		if (enemyHqLocation == null) {
			state = DroneState.ATTACKING;
		}
		if(carryingAssaulter) {
			/*if(ehqdist < 36) {
   			if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
   				DroneNav.beginNav(rc, this, enemyHqLocation);
   			}
   			DroneNav.nav(rc, this);
            return;
         }
         else*/
			if (tryDropAssaulter()) return;
			
			scanForSafe();
			if(nearestSafe == null) {

				if (enemyHqLocation != null) {
					if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
						DroneNav.beginNav(rc, this, enemyHqLocation);
					}
					DroneNav.nav(rc, this);
					return;
				} else {
					moveScout(rc);
					return;
				}
			}
			else {
				if(Utility.chebyshev(location, nearestSafe) <= 1) {
					if(rc.canDropUnit(location.directionTo(nearestSafe))) {
						rc.dropUnit(location.directionTo(nearestSafe));
						return;
					}
				} else {
					if (DroneNav.target == null || !DroneNav.target.equals(nearestSafe)) {
						DroneNav.beginNav(rc, this, nearestSafe);
					}
					DroneNav.nav(rc, this);
					return;
				}

				/*if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
					DroneNav.beginNav(rc, this, enemyHqLocation);
				}
				DroneNav.nav(rc, this);
				return;*/
			}
		}
		else {
			if (tryDrown()) return;
			scanForWater();
			if (doDropEnemy()) return;
			
			if(targetFriendly != null) {
				if (Utility.chebyshev(location, targetLocationf) <= 1) {
					if (rc.canPickUpUnit(targetFriendly.ID)) {
						rc.pickUpUnit(targetFriendly.ID);
						carryingAssaulter = true;
						return;
					}
				} else {
					if (DroneNav.target == null || !DroneNav.target.equals(targetLocationf)) {
						DroneNav.beginNav(rc, this, targetLocationf);
					}
					DroneNav.nav(rc, this);
					return;
				}
			}
			else {
				if (enemyHqLocation != null) {
					if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
						DroneNav.beginNav(rc, this, enemyHqLocation);
					}
					DroneNav.nav(rc, this);
					return;
				} else {
					moveScout(rc);
				}
				//state = DroneState.ATTACKING;
			}
		}
	}

	public boolean shouldAssault() {
		return id % 3 == 0;
	}

	public void doInitiateRush() throws GameActionException {
		if (enemyHqLocation != null) {
			if (Utility.chebyshev(location, enemyHqLocation) <= CRUNCH_RANGE) {
				if (friendlyDrones >= DRONE_COUNT_RUSH) {// || round > FINAL_CRUNCH_ROUND
					Communications.queueMessage(rc, 2, 4, enemyHqLocation.x, enemyHqLocation.y);
					//rush = true;
				}
			}




		}

	}

	public boolean scanForSafe() throws GameActionException {
		//scan for a safe spot next to enemy hq
		/*MapLocation ml;
		int rSq = senseRadiusSq;
		int radius = (int)(Math.sqrt(rSq));
		ml = null;
		int dx;
		int dy;
		int rad0 = 10000;
		int rad;
		int csDist;
		boolean toRet = false;  //found an appropriate tile?
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
		return toRet;*/
		if (enemyHqLocation == null) return false;
		boolean toRet = false;
		Direction[] directions = Utility.directions;
		Direction d;
		int csDist;
		MapLocation adj;
		int bestDist = 10000;
		RobotInfo ri;
		for (int i = 8; i-->0;) {
			d = directions[i];
			adj = enemyHqLocation.add(d);
			
			if (rc.canSenseLocation(adj)) {
				csDist = Utility.chebyshev(location, adj);
				if (csDist < bestDist) {
					ri = rc.senseRobotAtLocation(adj);
					if (ri == null) {
						nearestSafe = adj;
						bestDist = csDist;
						toRet = true;
					}
				}
			}
		}
		return toRet;
	}

	public boolean tryDrown() throws GameActionException {
		Direction[] directions = Utility.directions;
		Direction d;
		MapLocation ml;
		for (int i = 8; i-->0;) {
			d = directions[i];
			ml = location.add(d);
			if (rc.canSenseLocation(ml)) {
				if (rc.senseFlooding(ml)) {
					if (rc.canDropUnit(d)) {
						rc.dropUnit(d);
						return true;
					}
				}
			}
		}
		return false;
	}

	public void doTransport() throws GameActionException {
		if(rc.isCurrentlyHoldingUnit()) {
			if(nearestSafe == null) {
				if(scanForSafe()) {
					DroneNav.beginNav(rc, this, nearestSafe);
					DroneNav.nav(rc, this);
					return;
				}
				else {
					if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
						DroneNav.beginNav(rc, this, hqLocation);
					}
					DroneNav.nav(rc, this);
					return;
				}
			}
			else {
				if(Utility.chebyshev(location, nearestSafe) <= 1) {
					if(rc.canDropUnit(location.directionTo(nearestSafe))) {
						rc.dropUnit(location.directionTo(nearestSafe));
						return;
					}
				}

				if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
					DroneNav.beginNav(rc, this, hqLocation);
				}
				DroneNav.nav(rc, this);
				return;
			}
		}
		else if(minerToAssist != null) {
			if (Utility.chebyshev(location, minerToAssist.location) <= 1) {
				if (rc.canPickUpUnit(minerToAssist.ID)) {
					rc.pickUpUnit(minerToAssist.ID);
					scanForSafe();
					return;
				}
			} else {
				if (DroneNav.target == null || !DroneNav.target.equals(minerToAssist.location)) {
					DroneNav.beginNav(rc, this, minerToAssist.location);
				}
				DroneNav.nav(rc, this);
				return;
			}
		}
      else {
				if (DroneNav.target == null || !DroneNav.target.equals(minerAssistLocation)) {
					DroneNav.beginNav(rc, this, minerAssistLocation);
				}
      }
		//else wander
		//moveScout(rc);
	}

	public void scanForWater() throws GameActionException {
		MapLocation ml;
		int rSq = senseRadiusSq;
		int radius = (int)(Math.sqrt(rSq));
		ml = null;
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
	
	public boolean tryDropAssaulter() throws GameActionException {
		if (enemyHqLocation == null) return false;
		Direction[] directions = Utility.directions;
		Direction d;
		MapLocation ml;
		for (int i = 8; i-->0;) {
			d = directions[i];
			ml = location.add(d);
			if (Utility.chebyshev(ml, enemyHqLocation) != 1) continue;
			if (rc.canDropUnit(d)) {
				rc.dropUnit(d);
			}
		}
		return false;
	}

	public boolean doDropEnemy() throws GameActionException {
		if(carryingEnemy) {
			//pathfind towards target (water, soup)
			//if any of 8 locations around are flooded, place robot into flood, update nearestWater


			if(nearestWater != null) {
				//rc.setIndicatorLine(location, nearestWater,0, 100, 200);
				/*if(Utility.chebyshev(location, nearestWater) <= 1) {
					if(rc.canDropUnit(location.directionTo(nearestWater))) {
						rc.dropUnit(location.directionTo(nearestWater));
						return true;
					}

				}*/
				
				if (DroneNav.target == null || !DroneNav.target.equals(nearestWater)) {
					DroneNav.beginNav(rc, this, nearestWater);
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
						DroneNav.beginNav(rc, this, targetLocation);
					}
					DroneNav.nav(rc, this);
					return true;
				}
			} else if (targetLocation != null) {
				if (DroneNav.target == null || !DroneNav.target.equals(targetLocation)) {
					DroneNav.beginNav(rc, this, targetLocation);
				}
				DroneNav.nav(rc, this);
				return true;
			}
		}
		return false;
	}

	public void doAssistLandscaper() {

	}

	public void doAttack() throws GameActionException {
		if (tryDrown()) return;
		scanForWater();
		doInitiateRush();

		if (doDropEnemy()) return;

		if (enemyHqLocation != null) {
			if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
				DroneNav.beginNav(rc, this, enemyHqLocation);
			}
			DroneNav.nav(rc, this);
			return;
		}






		//System.out.println("SCOUTING");
		moveScout(rc);
	}

	public void doDefense() throws GameActionException {
		if (tryDrown()) return;
		scanForWater();
		/*if(rc.isCurrentlyHoldingUnit()) {
			//pathfind towards target (water, soup)
			//if any of 8 locations around are flooded, place robot into flood, update nearestWater


			if(nearestWater != null) {
				if(Utility.chebyshev(location, nearestWater) == 1) {
					if(rc.canDropUnit(location.directionTo(nearestWater))) {
						rc.dropUnit(location.directionTo(nearestWater));
						return;
					}

				}
				if (DroneNav.target == null || !DroneNav.target.equals(nearestWater)) {
					DroneNav.beginNav(rc, this, nearestWater);
				}
				DroneNav.nav(rc, this);
				return;
			} else {
				moveScout(rc);
			}

		} else if (targetRobot != null) {
			if (Utility.chebyshev(location, targetLocation) == 1) {
				if (rc.canPickUpUnit(targetRobot.ID)) {
					rc.pickUpUnit(targetRobot.ID);
               scanForWater();
					return;
				}
			} else {
				if (DroneNav.target == null || !DroneNav.target.equals(targetLocation)) {
					DroneNav.beginNav(rc, this, targetLocation);
				}
				DroneNav.nav(rc, this);
				return;
			}
		}*/

		if (rushLocation != null && targetLocation == null) targetLocation = rushLocation;

		if (doDropEnemy()) return;


		if (Utility.chebyshev(location, hqLocation) < 5) {
			moveScout(rc);
			return;
		} else {
			if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
				DroneNav.beginNav(rc, this, hqLocation);
			}
			DroneNav.nav(rc, this);
			return;
		}


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
		if (canMove(rc,d)) {
			rc.move(d);
			lastDirection = d;
			return true;
		}
		return false;
	}

	public boolean canMove(RobotController rc, Direction d) {
		MapLocation ml = location.add(d);
		boolean isSafePos = true;
		if (!rush ) {
			ArrayList<MapLocation> enemyGuns = enemyNetguns;
			MapLocation enemyGun;
			for (int i = enemyGuns.size(); i-->0;) {
				enemyGun = enemyGuns.get(i);
				if (enemyGun.isWithinDistanceSquared(location, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) return true;
				if (enemyGun.isWithinDistanceSquared(ml, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
					isSafePos = false;
				}

			}
		}
		return isSafePos && rc.canMove(d);
	}

	@Override
	public void processMessage(int m, int x, int y) {
		// TODO Auto-generated method stub
		switch (m) {                                       //will probably want to add a case for attack
		case 1:
			hqLocation = new MapLocation(x,y);
			System.out.println("Received HQ location: " + x + ", " + y);
			break;
		case 3:
			if (enemyHqLocation == null) {
				enemyHqLocation = new MapLocation(x, y);
				enemyNetguns.add(enemyHqLocation);
			}
			sentEHQL = true; //if already received enemy hq location, don't rebroadcast
			break;
		case 4:
			if (enemyHqLocation == null) {
				enemyHqLocation = new MapLocation(x, y);
				enemyNetguns.add(enemyHqLocation);
			}
			if (Utility.chebyshev(enemyHqLocation, location) <= CRUNCH_RANGE) {
				rush = true;
			}
			break;
		case 11:
			rushLocation = new MapLocation(x,y);
			turnsSinceRush = 0;
			if (Utility.chebyshev(location,hqLocation)<DEFEND_RANGE)rushDetected = true;
			break;
		case 12:
			state = DroneState.TRANSPORTING;
         minerAssistLocation = new MapLocation(x, y);
			break;
		case 13:
			enemyHqLocation = new MapLocation(x,y);
			if (shouldAssault()) state = DroneState.ASSAULTING;
			break;
      case 14:
         minerAssistLocation = new MapLocation(x, y);// DOES NOT WORK, NEEDS LOCATION TO FIND MINER AND LOCATION TO PLACE MINER
         state = DroneState.MINER_ASSIST;           //maybe move miner towards hq if its stuck pathfinding?
         break;
      }
	}

}

package rw8;

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
	private boolean rush = false; //don't avoid netguns + hq?
	private boolean sentEHQL = false;
	private Direction lastDirection;
	private int friendlyDrones;
	private MapLocation minerAssistLocation;  //where to put
	private MapLocation emptyWallLocation;
	private MapLocation nearestPath;
	private RobotInfo allyToAssist;

	private EnemyHqPossiblePosition enemyHqScouting = EnemyHqPossiblePosition.X_FLIP;
	private MapLocation enemyHqScoutingLocation;

	private MapLocation rushLocation;
	private int turnsSinceRush;
	private boolean areEnemies;

	private static final int RUSH_SAFE_TURNS = 50;
	private static final int DEFEND_RANGE = 15;
	public static final int DRONE_COUNT_RUSH = 16;
	private boolean scouting = true;
	private boolean rushDetected = true;
	private int enemyLandscapers;

	private boolean carryingEnemy;
	private boolean carryingAssaulter;
	private boolean carryingAllyLandscaper;
	private boolean carryingAlly;
	private MapLocation netGunLoc;

	private int random; // A random number from 0 to 255
	private static final int A = 623;
	private static final int B = 49;
	private static final int RUSH_RANGE = 12; //Chebyshev range to regard an enemy as a rusher
	private static final int CRUNCH_RANGE = 8; //If within this distance when crunch signal detected, crunch
	private static final int FINAL_CRUNCH_ROUND = 1550;
	private static final int GIVE_UP_RANGE = 8; //gives up on pursuing a target if within this distance from the location it was last seen

	private ArrayList<MapLocation> enemyNetguns;
	private static final int ASSAULT_ROUND = 1300;

	public DeliveryDroneRobot(RobotController rc) throws GameActionException {
		super(rc);

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
		DroneNav.beginNav(rc, this, enemyHqScouting.getLocation(hqLocation, mapWidth, mapHeight));

		// TODO Auto-generated method stub
		// Process nearby robots (may have to move this into the if statements below)
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		
		int targetDistance = 10000;
		int allyDistance = 10000;
		int distance;
		
		if (targetRobot != null) {
			if (rc.canSenseRobot(targetRobot.ID)) {
				r = rc.senseRobot(targetRobot.ID);
				targetLocation = r.location;
				targetDistance = Utility.chebyshev(location, targetLocation);
			} else if (location.isWithinDistanceSquared(targetLocation, GIVE_UP_RANGE)) {
				targetRobot = null;
				targetLocation = null;
			}
		}
		if (targetFriendly != null) {
			if (rc.canSenseRobot(targetRobot.ID)) {
				r = rc.senseRobot(targetRobot.ID);
				targetLocationf = r.location;
				allyDistance = Utility.chebyshev(location, targetLocationf);
			} else if (location.isWithinDistanceSquared(targetLocation, GIVE_UP_RANGE)) {
				targetFriendly = null;
				targetLocationf = null;
			}
		}
		
		friendlyDrones = 0;
		
		//rushDetected = false;
		areEnemies = false;

		//Calculate Random
		random = (A*random+B)%256;
		netGunLoc = null;

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
					if(!pathTile(r.location)) {
						distance = Utility.chebyshev(location, r.location);
						if (distance < allyDistance) { //|| && (!pathTile(r.location)) state == DroneState.MINER_ASSIST, kinda hacky, only pick up miner if its not on the path, unless in miner assist mode
							allyToAssist = r;
							allyDistance = distance;
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
					if(!pathTile(r.location)) {
						distance = Utility.chebyshev(location, r.location);
						if (distance < allyDistance) { //|| && (!pathTile(r.location)) state == DroneState.MINER_ASSIST, kinda hacky, only pick up miner if its not on the path, unless in miner assist mode
							allyToAssist = r;
							allyDistance = distance;
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
					netGunLoc = r.location;
					if (!enemyNetguns.contains(r.location))enemyNetguns.add(r.location);
					break;
				case REFINERY:
					// TODO: target?
					break;
				case DESIGN_SCHOOL:
					// TODO: target?
					if (Utility.chebyshev(r.location, hqLocation) < RUSH_RANGE) {
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
				if (targetLocation == null && !rushDetected) {
					targetLocation = r.location;
					targetRobot = r;
				}
			}
		}

		if(enemyHqLocation != null && !sentEHQL) {
			Communications.queueMessage(rc, 3, 3, enemyHqLocation.x, enemyHqLocation.y);
			sentEHQL = true;
		}

		if (!rc.isCurrentlyHoldingUnit()) {
			carryingAssaulter = false;
			carryingEnemy = false;
			carryingAlly = false;
			if (round < TURTLE_ROUND || rushDetected || (rushLocation != null)) {
				state = DroneState.DEFENDING;
			} else {
				state = DroneState.ATTACKING;
			}
			if (round > ASSAULT_ROUND && shouldAssault()) {
				state = DroneState.ASSAULTING;
			}
		}

		if (netGunLoc != null && !rush && netGunLoc.isWithinDistanceSquared(location, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
			if (rc.isReady()) {
				if (fuzzy(rc, this, netGunLoc.directionTo(location))) return;
			}
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

		/*if (state != DroneState.ASSAULTING && enemyHqLocation != null) {
			if (location.distanceSquaredTo(enemyHqLocation) < 8 && targetRobot == null) {
				Communications.queueMessage(rc, 1, 2, enemyHqLocation.x, enemyHqLocation.y);
				state = DroneState.ASSAULTING;
			}
		}*/

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
			if (!areEnemies)turnsSinceRush++;
			else turnsSinceRush=0;
			if (turnsSinceRush > RUSH_SAFE_TURNS) {
				rushLocation = null;
				turnsSinceRush = 0;
				rushDetected = false;
			}
		}
		//if (DroneNav.target != null) rc.setIndicatorDot(DroneNav.target, 255, 0, 255);
		if (nearestWater != null) rc.setIndicatorDot(nearestWater, 0, 255, 255);
		if (targetLocation != null) rc.setIndicatorDot(targetLocation, 255, 0, 0);

	}

	private void doAssist() throws GameActionException {
		if(rc.isCurrentlyHoldingUnit()) {
			if(Utility.chebyshev(location, minerAssistLocation) <= 1) {
				if(rc.canDropUnit(location.directionTo(minerAssistLocation))) {
					rc.dropUnit(location.directionTo(minerAssistLocation));
				}
			}
		} else if(allyToAssist != null) {
			if (Utility.chebyshev(location, allyToAssist.location) <= 1) {
				if (rc.canPickUpUnit(allyToAssist.ID)) {
					if (allyToAssist.type == RobotType.LANDSCAPER) {
						carryingAllyLandscaper = true;
					
					}
					carryingAlly = true;
					rc.pickUpUnit(allyToAssist.ID);
				}
			} else {
				if (DroneNav.target == null || !DroneNav.target.equals(allyToAssist.location)) {
					DroneNav.beginNav(rc, this, allyToAssist.location);
				}
				DroneNav.nav(rc, this);
			}
		} else {
			if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
				DroneNav.beginNav(rc, this, hqLocation);
			}
		}
	}

	private void doAssault() throws GameActionException {
		if (!rush) doInitiateRush();
		if (doDropEnemy()) return;
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
				} else {
					moveScout(rc);
				}
			}
			else {
				if(Utility.chebyshev(location, nearestSafe) <= 1) {
					if(rc.canDropUnit(location.directionTo(nearestSafe))) {
						rc.dropUnit(location.directionTo(nearestSafe));
					}
				} else {
					if (DroneNav.target == null || !DroneNav.target.equals(nearestSafe)) {
						DroneNav.beginNav(rc, this, nearestSafe);
					}
					DroneNav.nav(rc, this);
				}

				/*if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
					DroneNav.beginNav(rc, this, enemyHqLocation);
				}
				DroneNav.nav(rc, this);
				return;*/
			}
		}
		else {
			//scanForWater();
			if(targetFriendly != null) {
				if (Utility.chebyshev(location, targetLocationf) <= 1) {
					if (rc.canPickUpUnit(targetFriendly.ID)) {
						rc.pickUpUnit(targetFriendly.ID);
						carryingAssaulter = true;
					}
				} else {
					if (DroneNav.target == null || !DroneNav.target.equals(targetLocationf)) {
						DroneNav.beginNav(rc, this, targetLocationf);
					}
					DroneNav.nav(rc, this);
				}
			}
			else {
				if (enemyHqLocation != null) {
					if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
						DroneNav.beginNav(rc, this, enemyHqLocation);
					}
					DroneNav.nav(rc, this);
				} else {
					moveScout(rc);
				}
				//state = DroneState.ATTACKING;
			}
		}
	}

	private boolean shouldAssault() {
		return id % 3 == 0;
	}

	private void doInitiateRush() throws GameActionException {
		if (enemyHqLocation != null) {
			if (Utility.chebyshev(location, enemyHqLocation) <= CRUNCH_RANGE) {
				if (friendlyDrones >= DRONE_COUNT_RUSH || (round >= FINAL_CRUNCH_ROUND && round % 50 == 0)) {// || round > FINAL_CRUNCH_ROUND
					if (soup>1)Communications.queueMessage(rc, 1, 4, enemyHqLocation.x, enemyHqLocation.y);
					//rush = true;
				}
			}
		}
	}

	private void scanForSafe() throws GameActionException {
		if (enemyHqLocation == null) return;
		Direction[] dirs = Utility.directions;
		Direction d;
		MapLocation ml;
		MapLocation nearestSafe = null;
		int nearestCSD = 1000;
		int csd;
		for (int i = 8; i-->0;) {
			d = dirs[i];
			ml = enemyHqLocation.add(d);
			if (!rc.canSenseLocation(ml)) continue;
			if (rc.senseRobotAtLocation(ml) != null) continue;
			csd = Utility.chebyshev(location, ml);
			if (csd < nearestCSD) {
				nearestCSD = csd;
				nearestSafe = ml;
			}
		}
		
		this.nearestSafe = nearestSafe;
		
		//scan for a safe spot next to enemy hq
		/*MapLocation ml;
		int rSq = senseRadiusSq;
		int radius = (int)(Math.sqrt(rSq));
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
				rad = dx * dx + dy * dy;
				if (rad > rSq || rad <= 2) continue;
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
	}

	private boolean tryDrown() throws GameActionException {
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

	private boolean tryTransport() throws GameActionException {
		Direction[] directions = Utility.directions;
		Direction d;
		MapLocation ml;
		for (int i = 8; i-->0;) {
			d = directions[i];
			ml = location.add(d);
			if (pathTile(ml)) {
				if (rc.canDropUnit(d) && !rc.senseFlooding(ml)) {
					rc.dropUnit(d);
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean tryDropAssaulter() throws GameActionException {
		if (enemyHqLocation == null) return false;
		Direction[] directions = Utility.directions;
		Direction d;
		MapLocation ml;
		for (int i = 8; i-->0;) {
			d = directions[i];
			ml = location.add(d);
			if (enemyHqLocation.isAdjacentTo(ml)) {
				if (rc.canDropUnit(d)) {
					rc.dropUnit(d);
					return true;
				}
			}
		}
		return false;
	}

	private boolean doTransport() throws GameActionException {
		if(carryingAlly) {
			if  (tryTransport()) return true;
			scanForPath();
			if(nearestPath != null) {

				if (DroneNav.target == null || !DroneNav.target.equals(nearestPath)) {
					DroneNav.beginNav(rc, this, nearestPath);
					
				}
				DroneNav.nav(rc, this);
				return true;
			}
			if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
				DroneNav.beginNav(rc, this, hqLocation);
			}
			DroneNav.nav(rc, this);
			return true;
		} else if(allyToAssist != null) {
			if (Utility.chebyshev(location, allyToAssist.location) <= 1) {
				if (rc.canPickUpUnit(allyToAssist.ID)) {
					rc.pickUpUnit(allyToAssist.ID);
					if (allyToAssist.type == RobotType.LANDSCAPER) {
						carryingAllyLandscaper = true;
					}
					carryingAlly = true;
					return true;
				}
			} else {
				if (DroneNav.target == null || !DroneNav.target.equals(allyToAssist.location)) {
					DroneNav.beginNav(rc, this, allyToAssist.location);
				}
				DroneNav.nav(rc, this);
				return true;
			}
		}/* else {
			if (DroneNav.target == null || !DroneNav.target.equals(minerAssistLocation)) {
				DroneNav.beginNav(rc, this, minerAssistLocation);
			}
		}*/
		//else wander
		//moveScout(rc);
		return false;
	}

	private void scanForWater() throws GameActionException {
		MapLocation ml;
		int rSq = senseRadiusSq;
		int radius = (int)(Math.sqrt(rSq));
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

	private void scanForPath() throws GameActionException {
		MapLocation ml;
		int rSq = senseRadiusSq;
		int radius = (int)(Math.sqrt(rSq));
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
				if (csDist < rad0 && pathTile(ml) && !rc.senseFlooding(ml)) {
					if (rc.senseElevation(ml) < Utility.MAX_HEIGHT_THRESHOLD) {
						if (carryingAllyLandscaper) {
							csDist -= 1000;
						} else {
							csDist += 1000;
						}
					}

					ri = rc.senseRobotAtLocation(ml);

					if (ri == null) {
						rad0 = csDist;
						nearestPath = ml;
					}
				}
			}
		}
	}

	private boolean doDropEnemy() throws GameActionException {
		if(carryingEnemy) {
			//pathfind towards target (water, soup)
			//if any of 8 locations around are flooded, place robot into flood, update nearestWater
			if (tryDrown()) return true;
			scanForWater();
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

	private void doAssistLandscaper() {}

	private void doAttack() throws GameActionException {
		if (!rush) doInitiateRush();
		
		System.out.println("TRYDROP");
		if (doDropEnemy()) return;
		
		System.out.println("NAVEHQ" + enemyHqLocation);
		if (enemyHqLocation != null) {
			if (DroneNav.target == null || !DroneNav.target.equals(enemyHqLocation)) {
				DroneNav.beginNav(rc, this, enemyHqLocation);
			}
			DroneNav.nav(rc, this);
			return;
		}

		System.out.println("SCOUTING");
		moveScout(rc);
	}

	private void doDefense() throws GameActionException {
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
		if (doTransport()) return;


		if (Utility.chebyshev(location, hqLocation) < 5) {
			moveScout(rc);
		} else {
			if (DroneNav.target == null || !DroneNav.target.equals(hqLocation)) {
				DroneNav.beginNav(rc, this, hqLocation);
			}
			DroneNav.nav(rc, this);
		}
	}

	private void moveScout(RobotController rc) throws GameActionException {
		if (enemyHqLocation == null) {
			System.out.println("scout");
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
	}

	private void scoutEnemyHq() throws GameActionException {
		System.out.println("Scouting enemy HQ");

		if (rc.isCurrentlyHoldingUnit()) {
			Direction[] directions = Utility.directions;
			Direction direction;

			for (int i = 8; i-->0;) {
				direction = directions[i];
				if (rc.senseFlooding(location.add(direction))) {
					rc.dropUnit(direction);
					return;
				}
			}
		}

		RobotInfo robot;
		//RobotInfo[] nearbyRobots = super.nearbyRobots;

		/*for (int i = nearbyRobots.length; i-->0;) {
			robot = nearbyRobots[i];
			if (robot.team == team.opponent() && rc.canPickUpUnit(robot.getID())) {
				rc.pickUpUnit(robot.getID());
				return;
			}
		}*/

		enemyHqScoutingLocation = enemyHqScouting.getLocation(hqLocation, mapWidth, mapHeight);
		if (rc.canSenseLocation(enemyHqScoutingLocation)) {
			robot = rc.senseRobotAtLocation(enemyHqScoutingLocation);
			if (robot != null && robot.team != team && robot.type == RobotType.HQ) {
				// Enemy
				foundEnemyHq(enemyHqScoutingLocation);
				return;
			} else {
				// Not enemy
				switch (enemyHqScouting) {
				case X_FLIP:
					// Checked our first position
					enemyHqScouting = EnemyHqPossiblePosition.ROTATION;
					DroneNav.beginNav(rc, this, enemyHqScouting.getLocation(hqLocation, mapWidth, mapHeight));
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
		// TODO: Announce that we found enemy HQ
		// TODO: What state should it transition to?
		enemyHqLocation = location;
		state = DroneState.ASSAULTING;
		enemyHqScoutingLocation = null;
		System.out.println("======== Found it!!! ========");
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
		if (!rush ) {
			if (enemyHqScoutingLocation != null) {
				if (enemyHqScoutingLocation.isWithinDistanceSquared(ml,GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
					return false;
				}
			}
			ArrayList<MapLocation> enemyGuns = enemyNetguns;
			MapLocation enemyGun;
			for (int i = enemyGuns.size(); i-->0;) {
				enemyGun = enemyGuns.get(i);
				//if (enemyGun.isWithinDistanceSquared(location, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) return false;
				if (enemyGun.isWithinDistanceSquared(ml, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
					return false;
				}

			}
		}
		return rc.canMove(d);
	}

	public boolean fuzzy(RobotController rc, DeliveryDroneRobot r, Direction d) throws GameActionException {



		if (rc.canMove(d)) {
			rc.move(d);
			return true;
		}
		Direction dr = d.rotateRight();
		Direction dl = d.rotateLeft();
		if (rc.canMove(dr)) {
			rc.move(dr);
			return true;
		}
		if (rc.canMove(dl)) {
			rc.move(dl);
			return true;
		}


		return false;
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
		case 15:
			//MapLocation ml15 = new MapLocation(x,y);
			
		}
	}

}

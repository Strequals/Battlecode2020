package rw7;

import java.util.ArrayList;

import battlecode.common.*;

public strictfp class DeliveryDroneRobot extends Robot {

	private enum DroneState {
		ATTACKING,
		DEFENDING,
		TRANSPORTING
	}

	private MapLocation homeLocation;
	private MapLocation targetLocation;  //building or location to clear enemy robots from
	private RobotInfo targetRobot;
	private MapLocation nearestWater;
	private DroneState state;
	public boolean rush = false; //avoid netguns + hq?
	private boolean sentEHQL = false;
	private Direction lastDirection;
	private int friendlyDrones;
	
	public int random; // A random number from 0 to 255
	public static final int A = 623;
	public static final int B = 49;
	
	public ArrayList<MapLocation> enemyNetguns;

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
		
	}

	@Override
	public void run() throws GameActionException {
		// TODO Auto-generated method stub
		// Process nearby robots (may have to move this into the if statements below)
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		targetRobot = null;
		targetLocation = null;
		friendlyDrones = 0;
		int targetDistance = 10000;
		int distance;
		
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
				case DELIVERY_DRONE:
					friendlyDrones++;
					break;
				}
			} else if (r.getTeam() != Team.NEUTRAL) {
				// Enemy Units
				switch (r.getType()) {
				case MINER:
					// TODO: Block or bury
					distance = Utility.chebyshev(location, r.location);
					if (distance < targetDistance) {
						targetLocation = r.location;
						targetDistance = distance;
						targetRobot = r;
					}
					break;
				case LANDSCAPER:
					distance = Utility.chebyshev(location, r.location);
					if (distance < targetDistance) {
						targetLocation = r.location;
						targetDistance = distance;
						targetRobot = r;

					}
					break;
				case NET_GUN:
					// Avoid
					if (!enemyNetguns.contains(r.location))enemyNetguns.add(r.location);
					break;
				case REFINERY:
					// TODO: target?
					break;
				case DESIGN_SCHOOL:
					// TODO: target?
					break;
				case HQ:
					// We found it!
					//also avoid
					enemyHqLocation = r.location;
					if (!enemyNetguns.contains(r.location))enemyNetguns.add(r.location);
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
		if(enemyHqLocation != null && !sentEHQL) {
			Communications.queueMessage(rc, 3, 3, enemyHqLocation.x, enemyHqLocation.y);
		}

		if(nearestWater == null || !rc.isReady()) {
			//scan for water
			scanForWater();
		}
		
		if (round < TURTLE_END) {
			state = DroneState.DEFENDING;
		} else {
			state = DroneState.ATTACKING;
		}

		if (!rc.isReady()) 
			return;
		
		
		
		switch (state) {
		case DEFENDING:
			doDefense();
			break;
		case ATTACKING:
			doAttack();
			break;
		}
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
					rad0 = rad;
					nearestWater = ml;
				}
			}
		}
	}
	
	public void doAttack() throws GameActionException {
		if (enemyHqLocation != null) {
			if (Utility.chebyshev(location, enemyHqLocation) < 5) {
				if (round % 100 == 49 && friendlyDrones > 5) {
					if (soup > 2) Communications.queueMessage(rc, 2, 4, enemyHqLocation.x, enemyHqLocation.y);
				}
			}
		}
		
		if(rc.isCurrentlyHoldingUnit()) {
			//pathfind towards target (water, soup)
			//if any of 8 locations around are flooded, place robot into flood, update nearestWater

			
			if(nearestWater != null) {
				if(Utility.chebyshev(location, nearestWater) <= 2) {
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
			if (Utility.chebyshev(location, targetLocation) <= 1) {
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
		}
		
		
		if (enemyHqLocation != null) {
			if (Utility.chebyshev(location, enemyHqLocation) < 5) {
				if (round % 100 == 50 && friendlyDrones > 5) {
					rush = true;
				}
			}
			
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
		
		if(rc.isCurrentlyHoldingUnit()) {
			//pathfind towards target (water, soup)
			//if any of 8 locations around are flooded, place robot into flood, update nearestWater

			
			if(nearestWater != null) {
				if(Utility.chebyshev(location, nearestWater) <= 2) {
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
			if (Utility.chebyshev(location, targetLocation) <= 1) {
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
		}
		
		
			if (Utility.chebyshev(location, hqLocation) < 5) {
				moveScout(rc);
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
		if (!rush) {
		ArrayList<MapLocation> enemyGuns = enemyNetguns;
		MapLocation enemyGun;
		for (int i = enemyGuns.size(); i-->0;) {
			enemyGun = enemyGuns.get(i);
			if (enemyGun.isWithinDistanceSquared(ml, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)) {
				return false;
			}
		}
		}
		return rc.canMove(d);
	}

	@Override
	public void processMessage(int m, int x, int y) {
		// TODO Auto-generated method stub
		switch (m) {                                       //will probably want to add a case for attack
		case 1:
			hqLocation = new MapLocation(x,y);
			System.out.println("Received HQ location: " + x + ", " + y);
			sentEHQL = true; //if already received enemy hq location, don't rebroadcast
			break;
		case 3:
			if (enemyHqLocation == null) {
				enemyHqLocation = new MapLocation(x, y);
				enemyNetguns.add(enemyHqLocation);
			}
			break;
		case 4:
			if (enemyHqLocation == null) {
				enemyHqLocation = new MapLocation(x, y);
				enemyNetguns.add(enemyHqLocation);
			}
			if (Utility.chebyshev(enemyHqLocation, location) < 6) {
				rush = true;
			}
		}
	}

}

package rw2;

import java.util.HashSet;
import java.util.Iterator;

import battlecode.common.*;

public strictfp class MinerRobot extends Robot {

	public MapLocation hqLocation;
	public MinerState minerState;
	public LocationData seekSoup;
	public boolean navigatingHQ;
	public HashSet<LocationData> soupLocations;

	enum MinerState {
		SEEKING, MINING, RETURNING
	}

	public MinerRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
		minerState = MinerState.SEEKING;
		navigatingHQ = false;
		soupLocations = new HashSet<LocationData>();
	}

	@Override
	public void run() throws GameActionException {
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		int nearbyMiners = 0;
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

		//update and broadcast soup locations
		int radius = (int)(Math.sqrt(senseRadiusSq));
		int dx;
		int dy;
		MapLocation ml;
		int bytecode1 = Clock.getBytecodesLeft();
		int soup;
		LocationData ld;
		for (int x = Math.max(0, location.x - radius); x <= Math.min(mapWidth - 1, location.x + radius); x++) {
			for (int y = Math.max(0, location.y - radius); y <= Math.min(mapHeight - 1, location.y + radius); y++) {
				dx = x - location.x;
				dy = y - location.y;
				if (dx * dx + dy * dy > senseRadiusSq) continue;
				ml = new MapLocation(x, y);
				soup = rc.senseSoup(ml);
				ld = new LocationData(ml, soup);
				if (soup > 0 && !(rc.senseFlooding(ml))) {// && rc.senseFlooding(ml.add(Direction.NORTH)) && rc.senseFlooding(ml.add(Direction.NORTHEAST)) && rc.senseFlooding(ml.add(Direction.EAST)) && rc.senseFlooding(ml.add(Direction.SOUTHEAST)) && rc.senseFlooding(ml.add(Direction.SOUTH)) && rc.senseFlooding(ml.add(Direction.SOUTHWEST)) && rc.senseFlooding(ml.add(Direction.WEST)) && rc.senseFlooding(ml.add(Direction.NORTHWEST)))) {
					System.out.println("V"+Clock.getBytecodeNum());
						if (soupLocations.add(ld)) {
							Communications.queueMessage(1, 3, ld);
						}
					System.out.println("X"+Clock.getBytecodeNum());
				} else {
					System.out.println("X"+Clock.getBytecodeNum());
						if (soupLocations.remove(ld)) {
							Communications.queueMessage(1, 4, ld);
						}
					System.out.println("D"+Clock.getBytecodeNum());
				}
			}
		}
		System.out.println("SEARCH bytecodes: " + (bytecode1 - Clock.getBytecodesLeft()));

		if (cooldownTurns >= 1) return;

		//if (soupMine != null)System.out.println("TARGETING: " + soupMine.x + ", " + soupMine.y);
		switch (minerState) {
		case MINING:
			System.out.println("MINING");
			MapLocation soupMine = null;

			int soupLeft = 0;
			if (senseRadiusSq > 1) {
				int s;
				for (int x = Math.max(0, location.x - 1); x <= Math.min(mapWidth - 1, location.x + 1); x++) {
					for (int y = Math.max(0, location.y - 1); y <= Math.min(mapHeight - 1, location.y + 1); y++) {
						ml = new MapLocation(x, y);
						s = rc.senseSoup(ml);
						if (s > soupLeft) {
							soupMine = ml;
							soupLeft = s;
						}
					}
				}
			} else if (senseRadiusSq == 1) {
				int s;
				for (int x = Math.max(0, location.x - 1); x <= Math.min(mapWidth - 1, location.x + 1); x++) {
					for (int y = Math.max(0, location.y - 1); y <= Math.min(mapHeight - 1, location.y + 1); y++) {
						if ((x < 0 ? -x : x) + (y < 0 ? -y : y) > 1) continue;
						ml = new MapLocation(x, y);
						s = rc.senseSoup(ml);
						if (s > soupLeft) {
							soupMine = ml;
							soupLeft = s;
						}
					}
				}
			} else {
				System.out.println("cannot sense locations");
				return;
			}
			if (soupMine != null) {
				System.out.println("CARRYING " + soupCarrying +" / " + type.soupLimit + " SOUPS");
				rc.mineSoup(location.directionTo(soupMine));
				if (soupCarrying + Math.min(soupLeft, GameConstants.SOUP_MINING_RATE) >= type.soupLimit) {
					minerState = MinerState.RETURNING;
				}
				break;
			}
			//this code is executed if mining fails
			minerState = MinerState.SEEKING;
		case SEEKING:
			System.out.println("SEEKING");

			if (soupLocations.isEmpty()) {
				//random walk boi
				System.out.println("random walk");
				seekSoup = null;
				fuzzy(rc, Utility.directions[(int) (Math.random() * 8)]);
				return;
			} else if (seekSoup == null || !soupLocations.contains(seekSoup)) {
				seekSoup = soupLocations.iterator().next();
				Nav.beginNav(rc, this, seekSoup.location);
				Nav.nav(rc, this);
			} else {
				Nav.nav(rc, this);
			}

			Iterator<LocationData> soups = soupLocations.iterator();
			LocationData sl = null;
			while (soups.hasNext()) {
				sl = soups.next();
				if (sl.location.isAdjacentTo(location)) {
					minerState = MinerState.MINING;
					break;
				}
			}
			/*if (rc.canSenseLocation(soupMine)) {
				if (rc.senseSoup(soupMine) == 0) {
					soupMine = null;
				} else {
					//move towards mine
					//fuzzy(rc, location.directionTo(soupMine));
					if (Nav.target != soupMine) {
						Nav.beginNav(rc, this, soupMine);
					}
					Nav.nav(rc, this);
					if (location.distanceSquaredTo(soupMine) <= 2) {
						minerState = MinerState.MINING;
					}
					return;
				}
			} else {
				//move towards mine
				//fuzzy(rc, location.directionTo(soupMine));
				Nav.nav(rc, this);
				if (location.distanceSquaredTo(soupMine) <= 2) {
					minerState = MinerState.MINING;
				}
				return;
			}*/


			break;
		case RETURNING:
			System.out.println("RETURNING");
			if (!navigatingHQ) {
				Nav.beginNav(rc, this, hqLocation);
				navigatingHQ = true;
			}

			if (hqLocation != null) {
				Direction dirToHq = location.directionTo(hqLocation);
				if (location.distanceSquaredTo(hqLocation) <= 2) {
					rc.depositSoup(dirToHq, rc.getSoupCarrying());
					minerState = MinerState.SEEKING;
					navigatingHQ = false;
					return;
				}
				Nav.nav(rc, this);
				return;
			}

		}
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

	@Override
	public void processMessage(int m, int v, int x, int y) {
		LocationData ld = new LocationData(new MapLocation(x,y), v);
		switch (m) {
		case 1:
			hqLocation = new MapLocation(x,y);
			System.out.println("Recieved HQ location: " + x + ", " + y);
			break;
		case 3:
			int bc = Clock.getBytecodeNum();
			if (!soupLocations.contains(ld)) soupLocations.add(ld);
			System.out.println("got soup location with bc " + (Clock.getBytecodeNum() - bc));
			rc.setIndicatorDot(ld.location, 0, 0, 255);
			break;
		case 4:
			if (soupLocations.contains(ld)) soupLocations.remove(ld);
			rc.setIndicatorDot(ld.location, 255, 0, 0);
		}

	}
}

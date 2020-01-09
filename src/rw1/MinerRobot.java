package rw1;

import battlecode.common.*;

public strictfp class MinerRobot extends Robot {

	public MapLocation hqLocation;
	public MinerState minerState;
	public MapLocation soupMine;

	enum MinerState {
		SEEKING, MINING, RETURNING
	}

	public MinerRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
		minerState = MinerState.SEEKING;
		
	}

	@Override
	public void run() throws GameActionException {
		RobotInfo[] ri = nearbyRobots;
		RobotInfo r;
		for (int i = ri.length; --i >= 0;) {
			r = ri[i];
			if (r.getTeam() == team) {
				//Friendly Units
				switch (r.getType()) {
				case HQ:
					hqLocation = r.getLocation();
					break;
				case MINER:
					
					break;
				default:
					break;

				}
			} else if (r.getTeam() == Team.NEUTRAL) {
				//yeet the cow
				if (round > 100) {
					//Call the drones
					Communications.sendMessage(rc);
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

		if (cooldownTurns >= 1) return;

		switch (minerState) {
		case MINING:
			int soupLeft = rc.senseSoup(soupMine);
			if (soupLeft == 0) {
				soupMine = null;

				//Check adjacent locations for soup deposits
				MapLocation ml;
				if (senseRadiusSq > 1) {
					search: for (int x = Math.max(0, location.x - 1); x <= Math.min(mapWidth - 1, location.x + 1); x++) {
						for (int y = Math.max(0, location.y - 1); y <= Math.min(mapHeight - 1, location.y + 1); y++) {
							ml = new MapLocation(x, y);
							if (rc.senseSoup(ml) > 0 && !rc.senseFlooding(ml)) {
								soupMine = ml;
								soupLeft = rc.senseSoup(soupMine);
								break search;
							}
						}
					}
				} else if (senseRadiusSq == 1) {
					search: for (int x = Math.max(0, location.x - 1); x <= Math.min(mapWidth - 1, location.x + 1); x++) {
						for (int y = Math.max(0, location.y - 1); y <= Math.min(mapHeight - 1, location.y + 1); y++) {
							if ((x < 0 ? -x : x) + (y < 0 ? -y : y) > 1) continue;
							ml = new MapLocation(x, y);
							if (rc.senseSoup(ml) > 0 && !rc.senseFlooding(ml)) {
								soupMine = ml;
								soupLeft = rc.senseSoup(soupMine);
								break search;
							}
						}
					}
				}
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
			//search for a soup deposit, check optimal soup deposit within radius
			if (soupMine == null) {
			int rSq = senseRadiusSq;
			int radius = (int)(Math.sqrt(rSq));
			MapLocation ml;
			int dx;
			int dy;
			search: for (int x = Math.max(0, location.x - radius); x <= Math.min(mapWidth - 1, location.x + radius); x++) {
				for (int y = Math.max(0, location.y - radius); y <= Math.min(mapHeight - 1, location.y + radius); y++) {
					dx = x - location.x;
					dy = y - location.y;
					if (dx * dx + dy * dy > rSq) continue;
					ml = new MapLocation(x, y);
					if (rc.senseSoup(ml) > 0 && !rc.senseFlooding(ml)) {
						soupMine = ml;
						break search;
					}
				}
			}
			if (soupMine == null) {
				//random walk boi
				fuzzy(rc, Utility.directions[(int) (Math.random() * 8)]);
				return;
			} else {
				Nav.beginNav(rc, this, soupMine);
			}
			}
			if (rc.canSenseLocation(soupMine)) {
				if (rc.senseSoup(soupMine) == 0) {
					soupMine = null;
				} else {
					//move towards mine
					//fuzzy(rc, location.directionTo(soupMine));
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
			}


			break;
		case RETURNING:
			if (hqLocation != null) {
				Direction dirToHq = location.directionTo(hqLocation);
				if (location.distanceSquaredTo(hqLocation) <= 2) {
					rc.depositSoup(dirToHq, rc.getSoupCarrying());
					minerState = MinerState.SEEKING;
					return;
				}
				fuzzy(rc, dirToHq);
				return;
			}

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
}

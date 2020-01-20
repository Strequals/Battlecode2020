package rw7;

import battlecode.common.*;

public class Nav {
	enum BugState {
		MOTION_TO_GOAL, BUGGING
	}
	enum BugSide {
		LEFT, RIGHT
	}

	public static BugState state;
	public static BugSide side;

	public static MapLocation target;
	public static MapLocation position;
	public static Direction bugDirection;
	public static Direction lookDirection;
	public static int movesSinceObstacle;
	public static int rotations;
	public static int startDistanceSq;
	public static int turnsBugged;

	//Cory Li's Bug Algorithm - https://github.com/TheDuck314/battlecode2015/blob/master/teams/zephyr26_final/Nav.java
	public static void beginNav(RobotController rc, Robot r, MapLocation ml) {
		target = ml;
		position = r.location;
		state = BugState.MOTION_TO_GOAL;
	}

	public static void nav(RobotController rc, Robot r) throws GameActionException{
		position = r.location;
		if (state == BugState.BUGGING && canEndBug()) {
			state = BugState.MOTION_TO_GOAL;
		}

		switch (state) {
		case MOTION_TO_GOAL:
			System.out.println("MTG");
			if (tryDirect(rc)) {
				return;
			}
			state = BugState.BUGGING;
			startBug(rc);
		case BUGGING:
			System.out.println("BUG");
			int c1 = Clock.getBytecodesLeft();
			bug(r, rc);
		}
	}

	public static boolean canMove(RobotController rc, Direction d) throws GameActionException {
		MapLocation ml = rc.adjacentLocation(d);
		return rc.canMove(d) && !rc.senseFlooding(ml);
	}

	public static void startBug(RobotController rc) throws GameActionException {
		startDistanceSq = position.distanceSquaredTo(target);
		bugDirection = position.directionTo(target);
		lookDirection = bugDirection;
		rotations = 0;
		movesSinceObstacle = 0;
		turnsBugged = 0;

		Direction left = bugDirection.rotateLeft();
		for (int i = 0; i < 3; i++) {
			if (canMove(rc, left)) break;
			left = left.rotateLeft();

		}

		Direction right = bugDirection.rotateRight();
		for (int i = 0; i < 3; i++) {
			if (canMove(rc, right)) break;
			right = right.rotateRight();
		}

		if (position.add(left).distanceSquaredTo(target) <= position.add(right).distanceSquaredTo(target)) {
			side = BugSide.RIGHT;
		} else {
			side = BugSide.LEFT;
		}
	}

	public static void bug(Robot r, RobotController rc) throws GameActionException {
		
		if (detectEdge(r, rc)) {
			startBug(rc);
		}
		turnsBugged++;
		movesSinceObstacle++;
		Direction d = lookDirection;
		int i = 0;
		wiggle: switch (side) {
		case LEFT:
			for (i = 8; i-->0;) {
				if (canMove(rc, d)) break wiggle;
				d = d.rotateRight();
				movesSinceObstacle = 0;
			}
			d = null;
			break;
		case RIGHT:
			for (i = 8; i-->0;) {
				if (canMove(rc, d)) break wiggle;
				d = d.rotateLeft();
				movesSinceObstacle = 0;
			}
			d = null;
		}
		
		int rots = 7 - i;
		
		if (d != null) {
			rc.move(d);
			
			switch (side) {
			case LEFT:
				rotations += rots - ((bugDirection.ordinal()-lookDirection.ordinal() + 8) % 8);
				lookDirection = d.rotateLeft().rotateLeft();
				break;
			case RIGHT:
				rotations += rots - ((lookDirection.ordinal()-bugDirection.ordinal() + 8) % 8);
				lookDirection = d.rotateRight().rotateRight();
			}
		
			bugDirection = d;
		}
		
		


	}

	public static boolean canEndBug() {
		System.out.println("MSO:"+movesSinceObstacle);
		System.out.println("rotations:"+rotations);
		if (movesSinceObstacle >= 4 || turnsBugged > 100) return true;
		return (rotations < 0 || rotations >= 8) && position.isWithinDistanceSquared(target, startDistanceSq);
	}

	public static boolean detectEdge(Robot r, RobotController rc) {
		//TODO: add edge detection
		MapLocation ml = position.add(bugDirection.rotateLeft());
		if (side == BugSide.LEFT) {
			ml = position.add(bugDirection.rotateLeft());
		} else {
			ml = position.add(bugDirection.rotateRight());
		}
		return (ml.x < 0 || ml.x >= r.mapWidth || ml.y < 0 || ml.y >= r.mapHeight);
	}

	public static boolean tryDirect(RobotController rc) throws GameActionException {
		if (fuzzy(rc, position.directionTo(target))) {
			return true;
		}
		return false;
	}

	public static boolean fuzzy(RobotController rc, Direction d) throws GameActionException {
		int dsq = position.distanceSquaredTo(target);
		if (rc.canMove(d) && !rc.senseFlooding(rc.adjacentLocation(d))) {
			rc.move(d);
			return true;
		}
		Direction dr = d.rotateRight();
		MapLocation mlr = rc.adjacentLocation(dr);
		int dsqr = mlr.distanceSquaredTo(target);
		Direction dl = d.rotateLeft();
		MapLocation mll = rc.adjacentLocation(dl);
		int dsql = mll.distanceSquaredTo(target);
		if (dsqr <= dsql) {
			if (rc.canMove(dr) && !rc.senseFlooding(mlr) && mlr.isWithinDistanceSquared(target, dsq)) {
				rc.move(dr);
				return true;
			} else if (rc.canMove(dl) && !rc.senseFlooding(mll) && mll.isWithinDistanceSquared(target, dsq)) {
				rc.move(dl);
				return true;
			}
		} else if (rc.canMove(dl) && !rc.senseFlooding(mll) && mll.isWithinDistanceSquared(target, dsq)) {
			rc.move(dl);
			return true;
		} else if (rc.canMove(dr) && !rc.senseFlooding(mlr) && mlr.isWithinDistanceSquared(target, dsq)) {
			rc.move(dr);
			return true;
		}


		return false;
	}

}

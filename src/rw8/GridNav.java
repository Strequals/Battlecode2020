package rw8;

import javax.crypto.spec.RC2ParameterSpec;

import battlecode.common.*;

public class GridNav {
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
	public static void beginNav(LandscaperRobot r, MapLocation ml) {
		target = ml;
		position = r.location;
		state = BugState.MOTION_TO_GOAL;
	}

	public static void nav(RobotController rc, LandscaperRobot r) throws GameActionException{
		position = r.location;
		rc.setIndicatorLine(position, target, 255, 0, 255);
		if (state == BugState.BUGGING && canEndBug()) {
			state = BugState.MOTION_TO_GOAL;
		}

		switch (state) {
		case MOTION_TO_GOAL:
			//System.out.println("MTG");
			if (tryDirect(rc, r)) {
				return;
			}
			state = BugState.BUGGING;
			startBug(r);
		case BUGGING:
			//System.out.println("BUG");
			bug(r, rc);
		}
	}

	public static boolean canMove(LandscaperRobot r, Direction d) throws GameActionException {
		return r.canMove(d);
		
//		if (r.location.isWithinDistanceSquared(r.hqLocation, 8)) {
//			return r.canMove(d) && (r.hqFill == null || r.rc.senseElevation(r.location.add(d)) < Utility.MAX_HEIGHT_THRESHOLD);
//		}
//		else return r.canMove(d) && r.pathTile(r.location.add(d));
	}

	public static void startBug(LandscaperRobot r) throws GameActionException {
		startDistanceSq = position.distanceSquaredTo(target);
		bugDirection = position.directionTo(target);
		lookDirection = bugDirection;
		rotations = 0;
		movesSinceObstacle = 0;
		turnsBugged = 0;

		Direction left = bugDirection.rotateLeft();
		for (int i = 0; i < 3; i++) {
			if (canMove(r, left)) break;
			left = left.rotateLeft();
		}

		Direction right = bugDirection.rotateRight();
		for (int i = 0; i < 3; i++) {
			if (canMove(r, right)) break;
			right = right.rotateRight();
		}

		if (position.add(left).distanceSquaredTo(target) <= position.add(right).distanceSquaredTo(target)) {
			side = BugSide.RIGHT;
		} else {
			side = BugSide.LEFT;
		}
	}

	public static void bug(LandscaperRobot r, RobotController rc) throws GameActionException {
		if (detectEdge(r)) {
			startBug(r);
		}
		turnsBugged++;
		movesSinceObstacle++;
		Direction d = lookDirection;
		int i = 0;
		wiggle: switch (side) {
		case LEFT:
			for (i = 8; i-->0;) {
				if (canMove(r, d)) break wiggle;
				d = d.rotateRight();
				movesSinceObstacle = 0;
			}
			d = null;
			break;
		case RIGHT:
			for (i = 8; i-->0;) {
				if (canMove(r, d)) break wiggle;
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
		//System.out.println("MSO:"+movesSinceObstacle);
		//System.out.println("rotations:"+rotations);
		if (movesSinceObstacle >= 4) return true;
		return (rotations < 0 || rotations >= 8) && position.isWithinDistanceSquared(target, startDistanceSq-1);
	}

	public static boolean detectEdge(LandscaperRobot r) {
		// TODO: add edge detection
		MapLocation ml;
		if (side == BugSide.LEFT) {
			ml = position.add(bugDirection.rotateLeft());
		} else {
			ml = position.add(bugDirection.rotateRight());
		}
		return (ml.x < 0 || ml.x >= r.mapWidth || ml.y < 0 || ml.y >= r.mapHeight);
	}

	public static boolean tryDirect(RobotController rc, LandscaperRobot r) throws GameActionException {
		return fuzzy(rc, r, position.directionTo(target));
	}

	public static boolean fuzzy(RobotController rc, LandscaperRobot r, Direction d) throws GameActionException {
		position = r.location;
		
		if (target != null) {
			
			int dsq = position.distanceSquaredTo(target);
			if (canMove(r, d)) {
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
				if (canMove(r, dr) && mlr.isWithinDistanceSquared(target, dsq)) {
					rc.move(dr);
					return true;
				} else if (canMove(r, dl) && mll.isWithinDistanceSquared(target, dsq)) {
					rc.move(dl);
					return true;
				}
			} else if (canMove(r, dl) && mll.isWithinDistanceSquared(target, dsq)) {
				rc.move(dl);
				return true;
			} else if (canMove(r, dr) && mlr.isWithinDistanceSquared(target, dsq)) {
				rc.move(dr);
				return true;
			}


			return false;
		}
		if (canMove(r, d)) {
			rc.move(d);
			return true;
		}
		Direction dr = d.rotateRight();
		Direction dl = d.rotateLeft();
		if (canMove(r, dr)) {
			rc.move(dr);
			return true;
		} else if (canMove(r, dl)) {
			rc.move(dl);
			return true;
		}


		return false;
	}

}

package rw7;

import java.util.ArrayList;

import battlecode.common.*;

public class DroneNav {
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
	public static void beginNav(RobotController rc, DeliveryDroneRobot r, MapLocation ml) {
		target = ml;
		position = r.location;
		state = BugState.MOTION_TO_GOAL;
	}

	public static void nav(RobotController rc, DeliveryDroneRobot r) throws GameActionException{
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
			startBug(rc, r);
		case BUGGING:
			//System.out.println("BUG");
			int c1 = Clock.getBytecodesLeft();
			bug(r, rc);
		}
	}

	public static boolean canMove(RobotController rc, DeliveryDroneRobot r, Direction d) throws GameActionException {

		return r.canMove(rc, d);
	}

	public static void startBug(RobotController rc, DeliveryDroneRobot r) throws GameActionException {
		startDistanceSq = position.distanceSquaredTo(target);
		bugDirection = position.directionTo(target);
		lookDirection = bugDirection;
		rotations = 0;
		movesSinceObstacle = 0;
		turnsBugged = 0;

		Direction left = bugDirection.rotateLeft();
		for (int i = 0; i < 3; i++) {
			if (canMove(rc, r, left)) break;
			left = left.rotateLeft();

		}

		Direction right = bugDirection.rotateRight();
		for (int i = 0; i < 3; i++) {
			if (canMove(rc, r, right)) break;
			right = right.rotateRight();
		}

		if (position.add(left).distanceSquaredTo(target) <= position.add(right).distanceSquaredTo(target)) {
			side = BugSide.RIGHT;
		} else {
			side = BugSide.LEFT;
		}
	}

	public static void bug(DeliveryDroneRobot r, RobotController rc) throws GameActionException {

		if (detectEdge(r, rc)) {
			startBug(rc, r);
		}
		turnsBugged++;
		movesSinceObstacle++;
		Direction d = lookDirection;
		int i = 0;
		wiggle: switch (side) {
		case LEFT:
			for (i = 8; i-->0;) {
				if (canMove(rc, r, d)) break wiggle;
				d = d.rotateRight();
				movesSinceObstacle = 0;
			}
			d = null;
			break;
		case RIGHT:
			for (i = 8; i-->0;) {
				if (canMove(rc, r, d)) break wiggle;
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

	public static boolean detectEdge(DeliveryDroneRobot r, RobotController rc) {
		//TODO: add edge detection
		MapLocation ml = position.add(bugDirection.rotateLeft());
		if (side == BugSide.LEFT) {
			ml = position.add(bugDirection.rotateLeft());
		} else {
			ml = position.add(bugDirection.rotateRight());
		}
		return (ml.x < 0 || ml.x >= r.mapWidth || ml.y < 0 || ml.y >= r.mapHeight);
	}

	public static boolean tryDirect(RobotController rc, DeliveryDroneRobot r) throws GameActionException {
		if (fuzzy(rc, r, position.directionTo(target))) {
			return true;
		}
		return false;
	}

	public static boolean fuzzy(RobotController rc, DeliveryDroneRobot r, Direction d) throws GameActionException {
		position = r.location;
		
		if (target != null) {
			
			int dsq = position.distanceSquaredTo(target);
			if (canMove(rc, r, d)) {
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
				if (canMove(rc, r, dr) && mlr.isWithinDistanceSquared(target, dsq)) {
					rc.move(dr);
					return true;
				} else if (canMove(rc, r, dl) && mll.isWithinDistanceSquared(target, dsq)) {
					rc.move(dl);
					return true;
				}
			} else if (canMove(rc, r, dl) && mll.isWithinDistanceSquared(target, dsq)) {
				rc.move(dl);
				return true;
			} else if (canMove(rc, r, dr) && mlr.isWithinDistanceSquared(target, dsq)) {
				rc.move(dr);
				return true;
			}


			return false;
		}
		if (canMove(rc, r, d)) {
			rc.move(d);
			return true;
		}
		Direction dr = d.rotateRight();
		Direction dl = d.rotateLeft();
		if (canMove(rc, r, dr)) {
			rc.move(dr);
			return true;
		} else if (canMove(rc, r, dl)) {
			rc.move(dl);
			return true;
		}


		return false;
	}

}

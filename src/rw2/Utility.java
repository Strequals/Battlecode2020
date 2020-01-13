package rw2;

import battlecode.common.*;

public strictfp class Utility {
	public static final Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	public static Symmetry getSymmetry(RobotController rc) {
		
		return Symmetry.ROTATIONAL;
	}
	
	

}

package rw1;

import battlecode.common.*;

public strictfp class Utility {
	public static final Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	public static final Direction[] directionsC = {Direction.CENTER, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};

	
	public static Symmetry getSymmetry(RobotController rc) {
		
		return Symmetry.ROTATIONAL;
	}
	
	public static int chebyshev(MapLocation a, MapLocation b) {
		int x = a.x-b.x;
		int y = a.y-b.y;
		if (x < 0) x = -x;
		if (y < 0) y = -y;
		return (x > y ? x : y);
	}
	
	

}

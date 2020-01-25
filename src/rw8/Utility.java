package rw8;

import battlecode.common.*;

public strictfp class Utility {
	public static final Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	public static final Direction[] directionsC = {Direction.CENTER, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};

	// These came from LandscaperRobot. Should better integrate them here.
	static final int TERRAFORM_THRESHOLD = 100; //If change in elevation is greater, do not terraform this tile
	static final int MAX_TERRAFORM_RANGE = 2; //Chebyshev distance to limit
//	static final int TERRAFORM_RANGE_SQ = 8; //2x^2
	static final int TERRAFORM_RANGE_SQ = 6;
	static final int MAX_HEIGHT_THRESHOLD = 8; //don't try to build up to unreachable heights
	static final int BACKUP_THRESHOLD = 8;
	static final int MAX_NEARBY_TERRAFORMERS = 5;//Don't communicate location if there are 5 or more terraformers nearby

	static final int TERRAFORM_HOLES_EVERY = 2;

	// From DeliveryDroneRobot. Should better integrate here.
	static final int[] MOVE_CHANCE_BREAKPOINTS = new int[] { 65, 80, 87, 94, 97, 100 };
	
	public static int chebyshev(MapLocation a, MapLocation b) {
		int x = a.x-b.x;
		int y = a.y-b.y;
		if (x < 0) x = -x;
		if (y < 0) y = -y;
		return x > y ? x : y;
	}
}

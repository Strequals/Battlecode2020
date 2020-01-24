package rw8;

import battlecode.common.MapLocation;

public enum Symmetry {
	HORIZONTAL, VERTICAL, ROTATIONAL
}

enum EnemyHqPossiblePosition {
	X_FLIP,
	Y_FLIP,
	ROTATION;

	MapLocation getLocation(MapLocation hqLocation, int mapWidth, int mapHeight) {
		int enemyHqX;
		int enemyHqY;

		int maxXPosition;
		int maxYPosition;

		switch (this) {
			case X_FLIP:
				maxXPosition = mapWidth - 1;
				enemyHqX = maxXPosition - hqLocation.x;
				enemyHqY = hqLocation.y;
				break;
			case Y_FLIP:
				maxYPosition = mapHeight - 1;
				enemyHqX = hqLocation.x;
				enemyHqY = maxYPosition - hqLocation.y;
				break;
			case ROTATION:
				maxXPosition = mapWidth - 1;
				maxYPosition = mapHeight - 1;
				enemyHqX = maxXPosition - hqLocation.x;
				enemyHqY = maxYPosition - hqLocation.y;
				break;
			default:
				// There's not even anything left
				// But this prevents an error in IDEA :/
				throw new Error("how did this even happen");
		}

		return new MapLocation(enemyHqX, enemyHqY);
	}
}


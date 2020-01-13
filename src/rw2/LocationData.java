package rw2;

import battlecode.common.*;

public class LocationData {
	
	public MapLocation location;
	public int value;
	public int ord;
	
	public LocationData(MapLocation m, int v) {
		location = m;
		value = v;
		ord = (m.x << 6) + m.y;
	}
	
	@Override
	public boolean equals(Object o) {
		System.out.println("equality check");
		return location.equals(((LocationData)o).location);
	}
	
	@Override
	public int hashCode() {
		return ord;
	}

}

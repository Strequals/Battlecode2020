package rw8;

import battlecode.common.MapLocation;

public class WeightedMapLocation {
    public final MapLocation mapLocation;
    public final int weight;

    public WeightedMapLocation(MapLocation mapLocation, int weight) {
        this.mapLocation = mapLocation;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return mapLocation.toString() + ", w=" + weight;
    }
}
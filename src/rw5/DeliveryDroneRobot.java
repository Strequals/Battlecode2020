package rw5;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public strictfp class DeliveryDroneRobot extends Robot{
	
   private enum DroneState {
      ATTACKING,
      DEFENDING,
      TRANSPORTING
   }
   
   private MapLocation homeLocation;
   private MapLocation hqLocation;
   private MapLocation targetLocation;  //building or location to clear enemy robots from
   private DroneState state;
   private int robotElevation;
   private boolean rush = false; //avoid netguns + hq?

   public DeliveryDroneRobot(RobotController rc) throws GameActionException {
      super(rc);
   	// TODO Auto-generated constructor stub
      RobotInfo[] ri = rc.senseNearbyRobots(2, rc.getTeam());
      RobotInfo r;
      for (int i = ri.length; --i >= 0;) {
         r = ri[i];
      	// Friendly Units
         if (r.getType() == RobotType.FULFILLMENT_CENTER && rc.getLocation().isAdjacentTo(r.getLocation())) {
            homeLocation = r.getLocation();
            break;
         }
      }
      
      
   }

   @Override
   public void run() throws GameActionException {
   	// TODO Auto-generated method stub
      robotElevation = rc.senseElevation(location);
   	// Process nearby robots
      RobotInfo[] ri = nearbyRobots;
      RobotInfo r;
      int nearbyLandscapers = 0;
      int targetBuildingDistance = 1000000;
      targetBuildingLocation = null;
      for (int i = ri.length; --i >= 0;) {
         r = ri[i];
         if (r.getTeam() == team) {
         	// Friendly Units
            switch (r.getType()) {
               case HQ:
                  hqLocation = r.location;
                  break;
               case FULLFILLMENT_CENTER:
                  if (homeLocation == null) homeLocation = r.location;
                  break;
            }
         } else if (r.getTeam() != Team.NEUTRAL) {
         	// Enemy Units
            int distance;
            switch (r.getType()) {
               case DELIVERY_DRONE:
                  isDroneThreat = true;
                  break;
               case MINER:
               // TODO: Block or bury
                  break;
               case LANDSCAPER:
                  isEnemyRushing = true;
                  break;
               case NET_GUN:
               // TODO: Bury
                  distance = Utility.chebyshev(r.location, location);
                  if (distance < targetBuildingDistance) {
                     targetBuildingDistance = distance;
                     targetBuildingLocation = r.location;
                  }
                  break;
               case REFINERY:
               // TODO: Bury
                  distance = Utility.chebyshev(r.location, location);
                  if (distance < targetBuildingDistance) {
                     targetBuildingDistance = distance;
                     targetBuildingLocation = r.location;
                  }
                  break;
               case DESIGN_SCHOOL:
               // TODO: Bury
                  distance = Utility.chebyshev(r.location, location);
                  if (distance < targetBuildingDistance) {
                     targetBuildingDistance = distance;
                     targetBuildingLocation = r.location;
                  }
                  break;
               case FULFILLMENT_CENTER:
               // TODO: Bury
                  distance = Utility.chebyshev(r.location, location);
                  if (distance < targetBuildingDistance) {
                     targetBuildingDistance = distance;
                     targetBuildingLocation = r.location;
                  }
                  break;
               case VAPORATOR:
               // TODO: Bury
                  distance = Utility.chebyshev(r.location, location);
                  if (distance < targetBuildingDistance) {
                     targetBuildingDistance = distance;
                     targetBuildingLocation = r.location;
                  }
                  break;
               case HQ:
               // We found it!
                  enemyHqLocation = r.location;
               default:
               //Probably some structure, bury it if possible but low priority
               //Communications.sendMessage(rc);
                  break;
            }
         }
      }
   	
   
   }

   @Override
   public void processMessage(int m, int x, int y) {
   	// TODO Auto-generated method stub
      switch (m) {                                       //will probably want to add a case for attack
         case 1:
            hqLocation = new MapLocation(x,y);
            System.out.println("Received HQ location: " + x + ", " + y);
            break;
      }
   }

}

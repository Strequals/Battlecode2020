package rw5;

import battlecode.common.*;

public strictfp class DeliveryDroneRobot extends Robot {
	
   private enum DroneState {
      ATTACKING,
      DEFENDING,
      TRANSPORTING
   }
   
   private MapLocation homeLocation;
   private MapLocation hqLocation;
   private MapLocation targetLocation;  //building or location to clear enemy robots from
   private MapLocation nearestWater;
   private DroneState state;
   private int robotElevation;
   private boolean rush = false; //avoid netguns + hq?
   private boolean sentEHQL = false;

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
   	// Process nearby robots (may have to move this into the if statements below)
      RobotInfo[] ri = nearbyRobots;
      RobotInfo r;
      
      for (int i = ri.length; --i >= 0;) {
         r = ri[i];
         if (r.getTeam() == team) {
         	// Friendly Units
            switch (r.getType()) {
               case HQ:
                  hqLocation = r.location;
                  break;
               case FULFILLMENT_CENTER:
                  if (homeLocation == null) homeLocation = r.location;
                  break;
            }
         } else if (r.getTeam() != Team.NEUTRAL) {
         	// Enemy Units
            int distance;
            switch (r.getType()) {
               case MINER:
               // TODO: Block or bury
                  break;
               case LANDSCAPER:
                  
                  break;
               case NET_GUN:
               // Avoid
                  
                  break;
               case REFINERY:
               // TODO: target?
                  break;
               case DESIGN_SCHOOL:
               // TODO: target?
                  break;
               case HQ:
               // We found it!
               //also avoid
                  enemyHqLocation = r.location;
               default:
               //Probably some structure, bury it if possible but low priority
               //Communications.sendMessage(rc);
                  break;
            }
         }
      }
   	if(enemyHqLocation != null && !sentEHQL) {
         Communcations.queueMessage(rc, 3, 3, enemyHqLocation.x, enemyHqLocation.y);
      }
      
      if(nearestWater == null) {
         //scan for water
         
      }
      
      if(rc.isCurrentlyHoldingUnit()) {
         //pathfind towards target (water, soup)
         if(nearestWater.distanceSquaredTo(target) <= 2) {
            if(rc.senseFlooding(nearestWater)) {
               rc.dropUnit(location.directionTo(nearestWater));
            }
            else {
               nearestWater = null;
            }
         }
         
   		if (Nav.target == null) {
   			Nav.beginNav(rc, this, nearestWater);
   		}
   		Nav.nav(rc, this);
         
         //if any of 8 locations around are flooded, place robot into flood, update nearestWater
      }
      else {
         ri = rc.senseNearbyRobots(2);
         for (int i = ri.length; --i >= 0;) {
            r = ri[i];
            if(r.team != team && r.canBePickedUp()) {
               if(rc.canPickUpUnit(r.getID())) {
                  rc.pickUpUnit(r.getID());
                  break;
               }
            }
         }
         ri = rc.senseNearbyRobots(8);
         for(int i = ri.length; --i >= 0;) {
            r = ri[i];
            if(r.team != team && r.canBePickedUp()) {
               //step towards then pick up
               
               if(rc.canPickUpUnit(r.getID())) {
                  rc.pickUpUnit(r.getID());
                  break;
               }
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
            sentEHQL = true; //if already received enemy hq location, don't rebroadcast
            break;
      }
   }

}

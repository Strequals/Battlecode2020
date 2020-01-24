package rw8;

import battlecode.common.*;

public strictfp class NetGunRobot extends Robot {

   public NetGunRobot(RobotController rc) throws GameActionException {
      super(rc);
   	// TODO Auto-generated constructor stub
   }

   @Override
   public void run() throws GameActionException {
   	// TODO Auto-generated method stub
      RobotInfo[] ri = nearbyRobots;
      RobotInfo r;
      for (int i = ri.length; --i >= 0;) {
         r = ri[i];
         if (r.getTeam() == team) {
                //Friendly Units
            switch (r.getType()) {
               default:
                  break;
            
            }
         } else if (r.getTeam() == Team.NEUTRAL) {
                //It's a cow, yeet it from our base
            if (round > 100) {
                    //Call the drones
                    //Communications.sendMessage(rc);
            }
         } else {
                //Enemy Units
            switch (r.getType()) {
               case MINER:
                        //Call the drones
                        //Communications.sendMessage(rc);
                  break;
               case LANDSCAPER:
                        //Call the drones
                        //Communications.sendMessage(rc);
                  break;
               case DELIVERY_DRONE:
                        //pew pew pew
                  rc.shootUnit(r.getID());
                  return;
               case NET_GUN:
                        //Direct units to bury the net gun
                        //Communications.sendMessage(rc);
                  break;
               case REFINERY:
                        //Direct units to bury the refinery
                        //Communications.sendMessage(rc);
                  break;
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
   	
   }

}

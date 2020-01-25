package rw8;

import battlecode.common.*;

public class Communications {
	
	public static int verySecretNumber;
	public static final long A = 1337133713371339L; //Must be odd
	public static final long C = 3141592653589796238L;
	
	public static LinkedQueue<MessageUnit> messageQueue = new LinkedQueue<MessageUnit>();
	
	/*1:HQ location
	 *2:Soup location
	 *3:Enemy HQ location
	 *4:Drone rush enemy HQ
	 *5:Refinery location
	 *6:Design school location
	 *7:
	 *8:Refinery removed
	 *9:
	 *10:
	 *11:HQ under attack 
    *12:Enter Transport mode(save miners)
    *13:Enter Assault mode(transport landscapers)
    *14:Enter Miner Assist Mode
    *15:Terraform location
    **/
	
	enum Message {
		HQ_LOC
	}
	
	static class MessageUnit {
		int c;
		int k;
		
		public MessageUnit(int c, int k) {
			this.c = c;
			this.k = k;
		}
	}
	
	public static int getSecret(int round) {
		return (int)(A*round+C);
	}
	
	public static void calculateSecret(int round) {
		verySecretNumber = getSecret(round);
	}
	
	public static void queueMessage(RobotController rc, int cost, int m, int x, int y) throws GameActionException {
		messageQueue.add(new MessageUnit(cost, (m << 12) + (x << 6) + (y)));
	}
	
	public static void sendMessages(RobotController rc) throws GameActionException{
		if (!messageQueue.hasNext()) return;
		int[] message = new int[7];
		message[0] = verySecretNumber;
		int i = 1;
		MessageUnit mu;
		int c = 1;
		while (messageQueue.hasNext() && i < 7) {
			mu = messageQueue.poll();
			message[i] = mu.k;
			if (c < mu.c) c = mu.c;
			i++;
		}
		if (rc.canSubmitTransaction(message, c)) {
			rc.submitTransaction(message, c);
		}
		messageQueue.clear();
	}
	
	public static void processLastBlock(RobotController rc, Robot r) throws GameActionException {
		Transaction[] t = rc.getBlock(r.round);
		Transaction trans;
		int[] m;
		int c;
		int message;
		for (int i = t.length; i-- > 0;) {
			trans = t[i];
			m = trans.getMessage();
			c = trans.getCost();
			if (m[0] != verySecretNumber) continue;
			for (int j = m.length; j-- > 1;) {
				message = m[j];
				if (message != 0) r.processMessage((message >> 12), (message >> 6) % 64, message % 64);
			}
			
		}
	}
	
	public static void processFirstBlock(RobotController rc, Robot r) throws GameActionException {
		Transaction[] t = rc.getBlock(1);
		Transaction trans;
		int[] m;
		int c;
		int message;
		int vsn1 = getSecret(0);
		for (int i = t.length; i-- > 0;) {
			trans = t[i];
			m = trans.getMessage();
			c = trans.getCost();
			if (m[0] != vsn1) continue;
			for (int j = m.length; j-- > 1;) {
				message = m[j];
				if (message != 0) r.processMessage((message >> 12), (message >> 6) % 64, message % 64);
			}
			
		}
	}

}

package rw4;

import battlecode.common.*;

public class Communications {
	
	public static int verySecretNumber;
	public static final long A = 1337133713371337L; //Must be odd
	public static final long C = 3141592653589793238L;
	
	enum Message {
		HQ_LOC
	}
	
	public static int getSecret(int round) {
		return (int)(A*round+C+1);
	}
	
	public static void calculateSecret(int round) {
		verySecretNumber = getSecret(round);
	}
	
	public static void sendMessage(RobotController rc, int cost, int m, int x, int y) throws GameActionException {
		int[] message = new int[7];
		message[0] = verySecretNumber;
		message[1] = (m << 12) + (x << 6) + (y);
		rc.submitTransaction(message,cost);
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
			System.out.println(m[0]);
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
		int vsn1 = getSecret(1);
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

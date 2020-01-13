package rw2;

import java.util.Arrays;

import battlecode.common.*;

public class Communications {
	
	public static int verySecretNumber;
	public static final long A = 1337133713371337L; //Must be odd
	public static final long C = 3141592653589793238L;
	
	static class Message {
		int m;
		LocationData ld;
		
		public Message(int message, LocationData data) {
			m = message;
			ld = data;
		}
	}
	
	public static LinkedQueue<Message> queue = new LinkedQueue<Message>();
	static int nextCost = 1;
	
	//1:HQ location
	//2:enemy HQ location
	//3:soup found
	//4:soup exhausted
	
	public static void calculateSecret(int round) {
		nextCost = 0;
		verySecretNumber = (int)(A*round+C);
	}
	
	public static void queueMessage(int cost, int m, LocationData ld) {
		queue.add(new Message(m,ld));
		if (cost > nextCost) nextCost = 0;
	}
	
	public static void sendMessages(RobotController rc) throws GameActionException {
		if (!queue.hasNext()) return;
		int[] message = new int[7];
		message[0] = verySecretNumber;
		Message m;
		for (int i = 6; i-->0;) {
			m = queue.poll();
			message[i] = (m.m << 24) + (m.ld.value << 12) + (m.ld.location.x << 6) + (m.ld.location.y);
			if (!queue.hasNext()) break;
		}
		rc.submitTransaction(message,nextCost);
	}
	
	public static void processLastBlock(RobotController rc, Robot r) throws GameActionException {
		Transaction[] t = rc.getBlock(r.round);
		Transaction trans;
		int[] m;
		int c;
		int message;
		for (int i = t.length; i-- > 1;) {
			trans = t[i];
			m = trans.getMessage();
			c = trans.getCost();
			if (m[0] != verySecretNumber) continue;
			for (int j = m.length; j-- > 0;) {
				message = m[j];
				if (message != 0) r.processMessage((message >> 24), (message >> 12) % 64, (message >> 6) % 64, message % 64);
			}
			
		}
	}
	
	public static void processFirstBlock(RobotController rc, Robot r) throws GameActionException {
		Transaction[] t = rc.getBlock(1);
		Transaction trans;
		int[] m;
		int c;
		int message;
		int vsn1 = (int)(C+A);
		for (int i = t.length; i-- > 0;) {
			trans = t[i];
			m = trans.getMessage();
			c = trans.getCost();
			if (m[0] != vsn1) continue;
			for (int j = m.length; j-- > 1;) {
				message = m[j];
				if (message != 0) r.processMessage((message >> 24), (message >> 12) % 64, (message >> 6) % 64, message % 64);
			}
			
		}
	}

}

package rw8;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Transaction;

public class Communications {

    private static final long A = 1337133713371339L; // Must be odd
    private static final long C = 3141592653589796238L;
    private static int verySecretNumber;
    private static LinkedQueue<MessageUnit> messageQueue = new LinkedQueue<>();

    private static int getSecret(int round) {
        return (int) (A * round + C + 2);
    }

    public static void calculateSecret(int round) {
        verySecretNumber = getSecret(round);
    }

    public static void queueMessage(RobotController rc, int cost, Message m, int x, int y) {
        messageQueue.add(new MessageUnit(cost, (m.ordinal() << 12) + (x << 6) + (y)));
    }

    public static void queueMessage(RobotController rc, int cost, Message m, MapLocation location) {
        messageQueue.add(new MessageUnit(cost, (m.ordinal() << 12) + (location.x << 6) + (location.y)));
    }

    public static void sendMessages(RobotController rc) throws GameActionException {
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
        for (int i = t.length; i-- > 0; ) {
            trans = t[i];
            m = trans.getMessage();
            c = trans.getCost();
            if (m[0] != verySecretNumber) continue;
            for (int j = m.length; j-- > 1; ) {
                message = m[j];
                if (message != 0) r.processMessage(Message.values()[message >> 12], (message >> 6) % 64, message % 64);
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
        for (int i = t.length; i-- > 0; ) {
            trans = t[i];
            m = trans.getMessage();
            c = trans.getCost();
            if (m[0] != vsn1) continue;
            for (int j = m.length; j-- > 1; ) {
                message = m[j];
                if (message != 0) r.processMessage(Message.values()[message >> 12], (message >> 6) % 64, message % 64);
            }
        }
    }

    enum Message {
        HQ_LOCATION,
        SOUP_LOCATION,
        ENEMY_HQ_LOCATION,
        DRONE_RUSH_ENEMY_HQ,
        REFINERY_LOCATION,
        DESIGN_SCHOOL_LOCATION,
        REFINERY_REMOVED,
        HQ_UNDER_ATTACK,
        /**
         * Save miners
         */
        ENTER_TRANSPORT_MODE,
        /**
         * Transport landscapers
         */
        ENTER_ASSAULT_MODE,
        ENTER_MINER_ASSIST_MODE,
        TERRAFORM_LOCATION,
        // TODO: Implement in drones
        COW_NEAR_HQ
    }

    static class MessageUnit {
        int c;
        int k;

        MessageUnit(int c, int k) {
            this.c = c;
            this.k = k;
        }
    }
}

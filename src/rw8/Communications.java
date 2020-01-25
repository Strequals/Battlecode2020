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

    public static void queueMessage(Message m, MapLocation location) {
        messageQueue.add(new MessageUnit(m.COST, (m.ordinal() << 12) + (location.x << 6) + (location.y)));
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
        int message;
        for (int i = t.length; i-- > 0; ) {
            trans = t[i];
            m = trans.getMessage();
            if (m[0] != verySecretNumber) continue;
            for (int j = m.length; j-- > 1; ) {
                message = m[j];
                if (message != 0)
                    r.processMessage(Message.values()[message >> 12], new MapLocation((message >> 6) % 64, message % 64));
            }

        }
    }

    public static void processFirstBlock(RobotController rc, Robot r) throws GameActionException {
        Transaction[] t = rc.getBlock(1);
        Transaction trans;
        int[] m;
        int message;
        int vsn1 = getSecret(0);
        for (int i = t.length; i-- > 0; ) {
            trans = t[i];
            m = trans.getMessage();
            if (m[0] != vsn1) continue;
            for (int j = m.length; j-- > 1; ) {
                message = m[j];
                if (message != 0)
                    r.processMessage(Message.values()[message >> 12], new MapLocation((message >> 6) % 64, message % 64));
            }
        }
    }

    enum Message {
        HQ_LOCATION(20),
        SOUP_LOCATION(1),
        ENEMY_HQ_LOCATION(2),
        DRONE_RUSH_ENEMY_HQ(2),
        REFINERY_LOCATION(5),
        // TODO: Never sent
        DESIGN_SCHOOL_LOCATION(2),
        REFINERY_REMOVED(1),
        HQ_UNDER_ATTACK(2),
        /**
         * Save miners
         * TODO: Never sent
         */
        ENTER_TRANSPORT_MODE(2),
        /**
         * Transport landscapers
         * TODO: Never sent
         */
        ENTER_ASSAULT_MODE(2),
        // TODO: Never sent
        ENTER_MINER_ASSIST_MODE(2),
        TERRAFORM_LOCATION(2),
        // TODO: Receive in drones
        COW_NEAR_HQ(3);

        final int COST;

        Message(int cost) {
            COST = cost;
        }
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

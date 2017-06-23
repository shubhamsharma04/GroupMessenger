package edu.buffalo.cse.cse486586.groupmessenger2;

import android.net.Uri;

import java.util.PriorityQueue;

/**
 * Created by opensam on 3/7/17.
 */

public final class GeneralConstants {

    public static final int SERVER_PORT = 10000;
    public static final String ALL_PORTS[] = new String[]{"11108", "11112", "11116", "11120", "11124"};
    public static final String ACK_MSG = "HOUSTEN_I_GOT_YOU";
    public static final int ACTION_SEND_SEQ = 1;
    public static final int ACTION_ACCEPT_SEQ = 2;
    public static final int ACTION_WRITE_MSG = 3;
    public static final int ACTION_DELETE_MSG = 4;
    public static final String KEY_MSG = "msg";
    public static final String KEY_ID = "id";
    public static final String KEY_AGREEDID = "agreedId";
    public static final String KEY_PROPOSED_ID = "proposedId";
    public static final String KEY_PID = "pId";
    public static final String KEY_ACTION = "action";
    public static final int SOCKET_TIMEOUT = 1000;
    public static final int BASE_EMULATOR_ID = 5554;
}

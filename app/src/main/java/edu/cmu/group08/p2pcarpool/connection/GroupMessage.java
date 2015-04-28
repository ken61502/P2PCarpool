package edu.cmu.group08.p2pcarpool.connection;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by kenny on 2015/4/8.
 */
public class GroupMessage implements Serializable {
    public static int ID = 0;
    public boolean isMulticast;
    public String type;
    public String message;
    public String sender;
    public HashMap<String, Object> data;
    public int mId;
    public String ip_port;
    public GroupMessage(String t, String m, String s, boolean iM) {
        type = t;
        message = m;
        sender = s;
        data = new HashMap<>();
        isMulticast = iM;
        mId = ID++;
    }
    public GroupMessage(String t, String m, String s, boolean iM, int id) {
        type = t;
        message = m;
        sender = s;
        data = new HashMap<>();
        isMulticast = iM;
        mId = id;
    }

    public String[] unpack() {
        String[] res = {type, sender, message};
        return res;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        return sb
                .append("Type: " + type + "\n")
                .append("Message: " + message)
                .toString();
    }
}

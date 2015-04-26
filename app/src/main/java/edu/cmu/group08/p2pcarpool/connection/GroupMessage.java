package edu.cmu.group08.p2pcarpool.connection;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by kenny on 2015/4/8.
 */
public class GroupMessage implements Serializable {
    public boolean isMulticast;
    public String type;
    public String message;
    public String sender;
    public HashMap<String, Object> data;
    public GroupMessage(String t, String m, String s, boolean ism) {
        type = t;
        message = m;
        sender = s;
        data = new HashMap<>();
        isMulticast = ism;
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

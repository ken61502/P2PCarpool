package edu.cmu.group08.p2pcarpool.connection;

import java.io.Serializable;

/**
 * Created by kenny on 2015/4/8.
 */
public class GroupMessage implements Serializable {
    public int type;
    public String message;
    public GroupMessage(int t, String m) {
        type = t;
        message = m;
    }
    @Override
    public String toString(){
        return "Type:"+Integer.toString(type)+"Message:"+message;
    }
}

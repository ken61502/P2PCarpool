package edu.cmu.group08.p2pcarpool;

/**
 * Created by kenny on 2015/4/3.
 */
public class BroadcastPacket {
    private int mSendPort = -1;
    private int mListenPort = -1;
    private Object mData = null;
    private int mTime = -1;

    BroadcastPacket(int listen_port, int send_port, Object data) {
        mSendPort = send_port;
        mListenPort = listen_port;
        mData = data;
//        mTime = .
    }

    public Object getData() {
        return mData;
    }

    public int getSendPort() {
        return mSendPort;
    }

    public int getListenPort() {
        return mListenPort;
    }
}

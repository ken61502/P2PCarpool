package edu.cmu.group08.p2pcarpool.broadcast;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by kenny on 2015/4/3.
 */
/*
 * This class tries to send a broadcast UDP packet over your wifi network to discover the boxee service.
 */

public class ListenBroadcast extends Thread {
    private static final String TAG = "ListenBroadcast";
    private static final int TIMEOUT_MS = 500;
    private static final int UPDATE_LOG = 0;
    private boolean mStop = false;
    private boolean mPause = false;
    private WifiManager mWifi = null;
    private DatagramSocket mBroadcastSocket = null;
    private Handler mHandler = null;
    private int mListenPort = -1;

    interface DiscoveryReceiver {
        void addAnnouncedServers(InetAddress[] host, int port[]);
    }

    public ListenBroadcast(WifiManager wifi, DatagramSocket socket, Handler handler, int listen_port) {
        mWifi = wifi;
        mBroadcastSocket = socket;
        mHandler = handler;
        mListenPort = listen_port;
    }

    public void run() {
        try {
            if (mBroadcastSocket == null) {
                mBroadcastSocket = new DatagramSocket(mListenPort);
            }
            mBroadcastSocket.setBroadcast(true);
            mBroadcastSocket.setSoTimeout(TIMEOUT_MS);
            listenForResponses(mBroadcastSocket);
        } catch (IOException e) {
            Log.e(TAG, "Could not send discovery request", e);
        }
    }

    synchronized public void stopThread() {
        mStop = true;
    }

    synchronized public void pauseThread() {
        mPause = true;
    }

    synchronized public void resumeThread() {
        mPause = false;
    }

    /**
     * Listen on socket for responses, timing out after TIMEOUT_MS
     *
     * @param socket
     *          socket on which the announcement request was sent
     * @throws IOException
     */
    private void listenForResponses(DatagramSocket socket) throws IOException {
        byte[] buf = new byte[1024];

        while (!mStop) {
            try {
                if (!mPause) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String s = new String(packet.getData(), 0, packet.getLength());
                    Log.d(TAG, "Received response " + s);

                    if (mHandler != null) {
                        Message msg = mHandler.obtainMessage();
                        msg.what = UPDATE_LOG;
                        msg.obj = s;
                        mHandler.sendMessage(msg);
                    }
                }
                else {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                }

            } catch (SocketTimeoutException e) {
//                Log.d(TAG, "Receive timed out");
            }
        }
    }
}


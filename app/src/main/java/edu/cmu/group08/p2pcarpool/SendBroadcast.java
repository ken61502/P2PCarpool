package edu.cmu.group08.p2pcarpool;

import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by kenny on 2015/4/3.
 */
public class SendBroadcast extends Thread {
    private static final String TAG = "SendBraodcast";
    private static final String REMOTE_KEY = "b0xeeRem0tE!";

    private static final int TIMEOUT_MS = 500;
    private static final String mChallenge = "myvoice";
    private WifiManager mWifi = null;
    private DatagramSocket mBroadcastSocket = null;
    private int mListenPort = -1;
    private int mSendPort = -1;
    private Object mData = null;

    interface DiscoveryReceiver {
        void addAnnouncedServers(InetAddress[] host, int port[]);
    }


    SendBroadcast(WifiManager wifi, DatagramSocket socket, BroadcastPacket packet) {
        mWifi = wifi;
        mBroadcastSocket = socket;
        mListenPort = packet.getListenPort();
        mSendPort = packet.getSendPort();
        mData = packet.getData();
    }

    public void run() {
        try {
            if (mBroadcastSocket == null) {
                mBroadcastSocket = new DatagramSocket(mSendPort);
            }
            mBroadcastSocket.setBroadcast(true);
            mBroadcastSocket.setSoTimeout(TIMEOUT_MS);

            sendDiscoveryRequest(mBroadcastSocket);
        } catch (IOException e) {
            Log.e(TAG, "Could not send broadcast", e);
        }
    }
    /**
     * Send a broadcast UDP packet containing a request for boxee services to
     * announce themselves.
     *
     * @throws java.io.IOException
     */
    private void sendDiscoveryRequest(DatagramSocket socket) throws IOException {
        String data = String.format("Message Sent from: %s", getIpAddress().toString()); //String
//                .format(
//                        "<bdp1 cmd=\"discover\" application=\"iphone_remote\" challenge=\"%s\" signature=\"%s\"/>",
//                        mChallenge, getSignature(mChallenge));
        Log.d(TAG, "Sending data " + data);

        DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                getBroadcastAddress(), mListenPort);
        socket.send(packet);
    }

    /**
     * Calculate the broadcast IP we need to send the packet along. If we send it
     * to 255.255.255.255, it never gets sent. I guess this has something to do
     * with the mobile network not wanting to do broadcast.
     */
    private InetAddress getBroadcastAddress() throws IOException {
        DhcpInfo dhcp = mWifi.getDhcpInfo();
        if (dhcp == null) {
            Log.d(TAG, "Could not get dhcp info");
            return null;
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        Log.d(TAG, "BCIP:" + InetAddress.getByAddress(quads).toString());
        return InetAddress.getByAddress(quads);
    }

    public InetAddress getIpAddress() throws IOException {
        WifiInfo wifiInfo = mWifi.getConnectionInfo();
        if (wifiInfo == null) {
            Log.d(TAG, "Could not get wifi info");
            return null;
        }
        int ip = wifiInfo.getIpAddress();
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((ip >> k * 8) & 0xFF);
        Log.d(TAG, "IP:" + InetAddress.getByAddress(quads).toString());
        return InetAddress.getByAddress(quads);
    }
    /**
     * Calculate the signature we need to send with the request. It is a string
     * containing the hex md5sum of the challenge and REMOTE_KEY.
     *
     * @return signature string
     */
    private String getSignature(String challenge) {
        MessageDigest digest;
        byte[] md5sum = null;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(challenge.getBytes());
            digest.update(REMOTE_KEY.getBytes());
            md5sum = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        StringBuffer hexString = new StringBuffer();
        for (int k = 0; k < md5sum.length; ++k) {
            String s = Integer.toHexString((int) md5sum[k] & 0xFF);
            if (s.length() == 1)
                hexString.append('0');
            hexString.append(s);
        }
        return hexString.toString();
    }
}

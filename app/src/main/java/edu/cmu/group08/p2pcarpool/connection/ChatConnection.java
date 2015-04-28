/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.cmu.group08.p2pcarpool.connection;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.LoginFilter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import edu.cmu.group08.p2pcarpool.connection.GroupMessage;
import edu.cmu.group08.p2pcarpool.group.GroupContent;

public class ChatConnection {

    private Handler mUpdateHandler = null;
    private ChatServer mChatServer = null;
    private ConcurrentHashMap<String, ChatClient> mChatClients = new ConcurrentHashMap<>();
    private HashMap<String, String> mIpToName = new HashMap<>();
    private HashMap<String, HashSet<Integer>> mAcceptedMsg = new HashMap<>();

    private static final String TAG = "ChatConnection";

    private String mLocalName;
    private WifiManager mWifi;
    private int mPort = -1;

    private int mPassengerLimit = Integer.MAX_VALUE;
    private boolean mAccepting = true;

    private String mHostAddress = null;

    public ChatConnection(String name, Handler handler, WifiManager wifi) {
        mLocalName = name;
        mUpdateHandler = handler;
        mChatServer = new ChatServer(handler);
        mWifi = wifi;
    }

    public ChatConnection(String name, Handler handler, WifiManager wifi, int passengerLimit) {
        mLocalName = name;
        mUpdateHandler = handler;
        mChatServer = new ChatServer(handler);
        mWifi = wifi;
        mPassengerLimit = passengerLimit;

    }

    public boolean addAcceptedMessage(GroupMessage msg) {
        String sender = msg.sender;
        int id = msg.mId;
        if (sender == null || sender.equals(mLocalName)) {
            return false;
        }
        if (!mAcceptedMsg.containsKey(sender)) {
            // First time receive this sender's message
            HashSet<Integer> set = new HashSet<>();
            set.add(id);
            mAcceptedMsg.put(sender, set);
        }
        else {
            // Received from this sender before
            HashSet<Integer> set = mAcceptedMsg.get(sender);
            if (!set.contains(id)) {
                // Haven't receive this message from this sender before
                set.add(id);
            }
            else {
                // Already received this message
                // Should drop this message
                return false;
            }
        }
        return true;
    }

    public void updateClientServerIp(String client_ip_port, String server_port, String name) {
        if (mChatClients.containsKey(client_ip_port)) {
            mChatClients.get(client_ip_port).setServerAddressPortName(client_ip_port, server_port, name);
            sendHandlerMessage("join", name + " has joined.", -1, false, Settings.SYSTEM_SENDER);
            Log.d(TAG, "Client Name:" + name + " received");
        }
        else {
            Log.d(TAG, "Client does not exist.");
        }
    }

    public void updateClientList(String ip_list) {
        String[] ips_name = ip_list.split(",");
        String local_ip = getIpAddress().toString();
        for (int i = 0; i < ips_name.length; i++) {
            String[] tokens_tmp = ips_name[i].split("=", 2);
            String ip_port = tokens_tmp[0];
            String name = tokens_tmp[1];
            if (!mChatClients.containsKey(ip_port)) {
                String[] tokens = ip_port.trim().split(":");
                if (!tokens[0].equals(local_ip)) {
                    InetAddress addr;
                    try {
                        addr = InetAddress.getByName(tokens[0].replace("/",""));
                        sendHandlerMessage("join", name + " has joined.", -1, false, Settings.SYSTEM_SENDER);
                        mIpToName.put(ip_port, name);
                        if (local_ip.compareTo(tokens[0]) > 0) {
                            Log.d(TAG, "Connected to " + ip_port);
                            connectToRemote(addr, Integer.parseInt(tokens[1]), null);
                        }
                        else {
                            Log.d(TAG, "Waiting " + ip_port + " to connect me.");
                        }
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.d(TAG, ip_port + " local address.");
                }
            }
            else {
                Log.d(TAG, ip_port + " is already connected.");
            }
        }
    }

    public synchronized boolean checkPassengerLimit() {
        if (mHostAddress == null) {
            // Is Host
            Log.d(TAG, Integer.toString(mPassengerLimit - (mChatClients.size() + 1)));

            if ((mChatClients.size() + 1) == mPassengerLimit) {
                if (mAccepting) {
                    Log.d(TAG, "Passenger full");
                    sendHandlerMessage("stop_accept", null, -1, true, null);
                    mAccepting = false;
                }
                return true;
            }
            else if ((mChatClients.size() + 1) < mPassengerLimit) {
                if (!mAccepting) {
                    Log.d(TAG, "Passenger not full");
                    sendHandlerMessage("start_accept", null, -1, true, null);
                    mAccepting = true;
                }
                return true;
            }
            else {
                return false;
            }
        }
        return true;
    }

    public void connectToHost(InetAddress address, int port, Socket socket) {
        ChatClient client;
        String ip_port = address.toString() + ":" + Integer.toString(port);
        if (!mChatClients.containsKey(ip_port)) {
            client = new ChatClient(address, port, socket);
            mChatClients.put(ip_port, client);
        }
        else {
            Log.d(TAG, "Client already connected");
        }
        Log.d(TAG, getIpAddress().toString());
        mHostAddress = ip_port;
        sendDirectMessage(ip_port, Settings.UPDATE_CLIENT_INFO, mChatServer.getPort(), Settings.DUMMY);
    }

    public void connectToRemote(InetAddress address, int port, Socket socket) {
        ChatClient client;
        String ip_port = address.toString() + ":" + Integer.toString(port);
        if (!mChatClients.containsKey(ip_port)) {
            client = new ChatClient(address, port, socket);
            mChatClients.put(ip_port, client);
        }
        else {
            Log.d(TAG, "Client already connected");
        }
    }

    public void tearDown() {
        if (mChatServer != null) {
            mChatServer.tearDown();
            mChatServer = null;
        }
        if (!mChatClients.isEmpty()) {
            tearDownAllClient();
        }
        if (!mIpToName.isEmpty()) {
            mIpToName.clear();
        }
    }

    public void disconnectToServer() {
        if (!mChatClients.isEmpty()) {
            tearDownAllClient();
        }
        else {
            Log.d(TAG, "Client list is empty");
        }
    }

    public void tearDownAllClient() {
        if (!mChatClients.isEmpty()) {
            Iterator it = mChatClients.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ChatClient> entry = (Map.Entry<String, ChatClient>) it.next();
                entry.getValue().tearDown(true);
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
        else {
            Log.d(TAG, "Client list is empty");
        }
    }

    public void tearDownClientWithIp(String ip_port, boolean flood) {
        if (mChatClients.containsKey(ip_port)) {
            ChatClient client = mChatClients.get(ip_port);
            String name = client.getName();
            if (name.equals("")) {
                name = mIpToName.get(ip_port);
                if (name == null) {
                    name = "";
                }
            }
            sendHandlerMessage("leave", name + " has left.", -1, false, Settings.SYSTEM_SENDER);
            client.tearDown(flood);
            mChatClients.remove(ip_port);
        }
        else {
            Log.d(TAG, "Client manager doesn't has IP = " + ip_port);
        }
    }

    public void sendDirectMessage(String ip_port, String type, String msg, String sender) {
        if (mChatClients.containsKey(ip_port)) {
            if (type.equals(Settings.KICK_OFF)) {
                mChatClients.get(ip_port).addMessageToQueue(type, msg, sender, false, ip_port);
            }
            else {
                mChatClients.get(ip_port).addMessageToQueue(type, msg, sender, false);
            }
        }
        else {
            Log.e(TAG, "Client list does not contain " + ip_port);
        }
    }

    public void sendMulticastMessage(String type, String msg, String sender) {
        if (!mChatClients.isEmpty()) {
            Iterator it = mChatClients.entrySet().iterator();
            int id = GroupMessage.ID;
            while (it.hasNext()) {
                Map.Entry<String, ChatClient> entry = (Map.Entry<String, ChatClient>) it.next();
                entry.getValue().addMessageToQueue(type, msg, sender, true, id);
            }
            GroupMessage.ID++;
        }
        else {
            Log.d(TAG, "Client list is empty");
        }
    }

    public void sendForwardMulticastMessage(GroupMessage msg) {
        if (!mChatClients.isEmpty()) {
            Iterator it = mChatClients.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ChatClient> entry = (Map.Entry<String, ChatClient>) it.next();
                entry.getValue().addMessageToQueue(msg);
            }
        }
        else {
            Log.d(TAG, "Client list is empty");
        }
    }

    public InetAddress getIpAddress() {
        WifiInfo wifiInfo = mWifi.getConnectionInfo();
        if (wifiInfo == null) {
            Log.d(TAG, "Could not get wifi info");
            return null;
        }
        int ip = wifiInfo.getIpAddress();
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((ip >> k * 8) & 0xFF);
        try {
            Log.d(TAG, "IP:" + InetAddress.getByAddress(quads).toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            return InetAddress.getByAddress(quads);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getLocalPort() {
        return mPort;
    }
    
    public void setLocalPort(int port) {
        mPort = port;
    }

    private GroupMessage patchMessage(String type, String msg, String sender, boolean isMulticast) {
        GroupMessage groupMessage = new GroupMessage(type, msg, sender, isMulticast);
        return groupMessage;
//        return type + ":" + sender + ":" + msg;
    }

    private GroupMessage patchMessage(String type, String msg, String sender, boolean isMulticast, int id) {
        GroupMessage groupMessage =  new GroupMessage(type, msg, sender, isMulticast, id);
        return groupMessage;
//        return type + ":" + sender + ":" + msg;
    }

    private GroupMessage patchMessage(String type, String msg, String sender, boolean isMulticast, String ip_port) {
        GroupMessage groupMessage =  new GroupMessage(type, msg, sender, isMulticast);
        groupMessage.ip_port = ip_port;
        return groupMessage;
//        return type + ":" + sender + ":" + msg;
    }

    private String[] unpatchMessage(GroupMessage patchedMsg) {
        return patchedMsg.unpack();
//        return patchedMsg.split(":", 3);
    }


    public synchronized void sendHandlerMessage(String op, String msg, int id, boolean self, String sender) {
        Bundle messageBundle = new Bundle();
        messageBundle.putString("op", op);
        messageBundle.putString("msg", msg);
        messageBundle.putInt("id", id);
        messageBundle.putString("sender", sender);
        messageBundle.putBoolean("self", self);
        Message message = new Message();
        message.setData(messageBundle);
        mUpdateHandler.sendMessage(message);
    }


    /*
     *  ChatServer Class
     */
    private class ChatServer {
        ServerSocket mServerSocket = null;
        Thread mThread = null;

        HashMap<String, Socket> clientSocket = new HashMap<>();

        public ChatServer(Handler handler) {
            mThread = new Thread(new ServerThread());
            mThread.start();
        }

        public String getPort() {
            return Integer.toString(mServerSocket.getLocalPort());
        }
        public void tearDown() {
            mThread.interrupt();
        }

        public void closeAllClientSocket() {
            Iterator it = clientSocket.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Socket> entry = (Map.Entry<String, Socket>) it.next();
                try {
                    entry.getValue().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                it.remove(); // avoids a ConcurrentModificationException
            }
        }

        class ServerThread implements Runnable {

            @Override
            public void run() {

                try {
                    // Since discovery will happen via Nsd, we don't need to care which port is
                    // used.  Just grab an available one  and advertise it via Nsd.
                    mServerSocket = new ServerSocket(0);
                    setLocalPort(mServerSocket.getLocalPort());

//                    sendHandlerMessage("error",
//                            "sygroup room: listening to port=" + Integer.toString(mServerSocket.getLocalPort()), -1,false,Settings.SYSTEM_SENDER);

                    while (!Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        Socket socket = mServerSocket.accept();
                        Log.d(TAG, "Connected.");

                        int port = socket.getPort();
                        InetAddress address = socket.getInetAddress();
                        String ip_port = address.toString()+":"+Integer.toString(port);

                        connectToRemote(address, port, socket);

                        if(!checkPassengerLimit()) {
                            sendDirectMessage(ip_port, Settings.KICK_OFF, "", mLocalName);
                        }

                        // mHostAddress is fulfilled only when user click connect to Host
                        if (mHostAddress == null) {
                            //TODO Showing name instead of ip_port makes more sense.
//                            sendHandlerMessage("join", ip_port + "has joined", -1,false,Settings.SYSTEM_SENDER);
                            Thread updateThread = new Thread(new UpdateClientListThread());
                            updateThread.start();
                        }
                    }
                    try {
                        mServerSocket.close();
                        closeAllClientSocket();
                    } catch (IOException ioe) {
                        Log.e(TAG, "Error when closing server socket.");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error creating ServerSocket: ", e);
                    e.printStackTrace();
                }
            }
        }
        class UpdateClientListThread implements Runnable {

            @Override
            public void run() {
                if (!mChatClients.isEmpty()) {
                    Iterator it = mChatClients.entrySet().iterator();
                    String list = "";
                    Map.Entry<String, ChatClient> entry;

                    it.hasNext();
                    entry = (Map.Entry<String, ChatClient>) it.next();
                    String s_ip;
                    while((s_ip = entry.getValue().getServerIpPort()) == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
//                            e.printStackTrace();
                        }
                    }
                    String name = entry.getValue().getName();
                    list += (s_ip + "=" + name);
                    while (it.hasNext()) {
                        entry = (Map.Entry<String, ChatClient>) it.next();
                        while((s_ip = entry.getValue().getServerIpPort()) == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
//                            e.printStackTrace();
                            }
                        }
                        name = entry.getValue().getName();
                        list += ("," + s_ip + "=" + name);
                    }
                    sendMulticastMessage(Settings.UPDATE_CLIENT_LIST, list, mLocalName);
//                    it = mChatClients.entrySet().iterator();
//                    while (it.hasNext()) {
//                        entry = (Map.Entry<String, ChatClient>) it.next();
//                        entry.getValue().addMessageToQueue(Settings.UPDATE_CLIENT_LIST, list, Settings.DUMMY);
//                    }
                }
                else {
                    Log.d(TAG, "Client list is empty");
                }
            }

        }
    }
    /*
     *  ChatClient Class
     */
    private class ChatClient {

        private InetAddress mAddress;
        private int PORT;
        private InetAddress mServerAddress = null;
        private int mServerPort = -1;

        private final String CLIENT_TAG = "ChatClient";

        private Socket mSocket;
        private Thread mSendThread;
        private Thread mRecThread;
        private String mClientName = "";

        private int QUEUE_CAPACITY = 100;
        private BlockingQueue<GroupMessage> mMessageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        public ChatClient(InetAddress address, int port, Socket socket) {

            Log.d(CLIENT_TAG, "Creating chatClient");
            this.mAddress = address;
            this.PORT = port;
            this.mSocket = socket;

            mSendThread = new Thread(new SendingThread(mMessageQueue));
            mSendThread.start();
        }

        public void setServerAddressPortName(String client_ip_port, String server_port, String name) {
            String[] client_ip_port_token = client_ip_port.split(":", 2);
            Log.d(CLIENT_TAG, server_port);
            Log.d(CLIENT_TAG, client_ip_port);
            try {
                mClientName = name;
                mServerAddress = InetAddress.getByName(client_ip_port_token[0].replace("/", ""));
                mServerPort = Integer.parseInt(server_port);

                Log.d(CLIENT_TAG, "Server Address for " + mAddress.toString() + " set: " + getServerAddress().toString()+":"+ Integer.toString(getServerPort()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        public String getLocalIpPort() {
            if (mSocket != null) {
                return mSocket.getLocalAddress().toString()+":"+mSocket.getLocalPort();
            }
            else {
                return null;
            }
        }
        public String getServerIpPort() {
            if (mServerAddress != null && mServerPort != -1) {
                return mServerAddress.toString() + ":" + Integer.toString(mServerPort);
            }
            else {
                return null;
            }
        }
        public String getServerIp() {
            if (mServerAddress != null) {
                return mServerAddress.toString();
            }
            else {
                return null;
            }
        }
        public String getName() {
            return mClientName;
        }
        public InetAddress getServerAddress() {
            return mServerAddress;
        }
        public int getServerPort() {
            return mServerPort;
        }
        public void tearDown(boolean flood) {
            Log.e(CLIENT_TAG, "ClientChat Teardown");
            if (flood) {
                addMessageToQueue(Settings.TEARDOWN_MESSAGE, "", "", false);
            }
            mSendThread.interrupt();
            mRecThread.interrupt();
        }

        public void addMessageToQueue(String type, String msg, String sender, boolean isMulticast) {
            try {
                mMessageQueue.put(patchMessage(type, msg, sender, isMulticast));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        public void addMessageToQueue(String type, String msg, String sender, boolean isMulticast, String ip_port) {
            try {
                mMessageQueue.put(patchMessage(type, msg, sender, isMulticast, ip_port));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        public void addMessageToQueue(String type, String msg, String sender, boolean isMulticast, int id) {
            try {
                mMessageQueue.put(patchMessage(type, msg, sender, isMulticast, id));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        public void addMessageToQueue(GroupMessage msg) {
            try {
                mMessageQueue.put(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean sendMessage(GroupMessage msg, ObjectOutputStream out) {
            try {
                if (mSocket == null) {
                    Log.e(CLIENT_TAG, "Socket is null");
                    return false;
                }
                else if (mSocket.getOutputStream() == null) {
                    Log.e(CLIENT_TAG, "Socket output stream is null");
                    return false;
                }

//                PrintWriter out = new PrintWriter(
//                        new BufferedWriter(
//                                new OutputStreamWriter(mSocket.getOutputStream())), true);

//                out = new ObjectOutputStream(mSocket.getOutputStream());

//                out.println(msg);
                out.writeObject(msg);

//                    String[] t = unpatchMessage(msg);
//                    if (t[0].equals(Settings.CHAT_MESSAGE)) {
//                        // Send Handler message here will result in multiple message.
//                        // Commenting out.
//                        //sendHandlerMessage("chat", t[2], -1, true, t[1]);
//                    }

                out.flush();
//                out.close();

                Log.d(CLIENT_TAG, "Client sent message: " + msg);
                return true;
            } catch (UnknownHostException e) {
                Log.e(CLIENT_TAG, "Unknown Host", e);
                return false;
            } catch (IOException e) {
                Log.e(CLIENT_TAG, "I/O Exception", e);
                return false;
            } catch (Exception e) {
                Log.e(CLIENT_TAG, "Error3", e);
                return false;
            }
        }


        class SendingThread implements Runnable {

            private final BlockingQueue<GroupMessage> mMessageQueue;

            public SendingThread(BlockingQueue q) {
                mMessageQueue = q;
            }

            @Override
            public void run() {
                try {
                    if (mSocket == null) {
                        mSocket = new Socket(mAddress, PORT);
                        Log.d(CLIENT_TAG, "Client connect to server: Socket initialized");

                    } else {
                        Log.d(CLIENT_TAG, "Server accept client: Socket already initialized. No need to create new socket.");
                    }

                    mRecThread = new Thread(new ReceivingThread());
                    mRecThread.start();

                } catch (UnknownHostException e) {
                    Log.d(CLIENT_TAG, "Initializing socket failed, UHE", e);
                } catch (IOException e) {
                    Log.d(CLIENT_TAG, "Initializing socket failed, IOE.", e);
                }

                ObjectOutputStream out = null;
                try {
                    out = new ObjectOutputStream(mSocket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (true) {
                    try {
                        GroupMessage msg = mMessageQueue.take();
                        String type = msg.type;
                        if (type.equals(Settings.UPDATE_CLIENT_INFO)) {
                            msg.data.put("local_ip_port", getLocalIpPort());
                            msg.data.put("local_name", mLocalName);
//                            msg += ("," + getLocalIpPort() + "," + mLocalName);
                            Log.d(CLIENT_TAG, getLocalIpPort());
                        }
                        sendMessage(msg, out);
                        if (type.equals(Settings.KICK_OFF)) {
                            Log.e(TAG, "Kick off " + msg.ip_port);
//                            tearDownClientWithIp(msg.ip_port, false);
                        }
                    } catch (InterruptedException ie) {
                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }


        class ReceivingThread implements Runnable {
            @Override
            public void run() {

                ObjectInputStream input;
                try {
//                    input = new BufferedReader(new InputStreamReader(
//                            mSocket.getInputStream()));

                    input = new ObjectInputStream(mSocket.getInputStream());
                    while (!Thread.currentThread().isInterrupted() && !mSocket.isClosed()) {
                        GroupMessage messageObj = null;
//                        messageStr = input.readLine();
                        try {
                            messageObj = (GroupMessage) input.readObject();
                        }
                        catch (IOException e) {
                            Log.e(TAG, "Read Empty Message Object");
                        }
                        finally {
                            if (messageObj != null) {
                                Log.d(CLIENT_TAG, "Read from the stream: " + messageObj.toString());

                                String type = messageObj.type,
                                        sender = messageObj.sender,
                                        msg = messageObj.message,
                                        ip_port = mAddress.toString()+":"+Integer.toString(PORT);


                                if (type.equals(Settings.CHAT_MESSAGE)) {
                                    if (addAcceptedMessage(messageObj)) {
                                        if (messageObj.isMulticast) {
                                            sendForwardMulticastMessage(messageObj);
                                        }
                                        sendHandlerMessage("chat", msg, -1, false, sender);
                                    }
                                } else if (type.equals(Settings.TEARDOWN_MESSAGE)) {
                                    tearDownClientWithIp(ip_port, false);
                                    checkPassengerLimit();
                                } else if (type.equals(Settings.UPDATE_CLIENT_LIST)) {
                                    if (addAcceptedMessage(messageObj)) {
                                        sendForwardMulticastMessage(messageObj);
                                        updateClientList(msg);
                                    }
                                } else if (type.equals(Settings.UPDATE_CLIENT_INFO)) {
                                    String server_port = msg;
                                    String client_ip_port = (String) messageObj.data.get("local_ip_port");
                                    String name = (String) messageObj.data.get("local_name");
                                    Log.d(CLIENT_TAG, client_ip_port);
                                    updateClientServerIp(client_ip_port, server_port, name);
                                } else if (type.equals(Settings.KICK_OFF)) {
                                    sendHandlerMessage("reset", null, -1, false, sender);
                                }
                            } else {
                                Log.d(CLIENT_TAG, "The nulls! The nulls!");
                                break;
                            }
                        }
                    }
                    input.close();
                    try {
                        Log.e(TAG, "Closing socket!");
                        mSocket.close();
                    } catch (IOException ioe) {
                        Log.e(TAG, "Error when closing server socket.");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Server loop error: ", e);
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

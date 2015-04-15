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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.cmu.group08.p2pcarpool.connection.GroupMessage;
import edu.cmu.group08.p2pcarpool.group.GroupContent;

public class ChatConnection {

    private Handler mUpdateHandler = null;
    private ChatServer mChatServer = null;
    private HashMap<String, ChatClient> mChatClients = new HashMap<>();

    private static final String TAG = "ChatConnection";
    private static final String SYSTEM_SENDER = "System Message";
    private static final String DUMMY = "GROUP08";
    private static final String CHAT_MESSAGE = "chat";
    private static final String TEARDOWN_MESSAGE = "tear_down";
    private static final String UPDATE_CLIENT_LIST = "update_client_list";
    private static final String UPDATE_CLIENT_SERVER_IP = "update_client_server_ip";
    private WifiManager mWifi;
    private int mPort = -1;
    private String mHostAddress = null;

    public ChatConnection(Handler handler, WifiManager wifi) {
        mUpdateHandler = handler;
        mChatServer = new ChatServer(handler);
        mWifi = wifi;
    }

    public void updateClientServerIp(String client_ip_port, String server_port) {
        if (mChatClients.containsKey(client_ip_port)) {
            mChatClients.get(client_ip_port).setServerAddressPort(client_ip_port, server_port);
        }
        else {
            Log.d(TAG, "Client does not exist.");
        }
    }

    public void updateClientList(String ip_list) {
        String[] ips = ip_list.split(",");
        String local_ip = getIpAddress().toString();
        for (int i = 0; i < ips.length; i++) {
            String ip_port = ips[i];
            if (!mChatClients.containsKey(ip_port)) {
                String[] tokens = ip_port.trim().split(":");
                if (!tokens[0].equals(local_ip)) {
                    InetAddress addr;
                    try {
                        addr = InetAddress.getByName(tokens[0].replace("/",""));
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
        sendDirectMessage(ip_port, UPDATE_CLIENT_SERVER_IP, mChatServer.getPort(), DUMMY);
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
            mChatClients.get(ip_port).tearDown(flood);
            mChatClients.remove(ip_port);
        }
        else {
            Log.d(TAG, "Client manager doesn't has IP = " + ip_port);
        }
    }

    public void sendDirectMessage(String ip_port, String type, String msg, String sender) {
        if (mChatClients.containsKey(ip_port)) {
            mChatClients.get(ip_port).addMessageToQueue(type, msg, sender);
        }
        else {
            Log.e(TAG, "Client list does not contain " + ip_port);
        }
    }

    public void sendMulticastMessage(String type, String msg, String sender) {
        if (!mChatClients.isEmpty()) {
            Iterator it = mChatClients.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ChatClient> entry = (Map.Entry<String, ChatClient>) it.next();
                entry.getValue().addMessageToQueue(type, msg, sender);
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

    private String patchMessage(String type, String msg, String sender) {
        return type + ":" + sender + ":" + msg;
    }

    private String[] unpatchMessage(String patchedMsg) {
        return patchedMsg.split(":", 3);
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

                    sendHandlerMessage("error",
                            mServerSocket.getInetAddress().toString()+":"+Integer.toString(mServerSocket.getLocalPort()), -1,false,SYSTEM_SENDER);

                    while (!Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        Socket socket = mServerSocket.accept();
                        Log.d(TAG, "Connected.");

                        int port = socket.getPort();
                        InetAddress address = socket.getInetAddress();
                        String ip_port = address.toString()+":"+Integer.toString(port);

                        connectToRemote(address, port, socket);

                        // mHostAddress is fulfilled only when user click connect to Host
                        if (mHostAddress == null) {
                            //TODO Showing name instead of ip_port makes more sense.
                            sendHandlerMessage("join", ip_port + "has joined", -1,false,SYSTEM_SENDER);
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
                    list += s_ip;
                    while (it.hasNext()) {
                        entry = (Map.Entry<String, ChatClient>) it.next();
                        while((s_ip = entry.getValue().getServerIpPort()) == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
//                            e.printStackTrace();
                            }
                        }
                        list += ("," + s_ip);
                    }

                    it = mChatClients.entrySet().iterator();
                    while (it.hasNext()) {
                        entry = (Map.Entry<String, ChatClient>) it.next();
                        while(!entry.getValue().sendMessage(UPDATE_CLIENT_LIST, list, DUMMY)) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
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

        private int QUEUE_CAPACITY = 100;
        private BlockingQueue<String> mMessageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        public ChatClient(InetAddress address, int port, Socket socket) {

            Log.d(CLIENT_TAG, "Creating chatClient");
            this.mAddress = address;
            this.PORT = port;
            this.mSocket = socket;

            mSendThread = new Thread(new SendingThread(mMessageQueue));
            mSendThread.start();
        }

        public void setServerAddressPort(String client_ip_port, String server_port) {
            String[] client_ip_port_token = client_ip_port.split(":", 2);
            Log.d(CLIENT_TAG, server_port);
            Log.d(CLIENT_TAG, client_ip_port);
            try {
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
        public InetAddress getServerAddress() {
            return mServerAddress;
        }
        public int getServerPort() {
            return mServerPort;
        }
        public void tearDown(boolean flood) {
            Log.e(CLIENT_TAG, "ClientChat Teardown");
            if (flood) {
                sendMessage(TEARDOWN_MESSAGE, "", "");
            }
            mSendThread.interrupt();
            mRecThread.interrupt();
        }

        public void addMessageToQueue(String type, String msg, String sender) {
            try {
                mMessageQueue.put(patchMessage(type, msg, sender));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean sendMessage(String type, String msg, String sender) {
            try {
                if (mSocket == null) {
                    Log.e(CLIENT_TAG, "Socket is null");
                    return false;
                }
                else if (mSocket.getOutputStream() == null) {
                    Log.e(CLIENT_TAG, "Socket output stream is null");
                    return false;
                }

                PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(mSocket.getOutputStream())), true);
                if (type != null) {
                    out.println(patchMessage(type, msg, sender));
                }
                else {
                    out.println(msg);

                    String[] t = unpatchMessage(msg);
                    if (t[0].equals(CHAT_MESSAGE)) {
                        // Send Handler message here will result in multiple message.
                        // Commenting out.
                        //sendHandlerMessage("chat", t[2], -1, true, t[1]);
                    }
                }

                out.flush();

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

            private final BlockingQueue<String> mMessageQueue;

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

                while (true) {
                    try {
                        String msg = mMessageQueue.take();
                        String type = unpatchMessage(msg)[0];
                        if (type.equals(UPDATE_CLIENT_SERVER_IP)) {
                            msg += ("," + getLocalIpPort());
                            Log.d(CLIENT_TAG, getLocalIpPort());
                        }
                        sendMessage(null, msg, null);
                    } catch (InterruptedException ie) {
                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
                    }
                }
            }
        }


        class ReceivingThread implements Runnable {
            @Override
            public void run() {

                BufferedReader input;
                try {
                    input = new BufferedReader(new InputStreamReader(
                            mSocket.getInputStream()));
                    while (!Thread.currentThread().isInterrupted() && !mSocket.isClosed()) {
                        String messageStr;
                        messageStr = input.readLine();
                        if (messageStr != null) {
                            Log.d(CLIENT_TAG, "Read from the stream: " + messageStr);

                            String[] data = unpatchMessage(messageStr);
                            String type = data[0],
                                   sender = data[1],
                                   msg = data[2],
                                   ip_port = mAddress.toString()+":"+Integer.toString(PORT);

                            if (type.equals(CHAT_MESSAGE)) {
//                                updateMessages(msg, false);
                                sendHandlerMessage("chat", msg, -1, false, sender);
                            }
                            else if (type.equals(TEARDOWN_MESSAGE)) {
                                //sendHandlerMessage("leave", ip_port, -1,false,SYSTEM_SENDER);
                                sendHandlerMessage("leave", ip_port + "has left", -1,false,SYSTEM_SENDER);
                                tearDownClientWithIp(ip_port, false);
                            }
                            else if (type.equals(UPDATE_CLIENT_LIST)) {
                                updateClientList(msg);
                            }
                            else if (type.equals(UPDATE_CLIENT_SERVER_IP)) {
                                String[] ips = msg.split(",", 2);
                                Log.d(CLIENT_TAG, ips[1]);
                                updateClientServerIp(ips[1], ips[0]);
                            }
                        } else {
                            Log.d(CLIENT_TAG, "The nulls! The nulls!");
                            break;
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
                }
            }
        }
    }
}

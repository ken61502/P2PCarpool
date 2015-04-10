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
    private static final String TEARDOWN_MESSAGE = "tear_down";
    private static final String CHAT_MESSAGE = "chat";
    private int mPort = -1;

    public ChatConnection(Handler handler) {
        mUpdateHandler = handler;
        mChatServer = new ChatServer(handler);
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

    public void connectToServer(InetAddress address, int port, Socket socket) {
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
    public void sendMulticastMessage(String type, String msg) {
        if (!mChatClients.isEmpty()) {
            Iterator it = mChatClients.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ChatClient> entry = (Map.Entry<String, ChatClient>) it.next();
                entry.getValue().sendMessage(type, msg);
            }
        }
        else {
            Log.d(TAG, "Client list is empty");
        }
    }
    
    public int getLocalPort() {
        return mPort;
    }
    
    public void setLocalPort(int port) {
        mPort = port;
    }

    public synchronized void updateMessages(String msg, boolean local) {
        Log.e(TAG, "Updating message: " + msg);

        if (local) {
            msg = "me: " + msg;
        } else {
            msg = "them: " + msg;
        }

        sendHandlerMessage("chat", msg, -1);

    }

    private String patchMessage(String type, String msg) {
        return type + ":" + msg;
    }

    private String[] unpatchMessage(String patchedMsg) {
        return patchedMsg.split(":", 2);
    }

    public synchronized void sendHandlerMessage(String op, String msg, int id) {
        Bundle messageBundle = new Bundle();
        messageBundle.putString("op", op);
        messageBundle.putString("msg", msg);
        messageBundle.putInt("id", id);
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
///////
                    sendHandlerMessage("error",
                            mServerSocket.getInetAddress().toString()+":"+Integer.toString(mServerSocket.getLocalPort()), -1);
///////
                    while (!Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        Socket socket = mServerSocket.accept();
                        Log.d(TAG, "Connected.");

                        int port = socket.getPort();
                        InetAddress address = socket.getInetAddress();
                        String ip_port = address.toString()+":"+Integer.toString(port);
///////
                        sendHandlerMessage("join", ip_port, -1);
///////
//                        if (!clientSocket.containsKey(ip_port)) {
//                            clientSocket.put(ip_port, socket);
                        connectToServer(address, port, socket);
//                        }
//                        else {
//                            Log.d(TAG, "Client already connected.");
//                        }
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
    }
    /*
     *  ChatClient Class
     */
    private class ChatClient {

        private InetAddress mAddress;
        private int PORT;

        private final String CLIENT_TAG = "ChatClient";

        private Socket mSocket;
        private Thread mSendThread;
        private Thread mRecThread;
        private ObjectOutputStream mOutStream = null;
        public ChatClient(InetAddress address, int port, Socket socket) {

            Log.d(CLIENT_TAG, "Creating chatClient");
            this.mAddress = address;
            this.PORT = port;
            this.mSocket = socket;

            mSendThread = new Thread(new SendingThread());
            mSendThread.start();
        }
        public void tearDown(boolean flood) {
            Log.e(CLIENT_TAG, "ClientChat Teardown");
            if (flood) {
                sendMessage(TEARDOWN_MESSAGE, "");
            }
            mSendThread.interrupt();
            mRecThread.interrupt();
        }

        public void sendMessage(String type, String msg) {
            try {
                if (mSocket == null) {
                    Log.d(CLIENT_TAG, "Socket is null, wtf?");
                } else if (mSocket.getOutputStream() == null) {
                    Log.d(CLIENT_TAG, "Socket output stream is null, wtf?");
                }

//                if (mOutStream == null) {
//                    mOutStream = new ObjectOutputStream(mSocket.getOutputStream());
//                }
//
//                mOutStream.writeObject(new GroupMessage(type, msg));
                PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(mSocket.getOutputStream())), true);
                /////////Temp///////
                out.println(patchMessage(type, msg));
                ////////////////////

                out.flush();
//                mOutStream.flush();
                if (type.equals(CHAT_MESSAGE)) {
                    updateMessages(msg, true);
                }
            } catch (UnknownHostException e) {
                Log.d(CLIENT_TAG, "Unknown Host", e);
            } catch (IOException e) {
                Log.d(CLIENT_TAG, "I/O Exception", e);
            } catch (Exception e) {
                Log.d(CLIENT_TAG, "Error3", e);
            }
            Log.d(CLIENT_TAG, "Client sent message: " + msg);
        }


        class SendingThread implements Runnable {

            BlockingQueue<String> mMessageQueue;
            private int QUEUE_CAPACITY = 10;

            public SendingThread() {
                mMessageQueue = new ArrayBlockingQueue<String>(QUEUE_CAPACITY);
            }

            @Override
            public void run() {
                try {
                    if (mSocket == null) {
                        mSocket = new Socket(mAddress, PORT);
                        Log.d(CLIENT_TAG, "Client-side socket initialized.");

                    } else {
                        Log.d(CLIENT_TAG, "Socket already initialized. skipping!");
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
                        sendMessage(CHAT_MESSAGE, msg);
                    } catch (InterruptedException ie) {
                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
//                        try {
//                            mOutStream.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }
                }
            }
        }


        class ReceivingThread implements Runnable {

            @Override
            public void run() {

                BufferedReader input;
//                ObjectInputStream input;
                try {
                    input = new BufferedReader(new InputStreamReader(
                            mSocket.getInputStream()));
//                    input = new ObjectInputStream(mSocket.getInputStream());
                    while (!Thread.currentThread().isInterrupted() && !mSocket.isClosed()) {
//                        GroupMessage messageStr;
//                        messageStr = (GroupMessage) input.readObject();
                        String messageStr;
                        messageStr = input.readLine();
                        if (messageStr != null) {
                            Log.d(CLIENT_TAG, "Read from the stream: " + messageStr);

                            /////////Temp///////
                            String[] data = unpatchMessage(messageStr);
                            String type = data[0],
                                   msg = data[1],
                                   ip_port = mAddress.toString()+":"+Integer.toString(PORT);
                            /////////Temp///////

//                            updateMessages(messageStr, false);

                            if (type.equals(CHAT_MESSAGE)) {
                                updateMessages(msg, false);
                            }
                            else if (type.equals(TEARDOWN_MESSAGE)) {
                                sendHandlerMessage("leave", ip_port, -1);
                                tearDownClientWithIp(ip_port, false);
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
//                catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
            }
        }
    }
}

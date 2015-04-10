package edu.cmu.group08.p2pcarpool;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

import edu.cmu.group08.p2pcarpool.broadcast.BroadcastPacket;
import edu.cmu.group08.p2pcarpool.broadcast.ListenBroadcast;
import edu.cmu.group08.p2pcarpool.broadcast.SendBroadcast;


public class GroupRoomActivity extends ActionBarActivity {
    private static final int SEND_PORT = 2662;
    private static final int FROM_SEARCH_LISTEN_PORT = 2562;
    private static final int TO_SEARCH_LISTEN_PORT = 2560;
    private static final int UPDATE_LOG = 0;

    private ArrayList<ListenBroadcast> mListenerList = new ArrayList<>();

    private WifiManager mWifi = null;
    private DatagramSocket mReceiveBroadcastSocket = null;
    private DatagramSocket mSendBroadcastSocket = null;
    private Button mListenRiderButton = null;
    private TextView mLog = null;
    private Handler mReceiveHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_room);

        mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mListenRiderButton = (Button) findViewById(R.id.listen_rider);
        mLog = (TextView) findViewById(R.id.listen_rider_log);

        try {
            mSendBroadcastSocket = new DatagramSocket(SEND_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            mReceiveBroadcastSocket = new DatagramSocket(FROM_SEARCH_LISTEN_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        setupHandler();
        setupListener();
    }

    private void setupHandler() {
        mReceiveHandler= new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == UPDATE_LOG){
                    mLog.append(String.format("%s\n", msg.obj));
                    BroadcastPacket packet = new BroadcastPacket(TO_SEARCH_LISTEN_PORT, SEND_PORT, "data");
                    new SendBroadcast(mWifi, mSendBroadcastSocket, packet).start();
                }
                super.handleMessage(msg);
            }
        };
    }

    private void setupListener() {
        /*
         *  Broadcast data receiving handler
         */
        mListenRiderButton.setOnClickListener(new View.OnClickListener() {
            ListenBroadcast listener = null;
            private boolean start = false;
            @Override
            public void onClick(View v) {
                if (!start) {
                    if (listener == null) {
                        listener = new ListenBroadcast(
                                mWifi, mReceiveBroadcastSocket, mReceiveHandler, FROM_SEARCH_LISTEN_PORT);
                        listener.start();
                    }
                    else {
                        listener.resumeThread();
                    }
                    mListenRiderButton.setText("Pause Listening");
                    start = true;
                }
                else {
                    mListenRiderButton.setText("Resume Listen Rider");
                    start = false;
                    listener.pauseThread();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSendBroadcastSocket.close();
        mReceiveBroadcastSocket.close();
    }

        @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

            mListenRiderButton.setText("Listen Rider");
        for (ListenBroadcast listener : mListenerList) {
            listener.stopThread();
        }
    }
    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        setupHandler();
        setupListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group_room, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

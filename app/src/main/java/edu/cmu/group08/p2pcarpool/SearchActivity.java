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


public class SearchActivity extends ActionBarActivity {

    private static final int SEND_PORT = 2660;
    private static final int TO_GROUP_LISTEN_PORT = 2562;
    private static final int FROM_GROUP_LISTEN_PORT = 2560;
    private static final int UPDATE_LOG = 0;

    private ArrayList<ListenBroadcast> mListenerList = new ArrayList<>();

    private WifiManager mWifi = null;
    private DatagramSocket mReceiveBroadcastSocket = null;
    private DatagramSocket mSendBroadcastSocket = null;

    private Button mSendBroadcastButton = null;
    private Button mReceiveBroadcastButton = null;

    private TextView mLog = null;
    private Handler mReceiveHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mSendBroadcastButton = (Button) findViewById(R.id.send_bc);
        mReceiveBroadcastButton = (Button) findViewById(R.id.listen_bc);
        mLog = (TextView) findViewById(R.id.search_log);

        try {
            mSendBroadcastSocket = new DatagramSocket(SEND_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            mReceiveBroadcastSocket = new DatagramSocket(FROM_GROUP_LISTEN_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        setupHandler();
        setupListener();
    }

    private void setupHandler() {
        mReceiveHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == UPDATE_LOG){
                    mLog.append(String.format("%s\n", msg.obj));
                }
                super.handleMessage(msg);
            }
        };
    }

    public void setupListener() {
        /*
         *  Broadcast data sending handler
         */
        mSendBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BroadcastPacket packet = new BroadcastPacket(TO_GROUP_LISTEN_PORT, SEND_PORT, "data");
                new SendBroadcast(mWifi, mSendBroadcastSocket, packet).start();
            }
        });

        /*
         *  Broadcast data receiving handler
         */
        mReceiveBroadcastButton.setOnClickListener(new View.OnClickListener() {
            ListenBroadcast listener = null;
            private boolean start = false;
            @Override
            public void onClick(View v) {
                if (!start) {
                    if (listener == null) {
                        listener = new ListenBroadcast(
                                mWifi, mReceiveBroadcastSocket, mReceiveHandler, FROM_GROUP_LISTEN_PORT);
                        mListenerList.add(listener);
                        listener.start();
                    }
                    else {
                        listener.resumeThread();
                    }
                    mReceiveBroadcastButton.setText("Pause Listening");
                    start = true;
                }
                else {
                    mReceiveBroadcastButton.setText("Resume Listen BC");
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

        mReceiveBroadcastButton.setText("Listen BC");
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
        getMenuInflater().inflate(R.menu.menu_search, menu);
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

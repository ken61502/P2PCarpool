package edu.cmu.group08.p2pcarpool.fragment;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.group08.p2pcarpool.connection.ChatConnection;
import edu.cmu.group08.p2pcarpool.connection.NsdHelper;
import edu.cmu.group08.p2pcarpool.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HostFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HostFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "CarpoolHost";
    private static final String PROFILE_NAME = "Profile";
    private static final String TEARDOWN_MESSAGE = "tear_down";
    private static final String CHAT_MESSAGE = "chat";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ChatConnection mConnection = null;
    private NsdHelper mNsdHelper = null;
    private Handler mUpdateHandler;
    private SharedPreferences mSettings;
    private WifiManager mWifi;

    private TextView mMessage_window;
    private Button mSendBtn;
    private EditText mEditMsg;
    private ListView listViewMessages;
    private List<Message> listMessages;
    private MessagesListAdapter adapter;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HostFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HostFragment newInstance(String param1, String param2) {
        HostFragment fragment = new HostFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public HostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        mSettings = getActivity().getSharedPreferences(PROFILE_NAME, 0);
        mWifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);

//        mUpdateHandler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                String operation = msg.getData().getString("op");
//                String message = msg.getData().getString("msg");
//                int group_id = msg.getData().getInt("id");
//
//                if (operation.equals("chat")) {
//                    Log.d(TAG, "Received Message: " + message);
//                    addChatLine(message);
//                }
//                else if (operation.equals("error")) {
//                    Log.e(TAG, "Received Debug: " + message);
////                    addChatLine(message);
//                }
//                else if (operation.equals("join")) {
//                    Log.e(TAG, message + " has joined");
//                    addChatLine(message + " has joined");
//                }
//                else if (operation.equals("leave")) {
//                    Log.e(TAG, message + " has leaved");
//                    addChatLine(message + " has leaved");
//                }
//            }
//        };
        mUpdateHandler = new Handler() {
            //Receiver side message should be displayed at left.

            @Override
            public void handleMessage(Message msg) {
                String operation = msg.getData().getString("op");
                String message = msg.getData().getString("msg");
                int group_id = msg.getData().getInt("id");
                if (operation.equals("chat")) {
                    Log.d(TAG, "Sender:" + msg.getData().getString("sender"));
                    Log.d(TAG, "Message:" + msg.getData().getString("msg"));
                    Log.d(TAG, "Self:" + msg.getData().getBoolean("self"));
                    addChatLine(msg);
                }
                else if (operation.equals("error")) {
                    Log.e(TAG, "Received Debug: " + message);
//                    addChatLine(message);
                }
                else if (operation.equals("join")) {
                    Log.e(TAG, message + " has joined");
                //TODO add system message display in the middle
//                    addChatLine(message + " has joined");
                }
                else if (operation.equals("leave")) {
                    Log.e(TAG, message + " has leaved");
                //TODO
//                    addChatLine(message + " has left");
                }
            }
        };

        if (mConnection == null) {
            mConnection = new ChatConnection(mUpdateHandler, mWifi);
        }
        if (mNsdHelper == null) {
            mNsdHelper = new NsdHelper(
                    getActivity(),
                    mUpdateHandler,
                    mSettings.getString("destination", "5717 Hobart St. Pittsburgh, 15213")
            );
            mNsdHelper.initializeNsd();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_host, container, false);

        initializeView(view);
        while (mConnection.getLocalPort() < 0) {
            try {
                Log.d(TAG, "ServerSocket isn't bound.");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mNsdHelper.registerService(mConnection.getLocalPort());
        Log.d(TAG, "Start Advertising.");

        return view;
    }
    public void initializeView(View view) {
        mEditMsg = (EditText) view.findViewById(R.id.msg_input);
        mEditMsg.setText("");

        //mMessage_window = (TextView) view.findViewById(R.id.message_window);
        //mMessage_window.setMovementMethod(new ScrollingMovementMethod());
        listViewMessages = (ListView) view.findViewById(R.id.message_window);
        listMessages = new ArrayList<Message>();
        Context context = getActivity().getApplicationContext();
        adapter = new MessagesListAdapter(context, listMessages);
        listViewMessages.setAdapter(adapter);

        mSendBtn = (Button) view.findViewById(R.id.send_btn);
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickSend(v);
            }
        });
    }
//    public void clickSend(View v) {
//        if (mEditMsg != null) {
//            String msg = mEditMsg.getText().toString();
//            if (!msg.isEmpty()) {
//                addChatLine(msg);
//                mConnection.sendMulticastMessage(CHAT_MESSAGE, msg);
//                mEditMsg.setText("");
//            }
//        }
//    }
    public void clickSend(View v) {
        if (mEditMsg != null) {
            String msg = mEditMsg.getText().toString();
            if (!msg.isEmpty()) {

                mConnection.sendMulticastMessage(CHAT_MESSAGE, msg, mSettings.getString("name","Invalid"));
                mEditMsg.setText("");
            }
        }
    }
//    public void addChatLine(String line) {
//        mMessage_window.append("\n" + line);
//    }

    public void addChatLine(Message msg) {
        //Attention
        //Must do a copy of the message and then add it to
        Bundle messageBundle = new Bundle();
        messageBundle.putString("op", CHAT_MESSAGE);
        messageBundle.putString("sender", msg.getData().getString("sender"));
        messageBundle.putBoolean("self",msg.getData().getBoolean("self"));
        messageBundle.putString("msg", msg.getData().getString("msg"));
        messageBundle.putInt("id", -1);
        Message message = new Message();
        message.setData(messageBundle);
        listMessages.add(message);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.tearDown();
            mNsdHelper.stopDiscovery();
            mNsdHelper = null;
        }
        if (mConnection != null) {
            mConnection.tearDown();
            mConnection = null;
        }

        super.onPause();

    }
    @Override
    public void onResume() {
        super.onResume();

        if (mConnection == null) {
            mConnection = new ChatConnection(mUpdateHandler, mWifi);
        }
        if (mNsdHelper == null) {
            mNsdHelper = new NsdHelper(
                    getActivity(),
                    mUpdateHandler,
                    mSettings.getString("destination", "5717 Hobart St. Pittsburgh, 15213")
            );
            mNsdHelper.initializeNsd();
        }
        while (mConnection.getLocalPort() < 0) {
            try {
                Log.d(TAG, "ServerSocket isn't bound.");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mNsdHelper.registerService(mConnection.getLocalPort());
        Log.d(TAG, "Start Advertising.");
    }
    @Override
    public void onStop() {
        if (mNsdHelper != null) {
            mNsdHelper.tearDown();
            mNsdHelper.stopDiscovery();
            mNsdHelper = null;
        }
        if (mConnection != null) {
            mConnection.tearDown();
            mConnection = null;
        }
        super.onStop();
    }
}

package edu.cmu.group08.p2pcarpool.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.group08.p2pcarpool.connection.ChatConnection;
import edu.cmu.group08.p2pcarpool.connection.NsdHelper;
import edu.cmu.group08.p2pcarpool.R;
import edu.cmu.group08.p2pcarpool.connection.Settings;
import edu.cmu.group08.p2pcarpool.gmap.GeocoderHelper;
import edu.cmu.group08.p2pcarpool.group.GroupContent;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnGroupedSelectedListener}
 * interface.
 */
public class SearchFragment extends Fragment implements AbsListView.OnItemClickListener {

    public static final String TAG = "CarpoolGroupList";

    private ChatConnection mConnection = null;
    private NsdHelper mNsdHelper = null;
    private Handler mUpdateHandler;

    private OnGroupedSelectedListener mListener;

    private WifiManager mWifi;
    private SharedPreferences mSettings;
    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    //private TextView mStatusView;
    private ListView mStatusView;
    private List<Message> listMessages;
    private MessagesListAdapter adapter;

    private GeocoderHelper mGeocoder;

    private Button mSendBtn;

    private EditText mEditMsg;
//    private TextView mText;
    private LinearLayout linearLayout;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new GroupContent.GroupAdapter(getActivity(), GroupContent.ITEMS);
//        mAdapter = new ArrayAdapter<GroupContent.Group>(getActivity(),
//                android.R.layout.simple_list_item_1, android.R.id.text1, GroupContent.ITEMS);

        mWifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        mSettings = getActivity().getSharedPreferences(Settings.PROFILE_NAME, 0);
        mGeocoder = new GeocoderHelper(getActivity(), mSettings);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grouplist, container, false);

        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        mStatusView = (ListView) view.findViewById(R.id.message_window);
        listMessages = new ArrayList<Message> ();
        Context context = getActivity().getApplicationContext();
        adapter = new MessagesListAdapter(context, listMessages);
        mStatusView.setAdapter(adapter);

        initializeOnClickListener(view);

        mEditMsg = (EditText) view.findViewById(R.id.msg_input);
        mEditMsg.setText("");

        mUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String operation = msg.getData().getString("op");
                String message = msg.getData().getString("msg");
                int group_id = msg.getData().getInt("id");

                if (operation.equals("add")) {
                    Log.e(TAG, message);
                    String[] tokens = message.split(":", 2);
                    if (tokens.length == 2) {
                        tokens = tokens[1].split("#");
                        if (tokens.length == 3) {
                            String dest = tokens[0], price = tokens[1], max = tokens[2];
                            String dist = mGeocoder.calcDestinationDistance(dest);
                            GroupContent.addItem(new GroupContent.Group(group_id, dest, dist, "", "", max));
                        }
                    }

                    //TODO
                }
                else if (operation.equals("remove")) {
                    GroupContent.removeItem(group_id);
                    //TODO
                }
                else if (operation.equals("chat")) {
                    Log.d(TAG, "Received Message: " + message);
                    addChatLine(msg);
//                    GroupContent.clearAll();
                }
                else if (operation.equals("error")) {
                    Log.e(TAG, "Received Debug: " + message);
                    if (message.equals(Settings.CONNECTED_MSG)) {
                        setVisibilityMode(true);
                    }
                    addChatLine(msg);
//                    GroupContent.clearAll();
                }
                else if (operation.equals("clear_all")) {
                    GroupContent.clearAll();
                }
                else if (operation.equals("join")) {
                    Log.e(TAG, message + " has joined");
                    addChatLine(msg);
                }
                else if (operation.equals("leave")) {
                    Log.e(TAG, message + " has left");
//                    addChatLine(msg);
                }
                ((GroupContent.GroupAdapter) mAdapter).notifyDataSetChanged();
            }
        };

        if (mConnection == null) {
            mConnection = new ChatConnection(mSettings.getString("name", "No Name"), mUpdateHandler, mWifi);
        }
        if (mNsdHelper == null) {
            mNsdHelper = new NsdHelper(
                    getActivity(),
                    mUpdateHandler,
                    mSettings.getString("destination", "5717 Hobart St. Pittsburgh, 15213")
            );
            mNsdHelper.initializeNsd();
            mNsdHelper.discoverServices();
        }

//        mText = (TextView) view.findViewById(R.id.searchText);
        linearLayout = (LinearLayout) view.findViewById(R.id.llMsgCompose);
        setVisibilityMode(false);
        return view;
    }

    public void initializeOnClickListener(View view) {

        mSendBtn = (Button) view.findViewById(R.id.send_btn);

        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickSend(v);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnGroupedSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            int groupId = GroupContent.ITEMS.get(position).id;
            mListener.onGroupedSelected(groupId);
            mNsdHelper.selectGroup(groupId);
            Thread runConnect = new Thread(new ConnectHostThread(mNsdHelper, mConnection));
            runConnect.start();
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnGroupedSelectedListener {
        public void onGroupedSelected(Integer id);
    }

    public void clickSend(View v) {
        if (mEditMsg != null) {
            String msg = mEditMsg.getText().toString();
            if (!msg.isEmpty()) {
                mConnection.sendHandlerMessage("chat", msg, -1, true, mSettings.getString("name","Invalid"));
                mConnection.sendMulticastMessage(Settings.CHAT_MESSAGE, msg, mSettings.getString("name","Invalid"));
                mEditMsg.setText("");
            }
        }
    }

    public void addChatLine(Message msg) {
        Bundle messageBundle = new Bundle();
        messageBundle.putString("op", Settings.CHAT_MESSAGE);
        messageBundle.putString("sender", msg.getData().getString("sender"));
        messageBundle.putBoolean("self",msg.getData().getBoolean("self"));
        messageBundle.putString("msg", msg.getData().getString("msg"));
        messageBundle.putInt("id", -1);
        Message message = new Message();
        message.setData(messageBundle);
        listMessages.add(message);
        adapter.notifyDataSetChanged();
    }

    private void setVisibilityMode(boolean chatting) {
        if (chatting) {
//            mText.setVisibility(View.GONE);
            linearLayout.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            mStatusView.setVisibility(View.VISIBLE);
        }
        else {
//            mText.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
            mStatusView.setVisibility(View.GONE);
        }
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
        setVisibilityMode(false);
        super.onPause();

    }
    @Override
    public void onResume() {
        super.onResume();

        if (mConnection == null) {
            mConnection = new ChatConnection(mSettings.getString("name", "No Name"), mUpdateHandler, mWifi);
        }
        if (mNsdHelper == null) {
            mNsdHelper = new NsdHelper(
                    getActivity(),
                    mUpdateHandler,
                    mSettings.getString("destination", "5717 Hobart St. Pittsburgh, 15213")
            );
            mNsdHelper.initializeNsd();
            mNsdHelper.discoverServices();
        }
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
        setVisibilityMode(false);
        super.onStop();
    }

    private class ConnectHostThread implements Runnable {

        private ChatConnection mConnection = null;
        private NsdHelper mNsdHelper = null;

        ConnectHostThread(NsdHelper nsd, ChatConnection conn) {
            mNsdHelper = nsd;
            mConnection = conn;
        }

        @Override
        public void run() {
            NsdServiceInfo service;
            int counter = 0;
            while ((service = mNsdHelper.getChosenServiceInfo()) == null) {
                try {
                    if (counter > 20) {
                        break;
                    }
                    Thread.sleep(500);
                    counter++;
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
            if (service != null) {
                Log.d(TAG, "Connecting.");
                mConnection.connectToHost(service.getHost(),
                        service.getPort(), null);
                mConnection.sendHandlerMessage("error", Settings.CONNECTED_MSG, -1, false, Settings.SYSTEM_SENDER);
            } else {
                Log.d(TAG, "No service to connect to!");
            }
        }
    }

//    @Override
//    public void onDestroy() {
//        mNsdHelper.tearDown();
//        mConnection.tearDown();
//
//        super.onDestroy();
//    }
}

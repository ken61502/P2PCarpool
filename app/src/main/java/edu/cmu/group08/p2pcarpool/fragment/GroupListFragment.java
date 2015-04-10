package edu.cmu.group08.p2pcarpool.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ListAdapter;
import android.widget.TextView;

import edu.cmu.group08.p2pcarpool.connection.ChatConnection;
import edu.cmu.group08.p2pcarpool.connection.NsdHelper;
import edu.cmu.group08.p2pcarpool.R;
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
public class GroupListFragment extends Fragment implements AbsListView.OnItemClickListener {

    public static final String TAG = "CarpoolGroupList";

    private ChatConnection mConnection = null;
    private NsdHelper mNsdHelper = null;
    private Handler mUpdateHandler;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String PROFILE_NAME = "Profile";
    private static final String TEARDOWN_MESSAGE = "tear_down";
    private static final String CHAT_MESSAGE = "chat";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnGroupedSelectedListener mListener;


    private SharedPreferences mSettings;
    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;
    private TextView mStatusView;
    private EditText mEditMsg;
    private Button mAdvertiseBtn;
    private Button mDiscoverBtn;
    private Button mConnectBtn;

    private Button mDeadvertiseBtn;
    private Button mUndiscoverBtn;
    private Button mDisconnectBtn;

    private Button mSendBtn;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    // TODO: Rename and change types of parameters
    public static GroupListFragment newInstance(String param1, String param2) {
        GroupListFragment fragment = new GroupListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GroupListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        mAdapter = new ArrayAdapter<GroupContent.Group>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, GroupContent.ITEMS);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grouplist, container, false);

        mSettings = getActivity().getSharedPreferences(PROFILE_NAME, 0);

        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        mStatusView = (TextView) view.findViewById(R.id.status);
        mStatusView.setMovementMethod(new ScrollingMovementMethod());
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
                    GroupContent.addItem(new GroupContent.Group(group_id, message));
                }
                else if (operation.equals("remove")) {
                    GroupContent.removeItem(group_id);
                }
                else if (operation.equals("chat")) {
                    Log.d(TAG, "Received Message: " + message);
                    addChatLine(message);
//                    GroupContent.clearAll();
                }
                else if (operation.equals("error")) {
                    Log.e(TAG, "Received Debug: " + message);
                    addChatLine(message);
//                    GroupContent.clearAll();
                }
                else if (operation.equals("clear_all")) {
                    GroupContent.clearAll();
                }

                ((ArrayAdapter) mAdapter).notifyDataSetChanged();
            }
        };

        if (mConnection == null) {
            mConnection = new ChatConnection(mUpdateHandler);
        }
        if (mNsdHelper == null) {
            mNsdHelper = new NsdHelper(
                    getActivity(),
                    mUpdateHandler,
                    mSettings.getString("destination", "5717 Hobart St. Pittsburgh, 15213")
            );
            mNsdHelper.initializeNsd();
        }

        return view;
    }

    public void initializeOnClickListener(View view) {
        mAdvertiseBtn = (Button) view.findViewById(R.id.advertise_btn);
        mDiscoverBtn = (Button) view.findViewById(R.id.discover_btn);
        mConnectBtn = (Button) view.findViewById(R.id.connect_btn);

        mDeadvertiseBtn = (Button) view.findViewById(R.id.deadvertise_btn);
        mUndiscoverBtn = (Button) view.findViewById(R.id.undiscover_btn);
        mDisconnectBtn = (Button) view.findViewById(R.id.disconnect_btn);

        mSendBtn = (Button) view.findViewById(R.id.send_btn);

        mAdvertiseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickAdvertise(v);
            }
        });
        mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickDiscover(v);
            }
        });
        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickConnect(v);
            }
        });

        mDeadvertiseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickDeadvertise(v);
            }
        });
        mUndiscoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickUndiscover(v);
            }
        });
        mDisconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickDisconnect(v);
            }
        });
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


    /*
     *  Testing Button and TextView
     */
    public void clickAdvertise(View v) {
        // Register service
        if (mConnection.getLocalPort() > -1) {
            mNsdHelper.registerService(mConnection.getLocalPort());
        } else {
            Log.d(TAG, "ServerSocket isn't bound.");
            addChatLine("ServerSocket isn't bound.");
        }
    }
    public void clickDiscover(View v) {
        mNsdHelper.discoverServices();
    }
    public void clickConnect(View v) {
        NsdServiceInfo service = mNsdHelper.getChosenServiceInfo();
        if (service != null) {
            Log.d(TAG, "Connecting.");
            mConnection.connectToServer(service.getHost(),
                    service.getPort(), null);
        } else {
            Log.d(TAG, "No service to connect to!");
            addChatLine("No service to connect to!");
        }
    }
    public void clickDeadvertise(View v) {
        mNsdHelper.tearDown();
    }

    public void clickUndiscover(View v) {
        mNsdHelper.stopDiscovery();
    }

    public void clickDisconnect(View v) {
        mConnection.disconnectToServer();
    }

    public void clickSend(View v) {
        if (mEditMsg != null) {
            String msg = mEditMsg.getText().toString();
            if (!msg.isEmpty()) {
                addChatLine(msg);
                mConnection.sendMulticastMessage(CHAT_MESSAGE, msg);
                mEditMsg.setText("");
            }
        }
    }

    public void addChatLine(String line) {
        mStatusView.append("\n" + line);
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
            mConnection = new ChatConnection(mUpdateHandler);
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

//    @Override
//    public void onDestroy() {
//        mNsdHelper.tearDown();
//        mConnection.tearDown();
//
//        super.onDestroy();
//    }
}

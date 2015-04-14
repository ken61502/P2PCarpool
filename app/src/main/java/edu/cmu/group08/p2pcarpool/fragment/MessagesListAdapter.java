package edu.cmu.group08.p2pcarpool.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import edu.cmu.group08.p2pcarpool.R;

/**
 * Created by kaidiy on 4/12/15.
 */
public class MessagesListAdapter extends BaseAdapter {
    private static final String TAG = "CarpoolHost";
    private Context context;
    private List<Message> messagesItems;
    private SharedPreferences mSettings;

    public MessagesListAdapter(Context context, List<Message> navDrawerItems) {
        this.context = context;
        this.messagesItems = navDrawerItems;
    }

    @Override
    public int getCount() {
        return messagesItems.size();
    }

    @Override
    public Object getItem(int position) {
        return messagesItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        /**
         * The following list not implemented reusable list items as list items
         * are showing incorrect data Add the solution if you have one
         * */

        Message m = messagesItems.get(position);

        //LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);


        // Identifying the message owner
        //TODO Add message sender! null pointer check
        if (messagesItems.get(position).getData().getBoolean("self")) {
            // message belongs to you, so load the right aligned layout
            convertView = mInflater.inflate(R.layout.list_item_message_right,
                    null);
        } else {
            // message belongs to other person, load the left aligned layout
            convertView = mInflater.inflate(R.layout.list_item_message_left,
                    null);
        }

        TextView lblFrom = (TextView) convertView.findViewById(R.id.lblMsgFrom);
        TextView txtMsg = (TextView) convertView.findViewById(R.id.txtMsg);

        txtMsg.setText(m.getData().getString("msg"));
        //TODO Add sender name to the message!
        lblFrom.setText(messagesItems.get(position).getData().getString("sender"));

        Log.d(TAG, "List:Sender:" + messagesItems.get(position).getData().getString("sender"));
        Log.d(TAG, "List:Message:" + messagesItems.get(position).getData().getString("msg"));
        Log.d(TAG, "List:Self:" + messagesItems.get(position).getData().getBoolean("self"));


        return convertView;
    }
}
package edu.cmu.group08.p2pcarpool.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.cmu.group08.p2pcarpool.R;
import edu.cmu.group08.p2pcarpool.gmap.GeocoderHelper;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class GroupContent {

    /**
     * An array of sample (dummy) items.
     */
    public static List<Group> ITEMS = new ArrayList<Group>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static Map<Integer, Group> ITEM_MAP = new HashMap<>();

    static {
        // Add 3 sample items.
//        addItem(new Group(1, "Item 1"));
//        addItem(new Group(2, "Item 2"));
//        addItem(new Group(3, "Item 3"));
    }

    public static void addItem(Group item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }
    public static void removeItem(int id) {
        for (Iterator<Group> iterator = ITEMS.iterator(); iterator.hasNext();) {
            Group g = iterator.next();
            if (g.id == id) {
                iterator.remove();
                break;
            }
        }
        ITEM_MAP.remove(id);
    }
    public static void clearAll() {
        ITEMS.clear();
        ITEM_MAP.clear();
    }
    /**
     * A dummy item representing a piece of content.
     */
    public static class Group {
        public Integer id;
        public String destination;
        public String distance;
        public String price;
        public String car;
        public String max;

        public Group(int id, String dest, String dist, String p, String c, String m) {
            this.id = id;
            this.destination = dest;
            this.distance = dist;
            this.price = p;
            this.car = c;
            this.max = m;
        }

        @Override
        public String toString() {
            return destination + " " + distance;
        }
    }

    public static class GroupAdapter extends BaseAdapter {

        private Context mContext;
        private List<Group> mGroups;

        public GroupAdapter(Context context, List<Group> groups) {
            this.mContext = context;
            this.mGroups = groups;
        }

        @Override
        public int getCount() {
            return mGroups.size();
        }

        @Override
        public Object getItem(int position) {
            return mGroups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View groupListView;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                groupListView = inflater.inflate(R.layout.group_list_view, null);
            } else {
                groupListView = convertView;
            }

            TextView text_dest = (TextView) groupListView.findViewById(R.id.list_destination);
            TextView text_dist = (TextView) groupListView.findViewById(R.id.list_distance);
            TextView text_price = (TextView) groupListView.findViewById(R.id.list_price);
            TextView text_car = (TextView) groupListView.findViewById(R.id.list_car);
            TextView text_max = (TextView) groupListView.findViewById(R.id.list_max);

            text_dest.setText(mGroups.get(position).destination);
            text_dist.setText(mGroups.get(position).distance);
            text_price.setText(mGroups.get(position).price);
            text_car.setText(mGroups.get(position).car);
            text_max.setText(mGroups.get(position).max + " Passengers Max");

            return groupListView;
        }
    }
}

package edu.cmu.group08.p2pcarpool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;


public class ProfileActivity extends ActionBarActivity {

    public static final String PROFILE_NAME = "Profile";
    private static final String profile_item_list [] = {
            "name", "destination", "price_per_passenger", "destination_radius", "car", "max_passengers"};

    private ArrayList<ProfileItem> mList = null;
    private ListView mListView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences settings = getSharedPreferences(PROFILE_NAME, 0);

        mListView = (ListView) findViewById(R.id.list);
        mList = new ArrayList<>(Arrays.asList(
                new ProfileItem("Name", settings.getString("name", "Alice")),
                new ProfileItem("Destination", settings.getString("destination", "5717 Hobart St. Pittsburgh, 15213")),
                new ProfileItem("Price Per Passenger", settings.getString("price_per_passenger", "$ 10")),
                new ProfileItem("Destination Radius", settings.getString("destination_radius", "0.5 mile")),
                new ProfileItem("Car", settings.getString("car", "BMW 428")),
                new ProfileItem("Max Passengers", settings.getString("max_passengers", "4"))
        ));
        final ProfileItemAdapter adapter = new ProfileItemAdapter(this,
                R.layout.profile_item_row, mList);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                final int itemPosition = position;
                final String itemName = adapter.getItem(itemPosition).name;
                final String itemValue = adapter.getItem(itemPosition).value;

                // ListView Clicked item value
                AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());

                alert.setTitle(itemName);
                alert.setMessage("Please enter the value:");

                // Set an EditText view to get user input
                final EditText input = new EditText(view.getContext());

                if (itemName.startsWith("Price") || itemName.endsWith("Radius")) {
                    input.setRawInputType(
                            InputType.TYPE_CLASS_NUMBER |
                            InputType.TYPE_NUMBER_VARIATION_NORMAL |
                            InputType.TYPE_NUMBER_FLAG_DECIMAL
                    );
                }

                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            mList.set(itemPosition, new ProfileItem(itemName, itemValue));
                        }
                        else {
                            if (itemName.startsWith("Price")) {
                                value = "$ " + value;
                            }
                            if (itemName.endsWith("Radius")) {
                                value = value + " mile";
                            }
                            mList.set(itemPosition, new ProfileItem(itemName, value));
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                // Show Alert
//                Toast.makeText(getApplicationContext(),
//                        "Position :" + itemPosition + "  ListItem : " + itemValue, Toast.LENGTH_LONG)
//                        .show();

            }

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        saveProfileData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setting, menu);
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

    private void saveProfileData() {
        SharedPreferences settings = getSharedPreferences(PROFILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        for (int i = 0; i < profile_item_list.length; i++) {
            editor.putString(profile_item_list[i], mList.get(i).value);
        }
        editor.commit();
    }

    private class ProfileItemAdapter extends ArrayAdapter<ProfileItem>{

        Context context;
        int layoutResourceId;
        ArrayList<ProfileItem> data = null;

        public ProfileItemAdapter(Context context, int layoutResourceId, ArrayList<ProfileItem> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ProfileItemHolder holder;

            if(row == null)
            {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new ProfileItemHolder();
                holder.txtName = (TextView) row.findViewById(R.id.txtName);
                holder.txtValue = (TextView) row.findViewById(R.id.txtValue);

                row.setTag(holder);
            }
            else
            {
                holder = (ProfileItemHolder) row.getTag();
            }

            ProfileItem item = data.get(position);
            holder.txtName.setText(item.name);
            holder.txtValue.setText(item.value);

            return row;
        }

        private class ProfileItemHolder
        {
            TextView txtName;
            TextView txtValue;
        }
    }
}

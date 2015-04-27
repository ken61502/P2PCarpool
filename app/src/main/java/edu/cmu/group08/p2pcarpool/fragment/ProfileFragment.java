package edu.cmu.group08.p2pcarpool.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import edu.cmu.group08.p2pcarpool.R;
import edu.cmu.group08.p2pcarpool.gmap.GoogleMapAutoComplete;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ProfileFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String PROFILE_NAME = "Profile";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private static final String profile_item_list [] = {
            "name", "destination", "price_per_passenger", "destination_radius", "car", "max_passengers"};

    private ArrayList<ProfileItem> mList = null;
    private ListView mListView = null;
    private ProfileItemAdapter mAdapter = null;
    private OnFragmentInteractionListener mListener;

    private GoogleMapAutoComplete autoComplete = null;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        SharedPreferences settings = getActivity().getSharedPreferences(PROFILE_NAME, 0);
        mList = new ArrayList<>(Arrays.asList(
                new ProfileItem("Name", settings.getString("name", "Alice")),
                new ProfileItem("Destination", settings.getString("destination", "5717 Hobart St. Pittsburgh, 15213")),
                new ProfileItem("Price Per Passenger", settings.getString("price_per_passenger", "$ 10")),
                new ProfileItem("Destination Radius", settings.getString("destination_radius", "0.5 mile")),
                new ProfileItem("Car", settings.getString("car", "BMW 428")),
                new ProfileItem("Max Passengers", settings.getString("max_passengers", "4"))
        ));
        mAdapter = new ProfileItemAdapter(getActivity(), R.layout.profile_item_row, mList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mListView = (ListView) view.findViewById(R.id.list);

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                final int itemPosition = position;
                final String itemName = mAdapter.getItem(itemPosition).name;
                final String itemValue = mAdapter.getItem(itemPosition).value;

                // ListView Clicked item value
                AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());

                alert.setTitle(itemName);
                alert.setMessage("Please enter the value:");

                final EditText input;
                // Set an EditText view to get user input
                if (itemName.equals("Destination")) {
                    input = new AutoCompleteTextView(view.getContext());
                    autoComplete = new GoogleMapAutoComplete(view.getContext(), (AutoCompleteTextView) input);
                }
                else {
                    input = new EditText(view.getContext());
                    if (itemName.startsWith("Price") || itemName.endsWith("Radius")) {
                        input.setRawInputType(
                                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        );
                    }
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
                        mAdapter.notifyDataSetChanged();
                        autoComplete = null;
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            }

        });

        return view;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        saveProfileData();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
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

    private void saveProfileData() {
        SharedPreferences settings = getActivity().getSharedPreferences(PROFILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        for (int i = 0; i < profile_item_list.length; i++) {
            editor.putString(profile_item_list[i], mList.get(i).value);
        }
        editor.commit();
    }

    private class ProfileItemAdapter extends ArrayAdapter<ProfileItem> {

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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}

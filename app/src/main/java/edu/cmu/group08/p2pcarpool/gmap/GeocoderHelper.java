package edu.cmu.group08.p2pcarpool.gmap;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;


/**
 * Created by kenny on 2015/4/26.
 */
public class GeocoderHelper {

    private static final String TAG = "GEOCODER";

    private Geocoder mGeocoder;
    private SharedPreferences mSettings;


    public GeocoderHelper(Context context, SharedPreferences settings) {
        mGeocoder = new Geocoder(context);
        mSettings = settings;
    }
    public String calcDestinationDistance(String location) {
        try {
            location = location.replaceAll("\\([0-9]+\\)", "");
            String dest = mSettings.getString("destination", "5717 Hobart St. Pittsburgh, 15213");
            List<Address> candidates_loc  = mGeocoder.getFromLocationName(location, 10);
            List<Address> candidates_dest  = mGeocoder.getFromLocationName(dest, 10);
            double lat_loc, lng_loc, lat_dest, lng_dest;
            if (candidates_loc.size() > 0) {
                lat_loc = candidates_loc.get(0).getLatitude();
                lng_loc = candidates_loc.get(0).getLongitude();
            }
            else {
                Log.d(TAG, "No candidates");
                return "No distance data";
            }
            if (candidates_dest.size() > 0) {
                lat_dest = candidates_dest.get(0).getLatitude();
                lng_dest = candidates_dest.get(0).getLongitude();
            }
            else {
                Log.d(TAG, "No candidates");
                return "No distance data";
            }
            double distance;
            Location locationA = new Location("Loc");
            locationA.setLatitude(lat_loc);
            locationA.setLongitude(lng_loc);

            Location locationB = new Location("Dest");
            locationB.setLatitude(lat_dest);
            locationB.setLongitude(lng_dest);
            distance = locationA.distanceTo(locationB); //distance in meters

            return new DecimalFormat("#.##").format(distance / 1600.0) + " miles";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}

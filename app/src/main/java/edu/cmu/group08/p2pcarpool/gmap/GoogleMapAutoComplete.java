package edu.cmu.group08.p2pcarpool.gmap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.SimpleAdapter;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

/**
 * Created by kenny on 2015/4/13.
 */
public class GoogleMapAutoComplete {

    private Context baseContext;
    private AutoCompleteTextView autoPlace;
    private PlaceSearch placeSearch;
    private Parser parser;
    private SimpleAdapter adapter;
    public String destination = "";



    public GoogleMapAutoComplete(Context base_ctx, final AutoCompleteTextView auto) {

        baseContext = base_ctx;

        autoPlace = auto;
        autoPlace.setThreshold(1);
        autoPlace.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placeSearch = new PlaceSearch();
                placeSearch.execute(s.toString());

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });
        autoPlace.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, String> item = (HashMap<String, String>) adapter.getItem(position);
                destination = item.get("description");
                auto.setText(destination);
                adapter = null;
            }
        });
    }


    //download data from google map api url
    @SuppressLint("LongLogTag")
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        StringBuffer sb = new StringBuffer();
        InputStreamReader in;
        try{
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            in  = new InputStreamReader(urlConnection.getInputStream());
            char[] buff = new char[1024];//in case of too large data
            int read;
            while((read = in.read(buff)) != -1){
                sb.append(buff,0,read);
            }
            iStream = urlConnection.getInputStream();
            data = sb.toString();
        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());

        }finally{
            try{
                iStream.close();//avoid memory leak
                urlConnection.disconnect();
            }catch (IOException ex){
                Log.d("Exception while closing url", ex.toString());
            }

        }
        return data;
    }

    //get all locations from google places auto complete web
    private class PlaceSearch extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... place) {
            String data = null;
            //MY_GOOGLE_MAP_API_KEY
            String key = "key=AIzaSyCa1xsHVXbWp6LgvPj9dfemclGpIfzhOqo";
            String input="";
            try {
                //encode input text
                input = "input=" + URLEncoder.encode(place[0], "utf-8");
            } catch (UnsupportedEncodingException e1) {
                Log.d("Translate Exception", e1.toString());
            }

            // place type to be searched
            String types = "types=geocode";

            // Sensor enabled
            String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = input+"&"+types+"&"+sensor+"&"+key;

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;

            try{
                data = null;
                data = downloadUrl(url);
            }catch(Exception e){
                Log.d("Downloading Exception", e.toString());
            }
//            Log.d("Place Search Downloaded Data", data);
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            parser = new Parser();
            //parsing JSON format data
            parser.execute(result);
        }
    }

    //to parse JSON file download from google place web
    private class Parser extends AsyncTask<String, Integer, List<HashMap<String,String>>> {

        JSONObject jObject;
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;

            PlaceJSONParser placeJsonParser = new PlaceJSONParser();
            try {
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = placeJsonParser.parse(jObject);

            } catch (Exception e) {
                Log.d("Exception in parser", e.toString());
            }
//            Log.d("Parser Parsed data", places.toString());
            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
            String[] from = new String[]{"description"};
            int[] to = new int[]{android.R.id.text1};
            //for auto complete
            adapter = new SimpleAdapter(baseContext, result, android.R.layout.simple_list_item_1, from, to);
            autoPlace.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }

    }
}

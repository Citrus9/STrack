package pl.pwr.citrus.strack;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import pl.pwr.citrus.strack.db.DatabaseAdapter;

public class TraceActivity extends FragmentActivity implements OnMapReadyCallback, ConnectionCallbacks,
        OnConnectionFailedListener {

    private static int REQUEST_CODE_RECOVER_PLAY_SERVICES = 200;

    private GoogleMap mMap;
    private Button bSelect;
    private LatLng[] storeLatLng;
    private String[] storeNames;
    private double[][] storeDistances;
    private int[] indexes;
    private Polyline line;
    private ArrayList<Integer> result = new ArrayList<>();
    private ArrayList<LatLng> resultLatLng;

    private double distanceDouble;
    private boolean editMap = true;

    Location mLastLocation;
    LatLng mmLastLocation = new LatLng(51.110, 17.034);
    LatLng mEndLocation;
    private GoogleApiClient mGoogleApiClient;

    private DatabaseAdapter mDbHelper;
    private Cursor cursor;
    int sizeOfCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trace);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);


        buildGoogleApiClient();


        mDbHelper = DatabaseAdapter.getInstance(getBaseContext());
        mDbHelper.openConnection();

        //initializing the arrays
        cursor = mDbHelper.getAllStoreRecords();
        sizeOfCursor = cursor.getCount();
        storeLatLng = new LatLng[sizeOfCursor];
        storeNames = new String[sizeOfCursor];

        int i = 0;
        while (cursor.moveToNext()) {
            String[] arr = cursor.getString(cursor
                    .getColumnIndexOrThrow(DatabaseAdapter.STORE_LOCATION)).split(" ");
            LatLng latLng = new LatLng(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]));
            Log.i("LOG", latLng + " ind:" + i);
            //if (i != 0 || i != sizeOfCursor+1) {
            storeLatLng[i] = latLng;
            //Log.i("LOG2", latLng + " ind:" + i);
            storeNames[i] = cursor.getString(cursor
                    .getColumnIndexOrThrow(DatabaseAdapter.STORE_NAME));
            //}
            i++;
        }
        cursor.close();

        bSelect = (Button) findViewById(R.id.button);
        bSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                performAlgorithm();
                bSelect.setVisibility(View.GONE);
                editMap = false;
            }
        });


    }

    void performAlgorithm() {

        //initializing the arrays
        cursor = mDbHelper.getAllStoreRecords();
        sizeOfCursor = cursor.getCount();
        storeLatLng = new LatLng[sizeOfCursor + 2];
        storeNames = new String[sizeOfCursor];
        storeDistances = new double[sizeOfCursor + 2][sizeOfCursor + 2];
        indexes = new int[sizeOfCursor];
        for (int i = 0; i < sizeOfCursor; i++) {
            indexes[i] = i + 1;
        }

        int i = 0;


        storeLatLng[0] = mmLastLocation;
        //storeNames[0] = "Poczatek";

        storeLatLng[storeLatLng.length - 1] = mEndLocation;
        //storeNames[storeNames.length - 1] = "Koniec";

        while (cursor.moveToNext()) {
            String[] arr = cursor.getString(cursor
                    .getColumnIndexOrThrow(DatabaseAdapter.STORE_LOCATION)).split(" ");
            LatLng latLng = new LatLng(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]));
            Log.i("LOG", latLng + " ind:" + i);
            storeLatLng[i + 1] = latLng;
            storeNames[i] = cursor.getString(cursor
                    .getColumnIndexOrThrow(DatabaseAdapter.STORE_NAME));
            i++;
        }
        cursor.close();

        for (int k = 0; k < storeLatLng.length; k++) {
            for (int j = 0; j < storeLatLng.length; j++) {
                if (k != j) {
                    Location loc1 = new Location("");
                    loc1.setLatitude(storeLatLng[k].latitude);
                    loc1.setLongitude(storeLatLng[k].longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(storeLatLng[j].latitude);
                    loc2.setLongitude(storeLatLng[j].longitude);

                    distanceDouble = retrieveDistance(storeLatLng[k], storeLatLng[j]);
                } else
                    distanceDouble = 0;
                storeDistances[k][j] = distanceDouble;

            }
        }

        for (int l = 0; l < storeDistances.length; l++) {
            for (int k = 0; k < storeDistances.length; k++) {
                Log.i("LIST", "l: " + l + "k: " + k + " distance: " + storeDistances[l][k]);
            }
        }

        //algorithm
        double wholeDistance = 500000;
        double currentDistance;
        ArrayList<ArrayList<Integer>> permutated = permute(indexes);
        result = permutated.get(0);
        for (ArrayList<Integer> per : permutated) {
            currentDistance = 0;
            for (int k = 0; k <= per.size(); k++) {
                if (k == 0) {
                    Log.i("DISTANCE POINTS", "FROM " + k + " TO " + per.get(k));
                    currentDistance += storeDistances[k][per.get(k)];
                } else if (k == per.size()) {
                    Log.i("DISTANCE POINTS", "FROM " + per.get(k - 1) + " TO " + (k + 1));
                    currentDistance += storeDistances[per.get(k - 1)][k + 1];
                } else {
                    Log.i("DISTANCE POINTS", "FROM " + per.get(k - 1) + " TO " + per.get(k));
                    currentDistance += storeDistances[per.get(k - 1)][per.get(k)];
                }
            }
            Log.i("DISTANCE RESULT", currentDistance + " " + wholeDistance);
            if (currentDistance < wholeDistance) {
                wholeDistance = currentDistance;
                result = per;
            }
        }
        resultLatLng = new ArrayList<>();
        resultLatLng.add(mmLastLocation);
        resultLatLng.add(mEndLocation);
        for (Integer r : result) {
            Log.i("RESULT", "result " + r);
            resultLatLng.add(storeLatLng[r]);
        }

//        result.add(0, 0);
//        result.add(result.size());

        Log.i("MAP NOT READY", "size: " + resultLatLng.size());
        //justDistance = false;

        Log.i("MAP READY", "size: " + resultLatLng.size());
        if (resultLatLng.size() >= 2) {
            LatLng origin = resultLatLng.get(0);
            LatLng dest = resultLatLng.get(1);

            // Getting URL to the Google Directions API
            String url = getDirectionsUrl(origin, dest);

            DownloadTask downloadTask = new DownloadTask();

            // Start downloading json data from Google Directions API
            downloadTask.execute(url);
        }

//        resultLatLng.remove(1);
//        resultLatLng.add(mEndLocation);
        mMap.addMarker(new MarkerOptions().position(resultLatLng.get(0)).title("Poczatek"));
        mMap.addMarker(new MarkerOptions().position(resultLatLng.get(1)).title("Koniec"));
        for (int k = 2; k < resultLatLng.size(); k++) {
            Log.i("RESULT", "result " + k);
            mMap.addMarker(new MarkerOptions().position(resultLatLng.get(k)).title((k-1) + " "));
        }
    }

//    private boolean checkGooglePlayServices() {
//
//        int checkGooglePlayServices = GooglePlayServicesUtil
//                .isGooglePlayServicesAvailable(this);
//        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
//			/*
//			* google play services is missing or update is required
//			*  return code could be
//			* SUCCESS,
//			* SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
//			* SERVICE_DISABLED, SERVICE_INVALID.
//			*/
//            GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices,
//                    this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
//
//            return false;
//        }
//
//        return true;
//
//    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);

            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    2);
            return;
        }
        Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mmLastLocation = new LatLng(l.getLatitude(), l.getLongitude());
        if (l != null) {
//            mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,
//                    mLastLocation.getLatitude()));
//            mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,
//                    mLastLocation.getLongitude()));
        } else {
            Toast.makeText(this, "No location detected", Toast.LENGTH_LONG).show();
        }
    }


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }


            }

            case 2: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

//                    Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//                    mmLastLocation = new LatLng(l.getLatitude(), l.getLongitude());
//                    if (mLastLocation != null) {
//                        //            mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,
//                        //                    mLastLocation.getLatitude()));
//                        //            mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,
//                        //                    mLastLocation.getLongitude()));
//                    } else {
//                        Toast.makeText(this, "No location detected", Toast.LENGTH_LONG).show();
//                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }


            }

        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
//        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
//        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng wroclaw = new LatLng(51.110, 17.034);
        mMap.addMarker(new MarkerOptions().position(wroclaw));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wroclaw, 12));
        mMap.clear();
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);

            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    2);
            return;
        }
        mMap.setMyLocationEnabled(true);
        for (int k = 0; k < storeLatLng.length; k++) {
            Log.i("RESULT", "result " + k);
            mMap.addMarker(new MarkerOptions().position(storeLatLng[k]).title(storeNames[k]));
        }
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                if(editMap) {
                    bSelect.setEnabled(true);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("Koniec");
                    markerOptions.alpha(0.6f);
                    mMap.clear();
                    for (int k = 0; k < storeLatLng.length; k++) {
                        Log.i("RESULT", "result " + k);
                        mMap.addMarker(new MarkerOptions().position(storeLatLng[k]).title(storeNames[k]));
                    }
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.addMarker(markerOptions);
                    mEndLocation = latLng;
                }
            }
        });



    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Waypoints
        String waypoints = "";
        for(int i=2;i<resultLatLng.size();i++){
            LatLng point  = (LatLng) resultLatLng.get(i);
            if(i==2)
                waypoints = "waypoints=";
            waypoints += point.latitude + "," + point.longitude + "|";
        }

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+waypoints;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String>{

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service

            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(3);


                lineOptions.color(Color.RED);
            }
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
        }
    }

    public static int getRandom(int[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

        protected double retrieveDistance(LatLng start, LatLng end) {


            double distan = 0;

            HashMap<String, String> points = new HashMap<>();
            points.put("origin", start.latitude + "," + start.longitude);
            points.put("destination", end.latitude + "," + end.longitude);


            String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
            JSONParser jsonParser = new JSONParser();

            JSONObject obj = jsonParser.makeHttpRequest(DIRECTIONS_URL, "GET", points, true);

            if (obj == null) return Double.valueOf(0);

            try {

                JSONArray routeArray = obj.getJSONArray("routes");
                JSONObject routes = routeArray.getJSONObject(0);

                JSONArray legs = routes.getJSONArray("legs");

                JSONObject steps = legs.getJSONObject(0);

                JSONObject dist = steps.getJSONObject("distance");

                distan = Double.parseDouble(dist.getString("text").replaceAll("[^\\.0123456789]", ""));

                return distan;

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return Double.valueOf(0);
        }



//        protected void onPostExecute(double feed) {
//            // TODO: check this.exception
//            // TODO: do something with the feed
//        }
//    }



//    class GetDirectionsAsync extends AsyncTask<LatLng, Void, List<LatLng>> {
//
//        JSONParser jsonParser;
//        String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
//        double distan = 0;
//
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected List<LatLng> doInBackground(LatLng... params) {
//            LatLng start = params[0];
//            LatLng end = params[1];
//
//            HashMap<String, String> points = new HashMap<>();
//            points.put("origin", start.latitude + "," + start.longitude);
//            points.put("destination", end.latitude + "," + end.longitude);
//
//            jsonParser = new JSONParser();
//
//            JSONObject obj = jsonParser.makeHttpRequest(DIRECTIONS_URL, "GET", points, true);
//
//            if (obj == null) return null;
//
//            try {
//                List<LatLng> list = null;
//
//                JSONArray routeArray = obj.getJSONArray("routes");
//                JSONObject routes = routeArray.getJSONObject(0);
//                JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
//                String encodedString = overviewPolylines.getString("points");
//                JSONArray legs = routes.getJSONArray("legs");
//
//                JSONObject steps = legs.getJSONObject(0);
//
//                JSONObject dist = steps.getJSONObject("distance");
//
//                distan = Double.parseDouble(dist.getString("text").replaceAll("[^\\.0123456789]","") );
//
//                list = decodePoly(encodedString);
//
//                return list;
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(List<LatLng> pointsList) {
//
//            if (pointsList == null) return;
//
//            //if(!justDistance) {
//                if (line != null) {
//                    line.remove();
//                }
//
//                PolylineOptions options = new PolylineOptions().width(5).color(Color.MAGENTA).geodesic(true);
//                for (int i = 0; i < pointsList.size(); i++) {
//
//                    LatLng point = pointsList.get(i);
//                    options.add(point);
//                }
//                line = mMap.addPolyline(options);
//            //}
//            distanceDouble = distan;
//
//
//        }
//    }

    public ArrayList<ArrayList<Integer>> permute(int[] num) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        permute(num, 0, result);

        return result;
    }

    void permute(int[] num, int start, ArrayList<ArrayList<Integer>> result) {

        if (start >= num.length) {
            ArrayList<Integer> item = convertArrayToList(num);
            result.add(item);
        }

        for (int j = start; j <= num.length - 1; j++) {
            swap(num, start, j);
            permute(num, start + 1, result);
            swap(num, start, j);
        }
    }

    private ArrayList<Integer> convertArrayToList(int[] num) {
        ArrayList<Integer> item = new ArrayList<Integer>();
        for (int h = 0; h < num.length; h++) {
            item.add(num[h]);
        }
        return item;
    }

    private void swap(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }
}

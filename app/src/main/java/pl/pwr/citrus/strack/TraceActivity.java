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
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import pl.pwr.citrus.strack.db.DatabaseAdapter;

public class TraceActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static int REQUEST_CODE_RECOVER_PLAY_SERVICES = 200;

    private GoogleMap mMap;
    private LatLng [] storeLatLng;
    private String [] storeNames;
    private double [][] storeDistances;
    private int[] indexes;
    private Polyline line;
    private ArrayList<Integer> result;
    private ArrayList<LatLng> resultLatLng;

    private double distanceDouble;
    private boolean justDistance;

    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private DatabaseAdapter mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trace);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        mDbHelper = DatabaseAdapter.getInstance(getBaseContext());
        mDbHelper.openConnection();


        //initializing the arrays
        Cursor cursor = mDbHelper.getAllStoreRecords();
        int sizeOfCursor = cursor.getCount();
        storeLatLng = new LatLng[sizeOfCursor+2];
        storeNames = new String[sizeOfCursor+2];
        storeDistances = new double[sizeOfCursor+2][sizeOfCursor+2];
        indexes = new int[sizeOfCursor];
        for(int i = 0; i<sizeOfCursor; i++){
            indexes[i] = i+1;
        }
//        result = new LatLng[sizeOfCursor+2];
//        result[result.length-1] = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        justDistance = true;
        int i = 0;
        if (checkGooglePlayServices()) {
            buildGoogleApiClient();
        }
//        while(mLastLocation==null){
//
//        }

            storeLatLng[0] = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            storeNames[0] = "Poczatek";

            storeLatLng[storeLatLng.length - 1] = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            storeNames[storeNames.length - 1] = "Koniec";

            while (cursor.moveToNext()) {
                String[] arr = cursor.getString(cursor
                        .getColumnIndexOrThrow(DatabaseAdapter.STORE_LOCATION)).split(" ");
                LatLng latLng = new LatLng(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]));
                if (i != 1 || i != sizeOfCursor - 1) {
                    storeLatLng[i] = latLng;

                    storeNames[i] = cursor.getString(cursor
                            .getColumnIndexOrThrow(DatabaseAdapter.STORE_NAME));
                }
                for (int j = 0; j < sizeOfCursor + 2; j++) {
                    if (i != j)
                        try {
                            new GetDirectionsAsync().execute(storeLatLng[i], storeLatLng[j]).get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    else
                        distanceDouble = 0;
                    storeDistances[i][j] = distanceDouble;
                }
                i++;
            }
            cursor.close();

            //algorithm
            double wholeDistance = 500;
            double currentDistance = 500;
            ArrayList<ArrayList<Integer>> permutated = permute(indexes);

            for (ArrayList<Integer> per : permutated) {
                currentDistance = 0;
                currentDistance += storeDistances[0][per.get(0)];
                for (int k = 0; k < per.size(); k++) {
//                if(k+1<per.size()){
//                    currentDistance += storeDistances[k][per.get(k+1)];
//                }
                    currentDistance += storeDistances[k][per.get(k + 1)];
                }

                if (currentDistance < wholeDistance) {
                    result = per;
                }
            }

            resultLatLng.add(storeLatLng[0]);
            for (Integer r : result) {
                resultLatLng.add(storeLatLng[r]);
            }
            resultLatLng.add(storeLatLng[storeLatLng.length - 1]);

            justDistance = false;
            for (int l = 0; l < resultLatLng.size() - 1; l++) {
                try {
                    new GetDirectionsAsync().execute(resultLatLng.get(l), resultLatLng.get(l + 1)).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            }

    }

    private boolean checkGooglePlayServices() {

        int checkGooglePlayServices = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
			/*
			* google play services is missing or update is required
			*  return code could be
			* SUCCESS,
			* SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
			* SERVICE_DISABLED, SERVICE_INVALID.
			*/
            GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices,
                    this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();

            return false;
        }

        return true;

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng wroclaw = new LatLng(51.110, 17.034);
        mMap.addMarker(new MarkerOptions().position(wroclaw));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wroclaw, 12));
        mMap.clear();

//        if (mMap != null) {
//
//
//            mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
//
//                @Override
//                public void onMyLocationChange(Location arg0) {
//                    // TODO Auto-generated method stub
//
//                    mMap.addMarker(new MarkerOptions().position(new LatLng(arg0.getLatitude(), arg0.getLongitude())).title("It's Me!"));
//                }
//            });
//
//        }
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
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
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

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000); // Update location every second

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
//        if (mLastLocation != null) {
//            lat = String.valueOf(mLastLocation.getLatitude());
//            lon = String.valueOf(mLastLocation.getLongitude());
//
//        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    class GetDirectionsAsync extends AsyncTask<LatLng, Void, List<LatLng>> {

        JSONParser jsonParser;
        String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
        double distan = 0;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<LatLng> doInBackground(LatLng... params) {
            LatLng start = params[0];
            LatLng end = params[1];

            HashMap<String, String> points = new HashMap<>();
            points.put("origin", start.latitude + "," + start.longitude);
            points.put("destination", end.latitude + "," + end.longitude);

            jsonParser = new JSONParser();

            JSONObject obj = jsonParser.makeHttpRequest(DIRECTIONS_URL, "GET", points, true);

            if (obj == null) return null;

            try {
                List<LatLng> list = null;

                JSONArray routeArray = obj.getJSONArray("routes");
                JSONObject routes = routeArray.getJSONObject(0);
                JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
                String encodedString = overviewPolylines.getString("points");
                JSONArray legs = routes.getJSONArray("legs");

                JSONObject steps = legs.getJSONObject(0);

                JSONObject dist = steps.getJSONObject("distance");

                distan = Double.parseDouble(dist.getString("text").replaceAll("[^\\.0123456789]","") );

                list = decodePoly(encodedString);

                return list;

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<LatLng> pointsList) {

            if (pointsList == null) return;

            if(!justDistance) {
                if (line != null) {
                    line.remove();
                }

                PolylineOptions options = new PolylineOptions().width(5).color(Color.MAGENTA).geodesic(true);
                for (int i = 0; i < pointsList.size(); i++) {

                    LatLng point = pointsList.get(i);
                    options.add(point);
                }
                line = mMap.addPolyline(options);
            }
            distanceDouble = distan;


        }
    }

    public ArrayList<ArrayList<Integer>> permute(int[] num) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        permute(num, 0, result);
//        for(ArrayList<Integer> r:result){
//            r.add(0,);
//        }
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

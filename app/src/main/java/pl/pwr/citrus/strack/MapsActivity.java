package pl.pwr.citrus.strack;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private Bundle mExtras;
    private String intentId = "";

    private GoogleMap mMap;
    private String location = "";
    private Button bSelect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mExtras = getIntent().getExtras();
        if(mExtras!=null)
            intentId = mExtras.getString("POS");
        bSelect = (Button) findViewById(R.id.button);
        if(intentId==""){
            bSelect.setEnabled(false);
        } else {
            location = intentId;
        }
        setupMarker();

        bSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("LOCATION", location);
                MapsActivity.this.setResult(1, intent);
                MapsActivity.this.finish();
            }
        });



    }

    private void setupMarker(){
        if(intentId!=""){
            String[] arr = intentId.split(" ");
            MarkerOptions markerOptions = new MarkerOptions();
            LatLng latLng = new LatLng(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]));
            markerOptions.position(latLng);
            markerOptions.title(latLng.latitude + " : " + latLng.longitude);
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.addMarker(markerOptions);
        }
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

        // Add a marker in Sydney and move the camera
        LatLng wroclaw = new LatLng(51.110, 17.034);
        mMap.addMarker(new MarkerOptions().position(wroclaw));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wroclaw, 12));
        mMap.clear();
        // Setting a click event handler for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {
                bSelect.setEnabled(true);
                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                // This will be displayed on taping the marker
                markerOptions.title("NOWY: " + latLng.latitude + " : " + latLng.longitude);

                markerOptions.alpha(0.6f);
                // Clears the previously touched position
                mMap.clear();

                // Animating to the touched position
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                // Placing a marker on the touched position
                mMap.addMarker(markerOptions);

                location = latLng.latitude + " " + latLng.longitude;

                setupMarker();
            }
        });
    }
}

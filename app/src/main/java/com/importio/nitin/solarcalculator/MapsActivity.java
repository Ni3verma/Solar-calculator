package com.importio.nitin.solarcalculator;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final int LOCATION_PERMISSION_REQ_CODE = 234;
    private static final String TAG = "Nitin";
    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap mMap;
    private GeoDataClient mGeoDataClient;
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private AutoCompleteTextView mSearchBar;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mSearchBar = findViewById(R.id.search_bar);
        mSearchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    locatePlace();
                    return true;
                }
                return false;
            }
        });

        mGeoDataClient = Places.getGeoDataClient(this);

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(
                this,
                mGeoDataClient,
                new LatLngBounds(new LatLng(10.362045, 77.426159), new LatLng(31.816808, 76.668343)),
                null);

        mSearchBar.setAdapter(mPlaceAutocompleteAdapter);

        if (isServicesOK()) {
            getLocationPermission();
        }
    }

    private void locatePlace() {
        Log.d(TAG, "locatePlace: locating place");
        String str = mSearchBar.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(str, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (list.size() > 0) {
            Address address = list.get(0);
            Log.d(TAG, "found location" + address.toString());
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, address.getAddressLine(0));
        }
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        Toast.makeText(this, "map is ready", Toast.LENGTH_SHORT).show();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d("Nitin", "found current location");
                            Location currLocation = (Location) task.getResult();

                            if (currLocation != null) {
                                moveCamera(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), DEFAULT_ZOOM, "My location");
                            }
                        } else {
                            Toast.makeText(MapsActivity.this, "location not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException ex) {
            Log.e("Nitin", "security exception:" + ex.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if (!title.equals("My location")) { //don't add marker for user current location
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);

            mMap.addMarker(options);
            hideSoftKeyboard();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mLocationPermissionGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
    }

    private boolean isServicesOK() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (available == ConnectionResult.SUCCESS) {
            //everything is fine
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //error but we can resolve it
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, available, 123);
            dialog.show();
        } else {
            Toast.makeText(this, "google services unavailable", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            initMap();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQ_CODE: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = false;
                    return;
                }
                mLocationPermissionGranted = true;
                initMap();
            }
        }
    }

    public void gpsIconClicked(View view) {
        Log.d(TAG, "gpsIconClicked: clicked gps icon");
        getDeviceLocation();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

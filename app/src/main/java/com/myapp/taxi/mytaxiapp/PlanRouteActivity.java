package com.myapp.taxi.mytaxiapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PlanRouteActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private String provider;
    private Location location;
    private Circle currentMarker;
    private Marker destMarker;
    protected Spinner spinner_current;
    protected Spinner spinner_dest;
    private Button btn_calcDistance;

    private boolean current_location = false;
    private final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private boolean selectStartOnMap = false;
    private boolean selectDestOnMap = false;


    /**
     * @dropdown options for picking start locations is initialised
     */
    private void initStartList() {
        List<String> startList = new ArrayList<String>();
        startList.add("Current location");
        startList.add("Other");
        startList.add("Select on map");
        startList.add("Select start location");
        final ArrayAdapter<String> startAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, startList);
        startAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_current.setAdapter(startAdapter);
        spinner_current.setSelection(3);
    }

    /**
     * @dropdown options for picking destination locations is initialised
     */

    private void initEndList() {
        List<String> endList = new ArrayList<String>();
        endList.add("Other");
        endList.add("Select on map");
        endList.add("Select destination");
        final ArrayAdapter<String> destAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, endList);
        destAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_dest.setAdapter(destAdapter);
        spinner_dest.setSelection(2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("plan route", "created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_route);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //Map fragment is inflated
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //inflate widgets
        spinner_current = (Spinner) findViewById(R.id.spinner_current);
        spinner_dest = (Spinner) findViewById(R.id.spinner_dest);
        btn_calcDistance = (Button) findViewById(R.id.btn_calcDistance);

        //distance is calculated & toasted if both markers are present, otherwise an error message is toasted

        btn_calcDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMarker != null && destMarker != null) {
                    Toast.makeText(PlanRouteActivity.this, "Distance is " + calcDistance(currentMarker, destMarker) + " meters", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(PlanRouteActivity.this, "Start or destination marker has not been added", Toast.LENGTH_LONG).show();
                }

            }
        });

        initStartList();
        initEndList();


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria locCriteria = new Criteria();
        locCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(locCriteria, false);


        //permissions are requested
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);


            return;
        }


    }

    /**
     * This method is called after activity is started
     *
     * @LocationManager location updates are requested every 0 seconds for a change of 0 meters in a UI thread
     */
    @Override
    protected void onResume() {
        Runnable r = new Runnable() {
            @Override
            public void run() {

                try {

                    locationManager.requestLocationUpdates(provider, 0, 0, PlanRouteActivity.this);
                } catch (SecurityException se) {
                    se.printStackTrace();
                }
            }
        };

        this.runOnUiThread(r);


        super.onResume();
    }


    /**
     * @param googleMap map to display
     *                  This method is called when the google map has loaded
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
        this.mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        /**
         * @spiner_current downdown spinner selection:
         *                      "current location", a marker is added to the map with the location coordinates of the current location
         *                      "Other", a marker is added to location prodied from first address line
         *                      "Select on map", a marker is added on the location the user clicks onto the map
         *
         */

        spinner_current.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.v("current", "spinner");
                switch (position) {
                    case 0:
                        current_location = true;
                        selectStartOnMap = false;
                        try {
                            if (provider != null && current_location) {
                                location = locationManager.getLastKnownLocation(provider);
                                if (location == null) {
                                    Log.v(" location", "null");
                                } else {
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    addCurrentMarker(latLng);
                                }
                            } else {
                                Log.v("provider", "null");
                            }


                        } catch (SecurityException se) {
                            se.printStackTrace();
                        }
                        break;


                    case 1:
                        current_location = false;
                        selectStartOnMap = false;
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PlanRouteActivity.this);
                        final EditText editText = new EditText(PlanRouteActivity.this);
                        alertDialogBuilder.setMessage("Enter start address").setCancelable(true).setView(editText).setPositiveButton("OK", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String addressLine = editText.getText().toString();
                                if (addressLine != null && !addressLine.isEmpty()) {
                                    LatLng latLng = getFromAddress(addressLine);
                                    addCurrentMarker(latLng);
                                }
                            }
                        });
                        AlertDialog alert = alertDialogBuilder.create();
                        alert.setTitle("Start location");
                        alert.show();

                        break;

                    case 2:
                        current_location = false;
                        selectStartOnMap = true;
                        mMap.setOnMapClickListener(new OnMapClickListener() {
                            @Override
                            public void onMapClick(final LatLng latLng) {
                                if (selectStartOnMap) {
                                    if (selectDestOnMap) {
                                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PlanRouteActivity.this);
                                        alertDialogBuilder.setCancelable(true);
                                        alertDialogBuilder.setPositiveButton("Select start location", new OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                addCurrentMarker(latLng);
                                            }
                                        });
                                        alertDialogBuilder.setNegativeButton("Select destination", new OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                addDestMarker(latLng);
                                            }
                                        });
                                        AlertDialog alert = alertDialogBuilder.create();
                                        alert.setTitle("Start or destination?");
                                        alert.show();
                                    } else {
                                        addCurrentMarker(latLng);
                                    }
                                }
                            }
                        });
                        break;

                    case 3:
                        current_location = false;
                        selectStartOnMap = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        /**
         * @spiner_dest downdown spinner selection:
         *                      "Other", a marker is added to location prodied from first address line
         *                      "Select on map", a marker is added on the location the user clicks onto the map
         *
         */
        spinner_dest.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.v("destination", "spinner");
                switch (position) {
                    case 0:
                        selectDestOnMap = false;
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PlanRouteActivity.this);
                        final EditText editText = new EditText(PlanRouteActivity.this);
                        alertDialogBuilder.setMessage("Enter destination address").setCancelable(true).setView(editText).setPositiveButton("OK", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String addressLine = editText.getText().toString();
                                if (addressLine != null && !addressLine.isEmpty()) {
                                    LatLng latLng = getFromAddress(addressLine);
                                    addDestMarker(latLng);
                                }
                            }
                        });
                        AlertDialog alert = alertDialogBuilder.create();
                        alert.setTitle("Destination");
                        alert.show();

                        break;

                    case 1:
                        selectDestOnMap = true;
                        mMap.setOnMapClickListener(new OnMapClickListener() {
                            @Override
                            public void onMapClick(final LatLng latLng) {
                                if (selectDestOnMap) {
                                    if (selectStartOnMap) {
                                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PlanRouteActivity.this);
                                        alertDialogBuilder.setCancelable(true);
                                        alertDialogBuilder.setPositiveButton("Select start location", new OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                addCurrentMarker(latLng);
                                            }
                                        });
                                        alertDialogBuilder.setNegativeButton("Select destination", new OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                addDestMarker(latLng);
                                            }
                                        });
                                        AlertDialog alert = alertDialogBuilder.create();
                                        alert.setTitle("Start or destination?");
                                        alert.show();
                                    } else {
                                        addDestMarker(latLng);
                                    }
                                }
                            }
                        });
                        break;

                    case 3:
                        selectDestOnMap = false;
                        break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }

    /**
     * This method is called when this activity is stopped
     * <p>
     * The location updates stop being called
     */
    @Override
    protected void onStop() {
        super.onStop();

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    /**
     * @param addressLine first address line, for which to return the location coordinates
     * @return LatLng object
     * location coordinates for the location of the address line given
     */
    private LatLng getFromAddress(String addressLine) {
        if (addressLine != null && !addressLine.isEmpty()) {
            Geocoder geocoder = new Geocoder(getApplicationContext());
            try {
                List<android.location.Address> addressList = geocoder.getFromLocationName(addressLine, 1);
                android.location.Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                return latLng;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @param latLng location coorindates for which the map should be zoomed to with level 10
     *               location coordinates for which marker indicating start location should be added to
     */

    private void addCurrentMarker(LatLng latLng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        mMap.animateCamera(cameraUpdate);

        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = this.mMap.addCircle(new CircleOptions().center(latLng).radius(100).fillColor(Color.BLUE).strokeColor(Color.BLUE));
        Toast.makeText(this, "Marker for start location has been added", Toast.LENGTH_LONG).show();
    }

    /**
     * @param latLng location coorindates for which the map should be zoomed to with level 10
     *               location coordinates for which marker indicating destination location should be added to
     */
    private void addDestMarker(LatLng latLng) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        mMap.animateCamera(cameraUpdate);

        if (destMarker != null) {
            destMarker.remove();
        }
        destMarker = this.mMap.addMarker(new MarkerOptions().position(latLng));

    }

    /**
     * @param startMarker marker to indicate start location
     * @param endMarker   marker to indicate destination location
     * @return distance between start & destination location to 2 decimal places
     */
    private double calcDistance(Circle startMarker, Marker endMarker) {
        LatLng startPostion = startMarker.getCenter();
        LatLng endPosition = endMarker.getPosition();

        Location startLocation = new Location("");
        startLocation.setLongitude(startPostion.longitude);
        startLocation.setLatitude(endPosition.latitude);

        Location endLocation = new Location("");
        endLocation.setLongitude(endPosition.longitude);
        endLocation.setLatitude(endPosition.latitude);

        double distance = startLocation.distanceTo(endLocation);
        return roundDecimal(distance);

    }

    /**
     * @param number number to round to 2 decimal places
     * @return nuber to 2 decimal places
     */
    private double roundDecimal(double number) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        return Double.parseDouble(decimalFormat.format(number));
    }

    /**
     * @param location location for which marker should be added if
     *                 "current location" is selected for the options for the selection of the start location
     */

    @Override
    public void onLocationChanged(Location location) {
        if (current_location) {

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            addCurrentMarker(latLng);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


}



package com.myapp.taxi.mytaxiapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.InvalidDisplayException;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CalculateTariffActivity extends AppCompatActivity {



    /**
     * btn_start - Button
     * starts the location services & the timer
     * <p>
     * btn_stop - Button
     * stops the location services & the timer
     * <p>
     * btn_calculateTariff - Button
     * takes to page for displaying journey fare
     * <p>
     * tv-timer - TextView
     * total time taken is displayed here
     */
    private Button btn_start;
    private Button btn_stop;
    private Button btn_calculateTariff;
    private TextView tv_timer;

    /**
     * distance - double value
     * keeps track of total distance travelled throughout journey
     * <p>
     * fare - double value
     * keeps track of total journey fare
     * <p>
     * longJourneyFare - double value
     * keeps track of fare for long journeys
     * <p>
     * longJourneyDistance -double value
     * keeps track of distance to verify whether journey is considered a long journey &
     * keeps track of distance for long journeys in order to calculate fare
     * <p>
     * longJourney - boolean value
     * whether the journey is a long journey or not
     */
    private double distance = 0;
    private double fare = 0;
    private double longJourneyFare = 0;
    private boolean longJourney = false;

    /**
     * locationReceiver- BroadcastReceiver object
     * receives location updates in broadcasts from location services
     * <p>
     * locations - Location object ArrayList
     * keeps track of all location coordinates visited in journey
     * <p>
     * locationFilter - IntentFilter object
     * ensures all broadcasts only arrive from the LocationTracker class
     */
    private BroadcastReceiver locationReceiver;
    private BroadcastReceiver timeReceiver;


    private int Seconds, Minutes, MilliSeconds;

    private ArrayList<Location> locations=new ArrayList<>();


    private IntentFilter locationFilter = new IntentFilter("location");
    private IntentFilter timeFilter = new IntentFilter("time");

    private double longJourneydist1, longJourneydist2, longJourneydist3 = 0;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_tariff);
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

        //inflate widgets
        btn_start = (Button) findViewById(R.id.btn_startDrive);
        btn_stop = (Button) findViewById(R.id.btn_stopDrive);
        btn_calculateTariff = (Button) findViewById(R.id.btn_calculate);
        tv_timer = (TextView) findViewById(R.id.tv_timer);



        enable_buttons();

        if(LocationTracker.isServiceRunning()){
            locations=LocationTracker.getLocations();
        }


    }
    /**
     * @return total journey fare is the journey is long
     * @throws InvalidDisplayException
     * @throws if                      journey is not long
     */
    private double getLongJourneyFare() {
        if (longJourney) {
            double rest = distance - 6;
            double total = convertFromMetersToMiles(longJourneydist3) * 3.96 + convertFromMetersToMiles(longJourneydist2) * 3.37 + convertFromMetersToMiles(longJourneydist1)* 2.74+convertFromMetersToMiles(rest) * 3.7;
            return total;
        }
        throw new InvalidDisplayException("not long journey");

    }

    private double convertFromMetersToMiles(double distance){
        return distance*0.000621371192;
    }


    /**
     * Buttons are enabled.
     *
     * @Button btn_start - service is started & main timer is set
     * @Button btn_stop - service stops & main timer is stopped and reset
     * @Button btn_calculateTariff - fare is converted to UK Currency format
     * - distance is converted to 3 significant figures
     * - info is launched in new window
     */
    private void enable_buttons() {
        btn_start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!LocationTracker.isServiceRunning()) {
                    locations = new ArrayList<Location>();
                    distance = 0;
                    fare = 0;
                    longJourney = false;




                    Intent i = new Intent(CalculateTariffActivity.this, LocationTracker.class);
                    startService(i);
                }else{
                    Toast.makeText(CalculateTariffActivity.this,"Monitoring is already occuring",Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(LocationTracker.isServiceRunning()) {

                    tv_timer.setText("00: 00: 00");




                    Intent i = new Intent(CalculateTariffActivity.this, LocationTracker.class);
                    stopService(i);

                }else{
                    Toast.makeText(CalculateTariffActivity.this,"Monitoring is not occuring",Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_calculateTariff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    longJourneyFare = getLongJourneyFare();
                    fare = longJourneyFare;
                } catch (InvalidDisplayException e) {

                } finally {
                    Locale uk = Locale.UK;
                    String fareformat = getFareFormat();
                    double distanceformat = getDistFormat();
                    launchInfo(fareformat, distanceformat, Minutes, Seconds, MilliSeconds);
                }
            }
        });
    }

    private String getFareFormat(){
        Locale uk = Locale.UK;
        return NumberFormat.getCurrencyInstance(uk).format(fare);
    }

    private double getDistFormat(){
        return new BigDecimal(distance).round(new MathContext(3)).doubleValue();
    }

    /**
     * This method is called after the activity is created.
     *
     * @BroadCastReceiver info received from locationreceiver, location coordinates added to locations arraylist
     * locationreceiver is registered
     * @distance distance between current & previous location is added to current value of distance
     * @longJourneyDistance - distance is calculated then converted from meters to miles to keep track fo whether journey is long
     * -if distance if miles>6, journey is long journey
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationReceiver == null) {
            locationReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(intent.hasExtra("locations")){
                        locations= (ArrayList<Location>) intent.getSerializableExtra("locations");
                    }

                    if(intent.hasExtra("distance")){
                        distance=intent.getDoubleExtra("distance",0);
                    }
                    if(intent.hasExtra("longJourneyDist1")){
                        longJourneydist1=intent.getDoubleExtra("longJourneyDist1",0);
                    }
                    if(intent.hasExtra(("longJourneyDist2"))){
                        longJourneydist2=intent.getDoubleExtra("longJourneyDist2",0);
                    }
                    if(intent.hasExtra("longJourneyDist2")){
                        longJourneydist3=intent.getDoubleExtra("longJourneyDist3",0);
                    }
                    if(intent.hasExtra("longJourney")){
                        longJourney=intent.getBooleanExtra("longJourney",false);
                    }

               }
           };
            registerReceiver(locationReceiver, locationFilter);
        }

        if(timeReceiver==null){
            timeReceiver=new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.hasExtra("fare")) {
                        fare = intent.getDoubleExtra("fare", 0);
                    }
                    if (intent.hasExtra("Minutes") && intent.hasExtra("Seconds") && intent.hasExtra("Milliseconds")) {
                        Minutes = intent.getIntExtra("Minutes", 0);
                        Seconds = intent.getIntExtra("Seconds", 0);
                        MilliSeconds = intent.getIntExtra("Milliseconds", 0);
                        tv_timer.setText(Minutes+ ":"+Seconds+":"+MilliSeconds);
                    }

                }
            };
            registerReceiver(timeReceiver, timeFilter);
        }

    }

    /**
     * This method is called when system is low on resources
     *
     * @BroadcastReceiver locationreceiver is unregsitered so location updates are no longer received
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationReceiver != null) {
            unregisterReceiver(locationReceiver);
        }
    }

    /**
     * @param fare         fare of journey
     * @param distance     total distance travelled
     * @param minutes      minutes journey took
     * @param seconds      seconds journey took
     * @param milliSeconds milliseconds journey took
     * @launch CalculateTariffInfoActivity
     * displays fare, distance, time taken
     */
    private void launchInfo(String fare, double distance, int minutes, int seconds, int milliSeconds) {
        Intent i = new Intent(CalculateTariffActivity.this, CalculateTariffInfoActivity.class);
        i.putExtra("fare", fare);
        i.putExtra("distance", distance);
        i.putExtra("minutes", minutes);
        i.putExtra("seconds", seconds);
        i.putExtra("milliseconds", milliSeconds);
        startActivity(i);
    }

    /**
     * @return false
     */
    private boolean runtime_permissions() {

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

            return true;
        }
        return false;
    }

    /**
     * Permissions are handled in this method
     * If permissions are granted, buttons are enabled
     * Otherwise permission to location services is denied & permission is again requested
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enable_buttons();
            } else {
                runtime_permissions();
            }
        }
    }





}





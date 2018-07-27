package com.myapp.taxi.mytaxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class CalculateTariffInfoActivity extends AppCompatActivity {

    /**
     * tv_fare - TextView
     *              to display fare for journey
     */
    protected TextView tv_fare;

    /**
     * tv_time  TextView
     *              to display total time taken for journey
     */
    protected TextView tv_time;

    /**
     * tv_distance textView
     *              to display total distance travelled during journey
     */
    protected TextView tv_distance;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_tariff_info);
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
        tv_fare=(TextView)findViewById(R.id.tv_fare);
        tv_time=(TextView)findViewById(R.id.tv_time);
        tv_distance=(TextView)findViewById(R.id.tv_distance);

        fetchData();




    }

    /**
     * data from CalculateTariffActivity is collected and displayed
     * If the info can't be fetched, error messages will be displayed
     */
    private void fetchData(){
        Intent intent=getIntent();
        if(intent.hasExtra("fare")&&intent.hasExtra("minutes")&&intent.hasExtra("seconds")&&intent.hasExtra("milliseconds")&&intent.hasExtra("distance")){
            String fare=intent.getStringExtra("fare");
            int minutes=intent.getIntExtra("minutes",-1);
            int seconds=intent.getIntExtra("seconds",-1);
            int milliseconds=intent.getIntExtra("milliseconds",-1);
            double distance=intent.getDoubleExtra("distance",-1);

            tv_fare.setText("Fare : "+fare);
            StringBuffer sbTime=new StringBuffer("Time taken : ");
            sbTime.append(minutes);
            sbTime.append(":");
            sbTime.append(seconds);
            sbTime.append(":");
            sbTime.append(milliseconds);

            tv_time.setText(sbTime.toString());

            tv_distance.setText("Total Distance travelled: "+distance+" meters");
        } else{
            tv_fare.setText("Fare cannot be calculated");
            tv_time.setText("Time taken cannot be calculated");
            tv_distance.setText("Distance travelled cannot be calculated");
        }
    }





}

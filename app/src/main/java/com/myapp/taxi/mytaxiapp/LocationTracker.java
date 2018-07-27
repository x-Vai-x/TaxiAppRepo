package com.myapp.taxi.mytaxiapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class LocationTracker extends Service implements LocationListener {

    private LocationManager locationManager;
    private String provider;
    private static boolean serviceRunning = false;
    private static ArrayList<Location> locations;
    private static double distance = 0;
    private static double fare = 0;
    private static boolean longJourney = false;


    /**
     * MillisecondTime- long value
     * time in milliseconds elapsed since running service
     * <p>
     * StartTime - long value
     * time service started
     * <p>
     * Seconds - int value
     * number of seconds service has run for
     * <p>
     * Minutes - int value
     * number of minutes service has run for
     * <p>
     * Milliseconds - int value
     * number of milliseconds service has run for
     */
    private long MillisecondTime, StartTime = 0L;
    private static int Seconds, Minutes, MilliSeconds;


    /**
     * secondsTakenTariff3 - double value
     * number of seconds tariff 3 has run for
     * <p>
     * tariff3Distance - double value
     * keeps track of distance travelled during tariff 3 based on parameters so fares can be calculated
     * <p>
     * permTariff3Distance - double value
     * keeps track of total distance travelled for the duration of tariff 3
     * <p>
     * isTariff3Running - boolean value
     * keeps track of whether or not tariff 3 is active
     * <p>
     * isTariff3Extra - boolean value
     * keeps track of whether 162.4meters or 35 seconds has already been covered
     * <p>
     * t3start - long value
     * start time of tariff 3 run
     */
    private double secondsTakenTariff3 = 0;
    private double tariff3Distance = 0;
    private double permTariff3Distance = 0;
    private boolean isTariff3Running = false;
    private boolean isTariff3Extra = false;
    private double longJourneydist3 = 0;
    private long t3start;

    /**
     * secondsTakenTariff2 - double value
     * number of seconds tariff 2 has run for
     * <p>
     * tariff2Distance - double value
     * keeps track of distance travelled during tariff 2 based on parameters so fares can be calculated
     * <p>
     * permTariff2Distance - double value
     * keeps track of total distance travelled for the duration of tariff 2
     * <p>
     * isTariff2Running - boolean value
     * keeps track of whether or not tariff 2 is active
     * <p>
     * isTariff2Extra - boolean value
     * keeps track of whether 191meters or 41 seconds has already been covered
     * <p>
     * t2start - long value
     * start time of tariff 2 run
     */

    private double secondsTakenTariff2 = 0;
    private double tariff2Distance = 0;
    private double permTariff2Distance = 0;
    private boolean isTariff2Running = false;
    private boolean isTariff2Extra = false;
    private double longJourneydist2 = 0;
    private long t2start;


    /**
     * secondsTakenTariff1 - double value
     * number of seconds tariff 1 has run for
     * <p>
     * tariff1Distance - double value
     * keeps track of distance travelled during tariff 1 based on parameters so fares can be calculated
     * <p>
     * permTariff1Distance - double value
     * keeps track of total distance travelled for the duration of tariff 1
     * <p>
     * isTariff1Running - boolean value
     * keeps track of whether or not tariff 1 is active
     * <p>
     * isTariff1Extra - boolean value
     * keeps track of whether 234.5 meters or 50.4 seconds has already been covered
     * <p>
     * t1start - long value
     * start time of tariff 1 run
     */
    private double secondsTakenTariff1 = 0;
    private double tariff1Distance = 0;
    private double permTariff1Distance = 0;
    private boolean isTariff1Running = false;
    private boolean isTariff1Extra = false;
    private double longJourneydist1 = 0;
    private long t1start;

    /**
     * handler - Handler object
     * manages threads
     */
    private Handler handler;


    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    /**
     * @receive location upon service start if location is not null
     * @receive location updates for every 0 meters travelled and every 0 milliseconds when the location changes
     */
    @Override
    public void onCreate() {
        super.onCreate();

        serviceRunning = true;
        locations = new ArrayList<>();
        handler = new Handler();

        startTimer();

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        Criteria locCriteria = new Criteria();
        locCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(locCriteria, false);


        try {
            if (provider != null) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null) {
                    Log.v("location"
                            , "null");

                } else {

                    broadcastLocation(location);
                }
            } else {
                Log.v("provider", "null");
            }


        } catch (SecurityException se) {
            se.printStackTrace();
        }


        try {
            locationManager.requestLocationUpdates(provider, 0, 0, LocationTracker.this);
        } catch (SecurityException se) {
            se.printStackTrace();
        }


    }

    /**
     * @return DayOfWeek object
     * day of the week today?
     */

    private DayOfWeek getDay(Calendar cal) {
        DayOfWeek day = null;

        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        switch (dayOfWeek) {
            case Calendar.MONDAY:
                day = DayOfWeek.MONDAY;
                break;

            case Calendar.TUESDAY:
                day = DayOfWeek.TUESDAY;
                break;

            case Calendar.WEDNESDAY:
                day = DayOfWeek.WEDNESDAY;
                break;

            case Calendar.THURSDAY:
                day = DayOfWeek.THURSDAY;
                break;

            case Calendar.FRIDAY:
                day = DayOfWeek.FRIDAY;
                break;

            case Calendar.SATURDAY:
                day = DayOfWeek.SATURDAY;
                break;

            case Calendar.SUNDAY:
                day = DayOfWeek.SUNDAY;
                break;
        }
        if (day == null) {
            throw new NullPointerException("Day is null");
        }
        return day;
    }

    //in thread so that the application can continue to run the service and timer at the same time.
    public Runnable runnableMainTimer = new Runnable() {

        public void run() {

            //SystemClock.uptimeMillis() = time since app has been running in milliseconds. Since the startTime is the time of app boot when the service was started, the time that elapsed since running the service is represented by MilliSecondTime
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            //milliseconds converted to seconds - this is the total number of seconds
            Seconds = (int) (MillisecondTime / 1000);

            //seconds converted to minutes - this is the total number of minutes
            Minutes = Seconds / 60;

            //this is the remainder of the seconds so the timer can be displayed with minutes & seconds, not just seconds
            Seconds = Seconds % 60;

            //remainder of milliseconds  so the timer can be displayed with minutes, seconds & milliseconds, not just milliseconds
            MilliSeconds = (int) (MillisecondTime % 1000);

            broadcastTime();


            Date date = new Date();

            Calendar cal = Calendar.getInstance();
            DayOfWeek day = getDay(cal);

            int hour = cal.get(Calendar.HOUR_OF_DAY);


            if (!isBusinessDay(date, day) || hour >= 22 || hour <= 5) {
                handleTariff3(date, day, hour);
            } else if ((isDayWeekend(day) && hour >= 5 && hour <= 22) || (!isDayWeekend(day) && hour >= 20 && hour <= 22)) {
                handleTariff2(date, day, hour);


            } else if (!isDayWeekend(day) && hour >= 5 && hour <= 20) {
                handleTariff1(date, day, hour);
            }


            //so the timer is constantly updated
            handler.post(this);
        }

    };

    private void broadcastTime() {
        Intent timeIntent = new Intent("time");
        timeIntent.putExtra("fare", fare);
        timeIntent.putExtra("Minutes", Minutes);
        timeIntent.putExtra("Seconds", Seconds);
        timeIntent.putExtra("Milliseconds", MilliSeconds);
        sendBroadcast(timeIntent);

    }

    private void stopTimer() {
        handler.removeCallbacks(runnableMainTimer);

    }

    private void resetTimer() {
        MillisecondTime = 0L;
        StartTime = SystemClock.uptimeMillis();
        Minutes = 0;
        Seconds = 0;
        MilliSeconds = 0;
    }

    private void startTimer() {
        resetTimer();
        handler.post(runnableMainTimer);
    }


    /**
     * Tariff 3 applies between 22:00pm and 5:00 am the following day
     * restarted if not running
     *
     * @longjourneydist3 keeps track of the distance to be used in the calculation for the long distance fare calculation,
     * in case the journey is long
     * <p>
     * is updated until the long distance calculation reaches 6 miles
     * <p>
     * @tariff3extra keeps track of whether the initial 162.meters travelled or 35.6 seconds has been reached
     * @permtariff3distance keeps track of total time taken for the journey during the tariff 3 activation
     * <p>
     * This is used to verify whether the total distance for this tariff is 9656.1 or above
     * @tariff3distance keeps track of distance travelled and time taken until initial 162.4 meters travelled or 35.6 seconds time elapsed
     * <p>
     * Then, once this is reached, calculation can be performed to check whether the total tariff 3 distance is <9656.1,
     * in which case for every distance offor every 81.2 meters travelled or 17.5 seconds elapsed during the journey,
     * 20p is charged.
     * <p>
     * Once a total distance of 9656.1 meters has been travelled ,
     * there is a charge of 20p for 20p for every additional 86.9 meters travelled or 18.7 seconds elapsed during the journey
     */
    private void handleTariff3(Date date, DayOfWeek day, int hour) {
        if (!isBusinessDay(date, day) || hour >= 22 || hour <= 5) {
            if (!isTariff3Running) {
                restartTariff3();
            }
            if (locations.size() > 1) {
                if (!longJourney) {
                    longJourneydist3 += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));


                    if (convertFromMetersToMiles(longJourneydist3 + longJourneydist2 + longJourneydist1) > 6) {

                        longJourney = true;

                    }

                    if (!longJourney) {

                        tariff3Distance += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));
                        permTariff3Distance += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));

                        if (isTariff3Extra) {

                            if (permTariff3Distance < 9656.1) {
                                if ((tariff3Distance >= 81.2 || secondsTakenTariff3 >= 17.5)) {

                                    fare += 0.2;

                                    if (tariff3Distance >= 81.2 && secondsTakenTariff3 >= 17.5) {
                                        tariff3Distance -= 81.2;
                                        secondsTakenTariff3 -= 17.5;
                                    } else if (tariff3Distance >= 81.2) {
                                        tariff3Distance -= 81.2;
                                        secondsTakenTariff3 = 0;
                                    } else if (secondsTakenTariff3 >= 17.5) {
                                        secondsTakenTariff3 -= 17.5;
                                        tariff3Distance = 0;
                                    }

                                }
                            } else if (tariff3Distance >= 86.9 || secondsTakenTariff3 >= 18.7) {

                                fare += 0.2;
                                if (tariff3Distance >= 86.9 && secondsTakenTariff3 >= 18.7) {
                                    tariff3Distance -= 86.9;
                                    secondsTakenTariff3 -= 18.7;
                                } else if (tariff3Distance >= 86.9) {
                                    tariff3Distance -= 86.9;
                                    secondsTakenTariff3 = 0;
                                } else if (secondsTakenTariff3 >= 18.7) {
                                    secondsTakenTariff3 -= 18.7;
                                    tariff3Distance = 0;
                                }


                            }


                        } else if (tariff3Distance >= 162.4 || secondsTakenTariff3 >= 35) {
                            fare += 2.6;
                            if (tariff3Distance >= 162.4 && secondsTakenTariff3 >= 35) {
                                tariff3Distance -= 162.4;
                                secondsTakenTariff3 -= 35;
                            } else if (tariff3Distance >= 162.4) {
                                tariff3Distance -= 162.4;
                                secondsTakenTariff3 = 0;
                            } else if (secondsTakenTariff3 >= 35) {
                                secondsTakenTariff3 -= 35;
                                tariff3Distance = 0;
                            }

                            isTariff3Extra = true;

                        }
                    }

                }


            }


        }

    }

    private double convertFromMetersToMiles(double distance) {
        return distance * 0.000621371192;
    }

    /**
     * Tariff 2 applies between 5:00am and 22:00pm on a week day and on weekdays between 20:00pm and 2:00pm
     * restarted if not running
     *
     * @longjourneydist2 keeps track of the distance to be used in the calculation for the long distance fare calculation,
     * in case the journey is long
     * <p>
     * is updated until the long distance calculation reaches 6 miles
     * <p>
     * @tariff2extra keeps track of whether the initial 191 meters travelled or 41 seconds has been reached
     * @permtariff2distance keeps track of total time taken for the journey during the tariff 2 activation
     * <p>
     * This is used to verify whether the total distance for this tariff is 9656.1 or above
     * @tariff3distance keeps track of distance travelled and time taken until initial 162.4 meters travelled or 35.6 seconds time elapsed
     * <p>
     * Then, once this is reached, calculation can be performed to check whether the total tariff 2 distance is <9656.1,
     * in which case for every distance offor every 95.5 meters travelled or 20.5 seconds elapsed during the journey,
     * 20p is charged.
     * <p>
     * Once a total distance of 9656.1 meters has been travelled ,
     * there is a charge of 20p for every additional 86.9 meters travelled or 18.7 seconds elapsed during the journey
     */
    private void handleTariff2(Date date, DayOfWeek day, int hour) {
        if ((isDayWeekend(day) && hour >= 5 && hour <= 22) || (!isDayWeekend(day) && hour >= 20 && hour <= 22)) {
            stopTariff3();
            stopTariff1();
            if (!isTariff2Running) {
                restartTariff2();
            }

            if (locations.size() > 1) {
                if (!longJourney) {
                    longJourneydist2 += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));

                    if (convertFromMetersToMiles(longJourneydist3 + longJourneydist2 + longJourneydist1) > 6) {

                        longJourney = true;

                    }


                    if (!longJourney) {
                        if (isTariff2Extra) {
                            if (permTariff2Distance < 9656.1) {
                                if (tariff2Distance >= 95.5 || secondsTakenTariff2 >= 20.5) {
                                    fare += 20;
                                    if (tariff2Distance >= 95.5 && secondsTakenTariff2 >= 20.5) {
                                        secondsTakenTariff2 -= 20.5;
                                        tariff2Distance -= 95.5;
                                    } else if (tariff2Distance >= 95.5) {
                                        tariff2Distance -= 95.5;
                                        secondsTakenTariff2 = 0;
                                    } else if (secondsTakenTariff2 >= 20.5) {
                                        secondsTakenTariff2 -= 20.5;
                                        tariff2Distance = 0;
                                    }

                                }
                            } else {
                                if (tariff2Distance >= 86.9 || secondsTakenTariff2 >= 18.7) {
                                    fare += 20;
                                    if (tariff2Distance >= 96.9 && secondsTakenTariff2 >= 18.7) {
                                        tariff2Distance -= 86.9;
                                        secondsTakenTariff2 -= 18.7;
                                    } else if (tariff2Distance >= 96.9) {
                                        tariff2Distance -= 96.9;
                                        secondsTakenTariff2 = 0;
                                    } else if (secondsTakenTariff2 >= 18.7) {
                                        secondsTakenTariff2 -= 18.7;
                                        tariff2Distance = 0;
                                    }

                                }

                            }

                        } else if (tariff2Distance >= 191 || secondsTakenTariff2 >= 41) {
                            fare += 2.6;
                            if (tariff2Distance >= 191 && secondsTakenTariff2 >= 41) {
                                tariff2Distance -= 191;
                                secondsTakenTariff2 -= 41;
                            } else if (tariff2Distance >= 191) {
                                tariff2Distance -= 191;
                                secondsTakenTariff2 = 0;
                            } else if (secondsTakenTariff2 >= 41) {
                                secondsTakenTariff2 -= 41;
                                tariff2Distance = 0;
                            }
                            isTariff2Extra = true;
                        }
                    }

                    tariff2Distance += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));
                    permTariff2Distance += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));


                }


            }
        }
    }

    /**
     * Tariff 1 applies between 5:00am and 20:00pm on weekdays
     * restarted if not running
     *
     * @longjourneydist1 keeps track of the distance to be used in the calculation for the long distance fare calculation,
     * in case the journey is long
     * <p>
     * is updated until the long distance calculation reaches 6 miles
     * <p>
     * @tariff1extra keeps track of whether the initial 234.8 meters travelled or 18.7 seconds has been reached
     * @permtariff1distance keeps track of total distance travelled for the journey during the tariff 1 activation
     * <p>
     * This is used to verify whether the total distance for this tariff is 9656.1 or above
     * @tariff1distance keeps track of distance travelled and time taken until initial 234.8 meters travelled or 18.7 seconds time elapsed
     * <p>
     * Then, once this is reached, calculation can be performed to check whether the total tariff 1 distance is <9656.1,
     * in which case for every distance offor every 117.4 meters travelled or 25.2 seconds elapsed during the journey,
     * 20p is charged.
     * <p>
     * Once a total distance of 9656.1 meters has been travelled ,
     * there is a charge of 20p for every additional 86.9 meters travelled or 18.7 seconds elapsed during the journey
     */
    private void handleTariff1(Date date, DayOfWeek day, int hour) {
        if (!isDayWeekend(day) && hour >= 5 && hour <= 20) {
            stopTariff3();
            stopTariff2();

            if (!isTariff1Running) {
                restartTariff1();
            }

            if (locations.size() > 1) {
                if (!longJourney) {
                    longJourneydist1 += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));

                    if (convertFromMetersToMiles(longJourneydist3 + longJourneydist2 + longJourneydist1) > 6) {

                        longJourney = true;

                    }

                    if (!longJourney) {
                        tariff1Distance += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));
                        permTariff1Distance += locations.get(locations.size() - 1).distanceTo(locations.get(locations.size() - 2));

                        if (isTariff1Extra) {
                            if (permTariff1Distance < 9656.1) {
                                if (tariff1Distance >= 117.4 || secondsTakenTariff1 >= 25.2) {
                                    fare += 0.2;
                                    if (tariff1Distance >= 117.4 && secondsTakenTariff1 >= 25.2) {
                                        tariff1Distance -= 117.4;
                                        secondsTakenTariff1 -= 25.2;
                                    } else if (tariff1Distance >= 117.4) {
                                        tariff1Distance -= 117.4;
                                        secondsTakenTariff1 = 0;
                                    } else {
                                        secondsTakenTariff1 -= 25.5;
                                        tariff1Distance = 0;
                                    }

                                }
                            } else {
                                if (tariff1Distance >= 86.9 || secondsTakenTariff1 >= 18.7) {
                                    fare += 0.2;
                                    if (tariff1Distance >= 86.9) {
                                        tariff1Distance -= 86.9;
                                        secondsTakenTariff1 = 0;
                                    } else if (tariff1Distance >= 86.9) {
                                        tariff1Distance -= 86.9;
                                        secondsTakenTariff1 = 0;
                                    } else if (secondsTakenTariff1 >= 18.7) {
                                        secondsTakenTariff1 -= 18.7;
                                        tariff1Distance = 0;
                                    }

                                }
                            }
                        } else if (tariff1Distance >= 234.8 || secondsTakenTariff1 >= 18.7) {
                            fare += 0.2;
                            if (tariff1Distance >= 234.8 && secondsTakenTariff1 >= 18.7) {
                                tariff1Distance -= 234.8;
                                secondsTakenTariff1 -= 18.7;
                            } else if (tariff1Distance >= 234.8) {
                                tariff1Distance -= 234.8;
                                secondsTakenTariff1 = 0;
                            } else if (secondsTakenTariff1 >= 18.7) {
                                secondsTakenTariff1 -= 18.7;
                                tariff1Distance = 0;
                            }
                            isTariff1Extra = true;
                        }
                    }


                }


            }


        }
    }


    /**
     * @Runnable object
     * used for timing tariff 3 in seconds
     */
    public Runnable runnableTariff3 = new Runnable() {

        public void run() {
            long t3end = System.currentTimeMillis();
            long t3Delta = t3end - t3start;
            secondsTakenTariff3 = t3Delta / 1000;

            handler.post(this);
        }

    };

    /**
     * @Runnable object
     * used for timing tariff 2 in seconds
     */
    public Runnable runnableTariff2 = new Runnable() {
        @Override
        public void run() {
            long t2end = System.currentTimeMillis();
            long t2Delta = t2end - t2start;
            secondsTakenTariff2 = t2Delta / 1000;
            handler.post(this);
        }
    };

    /**
     * @Runnable object
     * used for timing tariff 1 in seconds
     */
    public Runnable runnableTariff1 = new Runnable() {
        @Override
        public void run() {
            long t1end = System.currentTimeMillis();
            long t1Delta = t1end - t1start;
            secondsTakenTariff1 = t1Delta / 1000;

            handler.post(this);
        }
    };

    /**
     * @param date date for which to verify whether is a business day
     * @param day  day for which to verify whether is a business day
     * @return true if a business day, otherwise return false
     */
    private boolean isBusinessDay(Date date, DayOfWeek day) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int month = cal.get(Calendar.MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        // check if weekend
        if (isDayWeekend(day)) {
            return false;
        }

        // check if New Year's Day
        if (month == Calendar.JANUARY
                && dayOfMonth == 1) {
            return false;
        }

        // check if Christmas
        if (month == Calendar.DECEMBER
                && dayOfMonth == 25) {
            return false;
        }

        // check if 4th of July
        if (month == Calendar.JULY
                && dayOfMonth == 4) {
            return false;
        }

        // check Thanksgiving (4th Thursday of November)
        if (month == Calendar.NOVEMBER
                && dayOfMonth == 4
                && day == DayOfWeek.THURSDAY) {
            return false;
        }

        // check Memorial Day (last Monday of May)
        if (month == Calendar.MAY
                && day == DayOfWeek.MONDAY
                && dayOfMonth > (31 - 7)) {
            return false;
        }

        // check Labor Day (1st Monday of September)
        if (month == Calendar.SEPTEMBER
                && dayOfMonth == 1
                && day == DayOfWeek.MONDAY) {
            return false;
        }

        // check President's Day (3rd Monday of February)
        if (month == Calendar.FEBRUARY
                && dayOfMonth == 3
                && day == DayOfWeek.MONDAY) {
            return true;
        }

        // check Veterans Day (November 11)
        if (month == Calendar.NOVEMBER
                && dayOfMonth == 11) {
            return true;
        }

        // check MLK Day (3rd Monday of January)
        if (month == Calendar.JANUARY
                && dayOfMonth == 3
                && day == DayOfWeek.MONDAY) {
            return true;
        }

        // IF NOTHING ELSE, IT'S A BUSINESS DAY
        return true;
    }

    /**
     * @restart tariff 3
     * -remove callbacks so that the time isn't constantly being updated
     * -reset fields
     * -restart time posting
     */

    private void restartTariff3() {

        if (isTariff3Running) {
            handler.removeCallbacks(runnableTariff3);
        }

        t3start = System.currentTimeMillis();

        isTariff3Running = true;

        handler.post(runnableTariff3);
    }

    /**
     * @stoptariff3 -remove callbacks so that the time isn't constantly being updated
     * -reset fields
     */


    private void stopTariff3() {

        if (isTariff3Running) {
            handler.removeCallbacks(runnableTariff3);
        }

        secondsTakenTariff3 = 0;
        tariff3Distance = 0;
        permTariff3Distance = 0;

        isTariff3Running = false;
        isTariff3Extra = false;

    }

    /**
     * @restart tariff 2
     * -remove callbacks so that the time isn't constantly being updated
     * -reset fields
     * -restart time posting
     */


    private void restartTariff2() {
        if (isTariff2Running) {
            handler.removeCallbacks(runnableTariff2);
        }

        t2start = System.currentTimeMillis();


        isTariff2Running = true;

        handler.post(runnableTariff2);

    }

    /**
     * @stop tariff 2
     * -remove callbacks so that the time isn't constantly being updated
     * -reset fields
     */

    private void stopTariff2() {
        if (isTariff2Running) {
            handler.removeCallbacks(runnableTariff2);
        }

        secondsTakenTariff2 = 0;
        tariff2Distance = 0;
        permTariff2Distance = 0;

        isTariff2Running = false;
        isTariff2Extra = false;

    }

    /**
     * @restart tariff 1
     * -remove callbacks so that the time isn't constantly being updated
     * -reset fields
     * -restart time posting
     */

    private void restartTariff1() {
        if (isTariff1Running) {
            handler.removeCallbacks(runnableTariff1);
        }

        t1start = System.currentTimeMillis();


        isTariff1Running = true;

        handler.post(runnableTariff1);

    }

    /**
     * @stop tariff 1
     * -remove callbacks so that the time isn't constantly being updated
     * -reset fields
     */

    private void stopTariff1() {
        if (isTariff1Running) {
            handler.removeCallbacks(runnableTariff1);
        }

        t1start = System.currentTimeMillis();
        secondsTakenTariff1 = 0;
        tariff1Distance = 0;
        permTariff1Distance = 0;
        isTariff1Running = false;
        isTariff1Extra = false;

    }

    /**
     * @param day day of the week to verify whether is a weekday or a day of the weekend
     * @return true
     * if not weekday
     * else return false
     */
    private boolean isDayWeekend(DayOfWeek day) {
        if (day.equals(DayOfWeek.SATURDAY) || day.equals(DayOfWeek.SUNDAY)) {
            return true;
        }
        return false;
    }


    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            broadcastLocation(location);
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

    /**
     * This method is called when the resources are lost.
     * <p>
     * Location is no longer being updated
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        serviceRunning = false;
        stopTariff3();
        stopTariff2();
        stopTariff1();

        stopTimer();

    }

    /**
     * @param location location object for which coordinates to broadcast
     *                 <p>
     *                 Method called when the service is initially started & when the location changes
     */
    protected void broadcastLocation(Location location) {
        Intent locationIntent = new Intent("location");
        if (locations.size() > 0) {
            distance += location.distanceTo(locations.get(locations.size() - 1));
        }

        locations.add(location);


        locationIntent.putExtra("locations", locations);
        locationIntent.putExtra("distance", distance);
        locationIntent.putExtra("longJourneyDist1", longJourneydist1);
        locationIntent.putExtra("longJourneyDist2", longJourneydist2);
        locationIntent.putExtra("longJourneyDist3", longJourneydist3);
        locationIntent.putExtra("fare", fare);
        locationIntent.putExtra("longJourney", longJourney);
        sendBroadcast(locationIntent);
    }

    public static boolean isServiceRunning() {
        return serviceRunning;
    }

    public static ArrayList<Location> getLocations() {
        return (ArrayList<Location>) locations.clone();
    }
}

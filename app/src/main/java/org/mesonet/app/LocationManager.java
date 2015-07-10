package org.mesonet.app;

import android.location.Location;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import java.util.Timer;
import java.util.TimerTask;


public class LocationManager
{
    private static LocationRequest sLocationRequest;
    private static LocationClient sLocationClient;
    private static Location sLastLocation;

    private static Timer sLocationTimer = new Timer();

    public static boolean sConnected = false;



    public static void Init()
    {
        sLocationRequest = LocationRequest.create();
        sLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        sLocationRequest.setInterval(90000);
        sLocationRequest.setFastestInterval(60000);

        sLocationClient = new LocationClient(MesonetApp.Activity(), MesonetApp.Activity(), MesonetApp.Activity());

        sLocationTimer.schedule(new LocationTask(), 0, DataContainer.kOneMinute * 5);
    }



    public static void SetLocation(Location inLocation)
    {
        sLastLocation = inLocation;
        Disconnect();
    }



    public static void StartUpdating()
    {
        sLocationClient.requestLocationUpdates(sLocationRequest, MesonetApp.Activity());
    }



    public static Location GetLocation()
    {
        return sLastLocation;
    }



    public static void Connect()
    {
        sConnected = false;
        if(sLocationClient != null && !sLocationClient.isConnected())
            sLocationClient.connect();
    }



    public static void Disconnect()
    {
        if(sConnected && sLocationClient != null && sLocationClient.isConnected()) {
            sLocationClient.removeLocationUpdates(MesonetApp.Activity());
            sLocationClient.disconnect();
        }
    }



    private static class LocationTask extends TimerTask
    {

        @Override
        public void run() {
            Connect();
        }
    }
}

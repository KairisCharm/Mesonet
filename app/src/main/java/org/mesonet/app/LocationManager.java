package org.mesonet.app;

import android.app.PendingIntent;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.Timer;
import java.util.TimerTask;


public class LocationManager
{
    private static LocationRequest sLocationRequest;
    private static FusedLocationProviderApi sLocationProviderApi;
    private static GoogleApiClient sLocationClient;
    private static Location sLastLocation;

    private static Timer sLocationTimer = new Timer();

    public static boolean sConnected = false;



    public static void Init()
    {
        sLocationRequest = LocationRequest.create();
        sLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        sLocationRequest.setInterval(90000);
        sLocationRequest.setFastestInterval(60000);

        sLocationProviderApi = new FusedLocationProviderApi() {
            @Override
            public Location getLastLocation(GoogleApiClient googleApiClient) {
                return null;
            }

            @Override
            public LocationAvailability getLocationAvailability(GoogleApiClient googleApiClient) {
                return null;
            }

            @Override
            public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationListener locationListener) {
                return null;
            }

            @Override
            public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationListener locationListener, Looper looper) {
                return null;
            }

            @Override
            public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationCallback locationCallback, Looper looper) {
                return null;
            }

            @Override
            public PendingResult<Status> requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, PendingIntent pendingIntent) {
                return null;
            }

            @Override
            public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, LocationListener locationListener) {
                return null;
            }

            @Override
            public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, PendingIntent pendingIntent) {
                return null;
            }

            @Override
            public PendingResult<Status> removeLocationUpdates(GoogleApiClient googleApiClient, LocationCallback locationCallback) {
                return null;
            }

            @Override
            public PendingResult<Status> setMockMode(GoogleApiClient googleApiClient, boolean b) {
                return null;
            }

            @Override
            public PendingResult<Status> setMockLocation(GoogleApiClient googleApiClient, Location location) {
                return null;
            }
        };

        sLocationTimer.schedule(new LocationTask(), 0, DataContainer.kOneMinute * 5);
    }



    public static void SetLocation(Location inLocation)
    {
        sLastLocation = inLocation;
        Disconnect();
    }



    public static void StartUpdating()
    {
        sLocationProviderApi.requestLocationUpdates(sLocationClient, sLocationRequest, MesonetApp.Activity());
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
            sLocationProviderApi.removeLocationUpdates(sLocationClient, MesonetApp.Activity());
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

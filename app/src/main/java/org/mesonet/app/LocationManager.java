package org.mesonet.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.LocationListener;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;


public class LocationManager{
    private static android.location.LocationManager sLocationManager = (android.location.LocationManager) MesonetApp.Context().getSystemService(Context.LOCATION_SERVICE);
    private static LocationAvailableListener sListener = new LocationAvailableListener();

    private static GoogleApiClient sGoogleApiClient = new GoogleApiClient.Builder(MesonetApp.Context())
            .addApi(LocationServices.API)
            .build();


    public static void Connect() {
        sGoogleApiClient.connect();

        try {
            sLocationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, sListener);
        }
        catch(SecurityException ex) {
            LocalFragment.LocationDisconnected();
        }
    }



    public static boolean IsConnected()
    {
        return sGoogleApiClient.isConnected();
    }



    public static Location GetLocation()
    {
        return LocationServices.FusedLocationApi.getLastLocation(sGoogleApiClient);
    }



    public static void Disconnect()
    {
        sGoogleApiClient.disconnect();
    }



    private static class LocationAvailableListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            LocalFragment.LocationConnected();
            RadarFragment.LocationConnected();
        }

        @Override
        public void onProviderDisabled(String provider) {
            LocalFragment.LocationDisconnected();
            RadarFragment.LocationDisconnected();
        }
    }
}

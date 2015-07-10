package org.mesonet.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;



public class RadarFragment extends StaticFragment
{
    private TextView mUpdateTimeText = null;
    private TextView mTimeText = null;
	private RadarMapView mMapView = null;
    private ImageView mLegend = null;
	private Button mPlayPauseBtn = null;
	private Spinner mLocateSpinner = null;
	private SeekBar mTransparencySeekBar = null;
	private RadarTransparencyWindow mTransparencyLayout = null;
	
	private static Timer sRadarLooper = new Timer();
    private static Timer sTrackbarTimer = null;
	private static int sCurrentImage = 0;
	private static boolean sPlaying = false;

    private static String sTimeStamp = "";
	private static Date sUpdateTime;

	
	
	@Override 
	public void onCreate(Bundle inSavedInstanceState)
	{
		super.onCreate(inSavedInstanceState);

        RadarData.Initialize();

		MapsInitializer.initialize(getActivity());
	}
	
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
	    super.onActivityCreated(savedInstanceState);
	    
	    mMapView.onCreate(savedInstanceState);
	}
	
	
	
	@Override
	public View onCreateView(LayoutInflater inInflater, ViewGroup inContainer, Bundle inSavedInstanceState)
	{
		return Inflate(inInflater, inContainer);
	}
	
	
	
	@Override
	public void onStart()
	{
		super.onStart();

		mTransparencySeekBar.setMax(255);
		mTransparencySeekBar.setProgress(RadarData.Transparency());
		mTransparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar inBar, int inProgress, boolean inFromUser)
			{
				RadarData.SetTransparency(inBar.getProgress());
			}
			
			
			
			@Override public void onStartTrackingTouch(SeekBar inBar)
            {
                sTrackbarTimer.cancel();
            }
			@Override public void onStopTrackingTouch(SeekBar inBar)
            {
                sTrackbarTimer = new Timer();
                sTrackbarTimer.schedule(new TrackbarRemover(), 10000);
            }
		});
	}
	
	
	
	@Override
	public void onResume()
	{
	    super.onResume();
	    mMapView.onResume();

        RadarData.StartDownload(false);

	    if(mMapView.getMap() != null)
	    {
		    mMapView.getMap().getUiSettings().setZoomControlsEnabled(false);
		    mMapView.getMap().getUiSettings().setRotateGesturesEnabled(false);

            InitActionBar(0);
	
			mMapView.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(RadarData.GetCurrentData().mLatitude, RadarData.GetCurrentData().mLongitude), 7.0f));

            Activate();
	    }

        mMapView.SetImage();
	}
	
	
	
	@Override
	public void onPause()
	{
	    super.onPause();

        RadarData.StopDownload();

	    if(mMapView != null)
	    	mMapView.onPause();
	    
	    if(sPlaying)
			PlayPauseToggle();
	}
	
	
	
	@Override
	public void onLowMemory()
	{
	    super.onLowMemory();
	    if(mMapView != null)
	    	mMapView.onLowMemory();
	}
	
	
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
	    super.onSaveInstanceState(outState);
	    
	    if(mMapView != null)
	    	mMapView.onSaveInstanceState(outState);
	}
	
	
	
	@Override
	public void onDestroy()
	{
	    super.onDestroy();
	    if(mMapView != null)
	    	mMapView.onDestroy();
	}



    protected View Inflate(LayoutInflater inInflater, ViewGroup inContainer)
    {
        View toReturn = inInflater.inflate(R.layout.radar_fragment_layout, inContainer, false);

        mUpdateTimeText = (TextView)toReturn.findViewById(R.id.radar_update_time);
        mTimeText = (TextView)toReturn.findViewById(R.id.radar_time);
        mMapView = (RadarMapView)toReturn.findViewById(R.id.radar_map);
        mLegend = (ImageView)toReturn.findViewById(R.id.radar_legend);
        mPlayPauseBtn = (Button)toReturn.findViewById(R.id.radar_play_pause);
        mTransparencyLayout = (RadarTransparencyWindow)toReturn.findViewById(R.id.transparency_layout);
        mTransparencySeekBar = (SeekBar)toReturn.findViewById(R.id.transparency_seekbar);

        mPlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayPauseToggle();
            }
        });

        return toReturn;
    }
	
	
	
	private static boolean Activated()
	{
		return (This() != null) && This().IsActivated();
	}



    @Override
    public View InitActionBar(int inLayoutId)
    {
        View actionBarView = super.InitActionBar(R.layout.local_and_radar_action_bar_layout);

        mLocateSpinner = (Spinner)actionBarView.findViewById(R.id.locate_spinner);
        Button locateButton = (Button)actionBarView.findViewById(R.id.locate_button);

        ArrayAdapter<Object> cursorAdapter = RadarData.GetListAdapter();
        cursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mLocateSpinner.setAdapter(cursorAdapter);
        mLocateSpinner.setSelection(RadarData.GetCityIndex());
        mLocateSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> inParentView, View inSelectedItemView, int inPosition, long inId)
            {
                mTimeText.setText("");
                mUpdateTimeText.setText("");

                if(RadarData.SetLocation((String)(RadarData.GetKeyList().toArray())[inPosition])) {
                    RadarData.ResetDownloads();

                    RefreshMap(RadarData.GetCurrentData());

                    SavedDataManager.SaveStringSetting("radar", RadarData.GetCity());
                }
            }



            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        locateButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                RadarData.StopDownload();
                Location location = LocationManager.GetLocation();

                if(location != null)
                    mLocateSpinner.setSelection(RadarData.GetNearestRadarLocation(location));
            }
        });

        return null;
    }



    private boolean IsTransparencyTrackerVisible()
    {
        return mTransparencyLayout.getVisibility() == View.VISIBLE;
    }
	
	
	
	public static void ShowTransparencyTracker()
	{
		if(!Activated())
			return;

        if(sTrackbarTimer != null)
            sTrackbarTimer.cancel();

        sTrackbarTimer = new Timer();

        sTrackbarTimer.schedule(new TrackbarRemover(), 10000);
		This().mTransparencyLayout.setVisibility(RelativeLayout.VISIBLE);
	}



    public static void SetTimeText(String inString, Date inUpdateTime)
    {
        sTimeStamp = inString;
        sUpdateTime = inUpdateTime;
        MesonetApp.Activity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH);
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date time = format.parse(sTimeStamp);
                    format = new SimpleDateFormat("h:mm a", Locale.ENGLISH);

                    if (time.getTime() < 0)
                        This().mTimeText.setText("-");
                    else
                        This().mTimeText.setText(format.format(time));

                    This().mUpdateTimeText.setText("Last update: " + format.format(sUpdateTime));
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });
    }
	
	
	
	private static RadarFragment This()
	{
		return MainActivity.RadarFragment();
	}



    public static boolean TurnOffTransparencyLayout()
    {
        if(!This().IsTransparencyTrackerVisible())
            return false;

        This().mTransparencyLayout.setVisibility(View.GONE);

        if(sTrackbarTimer != null)
            sTrackbarTimer.cancel();

        return true;
    }
	
	
	
	public static void RefreshMap(RadarData.RadarBoundsData inData)
	{
		if(!Activated())
			return;
		
		if(sPlaying)
			PlayPauseToggle();
		
		TurnOffTransparencyLayout();
		
		This().mMapView.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(inData.mLatitude, inData.mLongitude), 7.0f));
	}
	
	
	
	public static void PlayPauseToggle()
	{
		if(!Activated())
			return;
		
		TurnOffTransparencyLayout();
		if(sPlaying)
		{
			sPlaying = false;
			This().mPlayPauseBtn.setBackgroundResource(R.drawable.radar_play);
			
			sRadarLooper.cancel();
			sRadarLooper = new Timer();
			
			RadarMapView.Pause();
		}
		else
		{
			sPlaying = true;
			This().mPlayPauseBtn.setBackgroundResource(R.drawable.radar_pause);
			
			if(sCurrentImage == 0)
				sCurrentImage = RadarData.kLoopImageCount - 1;
			
			RadarMapView.Play();
		}
	}



    public static class TrackbarRemover extends TimerTask
    {
        @Override
        public void run() {
            MesonetApp.Activity().runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    TurnOffTransparencyLayout();
                }
            });
        }
    }
}
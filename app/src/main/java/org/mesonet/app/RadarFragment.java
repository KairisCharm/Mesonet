package org.mesonet.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
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
	private Button mPlayPauseBtn = null;
	private ImageView mLegend = null;
	private Spinner mLocateSpinner = null;
	private SeekBar mTransparencySeekBar = null;
	private RadarTransparencyWindow mTransparencyLayout = null;
	private ImageButton mLocateButton = null;
	private Toolbar mToolbar = null;
	private RelativeLayout mContent = null;
	private Drawable mLegendDrawableVert = null;
	private Drawable mLegendDrawableHorz = null;
	
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
		//View result =

		//ArrangeWithTabs(getResources().getConfiguration());

		return Inflate(inInflater, inContainer);
	}



	public void ResizeToolbar()
	{
		mToolbar.getLayoutParams().height = MainActivity.GetToolbarHeight();
		mToolbar.requestLayout();

		ArrangeWithTabs();

		mLegend.setImageDrawable(GetLegend());
	}



	private void ArrangeWithTabs()
	{
		switch (getResources().getConfiguration().orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				((ViewGroup.MarginLayoutParams)mContent.getLayoutParams()).setMargins(0, MainActivity.GetToolbarHeight(), 0, 0);
				if(getView() != null)
					getView().requestLayout();
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				((ViewGroup.MarginLayoutParams)mContent.getLayoutParams()).setMargins(0, 0, 0, 0);
				if(getView() != null)
					getView().requestLayout();
				break;
		}
	}
	
	
	
	@Override
	public void onStart()
	{
		super.onStart();

		mTransparencySeekBar.setMax(255);
		mTransparencySeekBar.setProgress(RadarData.Transparency());
		mTransparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar inBar, int inProgress, boolean inFromUser) {
				RadarData.SetTransparency(inBar.getProgress());
			}


			@Override
			public void onStartTrackingTouch(SeekBar inBar) {
				sTrackbarTimer.cancel();
			}

			@Override
			public void onStopTrackingTouch(SeekBar inBar) {
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
	
			mMapView.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(RadarData.GetCurrentData().mLatitude, RadarData.GetCurrentData().mLongitude), 7.0f));

            Activate();
	    }

        RadarMapView.SetImage();
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
        mPlayPauseBtn = (Button)toReturn.findViewById(R.id.radar_play_pause);
		mLegend = (ImageView)toReturn.findViewById(R.id.radar_legend);
        mTransparencyLayout = (RadarTransparencyWindow)toReturn.findViewById(R.id.transparency_layout);
        mTransparencySeekBar = (SeekBar)toReturn.findViewById(R.id.transparency_seekbar);
		mToolbar = (Toolbar)toReturn.findViewById(R.id.toolBar);
		mContent = (RelativeLayout)toReturn.findViewById(R.id.content);

        mPlayPauseBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayPauseToggle();
			}
		});

		InitActionBar((Toolbar) toReturn.findViewById(R.id.toolBar));

		GetLegend();

        return toReturn;
    }



	private Drawable GetLegend()
	{
		switch (getResources().getConfiguration().orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				if(mLegendDrawableVert == null) {
					if(Build.VERSION.SDK_INT >= 21)
						mLegendDrawableVert = getResources().getDrawable(R.drawable.map_legend, getActivity().getTheme());
					else
						mLegendDrawableVert = getResources().getDrawable(R.drawable.map_legend);
				}
				return mLegendDrawableVert;
			case Configuration.ORIENTATION_LANDSCAPE:
				if(mLegendDrawableHorz == null) {
					if(Build.VERSION.SDK_INT >= 21)
						mLegendDrawableHorz = getResources().getDrawable(R.drawable.map_legend, getActivity().getTheme());
					else
						mLegendDrawableHorz = getResources().getDrawable(R.drawable.map_legend);
				}
				return mLegendDrawableHorz;
		}

		return null;
	}
	
	
	
	private static boolean Activated()
	{
		return (This() != null) && This().IsActivated();
	}



    public View InitActionBar(Toolbar inToolbar)
    {
        mLocateSpinner = (Spinner)inToolbar.findViewById(R.id.locate_spinner);
		mLocateButton = (ImageButton)inToolbar.findViewById(R.id.locate_button);

        ArrayAdapter<Object> cursorAdapter = RadarData.GetListAdapter();
        cursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mLocateSpinner.setAdapter(cursorAdapter);
        mLocateSpinner.setSelection(RadarData.GetCityIndex());
        mLocateSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> inParentView, View inSelectedItemView, int inPosition, long inId) {
				mTimeText.setText("");
				mUpdateTimeText.setText("");

				if (RadarData.SetLocation((String) (RadarData.GetKeyList().toArray())[inPosition])) {
					RadarData.ResetDownloads();

					RefreshMap(RadarData.GetCurrentData());

					SavedDataManager.SaveStringSetting("radar", RadarData.GetCity());
				}
			}


			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

        mLocateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				RadarData.StopDownload();
				Location location = LocationManager.GetLocation();

				if (location != null)
					mLocateSpinner.setSelection(RadarData.GetNearestRadarLocation(location));
			}
		});

		inToolbar.inflateMenu(R.menu.main_menu);
		inToolbar.getMenu().getItem(2).setVisible(true);

		inToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem inItem) {
				MainActivity.SelectMenuItem(inItem);
				return true;
			}
		});

        return null;
    }



    private boolean IsTransparencyTrackerVisible()
    {
        return mTransparencyLayout.getVisibility() == View.VISIBLE;
    }



	public static void LocationConnected()
	{
		if(This().mLocateButton != null) {
			This().mLocateButton.setEnabled(true);
			This().mLocateButton.setImageResource(R.drawable.ic_gps_fixed_grey_36dp);
		}
	}



	public static void LocationDisconnected()
	{
		if(This().mLocateButton != null) {
			This().mLocateButton.setEnabled(false);
			This().mLocateButton.setImageResource(R.drawable.ic_gps_off_grey_36dp);
		}
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
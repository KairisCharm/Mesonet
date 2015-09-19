package org.mesonet.app;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;



public class LocalFragment extends StaticFragment
{
	private WebView mMeteoPage = null;
	private CurrentConditionsControls mCurrCondControls = null;
	private ForecastControls mForecastControls = null;

	private Spinner mLocateSpinner = null;
	private ViewPager mViewPager = null;

    private int mActionBarMode = kSelectMode;

    private static SimpleCursorAdapter sCursor = null;

    private static boolean sMeteoIsVisible = false;



    @Override public void onResume(){super.onResume();LocalData.StartDownload(false);}
    @Override public void onPause(){LocalData.StopDownload();super.onPause();}



    @Override
    public void onCreate(Bundle inSavedInstanceState)
    {
        super.onCreate(inSavedInstanceState);

        SiteData.Initialize();
        LocalData.Initialize();
    }
	
	

	@Override
	public View onCreateView(LayoutInflater inInflater, ViewGroup inContainer, Bundle inSavedInstanceState)
	{
        return Inflate(inInflater, inContainer, getResources().getConfiguration());
	}



    @Override
    public View InitActionBar(int inDummy)
    {
        mActionBarMode = kNonSelectMode;
        View actionBarView = super.InitActionBar(R.layout.local_and_radar_action_bar_layout);

        mLocateSpinner = (Spinner)actionBarView.findViewById(R.id.locate_spinner);

        Button locateButton = (Button)actionBarView.findViewById(R.id.locate_button);

        if (mLocateSpinner.getAdapter() == null && SiteData.SitesList() != null)
            UpdateList();

        mLocateSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> inParentView, View inSelectedItemView, int inPosition, long inId)
            {
                if(mActionBarMode != kNonSelectMode) {
                    SiteData.SetPosition(inPosition);
                    SetSite(SiteData.Stid());
                }

                mActionBarMode = kSelectMode;
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
                LocalData.ResetDownloads();
                ClearAllFields();
                GetNearestSiteLocation();
            }
        });

        return null;
    }



    protected View Inflate(LayoutInflater inInflater, ViewGroup inContainer, Configuration inConfiguration)
    {
        View toReturn = inInflater.inflate(R.layout.local_fragment_layout, inContainer, false);

        mCurrCondControls = new CurrentConditionsControls();
        mForecastControls = new ForecastControls();

        mMeteoPage = (WebView)toReturn.findViewById(R.id.meteogram);

        mMeteoPage.getSettings().setUserAgentString(MesonetApp.UserAgentString());
        mMeteoPage.getSettings().setBuiltInZoomControls(true);
        mMeteoPage.getSettings().setUseWideViewPort(true);
        mMeteoPage.setInitialScale(10);

        switch(inConfiguration.orientation)
        {
            case Configuration.ORIENTATION_PORTRAIT:
                mViewPager = (ViewPager)toReturn.findViewById(R.id.local_vertviewpager);
                mViewPager.setOffscreenPageLimit(4);
                mViewPager.setAdapter(new LocalVertPageAdapter());
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                mViewPager = (ViewPager)toReturn.findViewById(R.id.local_horzviewpager);
                mViewPager.setOffscreenPageLimit(2);
                mViewPager.setAdapter(new LocalHorzPageAdapter());
                break;
        }

        mViewPager.setCurrentItem(0);

        LinearLayout currCondLayout = (LinearLayout)toReturn.findViewById(R.id.curr_cond_container);

        if(currCondLayout != null)
            currCondLayout.addView(mCurrCondControls.GenerateLayout());

        SetTextsAndImages();

        return toReturn;
    }
	
	
	
	protected static boolean Activated()
	{
		return (This() != null) && This().IsActivated();
	}
	
	
	
	private static LocalFragment This()
	{
		return MainActivity.LocalFragment();
	}
	
	
	
	public static void SetCurrCondTexts()
	{
		if(!Activated())
			return;
		
		This().mCurrCondControls.SetTexts(CurrentConditions.Time(), CurrentConditions.AirTemp(), CurrentConditions.FeelsLike(), CurrentConditions.Dewpoint(), CurrentConditions.Wind(), CurrentConditions.Rain24Hr(), CurrentConditions.Humidity(), CurrentConditions.Gusts(), CurrentConditions.Pressure());

        if(CurrentConditions.MeteoUrl() != null && This().mMeteoPage.getUrl() != null && This().mMeteoPage.getUrl().compareTo(CurrentConditions.MeteoUrl()) != 0)
        {
            This().mMeteoPage.loadUrl(CurrentConditions.MeteoUrl());

            if(sMeteoIsVisible)
                This().mMeteoPage.setVisibility(View.VISIBLE);
        }
	}
	
	
	
	public static void UpdateList()
	{
		This().Activate();
		
		MesonetApp.Activity().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				SetList();

                String defStid = SavedDataManager.GetStringResource(R.string.default_station);
                SetSite(SavedDataManager.GetStringSetting("stid", defStid));
			}
		});
	}



    public static void SetList() {

        if(This() != null && This().mLocateSpinner != null) {

            String[] from = new String[]{"name"};
            int[] to = new int[]{android.R.id.text1};

            int index = SiteData.SiteIndex();

            if (sCursor == null) {
                sCursor = new SimpleCursorAdapter(MesonetApp.Activity(), android.R.layout.simple_spinner_item, SiteData.SitesList(), from, to, 0);
                sCursor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }

            This().mLocateSpinner.setAdapter(sCursor);

            if(index != -1)
                This().mLocateSpinner.setSelection(index);
        }
    }
	
	
	
	public static void SetForecastTextsAndImages()
	{
		if(!Activated())
			return;
		
		if(This().mForecastControls != null)
		{
			for(int i = 0; i < Forecast.kDataEntriesCount; i++)
			{
				if(Forecast.GetData(i) != null)
					This().mForecastControls.SetTextsAndImages(i, Forecast.GetData(i));
			}
		}
	}
	
	
	
	public static void SetTextsAndImages()
	{
		if(!Activated())
			return;
		
		SetCurrCondTexts();
		SetForecastTextsAndImages();
	}
	
	
	
	public static void ClearCurrCondFields()
	{
		if(!Activated())
			return;
		
		This().mCurrCondControls.ClearFields();
	}
	
	
	
	public static void ClearForecastFields()
	{
		if(!Activated())
			return;
		
		This().mForecastControls.ClearFields();
	}
	
	
	
	public static void ClearAllFields()
	{
		if(!Activated())
			return;
		
		This().mCurrCondControls.ClearFields();
		This().mForecastControls.ClearFields();
	}
	
	
	
	public static boolean BackToCurrCond()
	{
		if(!Activated())
			return false;
		
		if(This().mMeteoPage.getVisibility() == View.INVISIBLE)
			return false;
		
		sMeteoIsVisible = false;
		This().mMeteoPage.setVisibility(View.INVISIBLE);
		
		return true;
	}
	
	
	
	public static void DisplayMeteogram()
	{
		if(!Activated())
			return;
		
		sMeteoIsVisible = true;
		This().mMeteoPage.setVisibility(View.VISIBLE);
	}
	
	
	
	public static void GetNearestSiteLocation()
	{
		if(!Activated())
			return;
		
		Location location = LocationManager.GetLocation();
		
		if(location != null)
		{
            String nearestSite = SiteData.SetToNearestSite(location);
			SavedDataManager.SaveStringSetting("stid", nearestSite);

			SetSite(nearestSite);
		}
	}
	
	
	
	public static void SetSite(String inStid)
	{
		if(!Activated())
			return;

        ClearAllFields();
        if(SiteData.IsSameSite(inStid) && This().mActionBarMode != kUpdateMode)
            SetTextsAndImages();
		else
		    SiteData.SetStid(inStid);

        if(This().mLocateSpinner.getSelectedItemPosition() != SiteData.SiteIndex()) {
            This().mLocateSpinner.setSelection(SiteData.SiteIndex());
        }
	}
	
	
	
	private static class LocalVertPageAdapter extends LocalPageAdapter
	{
		@Override
		public Object instantiateItem(ViewGroup inContainer, int inPosition)
		{
			LinearLayout outFullPage = This().mForecastControls.GeneratePage(inPosition, SavedDataManager.GetIntResource(R.integer.forecasts_per_page));
			
			if(inPosition == 4)
				SetTextsAndImages();

			inContainer.addView(outFullPage);
			
			return outFullPage;
		}
		
		

		@Override
		public int getCount()
		{
			return 5;
		}
	}
	
	
	
	private static class LocalHorzPageAdapter extends LocalPageAdapter
	{
		@Override
		public Object instantiateItem(ViewGroup inContainer, int inPosition)
		{
			LinearLayout outFullPage;
				
			if(inPosition == 0)
			{
				outFullPage = This().mCurrCondControls.GenerateLayout();
				SetCurrCondTexts();
			}
			else
			{
				outFullPage = This().mForecastControls.GeneratePage(inPosition - 1, SavedDataManager.GetIntResource(R.integer.forecasts_per_page));
				if(inPosition == 2)
					SetForecastTextsAndImages();
			}

            if(outFullPage != null)
			    inContainer.addView(outFullPage);
			
			return outFullPage;
		}
		
		

		@Override
		public int getCount()
		{
			return 3;
		}
	}



    private static abstract class LocalPageAdapter extends PagerAdapter
    {
        @Override
        public void destroyItem(ViewGroup inContainer, int inPosition, Object inObject)
        {
            inContainer.removeView((View)inObject);
        }



        @Override
        public float getPageWidth(int inPosition)
        {
            return 1;
        }



        @Override
        public boolean isViewFromObject(View inView, Object inObject)
        {
            return(inView == inObject);
        }
    }
	
	
	
	public class CurrentConditionsControls
	{		
		private LinearLayout mFullPage;
		private TextView mTempText;
		private TextView mFeelsLikeText;
		private TextView mDewpointText;
		private TextView mRain24Text;
		private TextView mWindText;
		private TextView mTimeText;
		private TextView mHumidityText = null;
		private TextView mWindGustsText = null;
		private TextView mPressureText = null;
        private ImageButton mMeteoBtn;
		
		
		
		public LinearLayout GenerateLayout()
		{			
			if(mFullPage == null)
			{
				mFullPage = new LinearLayout(getActivity());
				mFullPage.setOrientation(LinearLayout.HORIZONTAL);
				
				mFullPage = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.current_conditions_layout, null, false);
				
				mTempText = (TextView)mFullPage.findViewById(R.id.current_tair);
				mFeelsLikeText = (TextView)mFullPage.findViewById(R.id.current_feelsLike);
				mDewpointText = (TextView)mFullPage.findViewById(R.id.current_dewpoint);
				mRain24Text = (TextView)mFullPage.findViewById(R.id.current_24hrRain);
				mWindText = (TextView)mFullPage.findViewById(R.id.current_wind);
				mTimeText = (TextView)mFullPage.findViewById(R.id.current_time);
				
				mHumidityText = (TextView)mFullPage.findViewById(R.id.current_humidity);
				mWindGustsText = (TextView)mFullPage.findViewById(R.id.current_windgusts);
				mPressureText = (TextView)mFullPage.findViewById(R.id.current_pressure);

                mMeteoBtn = (ImageButton)mFullPage.findViewById(R.id.meteogram_btn);
                mMeteoBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        DisplayMeteogram();
                    }
                });
			}
			else
			{
				ViewGroup pageParent = (ViewGroup)mFullPage.getParent();
				
				if(pageParent != null)
					pageParent.removeView(mFullPage);
			}
			
	        return mFullPage;
		}
		
		
		
		public void SetTexts(String inTime, String inTemp, String inFeelsLike, String inDewpoint, String inWind, String inRain, String inHumidity, String inWindGusts, String inPressure)
		{
			if(mTempText != null)
			{
				mTempText.setText(inTemp);
				mFeelsLikeText.setText(inFeelsLike);
				mDewpointText.setText(inDewpoint);
				mRain24Text.setText(inRain);
				mWindText.setText(inWind);
				mTimeText.setText(getResources().getString(R.string.time_observed_at) + inTime);
				if(mHumidityText != null)
				{
					mHumidityText.setText(inHumidity);
					mWindGustsText.setText(inWindGusts);
					mPressureText.setText(inPressure);
				}
			}
			
			This().mMeteoPage.loadUrl(CurrentConditions.MeteoUrl());
		}
		
		
		
		public void ClearFields()
		{
			if(mTempText != null)
			{
				Resources resources = getResources();
				
				String emptyField = resources.getString(R.string.empty_field);
				String emptyTime = resources.getString(R.string.empty_time);
				
				mTempText.setText(emptyField);
				mFeelsLikeText.setText(emptyField);
				mDewpointText.setText(emptyField);
				mRain24Text.setText(emptyField);
				mWindText.setText(emptyField);
				mTimeText.setText(getResources().getString(R.string.time_observed_at) + emptyTime);
				if(mHumidityText != null)
				{
					mHumidityText.setText(emptyField);
					mWindGustsText.setText(emptyField);
					mPressureText.setText(emptyField);
				}
				
				This().mMeteoPage.loadUrl("about:blank");
			}
		}
	}
	
	
	
	public class ForecastControls
	{
		static final public int kAlphaChannel = 0xff000000;

        private int mForecastsPerPage = 0;
		
		private LinearLayout[] mFullLayout = null;
		private ScalableLayout[] mForecastLayout = new ScalableLayout[Forecast.kDataEntriesCount];
		private ImageView[] mImage = new ImageView[Forecast.kDataEntriesCount];
		private TextView[] mPeriodText = new TextView[Forecast.kDataEntriesCount];
		private TextView[] mWindDirText = new TextView[Forecast.kDataEntriesCount];
		private TextView[] mWindSpeedText = new TextView[Forecast.kDataEntriesCount];
		private TextView[] mConditionText = new TextView[Forecast.kDataEntriesCount];
		private TextView[] mLowOrHighText = new TextView[Forecast.kDataEntriesCount];
		
		
		
		public LinearLayout GeneratePage(int inPosition, int inForecastsPerPage)
		{
			if(mFullLayout == null || mForecastsPerPage != inForecastsPerPage) {
                mForecastsPerPage = inForecastsPerPage;
                mFullLayout = new LinearLayout[Forecast.kDataEntriesCount / inForecastsPerPage];
            }
			
			if(mFullLayout[inPosition] == null)
			{
				mFullLayout[inPosition] = new LinearLayout(getActivity());
				mFullLayout[inPosition].setOrientation(LinearLayout.HORIZONTAL);
				
				
				for(int i = inPosition * inForecastsPerPage; i < ((inPosition + 1) * inForecastsPerPage); i++)
				{
					View layout = getActivity().getLayoutInflater().inflate(R.layout.forecast_layout, null, false);
					
					mForecastLayout[i] = (ScalableLayout)layout.findViewWithTag("forecast_layout"); 
					mPeriodText[i] = (TextView)layout.findViewWithTag("forecast_time");
					mPeriodText[i].setTypeface(mPeriodText[i].getTypeface(), Typeface.BOLD);
					mImage[i] = (ImageView)layout.findViewWithTag("forecast_image");
					mConditionText[i] = (TextView)layout.findViewWithTag("forecast_condition");
					mWindDirText[i] = (TextView)layout.findViewWithTag("forecast_winddir");
					mWindSpeedText[i] = (TextView)layout.findViewWithTag("forecast_windspeed");
					mLowOrHighText[i] = (TextView)layout.findViewWithTag("forecast_loworhigh");
					
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
					params.weight = 1;
					mFullLayout[inPosition].addView(layout, params);
				}
				
				return mFullLayout[inPosition];
			}
			
			return mFullLayout[inPosition];
		}
		
		
		
		public void SetTextsAndImages(int inIndex, Forecast.ForecastData inData)
		{
			if(mPeriodText[inIndex] != null && inData.mTime != null)
			{
				Resources resources = getActivity().getResources();
				
		 		mPeriodText[inIndex].setText(inData.mTime);
				mImage[inIndex].setImageBitmap(inData.mImage);
				mConditionText[inIndex].setText(inData.mCondition);
				
				mForecastLayout[inIndex].setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.day_background));
				int color = ContextCompat.getColor(getActivity(), R.color.pink_text_color);
				
				if(inData.mLowOrHigh != null && inData.mLowOrHigh.compareTo(resources.getString(R.string.forecast_low)) == 0)
				{
					mForecastLayout[inIndex].setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.night_background));
					color = ContextCompat.getColor(getActivity(), R.color.blue_text_color);
				}
					
				color -= kAlphaChannel;
				
				mLowOrHighText[inIndex].setText(Html.fromHtml("<font color='#" + Integer.toHexString(color) + "'>" + inData.mLowOrHigh + "</font> " +  inData.GetTemp()));
				
				if(mWindDirText[inIndex] != null && mWindSpeedText[inIndex] != null)
				{
					String direction = inData.mWindDir1;
					String magnitude = inData.mWindMag1;
					String units = resources.getString(R.string.wind_mph);
					
					if(inData.mWindDir1.compareTo(inData.mWindDir2) != 0)
						direction += "-" + inData.mWindDir2;
					
					if(inData.mWindMag1.compareTo(inData.mWindMag2) != 0)
						magnitude += "-" + inData.mWindMag2;
					
					if(MainMenu.GetUnitSystem() == MainMenu.UnitSystem.kMetric)
					{
						units = resources.getString(R.string.wind_mps);
						
						magnitude = Long.valueOf(Math.round(Conversion.MilesPerHourToMetersPerSecond(Double.parseDouble(inData.mWindMag1)))).toString();
						
						if(inData.mWindMag1.compareTo(inData.mWindMag2) != 0)
							magnitude += "-" + Long.valueOf(Math.round(Conversion.MilesPerHourToMetersPerSecond(Double.parseDouble(inData.mWindMag2)))).toString();
					}
					
					if(mWindDirText[inIndex] != null && mWindSpeedText[inIndex] != null)
					{
						mWindDirText[inIndex].setText(resources.getString(R.string.wind) + direction + resources.getString(R.string.wind_at));
						mWindSpeedText[inIndex].setText(magnitude + units);
						
						if(mWindDirText[inIndex].getTextSize() > mWindSpeedText[inIndex].getTextSize())
							mWindDirText[inIndex].setTextSize(mWindSpeedText[inIndex].getTextSize());
						
						if(mWindSpeedText[inIndex].getTextSize() > mWindDirText[inIndex].getTextSize())
							mWindSpeedText[inIndex].setTextSize(mWindDirText[inIndex].getTextSize());
					}
				}
			}
		}
		
		
		
		public void ClearFields()
		{
			if(mPeriodText[0] != null)
			{
				Resources resources = getResources();
				
				String emptyField = resources.getString(R.string.empty_field);
				
				for(int i = 0; (i < Forecast.kDataEntriesCount) && (mPeriodText[i] != null); i++)
				{
					mPeriodText[i].setText(emptyField);
					mImage[i].setImageBitmap(null);
					mConditionText[i].setText(emptyField);
					
					int color = ContextCompat.getColor(getActivity(), R.color.pink_text_color);
					
					mForecastLayout[i].setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.day_background));
					
					if((i % 2) == 1)
					{
						mForecastLayout[i].setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.night_background));
						color = ContextCompat.getColor(getActivity(), R.color.blue_text_color);
					}
					
					color -= kAlphaChannel;
					
					mLowOrHighText[i].setText(Html.fromHtml("<font color='#" + Integer.toHexString(color) + "'>" + emptyField + "</font> " + emptyField));
					
					if(mWindDirText[i] != null && mWindSpeedText[i] != null)
					{
						mWindDirText[i].setText(resources.getString(R.string.wind) + "-" + resources.getString(R.string.wind_at));
						mWindSpeedText[i].setText("-");
					}
				}
			}
		}
	}
}

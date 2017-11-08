package org.mesonet.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.support.design.widget.TabLayout;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;

import java.util.Calendar;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity
{
	private LocalFragment mLocalFragment = null;
	private MapsFragment mMapsFragment = null;
	private RadarFragment mRadarFragment = null;
	private AdvisoriesFragment mAdvisoriesFragment = null;
	private TabLayout mVertTabs;
	private TabLayout mHorzTabs;
	private ViewPager mViewPager;
	private int mToolbarSizeVert = 0;
	private int mToolbarSizeHorz = 0;
	private Drawable mAdvisoryCountImageVert = null;
	private Drawable mAdvisoryCountImageHorz = null;
	private FragmentPagerAdapter mAdapter = null;

    private static boolean sFirstStartup = true;

	
	
	@Override
	protected void onCreate(Bundle inSavedInstanceState)
	{
        super.onCreate(inSavedInstanceState);

		switch (getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				mToolbarSizeVert = GetActionBarSize();
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				mToolbarSizeHorz = GetActionBarSize();
				break;
		}

		MesonetApp.SetActivity(this);
		MapsData.Initialize();

		setContentView(R.layout.main_activity_port);

		InflateTabs();

		PopupManager.ShowChanges();

		mLocalFragment = new LocalFragment();
		mMapsFragment = new MapsFragment();
		mRadarFragment = new RadarFragment();
		mAdvisoriesFragment = new AdvisoriesFragment();

        if(sFirstStartup) {
            AnalyticsTracker.Send();
            sFirstStartup = false;
        }
	}



	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		InflateTabs();

		switch (newConfig.orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				if(mToolbarSizeVert == 0)
					mToolbarSizeVert = GetActionBarSize();

				mVertTabs.setVisibility(View.VISIBLE);
				mHorzTabs.setVisibility(View.GONE);
				mVertTabs.getLayoutParams().height = mToolbarSizeVert;
				mVertTabs.requestLayout();
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				if(mToolbarSizeHorz == 0)
					mToolbarSizeHorz = GetActionBarSize();

				mVertTabs.setVisibility(View.GONE);
				mHorzTabs.setVisibility(View.VISIBLE);
				mHorzTabs.getLayoutParams().height = mToolbarSizeHorz;
				mHorzTabs.requestLayout();
				break;
		}

		switch (mViewPager.getCurrentItem())
		{
			case 1:
				mMapsFragment.ResizeToolbar();
				break;
			case 2:
				mRadarFragment.ResizeToolbar();
				break;
			case 3:
				mAdvisoriesFragment.ResizeToolbar();
		}
	}



	private Drawable GetAdvisoryCountImage()
	{
		switch (getResources().getConfiguration().orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				if(mAdvisoryCountImageVert == null){
					if (Build.VERSION.SDK_INT >= 21)
						mAdvisoryCountImageVert = getResources().getDrawable(R.drawable.advisory_count, getTheme());
					else
						mAdvisoryCountImageVert = getResources().getDrawable(R.drawable.advisory_count);
				}
				return mAdvisoryCountImageVert;
			case Configuration.ORIENTATION_LANDSCAPE:
				if(mAdvisoryCountImageHorz == null)
				{
					if (Build.VERSION.SDK_INT >= 21)
						mAdvisoryCountImageHorz = getResources().getDrawable(R.drawable.advisory_count, getTheme());
					else
						mAdvisoryCountImageHorz = getResources().getDrawable(R.drawable.advisory_count);
				}
				return mAdvisoryCountImageHorz;
		}

		return null;
	}



	private void InflateTabs()
	{
		boolean generate = false;
		switch (getResources().getConfiguration().orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				if(mVertTabs == null) {
					mVertTabs = (TabLayout) findViewById(R.id.tab_bar_vert);
					generate = true;
				}
				if(mHorzTabs != null)
					mHorzTabs.setVisibility(View.GONE);
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				if(mHorzTabs == null) {
					mHorzTabs = (TabLayout) findViewById(R.id.tab_bar_horz);
					generate = true;
				}
				if(mVertTabs != null)
					mVertTabs.setVisibility(View.GONE);
				break;
		}

		if(mViewPager == null)
			mViewPager = (ViewPager) findViewById(R.id.pager);

		if (mAdapter == null) {
			mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
				@Override
				public Fragment getItem(int position) {
					switch (position) {
						case 0:
							return mLocalFragment;
						case 1:
							return mMapsFragment;
						case 2:
							return mRadarFragment;
						case 3:
							return mAdvisoriesFragment;
						default:
							return null;
					}
				}

				@Override
				public int getCount() {
					return 4;
				}
			};

			mViewPager.setAdapter(mAdapter);
		}

		if(generate) {
			switch (getResources().getConfiguration().orientation) {
				case Configuration.ORIENTATION_PORTRAIT:
					MesonetActionBar.GenerateTabs(mVertTabs, mViewPager);
					break;
				case Configuration.ORIENTATION_LANDSCAPE:
					MesonetActionBar.GenerateTabs(mHorzTabs, mViewPager);
					break;
			}
		}

		mAdapter.notifyDataSetChanged();
	}



	public static int GetToolbarHeight()
	{
		int result = 0;

		switch (This().getResources().getConfiguration().orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				result = This().mToolbarSizeVert;
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				result = This().mToolbarSizeHorz;
				break;
		}

		return result;
	}



	public static void SetPage(int inPage)
	{
		This().mViewPager.setCurrentItem(inPage);
	}



	@Override
	public void onStart()
	{
//        if(sLastOrientation != -1 && sLastOrientation != getResources().getConfiguration().orientation)
//            onRestart();
		LocationManager.Connect();

		super.onStart();
	}
	
	
	
	@Override
	public void onStop()
	{
		super.onStop();
		LocationManager.Disconnect();
	}



    @Override
    public void onRestart()
    {
        super.onRestart();
        AnalyticsTracker.Send();
    }
	
	
	
	@Override
	public void onResume()
	{
		super.onResume();
		DataContainer.StartDownloads();
	}
	
	
	
	@Override
	public void onPause()
	{
		DataContainer.StopDownloads();
		super.onPause();
	}



	@Override
	public void onDestroy()
	{
		DataContainer.CleanUp();
		super.onDestroy();
	}
	
	
	
	@Override
	public void onBackPressed()
	{
		String tag = MesonetActionBar.GetSelectedTab(mVertTabs);

		switch (tag) {
			case MesonetActionBar.kLocalTag:
				if (!LocalFragment.BackToCurrCond())
					HomeScreen();
				break;
			case MesonetActionBar.kMapsTag:
				if (!MapsFragment.BackToList())
					HomeScreen();
				break;
			case MesonetActionBar.kRadarTag:
				if (!RadarFragment.TurnOffTransparencyLayout())
					HomeScreen();
				break;
			case MesonetActionBar.kAdvisoriesTag:
				if (!AdvisoriesFragment.BackToList())
					HomeScreen();
				break;
			default:
				HomeScreen();
				break;
		}
	}



	@Override
    public void onTrimMemory(int inLevel)
    {
		String tab = MesonetActionBar.GetSelectedTab(mVertTabs);

		if(inLevel == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || !tab.equals("Radar"))
            RadarData.ClearImages();
    }



	public static void SetLocalFragment(LocalFragment inLocFrag)
	{
		This().mLocalFragment = inLocFrag;
	}



    public static void HomeScreen()
    {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        This().startActivity(startMain);
    }
	
	
	
	private static MainActivity This()
	{
		return MesonetApp.Activity();
	}
	
	
	
	public static LocalFragment LocalFragment()
	{
		return This().mLocalFragment;
	}
	
	
	
	public static MapsFragment MapsFragment()
	{
		return This().mMapsFragment;
	}
	
	
	
	public static RadarFragment RadarFragment()
	{
		return This().mRadarFragment;
	}
	
	
	
	public static AdvisoriesFragment AdvisoriesFragment()
	{
		return This().mAdvisoriesFragment;
	}



    public static FragmentManager FragmentManager()
    {
        return This().getSupportFragmentManager();
    }



	public static void ShareMap(View inView)
	{
		MapsFragment().ShareMap();
	}



	public static void OpenWebPage(View inView)
	{
		OpenApp(Intent.ACTION_VIEW, SavedDataManager.GetUrl(R.string.contact_web_address));
	}
	
	
	
	public static void OpenEmail(View inView)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("plain/text");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] {SavedDataManager.GetStringResource(R.string.contact_email_button)});
		This().startActivity(intent);
	}



	public int GetActionBarSize()
	{
		int toolbarSize;
		TypedValue typedValue = new TypedValue();
		getTheme().resolveAttribute(android.R.attr.textAppearanceLarge, typedValue, true);
		int[] textSizeAttr = new int[] { android.R.attr.actionBarSize };
		int indexOfAttrTextSize = 0;
		TypedArray a = obtainStyledAttributes(typedValue.data, textSizeAttr);
		toolbarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
		a.recycle();

		return toolbarSize;
	}


	public static void SelectMenuItem(MenuItem inItem)
	{
		if(inItem.getTitle().toString().equals(SavedDataManager.GetStringResource(R.string.settings_english)))
		{
			SavedDataManager.SaveStringSetting(DataContainer.kUnits, DataContainer.UnitSystem.kImperial.toString());
			LocalFragment.ClearAllFields();
			LocalFragment.SetTextsAndImages();
		}
		else if(inItem.getTitle().toString().equals(SavedDataManager.GetStringResource(R.string.settings_metric)))
		{
			SavedDataManager.SaveStringSetting(DataContainer.kUnits, DataContainer.UnitSystem.kMetric.toString());
			LocalFragment.ClearAllFields();
			LocalFragment.SetTextsAndImages();
		}
		else if(inItem.getTitle().toString().equals(SavedDataManager.GetStringResource(R.string.settings_radar_trans)))
		{
			RadarFragment.ShowTransparencyTracker();
		}
		else if(inItem.getTitle().toString().equals(SavedDataManager.GetStringResource(R.string.settings_ticker)))
		{
			LayoutInflater inflater = MesonetApp.Activity().getLayoutInflater();
			View popupView = inflater.inflate(R.layout.ticker_about_changes_layout, null);

			WebView tickerText = ((WebView)popupView.findViewById(R.id.popup_view));
			tickerText.getSettings().setUserAgentString(MesonetApp.UserAgentString());
			tickerText.loadUrl(SavedDataManager.GetUrl(R.string.ticker_url));

			PopupManager.Popup(popupView, R.string.popup_ticker_title);
		}
		else if(inItem.getTitle().toString().compareTo(SavedDataManager.GetStringResource(R.string.settings_contact)) == 0)
		{
			AlertDialog popup = PopupManager.Popup(R.layout.contact_layout, R.string.popup_contact_title);

			WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			lp.copyFrom(popup.getWindow().getAttributes());

			lp.width = Math.round(SavedDataManager.GetDimenResource(R.dimen.contact_popup_width));
			lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

			popup.getWindow().setAttributes(lp);
		}
		else if(inItem.getTitle().toString().compareTo(SavedDataManager.GetStringResource(R.string.settings_about)) == 0)
		{
			LayoutInflater inflater = MesonetApp.Activity().getLayoutInflater();
			View popupView = inflater.inflate(R.layout.ticker_about_changes_layout, null);

			String text = "";
			Scanner scanner = new Scanner(SavedDataManager.GetRawResource(R.raw.meso_about));

			String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
			String appVersion;
			try
			{
				appVersion = MesonetApp.Activity().getPackageManager().getPackageInfo(MesonetApp.Activity().getPackageName(), 0).versionName;
			}
			catch (Exception exception)
			{
				exception.printStackTrace();
				appVersion = "??";
			}

			while(scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				line = line.replace("%year;", year);
				line = line.replace("%version;", appVersion);
				text += line;
			}

			scanner.close();

			((WebView)popupView.findViewById(R.id.popup_view)).loadData(text, "text/html", "utf-8");

			PopupManager.Popup(popupView, R.string.settings_about);
		}
	}
	
	
	
	public static void OpenTwitter(View inView)
	{
		OpenAppOrPage(Intent.ACTION_VIEW, "twitter://user?screen_name=" + SavedDataManager.GetStringResource(R.string.contact_twitter_page), Intent.ACTION_VIEW, "https://twitter.com/" + SavedDataManager.GetStringResource(R.string.contact_twitter_page));
	}
	
	
	
	public static void OpenFacebook(View inView)
    {
		OpenAppOrPage(Intent.ACTION_VIEW, "fb://page/" + SavedDataManager.GetStringResource(R.string.contact_facebook_userid), Intent.ACTION_VIEW, SavedDataManager.GetStringResource(R.string.contact_facebook_url));
	}
	
	
	
	public static void OpenPhone(View inView)
	{
		OpenApp(Intent.ACTION_DIAL, "tel:" + SavedDataManager.GetStringResource(R.string.contact_phone_number));
	}

	
	
	public static void OpenMaps(View inView)
	{
		OpenApp(Intent.ACTION_VIEW, "geo:0,0?q=" + SavedDataManager.GetStringResource(R.string.contact_address));
	}



    private static void OpenAppOrPage(String inIntent1, String inUri1, String inIntent2, String inUri2)
    {
        try
        {
            OpenApp(inIntent1, inUri1);
        }
        catch(ActivityNotFoundException exception)
        {
            OpenApp(inIntent2, inUri2);
        }
    }



    private static void OpenApp(String inIntent, String inUri)
    {
        if(This() != null)
            This().startActivity(new Intent(inIntent, Uri.parse(inUri)));
    }
}



package org.mesonet.app;

import android.content.ActivityNotFoundException;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;



public class MainActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private MesonetActionBar mActionBar;
    private MainMenu mMenu;

    private static boolean sFirstStartup = true;

    private static int sLastOrientation = -1;

	
	
	@Override
	protected void onCreate(Bundle inSavedInstanceState)
	{
        super.onCreate(inSavedInstanceState);


        mActionBar = new MesonetActionBar();
        mMenu = new MainMenu();
		
		MesonetApp.SetActivity(this);
        MapsData.Initialize();
		
		PopupManager.ShowChanges();
		
		MesonetActionBar.GenerateTabs();

        LocationManager.Init();

        if(sFirstStartup) {
            AnalyticsTracker.Send();
            sFirstStartup = false;
        }
	}
	
	
	
	@Override
	public void onStart()
	{
        if(sLastOrientation != -1 && sLastOrientation != getResources().getConfiguration().orientation)
            onRestart();

        sLastOrientation = getResources().getConfiguration().orientation;

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
		sLastOrientation = getResources().getConfiguration().orientation;
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
	public boolean onCreateOptionsMenu(Menu inMenu)
	{
        MainMenu.GenerateMenu(inMenu);
		
		return true;
	}
	
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem inItem)
	{
		MainMenu.SelectMenuItem(inItem);

		return true;
	}
	
	
	
	@Override
	public void onBackPressed()
	{
		String tag = (String) getSupportActionBar().getSelectedTab().getTag();
		
		if (tag.equals(MesonetActionBar.kLocalTag))
		{
			if(!LocalFragment.BackToCurrCond())
				HomeScreen();
		}
		else if (tag.equals(MesonetActionBar.kMapsTag))
		{
			if (!MapsFragment.BackToList())
                HomeScreen();
		}
        else if(tag.equals(MesonetActionBar.kRadarTag))
        {
            if(!RadarFragment.TurnOffTransparencyLayout())
                HomeScreen();
        }
		else if(tag.equals(MesonetActionBar.kAdvisoriesTag))
		{
			if(!AdvisoriesFragment.BackToList())
                HomeScreen();
		}
		else
            HomeScreen();
	}
	
	
	
	@Override
	public void onLocationChanged(Location inLocation)
	{
        LocationManager.SetLocation(inLocation);
	}
	
	
    
	@Override
	public void onConnected(Bundle inDataBundle)
	{
        LocationManager.sConnected = true;
        LocationManager.StartUpdating();
	}

	@Override
	public void onConnectionSuspended(int i) {

	}


	@Override
    public void onTrimMemory(int inLevel)
    {
        if(inLevel == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || MesonetActionBar.GetSelectedTab().compareTo("Radar") != 0)
            RadarData.ClearImages();
    }


	@Override public void onConnectionFailed(ConnectionResult connectionResult){}



    public static void HomeScreen()
    {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        This().startActivity(startMain);
    }



    public static MainMenu Menu()
    {
        return This().mMenu;
    }
	
	
	
	private static MainActivity This()
	{
		return MesonetApp.Activity();
	}
	
	
	
	public static LocalFragment LocalFragment()
	{
		return (LocalFragment)FragmentManager().findFragmentByTag(MesonetActionBar.kLocalTag);
	}
	
	
	
	public static MapsFragment MapsFragment()
	{
		return (MapsFragment)FragmentManager().findFragmentByTag(MesonetActionBar.kMapsTag);
	}
	
	
	
	public static RadarFragment RadarFragment()
	{
		return (RadarFragment)FragmentManager().findFragmentByTag(MesonetActionBar.kRadarTag);
	}
	
	
	
	public static AdvisoriesFragment AdvisoriesFragment()
	{
		return (AdvisoriesFragment)FragmentManager().findFragmentByTag(MesonetActionBar.kAdvisoriesTag);
	}



    public static FragmentManager FragmentManager()
    {
        return This().getSupportFragmentManager();
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



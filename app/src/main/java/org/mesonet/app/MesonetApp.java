package org.mesonet.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;



public class MesonetApp extends Application
{
    public static final String kDegree = "\u00b0";
    public static final String kDegreeC = kDegree + "C";
    public static final String kDegreeF = kDegree + "F";

	private static MainActivity sActivity = null;
	private static String sUserAgent = null;
    private static Context sContext = null;
    private static MesonetApp sApplication = null;

    private static AnalyticsTracker sTracker = new AnalyticsTracker();



	@Override
	public void onCreate()
	{
		super.onCreate();

        sApplication = this;

        // Create the user agent string so we can ID our app in web stats
        // This code is from the Android project source code for creating the default user agent
        // string.  The only difference is that instead of indicating DalvikVM for the browser
        // type, it uses our app name and version.
        sContext = getApplicationContext();
        StringBuilder result = new StringBuilder(64);
        String packageName = getPackageName();
        String appName = "MesonetApp";
        String appVersion;
        try
        {
            appVersion = getPackageManager().getPackageInfo(packageName, 0).versionName;
        }
        catch (Exception exception)
        {
            appVersion = "??";
            exception.printStackTrace();
        }
        result.append(appName);
        result.append("/");
        result.append(appVersion);
        result.append(" (Linux; U; Android ");

        String version = Build.VERSION.RELEASE; // "1.0" or "3.4b5"
        result.append(version.length() > 0 ? version : "1.0");

        // add the model for the release build
        if ("REL".equals(Build.VERSION.CODENAME))
        {
            String model = Build.MODEL;
            if (model.length() > 0)
            {
                result.append("; ");
                result.append(model);
            }
        }
        String id = Build.ID;
        if (id.length() > 0)
        {
            result.append(" Build/");
            result.append(id);
        }
        result.append(")");
        sUserAgent = result.toString();

        System.setProperty("http.agent", UserAgentString());
	}



	public static void SetActivity(MainActivity inActivity)
	{
        sActivity = inActivity;
	}
	
	
	
	public static String UserAgentString()
	{
		return sUserAgent;
	}



    public static MesonetApp Application() {return sApplication; }
	
	
	
	public static MainActivity Activity()
	{
		return sActivity;
	}



    public static Context Context()
    {
        return sContext;
    }



    public static AnalyticsTracker Tracker()
    {
        return sTracker;
    }
}

package org.mesonet.app;

import java.util.Date;
import java.util.Formatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.location.Location;



public class LocalData
{
	private static int sSiteIndex = -1;

	private static ScheduledExecutorService sPool = null;

    private static Date sLastUpdate = null;
	
	
	
	public static void Initialize()
	{
        CurrentConditions.Initialize();
        Forecast.Initialize();
    }
	
	
	
	public static void StartDownload(boolean inForce)
	{
        if(inForce || sPool == null) {
            LocalFragment.ClearCurrCondFields();
            LocalFragment.ClearForecastFields();

            StopDownload();

            long nextDlTime = 0;
            Date now = new Date();
            if (!inForce && sLastUpdate != null && (now.getTime() - sLastUpdate.getTime()) < DataContainer.kOneMinute) {
                nextDlTime = now.getTime() - sLastUpdate.getTime();
                LocalFragment.SetTextsAndImages();
            }

            sPool = Executors.newScheduledThreadPool(1);
            sPool.scheduleAtFixedRate(new SiteDataUpdater(), nextDlTime, DataContainer.kOneMinute, TimeUnit.MILLISECONDS);
        }
	}
	
	
	
	public static void StopDownload()
	{
		if(sPool != null)
			sPool.shutdown();

        sPool = null;
	}



    public static void ClearData()
    {

    }



    public static void ResetDownloads()
    {
        CurrentConditions.ResetDownloads();
        Forecast.ResetDownloads();
    }
	
	
	
	//public static boolean MeteoIsVisible()
	//{
	//	return sMeteoIsVisible;
	//}
	
	
	
	//public static void SetMeteoIsVisible(boolean inVisible)
	//{
		//sMeteoIsVisible = inVisible;
	//}

	
	
	public static void SiteChanged(int inPosition)
	{
		if(sSiteIndex != inPosition)
		{
            StopDownload();
            ResetDownloads();

			sSiteIndex = inPosition;
			
			LocalFragment.ClearAllFields();

            CurrentConditions.SetMeteoPage();
			SavedDataManager.SaveStringSetting("stid", SiteData.Stid());

            StartDownload(true);
		}
	}

	
	
	/*public static void SetStid(String inStid)
	{
        sForceUpdate = true;
        SiteData.SetStid(inStid);
	}*/
	
	
	
//	public static int SiteIndex()
//	{
//		return SiteData.SiteIndex();
//	}
	
	
	
	/*public static String SetToNearestSite(Location inLocation)
	{
		return SiteData.SetToNearestSite(inLocation);
	}*/
	
	
	
	public static void PullData()
	{
        CurrentConditions.Download(true);
        Forecast.Download(true, SiteData.Stid());

        sLastUpdate = new Date();
	}



	private static class SiteDataUpdater implements Runnable
	{
		@Override
		public void run()
		{
			PullData();
		}
	}
}

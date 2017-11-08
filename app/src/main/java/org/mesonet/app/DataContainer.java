package org.mesonet.app;


import android.app.Activity;

public class DataContainer
{
	public enum UnitSystem {kImperial, kMetric}

    public static final int kOneMinute = 60000;
	public static final String kUnits = "units";



    public static void StartDownloads()
	{
		AdvisoryData.StartDownload();
	}
	
	
	
	public static void StopDownloads()
	{
		LocalData.StopDownload();
		RadarData.StopDownload();
		AdvisoryData.StopDownload();
	}



	public static void CleanUp()
	{
		LocalData.ClearData();
		MapsData.CleanUp();
		RadarData.ClearImages();
		AdvisoryData.CleanUp();
	}



	public static UnitSystem GetUnitSystem()
	{
		UnitSystem result = UnitSystem.kImperial;
		try {
			result = UnitSystem.valueOf(MesonetApp.Context().getSharedPreferences("MainActivity", Activity.MODE_PRIVATE).getString(kUnits, UnitSystem.kImperial.toString()));
		}
		catch(Exception exception)
		{
			try {
				result = UnitSystem.values()[MesonetApp.Context().getSharedPreferences("MainActivity", Activity.MODE_PRIVATE).getInt(kUnits, UnitSystem.kImperial.ordinal())];
			}
			catch (Exception exception2)
			{
				exception2.printStackTrace();;
			}
		}
		return result;
	}
}

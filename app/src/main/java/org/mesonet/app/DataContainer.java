package org.mesonet.app;



public class DataContainer
{
    public static final int kOneMinute = 60000;



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
}

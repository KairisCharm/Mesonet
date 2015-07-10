package org.mesonet.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;


public class Forecast
{
	public static final int kTime = 0;
	public static final int kImageUrl = 1;
	public static final int kCondition = 2;
	public static final int kLowOrHigh = 3;
	public static final int kLowOrHighTemp = 4;
	public static final int kWindDir1 = 5;
	public static final int kWindDir2 = 6;
	public static final int kWindMag1 = 7;
	public static final int kWindMag2 = 8;

	public static final int kDataEntriesCount = 10;
	
	private static ForecastData[] sData = new ForecastData[kDataEntriesCount];

    public static ForecastDownloader sForecastDownloader = new ForecastDownloader();

    private static boolean sInitialized = false;

    private static boolean sWaitingForCompletion = true;
	
	
	
	public synchronized static void Initialize()
	{
        if(!sInitialized) {
            for (int i = 0; i < kDataEntriesCount; i++) {
                if (sData[i] == null)
                    sData[i] = new ForecastData();
            }

            sInitialized = true;
        }
	}



    public static void Download(boolean inForceUpdate, String inStid)
    {
        sForecastDownloader.Update(inForceUpdate, inStid);
    }



    public static void ClearData()
    {
        if(sData == null)
            return;

        for(int i = 0; i < kDataEntriesCount; i++) {
            if (sData[i] != null)
                sData[i].ClearData();
        }
    }



    public static boolean WaitingForCompletion()
    {
        return sWaitingForCompletion;
    }



    public static void ResetDownloads()
    {
        sForecastDownloader.Cancel();
    }
	
	
	
	public static void SetData(String inData, WidgetProvider inProvider)
	{
		new DataSetter().execute(new DataSetter.InputData(inData, inProvider));
	}
	
	
	
	public synchronized static ForecastData GetData(int inIndex)
	{
		return sData[inIndex];
	}



    private static class DataSetter extends AsyncTask<DataSetter.InputData, Object, WidgetProvider>
    {
        @Override
        protected WidgetProvider doInBackground(InputData... inData)
        {
            sWaitingForCompletion = true;
            try {
                String[] splitData = inData[0].mData.split("\n");

                for (int i = 0; i < kDataEntriesCount; i++) {
                    String[] subData = splitData[i].split(",");
                    String[] splitUrl = subData[kImageUrl].split("/");

                    sData[i].mTime = subData[kTime].toUpperCase(Locale.ENGLISH);
                    sData[i].mCondition = subData[kCondition];

                    sData[i].mWindDir1 = subData[kWindDir1];
                    sData[i].mWindDir2 = subData[kWindDir2];
                    sData[i].mWindMag1 = subData[kWindMag1];
                    sData[i].mWindMag2 = subData[kWindMag2];
                    sData[i].mLowOrHigh = subData[kLowOrHigh];
                    sData[i].mLowOrHighTemp = Double.toString(Conversion.FahrenheitToCelsius(Double.parseDouble(subData[kLowOrHighTemp])));

                    if (sData[i].mBitmapName == null || sData[i].mBitmapName.compareTo(splitUrl[splitUrl.length - 1]) != 0) {
                        boolean foundImage = false;
                        for (int j = 0; j < kDataEntriesCount; j++) {
                            if (sData[j].mBitmapName != null && sData[j].mBitmapName.compareTo(splitUrl[splitUrl.length - 1]) == 0) {
                                sData[i].mImage = sData[j].mImage;
                                foundImage = true;
                            }
                        }

                        if (!foundImage) {
                            File cachedImage = new File(SavedDataManager.CacheDir().getPath() + splitUrl[splitUrl.length - 1]);

                            if (cachedImage.exists())
                                sData[i].mImage = BitmapFactory.decodeFile(cachedImage.getAbsolutePath());

                            else {
                                String downloadFilename = subData[kImageUrl].replaceAll(SavedDataManager.GetUrl(R.string.forecast_replace_image_url), SavedDataManager.GetUrl(R.string.forecast_image_url)).replaceAll(".jpg", SavedDataManager.GetStringResource(R.string.resolution_change));
                                try {
                                    InputStream is = (InputStream) new URL(downloadFilename).getContent();
                                    sData[i].mImage = BitmapFactory.decodeStream(is);

                                    FileOutputStream out = new FileOutputStream(cachedImage);
                                    sData[i].mImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                                    out.flush();
                                    out.close();
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                            }
                        }

                        sData[i].mBitmapName = splitUrl[splitUrl.length - 1];
                    }
                }

                return inData[0].mProvider;
            }
            catch(Exception exception)
            {
                Forecast.ClearData();
                exception.printStackTrace();
            }

            return null;
        }



        @Override
        protected void onPostExecute(WidgetProvider inWidget)
        {
            sWaitingForCompletion = false;

            if(MesonetApp.Activity() != null && MainActivity.LocalFragment() != null)
                LocalFragment.SetTextsAndImages();
            if(inWidget != null)
                inWidget.FinishedDownload();
        }



        protected static class InputData
        {
            public String mData;
            public WidgetProvider mProvider;



            public InputData(String inData, WidgetProvider inProvider) {
                mData = inData;
                mProvider = inProvider;
            }
        }
    }



    public static class ForecastDownloader extends DataDownloader
    {
        public void Update(boolean inForceUpdate, String inStid)
        {
            Update(SavedDataManager.GetUrl(R.string.forecast_url) + "/" + inStid, null, inForceUpdate, false);
        }



        @Override
        protected void PostExecute(DownloadTask.ResultParms inResult)
        {
            SetData(inResult.mData, inResult.mWidget);
        }
    }


	
	public static class ForecastData
	{
		String mTime;
		Bitmap mImage;
		String mBitmapName;
		String mCondition;
		String mWindDir1;
		String mWindDir2;
		String mWindMag1;
		String mWindMag2;
		String mLowOrHigh;
		String mLowOrHighTemp;
		
		
		
		public synchronized String GetTemp()
		{
			if(mLowOrHighTemp == null)
				return "";
			
			if(mLowOrHighTemp.compareTo(SavedDataManager.MissingField()) == 0)
				return mLowOrHighTemp;

			return Conversion.CreateDataString(new Conversion.CelsToFahrConverter(Float.parseFloat(mLowOrHighTemp)), MesonetApp.kDegreeF, MesonetApp.kDegreeC, 0, false);
		}



        public synchronized void ClearData()
        {
            mTime = "-";
            mImage = null;
            mBitmapName = null;
            mCondition = "-";
            mWindDir1 = "-";
            mWindDir2 = "-";
            mWindMag1 = "-";
            mWindMag2 = "-";
            mLowOrHigh = "-";
            mLowOrHighTemp = "-";
        }
	}
}
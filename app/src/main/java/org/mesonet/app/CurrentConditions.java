package org.mesonet.app;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import android.content.res.Resources;
import android.os.AsyncTask;


public class CurrentConditions
{
	public static final float kInvalidAirTemp = -999.0f;
	
	private static float sAirTemp = kInvalidAirTemp;
	private static float sFeelsLike = Float.NaN;
	private static float sDewpoint = Float.NaN;
	private static float sRain24Hr = Float.NaN;
	private static Vector sWind = new Vector(Float.NaN, Float.NaN);
	private static float sHumidity = Float.NaN;
	private static float sGusts = Float.NaN;
	private static float sPressure = Float.NaN;
	private static GregorianCalendar sLastUpdated = null;

    public static CurrCondDownloader sCurrCondDownloader = new CurrCondDownloader();

    private static String sMeteoUrl = null;



    public static void Initialize(){}



    public static void Download(boolean inForceUpdate)
    {
        sCurrCondDownloader.Update(inForceUpdate);
    }



    public static void ClearData()
    {
        sAirTemp = kInvalidAirTemp;
        sFeelsLike = Float.NaN;
        sDewpoint = Float.NaN;
        sRain24Hr = Float.NaN;
        sWind = new Vector(Float.NaN, Float.NaN);
        sHumidity = Float.NaN;
        sGusts = Float.NaN;
        sPressure = Float.NaN;
        sLastUpdated = null;
    }



    public static void ResetDownloads()
    {
        sCurrCondDownloader.Cancel();
    }

	
	
	public static String ExtractTextField(String allData, String field)
	{
		if(allData != null && field != null)
		{
			String[] columns = allData.split(",");
			String[] components;
			
			if(columns.length > 1)
			{
				for (String column : columns)
				{
					components = column.split("=");
					if(components[0].compareTo(field) == 0)
						return components[1];
				}
			}
		}
		
		return "";
	}
	
	
	
	public static float Validate(float inData)
	{
		return Conversion.Valid(inData) ? inData : Float.NaN;
	}
	
	
	
	public synchronized static String AirTemp()
	{
		return Conversion.CreateDataString(new Conversion.CelsToFahrConverter(sAirTemp), MesonetApp.kDegree, MesonetApp.kDegree, 0, false);
	}
	
	
	
	public synchronized static String FeelsLike()
	{
		return Conversion.CreateDataString(new Conversion.CelsToFahrConverter(sFeelsLike), MesonetApp.kDegreeF, MesonetApp.kDegreeC, 0, false);
	}
	
	
	
	public synchronized static String Dewpoint()
	{
		return Conversion.CreateDataString(new Conversion.CelsToFahrConverter(sDewpoint), MesonetApp.kDegreeF, MesonetApp.kDegreeC, 0, false);
	}
	
	
	
	public synchronized static String Rain24Hr()
	{
		return TryToFormat(Conversion.CreateDataString(new Conversion.MMToInConverter(sRain24Hr), SavedDataManager.GetStringResource(R.string.rain_inches), SavedDataManager.GetStringResource(R.string.rain_millimeters), 2, true), "0.00");
	}
	
	
	
	public synchronized static String Wind()
	{
        if(sWind.mMagnitude <= 3.0f)
            return SavedDataManager.GetStringResource(R.string.wind_calm);

		String magnitude = Conversion.CreateDataString(new Conversion.MpsToMphConverter(sWind.mMagnitude), SavedDataManager.GetStringResource(R.string.wind_mph), SavedDataManager.GetStringResource(R.string.wind_mps), 0, false);

		String direction = "";
		if (Conversion.Valid(sWind.mDirection))
			direction = Vector.CalcDirectionString(sWind.mDirection, SavedDataManager.Resources()) + SavedDataManager.GetStringResource(R.string.wind_at);
		
		return direction + magnitude;
	}
	
	
	
	public synchronized static String Humidity()
	{
		if (!Conversion.Valid(sHumidity))
			return SavedDataManager.MissingField();

		return Integer.toString(Math.round(sHumidity)) + "%";
	}
	
	
	
	public synchronized static String Gusts()
	{
		return Conversion.CreateDataString(new Conversion.MpsToMphConverter(sGusts), SavedDataManager.GetStringResource(R.string.wind_mph), SavedDataManager.GetStringResource(R.string.wind_mps), 0, false);
	}
	
	
	
	public synchronized static String Pressure()
	{		
		return TryToFormat(Conversion.CreateDataString(new Conversion.MBToInConverter(sPressure), SavedDataManager.GetStringResource(R.string.pressure_inches), SavedDataManager.GetStringResource(R.string.pressure_millimeters), 2, true), "00.00");
	}



    public synchronized static String Time()
    {
        if(sLastUpdated == null)
            return SavedDataManager.MissingField() + ":" + SavedDataManager.MissingField();

        SimpleDateFormat format = new SimpleDateFormat("h:mm a", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("US/Central"));

        return format.format(sLastUpdated.getTime());
    }
	
	
	
	public static String TryToFormat(String inValue, String inFormat)
	{
		DecimalFormat decFormat = new DecimalFormat(inFormat);
		String[] splitString = inValue.split(" ");

        float value;
		
		try
		{
			value = Float.parseFloat(splitString[0]);
		}
		catch(Exception ex)
		{
			return inValue;
		}
		
		String result = decFormat.format(value);
		
		for(int i = 1; i < splitString.length; i++)
			result += " " + splitString[i];
		
		return result;
	}
	
	

    // WidgetProvider field only necessary when being called from a widget
	public static void SetFields(String inResult, WidgetProvider inProvider)
	{
        new DataSetter().execute(new DataSetter.InputData(inResult, inProvider));
	}



    public static void SetMeteoPage()
    {
        Formatter formatter = new Formatter();
        formatter.format(SavedDataManager.GetUrl(R.string.meteogram_url), SiteData.Stid());

        sMeteoUrl = formatter.toString();

        formatter.close();
    }



    public static String MeteoUrl()
    {
        return sMeteoUrl;
    }
	
	

	private static class Vector
	{
		private float mDirection;
		private float mMagnitude;
		
		
		
		public Vector(float inDir, float inMag)
		{
			mDirection = inDir;
			mMagnitude = inMag;
		}
		
		
		
		public static String CalcDirectionString(float inDirection, Resources inResources)
		{
			if(inDirection < 0.0f || inDirection > 360.0f)
				return "";
			if(inDirection < 11.25f || inDirection >= 348.75)
				return inResources.getString(R.string.wind_north);
			if(inDirection < 33.75)
				return inResources.getString(R.string.wind_north_northeast);
			if(inDirection < 56.25)
				return inResources.getString(R.string.wind_northeast);
			if(inDirection < 78.75)
				return inResources.getString(R.string.wind_east_northeast);
			if(inDirection < 101.25)
				return inResources.getString(R.string.wind_east_southeast);
			if(inDirection < 123.75)
				return inResources.getString(R.string.wind_southeast);
			if(inDirection < 146.25)
				return inResources.getString(R.string.wind_south_southeast);
			if(inDirection < 191.25)
				return inResources.getString(R.string.wind_south);
			if(inDirection < 213.75)
				return inResources.getString(R.string.wind_south_southwest);
			if(inDirection < 236.25)
				return inResources.getString(R.string.wind_southwest);
			if(inDirection < 258.75)
				return inResources.getString(R.string.wind_west_southwest);
			if(inDirection < 281.25)
				return inResources.getString(R.string.wind_west);
			if(inDirection < 303.75)
				return inResources.getString(R.string.wind_west_northwest);
			if(inDirection < 326.25)
				return inResources.getString(R.string.wind_northwest);
			if(inDirection < 348.75)
				return inResources.getString(R.string.wind_north_northwest);
			
			return "-";
		}
	}



    private static class DataSetter extends AsyncTask<DataSetter.InputData, Object, WidgetProvider>
    {
        @Override
        protected WidgetProvider doInBackground(InputData... inData)
        {
            if(inData[0] == null || inData[0].mData.compareTo("") == 0)
                return null;

            try {
                sAirTemp = Validate(Float.parseFloat(ExtractTextField(inData[0].mData, "TAIR")));
                double fahrenheitAirTemp = Conversion.CelsiusToFahrenheit(sAirTemp);

                sWind.mDirection = Validate(Float.parseFloat(ExtractTextField(inData[0].mData, "WDIR")));
                sWind.mMagnitude = Validate(Float.parseFloat(ExtractTextField(inData[0].mData, "WSPD")));

                sHumidity = Validate(Float.parseFloat(ExtractTextField(inData[0].mData, "RELH")));
                sDewpoint = Validate((float) Conversion.Dewpoint(sAirTemp, sHumidity));

                sGusts = Validate(Float.parseFloat(ExtractTextField(inData[0].mData, "WMAX")));
                sPressure = Validate(Float.parseFloat(ExtractTextField(inData[0].mData, "PRES")));

                sFeelsLike = Validate((float) Conversion.FahrenheitToCelsius(Conversion.ApparentTemperature(fahrenheitAirTemp, sHumidity, Conversion.MetersPerSecondToMilesPerHour(sWind.mMagnitude))));

                sRain24Hr = Validate(Float.parseFloat(ExtractTextField(inData[0].mData, "RAIN_24H")));

                int month = Integer.parseInt(ExtractTextField(inData[0].mData, "MONTH")) - 1;
                int day = Integer.parseInt(ExtractTextField(inData[0].mData, "DAY"));
                int year = Integer.parseInt(ExtractTextField(inData[0].mData, "YEAR"));
                int hour = Integer.parseInt(ExtractTextField(inData[0].mData, "HOUR"));
                int minute = Integer.parseInt(ExtractTextField(inData[0].mData, "MINUTE"));

                if (sLastUpdated == null) {
                    sLastUpdated = new GregorianCalendar();
                    sLastUpdated.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
                }

                if (month < Calendar.JANUARY || month > Calendar.DECEMBER)
                    month = Calendar.JANUARY;

                sLastUpdated.set(year, month, day, hour, minute);

                return inData[0].mProvider;
            }
            catch(Exception exception)
            {
                CurrentConditions.ClearData();
                exception.printStackTrace();
            }

            return null;
        }



        @Override
        protected void onPostExecute(WidgetProvider inWidget)
        {
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



    public static class CurrCondDownloader extends DataDownloader
    {
        public void Update(boolean inForceUpdate)
        {
            Update(SavedDataManager.GetUrl(R.string.curr_cond_url) + "/" + SiteData.Stid(), null, inForceUpdate, false);
        }



        @Override
        protected void PostExecute(DownloadTask.ResultParms inResult)
        {
            CurrentConditions.SetFields(inResult.mData, inResult.mWidget);
        }
    }
}
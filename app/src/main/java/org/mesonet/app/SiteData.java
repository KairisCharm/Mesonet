package org.mesonet.app;

import java.util.Date;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationManager;



public class SiteData
{
	private static SQLiteDatabase sDatabase = null;	
	private static Cursor sSitesList = null;
	private static SiteListDbHelper sDbHelper = null;

    public static SiteListDownloader sSiteListDownloader = null;

    public static String sOldStid = null;

    public static boolean sInitialized = false;
	
	
	
	public static void Initialize()
	{
        if(!sInitialized) {
            sDbHelper = new SiteListDbHelper(MesonetApp.Context());
            sDatabase = sDbHelper.getWritableDatabase();
            sSiteListDownloader = new SiteListDownloader(LastUpdate());

            //SiteListQuery();

            Download();
        }
	}



    public static void Download()
    {
        sSiteListDownloader.Update(SavedDataManager.GetUrlFile(R.string.site_url, R.string.site_filename), SavedDataManager.GetFile(R.string.site_filename), true, false);
    }
	
	
	
	public static void UpdateList()
	{
		if(MesonetApp.Activity() != null)
		{
			MesonetApp.Activity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					SiteListQuery();
					
					LocalFragment.UpdateList();
				}
			});
		}
	}
	
	
	
	public static Date LastUpdate()
	{
		try
		{
			long time = 0;
			Cursor results = sDatabase.query(SiteListDbHelper.kModTimeTable, new String[]{"time"}, "file=?", new String[]{"sitedata"}, null, null, null);
			if (results.getCount() > 0) 
			{
				results.moveToFirst();
				time = results.getLong(0);
			}
			results.close();

			return new Date(time);
		}
		catch(Exception exception)
		{
			exception.printStackTrace();
		}

        return null;
	}
	
	
	
	public static void SqlUpdate (String inJson, Date inDate)
	{
		try
		{
			SiteListDbHelper.SqlUpdate(sDatabase, inJson, inDate, new Date());
		}
		catch(Exception exception)
		{
            exception.printStackTrace();
		}
	}
	
	
	
	public static void UpdateLastCheck ()
	{
		try
		{
			SiteListDbHelper.UpdateLastCheck(sDatabase, new Date());
		}
		catch(Exception exception)
		{
            exception.printStackTrace();
		}
	}
	
	
	
	public static Cursor SitesList()
	{
		return sSitesList;
	}



    private static void SiteListQuery()
    {
        int index = -1;

        if(sSitesList != null)
            index = sSitesList.getPosition();

        sSitesList = sDatabase.query(SiteListDbHelper.kSiteDataTable, new String[]{"_id", "stid", "name", "nlat", "elon","elev"}, "name NOT LIKE '%etired%'", null, null, null, "name");

        if(index != -1)
            sSitesList.moveToPosition(index);

        if(MesonetApp.Activity() != null && MainActivity.LocalFragment() != null)
            LocalFragment.SetList();
    }
	
	
	
	public static void CloseDatabase()
	{
		sDatabase.close();
		sSitesList.close();
		sDbHelper.close();
	}
	
	
	
	public static String SetToNearestSite(Location inCurrentLocation)
	{
		if(inCurrentLocation != null && sSitesList != null)
		{
			Location locationCompare = new Location(LocationManager.NETWORK_PROVIDER);
			
			int stidColIndex = sSitesList.getColumnIndex("stid");
			int latColIndex = sSitesList.getColumnIndex("nlat");
			int lonColIndex = sSitesList.getColumnIndex("elon");
			
			String nearestStid = null;
			int selectedIndex = -1;
			float smallestDistance = 9999999;
			
			sSitesList.moveToFirst();
			
			do
			{
				float lat = sSitesList.getFloat(latColIndex);
				float lon = sSitesList.getFloat(lonColIndex);
				
				locationCompare.setLatitude(lat);
				locationCompare.setLongitude(lon);
				
				float distanceCheck = inCurrentLocation.distanceTo(locationCompare);
				String stidCheck = sSitesList.getString(stidColIndex);
				
				if(distanceCheck < smallestDistance)
				{
					smallestDistance = distanceCheck;
					nearestStid = stidCheck;
					selectedIndex = sSitesList.getPosition();
				}
			} while(sSitesList.moveToNext());
			
			sSitesList.moveToPosition(selectedIndex);
			
			return nearestStid;
		}
		
		return null;
	}
	
	
	
	public static int SiteIndex()
	{
		if(sSitesList != null)
			return sSitesList.getPosition();
		
		return -1;
	}
	
	
	
	public static String GetName(String inId)
	{
		try
		{
			String name = null;
			Cursor results = sDatabase.query(SiteListDbHelper.kSiteDataTable, new String[]{"name"}, "stid=?", new String[]{inId}, null, null, null);
			if (results.getCount() > 0)
			{
				results.moveToFirst();
				name = results.getString(0);
			}
			results.close();

			return name;
		}
		catch(Exception exception)
		{
            exception.printStackTrace();
		}

        return null;
	}
	
	
	
	public static void SetStid(String inStid)
	{
		if(sSitesList != null)
		{
			sSitesList.moveToFirst();
			
			for(int i = 0; i < sSitesList.getCount(); i++)
			{
				if(sSitesList.getString(1).compareTo(inStid) == 0) {
                    sOldStid = Stid();

                    SavedDataManager.SaveStringSetting("stid", inStid);
                    SavedDataManager.SaveStringSetting("site_name", GetName(inStid));

                    CurrentConditions.SetMeteoPage();
                    LocalData.StartDownload(true);
                    return;
                }
				
				sSitesList.moveToNext();
			}
		}
	}



    public static boolean IsSameSite(String inStid)
    {
        return (sOldStid != null) && (inStid.compareTo(sOldStid) == 0);
    }
	
	
	
	public static String Stid()
	{
		if(sSitesList.isBeforeFirst() || sSitesList.isAfterLast())
			return "";
		
		return sSitesList.getString(1);
	}



    public static void SetPosition(int inPos)
    {
        sSitesList.moveToPosition(inPos);
    }
	
	
	
	private static class SiteListDbHelper extends SQLiteOpenHelper
	{
		private static final String kDbName = "sitedata.db";
		private static final int kDbVersion = 1;
		private static final String kSiteDataTable = "sitedata";
		private static final String kSiteDataCreate =
				"CREATE TABLE " + kSiteDataTable + " (" +
					"_id INTEGER PRIMARY KEY," +
					"stid TEXT," +
					"nlat REAL," +
					"elon REAL," +
					"elev REAL," +
					"name TEXT" +
				");";
		
		private static final String kModTimeTable = "modtime";
		private static final String kModTimeCreate =
				"CREATE TABLE " + kModTimeTable + " (" +
					"file TEXT PRIMARY KEY," +
					"time INTEGER," +
					"last_check INTEGER" +
				");";
		
		
		
		public SiteListDbHelper(Context inContext)
		{
			super(inContext, kDbName, null, kDbVersion);
		}
	
		
		
		@Override
		public void onCreate(SQLiteDatabase inDb) 
		{
            inDb.execSQL(kModTimeCreate);

            inDb.execSQL(kSiteDataCreate);

            Scanner scanner = new Scanner(SavedDataManager.GetRawResource(R.raw.siteinfo));
            String json = scanner.useDelimiter("\\A").next();

            if (json != null)
            {
                long modtime = Long.valueOf(SavedDataManager.GetStringResource(R.string.embedded_database_modtime));
                SqlUpdate(inDb, json, new Date(modtime), new Date(modtime));
            }

            scanner.close();
		}
		
		
		
		@Override
		public void onUpgrade(SQLiteDatabase inDb, int inOldVersion, int inNewVersion) 
		{
			// Still on version 1, so no upgrades to do
		}
		
		
		
		public static void SqlUpdate(SQLiteDatabase inDb, String inJson, Date inUpdated, Date inLastCheck)
		{
			try
			{
				JSONObject obj = (JSONObject) new JSONTokener(inJson).nextValue();
				JSONArray names = obj.names();
				
				inDb.beginTransaction();
				
				inDb.delete(kModTimeTable, "file=?", new String[]{"sitedata"});
				ContentValues cv = new ContentValues(3);
				cv.put("file", "sitedata");
				cv.put("time", inUpdated.getTime());
				cv.put("last_check", inLastCheck.getTime());
				inDb.insert(kModTimeTable, null, cv);

				inDb.delete(kSiteDataTable, null, null);
				for (int i = 0; i < names.length(); i++)
				{
					String id = (String)names.get(i);
					JSONObject thisObj = obj.getJSONObject(id);
					ContentValues cv1 = new ContentValues(5);
					cv1.put("_id", Integer.valueOf(thisObj.getString("stnm")));
					cv1.put("stid", id);
					cv1.put("nlat", Double.valueOf(thisObj.getString("lat")));
					cv1.put("elon", Double.valueOf(thisObj.getString("lon")));
					cv1.put("elev", Double.valueOf(thisObj.getString("elev")));
					cv1.put("name", thisObj.getString("name"));
					inDb.insert(kSiteDataTable, null, cv1);
				}
				inDb.setTransactionSuccessful();
				
				UpdateList();
			}
			catch (Exception exception)
			{
				exception.printStackTrace();
			}
            finally
            {
                if(inDb != null)
                    inDb.endTransaction();
            }
		}
		
		
		
		public static void UpdateLastCheck (SQLiteDatabase inDb, Date inDate)
		{
			try
			{
				ContentValues cv = new ContentValues(1);
				cv.put("last_check", inDate.getTime());
				inDb.beginTransaction();
				inDb.update(kModTimeTable, cv, "file=?", new String[]{"sitedata"});
				inDb.setTransactionSuccessful();
				inDb.endTransaction();
			}
			catch (Exception exception)
			{
                exception.printStackTrace();
				if (inDb != null)
					inDb.endTransaction();
			}
		}
	}



    public static class SiteListDownloader extends DataDownloader
    {
        public SiteListDownloader(Date inDate)
        {
            SetUpdateTime(inDate);
        }



        @Override
        protected void PostExecute(DownloadTask.ResultParms inResult)
        {
            SqlUpdate(inResult.mData, new Date(inResult.mLastModified));
            UpdateLastCheck();
            SiteListQuery();
        }
    }
}

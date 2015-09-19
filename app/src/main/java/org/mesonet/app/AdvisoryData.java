package org.mesonet.app;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;



public class AdvisoryData
{
	public static final int kFieldCount = 6;
	
	private static AdvisoriesListDbHelper sDbHelper = null;
	private static SQLiteDatabase sDatabase = null;
	
	private static ScheduledExecutorService sPool = null;

	private static String sAdvisoryText = null;

    private static AdvisoriesListUpdater sAdvisoriesListUpdater = new AdvisoriesListUpdater();
    private static AdvisoryUpdater sAdvisoryUpdater = new AdvisoryUpdater();

    private static Date sLastUpdate = null;



	public static void Initialize()
	{
		sDbHelper = new AdvisoriesListDbHelper();
		sDatabase = sDbHelper.getWritableDatabase();

        UpdateAdvisoriesUI();
	}
	
	
	
	public static void SetText(String inText)
	{
		sAdvisoryText = inText;
	}
	
	
	
	public static String GetText()
	{
		return sAdvisoryText;
	}
	
	
	
	public static void StartDownload()
	{
        long dlTime = 0;

        Date now = new Date();

        if(sLastUpdate != null && (now.getTime() - sLastUpdate.getTime()) < DataContainer.kOneMinute)
            dlTime = now.getTime() - sLastUpdate.getTime();

        StopDownload();

        sPool = Executors.newScheduledThreadPool(1);
        sPool.scheduleAtFixedRate(new AdvisoriesUpdateTimer(), dlTime, DataContainer.kOneMinute, TimeUnit.MILLISECONDS);
	}
	
	
	
	public static void StopDownload()
	{
		if (sPool != null)
			sPool.shutdown();
	}
	
	
	
	private static void UpdateAdvisoriesUI ()
	{
		int count = Count();
		
		TextView tabCountText = MesonetActionBar.AdvisoryCountText();
		ImageView tabCountImage = MesonetActionBar.AdvisoryCountImage();
		
		if(tabCountText != null)
		{
			tabCountText.setText(Integer.toString(count));
			
			if (count > 0)
			{
				tabCountImage.setVisibility(View.VISIBLE);
				tabCountText.setVisibility(View.VISIBLE);
			}
			else
			{
				tabCountImage.setVisibility(View.INVISIBLE);
				tabCountText.setVisibility(View.INVISIBLE);
			}
		}
		
		AdvisoriesFragment.UpdateList();
	}
	
	
	
	public static void SqlUpdate (String inCsv, Date inDate)
	{
		JSONArray json = ParseFile(inCsv);
		sDbHelper.Update(sDatabase, json, inDate);
	}
	
	
	
	public static int Count ()
	{
		Cursor results = sDatabase.query(AdvisoriesListDbHelper.ADVISORIES_TABLE, new String[]{"_id"}, null, null, null, null, null);
		int count = results.getCount();
		results.close();
		return count;
	}
	
	
	
	public static Cursor Advisories ()
	{
		return sDatabase.query("advisories", new String[]{"_id", "heading", "counties", "url"}, null, null, null, null, null);
	}
	
	
	
	public static void DisplayAdvisory(String inUrl)
	{
	    sAdvisoryUpdater.Update(inUrl, true, false);
	}
	
	
	
	private static JSONArray ParseFile (String inCsv)
	{
		JSONArray advisoriesJSON = null;
		if (!CheckValidAdvisoryData(inCsv))
			return null;
		
		if (inCsv != null)
		{
			String[] warningStrings = inCsv.split("\n");
			
			TreeSet<String> sigSort = new TreeSet<>(new AdvisoryComparator());
            sigSort.addAll(Arrays.asList(warningStrings));
			
			Iterator<String> warningIt = sigSort.iterator();
			
			Vector<String> phenList = new Vector<>();
			Vector<String> sigList = new Vector<>();
			Vector<String> countyList = new Vector<>();

			TypedArray phenArray = SavedDataManager.GetArrayResource(R.array.phenomena_order);
			TypedArray sigArray = SavedDataManager.GetArrayResource(R.array.significance_order);
			TypedArray countyArray = SavedDataManager.GetArrayResource(R.array.county_array);
			
			for(int i = 0; i < phenArray.length(); i++)
			{
				int id = phenArray.getResourceId(i, 0);
				phenList.add(SavedDataManager.GetResourceName(id));
			}
			
			for(int i = 0; i < sigArray.length(); i++)
			{
				int id = sigArray.getResourceId(i, 0);
				sigList.add(SavedDataManager.GetResourceName(id));
			}
			
			for(int i = 0; i < countyArray.length(); i++)
			{
				int id = countyArray.getResourceId(i, 0);
				countyList.add(SavedDataManager.GetResourceName(id));
			}
			
			advisoriesJSON = new JSONArray();
	
			for (int i = 0; warningIt.hasNext(); i++)
			{
				String[] components = warningIt.next().split(";");
				if (components.length >= 4)
				{
					String advisoryType = components[0];
					String advisoryPath = components[4];
					String advisoryCountyString = components[5];
					
					String[] advisoryCounties = advisoryCountyString.split(",");
					Vector<String> advisoryCountyNames = new Vector<>();
                    advisoryCountyNames.addAll(Arrays.asList(advisoryCounties));
					
					int id = phenArray.getResourceId(phenList.indexOf(advisoryType.substring(0, 2)), 0);
					String phenomena = SavedDataManager.GetStringResource(id);
					
					id = sigArray.getResourceId(sigList.indexOf(advisoryType.substring(3)), 0);
					String significance = SavedDataManager.GetStringResource(id);

                    advisoryCountyString = "";
	
					for(int j = 0; j < advisoryCountyNames.size(); j++)
					{
						if(j > 0)
							advisoryCountyString += ", ";
						
						if(countyList.contains(advisoryCountyNames.get(j)))
						{
							id = countyArray.getResourceId(countyList.indexOf(advisoryCountyNames.get(j)), 0);
							advisoryCountyString += SavedDataManager.GetStringResource(id);
						}
						else
							advisoryCountyString += advisoryCountyNames.get(j);
					}

					try
					{
						JSONObject newLine = new JSONObject();

						newLine.put("phenomenon", phenomena);
						newLine.put("significance", significance);
						newLine.put("counties" , advisoryCountyString);
						newLine.put("url", advisoryPath);

						advisoriesJSON.put(i, newLine);
					}
					catch(Exception exception)
					{
                        exception.printStackTrace();
					}
				}
			}
			
			phenArray.recycle();
			sigArray.recycle();
			countyArray.recycle();
		}
		return advisoriesJSON;
	}
	
	
	
	public static boolean CheckValidAdvisoryData (String inCsv)
	{
		String[] split = inCsv.split("\n");
		
		String[] firstLineSplit = split[0].split(";");

        return firstLineSplit.length >= kFieldCount;
	}
	
	
	
	private static class AdvisoriesListDbHelper extends SQLiteOpenHelper
	{
		private static final String DB_NAME = "advisories.db";
		private static final int DB_VERSION = 1;
		
		private static final String ADVISORIES_TABLE = "advisories";
		private static final String ADVISORIES_CREATE = 
				"CREATE TABLE " + ADVISORIES_TABLE + " (" +
					"_id int PRIMARY KEY," +
					"heading TEXT," +
					"counties TEXT," +
					"url TEXT" +
				");";
		
		private static final String MOD_TIME_TABLE = "modtime";
		private static final String MOD_TIME_CREATE =
				"CREATE TABLE " + MOD_TIME_TABLE + " (" +
					"file TEXT PRIMARY KEY," +
					"time INTEGER" +
				");";
		
		private static final String ADVISORIES_FILE = "advisories_data";
		

		public AdvisoriesListDbHelper()
		{
			super(MesonetApp.Activity(), DB_NAME, null, DB_VERSION);
		}

		
		
		@Override
		public void onCreate(SQLiteDatabase inDb)
		{
			inDb.execSQL(MOD_TIME_CREATE);
			inDb.execSQL(ADVISORIES_CREATE);
		}



		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			// Still on version 1, so no upgrades to do
		}


		
		public void Update(SQLiteDatabase inDb, JSONArray inJson, Date inUpdated)
		{
            inDb.beginTransaction();

            inDb.delete(MOD_TIME_TABLE, "file=?", new String[]{ADVISORIES_FILE});
            ContentValues cv = new ContentValues(3);
            cv.put("file", ADVISORIES_FILE);
            cv.put("time", inUpdated.getTime());
            inDb.insert(MOD_TIME_TABLE, null, cv);

            inDb.delete(ADVISORIES_TABLE, null, null);

            JSONObject buffer;

            ContentValues values;
            int i;

            if (inJson != null)
            {
                for (i = 0; i < inJson.length(); i++)
                {
                    try
                    {
                        buffer = inJson.getJSONObject(i);
                        values = new ContentValues(4);
                        values.put("_id", i);

                        values.put("heading", buffer.getString("phenomenon") + " " + buffer.getString("significance"));
                        values.put("counties", buffer.getString("counties"));
                        values.put("url", SavedDataManager.GetUrl(R.string.advisory_url) + buffer.getString("url"));

                        inDb.insert(ADVISORIES_TABLE, null, values);
                    }
                    catch(Exception exception)
                    {
                        exception.printStackTrace();
                    }
                }
            }

            inDb.setTransactionSuccessful();
            inDb.endTransaction();
		}
	}



	private static class AdvisoryComparator implements Comparator<String>
	{
		Vector<String> mSigKeys = new Vector<>();
		Vector<String> mPhenKeys = new Vector<>();
		
		
		
		@Override
		public int compare(String left, String right)
		{
			Resources res = MesonetApp.Activity().getResources();
			
			TypedArray sigOrder = res.obtainTypedArray(R.array.significance_order);

			for (int i = 0; i < sigOrder.length(); ++i)
			{
			    int id = sigOrder.getResourceId(i, 0);

			    mSigKeys.add(res.getResourceEntryName(id));
			}

			TypedArray phenOrder = res.obtainTypedArray(R.array.phenomena_order);

			for (int i = 0; i < sigOrder.length(); ++i)
			{
			    int id = phenOrder.getResourceId(i, 0);

			    mSigKeys.add(res.getResourceEntryName(id));
			}
			
			sigOrder.recycle();
			phenOrder.recycle();
			
			String[] leftSplit = left.split(";");
			String[] rightSplit = right.split(";");
			
			String leftKey = leftSplit[0].substring(3, 4);
			String rightKey = rightSplit[0].substring(3, 4);
			 
			int leftIndex = mSigKeys.indexOf(leftKey);
			int rightIndex = mSigKeys.indexOf(rightKey);
			
			if(leftIndex < rightIndex)
				return -1;
			if(leftIndex > rightIndex)
			
			leftKey = leftSplit[0].substring(0, 2);
			rightKey = rightSplit[0].substring(0, 2);
			
			leftIndex = mPhenKeys.indexOf(leftKey);
			rightIndex = mPhenKeys.indexOf(rightKey);
			
			if(leftIndex < rightIndex)
				return -1;
			if(leftIndex > rightIndex)
				return 1;
			
			return 1;
		}
	}
	
	
	
	private static class AdvisoriesUpdateTimer implements Runnable 
	{
        @Override
		public void run() 
		{
            sAdvisoriesListUpdater.Update();
		}
	}
	
	
	
	private static class AdvisoriesListUpdater extends DataDownloader
	{
        public void Update()
        {
            Update(SavedDataManager.GetUrl(R.string.advisory_list_url) + "/" + SavedDataManager.GetStringResource(R.string.advisory_filename), null, false, false);
        }



        @Override
		protected void PostExecute(DownloadTask.ResultParms inResult)
		{
            sLastUpdate = new Date();
            SqlUpdate(inResult.mData, Time());

			UpdateAdvisoriesUI();
		}
	}

	
	
	public static class AdvisoryUpdater extends DataDownloader
	{
        @Override
		protected void PostExecute(DownloadTask.ResultParms inResult)
		{
			AdvisoriesFragment.SetAdvisoryText(inResult.mData);
		}
	}
}

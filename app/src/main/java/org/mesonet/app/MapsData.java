package org.mesonet.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.Html;



public class MapsData
{
	private static MapsListDbHelper sDbHelper = null;
	private static SQLiteDatabase sDatabase = null;
	
	private static int sCurrentSection = 0;
	private static int sActivePage = 0;
	private static String sCurrentUrl = null;
	private static JSONObject sProductJSON = null;

    private static MapsListUpdater sMapsListUpdater = new MapsListUpdater();
	
	
	
	public static void Initialize()
	{
		PopulateProductJSON();
		
		sMapsListUpdater.Update(SavedDataManager.GetUrlFile(R.string.maps_url, R.string.maps_filename), SavedDataManager.GetFile(R.string.maps_filename), false, false);
		
		if(sDbHelper == null)
		{
			SetProductJSON(sProductJSON);
		}
	}



    public static void Download()
    {
        new MapDownloader().execute();
    }
	
	
	
	public static SQLiteDatabase Database()
	{
		return sDatabase;
	}
	
	
	
	public static void SetSection(int inSection)
	{
		sCurrentSection = inSection;
	}
	
	
	
	public static void SetPage(int inPage)
	{
		sActivePage = inPage;
	}



	public static void SetUrl(String inUrl)
	{
		sCurrentUrl = inUrl;
	}
	
	
	
	public static int Section()
	{
		return sCurrentSection;
	}
	
	
	
	public static int Page()
	{
		return sActivePage;
	}
	
	
	
	public static String Url()
	{
		return sCurrentUrl;
	}
	
	
	
	private static void PopulateProductJSON()
	{
		try
		{
			String result = "";
			File mapsFile = new File(SavedDataManager.GetFile(R.string.maps_filename));
			Scanner scanner = new Scanner(mapsFile);

			while(scanner.hasNextLine())
			{
				result += scanner.nextLine();
			}

			scanner.close();

			sProductJSON = new JSONObject(result);
		}
		catch (FileNotFoundException exception)
		{
			sMapsListUpdater.Update(SavedDataManager.GetUrlFile(R.string.maps_url, R.string.maps_filename), false, false);
		}
		catch (Exception exception)
		{
            exception.printStackTrace();
		}
	}
	
	
	
	public static void SetProductJSON(JSONObject inJSON)
	{
		if (inJSON == null)
			return;
		
		if(sDbHelper == null)
			sDbHelper = new MapsListDbHelper(MesonetApp.Activity());
		
		sDatabase = sDbHelper.getReadableDatabase();
		
		try
		{
			SQLiteDatabase mDatabase = sDbHelper.getWritableDatabase();
			MapsListDbHelper.Update(mDatabase, inJSON);
		}
		catch(Exception exception)
		{
            exception.printStackTrace();
		}
	}
	
	
	
	public static class MapsListUpdater extends DataDownloader
	{
		@Override
		protected void PostExecute(DownloadTask.ResultParms inResult)
		{
            SavedDataManager.SaveFile(SavedDataManager.GetStringResource(R.string.maps_filename), inResult.mData, inResult.mLastModified);

            try {
                sProductJSON = new JSONObject(inResult.mData);
                SetProductJSON(sProductJSON);
            }
            catch(Exception exception)
            {
                exception.printStackTrace();
            }
		}
	}
	
	
	
	private static class MapsListDbHelper extends SQLiteOpenHelper
	{
		private static final String DB_NAME = "maps.db";
		private static final int DB_VERSION = 3;
		private static final String SECTIONS_TABLE = "sections";
		private static final String PRODUCTS_TABLE = "products";
		
		private static final String SECTIONS_CREATE = 
				"CREATE TABLE " + SECTIONS_TABLE + " (" +
					"_id int PRIMARY KEY," +
					"title TEXT," +
					"sections ARRAY" +
				");";
		
		private static final String PRODUCTS_CREATE = 
				"CREATE TABLE " + PRODUCTS_TABLE + " (" +
					"_id int PRIMARY KEY," +
					"title TEXT," +
					"product TEXT," +
					"section int," + 
					"url TEXT" +
				");";
		
		private static final String MOD_TIME_TABLE = "modtime";
		private static final String MOD_TIME_CREATE =
				"CREATE TABLE " + MOD_TIME_TABLE + " (" +
					"file TEXT PRIMARY KEY," +
					"time INTEGER," +
					"last_check INTEGER" +
				");";
		
		

		public MapsListDbHelper(Context inContext)
		{
			super(inContext, DB_NAME, null, DB_VERSION);
		}

		
		
		@Override
		public void onCreate(SQLiteDatabase inDb)
		{
		}

		
		
		@Override
		public void onUpgrade(SQLiteDatabase inDb, int inOldVersion, int inNewVersion)
		{
			inDb.beginTransaction();

			if (inOldVersion == 1 && inNewVersion == DB_VERSION) 
			{
				inDb.execSQL("DROP TABLE IF EXISTS " + MOD_TIME_TABLE);
				inDb.execSQL("DROP TABLE IF EXISTS " + SECTIONS_TABLE);
				inDb.execSQL("DROP TABLE IF EXISTS " + PRODUCTS_TABLE);

				inDb.execSQL(SECTIONS_CREATE);

				Update(inDb, sProductJSON);

				inDb.setTransactionSuccessful();
				inDb.endTransaction();
			}
			else if (inOldVersion == 2 && inNewVersion == DB_VERSION)
			{

				// Read old data from MOD_TIME_TABLE
				long time = 0;
				Cursor results = inDb.query(MOD_TIME_TABLE, new String[]{"time"}, "file=?", new String[]{"maps"}, null, null, null);
				if (results.getCount() > 0) 
				{
					results.moveToFirst();
					time = results.getLong(0);
				}
				results.close();
				
				// Recreate MOD_TIME_TABLE
				inDb.execSQL("DROP TABLE " + MOD_TIME_TABLE);
				inDb.execSQL(MOD_TIME_CREATE);
				
				// Add old data back in to MOD_TIME_TABLE
				ContentValues values = new ContentValues(3);
				values.put("file", "maps");
				values.put("time", time);
				values.put("last_check", time);
				inDb.insert(MOD_TIME_TABLE, null, values);

				inDb.setTransactionSuccessful();
				inDb.endTransaction();
			}
		}
		
		
		
		public static void Update(SQLiteDatabase inDb, JSONObject inJson)
		{
			try
			{
				inDb.beginTransaction();

				ContentValues values;
				
				inDb.execSQL("DROP TABLE IF EXISTS " + SECTIONS_TABLE);
				inDb.execSQL("DROP TABLE IF EXISTS " + PRODUCTS_TABLE);

				inDb.execSQL(SECTIONS_CREATE);
				inDb.execSQL(PRODUCTS_CREATE);

				JSONArray mainobj = inJson.getJSONArray("main");
				JSONObject secobj = inJson.getJSONObject("sections");
				JSONObject prodobj = inJson.getJSONObject("products");
				
				JSONObject buffer;
				JSONArray arrayBuffer1;
				JSONArray arrayBuffer2;
				
				int productCount = 0;
				
				for (int i = 0; i < mainobj.length(); i++)
				{
					buffer = mainobj.getJSONObject(i);
					values = new ContentValues(3);
					values.put("_id", i);
					values.put("title", Html.fromHtml(buffer.getString("title")).toString());
					values.put("sections", buffer.getString("sections"));
					inDb.insert(SECTIONS_TABLE, null, values);
					
					arrayBuffer1 = buffer.getJSONArray("sections");
					
					for(int j = 0; j < arrayBuffer1.length(); j++)
					{
						if(secobj.has(arrayBuffer1.getString(j)))
						{
							buffer = secobj.getJSONObject(arrayBuffer1.getString(j));
							String title = "";
							
							if(buffer.has("title"))
								title = buffer.getString("title");
							
							arrayBuffer2 = buffer.getJSONArray("products");
							
							for(int k = 0; k < arrayBuffer2.length(); k++, productCount++)
							{
								buffer = prodobj.getJSONObject(arrayBuffer2.getString(k));

								values = new ContentValues(5);
								values.put("_id", productCount);
								values.put("title", Html.fromHtml(title).toString().toUpperCase(Locale.ENGLISH));
								values.put("section", i);
								values.put("url", buffer.getString("url"));
								values.put("product", Html.fromHtml(buffer.getString("title")).toString());
								inDb.insert(PRODUCTS_TABLE, null, values);
								
								title = "";
							}
						}
					}
				}
				
				inDb.setTransactionSuccessful();
				MapsFragment.UpdateList(inDb);
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
	}



    private static class MapDownloader extends AsyncTask<String, String, File>
    {
        @Override
        protected File doInBackground(String... urls)
        {
            File imageFile = new File(MesonetApp.Activity().getCacheDir().toString() + "/MapShare.png");

            try
            {
                InputStream is = (InputStream) new URL(sCurrentUrl).getContent();
                Bitmap image = BitmapFactory.decodeStream(is);

                FileOutputStream out = new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            }
            catch(final Exception exception)
            {
                return null;
            }

            return imageFile;
        }



        @Override
        protected void onPostExecute(File inResult)
        {
            MapsFragment.FinishShare(inResult);
        }
    }
}

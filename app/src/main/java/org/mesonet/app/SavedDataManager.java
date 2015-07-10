package org.mesonet.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;



public class SavedDataManager
{
	public static File CacheDir()
	{
		return MesonetApp.Context().getApplicationContext().getCacheDir();
	}
	
	
	
	public static String FilesDir()
	{
		return MesonetApp.Context().getFilesDir().getPath();
	}
	
	
	
	public static Resources Resources()
	{
		return MesonetApp.Context().getResources();
	}



    public static String GetFile(int inId) { return FilesDir() + "/" + GetStringResource(inId); }
	
	
	
	public static SharedPreferences Settings()
	{
		return MesonetApp.Context().getSharedPreferences("MainActivity", Activity.MODE_PRIVATE);
	}
	
	
	
	public static String MissingField()
	{
		return GetStringResource(R.string.empty_field);
	}
	
	
	
	public static void SaveIntSetting(String inSettingName, int inValue)
	{
		SharedPreferences.Editor editor = GetPreferenceEditor();
		editor.putInt(inSettingName, inValue);
		editor.apply();
	}
	
	
	
	public static void SaveStringSetting(String inSettingName, String inValue)
	{
		SharedPreferences.Editor editor = GetPreferenceEditor();
		editor.putString(inSettingName, inValue);
		editor.apply();
	}



    private static SharedPreferences.Editor GetPreferenceEditor()
    {
        return Settings().edit();
    }
	
	
	
	public static InputStream GetRawResource(int inId)
	{
		return Resources().openRawResource(inId);
	}
	
	
	
	public static String GetStringResource(int inId)
	{
		return Resources().getString(inId);
	}



    public static String GetUrl(int inId)
    {
        String url = GetStringResource(inId);

        if(!url.startsWith("http"))
            url = GetStringResource(R.string.mesonet_url) + url;

        return url;
    }



    public static String GetUrlFile(int inUrlId, int inFileId) { return GetUrl(inUrlId) + "/" + GetStringResource(inFileId); }
	
	
	
	public static int GetIntResource(int inId)
	{
		return Resources().getInteger(inId);
	}
	
	
	
	public static boolean GetBooleanResource(int inId)
	{
		return Resources().getBoolean(inId);
	}



    public static float GetDimenResource(int inId) { return Resources().getDimension(inId);}



    public static TypedArray GetArrayResource(int inId) { return Resources().obtainTypedArray(inId); }



    public static String GetResourceName(int inId){ return Resources().getResourceEntryName(inId); }
	
	
	
	public static int GetIntSetting(String inId, int inDefaultValue)
	{
		return Settings().getInt(inId, inDefaultValue);
	}
	
	
	
	public static String GetStringSetting(String inId, String inDefaultValue)
	{
		return Settings().getString(inId, inDefaultValue);
	}
	
	
	
	public static void SaveFile(String inLocation, String inData, long inLastModified)
	{
		try
		{
			FileOutputStream outputStream = MesonetApp.Context().openFileOutput(inLocation, Context.MODE_PRIVATE);
			outputStream.write(inData.getBytes());
			outputStream.close();
			new File(FilesDir() + "/" + inLocation).setLastModified(inLastModified);
		}
		catch(Exception exception)
        {
            exception.printStackTrace();
        }
	}
}

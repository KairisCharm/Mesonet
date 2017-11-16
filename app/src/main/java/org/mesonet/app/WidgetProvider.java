package org.mesonet.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.RemoteViews;



abstract public class WidgetProvider extends AppWidgetProvider
{
	protected Context mContext;
	protected AppWidgetManager mAppWidgetManager;
	private int[] mAppWidgetIds;
	private String mSiteName;
    protected String mStid;
	
	private WidgetCurrCondDownloader sDownloader = new WidgetCurrCondDownloader();


	
	@Override
	public void onUpdate(Context inContext, AppWidgetManager inAppWidgetManager, int[] inAppWidgetIds)
	{
        super.onUpdate(inContext, inAppWidgetManager, inAppWidgetIds);
		
		mContext = inContext;
		mAppWidgetManager = inAppWidgetManager;
		mAppWidgetIds = inAppWidgetIds;

        CurrentConditions.Initialize();
        mStid = inContext.getSharedPreferences("MainActivity", Activity.MODE_PRIVATE).getString("stid", "nrmn");
        mSiteName = inContext.getSharedPreferences("MainActivity", Activity.MODE_PRIVATE).getString("site_name", "Norman");

        FullUpdate(inContext);
	}



	protected void FinishedDownload()
	{
		if(mAppWidgetManager != null)
		{
			ComponentName thisWidget = GetComponentName(mContext);
			int[] allWidgetIds = mAppWidgetManager.getAppWidgetIds(thisWidget);
			
			for (int widgetId : allWidgetIds)
			{
				RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), GetLayoutId());

                SetViews(remoteViews);
				
				Intent mainActIntent = new Intent(mContext, MainActivity.class);
				PendingIntent mainActPendingIntent = PendingIntent.getActivity(mContext, 0, mainActIntent, 0);
				
				Intent widgetProvIntent = GenerateIntent(mContext);
				widgetProvIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				widgetProvIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds);
				PendingIntent widgetProvPendingIntent = PendingIntent.getBroadcast(mContext, 0, widgetProvIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				remoteViews.setOnClickPendingIntent(GetLayoutNameId(), mainActPendingIntent);
				
				remoteViews.setOnClickPendingIntent(R.id.widget_update, widgetProvPendingIntent);

                mAppWidgetManager.updateAppWidget(widgetId, remoteViews);
			}
		}
	}
	
	
	
	void SetViews(RemoteViews inViews)
	{
		inViews.setTextViewText(R.id.widget_place, mSiteName);
		inViews.setTextViewText(R.id.widget_tair, CurrentConditions.AirTemp());
		
		inViews.setTextViewText(R.id.widget_feelslike, CurrentConditions.FeelsLike());
		inViews.setTextViewText(R.id.widget_wind, CurrentConditions.Wind(false));
		inViews.setTextViewText(R.id.widget_time, CurrentConditions.Time());
	}
	
	
	
	protected boolean NetworkIsAvailable()
	{
	    ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	
	
	abstract int GetLayoutId();
	abstract int GetLayoutNameId();
	abstract Intent GenerateIntent(Context inContext);
	abstract ComponentName GetComponentName(Context inContext);
	
	
	
	void FullUpdate(Context inContext)
	{
		if(NetworkIsAvailable())
            sDownloader.Update();
	}




    private class WidgetCurrCondDownloader extends CurrentConditions.CurrCondDownloader
    {
        public void Update()
        {
            Update(true);
        }



        @Override
        public void Update(boolean inForceUpdate)
        {
            Update(SavedDataManager.GetUrl(R.string.curr_cond_url) + "/" + mStid, null, inForceUpdate, WidgetProvider.this, false);
        }



        @Override
        protected void PostExecute(DownloadTask.ResultParms inResult)
        {
            super.PostExecute(inResult);

            //FinishedDownload();
        }
    }
}
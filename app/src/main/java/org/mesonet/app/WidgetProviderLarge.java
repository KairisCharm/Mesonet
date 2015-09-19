package org.mesonet.app;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.RemoteViews;


public class WidgetProviderLarge extends WidgetProvider
{
    public WidgetForecastDownloader mWidgetForecastDownloader = new WidgetForecastDownloader();


	@Override
	public void onUpdate(Context inContext, AppWidgetManager inAppWidgetManager, int[] inAppWidgetIds)
	{
		super.onUpdate(inContext, inAppWidgetManager, inAppWidgetIds);
        Forecast.Initialize();
	}
	
	
	
	@Override
	int GetLayoutId()
	{
		return R.layout.widget_large_layout;
	}
	
	
	
	@Override
	int GetLayoutNameId()
	{
		return R.id.widget_large_layout;
	}

	
	
	@Override
	Intent GenerateIntent(Context inContext)
	{
		return new Intent(inContext, WidgetProviderLarge.class);
	}
	
	
	
	@Override
	ComponentName GetComponentName(Context inContext)
	{
		return new ComponentName(inContext, WidgetProviderLarge.class);
	}
	
	
	
	@Override
	void FullUpdate(Context inContext)
	{
		super.FullUpdate(inContext);

		if(NetworkIsAvailable())
            mWidgetForecastDownloader.DoUpdate(new DataDownloader.DownloadTask.DownloadInput(SavedDataManager.GetUrl(R.string.forecast_url) + "/" + mStid, null, true, this, false));
	}
	
	
	
	@Override
	void SetViews(RemoteViews inViews)
	{
		super.SetViews(inViews);

        if(!Forecast.WaitingForCompletion())
        {
            SetSingleForecast(inViews, 0, R.id.widget_forecast_time1, R.id.widget_forecast_image1, R.id.widget_forecast_loworhigh1, R.id.widget_forecast_hightemp1, R.id.widget_forecast_lowtemp1, R.id.widget_forecast_condition1);
            SetSingleForecast(inViews, 1, R.id.widget_forecast_time2, R.id.widget_forecast_image2, R.id.widget_forecast_loworhigh2, R.id.widget_forecast_hightemp2, R.id.widget_forecast_lowtemp2, R.id.widget_forecast_condition2);
        }
	}




    @Override
    protected void FinishedDownload()
    {
        super.FinishedDownload();
    }
	
	
	
	private void SetSingleForecast(RemoteViews inViews, int index, int inTimeId, int inImageId, int inLowOrHighId, int inHighTempId, int inLowTempId, int inConditionId)
	{
		Resources resources = mContext.getResources();
		Forecast.ForecastData data = Forecast.GetData(index);
		
		inViews.setViewVisibility(inHighTempId, View.VISIBLE);
		inViews.setViewVisibility(inLowTempId, View.INVISIBLE);
		
		if(data.mLowOrHigh != null && data.mLowOrHigh.compareTo(resources.getString(R.string.forecast_low)) == 0)
		{
			inViews.setViewVisibility(inHighTempId, View.INVISIBLE);
			inViews.setViewVisibility(inLowTempId, View.VISIBLE);
		}
		
		String temp = data.GetTemp();
		
		inViews.setTextViewText(inTimeId, data.mTime);
		inViews.setImageViewBitmap(inImageId, data.mImage);
		inViews.setTextViewText(inLowOrHighId, data.mLowOrHigh);
		inViews.setTextViewText(inHighTempId, temp);
		inViews.setTextViewText(inLowTempId, temp);
		inViews.setTextViewText(inConditionId, data.mCondition);
	}



    public class WidgetForecastDownloader extends Forecast.ForecastDownloader
    {
        @Override
        public void Update(boolean inForceUpdate, String inStid)
        {
            Update(SavedDataManager.GetUrl(R.string.forecast_url) + "/" + inStid, null, inForceUpdate, WidgetProviderLarge.this, false);
        }



        @Override
        protected void PostExecute(DownloadTask.ResultParms inResult)
        {
			super.PostExecute(inResult);
        }
    }
}
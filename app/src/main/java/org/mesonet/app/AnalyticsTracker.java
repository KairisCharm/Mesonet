package org.mesonet.app;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;



public class AnalyticsTracker {

    private Tracker mTracker = null;



    private void CreateTracker()
    {
        if(mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(MesonetApp.Application());

            mTracker = analytics.newTracker(SavedDataManager.GetStringResource(R.string.analytics_id));
            mTracker.setScreenName(SavedDataManager.GetStringResource(R.string.analytics_view_name));
        }
    }



    public static void Send()
    {
        This().CreateTracker();

        This().mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }




    private static AnalyticsTracker This()
    {
        return MesonetApp.Tracker();
    }
}

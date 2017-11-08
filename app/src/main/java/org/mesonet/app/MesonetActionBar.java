package org.mesonet.app;

import android.support.design.widget.TabLayout;
import android.support.v7.widget.Toolbar;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;



public class MesonetActionBar
{
    final static String kLocalTag = "Local";
    final static String kMapsTag = "Maps";
    final static String kRadarTag = "Radar";
    final static String kAdvisoriesTag = "Advisories";

    private static ImageView sTabCountImage;
    private static TextView sTabCountText;



    public static void GenerateTabs(TabLayout inTabLayout, ViewPager inPager)
    {
        String text = "";
        int visibility = View.VISIBLE;

        if (sTabCountText != null)
        {
            text = sTabCountText.getText().toString();
            visibility = sTabCountText.getVisibility();
        }

        if (inTabLayout.getTabCount() != 0)
            inTabLayout.removeAllTabs();

        inTabLayout.addTab(inTabLayout.newTab().setCustomView(R.layout.local_tab_layout).setTag(kLocalTag));
        inTabLayout.addTab(inTabLayout.newTab().setCustomView(R.layout.maps_tab_layout).setTag(kMapsTag));
        inTabLayout.addTab(inTabLayout.newTab().setCustomView(R.layout.radar_tab_layout).setTag(kRadarTag));

        LinearLayout advisoryTabLayout = (LinearLayout) MesonetApp.Activity().getLayoutInflater().inflate(R.layout.advisories_tab_layout, null);

        inTabLayout.addTab(inTabLayout.newTab().setIcon(R.drawable.advisories_image).setCustomView(advisoryTabLayout).setTag(kAdvisoriesTag));

        sTabCountImage = (ImageView) advisoryTabLayout.findViewById(R.id.advisory_count_image);
        sTabCountText = (TextView) advisoryTabLayout.findViewById(R.id.advisory_tab_count);

        sTabCountImage.setVisibility(visibility);
        sTabCountText.setText(text);
        sTabCountText.setVisibility(visibility);

        inPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(inTabLayout));
        inTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                MainActivity.SetPage(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        TabLayout.Tab selectedTab = inTabLayout.getTabAt(0);

        if(selectedTab != null)
            selectedTab.select();

        AdvisoryData.Initialize();
    }



    public static TextView AdvisoryCountText()
    {
        return sTabCountText;
    }



    public static ImageView AdvisoryCountImage()
    {
        return sTabCountImage;
    }



    public static String GetSelectedTab(TabLayout inTabLayout)
    {
        TabLayout.Tab selectedTab = inTabLayout.getTabAt(inTabLayout.getSelectedTabPosition());

        if(selectedTab != null)
            return (String)selectedTab.getTag();

        return "";
    }
}

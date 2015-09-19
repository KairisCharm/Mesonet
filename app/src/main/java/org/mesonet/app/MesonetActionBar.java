package org.mesonet.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
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

    private static ActionBar sABar = null;
    private static int sSelectedTab = 0;

    private static ImageView sTabCountImage;
    private static TextView sTabCountText;



    private static void Init()
    {
        sABar = MesonetApp.Activity().getSupportActionBar();
    }



    public static void GenerateTabs()
    {
        if (sABar != null)
            sSelectedTab = sABar.getSelectedTab().getPosition();

        String text = "";
        int visibility = View.VISIBLE;

        if (sTabCountText != null)
        {
            text = sTabCountText.getText().toString();
            visibility = sTabCountText.getVisibility();
        }

        Init();

        sABar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        sABar.setDisplayShowTitleEnabled(false);

        if (sABar.getTabCount() != 0)
            sABar.removeAllTabs();

        ActionBar.Tab tab = sABar.newTab().setCustomView(R.layout.local_tab_layout).setTag(kLocalTag)
                .setTabListener(new TabListener<>(MesonetApp.Activity(), kLocalTag, LocalFragment.class));
        sABar.addTab(tab);

        tab = sABar.newTab().setCustomView(R.layout.maps_tab_layout).setTag(kMapsTag)
                .setTabListener(new TabListener<>(MesonetApp.Activity(), kMapsTag, MapsFragment.class));
        sABar.addTab(tab);

        tab = sABar.newTab().setCustomView(R.layout.radar_tab_layout).setTag(kRadarTag)
                .setTabListener(new TabListener<>(MesonetApp.Activity(), kRadarTag, RadarFragment.class));
        sABar.addTab(tab);

        LinearLayout advisoryTabLayout = (LinearLayout) MesonetApp.Activity().getLayoutInflater().inflate(R.layout.advisories_tab_layout, null);

        sABar.addTab(sABar.newTab().setIcon(R.drawable.advisories_image).setCustomView(advisoryTabLayout).setTag(kAdvisoriesTag).setTabListener(new TabListener<AdvisoriesFragment>(MesonetApp.Activity(), kAdvisoriesTag, AdvisoriesFragment.class)));

        sTabCountImage = (ImageView) advisoryTabLayout.findViewById(R.id.advisory_count_image);
        sTabCountText = (TextView) advisoryTabLayout.findViewById(R.id.advisory_tab_count);

        sTabCountImage.setVisibility(visibility);
        sTabCountText.setText(text);
        sTabCountText.setVisibility(visibility);

        sABar.setDisplayShowCustomEnabled(true);

        sABar.getTabAt(sSelectedTab).select();

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



    public static String GetSelectedTab()
    {
        return (String)sABar.getSelectedTab().getTag();
    }



    private static class TabListener<T extends Fragment> implements ActionBar.TabListener
    {
        private Fragment mFragment;
        private final FragmentActivity mActivity;
        private final String mTag;
        private final Class<T> mClass;



        public TabListener(FragmentActivity inActivity, String inTag, Class<T> inClass)
        {
            mActivity = inActivity;
            mTag = inTag;
            mClass = inClass;
            mFragment = inActivity.getSupportFragmentManager().findFragmentByTag(inTag);
        }



        @Override
        public void onTabSelected(ActionBar.Tab inTab, FragmentTransaction inFragTrans)
        {
            if (mFragment == null)
            {
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                inFragTrans.add(android.R.id.content, mFragment, mTag);
            }
            else
                inFragTrans.attach(mFragment);

            MainMenu.CheckRadarTransparencyItem(inTab);
        }



        @Override
        public void onTabUnselected(ActionBar.Tab inTab, FragmentTransaction inFragTrans)
        {
            if (mFragment != null)
                inFragTrans.detach(mFragment);
        }



        @Override
        public void onTabReselected(ActionBar.Tab inTab, FragmentTransaction inFragTrans)
        {
            // User selected the already selected tab. Usually do nothing.
        }
    }
}

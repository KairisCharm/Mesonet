package org.mesonet.app;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.common.api.Releasable;


public class AdvisoriesFragment extends StaticFragment
{
	private ListView mList = null;
	private ScrollView mAdvisoryDisplay = null;
	private TextView mAdvisoryText = null;
	private Toolbar mToolbar = null;
	private RelativeLayout mContent = null;
	
	private static SimpleCursorAdapter sAdvisoryListAdapter = null;
	
	
	
	@Override
	public void onCreate(Bundle inSavedInstanceState)
	{
		super.onCreate(inSavedInstanceState);		

		setHasOptionsMenu(false);
	}



	@Override
	public View onCreateView(LayoutInflater inInflater, ViewGroup inContainer, Bundle inSavedInstanceState)
	{
		//View toReturn = Inflate(inInflater, inContainer);

		//ArrangeWithTabs(getResources().getConfiguration());

        return Inflate(inInflater, inContainer);
	}



	@Override
	public void onConfigurationChanged(Configuration inNewConfiguration)
	{
		super.onConfigurationChanged(inNewConfiguration);
		ArrangeWithTabs();
	}



	public void ResizeToolbar()
	{
		mToolbar.getLayoutParams().height = MainActivity.GetToolbarHeight();
		mToolbar.requestLayout();

		ArrangeWithTabs();
	}



	private void ArrangeWithTabs()
	{
		switch (getResources().getConfiguration().orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				((ViewGroup.MarginLayoutParams)mContent.getLayoutParams()).setMargins(0, MainActivity.GetToolbarHeight(), 0, 0);
				if(getView() != null)
					getView().requestLayout();
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				((ViewGroup.MarginLayoutParams)mContent.getLayoutParams()).setMargins(0, 0, 0, 0);
				if(getView() != null)
					getView().requestLayout();
				break;
		}
	}
	
	
	
	@Override
	public void onResume()
	{
		super.onResume();

        Activate();
		
		UpdateList();
	}
	
	
	
	private static boolean Activated()
	{
		return (This() != null) && This().IsActivated();
	}
	
	
	
	private static AdvisoriesFragment This()
	{
		return MainActivity.AdvisoriesFragment();
	}



    public View Inflate(LayoutInflater inInflater, ViewGroup inView)
    {
        View toReturn = inInflater.inflate(R.layout.advisories_fragment_layout, inView, false);
        mList = (ListView)toReturn.findViewById(R.id.advisory_list);
        mAdvisoryDisplay = (ScrollView)toReturn.findViewById(R.id.advisory_display);
        mAdvisoryText = (TextView)toReturn.findViewById(R.id.advisory_text);
		mContent = (RelativeLayout) toReturn.findViewById(R.id.content);

        if(sAdvisoryListAdapter != null)
        {
            mList.setAdapter(sAdvisoryListAdapter);

            if(mAdvisoryDisplay.getVisibility() == View.VISIBLE)
                mList.setVisibility(View.GONE);
        }

        View view = toReturn.findViewById(R.id.advisories_empty_list);
        mList.setEmptyView(view);

        if(AdvisoryData.GetText() != null)
        {
            mList.setVisibility(View.GONE);
            mAdvisoryDisplay.setVisibility(View.VISIBLE);
            mAdvisoryText.setText(AdvisoryData.GetText());
        }

        mList.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {

                Cursor cursor = (Cursor) sAdvisoryListAdapter.getItem(position);

                // If the list that was clicked was the main category list (so no URL attached to it)
                int index = cursor.getColumnIndex("url");

                if(cursor.getString(index).compareTo("") != 0)
                    AdvisoryData.DisplayAdvisory(cursor.getString(index));
            }
        });

		mToolbar = (Toolbar)toReturn.findViewById(R.id.toolBar);
		InitActionBar(mToolbar);

        return toReturn;
    }



	public void InitActionBar(Toolbar inToolbar)
	{
		TextView actionBarText = (TextView)inToolbar.findViewById(R.id.action_bar_text);
		actionBarText.setText(R.string.advisory_heading);

		mToolbar.inflateMenu(R.menu.main_menu);

		mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem inItem) {
				MainActivity.SelectMenuItem(inItem);
				return true;
			}
		});
	}
	
	
	
	public static void UpdateList()
	{
		if(!Activated())
			return;
		
		sAdvisoryListAdapter = new SimpleCursorAdapter(MesonetApp.Activity(), R.layout.advisories_list_item_layout, AdvisoryData.Advisories(), new String[]{"heading", "counties", "url"}, new int[]{R.id.advisories_list_heading, R.id.advisories_list_counties}, 0);

    	if(This().mList != null)
    	{
    		This().mList.setAdapter(sAdvisoryListAdapter);
	        
	        if(This().mAdvisoryDisplay.getVisibility() == View.VISIBLE)
	        	This().mList.setVisibility(View.GONE);
    	}
    }
	
	
	
	public static void SetAdvisoryText(String inResult)
	{
		if(!Activated())
			return;
		
		AdvisoryData.SetText(inResult);
		This().mList.setVisibility(View.GONE);
		This().mAdvisoryDisplay.setVisibility(View.VISIBLE);
		This().mAdvisoryText.setText(inResult);
	}
	
	
	
	public static boolean BackToList()
	{
		if(!Activated())
			return false;
		
		if(This().mAdvisoryDisplay.getVisibility() == View.VISIBLE)
		{
			AdvisoryData.SetText(null);
			This().mAdvisoryDisplay.setVisibility(View.GONE);
			This().mList.setVisibility(View.VISIBLE);
			return true;
		}
		
		return false;
	}
}

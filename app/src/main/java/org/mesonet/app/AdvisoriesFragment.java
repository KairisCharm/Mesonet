package org.mesonet.app;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;



public class AdvisoriesFragment extends StaticFragment
{
	private ListView mList = null;
	private ScrollView mAdvisoryDisplay = null;
	private TextView mAdvisoryText = null;
	
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
		View toReturn = Inflate(inInflater, inContainer);

        InitActionBar(0);

        return toReturn;
	}
	
	
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		InitActionBar(0);

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

        return toReturn;
    }



    @Override
    public View InitActionBar(int inLayoutId)
    {
        View actionBarView = super.InitActionBar(R.layout.advisories_action_bar_layout);

        TextView actionBarText = (TextView)actionBarView.findViewById(R.id.action_bar_text);
        actionBarText.setText(R.string.advisory_heading);

        return null;
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

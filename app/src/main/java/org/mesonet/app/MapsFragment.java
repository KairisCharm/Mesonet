package org.mesonet.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;



public class MapsFragment extends StaticFragment {
    private static SimpleCursorAdapter sSubcatListAdapter = null;

    private ListView mMapsList;
    private ListView mSubcatList;
    private WebView mMapDisplay;
    private Button mShareButton;
    private static ProgressDialog sProgDialog = null;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
    }


    @Override
    public View onCreateView(LayoutInflater inInflater, ViewGroup inContainer, Bundle inSavedInstanceState) {
        return Inflate(inInflater, inContainer);
    }


    @Override
    public void onResume() {
        super.onResume();

        // Create custom view for action bar
        InitActionBar(0);

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareMap();
            }
        });

        Activate();

        UpdateList(MapsData.Database());

        switch (MapsData.Page()) {
            case 1:
                mMapsList.setVisibility(View.GONE);
                mSubcatList.setVisibility(View.VISIBLE);
                mMapDisplay.setVisibility(View.GONE);

                mShareButton.setVisibility(View.GONE);

                DisplaySection(MapsData.Section(), MapsData.Database());

                break;
            case 2:
                DisplaySection(MapsData.Section(), MapsData.Database());
                mMapsList.setVisibility(View.GONE);
                mSubcatList.setVisibility(View.GONE);
                mMapDisplay.setVisibility(View.VISIBLE);

                mShareButton.setVisibility(View.VISIBLE);

                mMapDisplay.loadUrl(MapsData.Url());

                break;
        }
    }



    protected View Inflate(LayoutInflater inInflater, ViewGroup inView)
    {
        View toReturn = inInflater.inflate(R.layout.maps_fragment_layout, inView, false);

        mMapsList = (ListView)toReturn.findViewById(R.id.maps_list);
        mSubcatList = (ListView)toReturn.findViewById(R.id.maps_subcat_list);
        mMapDisplay = (WebView)toReturn.findViewById(R.id.maps_display);

        mMapDisplay.getSettings().setUserAgentString(MesonetApp.UserAgentString());
        mMapDisplay.getSettings().setBuiltInZoomControls(true);
        mMapDisplay.getSettings().setUseWideViewPort(true);

        mMapDisplay.setInitialScale(10);

        mMapsList.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                DisplaySection(position, MapsData.Database());
                MapsData.SetPage(1);
            }
        });

        mSubcatList.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Cursor cursor = (Cursor) sSubcatListAdapter.getItem(position);
                String url = cursor.getString(cursor.getColumnIndex("url"));
                DisplayMap(url);

                MapsData.SetPage(2);
                MapsData.SetUrl(url);
            }
        });

        return toReturn;
    }
	
	
	
	private static boolean Activated()
	{
		return (This() != null) && This().IsActivated();
	}
	
	
	
	private static MapsFragment This()
	{
		return MainActivity.MapsFragment();
	}



    @Override
    public View InitActionBar(int inLayoutId)
    {
        View actionBarView = super.InitActionBar(R.layout.maps_action_bar_layout);

        TextView actionBarText = (TextView)actionBarView.findViewById(R.id.action_bar_text);
        actionBarText.setText("Maps");

        mShareButton = (Button)actionBarView.findViewById(R.id.action_bar_share_button);

        if (mMapDisplay.getVisibility() == View.VISIBLE)
            mShareButton.setVisibility(View.VISIBLE);

        return null;
    }
	
	
	
	public static void UpdateList(SQLiteDatabase inDatabase)
	{
		if(!Activated())
			return;
		
	    Cursor mapCursor = inDatabase.query("sections", new String[]{"_id", "title", "sections"}, null, null, null, null, null);
        SimpleCursorAdapter mapsListAdapter = new SimpleCursorAdapter(MesonetApp.Activity(), R.layout.map_category_list_item_layout, mapCursor, new String[]{"title"}, new int[]{R.id.maps_category_title}, 0);
        
        This().mMapsList.setAdapter(mapsListAdapter);
    }
	
	
	
	public static boolean BackToList()
	{
		if(!Activated())
			return false;
		
		This().mShareButton.setVisibility(View.GONE);
		MapsData.SetPage(0);
		This().mMapDisplay.loadUrl("about:blank");
		if(This().mMapDisplay.getVisibility() == View.VISIBLE)
		{
			This().mMapsList.setVisibility(View.GONE);
			This().mSubcatList.setVisibility(View.VISIBLE);
			This().mMapDisplay.setVisibility(View.GONE);
			MapsData.SetPage(1);
			return true;
		}
		else if(This().mSubcatList.getVisibility() == View.VISIBLE)
		{
			This().mMapDisplay.setVisibility(View.GONE);
			This().mSubcatList.setVisibility(View.GONE);
			This().mMapsList.setVisibility(View.VISIBLE);
			MapsData.SetPage(0);
			return true;
		}
		
		return false;
	}

	
	
	public static void DisplaySection(int inSectionNumber, SQLiteDatabase inDatabase)
	{
		if(!Activated())
			return;
		
		Cursor subcatCursor = inDatabase.query("products", new String[]{"_id", "title", "product", "section", "url"}, "section=" + Integer.toString(inSectionNumber), null, null, null, null);
		sSubcatListAdapter = new SimpleCursorAdapter(MesonetApp.Activity(), R.layout.maps_list_item_layout, subcatCursor, new String[]{"product", "title", "url"}, new int[]{R.id.maps_list_title, R.id.maps_list_separator}, 0);
        
        sSubcatListAdapter.setViewBinder(new DataBinder());
        This().mSubcatList.setAdapter(sSubcatListAdapter);
        
        This().mMapsList.setVisibility(View.GONE);
        This().mSubcatList.setVisibility(View.VISIBLE);
        
        MapsData.SetSection(inSectionNumber);
	}
	
	
	
	public static void DisplayMap(String inUrl)
	{
		if(!Activated())
			return;
		
		This().mShareButton.setVisibility(View.VISIBLE);
		This().mMapDisplay.loadUrl(inUrl);
		
		This().mMapDisplay.setVisibility(View.VISIBLE);
		This().mSubcatList.setVisibility(View.GONE);
	}
	
	
	
	public static void ShareMap()
	{
		if(!Activated())
			return;

        MapsData.SetUrl(This().mMapDisplay.getUrl());
		sProgDialog = new ProgressDialog(MesonetApp.Activity());
		sProgDialog.setMessage("Getting map...");
		sProgDialog.show();
		
		ConnectivityManager connMgr = (ConnectivityManager)MesonetApp.Activity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();	

		if (networkInfo != null && networkInfo.isConnected())
		{
			MapsData.Download();
		}
	}



    public static void FinishShare(File inFile)
    {
        if(inFile != null)
        {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(inFile));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            MesonetApp.Activity().startActivity(Intent.createChooser(intent, "Share Map Using"));
            sProgDialog.dismiss();
        }
        else
            sProgDialog.dismiss();
    }
	
	
	
	private static class DataBinder implements SimpleCursorAdapter.ViewBinder
	{
		@Override
		public boolean setViewValue(View inView, Cursor inCursor, int inColumnIndex)
		{
			TextView textView = (TextView) inView;
			
			String textRepresentation = inCursor.getString(inColumnIndex);
			
			if (textView.getId() == R.id.maps_list_separator)
			{
				if (textRepresentation.equals(""))
					textView.setVisibility(View.GONE);
				else
				{
					textView.setText(textRepresentation);
					textView.setVisibility(View.VISIBLE);
				}
			}
			else
				textView.setText(textRepresentation);
			return true;
		}
	}
}

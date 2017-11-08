package org.mesonet.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;



public class MapsFragment extends StaticFragment {
    private static SimpleCursorAdapter sSubcatListAdapter = null;

    private ListView mMapsList;
    private ListView mSubcatList;
    private WebView mMapDisplay;
    private ImageButton mShareButton;
    private Toolbar mToolbar;
    private RelativeLayout mContent;
    private static ProgressDialog sProgDialog = null;

    private static Cursor sMapSectionList = null;
    private static Cursor sMapList = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
    }


    @Override
    public View onCreateView(LayoutInflater inInflater, ViewGroup inContainer, Bundle inSavedInstanceState) {
        //View result =
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
    public void onResume() {
        super.onResume();
        // Create custom view for action bar

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

        //MesonetActionBar.Refresh();
    }



    @Override
    public void onDestroy()
    {
        sMapSectionList.close();

        if(sMapList != null)
            sMapList.close();

        super.onDestroy();
    }




    protected View Inflate(LayoutInflater inInflater, ViewGroup inView)
    {
        View toReturn = inInflater.inflate(R.layout.maps_fragment_layout, inView, false);

        mMapsList = (ListView)toReturn.findViewById(R.id.maps_list);
        mSubcatList = (ListView)toReturn.findViewById(R.id.maps_subcat_list);
        mMapDisplay = (WebView)toReturn.findViewById(R.id.maps_display);
        mToolbar = (Toolbar)toReturn.findViewById(R.id.toolBar);
        mContent = (RelativeLayout)toReturn.findViewById(R.id.content);

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

        InitActionBar((Toolbar)toReturn.findViewById(R.id.toolBar));

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



    //@Override
    public void InitActionBar(Toolbar inToolbar)
    {
        TextView actionBarText = (TextView)inToolbar.findViewById(R.id.action_bar_text);
        actionBarText.setText("Maps");

        mShareButton = (ImageButton)inToolbar.findViewById(R.id.action_bar_share_button);

        if (mMapDisplay.getVisibility() == View.VISIBLE)
            mShareButton.setVisibility(View.VISIBLE);

        inToolbar.inflateMenu(R.menu.main_menu);

        inToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem inItem) {
                MainActivity.SelectMenuItem(inItem);
                return true;
            }
        });
    }
	
	
	
	public static void UpdateList(SQLiteDatabase inDatabase)
	{
		if(!Activated())
			return;

        if(sMapSectionList == null && inDatabase != null)
	        sMapSectionList = inDatabase.query("sections", new String[]{"_id", "title", "sections"}, null, null, null, null, null);

        SimpleCursorAdapter mapsListAdapter = new SimpleCursorAdapter(MesonetApp.Activity(), R.layout.map_category_list_item_layout, sMapSectionList, new String[]{"title"}, new int[]{R.id.maps_category_title}, 0);

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

	
	
	public static synchronized void DisplaySection(int inSectionNumber, SQLiteDatabase inDatabase)
	{
		if(!Activated())
			return;

        if(inSectionNumber != MapsData.GetSection() || sMapList == null) {
            if (sMapList != null)
                sMapList.close();
            sMapList = inDatabase.query("products", new String[]{"_id", "title", "product", "section", "url"}, "section=" + Integer.toString(inSectionNumber), null, null, null, null);
            sSubcatListAdapter = new SimpleCursorAdapter(MesonetApp.Activity(), R.layout.maps_list_item_layout, sMapList, new String[]{"product", "title", "url"}, new int[]{R.id.maps_list_title, R.id.maps_list_separator}, 0);
        }

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

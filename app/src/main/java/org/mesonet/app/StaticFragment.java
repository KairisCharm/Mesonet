package org.mesonet.app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;



public abstract class StaticFragment extends SherlockFragment
{
	protected boolean mInitialized = false;
    protected boolean mSavedInstanceState = false;

    protected static final int kNonSelectMode = 0;
    protected static final int kSelectMode = 1;
    protected static final int kUpdateMode = 2;



    @Override
    public void onStart()
    {
        super.onStart();

        InitActionBar(0);
    }



	protected void Activate()
	{
		mInitialized = true;
	}
	
	
	
	public boolean IsActivated()
	{
		return mInitialized;
	}



    protected View InitActionBar(int inLayoutId)
    {
        LayoutInflater factory = getActivity().getLayoutInflater();
        View actionBarView = factory.inflate(inLayoutId, null);

        ActionBar aBar = ((MainActivity)getActivity()).getSupportActionBar();
        aBar.setCustomView(actionBarView);

        return actionBarView;
    }



    @Override
    public void onSaveInstanceState(Bundle inBundle)
    {
        mSavedInstanceState = true;
        super.onSaveInstanceState(inBundle);
    }



    /*@Override
    public void onConfigurationChanged(Configuration inNewConfig)
    {
        super.onConfigurationChanged(inNewConfig);

        /*if(!mSavedInstanceState)
            RedoActionBar();*
    }*/
}

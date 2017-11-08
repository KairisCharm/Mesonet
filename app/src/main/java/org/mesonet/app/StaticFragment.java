package org.mesonet.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;



public abstract class StaticFragment extends Fragment
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

        //InitActionBar(0);
    }



	protected void Activate()
	{
		mInitialized = true;
	}
	
	
	
	public boolean IsActivated()
	{
		return mInitialized;
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

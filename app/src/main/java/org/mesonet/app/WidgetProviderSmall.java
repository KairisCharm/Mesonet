package org.mesonet.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;



public class WidgetProviderSmall extends WidgetProvider
{
	@Override
	protected int GetLayoutId()
	{
		return R.layout.widget_small_layout;
	}
	
	
	
	@Override
	protected int GetLayoutNameId()
	{
		return R.id.widget_small_layout;
	}

	
	
	@Override
	protected Intent GenerateIntent(Context inContext)
	{
		return new Intent(inContext, WidgetProviderSmall.class);
	}
	
	
	
	@Override
	protected ComponentName GetComponentName(Context inContext)
	{
		return new ComponentName(inContext, WidgetProviderSmall.class);
	}
}

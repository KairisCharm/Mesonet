package org.mesonet.app;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;



public class ScalableLayout extends LinearLayout
{
	private float mWidthScale;
	private float mHeightScale;
	private float mTextScale;
	
	private boolean mFirstMeasure = true;
	
	
	
	public ScalableLayout(Context inContext)
	{
		super(inContext);
		InitLayoutParams(inContext, 1.0f, 1.0f);
	}

	
	
	public ScalableLayout(Context inContext, AttributeSet inAttributes)
	{
		super(inContext, inAttributes);
		InitLayoutParams(inContext, inAttributes);
	}
	
	
	
	public ScalableLayout(Context inContext, AttributeSet inAttributes, int inDefStyle)
	{
        // cannot use the 3 parameter version because it is not compatible with Gingerbread, and the "super" call must be the first line in the constructor
        super(inContext, inAttributes);

		InitLayoutParams(inContext, inAttributes);
	}
	
	
	
	@Override
	protected void onMeasure(int inWidthMeasureSpec, int inHeightMeasureSpec)
	{
		View child;
		
		for(int i = 0; i < getChildCount(); i++)
		{
			child = getChildAt(i);
			
			if(mFirstMeasure)
			{
				LayoutParams params = (LayoutParams)child.getLayoutParams();
				params.leftMargin *= mWidthScale;
				params.topMargin *= mHeightScale;
				params.rightMargin *= mWidthScale;
				params.bottomMargin *= mHeightScale;
				
				if(params.width != LayoutParams.WRAP_CONTENT && params.width != LayoutParams.MATCH_PARENT)
					params.width *= mWidthScale;
				
				if(params.height != LayoutParams.WRAP_CONTENT && params.height != LayoutParams.MATCH_PARENT)
					params.height *= mHeightScale;
				
				child.setLayoutParams(params);
				
				if(child instanceof TextView)
                {
                    TextView textView = (TextView)child;
                    textView.setTextSize(textView.getTextSize() * mTextScale);
                }
			}
		}
		
		mFirstMeasure = false;
		
		super.onMeasure(inWidthMeasureSpec, inHeightMeasureSpec);
	}
	
	
	
	@Override
	protected void onLayout(boolean inChanged, int inLeft, int inTop, int inRight, int inBottom)
	{		
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)
			super.onLayout(inChanged, inLeft, inTop, inRight, inBottom);
		else
			super.onLayout(inChanged, Math.round(inLeft * mWidthScale), Math.round(inTop * mHeightScale), Math.round(inRight * mWidthScale), Math.round(inBottom * mHeightScale));
		
		View view;
		
		for(int i = 0; i < getChildCount(); i++)
		{
			view = getChildAt(i);

			if(view instanceof AutoResizeTextView)
            {
                AutoResizeTextView arTextView = (AutoResizeTextView) view;
                arTextView.AllowResize();
            }
		}
	}



    private void InitLayoutParams(Context inContext, AttributeSet inAttributes)
    {
        InitLayoutParams(inContext, inAttributes.getAttributeFloatValue(null, "base_width", 1.0f), inAttributes.getAttributeFloatValue(null, "base_height", 1.0f));
    }
	
	
	
	private void InitLayoutParams(Context inContext, float inBaseWidth, float inBaseHeight)
	{
        if(!(inContext instanceof Activity))
            return;

        Activity activity = (Activity) inContext;

		DisplayMetrics display = new DisplayMetrics();
		
		activity.getWindowManager().getDefaultDisplay().getMetrics(display);
		
		float currentWidthInches = (float)display.widthPixels / display.densityDpi;
		float currentHeightInches = (float)display.heightPixels / display.densityDpi;
		
		mWidthScale = currentWidthInches / inBaseWidth;
		mHeightScale = currentHeightInches / inBaseHeight;
		
		mTextScale = (mHeightScale / display.scaledDensity) / getResources().getConfiguration().fontScale;
	}
}

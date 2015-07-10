package org.mesonet.app;

import java.util.TreeMap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;



public class AutoResizeTextView extends TextView
{
    private static final int kNoLineLimit = -1;
    private static final int kNoWidthLimit = 0;

	private RectF mTextRect = new RectF();
	private RectF mAvailableSpaceRect;
	
	private TreeMap<Integer, Float> mAlreadyTriedSizes;
	
	private TextPaint mPaint;
	
	private float mMaxTextSize;
	private float mSpacingMult = 1.0f;
	private float mSpacingAdd = 0.0f;

	private int mWidthLimit;

	private int mMaxLines;
	private boolean mInitialized;
	private boolean mAllowResize = false;

	private int mLineCount;



	public AutoResizeTextView(Context inContext)
	{
		super(inContext);
		Initialize();
	}
	
	
	
	public AutoResizeTextView(Context inContext, AttributeSet inAttributes)
	{
		super(inContext, inAttributes);
		Initialize();
        GetLineCountAttribute(inAttributes);
	}
	
	
	
	public AutoResizeTextView(Context inContext, AttributeSet inAttributes, int inDefStyle)
	{
		super(inContext, inAttributes, inDefStyle);
		Initialize();
        GetLineCountAttribute(inAttributes);
	}
	
	
	
	@Override
	public void setText(final CharSequence inText, BufferType inType)
	{
		super.setText(inText, inType);

		if(inText != null)
			AdjustTextSize();
	}
	
	
	
	@Override
	public void setTextSize(float inSize)
	{
		if(!mAllowResize)
		{
			super.setTextSize(inSize);

			mMaxTextSize = getTextSize();

			return;
		}

		mMaxTextSize = inSize;
        mAlreadyTriedSizes.clear();
		
		if(mWidthLimit > kNoWidthLimit)
			AdjustTextSize();
	}
	
	
	
	@Override
	public void setMaxLines(int inMaxlines)
	{
		super.setMaxLines(inMaxlines);

		mMaxLines = inMaxlines;
		AdjustTextSize();
	}
	
	
	
	@Override
	public void setSingleLine()
	{
		super.setSingleLine();

		mMaxLines = 1;
		AdjustTextSize();
	}
	
	
	
	@Override
	public void setSingleLine(boolean inSingleLine)
	{
		super.setSingleLine(inSingleLine);
		
		if (inSingleLine)
			mMaxLines = 1;
		else
			mMaxLines = kNoLineLimit;
		
		AdjustTextSize();
	}
	
	
	
	@Override
	public void setLines(int inLines)
	{
		super.setLines(inLines);
		mMaxLines = inLines;
		AdjustTextSize();
	}
	
	
	
	@Override
	public void setTextSize(int inUnit, float inSize)
	{
		if(!mAllowResize)
		{
			super.setTextSize(inUnit, inSize);
			mMaxTextSize = getTextSize();
			return;
		}
		
		Context context = getContext();
		Resources resources;
		
		if (context == null)
            resources = Resources.getSystem();
		else
            resources = context.getResources();
		
		mMaxTextSize = TypedValue.applyDimension(inUnit, inSize, resources.getDisplayMetrics());
        mAlreadyTriedSizes.clear();
		AdjustTextSize();
	}
	
	
	
	@Override
	public void setLineSpacing(float inAdd, float inMult)
	{
		super.setLineSpacing(inAdd, inMult);
		mSpacingMult = inMult;
		mSpacingAdd = inAdd;
	}
	
	
	
	@Override
	protected void onTextChanged(final CharSequence inText, final int inStart, final int inBefore, final int inAfter)
	{
		super.onTextChanged(inText, inStart, inBefore, inAfter);
		AdjustTextSize();
	}
	
	
	
	@Override
	protected void onSizeChanged(int inWidth, int inHeight, int inOldWidth, int inOldHeight)
	{
        mAlreadyTriedSizes.clear();
		
		super.onSizeChanged(inWidth, inHeight, inOldWidth, inOldHeight);
		
		if (inWidth != inOldWidth || inHeight != inOldHeight)
			AdjustTextSize();
	}
	
	
	
	private void Initialize()
	{
		mPaint = new TextPaint(getPaint());
		
		mMaxTextSize = getTextSize();
		mAvailableSpaceRect = new RectF();
        mAlreadyTriedSizes = new TreeMap<Integer, Float>();

		if (mMaxLines == 0)
		{
			// no value was assigned during construction
			mMaxLines = kNoLineLimit;
		}

		mInitialized = true;
	}



    private void GetLineCountAttribute(AttributeSet inAttributes)
    {
        mLineCount = inAttributes.getAttributeIntValue("http://schemas.android.com/apk/res/android", "lines", 0);
    }
	
	
	
	public void AllowResize()
	{
		mAllowResize = true;
	}
	
	
	
	private void AdjustTextSize()
	{
		if(!mAllowResize || !mInitialized)
			return;
		
		int startSize = 0;
		int heightLimit = getMeasuredHeight() - getCompoundPaddingBottom() - getCompoundPaddingTop();
		mWidthLimit = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
		
		mAvailableSpaceRect.right = mWidthLimit;
		mAvailableSpaceRect.bottom = heightLimit;
		
		super.setTextSize(TypedValue.COMPLEX_UNIT_PX, TextSizeSearch(startSize, mMaxTextSize, mSizeTester, mAvailableSpaceRect));
	}
	
	
	
	private float TextSizeSearch(float inStart, float inEnd, ISizeTester inSizeTester, RectF inAvailableSpace)
	{
		String text = getText().toString();
		int key = ((text == null) ? 0 : text.length());
		float size = 0;
	    
		if(mAlreadyTriedSizes.keySet().contains(key))
			size = mAlreadyTriedSizes.get(key);
		
		if (size != 0)
			return size;
		
		size = BinarySearch(inStart, inEnd, inSizeTester, inAvailableSpace);
        mAlreadyTriedSizes.put(key, size);

		return size;
	}
	
	
	
	private float BinarySearch(float inStart, float inEnd, ISizeTester inSizeTester, RectF inAvailableSpace)
	{
		float lastBest = inStart;
		float lo = inStart;
		float hi = inEnd;
		float mid;

		if(inSizeTester.onTestSize(hi, inAvailableSpace) < 0)
			return hi;
		
		float offset = .125f;
		
		while (lo <= hi)
		{
			mid = (lo + hi) / 2;

			float midValueCompareResult = inSizeTester.onTestSize(mid, inAvailableSpace);

			if (midValueCompareResult < 0)
			{
				lastBest = lo;
			    lo = mid + offset;
			}
			else if(midValueCompareResult > 0)
			{
				hi -= offset;
			    lastBest = hi - offset;
			}
			else
		    	return mid;
		}

		return lastBest;
	}
	
	
	
	private interface ISizeTester
	{
		/**
		 * 
		 * @param inSuggestedSize
		 *            Size of text to be tested
		 * @param inAvailableSpace
		 *            available space in which text must fit
		 * @return an integer < 0 if after applying {@code suggestedSize} to
		 *         text, it takes less space than {@code availableSpace}, > 0
		 *         otherwise
		 */
		public float onTestSize(float inSuggestedSize, RectF inAvailableSpace);
	}
	
	
	
	private final ISizeTester mSizeTester = new ISizeTester()
	{
		@Override
		public float onTestSize(float inSuggestedSize, RectF inAvailableSpace)
		{
			mPaint.setTextSize(inSuggestedSize);
			String text = getText().toString();

			boolean singleline = (mLineCount == 0);
			
			if (singleline)
			{
				mTextRect.bottom = mPaint.getFontSpacing();
				mTextRect.right = mPaint.measureText(text);
			}
			else
			{
				StaticLayout layout = new StaticLayout(text, mPaint, mWidthLimit, Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, true);
				
				mPaint.setTypeface(getTypeface());
				
				String checkText = text.split("\n")[0];

				// return early if we have more lines
				while(layout.getLineCount() < mLineCount)
				{
					checkText += "\nl";
					layout = new StaticLayout(checkText, mPaint, mWidthLimit, Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, true);
				}
				
				if (mLineCount != kNoLineLimit && layout.getLineCount() > mLineCount)
					return 1;
				
				mTextRect.bottom = layout.getHeight();

				int maxWidth = -1;
				
				for (int i = 0; i < layout.getLineCount(); i++)
				{
					if (maxWidth < layout.getLineWidth(i))
						maxWidth = (int) layout.getLineWidth(i);
				}
				
				mTextRect.right = maxWidth;
			}
			
			mTextRect.offsetTo(0, 0);

			if (inAvailableSpace.contains(mTextRect))
			{
				// may be too small, but this will find the best match
				
				String[] splitString = text.split(" ");
				
				for(String textTest : splitString)
				{
                    // start with double each word's current amount of space and work downwards to find each word's maximum fit
					StaticLayout layout = new StaticLayout(textTest + " " + textTest, mPaint, mWidthLimit, Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, true);
					
					RectF singleSpace = new RectF(inAvailableSpace);
					singleSpace.bottom /= mLineCount;
					singleSpace.bottom *= 2;
					
					RectF singleRect = new RectF(mTextRect);
					singleRect.bottom = layout.getHeight();
					
					int maxWidth = -1;
					for (int j = 0; j < layout.getLineCount(); j++)
					{
						if (maxWidth < layout.getLineWidth(j))
							maxWidth = (int) layout.getLineWidth(j);
					}
					
					singleRect.right = maxWidth;
					
					if (!singleSpace.contains(singleRect))
						return 1;
				}
				
				return -1;
			}
			else
			{
				// too big
				return 1;
			}
		}
	};
}
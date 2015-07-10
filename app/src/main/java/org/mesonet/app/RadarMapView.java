package org.mesonet.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.Projection;



public class RadarMapView extends MapView
{
	private static final int kAccuracy = 5;
	
	private static Paint sPaint;
    private static Paint sDrawingPaint;
	
	private static boolean sLooping = false;
	private static int sCurrentLoop = 0;
	private static int sWait = 0;

    private static Rect sDrawRect = null;

    private static Bitmap sFinalImage = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888);
    private static Bitmap sDrawingImage = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888);

    public static String sCurrentTime = null;
	
	
	
	public RadarMapView(Context inContext)
	{
		super(inContext);
		Init();
	}
	
	
	
	public RadarMapView(Context inContext, AttributeSet inAttrs)
	{
		super(inContext, inAttrs);
		Init();
	}
	
	
	
	public RadarMapView(Context inContext, AttributeSet inAttrs, int inDefStyle)
	{
		super(inContext, inAttrs, inDefStyle);
		Init();
	}



	public RadarMapView(Context inContext, GoogleMapOptions inOptions)
	{
		super(inContext, inOptions);
		Init();
	}
	
	
	
	public static void Init()
	{
		sPaint = new Paint();
        sDrawingPaint = new Paint();
	}
	
	
	
	public static void Play()
	{
		sLooping = true;
	}
	
	
	
	public static void Pause()
	{
		sLooping = false;
	}



    @Override
	public void computeScroll()
	{
		super.computeScroll();
        invalidate();
	}
	
	

	@Override
	public void dispatchDraw(Canvas inCanvas)
	{
		super.dispatchDraw(inCanvas);

        if(inCanvas != null && getMap() != null)
        {
            Projection projection = getMap().getProjection();

            if (projection != null)
            {
                sPaint.setAlpha(RadarData.Transparency());

                Rect newDrawRect = RadarData.GetArea(0, kAccuracy, projection);

                if(sDrawRect == null || !newDrawRect.equals(sDrawRect)) {
                    sDrawRect = newDrawRect;
                    RadarFragment.TurnOffTransparencyLayout();
                }

                if(sFinalImage != null) {
                    for (int i = 0; i < kAccuracy; i++)
                        inCanvas.drawBitmap(sFinalImage, new Rect(0, i * (sFinalImage.getHeight() / kAccuracy), sFinalImage.getWidth() - 1, ((i + 1) * (sFinalImage.getHeight() / kAccuracy)) - 1), RadarData.GetArea(i, kAccuracy, projection), sPaint);

                    if (sCurrentTime != null)
                        RadarFragment.SetTimeText(sCurrentTime, RadarData.FileUpdate());
                }
            }

            if (sLooping)
            {
                sWait++;

                if (sWait == 20)
                {
                    sCurrentLoop--;

                    if (sCurrentLoop < 0)
                        sCurrentLoop = RadarData.kLoopImageCount - 1;

                    sWait = 0;

                    SetImage();
                }
            }
        }
	}



    public synchronized static void SetImage()
    {
        if(sFinalImage == null)
            sFinalImage = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888);
        if(sDrawingImage == null)
            sDrawingImage = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888);

        RadarData.RadarImageInfo info = RadarData.GetImage(sCurrentLoop);

        if(info == null || info.mPixels.isEmpty())
            return;

        sDrawingImage.eraseColor(0);

        for(int i = 0; i < info.mPixels.size(); i++)
            sDrawingImage.setPixel(info.mPixels.get(i).mX, info.mPixels.get(i).mY, info.mPixels.get(i).mColor);

        sFinalImage.eraseColor(0);

        Canvas canvas = new Canvas(sFinalImage);

        Rect area = new Rect(0, 0, sDrawingImage.getWidth(), sDrawingImage.getHeight());

        canvas.drawBitmap(sDrawingImage, area, area, sDrawingPaint);

        canvas.save();

        sCurrentTime = info.mFilepath;
    }



    public synchronized static void ClearImages()
    {
        if(sDrawingImage != null) {
            sDrawingImage.recycle();
            sDrawingImage = null;
        }
        if(sFinalImage != null) {
            sFinalImage.recycle();
            sFinalImage = null;
        }
    }
}

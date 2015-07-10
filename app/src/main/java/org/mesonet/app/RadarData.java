package org.mesonet.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;



public class RadarData implements FilenameFilter
{
	public static final int kLoopImageCount = 6;
	public static final int kDefaultImageAlpha = 191; // roughly 70% opacity
	
	private static TreeMap<String, RadarBoundsData> sRadarList = new TreeMap<String, RadarBoundsData>();
	
	private static RadarImageInfo[] sRadarImages = new RadarImageInfo[kLoopImageCount];
    private static String[] sCurrentImagePaths = new String[kLoopImageCount];

    private static String sXmlData = null;
	
	private static String sRadarCity;
	
	private static Timer sUpdateTimer = null;

    private static boolean sShouldHaveCleared = false;

    private static RadarXmlUpdater sXmlUpdater = new RadarXmlUpdater();
    private static RadarImageUpdater sImageUpdater = new RadarImageUpdater();

    private static Date sLastUpdate = null;
    private static Date sFileUpdate = null;
	
	
	
	public static class RadarBoundsData
	{
		String mRadarName;
		float mLatitude;
		float mLongitude;
		float mLatDelta;
		float mLonDelta;
	}
	
	
	
	public static void Initialize()
	{
		DefaultHandler handler = new DefaultHandler()
		{
			private String mDataType = "";
			private String mKeyOrString = "";
			private String mCurrentRadar;
			private boolean mNextIsName = true;
			RadarBoundsData mCurrentData = new RadarBoundsData();



			@Override
			public void startElement(String inUri, String inLocalName, String inQName, Attributes inAttributes) throws SAXException
			{
				mKeyOrString = inLocalName;
			}
		 
		 
		 
		 	@Override
			public void endElement(String inUri, String inLocalName, String inQName) throws SAXException
			{
			}
		 
		 
		 
		 	@Override
			public void characters(char inData[], int inStart, int inLength) throws SAXException
			{
				if(Character.isWhitespace(inData[inStart]))
					return;
				
				if(mKeyOrString.compareTo("key") == 0)
				{
					mDataType = new String(inData, inStart, inLength);
					
					if(mNextIsName)
					{
						mCurrentData.mRadarName = mDataType;
						mNextIsName = false;
					}
				}
				
				else if(mKeyOrString.compareTo("string") == 0 || mKeyOrString.compareTo("real") == 0)
				{
					if(mDataType.compareTo("name") == 0)
						mCurrentRadar = new String(inData, inStart, inLength);
					else if(mDataType.compareTo("latitude") == 0)
						mCurrentData.mLatitude = Float.parseFloat(new String(inData, inStart, inLength));
					else if(mDataType.compareTo("longitude") == 0)
						mCurrentData.mLongitude = Float.parseFloat(new String(inData, inStart, inLength));
					else if(mDataType.compareTo("latDelta") == 0)
						mCurrentData.mLatDelta = Float.parseFloat(new String(inData, inStart, inLength));
					else if(mDataType.compareTo("lonDelta") == 0)
					{
						mCurrentData.mLonDelta = Float.parseFloat(new String(inData, inStart, inLength));
						
						sRadarList.put(mCurrentRadar, mCurrentData);
						mCurrentData = new RadarBoundsData();
						mNextIsName = true;
					}
				}
			}
		};

		try
		{
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(SavedDataManager.GetRawResource(R.raw.radar_list), handler);

			SetLocation(SavedDataManager.GetStringSetting("radar", SavedDataManager.GetStringResource(R.string.default_radar)));
		}
		catch(Exception exception)
		{
            exception.printStackTrace();
		}
	}
	
	
	
	@Override
	public boolean accept(File dir, String name)
	{
		Vector<String> keyList = GetKeyList();
		
		for(int i = 0; i < sRadarList.size(); i++)
		{
			if(sRadarList.get(keyList.get(i)).mRadarName.compareTo(name.substring(0, 4)) == 0)
			{
				String timeString = name.substring(4, name.length() - 4);
				
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
				Date fileDate = null;
				
				try
				{
					fileDate = dateFormatter.parse(timeString);
				}
				catch(Exception exception)
				{
					exception.printStackTrace();
				}
				
				Date anHourAgo = null;
				
				Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
				calendar.setTime(new Date());				
				calendar.add(Calendar.MINUTE, -70);
				timeString = String.format("%4d", calendar.get(Calendar.YEAR)) + String.format("%2d", calendar.get(Calendar.MONTH) + 1) + String.format("%2d", calendar.get(Calendar.DAY_OF_MONTH)) + String.format("%2d", calendar.get(Calendar.HOUR_OF_DAY)) + String.format("%2d", calendar.get(Calendar.MINUTE));
				
				try
				{
					anHourAgo = dateFormatter.parse(timeString);
				}
				catch(Exception exception)
				{
					exception.printStackTrace();
				}

                if(fileDate != null)
				    return fileDate.before(anHourAgo);
			}
		}
		return false;
	}
	
	
	
	public static void StartDownload(boolean inForce)
	{
        if(inForce || sUpdateTimer == null) {
            StopDownload();

            if(sShouldHaveCleared)
                ClearImages();

            long nextDlTime = 0;
            Date now = new Date();
            if (sRadarImages[0] != null && !inForce && sLastUpdate != null && (now.getTime() - sLastUpdate.getTime()) < DataContainer.kOneMinute)
                nextDlTime = now.getTime() - sLastUpdate.getTime();

            if(sImageUpdater.Activated()) {
                sImageUpdater.cancel(true);
                sImageUpdater = new RadarImageUpdater();
            }

            if(sUpdateTimer != null)
                sUpdateTimer.cancel();

            sUpdateTimer = new Timer();
            sUpdateTimer.schedule(new RadarUpdateTimer(), nextDlTime, DataContainer.kOneMinute);
        }
	}
	
	
	
	public static void StopDownload()
	{
		if(sUpdateTimer != null)
			sUpdateTimer.cancel();

        sUpdateTimer = null;
	}



    public static void ResetDownloads()
    {
        if(sImageUpdater.Activated()) {
            sImageUpdater.cancel(true);

            ClearImages();
        }

        sXmlUpdater.Cancel();
        sXmlUpdater.SetUpdateTime(new Date(0));

        sImageUpdater = new RadarImageUpdater();
    }
	
	
	
	public static Vector<String> GetKeyList()
	{
		Set<String> keySet = sRadarList.keySet();
		Vector<String> keyList = new Vector<String>(keySet.size());
		keyList.addAll(sRadarList.keySet());
		
		return keyList;
	}
	
	
	
	public static ArrayAdapter<Object> GetListAdapter()
	{
		return new ArrayAdapter<Object>(MesonetApp.Activity(), android.R.layout.simple_spinner_item, GetKeyList().toArray());
    }



    public static Date LastUpdate()
    {
        return sLastUpdate;
    }



    public static Date FileUpdate()
    {
        return sFileUpdate;
    }
	
	
	
	public static RadarBoundsData GetCurrentData()
	{
		return sRadarList.get(sRadarCity);
	}



    public synchronized static void ClearImages() {
        sShouldHaveCleared = true;
        for (int i = 0; i < kLoopImageCount; i++)
            sRadarImages[i] = null;

        System.gc();
        sShouldHaveCleared = false;

        RadarMapView.ClearImages();
    }

	
	public static boolean SetLocation(String inRadarName)
	{
        if(sRadarCity == null || sRadarCity.compareTo(inRadarName) != 0) {
            RadarMapView.sCurrentTime = null;
            ClearImages();
            sRadarCity = inRadarName;

            StartDownload(true);

            return true;
        }

        return false;
	}
	
	
	
	public static Rect GetArea(int inArea, int inAccuracy, Projection inProjection)
	{
		RadarBoundsData radarData = GetCurrentData();
		
		float mDelta = radarData.mLatDelta / inAccuracy;
		
		LatLng topLeftLatLng = new LatLng((radarData.mLatitude + (radarData.mLatDelta / 2)) - (mDelta * inArea), radarData.mLongitude - (radarData.mLonDelta / 2));
		LatLng bottomRightLatLng = new LatLng(topLeftLatLng.latitude - mDelta, radarData.mLongitude + (radarData.mLonDelta / 2));
		
		Point topLeft = inProjection.toScreenLocation(topLeftLatLng);
		Point bottomRight = inProjection.toScreenLocation(bottomRightLatLng);
		
		return new Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
	}
	
	
	
	public static void SetTransparency(int inTransparency)
	{
		SavedDataManager.SaveIntSetting("RadarTransparency", inTransparency);
	}
	
	
	
	public synchronized static RadarImageInfo GetImage(int inIndex) {
        /*for(int i = inIndex; i >= 0; i--)
        {
            if (sRadarImages == null || sRadarImages[i] == null)
                continue;

            boolean found = false;

            for (String path : sCurrentImagePaths) {

                String[] splitPath = path.split("/");

                if (sRadarImages[i] != null && sRadarImages[i].mFilepath.compareTo(splitPath[splitPath.length - 1]) == 0) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                sRadarImages[i] = null;
            }

            if(i < sRadarImages.length)*/
                return sRadarImages[inIndex];
        //}

		//return null;
	}
	
	
	
	public static String GetCity()
	{
		return sRadarCity;
	}
	
	
	
	public static int Transparency()
	{
		return SavedDataManager.GetIntSetting("RadarTransparency", kDefaultImageAlpha);
	}
	
	
	
	public static int GetNearestRadarLocation(Location inLocation)
	{
		Location locationCompare = new Location(LocationManager.NETWORK_PROVIDER);
		
		float smallestDistance = 999999;
		int index = -1;
		
		Object[] keys = sRadarList.keySet().toArray();

		for(int i = 0; i < sRadarList.size(); i++)
		{
			RadarBoundsData radarData = sRadarList.get(keys[i]);
			
			locationCompare.setLatitude(radarData.mLatitude);
			locationCompare.setLongitude(radarData.mLongitude);
			
			float distanceCheck = inLocation.distanceTo(locationCompare);
			
			if(distanceCheck < smallestDistance)
			{
				index = i;
				smallestDistance = distanceCheck;
			}
		}
		
		return index;
	}
	
	
	
	public static int GetCityIndex()
	{
		Set<String> keySet = sRadarList.keySet();
		Vector<String> keyList = new Vector<String>(keySet.size());
		keyList.addAll(sRadarList.keySet());
		
		return keyList.indexOf(sRadarCity);
	}
	
	
	
	private static class RadarUpdateTimer extends TimerTask
	{
	    public void run()
	    {
            Formatter formatter = new Formatter();
            String urlResult = formatter.format(SavedDataManager.GetUrl(R.string.radar_xml_url), sRadarList.get(sRadarCity).mRadarName).toString();
            formatter.close();

            sXmlUpdater.Update(urlResult, false, true);
	    }
	}
	
	
	
	private static class RadarXmlUpdater extends DataDownloader
	{
        @Override
        protected void PostExecute(DownloadTask.ResultParms inResult)
        {
            Date fileDate = FileDate();

            if(fileDate != null && (sFileUpdate == null || fileDate.getTime() != 0))
                sFileUpdate = fileDate;

            if(inResult != null && inResult.mData.length() > 0)
                sXmlData = inResult.mData;

            else if(sRadarImages[0] != null && sRadarImages[kLoopImageCount - 1] != null)
                return;

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            InputSource input;

            try
            {
                db = dbf.newDocumentBuilder();
                StringReader reader = new StringReader(sXmlData);
                input = new InputSource(reader);
                Document xmlDocument = db.parse(input);

                NodeList nodeList = xmlDocument.getElementsByTagName("frame");

                for(int i = 0; i < kLoopImageCount; i++) {
                    if (Cancelled())
                        return;

                    Node node = nodeList.item(i);
                    NamedNodeMap nodeMap = node.getAttributes();
                    Node filename = nodeMap.getNamedItem("filename");
                    sCurrentImagePaths[i] = filename.getNodeValue();
                }

                sLastUpdate = new Date();
            }
            catch(Exception exception)
            {
                exception.printStackTrace();
            }

            if(Cancelled())
                return;

            if(!sImageUpdater.Activated())
                sImageUpdater.execute();
        }
	}

	
	
	private static class RadarImageUpdater extends AsyncTask<String, InputStream, Object>
	{
        private boolean mActivated = false;



		@Override
		protected Object doInBackground(String... inRadarName)
		{
            mActivated = true;

			try
			{
                for(int i = 0; i < sRadarImages.length; i++)
                {
                    boolean found = false;

                    if(sRadarImages[i] != null) {
                        for (int j = 0; j < sRadarImages.length; j++) {
                            if (isCancelled())
                                return null;

                            if (sRadarImages[i].mFilepath.compareTo(sCurrentImagePaths[j]) == 0) {
                                if (isCancelled())
                                    return null;

                                sRadarImages[i].SetInfo(sRadarImages[j]);

                                found = true;
                                break;
                            }
                        }
                    }

                    if(found)
                        continue;

                    RadarImageInfo resultImage = ReadImage(i);

                    if(resultImage == null)
                        break;

                    if(isCancelled())
                        return null;

                    sRadarImages[i] = resultImage;

                    RadarMapView.SetImage();

                    System.gc();
                }
			} 
			catch (Exception exception)
			{
                exception.printStackTrace();
			}

			return null;
		}
		
		
		
		@Override
		protected void onPostExecute(Object inResult)
        {
            sImageUpdater = new RadarImageUpdater();

            RadarMapView.SetImage();
		}



        public boolean Activated()
        {
            return mActivated;
        }
		
		
		
		private RadarImageInfo ReadImage(int inIndex) throws IOException
		{
            if(isCancelled())
                return null;

            String gifUrl = SavedDataManager.GetUrl(R.string.mesonet_url) + sCurrentImagePaths[inIndex];
            String[] splitUrl = gifUrl.split("/");
            File cachedImage = new File(SavedDataManager.CacheDir().getPath() + "/" + sRadarList.get(sRadarCity).mRadarName + splitUrl[splitUrl.length - 1]);

            if(!cachedImage.exists())
            {
                gifUrl = gifUrl.replace(".gif", ".fullrange.gif");
                URL url = new URL(gifUrl);
                URLConnection connection = url.openConnection();
                InputStream input = connection.getInputStream();
                byte[] buffer = new byte[4096];

                int n;

                OutputStream output = new FileOutputStream( cachedImage );

                while ( (n = input.read(buffer)) != -1)
                {
                    if(isCancelled()) {
                        input.close();
                        output.close();
                        cachedImage.delete();
                        return null;
                    }

                    if (n > 0)
                    {
                        output.write(buffer, 0, n);
                    }
                }
                output.close();
                input.close();
            }

            Bitmap image = Decode(new FileInputStream(cachedImage));

            if(image == null)
                return null;

            return new RadarImageInfo(image, splitUrl[splitUrl.length - 1]);
		}
		
		
		
		private static Bitmap Decode(InputStream inStream) throws IOException
		{
            try {
                Bitmap result = BitmapFactory.decodeStream(inStream);
                return result;
            }
            catch(Exception exception)
            {
                return null;
            }
		}
	}



    public static class RadarImageInfo
    {
        public ArrayList<RadarPixel> mPixels = new ArrayList<RadarPixel>();
        public String mFilepath;



        public RadarImageInfo(Bitmap inImage, String inFilepath)
        {
            for(int x = 0; x < inImage.getWidth(); x++)
            {
                for(int y = 0; y < inImage.getHeight(); y++)
                {
                    int color = inImage.getPixel(x, y);

                    if(color != 0 && color != (255 << 24))
                        mPixels.add(new RadarPixel(x, y, color));
                }
            }

            inImage.recycle();
            mFilepath = inFilepath;
        }



        public void SetInfo(RadarImageInfo inCopy)
        {
            int i;
            for(i = 0; i < inCopy.mPixels.size() && i <mPixels.size(); i++)
                mPixels.set(i, inCopy.mPixels.get(i));

            for(; i < inCopy.mPixels.size(); i++)
                mPixels.add(i, inCopy.mPixels.get(i));

            for(; i < mPixels.size(); i++)
                mPixels.remove(i);
        }
    }



    public static class RadarPixel
    {
        public int mX, mY, mColor;



        public RadarPixel(int inX, int inY, int inColor)
        {
            mX = inX;
            mY = inY;
            mColor = inColor;
        }
    }
}
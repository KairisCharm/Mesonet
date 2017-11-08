package org.mesonet.app;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Scanner;



abstract class DataDownloader
{
    private DownloadTask mTask =
            null;
    private Date mFileDate;



    protected boolean DoUpdate(final DownloadTask.DownloadInput inParms) {
        if(mTask == null)
            NewInstance(new Date(0));

        if(mTask.Activated())
            return false;

        if (!inParms.ForceUpdate() && (!mTask.NeedsUpdate() || inParms.SavedFile() != null))
            return true;

        ConnectivityManager connMgr = (ConnectivityManager) MesonetApp.Context().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (!mTask.Activated() && networkInfo != null && networkInfo.isConnected()) {
            mTask.mActivated = true;
            mTask.execute(inParms);
        }

        return true;
    }



    public Date Time()
    {
        return mTask.mUpdateTime;
    }



    public boolean Cancelled()
    {
        return mTask.isCancelled();
    }



    public Date FileDate()
    {
        return mFileDate;
    }



    protected void NewInstance(Date inDate)
    {
        mTask = new DownloadTask(this, inDate);
    }



    protected void SetUpdateTime(Date inDate)
    {
        if(mTask == null)
            NewInstance(inDate);
        else
            mTask.mUpdateTime = inDate;
    }



    public void Cancel()
    {
        if(mTask != null && mTask.Activated()) {
            mTask.cancel(true);
            NewInstance(new Date(0));
        }
    }



    protected abstract void PostExecute(DownloadTask.ResultParms inResult);
    //public abstract void Update();
    //public abstract void Update(boolean inForceUpdate);



    public void Update(String inUrl, boolean inForceUpdate, boolean inPostExecuteWhenNull)
    {
        Update(inUrl, null, inForceUpdate, inPostExecuteWhenNull);
    }



    public void Update(String inUrl, String inFilePath, boolean inForceUpdate, boolean inPostExecuteWhenNull)
    {
        Update(inUrl, inFilePath, inForceUpdate, null, inPostExecuteWhenNull);
    }



    public void Update(String inUrl, String inFilePath, boolean inForceUpdate, WidgetProvider inProvider, boolean inPostExecuteWhenNull)
    {
        DoUpdate(new DataDownloader.DownloadTask.DownloadInput(inUrl, inFilePath, inForceUpdate, inProvider, inPostExecuteWhenNull));
    }



    protected static class DownloadTask extends AsyncTask<DownloadTask.DownloadInput, DownloadTask.ResultParms, DownloadTask.ResultParms> {
        private static final int kReadTimeOut = 10000;        //	time in milliseconds
        private static final int kConnectTimeOut = 15000;    //	time in milliseconds

        private boolean mActivated = false;
        private boolean mPostExecuteWhenNull = false;
        protected Date mUpdateTime;

        protected DataDownloader mDownloader;



        public DownloadTask(DataDownloader inDownloader, Date inUpdateTime) {
            mUpdateTime = new Date(inUpdateTime.getTime());
            mDownloader = inDownloader;
        }



        protected boolean NeedsUpdate() {
            return (new Date().getTime() - mUpdateTime.getTime()) > 60000;
        }


        protected boolean Activated() {
            return mActivated;
        }


        public Date GetDate(ResultParms inParms) {
            if (inParms == null)
                return mUpdateTime;

            return new Date(inParms.mLastModified);
        }


        @Override
        protected ResultParms doInBackground(DownloadInput... inParms) {
            InputStream input = null;
            HttpURLConnection conn = null;

            mPostExecuteWhenNull = inParms[0].mPostExecuteWhenNull;

            String resultString = "";

            if (isCancelled())
                return null;

            ActivityManager memoryChecker = (ActivityManager)MesonetApp.Application().getSystemService(Context.ACTIVITY_SERVICE);

            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            memoryChecker.getMemoryInfo(memoryInfo);

            if(memoryInfo.lowMemory)
                return null;

            if (inParms[0].SavedFile() != null) {
                try {
                    Scanner reader = new Scanner(inParms[0].SavedFile());

                    while (reader.hasNextLine()) {
                        resultString = reader.nextLine() + '\n';

                        memoryChecker.getMemoryInfo(memoryInfo);

                        if(memoryInfo.lowMemory)
                            return null;
                    }

                    reader.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            try {
                URL url = new URL(inParms[0].Url());
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(kReadTimeOut);
                conn.setConnectTimeout(kConnectTimeOut);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                if (!inParms[0].mForceUpdate)
                    conn.setIfModifiedSince(mUpdateTime.getTime());

                if (isCancelled())
                    return null;

                conn.connect();

                if (isCancelled())
                    return null;

                mDownloader.mFileDate = new Date(conn.getLastModified());

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    resultString = "";
                    input = conn.getInputStream();

                    Scanner scanner = new Scanner(conn.getInputStream());

                    while (scanner.hasNextLine()) {
                        if (isCancelled())
                            return null;

                        memoryChecker.getMemoryInfo(memoryInfo);

                        if(memoryInfo.lowMemory)
                            return null;

                        resultString += scanner.nextLine() + '\n';
                    }

                    scanner.close();

                    conn.disconnect();
                }
                if(resultString.compareTo("") != 0)
                    return new ResultParms(resultString, mDownloader.mFileDate.getTime(), inParms[0].mWidget);

            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            } finally {
                if (conn != null)
                    conn.disconnect();
                if (input != null) {
                    try {
                        input.close();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }

            return null;
        }



        @Override
        public void onPostExecute(ResultParms inParms)
        {
            mDownloader.NewInstance(GetDate(inParms));

            if((inParms == null && !mPostExecuteWhenNull) || (inParms != null && inParms.mData.length() == 0))
                return;

            mDownloader.PostExecute(inParms);
        }



        @Override
        public void onCancelled(ResultParms inParms)
        {
            mDownloader.NewInstance(mUpdateTime);
        }


        public static class DownloadInput {
            private String mSavedFile;
            private String mUrl;
            private boolean mForceUpdate = false;
            private WidgetProvider mWidget;
            private boolean mPostExecuteWhenNull = false;


            public DownloadInput(String inUrl, String inSavedFile) {
                mUrl = inUrl;
                mSavedFile = inSavedFile;
                mWidget = null;
            }


            public DownloadInput(String inUrl, String inSavedFile, boolean inForceUpdate) {
                this(inUrl, inSavedFile);
                mForceUpdate = inForceUpdate;
            }



            public DownloadInput(String inUrl, String inSavedFile, boolean inForceUpdate, WidgetProvider inWidget, boolean inPostExecuteWhenNull)
            {
                this(inUrl, inSavedFile, inForceUpdate);
                mWidget = inWidget;
                mPostExecuteWhenNull = inPostExecuteWhenNull;
            }



            public String Url() {
                return mUrl;
            }


            public String SavedFile() {
                return mSavedFile;
            }


            public boolean ForceUpdate() {
                return mForceUpdate;
            }
        }


        public static class ResultParms {
            public String mData;
            public long mLastModified;
            public WidgetProvider mWidget;


            public ResultParms(String inData, long inLastModified, WidgetProvider inWidget) {
                mData = inData;
                mLastModified = inLastModified;
                mWidget = inWidget;
            }
        }
    }
}

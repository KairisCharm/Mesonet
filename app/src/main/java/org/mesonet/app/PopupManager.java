package org.mesonet.app;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.Scanner;



public class PopupManager
{
    public static AlertDialog Popup(int inLayoutId, int inTitleId)
    {
        return Popup(MesonetApp.Activity().getLayoutInflater().inflate(inLayoutId, null), inTitleId);
    }



    public static AlertDialog Popup(View inView, int inTitleId)
    {
        AlertDialog popup = new AlertDialog.Builder(new ContextThemeWrapper(MesonetApp.Context(), android.R.style.Theme_Dialog)).create();

        popup.setTitle(inTitleId);
        popup.setView(inView);
        popup.setButton(AlertDialog.BUTTON_POSITIVE, SavedDataManager.GetStringResource(R.string.popup_close_button), new DialogInterface.OnClickListener()
        {

            public void onClick(DialogInterface dialog, int id)
            {
            }
        });

        popup.show();

        return popup;
    }



    public static void ShowChanges()
    {
        String version = "";

        try
        {
            version = MesonetApp.Activity().getPackageManager().getPackageInfo(MesonetApp.Activity().getPackageName(), 0).versionName;
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }

        if(SavedDataManager.GetBooleanResource(R.bool.show_updates) && SavedDataManager.GetStringSetting("CurrentVersion", "").compareTo(version) != 0)
        {
            AlertDialog changesPopup = new AlertDialog.Builder(MesonetApp.Activity()).create();

            LayoutInflater inflater = MesonetApp.Activity().getLayoutInflater();
            View popupView = inflater.inflate(R.layout.ticker_about_changes_layout, null);

            String text = "";
            Scanner scanner = new Scanner(SavedDataManager.GetRawResource(R.raw.changes));

            while(scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                text += line;
            }

            scanner.close();

            ((WebView)popupView.findViewById(R.id.popup_view)).loadData(text, "text/html", "utf-8");

            changesPopup.setTitle("Updates");
            changesPopup.setView(popupView);
            changesPopup.setButton(AlertDialog.BUTTON_POSITIVE, SavedDataManager.GetStringResource(R.string.popup_close_button), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                }
            });

            changesPopup.show();

            SavedDataManager.SaveStringSetting("CurrentVersion", version);
        }
    }
}

package org.mesonet.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.util.Calendar;
import java.util.Scanner;



public class MainMenu
{
    public enum UnitSystem {kImperial, kMetric}

    final static String kUnits = "units";

    private MenuItem mTransparencyMenuItem;



    public static void GenerateMenu(Menu inMenu)
    {
        inMenu.add(SavedDataManager.GetStringResource(R.string.settings_english));
        inMenu.add(SavedDataManager.GetStringResource(R.string.settings_metric));
        This().mTransparencyMenuItem = inMenu.add(SavedDataManager.GetStringResource(R.string.settings_radar_trans));
        inMenu.add(SavedDataManager.GetStringResource(R.string.settings_ticker));
        inMenu.add(SavedDataManager.GetStringResource(R.string.settings_contact));
        inMenu.add(SavedDataManager.GetStringResource(R.string.settings_about));

        This().mTransparencyMenuItem.setVisible(false);

        if(MesonetActionBar.GetSelectedTab().compareTo(MesonetActionBar.kRadarTag) == 0)
            This().mTransparencyMenuItem.setVisible(true);
    }



    public static void CheckRadarTransparencyItem(ActionBar.Tab inTab)
    {
        if(This().mTransparencyMenuItem != null)
        {
            This().mTransparencyMenuItem.setVisible(false);

            if(((String)inTab.getTag()).compareTo(MesonetActionBar.kRadarTag) == 0)
                This().mTransparencyMenuItem.setVisible(true);
        }
    }



    public static UnitSystem GetUnitSystem()
    {
        UnitSystem result = UnitSystem.kImperial;
        try {
            result = UnitSystem.valueOf(MesonetApp.Context().getSharedPreferences("MainActivity", Activity.MODE_PRIVATE).getString(kUnits, UnitSystem.kImperial.toString()));
        }
        catch(Exception exception)
        {
            try {
                result = UnitSystem.values()[MesonetApp.Context().getSharedPreferences("MainActivity", Activity.MODE_PRIVATE).getInt(kUnits, UnitSystem.kImperial.ordinal())];
            }
            catch (Exception exception2)
            {
                exception2.printStackTrace();;
            }
        }
        return result;
    }



    public static void SelectMenuItem(MenuItem inItem)
    {
        if(inItem.getTitle().toString().compareTo(SavedDataManager.GetStringResource(R.string.settings_english)) == 0)
        {
            SavedDataManager.SaveStringSetting(kUnits, UnitSystem.kImperial.toString());
            LocalFragment.ClearAllFields();
            LocalFragment.SetTextsAndImages();
        }
        else if(inItem.getTitle().toString().equals(SavedDataManager.GetStringResource(R.string.settings_metric)))
        {
            SavedDataManager.SaveStringSetting(kUnits, UnitSystem.kMetric.toString());
            LocalFragment.ClearAllFields();
            LocalFragment.SetTextsAndImages();
        }
        else if(inItem.getTitle().toString().equals(SavedDataManager.GetStringResource(R.string.settings_radar_trans)))
        {
            RadarFragment.ShowTransparencyTracker();
        }
        else if(inItem.getTitle().toString().equals(SavedDataManager.GetStringResource(R.string.settings_ticker)))
        {
            LayoutInflater inflater = MesonetApp.Activity().getLayoutInflater();
            View popupView = inflater.inflate(R.layout.ticker_about_changes_layout, null);

            WebView tickerText = ((WebView)popupView.findViewById(R.id.popup_view));
            tickerText.getSettings().setUserAgentString(MesonetApp.UserAgentString());
            tickerText.loadUrl(SavedDataManager.GetUrl(R.string.ticker_url));

            PopupManager.Popup(popupView, R.string.popup_ticker_title);
        }
        else if(inItem.getTitle().toString().compareTo(SavedDataManager.GetStringResource(R.string.settings_contact)) == 0)
        {
            AlertDialog popup = PopupManager.Popup(R.layout.contact_layout, R.string.popup_contact_title);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(popup.getWindow().getAttributes());

            lp.width = Math.round(SavedDataManager.GetDimenResource(R.dimen.contact_popup_width));
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

            popup.getWindow().setAttributes(lp);
        }
        else if(inItem.getTitle().toString().compareTo(SavedDataManager.GetStringResource(R.string.settings_about)) == 0)
        {
            LayoutInflater inflater = MesonetApp.Activity().getLayoutInflater();
            View popupView = inflater.inflate(R.layout.ticker_about_changes_layout, null);

            String text = "";
            Scanner scanner = new Scanner(SavedDataManager.GetRawResource(R.raw.meso_about));

            String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
            String appVersion;
            try
            {
                appVersion = MesonetApp.Activity().getPackageManager().getPackageInfo(MesonetApp.Activity().getPackageName(), 0).versionName;
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
                appVersion = "??";
            }

            while(scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                line = line.replace("%year;", year);
                line = line.replace("%version;", appVersion);
                text += line;
            }

            scanner.close();

            ((WebView)popupView.findViewById(R.id.popup_view)).loadData(text, "text/html", "utf-8");

            PopupManager.Popup(popupView, R.string.settings_about);
        }
    }



    private static MainMenu This()
    {
        return MainActivity.Menu();
    }
}

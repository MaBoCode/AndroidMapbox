package fr.example.androidmapbox;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.MaterialColors;

public abstract class ThemeUtils {

    public static boolean isDarkThemeEnabled(Resources resources) {
        int nightModeFlag = resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlag == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void switchTheme(Resources resources) {
        int nightModeFlag = resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlag) {
            case Configuration.UI_MODE_NIGHT_YES:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Configuration.UI_MODE_NIGHT_NO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    public static int getThemeColor(Context context, int attr) {
        return MaterialColors.getColor(context, attr, Color.GRAY);
    }

}

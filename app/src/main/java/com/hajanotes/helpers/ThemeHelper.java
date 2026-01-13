package com.hajanotes.helpers;

import android.content.Context;
import com.hajanotes.R;

public class ThemeHelper {
    
    public static void applyTheme(Context context, String theme) {
        switch (theme.toLowerCase()) {
            case "green":
                context.setTheme(R.style.Theme_HajaNotes_Green);
                break;
            case "purple":
                context.setTheme(R.style.Theme_HajaNotes_Purple);
                break;
            case "orange":
                context.setTheme(R.style.Theme_HajaNotes_Orange);
                break;
            case "red":
                context.setTheme(R.style.Theme_HajaNotes_Red);
                break;
            case "blue":
            default:
                context.setTheme(R.style.Theme_HajaNotes);
                break;
        }
    }
}
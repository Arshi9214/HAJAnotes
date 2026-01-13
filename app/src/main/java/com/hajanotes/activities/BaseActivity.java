package com.hajanotes.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import com.hajanotes.helpers.AppSettingsInit;
import com.hajanotes.helpers.ThemeHelper;
import com.hajanotes.helpers.Utils;

/**
 * 
 */
public abstract class BaseActivity extends AppCompatActivity {

	protected Context ctx = this;
	protected Context appCtx;

	public void onCreate(Bundle savedInstanceState) {

		// Apply theme before calling super.onCreate
		String currentTheme = getSharedPreferences("hajanotes_preferences", MODE_PRIVATE)
				.getString("app_theme", "blue");
		ThemeHelper.applyTheme(this, currentTheme);

		super.onCreate(savedInstanceState);

		this.appCtx = getApplicationContext();

		try {
			AppSettingsInit.initDefaultSettings(appCtx);
		} catch (Exception e) {
			// Handle database initialization error gracefully
			e.printStackTrace();
		}

		AppSettingsInit.updateSettingsValue(
				getApplicationContext(), "false", AppSettingsInit.APP_BIOMETRIC_LIFE_CYCLE_KEY);

		applyLocale();
	}

	private void applyLocale() {

		String savedLocale =
				getSharedPreferences("hajanotes_preferences", MODE_PRIVATE)
						.getString("app_locale", "en");
		Utils.setLocale(this, savedLocale);
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(Utils.setLocale(base, getCurrentLocaleFromPreferences(base)));
	}

	private String getCurrentLocaleFromPreferences(Context context) {
		return context.getSharedPreferences("hajanotes_preferences", MODE_PRIVATE)
				.getString("app_locale", "en");
	}

	public void onResume() {
		super.onResume();

		// Use device credentials (PIN/password) instead of biometric
		if (Boolean.parseBoolean(
					AppSettingsInit.getSettingsValue(
							ctx, AppSettingsInit.APP_BIOMETRIC_KEY))
				&& !Boolean.parseBoolean(
						AppSettingsInit.getSettingsValue(
								ctx, AppSettingsInit.APP_BIOMETRIC_LIFE_CYCLE_KEY))) {

			Intent unlockIntent = new Intent(ctx, BiometricLockActivity.class);
			ctx.startActivity(unlockIntent);
		}
	}
}

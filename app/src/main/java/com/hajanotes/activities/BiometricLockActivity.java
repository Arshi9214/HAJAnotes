package com.hajanotes.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.hajanotes.R;
import com.hajanotes.databinding.ActivityBiometricLockBinding;
import com.hajanotes.helpers.AppSettingsInit;
import java.util.concurrent.Executor;

/**
 * 
 */
public class BiometricLockActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		ActivityBiometricLockBinding activityUnlockBinding =
				ActivityBiometricLockBinding.inflate(getLayoutInflater());
		setContentView(activityUnlockBinding.getRoot());
	}

	public void onResume() {
		super.onResume();

		Executor executor = ContextCompat.getMainExecutor(this);

		BiometricPrompt biometricPrompt =
				new BiometricPrompt(
						this,
						executor,
						new BiometricPrompt.AuthenticationCallback() {

							@Override
							public void onAuthenticationError(
									int errorCode, @NonNull CharSequence errString) {

								super.onAuthenticationError(errorCode, errString);

								MainActivity.closeActivity = true;
								finish();
							}

							// Authentication succeeded, continue to app
							@Override
							public void onAuthenticationSucceeded(
									@NonNull BiometricPrompt.AuthenticationResult result) {
								super.onAuthenticationSucceeded(result);
								AppSettingsInit.updateSettingsValue(
										getApplicationContext(),
										"true",
										AppSettingsInit.APP_BIOMETRIC_LIFE_CYCLE_KEY);
								finish();
							}

							// Authentication failed, close the app
							@Override
							public void onAuthenticationFailed() {
								super.onAuthenticationFailed();
							}
						});

		BiometricPrompt.PromptInfo biometricPromptBuilder =
				new BiometricPrompt.PromptInfo.Builder()
						.setTitle(getString(R.string.biometric_auth_title))
						.setSubtitle(getString(R.string.biometric_auth_sub_title))
						.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
						.build();

		biometricPrompt.authenticate(biometricPromptBuilder);
	}
}

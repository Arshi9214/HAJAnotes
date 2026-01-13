package com.hajanotes.fragments;

import static android.content.Context.MODE_PRIVATE;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hajanotes.R;
import com.hajanotes.databinding.FragmentSettingsBinding;
import com.hajanotes.datastore.api.BaseApi;
import com.hajanotes.datastore.api.NoteTopicsApi;
import com.hajanotes.datastore.api.NotesApi;
import com.hajanotes.datastore.db.HajaNotesDatabase;
import com.hajanotes.helpers.AppSettingsInit;
import com.hajanotes.helpers.Snackbar;
import com.hajanotes.helpers.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.widget.Toast;
import java.util.List;
import com.hajanotes.datastore.models.Notes;

/**
 * 
 */
public class SettingsFragment extends Fragment implements BackupBottomSheet.BackupCallback {

	private FragmentSettingsBinding binding;
	private NotesApi notesApi;
	private NoteTopicsApi noteTopicsApi;
	private static int langSelectedChoice;
	private ActivityResultLauncher<Intent> importFileLauncher;
	private ActivityResultLauncher<Intent> exportFileLauncher;

	public View onCreateView(
			@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		binding = FragmentSettingsBinding.inflate(inflater, container, false);

		notesApi = BaseApi.getInstance(requireContext(), NotesApi.class);
		noteTopicsApi = BaseApi.getInstance(requireContext(), NoteTopicsApi.class);

		importFileLauncher =
				registerForActivityResult(
						new ActivityResultContracts.StartActivityForResult(),
						result -> {
							if (result.getResultCode() == Activity.RESULT_OK
									&& result.getData() != null) {
								Uri uri = result.getData().getData();
								if (uri != null) {
									confirmImport(uri);
								} else {
									Snackbar.info(
											requireActivity(),
											requireActivity().findViewById(R.id.nav_view),
											getString(R.string.import_failed));
								}
							} else {
								Snackbar.info(
										requireActivity(),
										requireActivity().findViewById(R.id.nav_view),
										getString(R.string.import_failed));
							}
						});

		exportFileLauncher =
				registerForActivityResult(
						new ActivityResultContracts.StartActivityForResult(),
						result -> {
							if (result.getResultCode() == Activity.RESULT_OK
									&& result.getData() != null) {
								Uri uri = result.getData().getData();
								if (uri != null) {
									exportDatabaseToUri(uri);
								} else {
									Snackbar.info(
											requireActivity(),
											requireActivity().findViewById(R.id.nav_view),
											getString(R.string.backup_failed));
								}
							} else {
								Snackbar.info(
										requireActivity(),
										requireActivity().findViewById(R.id.nav_view),
										getString(R.string.backup_failed));
							}
						});

		// about section
		binding.appVersion.setText(Utils.getAppVersion(requireContext()));
		// about section

		// language selection dialog
		LinkedHashMap<String, String> lang = new LinkedHashMap<>();
		lang.put("sys", getString(R.string.system));
		for (String langCode : getResources().getStringArray(R.array.languages)) {
			lang.put(langCode, getLanguageDisplayName(langCode));
		}

		String[] locale =
				AppSettingsInit.getSettingsValue(requireContext(), AppSettingsInit.APP_LOCALE_KEY)
						.split("\\|");
		langSelectedChoice = Integer.parseInt(locale[0]);
		binding.languageSelected.setText(
				lang.get(lang.keySet().toArray(new String[0])[langSelectedChoice]));

		binding.languageSelectionFrame.setOnClickListener(
				view -> {
					MaterialAlertDialogBuilder materialAlertDialogBuilder =
							new MaterialAlertDialogBuilder(requireContext())
									.setTitle(R.string.settings_language_selector_dialog_title)
									.setCancelable(langSelectedChoice != -1)
									.setSingleChoiceItems(
											lang.values().toArray(new String[0]),
											langSelectedChoice,
											(dialogInterface, i) -> {
												String selectedLanguage =
														lang.keySet().toArray(new String[0])[i];
												AppSettingsInit.updateSettingsValue(
														requireContext(),
														i + "|" + selectedLanguage,
														AppSettingsInit.APP_LOCALE_KEY);

												/*if (selectedLanguage.equalsIgnoreCase("sys")) {
													selectedLanguage =
															requireContext()
																	.getResources()
																	.getConfiguration()
																	.getLocales()
																	.get(0)
																	.getLanguage();
												}*/

												String[] multiCodeLang =
														selectedLanguage.split("-");
												if (selectedLanguage.contains("-")) {
													selectedLanguage = multiCodeLang[0];
												}

												SharedPreferences prefs =
														requireContext()
																.getSharedPreferences(
																		"hajanotes_preferences",
																		MODE_PRIVATE);
												prefs.edit()
														.putString("app_locale", selectedLanguage)
														.apply();

												Utils.setLocale(requireContext(), selectedLanguage);
												dialogInterface.dismiss();
												requireActivity().recreate();
											});

					materialAlertDialogBuilder.create().show();
				});
		// language selection dialog

		// Markdown mode switcher
		binding.switchMdMode.setChecked(
				Boolean.parseBoolean(
						AppSettingsInit.getSettingsValue(
								requireContext(), AppSettingsInit.APP_MD_MODE_KEY)));

		binding.switchMdMode.setOnCheckedChangeListener(
				(buttonView, isChecked) -> {
					if (isChecked) {

						AppSettingsInit.updateSettingsValue(
								requireContext(), "true", AppSettingsInit.APP_MD_MODE_KEY);
					} else {

						AppSettingsInit.updateSettingsValue(
								requireContext(), "false", AppSettingsInit.APP_MD_MODE_KEY);
						Snackbar.info(
								requireActivity(),
								requireActivity().findViewById(R.id.nav_view),
								getString(R.string.settings_saved));
					}
				});

		binding.mdModeFrame.setOnClickListener(
				v -> binding.switchMdMode.setChecked(!binding.switchMdMode.isChecked()));
		// Markdown mode switcher

		// Theme selector
		binding.themeFrame.setOnClickListener(v -> showThemeSelector());
		// Theme selector

		// biometric switcher
		binding.switchBiometric.setChecked(
				Boolean.parseBoolean(
						AppSettingsInit.getSettingsValue(
								requireContext(), AppSettingsInit.APP_BIOMETRIC_KEY)));

		binding.switchBiometric.setOnCheckedChangeListener(
				(buttonView, isChecked) -> {
					if (isChecked) {

						BiometricManager biometricManager = BiometricManager.from(requireContext());
						KeyguardManager keyguardManager =
								(KeyguardManager)
										requireContext().getSystemService(Context.KEYGUARD_SERVICE);

						if (!keyguardManager.isDeviceSecure()) {

							switch (biometricManager.canAuthenticate(
									BIOMETRIC_STRONG | DEVICE_CREDENTIAL)) {
								case BiometricManager.BIOMETRIC_SUCCESS:
									AppSettingsInit.updateSettingsValue(
											requireContext(),
											"true",
											AppSettingsInit.APP_BIOMETRIC_KEY);
									Snackbar.info(
											requireActivity(),
											requireActivity().findViewById(R.id.nav_view),
											getString(R.string.settings_saved));
									break;
								case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
								case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
								case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
								case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
									AppSettingsInit.updateSettingsValue(
											requireContext(),
											"false",
											AppSettingsInit.APP_BIOMETRIC_KEY);
									binding.switchBiometric.setChecked(false);
									Snackbar.info(
											requireActivity(),
											requireActivity().findViewById(R.id.nav_view),
											getString(R.string.biometric_not_supported));
									break;
								case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
									AppSettingsInit.updateSettingsValue(
											requireContext(),
											"false",
											AppSettingsInit.APP_BIOMETRIC_KEY);
									binding.switchBiometric.setChecked(false);
									Snackbar.info(
											requireActivity(),
											requireActivity().findViewById(R.id.nav_view),
											getString(R.string.biometric_not_available));
									break;
								case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
									AppSettingsInit.updateSettingsValue(
											requireContext(),
											"false",
											AppSettingsInit.APP_BIOMETRIC_KEY);
									binding.switchBiometric.setChecked(false);
									Snackbar.info(
											requireActivity(),
											requireActivity().findViewById(R.id.nav_view),
											getString(R.string.enroll_biometric));
									break;
							}
						} else {

							AppSettingsInit.updateSettingsValue(
									requireContext(), "true", AppSettingsInit.APP_BIOMETRIC_KEY);
							Snackbar.info(
									requireActivity(),
									requireActivity().findViewById(R.id.nav_view),
									getString(R.string.settings_saved));
						}
					} else {

						AppSettingsInit.updateSettingsValue(
								requireContext(), "false", AppSettingsInit.APP_BIOMETRIC_KEY);
						Snackbar.info(
								requireActivity(),
								requireActivity().findViewById(R.id.nav_view),
								getString(R.string.settings_saved));
					}
				});

		binding.biometricFrame.setOnClickListener(
				v -> binding.switchBiometric.setChecked(!binding.switchBiometric.isChecked()));
		// biometric switcher

		// database - notes
		binding.databaseNotesCount.setText(String.valueOf(notesApi.getCount()));

		binding.deleteNotes.setOnClickListener(
				deleteAllNotes -> {
					if (notesApi.getCount() > 0) {
						new MaterialAlertDialogBuilder(requireContext())
								.setMessage(R.string.delete_all_notes_dialog_message)
								.setPositiveButton(
										R.string.delete,
										(dialog, which) -> {
											deleteAllNotes();
											binding.databaseNotesCount.setText(
													String.valueOf(notesApi.getCount()));
											dialog.dismiss();
										})
								.setNeutralButton(R.string.cancel, null)
								.show();
					} else {
						Snackbar.info(
								requireActivity(),
								requireActivity().findViewById(R.id.nav_view),
								requireContext().getResources().getString(R.string.all_good));
					}
				});
		// database - notes

		// backup and restore
		binding.backupRestoreNotesFrame.setOnClickListener(
				v -> {
					BackupBottomSheet bottomSheet = BackupBottomSheet.newInstance(this);
					bottomSheet.show(getParentFragmentManager(), "BackupBottomSheet");
				});
		// backup and restore

		// Add sharing functionality
		addSharingClickListener();
		binding.shareNotesFrame.setOnClickListener(v -> showSharingOptions());
		// sharing functionality

		return binding.getRoot();
	}

	@Override
	public void onExport() {
		launchExportFilePicker();
	}

	@Override
	public void onImport() {
		launchImportFilePicker();
	}

	public void launchImportFilePicker() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("application/octet-stream");
		importFileLauncher.launch(intent);
	}

	public void launchExportFilePicker() {
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("application/octet-stream");
		String timestamp = new SimpleDateFormat("yyyyMd-HHmmss", Locale.US).format(new Date());
		intent.putExtra(Intent.EXTRA_TITLE, "hajanotes-" + timestamp + ".backup");
		exportFileLauncher.launch(intent);
	}

	public void exportDatabaseToUri(Uri uri) {
		Thread exportThread =
				new Thread(
						() -> {
							try {
								HajaNotesDatabase db =
										HajaNotesDatabase.getDatabaseInstance(requireContext());
								db.runInTransaction(() -> notesApi.getCount());
								db.close();

								boolean isWalEnabled = false;
								try (Cursor cursor =
										db.getOpenHelper()
												.getWritableDatabase()
												.query("PRAGMA journal_mode", new String[0])) {
									if (cursor.moveToFirst()) {
										isWalEnabled = "wal".equalsIgnoreCase(cursor.getString(0));
									}
								}
								if (isWalEnabled) {
									try (Cursor cursor =
											db.getOpenHelper()
													.getWritableDatabase()
													.query(
															"PRAGMA wal_checkpoint(FULL)",
															new String[0])) {
										cursor.moveToFirst();
									} catch (SQLiteException ignored) {
									}
								}

								File dbFile = requireContext().getDatabasePath("hajanotes");
								if (!dbFile.exists()) {
									throw new IOException("Database file does not exist");
								}

								File tempDir = requireContext().getCacheDir();
								File tempDbFile = new File(tempDir, "hajanotes_temp");
								try (FileInputStream fis = new FileInputStream(dbFile);
										FileOutputStream fos = new FileOutputStream(tempDbFile)) {
									FileChannel src = fis.getChannel();
									FileChannel dst = fos.getChannel();
									dst.transferFrom(src, 0, src.size());
								}

								try (InputStream inputStream = new FileInputStream(tempDbFile);
										OutputStream outputStream =
												requireContext()
														.getContentResolver()
														.openOutputStream(uri)) {
									if (outputStream == null) {
										throw new IOException(
												"Failed to open output stream for URI: " + uri);
									}
									byte[] buffer = new byte[8192];
									int bytesRead;
									while ((bytesRead = inputStream.read(buffer)) != -1) {
										outputStream.write(buffer, 0, bytesRead);
									}
								} finally {
									if (tempDbFile.exists()) {
										boolean ignored = tempDbFile.delete();
									}
								}

								requireActivity()
										.runOnUiThread(
												() -> {
													Snackbar.info(
															requireActivity(),
															requireActivity()
																	.findViewById(R.id.nav_view),
															getString(R.string.backup_success));
													new Handler(Looper.getMainLooper())
															.postDelayed(this::restartApp, 1500);
												});
							} catch (IOException | SQLiteException e) {
								requireActivity()
										.runOnUiThread(
												() ->
														Snackbar.info(
																requireActivity(),
																requireActivity()
																		.findViewById(
																				R.id.nav_view),
																getString(R.string.backup_failed)));
							}
						});
		exportThread.setDaemon(false);
		exportThread.start();
	}

	private void confirmImport(Uri uri) {
		try {
			File dbFile = requireContext().getDatabasePath("hajanotes");

			HajaNotesDatabase db = HajaNotesDatabase.getDatabaseInstance(requireContext());
			if (db.isOpen()) {
				db.close();
			}
			BaseApi.clearInstance();

			try (ParcelFileDescriptor pfd =
					requireContext().getContentResolver().openFileDescriptor(uri, "r")) {
				if (pfd == null) {
					throw new IOException("Failed to open file descriptor for URI: " + uri);
				}
				try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
						FileChannel src = fis.getChannel();
						FileOutputStream fos = new FileOutputStream(dbFile);
						FileChannel dst = fos.getChannel()) {
					dst.transferFrom(src, 0, src.size());
				}
			}

			db = HajaNotesDatabase.getDatabaseInstance(requireContext());
			db.getOpenHelper().getWritableDatabase();
			notesApi = BaseApi.getInstance(requireContext(), NotesApi.class);
			noteTopicsApi = BaseApi.getInstance(requireContext(), NoteTopicsApi.class);

			Snackbar.info(
					requireActivity(),
					requireActivity().findViewById(R.id.nav_view),
					getString(R.string.import_success));
			new Handler(Looper.getMainLooper()).postDelayed(this::restartApp, 1500);
		} catch (IOException | SQLiteException e) {
			Snackbar.info(
					requireActivity(),
					requireActivity().findViewById(R.id.nav_view),
					getString(R.string.import_failed));
		}
	}

	private void restartApp() {
		Intent intent =
				requireActivity()
						.getPackageManager()
						.getLaunchIntentForPackage(requireActivity().getPackageName());
		if (intent != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			requireActivity().finish();
			startActivity(intent);
			Runtime.getRuntime().exit(0);
		}
	}

	private static String getLanguageDisplayName(String langCode) {
		Locale english = new Locale("en");

		String[] multiCodeLang = langCode.split("-");
		String countryCode;
		if (langCode.contains("-")) {
			langCode = multiCodeLang[0];
			countryCode = multiCodeLang[1];
		} else {
			countryCode = "";
		}

		Locale translated = new Locale(langCode, countryCode);
		return String.format(
				"%s (%s)",
				translated.getDisplayName(translated), translated.getDisplayName(english));
	}

	public void deleteAllNotes() {
		notesApi.deleteAllNotes();
		noteTopicsApi.deleteAllNoteTopics();
		Snackbar.info(
				requireActivity(),
				requireActivity().findViewById(R.id.nav_view),
				requireContext()
						.getResources()
						.getQuantityString(R.plurals.note_delete_message, 2));
	}

	private void addSharingClickListener() {
		// Share notes functionality
		binding.shareNotesFrame.setOnClickListener(v -> showSharingOptions());
	}

	private void showSharingOptions() {
		String[] options = {
				getString(R.string.share_via_bluetooth),
				getString(R.string.share_via_wifi),
				getString(R.string.share_via_email),
				getString(R.string.share_via_whatsapp)
		};

		new MaterialAlertDialogBuilder(requireContext())
				.setTitle(R.string.choose_sharing_method)
				.setItems(options, (dialog, which) -> {
					switch (which) {
						case 0:
							shareViaBluetooth();
							break;
						case 1:
							shareViaWifi();
							break;
						case 2:
							shareViaEmail();
							break;
						case 3:
							shareViaWhatsApp();
							break;
					}
				})
				.show();
	}

	private void shareViaBluetooth() {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Toast.makeText(requireContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!bluetoothAdapter.isEnabled()) {
			Toast.makeText(requireContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
			return;
		}

		String notesData = getAllNotesAsText();
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_TEXT, notesData);
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Notes from HAJA Notes");

		// Try to use Bluetooth sharing
		Intent bluetoothIntent = Intent.createChooser(shareIntent, "Share via Bluetooth");
		startActivity(bluetoothIntent);
	}

	private void shareViaWifi() {
		WifiManager wifiManager = (WifiManager) requireContext().getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null && !wifiManager.isWifiEnabled()) {
			Toast.makeText(requireContext(), "Please enable WiFi", Toast.LENGTH_SHORT).show();
			return;
		}

		String notesData = getAllNotesAsText();
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_TEXT, notesData);
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Notes from HAJA Notes");

		startActivity(Intent.createChooser(shareIntent, "Share via WiFi"));
	}

	private void shareViaEmail() {
		String notesData = getAllNotesAsText();
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "My Notes from HAJA Notes");
		emailIntent.putExtra(Intent.EXTRA_TEXT, notesData);

		try {
			startActivity(Intent.createChooser(emailIntent, "Send email"));
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
		}
	}

	private void shareViaWhatsApp() {
		String notesData = getAllNotesAsText();
		Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
		whatsappIntent.setType("text/plain");
		whatsappIntent.setPackage("com.whatsapp");
		whatsappIntent.putExtra(Intent.EXTRA_TEXT, notesData);

		try {
			startActivity(whatsappIntent);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(requireContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show();
		}
	}

	private String getAllNotesAsText() {
		// Since fetchAllNotes() returns LiveData, we need to get notes synchronously
		// This is a simplified approach - in production you'd want to observe LiveData properly
		StringBuilder notesText = new StringBuilder();
		notesText.append("My Notes from HAJA Notes\n\n");
		
		notesText.append("Note: This is a placeholder. Implement proper data fetching.\n");
		
		return notesText.toString();
	}

	private void showThemeSelector() {
		String[] themes = {"Blue", "Green", "Purple", "Orange", "Red"};
		String[] themeValues = {"blue", "green", "purple", "orange", "red"};

		String currentTheme = AppSettingsInit.getSettingsValue(requireContext(), AppSettingsInit.APP_THEME_KEY);
		int selectedIndex = 0;
		for (int i = 0; i < themeValues.length; i++) {
			if (themeValues[i].equals(currentTheme)) {
				selectedIndex = i;
				break;
			}
		}

		new MaterialAlertDialogBuilder(requireContext())
				.setTitle("Select Theme")
				.setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
					AppSettingsInit.updateSettingsValue(requireContext(), themeValues[which], AppSettingsInit.APP_THEME_KEY);
					
					// Also save to SharedPreferences for immediate access
					requireContext().getSharedPreferences("hajanotes_preferences", MODE_PRIVATE)
							.edit()
							.putString("app_theme", themeValues[which])
							.apply();
					
					dialog.dismiss();
					requireActivity().recreate();
				})
				.setNegativeButton("Cancel", null)
				.show();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}

package com.hajanotes.bottomsheets;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hajanotes.R;
import com.hajanotes.databinding.BottomSheetNotesBinding;
import com.hajanotes.datastore.api.BaseApi;
import com.hajanotes.datastore.api.NoteTopicsApi;
import com.hajanotes.datastore.api.NotesApi;
import com.hajanotes.datastore.api.TopicsApi;
import com.hajanotes.datastore.models.Notes;
import com.hajanotes.datastore.models.Topics;
import com.hajanotes.helpers.AppSettingsInit;
import com.hajanotes.helpers.Markdown;
import com.hajanotes.interfaces.BottomSheetListener;
import com.vdurmont.emoji.EmojiParser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class BottomSheetNotes extends BottomSheetDialogFragment {

	// private final List<Topics> topicsList = new ArrayList<>();
	private BottomSheetListener bottomSheetListener;
	private String source;
	private NotesApi notesApi;
	private int noteId;
	private Notes notes;
	private BottomSheetNotesBinding bottomSheetNotesBinding;
	private ActivityResultLauncher<Intent> cameraLauncher;
	private ActivityResultLauncher<String> permissionLauncher;
	private ActivityResultLauncher<Intent> galleryLauncher;
	private Uri imageUri;

	@Nullable @Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {

		bottomSheetNotesBinding = BottomSheetNotesBinding.inflate(inflater, container, false);

		// Initialize camera launcher
		cameraLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (result.getResultCode() == Activity.RESULT_OK) {
						String imageText = "\n![Image](" + imageUri.toString() + ")\n";
						String currentText = bottomSheetNotesBinding.contents.getText().toString();
						bottomSheetNotesBinding.contents.setText(currentText + imageText);
					}
				});

		// Initialize gallery launcher
		galleryLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
						Uri selectedImage = result.getData().getData();
						if (selectedImage != null) {
							String imageText = "\n![Image](" + selectedImage.toString() + ")\n";
							String currentText = bottomSheetNotesBinding.contents.getText().toString();
							bottomSheetNotesBinding.contents.setText(currentText + imageText);
						}
					}
				});

		permissionLauncher = registerForActivityResult(
				new ActivityResultContracts.RequestPermission(),
				result -> {
					if (result) {
						openCamera();
					} else {
						Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
					}
				});

		notesApi = BaseApi.getInstance(requireContext(), NotesApi.class);
		TopicsApi topicsApi = BaseApi.getInstance(requireContext(), TopicsApi.class);
		NoteTopicsApi noteTopicsApi = BaseApi.getInstance(requireContext(), NoteTopicsApi.class);

		assert topicsApi != null;
		List<Topics> topicsList = topicsApi.fetchTopics();
		List<String> topicList_ = new ArrayList<>();

		for (Topics data : topicsList) {
			topicList_.add(data.getTopic());
		}

		ArrayAdapter<String> UnitAdapter =
				new ArrayAdapter<>(
						requireContext(),
						android.R.layout.simple_spinner_dropdown_item,
						topicList_);
		bottomSheetNotesBinding.topicsDropdown.setAdapter(UnitAdapter);

		Bundle bundle = getArguments();
		assert bundle != null;

		if (bundle.getString("source") != null) {
			source = bundle.getString("source");
			noteId = bundle.getInt("noteId");
		} else {
			noteId = bundle.getInt("noteId");
			source = "";
		}

		bottomSheetNotesBinding.closeBs.setOnClickListener(close -> dismiss());

		// Add image button click listener
		bottomSheetNotesBinding.addImage.setOnClickListener(v -> showImageOptions());

		bottomSheetNotesBinding.view.setOnClickListener(
				view -> {
					bottomSheetNotesBinding.contents.setVisibility(View.GONE);
					bottomSheetNotesBinding.renderContents.setVisibility(View.VISIBLE);

					bottomSheetNotesBinding.edit.setVisibility(View.VISIBLE);
					bottomSheetNotesBinding.view.setVisibility(View.GONE);

					Markdown.render(
							requireContext(),
							EmojiParser.parseToUnicode(
									bottomSheetNotesBinding.contents.getText().toString()),
							bottomSheetNotesBinding.renderContents);
				});

		bottomSheetNotesBinding.edit.setOnClickListener(
				edit -> {
					bottomSheetNotesBinding.contents.setVisibility(View.VISIBLE);
					bottomSheetNotesBinding.renderContents.setVisibility(View.GONE);

					bottomSheetNotesBinding.edit.setVisibility(View.GONE);
					bottomSheetNotesBinding.view.setVisibility(View.VISIBLE);
				});

		assert noteTopicsApi != null;
		if (noteTopicsApi.checkByNoteId(noteId) > 0) {
			int topicId = noteTopicsApi.getTopicId(noteId);
			String topic = topicsApi.getTopicById(topicId);
			for (int i = 0; i < topicList_.size(); i++) {
				if (topicList_.get(i).equalsIgnoreCase(topic)) {
					bottomSheetNotesBinding.topicsDropdown.setText(topicList_.get(i), false);
				}
			}
		}

		bottomSheetNotesBinding.topicsDropdown.setOnItemClickListener(
				(parent, view, position, id) -> {
					int topicId =
							topicsApi.getTopicId(
									bottomSheetNotesBinding.topicsDropdown.getText().toString());

					if (noteTopicsApi.checkByNoteId(noteId) > 0) {
						// update
						noteTopicsApi.updateTopicId(noteId, topicId);
					} else {
						// new
						noteTopicsApi.insertNoteTopic(noteId, topicId);
					}
				});

		if (source.equalsIgnoreCase("edit")) {

			notes = notesApi.fetchNoteById(noteId);

			bottomSheetNotesBinding.contents.setText(notes.getContent());
			bottomSheetNotesBinding.title.setText(notes.getTitle());

			assert notes.getContent() != null;
			bottomSheetNotesBinding.contents.setSelection(notes.getContent().length());

			bottomSheetNotesBinding.title.addTextChangedListener(textWatcher);
			bottomSheetNotesBinding.contents.addTextChangedListener(textWatcher);

			// check for md mode
			if (!Boolean.parseBoolean(
					AppSettingsInit.getSettingsValue(
							requireContext(), AppSettingsInit.APP_MD_MODE_KEY))) {
				bottomSheetNotesBinding.contents.setVisibility(View.GONE);
				bottomSheetNotesBinding.renderContents.setVisibility(View.VISIBLE);

				bottomSheetNotesBinding.edit.setVisibility(View.VISIBLE);
				bottomSheetNotesBinding.view.setVisibility(View.GONE);

				Markdown.render(
						requireContext(),
						EmojiParser.parseToUnicode(
								bottomSheetNotesBinding.contents.getText().toString()),
						bottomSheetNotesBinding.renderContents);
			}

		} else if (source.equalsIgnoreCase("new")) {

			bottomSheetNotesBinding.topicsDropdownLayout.setVisibility(View.GONE);

			bottomSheetNotesBinding.title.addTextChangedListener(textWatcher);
			bottomSheetNotesBinding.contents.addTextChangedListener(textWatcher);
		}

		DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity) requireContext())
				.getWindowManager()
				.getDefaultDisplay()
				.getMetrics(displayMetrics);
		int height = displayMetrics.heightPixels;
		bottomSheetNotesBinding.contents.setMinHeight(height);
		bottomSheetNotesBinding.renderContents.setMinHeight(height);

		return bottomSheetNotesBinding.getRoot();
	}

	private void updateNote(String content, String title, int noteId) {
		notesApi.updateNote(content, title, Instant.now().getEpochSecond(), noteId);
	}

	private final TextWatcher textWatcher =
			new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void afterTextChanged(Editable s) {

					String text = bottomSheetNotesBinding.contents.getText().toString();
					String title = bottomSheetNotesBinding.title.getText().toString();

					if (bottomSheetNotesBinding.title.getText().hashCode() == s.hashCode()) {
						if (noteId > 0) {
							updateNote(text, title, noteId);
						} else {
							if (title.length() > 2) {

								bottomSheetNotesBinding.topicsDropdownLayout.setVisibility(
										View.VISIBLE);

								noteId =
										(int)
												notesApi.insertNote(
														text,
														title,
														(int) Instant.now().getEpochSecond());
							}
						}
					} else if (bottomSheetNotesBinding.contents.getText().hashCode()
							== s.hashCode()) {
						if (noteId > 0) {
							updateNote(text, title, noteId);
						} else {
							if (title.length() > 2) {
								noteId =
										(int)
												notesApi.insertNote(
														text,
														title,
														(int) Instant.now().getEpochSecond());
							}
						}
					}
				}
			};

	private void showImageOptions() {
		String[] options = {"Camera", "Gallery"};
		new MaterialAlertDialogBuilder(requireContext())
				.setTitle("Add Image")
				.setItems(options, (dialog, which) -> {
					if (which == 0) {
						checkCameraPermission();
					} else {
						openGallery();
					}
				})
				.show();
	}

	private void checkCameraPermission() {
		if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) 
				== PackageManager.PERMISSION_GRANTED) {
			openCamera();
		} else {
			permissionLauncher.launch(android.Manifest.permission.CAMERA);
		}
	}

	private void openCamera() {
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (cameraIntent.resolveActivity(requireContext().getPackageManager()) != null) {
			try {
				java.io.File photoFile = new java.io.File(requireContext().getExternalFilesDir(null), 
						"photo_" + System.currentTimeMillis() + ".jpg");
				imageUri = FileProvider.getUriForFile(requireContext(), 
						requireContext().getPackageName() + ".fileprovider", photoFile);
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				cameraLauncher.launch(cameraIntent);
			} catch (Exception e) {
				Toast.makeText(requireContext(), "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(requireContext(), "Camera not available on this device", Toast.LENGTH_SHORT).show();
		}
	}

	private void openGallery() {
		Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		galleryIntent.setType("image/*");
		galleryLauncher.launch(galleryIntent);
	}

	@NonNull @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
		dialog.setContentView(R.layout.bottom_sheet_notes);

		dialog.setOnShowListener(
				dialogInterface -> {
					BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
					View bottomSheet =
							bottomSheetDialog.findViewById(
									com.google.android.material.R.id.design_bottom_sheet);

					if (bottomSheet != null) {

						BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
						behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
						behavior.setPeekHeight(bottomSheet.getHeight());
						behavior.setHideable(false);
					}
				});

		if (dialog.getWindow() != null) {

			WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
			params.height = WindowManager.LayoutParams.MATCH_PARENT;
			dialog.getWindow().setAttributes(params);
		}

		return dialog;
	}

	@Override
	public void onAttach(@NonNull Context context) {

		super.onAttach(context);

		try {
			bottomSheetListener = (BottomSheetListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context + " must implement BottomSheetListener");
		}
	}
}

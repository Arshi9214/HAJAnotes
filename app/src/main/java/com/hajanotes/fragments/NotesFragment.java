package com.hajanotes.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.hajanotes.adapters.NotesAdapter;
import com.hajanotes.bottomsheets.BottomSheetNotes;
import com.hajanotes.databinding.FragmentNotesBinding;
import com.hajanotes.datastore.api.BaseApi;
import com.hajanotes.datastore.api.NotesApi;
import com.hajanotes.datastore.models.Notes;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class NotesFragment extends Fragment {

	private FragmentNotesBinding binding;
	private NotesApi notesApi;
	private List<Notes> notesList;
	private List<Notes> notesListSearch;
	private NotesAdapter adapter;
	private NotesAdapter adapterSearch;

	public View onCreateView(
			@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		binding = FragmentNotesBinding.inflate(inflater, container, false);

		notesApi = BaseApi.getInstance(requireContext(), NotesApi.class);

		notesListSearch = new ArrayList<>();
		adapterSearch =
				new NotesAdapter(
						requireContext(),
						"search",
						notesListSearch,
						binding.newNote,
						requireActivity());
		binding.recyclerViewSearch.setAdapter(adapterSearch);

		notesList = new ArrayList<>();
		adapter =
				new NotesAdapter(
						requireContext(), "", notesList, binding.newNote, requireActivity());
		binding.recyclerView.setAdapter(adapter);

		binding.recyclerView.setHasFixedSize(true);

		binding.pullToRefresh.setOnRefreshListener(
				() ->
						new Handler(Looper.getMainLooper())
								.postDelayed(
										() -> {
											notesList.clear();
											binding.pullToRefresh.setRefreshing(false);
											binding.progressBar.setVisibility(View.VISIBLE);
											getNotes();
										},
										350));

		getNotes();

		binding.searchView
				.getEditText()
				.addTextChangedListener(
						new TextWatcher() {
							@Override
							public void beforeTextChanged(
									CharSequence charSequence, int i, int i1, int i2) {}

							@Override
							public void onTextChanged(
									CharSequence charSequence, int i, int i1, int i2) {

								String searchText = charSequence.toString();
								if (searchText.length() >= 2) {
									updateSearchResult(searchText);
								} else {
									notesListSearch.clear();
									adapterSearch.notifyDataChanged();
								}
							}

							@Override
							public void afterTextChanged(Editable editable) {}
						});

		requireActivity()
				.getOnBackPressedDispatcher()
				.addCallback(
						getViewLifecycleOwner(),
						new OnBackPressedCallback(true) {

							@Override
							public void handleOnBackPressed() {
								if (binding.searchView.isShowing()) {
									binding.searchView.hide();
								} else {
									requireActivity().finish();
								}
							}
						});

		Bundle bsBundle = new Bundle();
		binding.newNote.setOnClickListener(
				newNote -> {
					bsBundle.putString("source", "new");
					BottomSheetNotes bottomSheet = new BottomSheetNotes();
					bottomSheet.setArguments(bsBundle);
					bottomSheet.show(getParentFragmentManager(), "notesBottomSheet");
				});

		return binding.getRoot();
	}

	private void getNotes() {

		notesApi.fetchAllNotes()
				.observe(
						getViewLifecycleOwner(),
						allNotes -> {
							binding.pullToRefresh.setRefreshing(false);
							assert allNotes != null;
							if (!allNotes.isEmpty()) {

								notesList.clear();
								binding.nothingFoundFrame.getRoot().setVisibility(View.GONE);
								notesList.addAll(allNotes);
								adapter.notifyDataChanged();
							} else {

								binding.nothingFoundFrame.getRoot().setVisibility(View.VISIBLE);
							}
							binding.progressBar.setVisibility(View.GONE);
						});
	}

	public void updateSearchResult(String text) {

		binding.recyclerViewSearch.setHasFixedSize(true);

		notesApi.extendedSearchedNotes(text)
				.observe(
						this,
						allNotes -> {
							assert allNotes != null;
							if (!allNotes.isEmpty()) {
								notesListSearch.clear();
								notesListSearch.addAll(allNotes);
								adapterSearch.notifyDataChanged();
							}
						});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}

package com.hajanotes.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.hajanotes.adapters.TopicsAdapter;
import com.hajanotes.bottomsheets.BottomSheetTopics;
import com.hajanotes.databinding.FragmentTopicsBinding;
import com.hajanotes.datastore.api.BaseApi;
import com.hajanotes.datastore.api.NoteTopicsApi;
import com.hajanotes.datastore.api.TopicsApi;
import com.hajanotes.datastore.models.Topics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class TopicsFragment extends Fragment {

	private FragmentTopicsBinding binding;
	private TopicsApi topicsApi;
	private List<Topics> topicsList;
	private TopicsAdapter adapter;

	public View onCreateView(
			@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		binding = FragmentTopicsBinding.inflate(inflater, container, false);

		topicsApi = BaseApi.getInstance(requireContext(), TopicsApi.class);
		NoteTopicsApi noteTopicsApi = BaseApi.getInstance(requireContext(), NoteTopicsApi.class);

		assert topicsApi != null;
		if (topicsApi.checkTopic("General") == 0) {
			topicsApi.insertTopic("General", "850970", (int) Instant.now().getEpochSecond());
		}

		topicsList = new ArrayList<>();
		adapter = new TopicsAdapter(requireContext(), "", topicsList, binding.newTopic);
		binding.recyclerView.setAdapter(adapter);

		binding.recyclerView.setHasFixedSize(true);
		binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

		binding.pullToRefresh.setOnRefreshListener(
				() ->
						new Handler(Looper.getMainLooper())
								.postDelayed(
										() -> {
											topicsList.clear();
											binding.pullToRefresh.setRefreshing(false);
											binding.progressBar.setVisibility(View.VISIBLE);
											getTopics();
										},
										350));

		getTopics();

		Bundle bsBundle = new Bundle();
		binding.newTopic.setOnClickListener(
				newNote -> {
					bsBundle.putString("source", "new");
					BottomSheetTopics bottomSheet = new BottomSheetTopics();
					bottomSheet.setArguments(bsBundle);
					bottomSheet.show(getParentFragmentManager(), "topicsBottomSheet");
				});

		return binding.getRoot();
	}

	private void getTopics() {

		topicsApi
				.fetchAllTopics()
				.observe(
						getViewLifecycleOwner(),
						allTopics -> {
							binding.pullToRefresh.setRefreshing(false);
							assert allTopics != null;
							if (!allTopics.isEmpty()) {

								topicsList.clear();
								binding.nothingFoundFrame.getRoot().setVisibility(View.GONE);
								topicsList.addAll(allTopics);
								adapter.notifyDataChanged();
							} else {

								binding.nothingFoundFrame.getRoot().setVisibility(View.VISIBLE);
							}
							binding.progressBar.setVisibility(View.GONE);
						});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}

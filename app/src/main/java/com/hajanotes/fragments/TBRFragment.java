package com.hajanotes.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.hajanotes.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TBRFragment extends Fragment {

	private LinearLayout tbrListContainer;
	private List<String> programmingBooks;
	private List<String> fictionBooks;
	private List<String> nonFictionBooks;
	private ArrayList<BookItem> myTBRList = new ArrayList<>();

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_tbr, container, false);

		tbrListContainer = view.findViewById(R.id.tbr_list_container);

		initializeRecommendations();
		setupProgrammingBooks(view);
		setupFictionBooks(view);
		setupNonFictionBooks(view);

		view.findViewById(R.id.add_book_fab).setOnClickListener(v -> showAddBookDialog());

		return view;
	}

	private void initializeRecommendations() {
		programmingBooks = Arrays.asList(
				"Clean Code - Robert C. Martin",
				"The Pragmatic Programmer - Andrew Hunt",
				"Design Patterns - Gang of Four",
				"Head First Java - Kathy Sierra",
				"Effective Java - Joshua Bloch"
		);

		fictionBooks = Arrays.asList(
				"1984 - George Orwell",
				"To Kill a Mockingbird - Harper Lee",
				"The Great Gatsby - F. Scott Fitzgerald",
				"Pride and Prejudice - Jane Austen",
				"The Catcher in the Rye - J.D. Salinger"
		);

		nonFictionBooks = Arrays.asList(
				"Sapiens - Yuval Noah Harari",
				"Educated - Tara Westover",
				"Atomic Habits - James Clear",
				"Thinking, Fast and Slow - Daniel Kahneman",
				"The Power of Habit - Charles Duhigg"
		);
	}

	private void setupProgrammingBooks(View parentView) {
		ListView listView = parentView.findViewById(R.id.programming_books_list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				requireContext(), R.layout.item_book_recommendation, R.id.book_title, new ArrayList<>()) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				if (convertView == null) {
					convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_book_recommendation, parent, false);
				}
				String bookInfo = programmingBooks.get(position);
				String[] parts = bookInfo.split(" - ");
				
				TextView titleView = convertView.findViewById(R.id.book_title);
				TextView authorView = convertView.findViewById(R.id.book_author);
				Button addButton = convertView.findViewById(R.id.add_button);

				titleView.setText(parts[0]);
				authorView.setText(parts.length > 1 ? parts[1] : "");
				addButton.setOnClickListener(v -> addBookToTBR(parts[0], parts.length > 1 ? parts[1] : ""));

				return convertView;
			}

			@Override
			public int getCount() {
				return programmingBooks.size();
			}
		};
		listView.setAdapter(adapter);
		setListViewHeightBasedOnChildren(listView);
	}

	private void setupFictionBooks(View parentView) {
		ListView listView = parentView.findViewById(R.id.fiction_books_list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				requireContext(), R.layout.item_book_recommendation, R.id.book_title, new ArrayList<>()) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				if (convertView == null) {
					convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_book_recommendation, parent, false);
				}
				String bookInfo = fictionBooks.get(position);
				String[] parts = bookInfo.split(" - ");
				
				TextView titleView = convertView.findViewById(R.id.book_title);
				TextView authorView = convertView.findViewById(R.id.book_author);
				Button addButton = convertView.findViewById(R.id.add_button);

				titleView.setText(parts[0]);
				authorView.setText(parts.length > 1 ? parts[1] : "");
				addButton.setOnClickListener(v -> addBookToTBR(parts[0], parts.length > 1 ? parts[1] : ""));

				return convertView;
			}

			@Override
			public int getCount() {
				return fictionBooks.size();
			}
		};
		listView.setAdapter(adapter);
		setListViewHeightBasedOnChildren(listView);
	}

	private void setupNonFictionBooks(View parentView) {
		ListView listView = parentView.findViewById(R.id.nonfiction_books_list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				requireContext(), R.layout.item_book_recommendation, R.id.book_title, new ArrayList<>()) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				if (convertView == null) {
					convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_book_recommendation, parent, false);
				}
				String bookInfo = nonFictionBooks.get(position);
				String[] parts = bookInfo.split(" - ");
				
				TextView titleView = convertView.findViewById(R.id.book_title);
				TextView authorView = convertView.findViewById(R.id.book_author);
				Button addButton = convertView.findViewById(R.id.add_button);

				titleView.setText(parts[0]);
				authorView.setText(parts.length > 1 ? parts[1] : "");
				addButton.setOnClickListener(v -> addBookToTBR(parts[0], parts.length > 1 ? parts[1] : ""));

				return convertView;
			}

			@Override
			public int getCount() {
				return nonFictionBooks.size();
			}
		};
		listView.setAdapter(adapter);
		setListViewHeightBasedOnChildren(listView);
	}

	private void setListViewHeightBasedOnChildren(ListView listView) {
		ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();
		if (adapter == null) return;

		int totalHeight = 0;
		for (int i = 0; i < adapter.getCount(); i++) {
			View listItem = adapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}

	private void showAddBookDialog() {
		View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_book, null);
		EditText editTitle = dialogView.findViewById(R.id.edit_book_title);
		EditText editAuthor = dialogView.findViewById(R.id.edit_author_name);

		AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

		dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
		dialogView.findViewById(R.id.btn_add).setOnClickListener(v -> {
			String title = editTitle.getText().toString().trim();
			String author = editAuthor.getText().toString().trim();
			if (!title.isEmpty()) {
				addBookToTBR(title, author);
				dialog.dismiss();
			} else {
				Toast.makeText(requireContext(), "Please enter book title", Toast.LENGTH_SHORT).show();
			}
		});

		dialog.show();
	}

	private void addBookToTBR(String title, String author) {
		BookItem book = new BookItem(title, author);
		myTBRList.add(book);
		displayTBRList();
		Toast.makeText(requireContext(), getString(R.string.book_added), Toast.LENGTH_SHORT).show();
	}

	private void displayTBRList() {
		tbrListContainer.removeAllViews();
		for (int i = 0; i < myTBRList.size(); i++) {
			final int index = i;
			BookItem book = myTBRList.get(i);
			
			View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_tbr_book, tbrListContainer, false);
			
			TextView titleView = itemView.findViewById(R.id.tbr_book_title);
			TextView authorView = itemView.findViewById(R.id.tbr_book_author);
			CheckBox checkBox = itemView.findViewById(R.id.read_checkbox);
			ImageButton deleteButton = itemView.findViewById(R.id.delete_button);

			titleView.setText(book.title);
			authorView.setText(book.author);
			checkBox.setChecked(book.isRead);
			
			checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> book.isRead = isChecked);
			
			deleteButton.setOnClickListener(v -> {
				myTBRList.remove(index);
				displayTBRList();
				Toast.makeText(requireContext(), getString(R.string.book_removed), Toast.LENGTH_SHORT).show();
			});

			tbrListContainer.addView(itemView);
		}
	}

	private static class BookItem {
		String title;
		String author;
		boolean isRead;

		BookItem(String title, String author) {
			this.title = title;
			this.author = author;
			this.isRead = false;
		}
	}
}

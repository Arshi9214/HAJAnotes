package com.hajanotes.datastore.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.hajanotes.datastore.dao.AppSettingsDao;
import com.hajanotes.datastore.dao.NoteTopicDao;
import com.hajanotes.datastore.dao.NotesDao;
import com.hajanotes.datastore.dao.TopicsDao;
import com.hajanotes.datastore.models.AppSettings;
import com.hajanotes.datastore.models.NoteTopics;
import com.hajanotes.datastore.models.Notes;
import com.hajanotes.datastore.models.Topics;

/**
 * 
 */
@Database(
		entities = {Notes.class, Topics.class, AppSettings.class, NoteTopics.class},
		version = 1,
		exportSchema = false)
public abstract class HajaNotesDatabase extends RoomDatabase {

	private static final String DB_NAME = "hajanotes";

	private static volatile HajaNotesDatabase hajaNotesDatabase;

	public static HajaNotesDatabase getDatabaseInstance(Context context) {

		if (hajaNotesDatabase == null) {
			synchronized (HajaNotesDatabase.class) {
				if (hajaNotesDatabase == null) {

					hajaNotesDatabase =
							Room.databaseBuilder(context, HajaNotesDatabase.class, DB_NAME)
									// .fallbackToDestructiveMigration()
									.allowMainThreadQueries()
									// .addMigrations(MIGRATION_1_2)
									.build();
				}
			}
		}

		return hajaNotesDatabase;
	}

	public abstract NotesDao notesDao();

	public abstract TopicsDao topicsDao();

	public abstract AppSettingsDao appSettingsDao();

	public abstract NoteTopicDao noteTopicDao();
}

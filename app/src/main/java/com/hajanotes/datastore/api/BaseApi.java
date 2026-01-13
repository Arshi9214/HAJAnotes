package com.hajanotes.datastore.api;

import android.content.Context;
import androidx.annotation.NonNull;
import com.hajanotes.datastore.db.HajaNotesDatabase;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 */
public abstract class BaseApi {

	protected static final ExecutorService executorService = Executors.newCachedThreadPool();
	private static final Map<Class<? extends BaseApi>, Object> instances = new HashMap<>();
	protected static HajaNotesDatabase hajanotesDatabase;

	protected BaseApi(Context context) {
		hajanotesDatabase = HajaNotesDatabase.getDatabaseInstance(context);
	}

	@SuppressWarnings("unchecked")
	public static <T extends BaseApi> T getInstance(
			@NonNull Context context, @NonNull Class<T> clazz) {

		try {

			if (!instances.containsKey(clazz)) {
				synchronized (BaseApi.class) {
					if (!instances.containsKey(clazz)) {

						T instance =
								clazz.getDeclaredConstructor(Context.class).newInstance(context);

						instances.put(clazz, instance);
						return instance;
					}
				}
			}

			return (T) instances.get(clazz);

		} catch (NoSuchMethodException
				| IllegalAccessException
				| InvocationTargetException
				| InstantiationException ignored) {
		}

		return null;
	}

	public static void clearInstance() {
		if (hajanotesDatabase != null && hajanotesDatabase.isOpen()) {
			hajanotesDatabase.close();
		}
		hajanotesDatabase = null;
	}
}

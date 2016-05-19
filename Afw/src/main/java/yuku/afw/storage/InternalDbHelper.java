package yuku.afw.storage;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import yuku.afw.App;

public abstract class InternalDbHelper extends SQLiteOpenHelper {
	public static final String TAG = InternalDbHelper.class.getSimpleName();

	/**
	 * @deprecated Use {@link yuku.afw.storage.InternalDbHelper#InternalDbHelper(String)} instead to allow multiple databases in one app.
	 */
	@Deprecated public InternalDbHelper() {
		this("InternalDb");
	}
	
	public InternalDbHelper(String name) {
		super(App.context, name, null, App.getVersionCode());
	}

	@Override public void onOpen(SQLiteDatabase db) {
		//
	};

	@Override public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate called");

		try {
			createTables(db);
			createIndexes(db);
		} catch (SQLException e) {
			Log.e(TAG, "onCreate db failed!", e);
			throw e;
		}
	}

	public abstract void createTables(SQLiteDatabase db);
	public abstract void createIndexes(SQLiteDatabase db);

	@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
}

package yuku.alkitab.base.storage;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import yuku.alkitab.base.App;
import yuku.alkitab.base.util.SongBookUtil;

public class SongDbHelper extends SQLiteOpenHelper {
	public static final String TAG = SongDbHelper.class.getSimpleName();

	public SongDbHelper() {
		super(App.context, "SongDb", null, App.getVersionCode());
		if (Build.VERSION.SDK_INT >= 16) {
			setWriteAheadLoggingEnabled(true);
		}
	}

	@Override
	public void onOpen(final SQLiteDatabase db) {
		super.onOpen(db);
		if (Build.VERSION.SDK_INT < 16) {
			db.enableWriteAheadLogging();
		}
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		setupTableSongInfo(db);
		setupTableSongBookInfo(db);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		if (oldVersion < 14000202 /* 4.1-beta2 */) {
			addColumnSongInfo(db);
			setupTableSongBookInfo(db);
			insertSongBookInfosFromSongInfos(db);
		}
	}

	/**
	 * Pre-4.1, song books are hardcoded. So for 4.1 onwards, we need to populate SongBookInfo
	 * table with the songs users already have.
	 */
	private void insertSongBookInfosFromSongInfos(final SQLiteDatabase db) {
		final Cursor c = db.rawQuery("select distinct " + Table.SongInfo.bookName + " from " + Table.SongInfo.tableName(), null);
		try {
			while (c.moveToNext()) {
				final String bookName = c.getString(0);
				final SongBookUtil.SongBookInfo info = SongBookUtil.getSongBookInfo(db, bookName);
				SongDb.insertSongBookInfo(db, info);
			}
		} finally {
			c.close();
		}
	}

	private void addColumnSongInfo(final SQLiteDatabase db) {
		db.execSQL("alter table " + Table.SongInfo.tableName() + " add column " + Table.SongInfo.updateTime + " " + Table.SongInfo.updateTime.type);
	}

	private void setupTableSongInfo(final SQLiteDatabase db) {
		{ // table
			final StringBuilder sb = new StringBuilder("create table " + Table.SongInfo.tableName() + " ( _id integer primary key ");
			for (Table.SongInfo field : Table.SongInfo.values()) {
				sb.append(',');
				sb.append(field.name());
				sb.append(' ');
				sb.append(field.type.name());
				if (field.suffix != null) {
					sb.append(' ');
					sb.append(field.suffix);
				}
			}
			sb.append(")");
			db.execSQL(sb.toString());
		}

		// index SongInfo(bookName, code)
		db.execSQL("create index " + Table.SongInfo.tableName() + "_001_index on " + Table.SongInfo.tableName() + " ("
			+ Table.SongInfo.bookName + ","
			+ Table.SongInfo.code
			+ ")");

		// index SongInfo(bookName, ordering)
		db.execSQL("create index " + Table.SongInfo.tableName() + "_002_index on " + Table.SongInfo.tableName() + " ("
			+ Table.SongInfo.bookName + ","
			+ Table.SongInfo.ordering
			+ ")");
	}

	private void setupTableSongBookInfo(final SQLiteDatabase db) {
		{ // table
			final StringBuilder sb = new StringBuilder("create table " + Table.SongBookInfo.tableName() + " ( _id integer primary key ");
			for (Table.SongBookInfo field : Table.SongBookInfo.values()) {
				sb.append(',');
				sb.append(field.name());
				sb.append(' ');
				sb.append(field.type.name());
				if (field.suffix != null) {
					sb.append(' ');
					sb.append(field.suffix);
				}
			}
			sb.append(")");
			db.execSQL(sb.toString());
		}

		// index SongBookInfo(name)
		db.execSQL("create index " + Table.SongBookInfo.tableName() + "_001_index on " + Table.SongBookInfo.tableName() + " ("
			+ Table.SongBookInfo.name
			+ ")");

	}
}

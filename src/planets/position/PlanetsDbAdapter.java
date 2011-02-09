package planets.position;

/*
 * Copyright (C) 2011 Tim Gaddis
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This is the helper class for the dynamically created location database.
 * 
 * @author Tim Gaddis
 * 
 */
public class PlanetsDbAdapter {

	public static final String KEY_NAME = "name";
	public static final String KEY_ROWID = "_id";

	private static final String TAG = "PlanetsDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statements
	 */
	private static final String LOC_DB_CREATE = "create table location "
			+ "(_id integer primary key autoincrement, name text not null, "
			+ "lat real, lng real, temp real, pressure real, elevation real, "
			+ "date integer, offset real, ioffset integer);";

	private static final String insertRow = "insert into location "
			+ "(_id, name, lat, lng, temp, pressure, elevation, date, offset, ioffset) "
			+ "VALUES (0, 'Location', -1.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 13);";

	private static final String PL_DB_CREATE = "create table planets "
			+ "(_id integer primary key autoincrement, name text not null, "
			+ "ra real, dec real, az real, alt real, dis real);";

	private static final String DATABASE_NAME = "PlanetsDB";
	private static String DATABASE_TABLE;
	private static final int DATABASE_VERSION = 3;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(LOC_DB_CREATE);
			db.execSQL(insertRow);
			db.execSQL(PL_DB_CREATE);
			String ip1 = "insert into planets "
					+ "(_id, name, ra, dec, az, alt, dis) VALUES (";
			String ip2 = ", 'P', 0.0, 0.0, 0.0, 0.0, 0.0);";
			for (int i = 0; i < 10; i++) {
				db.execSQL(ip1 + i + ip2);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS location");
			db.execSQL("DROP TABLE IF EXISTS planets");
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public PlanetsDbAdapter(Context ctx, String table) {
		this.mCtx = ctx;
		DATABASE_TABLE = table;
	}

	/**
	 * Open the notes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public PlanetsDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public boolean updateLocation(long rowID, double lat, double lng,
			double temp, double pressure, long date, double offset,
			int ioffset, double elevation) {
		ContentValues args = new ContentValues();
		args.put("lat", lat);
		args.put("lng", lng);
		args.put("temp", temp);
		args.put("pressure", pressure);
		args.put("elevation", elevation);
		args.put("date", date);
		args.put("offset", offset);
		args.put("ioffset", ioffset);
		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowID, null) > 0;
	}

	public boolean updatePlanet(long rowID, String name, double ra, double dec,
			double az, double alt, double dis) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put("ra", ra);
		args.put("dec", dec);
		args.put("az", az);
		args.put("alt", alt);
		args.put("dis", dis);
		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowID, null) > 0;
	}

	/**
	 * Delete the note with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEntry(long rowId) {

		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor fetchAllList() {
		// "alt > 0"
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_NAME,
				"az", "alt" }, "alt > 0", null, null, null, null);
	}

	/**
	 * Return a Cursor positioned at the id that matches the given rowId
	 * 
	 * @param rowId
	 *            id of note to retrieve
	 * @return Cursor positioned to matching note, if found
	 * @throws SQLException
	 *             if note could not be found/retrieved
	 */
	public Cursor fetchEntry(long rowId) throws SQLException {

		Cursor mCursor = mDb.query(true, DATABASE_TABLE, null, KEY_ROWID + "="
				+ rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
}

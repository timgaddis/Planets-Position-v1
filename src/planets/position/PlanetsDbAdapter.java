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
			+ "ra real, dec real, az real, alt real, dis real, mag integer, setT integer);";

	private static final String SE_DB_CREATE = "create table solarEcl "
			+ "(_id integer primary key autoincrement, localType integer,globalType integer,"
			+ "local integer,localMaxTime real,localFirstTime real,localSecondTime real,"
			+ "localThirdTime real,localFourthTime real,diaRatio real,fracCover real,sunAz real,"
			+ "sunAlt real,localMag real,sarosNum integer,sarosMemNum integer,moonAz real,"
			+ "moonAlt real,globalMaxTime real,globalBeginTime real,globalEndTime real,"
			+ "globalTotBegin real,globalTotEnd real,globalCenterBegin real,globalCenterEnd real);";

	private static final String LE_DB_CREATE = "create table lunarEcl "
			+ "(_id integer primary key autoincrement, type integer,local integer,maxEclTime real,"
			+ "partBegin real,partEnd real,totBegin real,totEnd real,penBegin real,penEnd real,"
			+ "eclipseMag real,penMag real,sarosNum integer,sarosMemNum integer);";

	private static final String DATABASE_NAME = "PlanetsDB";
	private static String DATABASE_TABLE;
	private static final int DATABASE_VERSION = 5;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			String ip1, ip2;
			db.execSQL(LOC_DB_CREATE);
			db.execSQL(insertRow);
			db.execSQL(PL_DB_CREATE);
			ip1 = "insert into planets "
					+ "(_id, name, ra, dec, az, alt, dis, mag, setT) VALUES (";
			ip2 = ", 'P', 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0);";
			for (int i = 0; i < 10; i++) {
				db.execSQL(ip1 + i + ip2);
			}
			db.execSQL(SE_DB_CREATE);
			ip1 = "insert into solarEcl "
					+ "(_id,localType,globalType,local,localMaxTime,localFirstTime,"
					+ "localSecondTime,localThirdTime,localFourthTime,diaRatio,"
					+ "fracCover,sunAz,sunAlt,localMag,sarosNum,sarosMemNum,moonAz,"
					+ "moonAlt,globalMaxTime,globalBeginTime,globalEndTime,globalTotBegin,"
					+ "globalTotEnd,globalCenterBegin,globalCenterEnd) VALUES (";
			ip2 = ",0,0,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0,0.0,"
					+ "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0);";
			for (int i = 0; i < 10; i++) {
				db.execSQL(ip1 + i + ip2);
			}
			db.execSQL(LE_DB_CREATE);
			ip1 = "insert into lunarEcl "
					+ "(_id,type,local,maxEclTime,partBegin,partEnd,totBegin,"
					+ "totEnd,penBegin,penEnd,eclipseMag,penMag,sarosNum,"
					+ "sarosMemNum) VALUES (";
			ip2 = ",0,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0);";
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
			db.execSQL("DROP TABLE IF EXISTS solarEcl");
			db.execSQL("DROP TABLE IF EXISTS lunarEcl");
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
			double az, double alt, double dis, long mag, long setT) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put("ra", ra);
		args.put("dec", dec);
		args.put("az", az);
		args.put("alt", alt);
		args.put("dis", dis);
		args.put("mag", mag);
		args.put("setT", setT);
		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowID, null) > 0;
	}

	public boolean updateSolar(int rowID, int localType, int globalType,
			int local, double localMaxTime, double localFirstTime,
			double localSecondTime, double localThirdTime,
			double localFourthTime, double diaRatio, double fracCover,
			double sunAz, double sunAlt, double localMag, int sarosNum,
			int sarosMemNum, double moonAz, double moonAlt,
			double globalMaxTime, double globalBeginTime, double globalEndTime,
			double globalTotBegin, double globalTotEnd,
			double globalCenterBegin, double globalCenterEnd) {
		ContentValues args = new ContentValues();
		args.put("localType", localType);
		args.put("globalType", globalType);
		args.put("local", local);
		args.put("localMaxTime", localMaxTime);
		args.put("localFirstTime", localFirstTime);
		args.put("localSecondTime", localSecondTime);
		args.put("localThirdTime", localThirdTime);
		args.put("localFourthTime", localFourthTime);
		args.put("diaRatio", diaRatio);
		args.put("fracCover", fracCover);
		args.put("sunAz", sunAz);
		args.put("sunAlt", sunAlt);
		args.put("localMag", localMag);
		args.put("sarosNum", sarosNum);
		args.put("sarosMemNum", sarosMemNum);
		args.put("moonAz", moonAz);
		args.put("moonAlt", moonAlt);
		args.put("globalMaxTime", globalMaxTime);
		args.put("globalBeginTime", globalBeginTime);
		args.put("globalEndTime", globalEndTime);
		args.put("globalTotBegin", globalTotBegin);
		args.put("globalTotEnd", globalTotEnd);
		args.put("globalCenterBegin", globalCenterBegin);
		args.put("globalCenterEnd", globalCenterEnd);
		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowID, null) > 0;
	}

	public boolean updateLunar(long rowID, int type, int local,
			double maxEclTime, double partBegin, double partEnd,
			double totBegin, double totEnd, double penBegin, double penEnd,
			double eclipseMag, double penMag, int sarosNum, int sarosMemNum) {
		ContentValues args = new ContentValues();
		args.put("type", type);
		args.put("local", local);
		args.put("maxEclTime", maxEclTime);
		args.put("partBegin", partBegin);
		args.put("partEnd", partEnd);
		args.put("totBegin", totBegin);
		args.put("totEnd", totEnd);
		args.put("penBegin", penBegin);
		args.put("penEnd", penEnd);
		args.put("eclipseMag", eclipseMag);
		args.put("penMag", penMag);
		args.put("sarosNum", sarosNum);
		args.put("sarosMemNum", sarosMemNum);
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

	/**
	 * Returns all of the planets above the horizon
	 * 
	 * @return A cursor with the data
	 */
	public Cursor fetchAllList() {
		// "alt > 0"
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_NAME,
				"az", "alt", "mag" }, "alt > 0", null, null, null, null);
	}

	/**
	 * Return all of the planets above the horizon and visible to the naked eye
	 * 
	 * @return A cursor with the data
	 */
	public Cursor fetchEyeList() {
		// "alt > 0 AND mag <= 6"
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_NAME,
				"az", "alt", "mag" }, "alt > 0 AND mag <= 6", null, null, null,
				null);
	}

	/**
	 * Returns all of the solar eclipses
	 * 
	 * @return A cursor with the data
	 */
	public Cursor fetchAllSolar() {
		// "alt > 0"
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, "az", "alt",
				"mag" }, null, null, null, null, null);
	}

	/**
	 * Returns all of the lunar eclipses
	 * 
	 * @return A cursor with the data
	 */
	public Cursor fetchAllLunar() {
		// "alt > 0"
		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, "az", "alt",
				"mag" }, null, null, null, null, null);
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

package planets.position.data;

/*
 * Copyright (C) 2012 Tim Gaddis
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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This is the helper class for the dynamically created location database.
 * 
 * @author Tim Gaddis
 * 
 */
public class PlanetsDbAdapter extends SQLiteOpenHelper {

	private static final String DEBUG_TAG = "PlanetsDbAdapter";
	private static final String DATABASE_NAME = "PlanetsDB";
	private static final int DATABASE_VERSION = 8;

	public static final String TABLE_LOCATION = "location";
	public static final String TABLE_PLANETS = "planets";
	public static final String TABLE_SOLAR_ECL = "solarEcl";
	public static final String TABLE_LUNAR_ECL = "lunarEcl";
	// public static final String KEY_NAME = "name";
	public static final String KEY_ROWID = "_id";
	// public static final String KEY_ALT = "alt";

	/**
	 * Database creation sql statements
	 */
	private static final String LOC_DB_CREATE = "create table location "
			+ "(_id integer primary key autoincrement, "
			+ "lat real, lng real, temp real, pressure real, elevation real, "
			+ "date integer, offset real, ioffset integer);";

	private static final String insertRow = "insert into location "
			+ "(_id, lat, lng, temp, pressure, elevation, date, offset, ioffset) "
			+ "VALUES (0, 91.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 13);";

	private static final String PL_DB_CREATE = "create table planets "
			+ "(_id integer primary key autoincrement, name text not null, "
			+ "ra real, dec real, az real, alt real, dis real, mag integer, setT integer);";

	private static final String SE_DB_CREATE = "create table solarEcl "
			+ "(_id integer primary key autoincrement, localType integer,globalType integer,"
			+ "local integer,localMaxTime real,localFirstTime real,localSecondTime real,"
			+ "localThirdTime real,localFourthTime real,diaRatio real,fracCover real,"
			+ "sunAz real,sunAlt real,localMag real,sarosNum integer,sarosMemNum integer,"
			+ "moonAz real,moonAlt real,globalMaxTime real,globalBeginTime real,"
			+ "globalEndTime real,globalTotBegin real,globalTotEnd real,"
			+ "globalCenterBegin real,globalCenterEnd real,eclipseDate text,eclipseType text);";

	private static final String LE_DB_CREATE = "create table lunarEcl "
			+ "(_id integer primary key autoincrement, type integer,local integer,maxEclTime real,"
			+ "partBegin real,partEnd real,totBegin real,totEnd real,penBegin real,penEnd real,"
			+ "eclipseMag real,sarosNum integer,sarosMemNum integer,"
			+ "eclipseDate text,eclipseType text,moonAz real,moonAlt real,rTime real,sTime real);";

	public PlanetsDbAdapter(Context context) {
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
				+ "globalTotEnd,globalCenterBegin,globalCenterEnd,eclipseDate,"
				+ "eclipseType) VALUES (";
		ip2 = ",0,0,-1,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0,0.0,"
				+ "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,'D','T');";
		for (int i = 0; i < 8; i++) {
			db.execSQL(ip1 + i + ip2);
		}
		db.execSQL(LE_DB_CREATE);
		ip1 = "insert into lunarEcl "
				+ "(_id,type,local,maxEclTime,partBegin,partEnd,totBegin,"
				+ "totEnd,penBegin,penEnd,eclipseMag,sarosNum,"
				+ "sarosMemNum,eclipseDate,eclipseType,moonAz,moonAlt,rTime,"
				+ "sTime) VALUES (";
		ip2 = ",0,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0,0,'D','T',0.0,0.0,"
				+ "0.0,0.0);";
		for (int i = 0; i < 8; i++) {
			db.execSQL(ip1 + i + ip2);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DEBUG_TAG, "Upgrading database from version " + oldVersion
				+ " to " + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS location");
		db.execSQL("DROP TABLE IF EXISTS planets");
		db.execSQL("DROP TABLE IF EXISTS solarEcl");
		db.execSQL("DROP TABLE IF EXISTS lunarEcl");
		onCreate(db);
	}
}

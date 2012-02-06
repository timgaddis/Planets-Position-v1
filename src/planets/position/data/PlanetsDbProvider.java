package planets.position.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class PlanetsDbProvider extends ContentProvider {
	private PlanetsDbAdapter mDB;
	private static final String AUTHORITY = "planets.position.data.planetsdbprovider";
	public static final int LOCATION = 90;
	public static final int LOCATION_ID = 100;
	public static final int PLANETS = 110;
	public static final int PLANETS_ID = 120;
	public static final int SOLAR = 130;
	public static final int SOLAR_ID = 140;
	public static final int LUNAR = 150;
	public static final int LUNAR_ID = 160;

	public static final String LOCATION_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/pp-location";
	public static final String LOCATION_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/pp-location";
	public static final String PLANET_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/pp-planet";
	public static final String PLANET_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/pp-planet";
	public static final String SLOAR_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/pp-solar";
	public static final String SLOAR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/pp-solar";
	public static final String LUNAR_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/pp-lunar";
	public static final String LUNAR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/pp-lunar";

	public static final Uri LOCATION_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + PlanetsDbAdapter.TABLE_LOCATION);
	public static final Uri PLANETS_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + PlanetsDbAdapter.TABLE_PLANETS);
	public static final Uri SOLAR_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + PlanetsDbAdapter.TABLE_SOLAR_ECL);
	public static final Uri LUNAR_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + PlanetsDbAdapter.TABLE_LUNAR_ECL);

	private static final UriMatcher URIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		URIMatcher.addURI(AUTHORITY, PlanetsDbAdapter.TABLE_LOCATION, LOCATION);
		URIMatcher.addURI(AUTHORITY, PlanetsDbAdapter.TABLE_LOCATION + "/#",
				LOCATION_ID);
		URIMatcher.addURI(AUTHORITY, PlanetsDbAdapter.TABLE_PLANETS, PLANETS);
		URIMatcher.addURI(AUTHORITY, PlanetsDbAdapter.TABLE_PLANETS + "/#",
				PLANETS_ID);
		URIMatcher.addURI(AUTHORITY, PlanetsDbAdapter.TABLE_SOLAR_ECL, SOLAR);
		URIMatcher.addURI(AUTHORITY, PlanetsDbAdapter.TABLE_SOLAR_ECL + "/#",
				SOLAR_ID);
		URIMatcher.addURI(AUTHORITY, PlanetsDbAdapter.TABLE_LUNAR_ECL, LUNAR);
		URIMatcher.addURI(AUTHORITY, PlanetsDbAdapter.TABLE_LUNAR_ECL + "/#",
				LUNAR_ID);
	}

	@Override
	public boolean onCreate() {
		mDB = new PlanetsDbAdapter(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		// Log.i("PlanetsDbProvider query", "uri: " + uri);
		int uriType = URIMatcher.match(uri);
		// Log.i("PlanetsDbProvider query", "uriType: " + uriType);
		switch (uriType) {
		case LOCATION:
			queryBuilder.setTables(PlanetsDbAdapter.TABLE_LOCATION);
			break;
		case LOCATION_ID:
			queryBuilder.setTables(PlanetsDbAdapter.TABLE_LOCATION);
			queryBuilder.appendWhere(PlanetsDbAdapter.KEY_ROWID + "="
					+ uri.getLastPathSegment());
			break;
		case PLANETS:
			queryBuilder.setTables(PlanetsDbAdapter.TABLE_PLANETS);
			// queryBuilder.appendWhere(PlanetsDbAdapter.KEY_ALT + "> 0.0");
			break;
		case PLANETS_ID:
			queryBuilder.setTables(PlanetsDbAdapter.TABLE_PLANETS);
			queryBuilder.appendWhere(PlanetsDbAdapter.KEY_ROWID + "="
					+ uri.getLastPathSegment());
			break;
		case SOLAR:
			queryBuilder.setTables(PlanetsDbAdapter.TABLE_SOLAR_ECL);
			// no filter
			break;
		case SOLAR_ID:
			queryBuilder.setTables(PlanetsDbAdapter.TABLE_SOLAR_ECL);
			queryBuilder.appendWhere(PlanetsDbAdapter.KEY_ROWID + "="
					+ uri.getLastPathSegment());
			break;
		case LUNAR:
			queryBuilder.setTables(PlanetsDbAdapter.TABLE_LUNAR_ECL);
			// no filter
			break;
		case LUNAR_ID:
			queryBuilder.setTables(PlanetsDbAdapter.TABLE_LUNAR_ECL);
			queryBuilder.appendWhere(PlanetsDbAdapter.KEY_ROWID + "="
					+ uri.getLastPathSegment());
			break;
		default:
			Log.e("PlanetsDbProvider error", "query Unknown URI error: " + uri);
			throw new IllegalArgumentException("Unknown URI");
		}

		Cursor cursor = queryBuilder.query(mDB.getReadableDatabase(),
				projection, selection, selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase sqlDB = mDB.getWritableDatabase();
		int rowsAffected = 0;
		String id;
		int uriType = URIMatcher.match(uri);
		switch (uriType) {
		case LOCATION:
			rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_LOCATION,
					selection, selectionArgs);
			break;
		case LOCATION_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_LOCATION,
						PlanetsDbAdapter.KEY_ROWID + "=" + id, null);
			} else {
				rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_LOCATION,
						selection + " AND " + PlanetsDbAdapter.KEY_ROWID + "="
								+ id, selectionArgs);
			}
			break;
		case PLANETS:
			rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_PLANETS,
					selection, selectionArgs);
			break;
		case PLANETS_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_PLANETS,
						PlanetsDbAdapter.KEY_ROWID + "=" + id, null);
			} else {
				rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_PLANETS,
						selection + " AND " + PlanetsDbAdapter.KEY_ROWID + "="
								+ id, selectionArgs);
			}
			break;
		case SOLAR:
			rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_SOLAR_ECL,
					selection, selectionArgs);
			break;
		case SOLAR_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_SOLAR_ECL,
						PlanetsDbAdapter.KEY_ROWID + "=" + id, null);
			} else {
				rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_SOLAR_ECL,
						selection + " AND " + PlanetsDbAdapter.KEY_ROWID + "="
								+ id, selectionArgs);
			}
			break;
		case LUNAR:
			rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_LUNAR_ECL,
					selection, selectionArgs);
			break;
		case LUNAR_ID:
			id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_LUNAR_ECL,
						PlanetsDbAdapter.KEY_ROWID + "=" + id, null);
			} else {
				rowsAffected = sqlDB.delete(PlanetsDbAdapter.TABLE_LUNAR_ECL,
						selection + " AND " + PlanetsDbAdapter.KEY_ROWID + "="
								+ id, selectionArgs);
			}
			break;
		default:
			Log.e("PlanetsDbProvider error", "delete Unknown or Invalid URI "
					+ uri);
			throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsAffected;
	}

	@Override
	public String getType(Uri uri) {
		int uriType = URIMatcher.match(uri);
		switch (uriType) {
		case LOCATION:
			return LOCATION_TYPE;
		case LOCATION_ID:
			return LOCATION_ITEM_TYPE;
		case PLANETS:
			return PLANET_TYPE;
		case PLANETS_ID:
			return PLANET_ITEM_TYPE;
		case SOLAR:
			return SLOAR_TYPE;
		case SOLAR_ID:
			return SLOAR_ITEM_TYPE;
		case LUNAR:
			return LUNAR_TYPE;
		case LUNAR_ID:
			return LUNAR_ITEM_TYPE;
		default:
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table;
		int uriType = URIMatcher.match(uri);
		switch (uriType) {
		case LOCATION:
			table = PlanetsDbAdapter.TABLE_LOCATION;
			break;
		case LOCATION_ID:
			Log.e("PlanetsDbProvider error", "insert LOCATION_ID error");
			throw new IllegalArgumentException("Invalid URI for insert");
		case PLANETS:
			table = PlanetsDbAdapter.TABLE_PLANETS;
			break;
		case PLANETS_ID:
			Log.e("PlanetsDbProvider error", "insert PLANETS_ID error");
			throw new IllegalArgumentException("Invalid URI for insert");
		case SOLAR:
			table = PlanetsDbAdapter.TABLE_SOLAR_ECL;
			break;
		case SOLAR_ID:
			Log.e("PlanetsDbProvider error", "insert SOLAR_ID error");
			throw new IllegalArgumentException("Invalid URI for insert");
		case LUNAR:
			table = PlanetsDbAdapter.TABLE_LUNAR_ECL;
			break;
		case LUNAR_ID:
			Log.e("PlanetsDbProvider error", "insert LUNAR_ID error");
			throw new IllegalArgumentException("Invalid URI for insert");
		default:
			Log.e("PlanetsDbProvider error", "insert Unknown URI error: " + uri);
			throw new IllegalArgumentException("Unknown URI");
		}
		SQLiteDatabase sqlDB = mDB.getWritableDatabase();
		long newID = sqlDB.insert(table, null, values);
		if (newID > 0) {
			Uri newUri = ContentUris.withAppendedId(uri, newID);
			getContext().getContentResolver().notifyChange(uri, null);
			return newUri;
		} else {
			Log.e("PlanetsDbProvider error", "Failed to insert row into " + uri);
			throw new SQLException("Failed to insert row into " + uri);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int rowsAffected;
		String id;
		StringBuilder modSelection;
		SQLiteDatabase sqlDB = mDB.getWritableDatabase();
		int uriType = URIMatcher.match(uri);
		switch (uriType) {
		case LOCATION:
			rowsAffected = sqlDB.update(PlanetsDbAdapter.TABLE_LOCATION,
					values, selection, selectionArgs);
			break;
		case LOCATION_ID:
			id = uri.getLastPathSegment();
			modSelection = new StringBuilder(PlanetsDbAdapter.KEY_ROWID + "="
					+ id);
			if (!TextUtils.isEmpty(selection)) {
				modSelection.append(" AND " + selection);
			}
			rowsAffected = sqlDB.update(PlanetsDbAdapter.TABLE_LOCATION,
					values, modSelection.toString(), null);
			break;
		case PLANETS:
			rowsAffected = sqlDB.update(PlanetsDbAdapter.TABLE_PLANETS, values,
					selection, selectionArgs);
			break;
		case PLANETS_ID:
			id = uri.getLastPathSegment();
			modSelection = new StringBuilder(PlanetsDbAdapter.KEY_ROWID + "="
					+ id);
			if (!TextUtils.isEmpty(selection)) {
				modSelection.append(" AND " + selection);
			}
			rowsAffected = sqlDB.update(PlanetsDbAdapter.TABLE_PLANETS, values,
					modSelection.toString(), null);
			break;
		case SOLAR:
			rowsAffected = sqlDB.update(PlanetsDbAdapter.TABLE_SOLAR_ECL,
					values, selection, selectionArgs);
			break;
		case SOLAR_ID:
			id = uri.getLastPathSegment();
			modSelection = new StringBuilder(PlanetsDbAdapter.KEY_ROWID + "="
					+ id);
			if (!TextUtils.isEmpty(selection)) {
				modSelection.append(" AND " + selection);
			}
			rowsAffected = sqlDB.update(PlanetsDbAdapter.TABLE_SOLAR_ECL,
					values, modSelection.toString(), null);
			break;
		case LUNAR:
			rowsAffected = sqlDB.update(PlanetsDbAdapter.TABLE_LUNAR_ECL,
					values, selection, selectionArgs);
			break;
		case LUNAR_ID:
			id = uri.getLastPathSegment();
			modSelection = new StringBuilder(PlanetsDbAdapter.KEY_ROWID + "="
					+ id);
			if (!TextUtils.isEmpty(selection)) {
				modSelection.append(" AND " + selection);
			}
			rowsAffected = sqlDB.update(PlanetsDbAdapter.TABLE_LUNAR_ECL,
					values, modSelection.toString(), null);
			break;
		default:
			Log.e("PlanetsDbProvider error", "update Unknown URI error: " + uri);
			throw new IllegalArgumentException("Unknown URI");
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsAffected;
	}
}

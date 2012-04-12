package planets.position;

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

import java.util.Calendar;

import planets.position.data.PlanetsDbAdapter;
import planets.position.data.PlanetsDbProvider;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class EclipseData extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private TextView eclDateText, eclTypeText, eclGlobalDataText,
			eclLocalDataText;
	private int eclipseNum;
	private boolean helpSE, localEcl;
	private double offset;

	private static final int SOLAR_LOADER = 1;
	private static final int LUNAR_LOADER = 2;
	private static final int SOLAR_LOCAL_LOADER = 3;
	private static final int LUNAR_LOCAL_LOADER = 4;
	private String[] le_projection = { PlanetsDbAdapter.KEY_ROWID,
			"eclipseType", "eclipseDate", "penBegin", "partBegin", "totBegin",
			"maxEclTime", "totEnd", "partEnd", "penEnd" };
	private String[] se_projection = { PlanetsDbAdapter.KEY_ROWID,
			"eclipseType", "eclipseDate", "globalBeginTime", "globalTotBegin",
			"globalMaxTime", "globalTotEnd", "globalEndTime" };
	private String[] lel_projection = { PlanetsDbAdapter.KEY_ROWID,
			"eclipseType", "eclipseDate", "penBegin", "partBegin", "totBegin",
			"maxEclTime", "totEnd", "partEnd", "penEnd", "moonAz", "moonAlt",
			"eclipseMag", "sarosNum", "sarosMemNum", "rTime", "sTime" };
	private String[] sel_projection = { PlanetsDbAdapter.KEY_ROWID,
			"eclipseType", "eclipseDate", "globalBeginTime", "globalTotBegin",
			"globalMaxTime", "globalTotEnd", "globalEndTime", "localType",
			"localFirstTime", "localSecondTime", "localMaxTime",
			"localThirdTime", "localFourthTime", "sunAz", "sunAlt",
			"fracCover", "localMag", "sarosNum", "sarosMemNum" };
	private ContentResolver cr;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native String jd2utc(double jdate);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.eclipse_data);

		eclDateText = (TextView) findViewById(R.id.ecl_date_text);
		eclTypeText = (TextView) findViewById(R.id.ecl_type_text);
		eclGlobalDataText = (TextView) findViewById(R.id.ecl_globalData);
		eclLocalDataText = (TextView) findViewById(R.id.ecl_localData);

		cr = getApplicationContext().getContentResolver();
		// load bundle from previous activity
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			eclipseNum = bundle.getInt("eclipseNum", 0);
			offset = bundle.getDouble("Offset", 0);
			localEcl = bundle.getBoolean("local");
			if (bundle.getBoolean("db")) {
				if (localEcl) {
					getSupportLoaderManager().initLoader(SOLAR_LOCAL_LOADER,
							null, this);
				} else {
					getSupportLoaderManager().initLoader(SOLAR_LOADER, null,
							this);
				}
				helpSE = true;
				fillSolarData();
			} else {
				if (localEcl) {
					getSupportLoaderManager().initLoader(LUNAR_LOCAL_LOADER,
							null, this);
				} else {
					getSupportLoaderManager().initLoader(LUNAR_LOADER, null,
							this);
				}
				helpSE = false;
				fillLunarData();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		setResult(RESULT_OK);
		finish();
	}

	private void fillLunarData() {
		String localData, globalData;
		Cursor eclipseCursor;
		if (localEcl) {
			eclipseCursor = cr.query(
					Uri.withAppendedPath(PlanetsDbProvider.LUNAR_URI,
							String.valueOf(eclipseNum)), lel_projection, null,
					null, null);
		} else {
			eclipseCursor = cr.query(
					Uri.withAppendedPath(PlanetsDbProvider.LUNAR_URI,
							String.valueOf(eclipseNum)), le_projection, null,
					null, null);
		}
		eclipseCursor.moveToFirst();
		eclTypeText.setText(eclipseCursor.getString(eclipseCursor
				.getColumnIndexOrThrow("eclipseType")) + " Eclipse");
		eclDateText.setText(eclipseCursor.getString(eclipseCursor
				.getColumnIndexOrThrow("eclipseDate")));
		globalData = "Eclipse Times (UTC)\n";// -------------------\n";
		globalData += String.format(
				"%-16s%13s\n",
				"Penumbral Start",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("penBegin")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Partial Start",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("partBegin")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Totality Start",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("totBegin")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Maximum Eclipse",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("maxEclTime")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Totality End",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("totEnd")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Partial End",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("partEnd")), false));
		globalData += String.format(
				"%-16s%13s",
				"Penumbral End",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("penEnd")), false));
		eclGlobalDataText.setText(globalData);
		if (localEcl) {
			// local eclipse
			localData = "Local Eclipse Data\n";// ------------------\n";
			localData += String.format(
					"%-16s%13s\n",
					"Penumbral Start",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("penBegin")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Partial Start",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("partBegin")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Totality Start",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("totBegin")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Maximum Eclipse",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("maxEclTime")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Totality End",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("totEnd")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Partial End",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("partEnd")), true));
			localData += String.format(
					"%-16s%13s\n\n",
					"Penumbral End",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("penEnd")), true));
			localData += "Moon Position @ Max Eclipse\n";
			localData += String.format("%-17s%8.1f\u00b0\n", "Azimuth",
					eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("moonAz")));
			localData += String.format("%-17s%8.1f\u00b0\n\n", "Altitude",
					eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("moonAlt")));
			// localData += String.format(
			// "%-13s%13s\n",
			// "Moon Rise",
			// convertDate(planetCur.getDouble(planetCur
			// .getColumnIndexOrThrow("rTime")), true));
			// localData += String.format(
			// "%-13s%13s\n",
			// "Moon Set",
			// convertDate(planetCur.getDouble(planetCur
			// .getColumnIndexOrThrow("sTime")), true));
			localData += String.format("%-18s%8.1f\n", "Magnitude",
					eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("eclipseMag")));
			localData += String.format("%-18s%8d\n", "Saros Number",
					eclipseCursor.getInt(eclipseCursor
							.getColumnIndexOrThrow("sarosNum")));
			localData += String.format("%-18s%8d", "Saros Member #",
					eclipseCursor.getInt(eclipseCursor
							.getColumnIndexOrThrow("sarosMemNum")));
			eclLocalDataText.setText(localData);
		} else {
			eclLocalDataText.setVisibility(View.GONE);
		}
		// planetDbHelper.close();
	}

	private void fillSolarData() {
		String eclType, localData, globalData;
		int val;
		Cursor eclipseCursor;
		if (localEcl) {
			eclipseCursor = cr.query(
					Uri.withAppendedPath(PlanetsDbProvider.SOLAR_URI,
							String.valueOf(eclipseNum)), sel_projection, null,
					null, null);
		} else {
			eclipseCursor = cr.query(
					Uri.withAppendedPath(PlanetsDbProvider.SOLAR_URI,
							String.valueOf(eclipseNum)), se_projection, null,
					null, null);
		}
		eclipseCursor.moveToFirst();
		eclTypeText.setText(eclipseCursor.getString(eclipseCursor
				.getColumnIndexOrThrow("eclipseType")) + " Eclipse");
		eclDateText.setText(eclipseCursor.getString(eclipseCursor
				.getColumnIndexOrThrow("eclipseDate")));
		globalData = "Eclipse Times (UTC)\n";// -------------------\n";
		globalData += String.format(
				"%-16s%13s\n",
				"Eclipse Start",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("globalBeginTime")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Totality Start",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("globalTotBegin")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Maximum Eclipse",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("globalMaxTime")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Totality End",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("globalTotEnd")), false));
		globalData += String.format(
				"%-16s%13s",
				"Eclipse End",
				convertDate(eclipseCursor.getDouble(eclipseCursor
						.getColumnIndexOrThrow("globalEndTime")), false));
		eclGlobalDataText.setText(globalData);

		if (localEcl) {
			// local eclipse
			val = eclipseCursor.getInt(eclipseCursor
					.getColumnIndexOrThrow("localType"));
			if ((val & 4) == 4) // SE_ECL_TOTAL
				eclType = "Total Eclipse";
			else if ((val & 8) == 8) // SE_ECL_ANNULAR
				eclType = "Annular Eclipse";
			else if ((val & 16) == 16) // SE_ECL_PARTIAL
				eclType = "Partial Eclipse";
			else if ((val & 32) == 32) // SE_ECL_ANNULAR_TOTAL
				eclType = "Hybrid Eclipse";
			else
				eclType = "Other Eclipse";
			localData = "Local Eclipse Data\n";// ------------------\n";
			localData += "Type: " + eclType + "\n";
			localData += String.format(
					"%-16s%13s\n",
					"Eclipse Start",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("localFirstTime")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Totality Start",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("localSecondTime")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Maximum Eclipse",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("localMaxTime")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Totality End",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("localThirdTime")), true));
			localData += String.format(
					"%-16s%13s\n\n",
					"Eclipse End",
					convertDate(eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("localFourthTime")), true));
			localData += "Sun Position @ Max Eclipse\n";
			localData += String.format("%-17s%8.1f\u00b0\n", "Azimuth",
					eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("sunAz")));
			localData += String.format("%-17s%8.1f\u00b0\n\n", "Altitude",
					eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("sunAlt")));
			localData += String.format("%-16s%7.1f%%\n", "Sun Coverage",
					eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("fracCover")) * 100);
			localData += String.format("%-16s%8.1f\n", "Magnitude",
					eclipseCursor.getDouble(eclipseCursor
							.getColumnIndexOrThrow("localMag")));
			localData += String.format("%-16s%8d\n", "Saros Number",
					eclipseCursor.getInt(eclipseCursor
							.getColumnIndexOrThrow("sarosNum")));
			localData += String.format("%-16s%8d", "Saros Member #",
					eclipseCursor.getInt(eclipseCursor
							.getColumnIndexOrThrow("sarosMemNum")));
			eclLocalDataText.setText(localData);
		} else {
			eclLocalDataText.setVisibility(View.GONE);
		}
	}

	/**
	 * Converts the given Julian Date to a String
	 * 
	 * @param jd
	 *            - Julian Date to convert
	 * @param local
	 *            - Set if date/time is local
	 * @return CharSequence of Date
	 */
	private CharSequence convertDate(double jd, boolean local) {
		if (jd > 0.0) {
			Calendar c = Calendar.getInstance();

			String[] dateArr = jd2utc(jd).split("_");
			c.set(Integer.parseInt(dateArr[1]),
					Integer.parseInt(dateArr[2]) - 1,
					Integer.parseInt(dateArr[3]), Integer.parseInt(dateArr[4]),
					Integer.parseInt(dateArr[5]));
			c.set(Calendar.MILLISECOND,
					(int) (Double.parseDouble(dateArr[6]) * 1000));
			if (local) {
				// convert c to local time
				c.add(Calendar.MINUTE, (int) (offset * 60));
			}
			return DateFormat.format("MMM d kk:mm", c);
		} else {
			return "-N/A-   ";
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.help_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Bundle b;
		Intent i;
		switch (item.getItemId()) {
		case R.id.id_menu_help:
			b = new Bundle();
			if (helpSE)
				b.putInt("res", R.string.solarEcl_help);
			else
				b.putInt("res", R.string.lunarEcl_help);
			i = new Intent(this, About.class);
			i.putExtras(b);
			startActivity(i);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	// *** Loader Manager methods ***
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader cursorLoader = null;
		if (id == SOLAR_LOADER) {
			if (localEcl) {
				cursorLoader = new CursorLoader(this,
						PlanetsDbProvider.SOLAR_URI, sel_projection, null,
						null, null);
			} else {
				cursorLoader = new CursorLoader(this,
						PlanetsDbProvider.SOLAR_URI, se_projection, null, null,
						null);
			}
		} else if (id == LUNAR_LOADER) {
			if (localEcl) {
				cursorLoader = new CursorLoader(this,
						PlanetsDbProvider.LUNAR_URI, lel_projection, null,
						null, null);
			} else {
				cursorLoader = new CursorLoader(this,
						PlanetsDbProvider.LUNAR_URI, le_projection, null, null,
						null);
			}
		}
		return cursorLoader;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
	}
}

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

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class EclipseData extends Activity {

	private TextView eclDateText, eclTypeText, eclGlobalDataText,
			eclLocalDataText;
	private int eclipseNum;
	private boolean helpSE;
	private double offset;
	private PlanetsDbAdapter planetDbHelper;

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

		// load bundle from previous activity
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			eclipseNum = bundle.getInt("eclipseNum", 0);
			offset = bundle.getDouble("Offset", 0);
			if (bundle.getBoolean("db")) {
				planetDbHelper = new PlanetsDbAdapter(this, "solarEcl");
				helpSE = true;
				fillSolarData();
			} else {
				planetDbHelper = new PlanetsDbAdapter(this, "lunarEcl");
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
		planetDbHelper.open();
		Cursor planetCur = planetDbHelper.fetchEntry(eclipseNum);
		startManagingCursor(planetCur);
		eclTypeText.setText(planetCur.getString(planetCur
				.getColumnIndexOrThrow("eclipseType")) + " Eclipse");
		eclDateText.setText(planetCur.getString(planetCur
				.getColumnIndexOrThrow("eclipseDate")));
		globalData = "Eclipse Times (UTC)\n";// -------------------\n";
		globalData += String.format(
				"%-16s%13s\n",
				"Penumbral Start",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("penBegin")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Partial Start",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("partBegin")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Totality Start",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("totBegin")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Maximum Eclipse",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("maxEclTime")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Totality End",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("totEnd")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Partial End",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("partEnd")), false));
		globalData += String.format(
				"%-16s%13s",
				"Penumbral End",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("penEnd")), false));
		eclGlobalDataText.setText(globalData);
		if (planetCur.getInt(planetCur.getColumnIndexOrThrow("local")) > 0) {
			// local eclipse
			localData = "Local Eclipse Data\n";// ------------------\n";
			localData += String.format(
					"%-16s%13s\n",
					"Penumbral Start",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("penBegin")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Partial Start",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("partBegin")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Totality Start",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("totBegin")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Maximum Eclipse",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("maxEclTime")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Totality End",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("totEnd")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Partial End",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("partEnd")), true));
			localData += String.format(
					"%-16s%13s\n\n",
					"Penumbral End",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("penEnd")), true));
			localData += "Moon Position @ Max Eclipse\n";
			localData += String.format("%-17s%8.1f\u00b0\n", "Azimuth",
					planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("moonAz")));
			localData += String.format("%-17s%8.1f\u00b0\n\n", "Altitude",
					planetCur.getDouble(planetCur
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
			localData += String.format("%-18s%8.1f\n", "Magnitude", planetCur
					.getDouble(planetCur.getColumnIndexOrThrow("eclipseMag")));
			localData += String.format("%-18s%8d\n", "Saros Number", planetCur
					.getInt(planetCur.getColumnIndexOrThrow("sarosNum")));
			localData += String.format("%-18s%8d", "Saros Member #", planetCur
					.getInt(planetCur.getColumnIndexOrThrow("sarosMemNum")));
			eclLocalDataText.setText(localData);
		} else {
			eclLocalDataText.setVisibility(View.GONE);
		}
		planetDbHelper.close();
	}

	private void fillSolarData() {
		String eclType, localData, globalData;
		int val;
		planetDbHelper.open();
		Cursor planetCur = planetDbHelper.fetchEntry(eclipseNum);
		startManagingCursor(planetCur);
		eclTypeText.setText(planetCur.getString(planetCur
				.getColumnIndexOrThrow("eclipseType")) + " Eclipse");
		eclDateText.setText(planetCur.getString(planetCur
				.getColumnIndexOrThrow("eclipseDate")));
		globalData = "Eclipse Times (UTC)\n";// -------------------\n";
		globalData += String.format(
				"%-16s%13s\n",
				"Eclipse Start",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("globalBeginTime")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Totality Start",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("globalTotBegin")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Maximum Eclipse",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("globalMaxTime")), false));
		globalData += String.format(
				"%-16s%13s\n",
				"Totality End",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("globalTotEnd")), false));
		globalData += String.format(
				"%-16s%13s",
				"Eclipse End",
				convertDate(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("globalEndTime")), false));
		eclGlobalDataText.setText(globalData);

		if (planetCur.getInt(planetCur.getColumnIndexOrThrow("local")) > 0) {
			// local eclipse
			val = planetCur
					.getInt(planetCur.getColumnIndexOrThrow("localType"));
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
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("localFirstTime")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Totality Start",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("localSecondTime")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Maximum Eclipse",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("localMaxTime")), true));
			localData += String.format(
					"%-16s%13s\n",
					"Totality End",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("localThirdTime")), true));
			localData += String.format(
					"%-16s%13s\n\n",
					"Eclipse End",
					convertDate(planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("localFourthTime")), true));
			localData += "Sun Position @ Max Eclipse\n";
			localData += String.format("%-17s%8.1f\u00b0\n", "Azimuth",
					planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("sunAz")));
			localData += String.format("%-17s%8.1f\u00b0\n\n", "Altitude",
					planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("sunAlt")));
			localData += String.format("%-16s%7.1f%%\n", "Sun Coverage",
					planetCur.getDouble(planetCur
							.getColumnIndexOrThrow("fracCover")) * 100);
			localData += String.format("%-16s%8.1f\n", "Magnitude", planetCur
					.getDouble(planetCur.getColumnIndexOrThrow("localMag")));
			localData += String.format("%-16s%8d\n", "Saros Number", planetCur
					.getInt(planetCur.getColumnIndexOrThrow("sarosNum")));
			localData += String.format("%-16s%8d", "Saros Member #", planetCur
					.getInt(planetCur.getColumnIndexOrThrow("sarosMemNum")));
			eclLocalDataText.setText(localData);
		} else {
			eclLocalDataText.setVisibility(View.GONE);
		}
		planetDbHelper.close();
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
}

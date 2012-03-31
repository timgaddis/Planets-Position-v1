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
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

public class SolarEclipse extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private Button prevEclButton, nextEclButton;
	private ListView eclipseList;
	private double[] time, g = new double[3];
	private double offset, firstEcl, lastEcl;
	private Calendar c;
	private final int ECLIPSE_DATA = 0;
	private static final int SOLAR_LOADER = 1;
	private String[] projection = { PlanetsDbAdapter.KEY_ROWID, "eclipseDate",
			"eclipseType", "local" };
	private SimpleCursorAdapter cursorAdapter;
	private ContentResolver cr;
	private DialogFragment eclipseDialog;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native double[] solarDataPos(double d2);

	public native double[] solarDataLocal(double d2, double[] loc, int back);

	public native double[] solarDataGlobal(double d2, int back);

	public native double[] utc2jd(int m, int d, int y, int hr, int min,
			double sec);

	public native String jd2utc(double jdate);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.solar_main);

		prevEclButton = (Button) findViewById(R.id.prevSEclButton);
		nextEclButton = (Button) findViewById(R.id.nextSEclButton);
		eclipseList = (ListView) findViewById(R.id.solarEclList);

		// load bundle from previous activity
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
		}

		cr = getApplicationContext().getContentResolver();
		getSupportLoaderManager().initLoader(SOLAR_LOADER, null, this);

		c = Calendar.getInstance();
		// convert local time to utc
		c.add(Calendar.MINUTE, (int) (offset * -60));
		// calculate Julian date
		time = utc2jd(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
				c.get(Calendar.YEAR), c.get(Calendar.HOUR_OF_DAY),
				c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
		if (time == null) {
			Log.e("Solar Eclipse error", "utc2jd error");
			Toast.makeText(getApplicationContext(),
					"Date conversion error,\nplease restart the activity",
					Toast.LENGTH_LONG).show();
			this.finish();
		}
		// jdTT = time[0];
		// jdUT = time[1];

		new ComputeEclipsesTask().execute(time[1], 0.0);

		prevEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new ComputeEclipsesTask().execute(firstEcl, 1.0);
			}
		});

		nextEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new ComputeEclipsesTask().execute(lastEcl, 0.0);
			}
		});

		eclipseList.setOnItemClickListener(new EclipseSelectedListener());
	}

	private void fillData() {
		Cursor eclCursor = cr.query(PlanetsDbProvider.SOLAR_URI, projection,
				null, null, "globalBeginTime");
		String[] from = new String[] { "eclipseDate", "eclipseType", "local" };
		int[] to = new int[] { R.id.eclDate, R.id.eclType, R.id.eclLocal };
		// Now create a simple cursor adapter and set it to display
		cursorAdapter = new SimpleCursorAdapter(getApplicationContext(),
				R.layout.ecl_row, eclCursor, from, to, 0);
		// Binds the 'local' field in the db to the checked attribute for the
		// CheckBox
		cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				if (columnIndex == 3) {
					CheckBox cb = (CheckBox) view;
					cb.setChecked(cursor.getInt(3) > 0);
					return true;
				}
				return false;
			}
		});
		eclipseList.setAdapter(cursorAdapter);
	}

	/**
	 * Computes the eclipses in a separate thread.
	 * 
	 * @author tgaddis
	 * @params double list for doInBackground: (start date,
	 *         direction(forward=0.0/back=1.0))
	 * 
	 */
	private class ComputeEclipsesTask extends AsyncTask<Double, Void, Void> {
		String eclDate, eclType;
		ContentValues values;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			values = new ContentValues();
			eclipseList.setVisibility(View.INVISIBLE);
			eclipseDialog = CalcDialog.newInstance(R.string.eclipse_dialog);
			eclipseDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
			eclipseDialog.show(getSupportFragmentManager(), "eclipseDialog");
		}

		@Override
		protected Void doInBackground(Double... params) {
			// compute next or previous 8 eclipses and save them to the DB
			// startTime is in UTC
			double start;
			double[] data1 = null, data2 = null;
			int i, backward;

			backward = (int) Math.round(params[1]);
			start = params[0];

			// Log.i("Solar Eclipse Type", "params[2]: " + params[2]);
			data2 = solarDataLocal(start, g, backward);
			if (data2 == null) {
				Log.e("Solar Eclipse error", "computeEclipses data1 error");
				Toast.makeText(
						getApplicationContext(),
						"computeEclipses error 1,\nplease restart the activity",
						Toast.LENGTH_LONG).show();
				finish();
			}
			Log.i("Solar Eclipse", "Local date1: " + data2[1]);

			for (i = 0; i < 8; i++) {
				values.clear();
				// ***************************************
				// Global Eclipse Calculations
				// ***************************************
				data1 = solarDataGlobal(start, backward);
				if (data1 == null) {
					Log.e("Solar Eclipse error", "computeEclipses data1g error");
					Toast.makeText(
							getApplicationContext(),
							"computeEclipses error 3,\nplease restart the activity",
							Toast.LENGTH_LONG).show();
					break;
				}
				// Log.i("Solar Eclipse", "Global number: " + (i + 1)
				// + " - date: " + data1[1]);
				// save the beginning time of the eclipse
				if (i == 0)
					if (backward == 0)
						firstEcl = data1[3];
					else
						lastEcl = data1[4];
				// save the ending time of the eclipse
				if (i == 7)
					if (backward == 0)
						lastEcl = data1[4];
					else
						firstEcl = data1[3];

				// create date string use data1[1]
				String[] dateArr = jd2utc(data1[1]).split("_");
				c.set(Integer.parseInt(dateArr[1]),
						Integer.parseInt(dateArr[2]) - 1,
						Integer.parseInt(dateArr[3]),
						Integer.parseInt(dateArr[4]),
						Integer.parseInt(dateArr[5]));
				c.set(Calendar.MILLISECOND,
						(int) (Double.parseDouble(dateArr[6]) * 1000));
				eclDate = (DateFormat.format("dd MMM yyyy", c)).toString();

				// create type string use data1[0]
				int val = (int) data1[0];
				if ((val & 4) == 4) // SE_ECL_TOTAL
					eclType = "Total";
				else if ((val & 8) == 8) // SE_ECL_ANNULAR
					eclType = "Annular";
				else if ((val & 16) == 16) // SE_ECL_PARTIAL
					eclType = "Partial";
				else if ((val & 32) == 32) // SE_ECL_ANNULAR_TOTAL
					eclType = "Hybrid";
				else
					eclType = "Other";

				if (Math.abs(data2[1] - data1[1]) <= 1.0) {
					// if local eclipse time is within one day of the global
					// time, then eclipse is visible locally
					values.put("localType", (int) data2[0]);
					values.put("globalType", (int) data1[0]);
					values.put("local", 1);
					values.put("localMaxTime", data2[1]);
					values.put("localFirstTime", data2[2]);
					values.put("localSecondTime", data2[3]);
					values.put("localThirdTime", data2[4]);
					values.put("localFourthTime", data2[5]);
					values.put("diaRatio", data2[7]);
					values.put("fracCover", data2[8]);
					values.put("sunAz", data2[10]);
					values.put("sunAlt", data2[11]);
					values.put("localMag", data2[14]);
					values.put("sarosNum", (int) data2[15]);
					values.put("sarosMemNum", (int) data2[16]);
					values.put("moonAz", data2[17]);
					values.put("moonAlt", data2[18]);
					values.put("globalMaxTime", data1[1]);
					values.put("globalBeginTime", data1[3]);
					values.put("globalEndTime", data1[4]);
					values.put("globalTotBegin", data1[5]);
					values.put("globalTotEnd", data1[6]);
					values.put("globalCenterBegin", data1[7]);
					values.put("globalCenterEnd", data1[8]);
					values.put("eclipseDate", eclDate);
					values.put("eclipseType", eclType);

					data2 = solarDataLocal(data2[5], g, backward);
					if (data2 == null) {
						Log.e("Solar Eclipse error",
								"computeEclipses data2a error");
						Toast.makeText(
								getApplicationContext(),
								"computeEclipses error 1,\nplease restart the activity",
								Toast.LENGTH_LONG).show();
						break;
					}
				} else {
					// Global Eclipse
					values.put("localType", -1);
					values.put("globalType", (int) data1[0]);
					values.put("local", 0);
					values.put("localMaxTime", -1);
					values.put("localFirstTime", -1);
					values.put("localSecondTime", -1);
					values.put("localThirdTime", -1);
					values.put("localFourthTime", -1);
					values.put("diaRatio", -1);
					values.put("fracCover", -1);
					values.put("sunAz", -1);
					values.put("sunAlt", -1);
					values.put("localMag", -1);
					values.put("sarosNum", -1);
					values.put("sarosMemNum", -1);
					values.put("moonAz", -1);
					values.put("moonAlt", -1);
					values.put("globalMaxTime", data1[1]);
					values.put("globalBeginTime", data1[3]);
					values.put("globalEndTime", data1[4]);
					values.put("globalTotBegin", data1[5]);
					values.put("globalTotEnd", data1[6]);
					values.put("globalCenterBegin", data1[7]);
					values.put("globalCenterEnd", data1[8]);
					values.put("eclipseDate", eclDate);
					values.put("eclipseType", eclType);
				}
				cr.update(Uri.withAppendedPath(PlanetsDbProvider.SOLAR_URI,
						String.valueOf(i)), values, null, null);

				if (backward == 0)
					start = data1[4];
				else
					start = data1[3];
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			eclipseDialog.dismiss();
			eclipseList.setVisibility(View.VISIBLE);
			fillData();
		}
	}

	private void showEclipseData(int num, boolean local) {
		Bundle bundle = new Bundle();
		bundle.putInt("eclipseNum", num);
		bundle.putDouble("Offset", offset);
		bundle.putBoolean("db", true);
		bundle.putBoolean("local", local);
		Intent i = new Intent(this, EclipseData.class);
		i.putExtras(bundle);
		startActivityForResult(i, ECLIPSE_DATA);
	}

	public class EclipseSelectedListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
			// Log.i("SolarEclipse list", "selected id:" + id + " pos:" + pos);
			CheckBox cb = (CheckBox) view.findViewById(R.id.eclLocal);
			showEclipseData((int) id, cb.isChecked());
		}
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case ECLIPSE_DATA:
			fillData();
			return;
		}
	}

	// *** Loader Manager methods ***
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cursorLoader = new CursorLoader(this,
				PlanetsDbProvider.SOLAR_URI, projection, null, null, null);
		return cursorLoader;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (cursorAdapter != null)
			cursorAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		if (cursorAdapter != null)
			cursorAdapter.swapCursor(data);
	}
}

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
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class SolarEclipse extends Activity {

	private Button prevEclButton, nextEclButton;
	private ListView eclipseList;
	private double[] time, g = new double[3];
	private double offset, firstEcl, lastEcl, local, direction;
	private PlanetsDbAdapter planetDbHelper;
	private Calendar c;
	private final int ECLIPSE_DATA = 0;

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
		planetDbHelper = new PlanetsDbAdapter(this, "solarEcl");

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

		new ComputeEclipsesTask().execute(time[1], 0.0, 0.0, -1.0, -1.0);

		prevEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				direction = 1.0;
				new ComputeEclipsesTask().execute(firstEcl, 1.0, local, -1.0,
						-1.0);
			}
		});

		nextEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				direction = 0.0;
				new ComputeEclipsesTask().execute(lastEcl, 0.0, local, -1.0,
						-1.0);
			}
		});

		eclipseList.setOnItemClickListener(new EclipseSelectedListener());
	}

	private void fillData() {
		Cursor eclCursor;
		planetDbHelper.open();
		eclCursor = planetDbHelper.fetchAllSolar();
		startManagingCursor(eclCursor);
		String[] from = new String[] { "eclipseDate", "eclipseType", "local" };
		int[] to = new int[] { R.id.eclDate, R.id.eclType, R.id.eclLocal };
		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter loc = new SimpleCursorAdapter(this,
				R.layout.ecl_row, eclCursor, from, to);
		// Binds the 'local' field in the db to the checked attribute for the
		// CheckBox
		loc.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
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
		eclipseList.setAdapter(loc);
		planetDbHelper.close();
	}

	private void showEclipseData(int num) {
		Bundle bundle = new Bundle();
		bundle.putInt("eclipseNum", num);
		bundle.putDouble("Offset", offset);
		Intent i = new Intent(this, EclipseData.class);
		i.putExtras(bundle);
		startActivityForResult(i, ECLIPSE_DATA);
	}

	/**
	 * Computes the eclipses in a separate thread.
	 * 
	 * @author tgaddis
	 * @params double list for doInBackground: (start date,
	 *         direction(forward=0.0/back=1.0), local(1.0)/global(0.0), first
	 *         eclipse date, last eclipse date)
	 * 
	 */
	private class ComputeEclipsesTask extends AsyncTask<Double, Void, Void> {
		ProgressDialog dialog;
		String eclDate, eclType;
		double startDate = 0, endDate = 0;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			planetDbHelper.open();
			dialog = ProgressDialog.show(SolarEclipse.this, "",
					"Calculating eclipses,\nplease wait...", true);
		}

		@Override
		protected Void doInBackground(Double... params) {
			// compute next or previous 8 eclipses and save them to the DB
			// startTime is in UTC
			double start;
			double[] data1 = null, data2 = null;
			int i, backward;

			if (params[3] > 0)
				startDate = params[3];
			if (params[4] > 0)
				endDate = params[4];
			backward = (int) Math.round(params[1]);
			start = params[0];

			data2 = solarDataLocal(start, g, backward);
			if (data2 == null) {
				Log.e("Solar Eclipse error", "computeEclipses data1 error");
				Toast.makeText(
						getApplicationContext(),
						"computeEclipses error 1,\nplease restart the activity",
						Toast.LENGTH_LONG).show();
				finish();
			}

			for (i = 0; i < 8; i++) {
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
				// convert c to local time
				// c.add(Calendar.MINUTE, (int) (offset * 60));
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

				if (Math.abs(data2[1] - data1[1]) < 1.0) {
					// if local eclipse time is within one day of the global
					// time, then eclipse is visible locally
					planetDbHelper.updateSolar(i, (int) data2[0],
							(int) data1[0], 1, data2[1], data2[2], data2[3],
							data2[4], data2[5], data2[7], data2[8], data2[10],
							data2[11], data2[14], (int) data2[15],
							(int) data2[16], data2[17], data2[18], data1[1],
							data1[3], data1[4], data1[5], data1[6], data1[7],
							data1[8], eclDate, eclType);

					data2 = solarDataLocal(data2[5], g, backward);
					if (data2 == null) {
						Log.e("Solar Eclipse error",
								"computeEclipses data2 error");
						Toast.makeText(
								getApplicationContext(),
								"computeEclipses error 1,\nplease restart the activity",
								Toast.LENGTH_LONG).show();
						break;
					}
				} else {
					planetDbHelper.updateSolar(i, -1, (int) data1[0], 0, -1,
							-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
							data1[1], data1[3], data1[4], data1[5], data1[6],
							data1[7], data1[8], eclDate, eclType);
				}
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
			if (startDate > 0)
				firstEcl = startDate;
			if (endDate > 0)
				lastEcl = endDate;
			planetDbHelper.close();
			dialog.dismiss();
			fillData();
		}

	}

	public class EclipseSelectedListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
			showEclipseData((int) id);
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
}

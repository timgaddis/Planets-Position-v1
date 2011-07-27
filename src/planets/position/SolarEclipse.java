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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Toast;

public class SolarEclipse extends Activity {

	private Button prevEclButton, nextEclButton;
	private CheckBox localEclCheck;
	private ListView list;
	private Bundle bundle;
	double[] time, g = new double[3];
	private double offset, firstEcl, lastEcl;
	// private long startEcl, endEcl;
	private PlanetsDbAdapter planetDbHelper;
	private Calendar c;
	private boolean local = false;
	private int direction = 0;

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
		localEclCheck = (CheckBox) findViewById(R.id.localSEclCheck);
		list = (ListView) findViewById(R.id.solarEclList);

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
		}

		planetDbHelper = new PlanetsDbAdapter(this, "solarEcl");
		// planetDbHelper.open();

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

		computeEclipses(time[1], 0, false);

		Log.i("Solar Eclipse", "firstEcl: " + firstEcl);
		Log.i("Solar Eclipse", "lastEcl: " + lastEcl);

		Toast.makeText(getApplicationContext(), "Ready", Toast.LENGTH_SHORT)
				.show();

		prevEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				direction = 1;
				computeEclipses(firstEcl, 1, local);
				Log.i("Solar Eclipse", "prev firstEcl: " + firstEcl);
				Log.i("Solar Eclipse", "prev lastEcl: " + lastEcl);
				Toast.makeText(getApplicationContext(), "Previous 10 eclipses",
						Toast.LENGTH_SHORT).show();
			}
		});

		nextEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				direction = 0;
				computeEclipses(lastEcl, 0, local);
				Log.i("Solar Eclipse", "next firstEcl: " + firstEcl);
				Log.i("Solar Eclipse", "next lastEcl: " + lastEcl);
				Toast.makeText(getApplicationContext(), "Next 10 eclipses",
						Toast.LENGTH_SHORT).show();
			}
		});

		localEclCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				double s = firstEcl, e = lastEcl, sd;
				if (direction == 0)
					sd = firstEcl;
				else
					sd = lastEcl;
				if (arg1) {
					local = true;
					computeEclipses(sd, direction, local);
					Toast.makeText(getApplicationContext(), "Selected",
							Toast.LENGTH_SHORT).show();
					Log.i("Solar Eclipse", "next loc firstEcl: " + firstEcl);
					Log.i("Solar Eclipse", "next loc lastEcl: " + lastEcl);
				} else {
					local = false;
					computeEclipses(sd, direction, local);
					Toast.makeText(getApplicationContext(), "Not selected",
							Toast.LENGTH_SHORT).show();
					Log.i("Solar Eclipse", "next glo firstEcl: " + firstEcl);
					Log.i("Solar Eclipse", "next glo lastEcl: " + lastEcl);
				}
				firstEcl = s;
				lastEcl = e;
			}

		});

	}

	// @Override
	// protected void onDestroy() {
	// super.onDestroy();
	// planetDbHelper.close();
	// }

	private class GetGPSTask extends AsyncTask<Void, Void, Void> {
		ProgressDialog dialog;

		@Override
		protected Void doInBackground(Void... params) {

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			dialog.dismiss();
		}

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(getApplicationContext(), "",
					"Calculating Eclipses.\nPlease wait...", true);
		}

	}

	private void computeEclipses(double startTime, int backward, boolean local) {
		// compute next or previous 10 eclipses and save them to the DB
		// startTime is in UTC
		double start;
		double[] data1 = null, data2 = null;
		int i;
		start = startTime;
		planetDbHelper.open();
		Log.i("Solar Eclipse", "var: " + start + ":" + backward + ":" + local);
		for (i = 0; i < 10; i++) {
			if (local) {
				data1 = solarDataLocal(start, g, backward);
				if (data1 == null) {
					Log.e("Solar Eclipse error", "computeEclipses data1 error");
					Toast.makeText(
							getApplicationContext(),
							"computeEclipses error 1,\nplease restart the activity",
							Toast.LENGTH_LONG).show();
					break;
				}
				data2 = solarDataGlobal(data1[2] - 1, backward);
				if (data2 == null) {
					Log.e("Solar Eclipse error", "computeEclipses data2 error");
					Toast.makeText(
							getApplicationContext(),
							"computeEclipses error 2,\nplease restart the activity",
							Toast.LENGTH_LONG).show();
					break;
				}
				// save the beginning time of the eclipse
				if (i == 0)
					if (backward == 0)
						firstEcl = data2[3];
					else
						lastEcl = data2[4];
				// save the ending time of the eclipse
				if (i == 9)
					if (backward == 0)
						lastEcl = data2[4];
					else
						firstEcl = data2[3];

				planetDbHelper.updateSolar(i, (int) data1[0], (int) data2[0],
						1, data1[1], data1[2], data1[3], data1[4], data1[5],
						data1[7], data1[8], data1[10], data1[11], data1[14],
						(int) data1[15], (int) data1[16], data1[17], data1[18],
						data2[1], data2[3], data2[4], data2[5], data2[6],
						data2[7], data2[8]);

				if (backward == 0)
					start = data2[4];
				else
					start = data2[3];

			} else {
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
				if (i == 9)
					if (backward == 0)
						lastEcl = data1[4];
					else
						firstEcl = data1[3];

				planetDbHelper.updateSolar(i, -1, (int) data1[0], 0, -1, -1,
						-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
						data1[1], data1[3], data1[4], data1[5], data1[6],
						data1[7], data1[8]);

				if (backward == 0)
					start = data1[4];
				else
					start = data1[3];
			}
		}
		planetDbHelper.close();
	}
}

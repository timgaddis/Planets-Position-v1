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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
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

public class LunarEclipse extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private Button prevEclButton, nextEclButton;
	private ListView eclipseList;
	private double[] time, g = new double[3];
	private double offset, firstEcl, lastEcl;
	private Calendar c;
	private final int ECLIPSE_DATA = 0;
	private static final int LUNAR_LOADER = 1;
	private String[] projection = { PlanetsDbAdapter.KEY_ROWID, "eclipseDate",
			"eclipseType", "local" };
	private SimpleCursorAdapter cursorAdapter;
	private ContentResolver cr;
	private DialogFragment eclipseDialog;
	private ComputeEclipsesTask computeEclipses;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native double[] lunarDataLocal(double d2, double[] loc);

	public native double[] lunarDataGlobal(double d2, double[] loc, int back);

	public native double[] utc2jd(int m, int d, int y, int hr, int min,
			double sec);

	public native String jd2utc(double jdate);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lunar_main);

		prevEclButton = (Button) findViewById(R.id.prevLEclButton);
		nextEclButton = (Button) findViewById(R.id.nextLEclButton);
		eclipseList = (ListView) findViewById(R.id.lunarEclList);

		// load bundle from previous activity
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
		}

		cr = getApplicationContext().getContentResolver();
		getSupportLoaderManager().initLoader(LUNAR_LOADER, null, this);

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

		computeEclipses = new ComputeEclipsesTask();
		computeEclipses.execute(time[1], 0.0);

		prevEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				computeEclipses = new ComputeEclipsesTask();
				computeEclipses.execute(firstEcl, 1.0);
			}
		});

		nextEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				computeEclipses = new ComputeEclipsesTask();
				computeEclipses.execute(lastEcl, 0.0);
			}
		});

		eclipseList.setOnItemClickListener(new EclipseSelectedListener());

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (computeEclipses != null)
			computeEclipses.cancel(true);
	}

	private void fillData() {
		Cursor eclCursor = cr.query(PlanetsDbProvider.LUNAR_URI, projection,
				null, null, "penBegin");
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
			eclipseDialog.show(getSupportFragmentManager(),
					"eclipseDialogLunar");
		}

		@Override
		protected Void doInBackground(Double... params) {
			// compute next or previous 8 eclipses and save them to the DB
			// startTime is in UTC
			double start, mag = 0;
			double[] data1 = null, data2 = null;
			int i, backward, val;

			backward = (int) Math.round(params[1]);
			start = params[0];

			for (i = 0; i < 8; i++) {
				if (this.isCancelled())
					break;
				values.clear();
				data1 = lunarDataGlobal(start, g, backward);
				if (data1 == null) {
					Log.e("Lunar Eclipse error",
							"ComputeEclipsesTask data1g error");
					Toast.makeText(getApplicationContext(),
							"Lunar eclipse error\nplease restart the activity",
							Toast.LENGTH_LONG).show();
					break;
				}
				// save the beginning time of the eclipse
				if (i == 0)
					if (backward == 0)
						firstEcl = data1[7];
					else
						lastEcl = data1[8];
				// save the ending time of the eclipse
				if (i == 7)
					if (backward == 0)
						lastEcl = data1[8];
					else
						firstEcl = data1[7];

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
				val = (int) data1[0];
				if ((val & 4) == 4) // SE_ECL_TOTAL
					eclType = "Total";
				else if ((val & 64) == 64) // SE_ECL_PENUMBRAL
					eclType = "Penumbral";
				else if ((val & 16) == 16) // SE_ECL_PARTIAL
					eclType = "Partial";
				else
					eclType = "Other";

				if (data1[9] > 0 || data1[10] > 0) {
					// moon is above horizon at the beginning or end of eclipse
					data2 = lunarDataLocal(data1[1], g);
					if (data2 == null) {
						Log.e("Lunar Eclipse error",
								"ComputeEclipsesTask data1l error");
						Toast.makeText(
								getApplicationContext(),
								"Lunar eclipse error\nplease restart the activity",
								Toast.LENGTH_LONG).show();
						break;
					}
					val = (int) data1[0];
					if ((val & 4) == 4) // SE_ECL_TOTAL
						mag = data2[0];
					else if ((val & 64) == 64) // SE_ECL_PENUMBRAL
						mag = data2[1];
					else if ((val & 16) == 16) // SE_ECL_PARTIAL
						mag = data2[0];
					else
						mag = 0;

					values.put("type", (int) data1[0]);
					values.put("local", 1);
					values.put("maxEclTime", data1[1]);
					values.put("partBegin", data1[3]);
					values.put("partEnd", data1[4]);
					values.put("totBegin", data1[5]);
					values.put("totEnd", data1[6]);
					values.put("penBegin", data1[7]);
					values.put("penEnd", data1[8]);
					values.put("eclipseMag", mag);
					values.put("sarosNum", (int) data2[2]);
					values.put("sarosMemNum", (int) data2[3]);
					values.put("eclipseDate", eclDate);
					values.put("eclipseType", eclType);
					values.put("moonAz", data2[4]);
					values.put("moonAlt", data2[5]);
					values.put("rTime", 0.0);
					values.put("sTime", 0.0);
				} else {
					values.put("type", (int) data1[0]);
					values.put("local", 0);
					values.put("maxEclTime", data1[1]);
					values.put("partBegin", data1[3]);
					values.put("partEnd", data1[4]);
					values.put("totBegin", data1[5]);
					values.put("totEnd", data1[6]);
					values.put("penBegin", data1[7]);
					values.put("penEnd", data1[8]);
					values.put("eclipseMag", -1);
					values.put("sarosNum", -1);
					values.put("sarosMemNum", -1);
					values.put("eclipseDate", eclDate);
					values.put("eclipseType", eclType);
					values.put("moonAz", -1);
					values.put("moonAlt", -1);
					values.put("rTime", -1);
					values.put("sTime", -1);
				}
				cr.update(Uri.withAppendedPath(PlanetsDbProvider.LUNAR_URI,
						String.valueOf(i)), values, null, null);

				if (backward == 0)
					start = data1[8];
				else
					start = data1[7];
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			// eclipseDialog.dismiss();
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			Fragment prev = getSupportFragmentManager().findFragmentByTag(
					"eclipseDialogLunar");
			if (prev != null) {
				ft.remove(prev);
			}
			ft.commit();
			eclipseList.setVisibility(View.VISIBLE);
			fillData();
		}
	}

	private void showEclipseData(int num, boolean local) {
		Bundle bundle = new Bundle();
		bundle.putInt("eclipseNum", num);
		bundle.putDouble("Offset", offset);
		bundle.putBoolean("db", false);
		bundle.putBoolean("local", local);
		Intent i = new Intent(this, EclipseData.class);
		i.putExtras(bundle);
		startActivityForResult(i, ECLIPSE_DATA);
	}

	public class EclipseSelectedListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
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
				PlanetsDbProvider.LUNAR_URI, projection, null, null, null);
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

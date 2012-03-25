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
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class ViewWhatsUp extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private ListView planetsList;
	private Button refreshButton;
	private TextView viewDate;
	private double offset;
	private double[] g = new double[3];
	private int filter = 1;
	private String[] planetNames = { "Sun", "Moon", "Mercury", "Venus", "Mars",
			"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto" };
	private Bundle bundle;
	private final int PLANET_DATA = 0;
	private static final int UP_LOADER = 1;
	private static final String TAG = ViewWhatsUp.class.getSimpleName();
	private String[] projection = { PlanetsDbAdapter.KEY_ROWID, "name", "az",
			"alt", "mag" };
	private SimpleCursorAdapter cursorAdapter;
	private ContentResolver cr;
	private ComputePlanetsTask computePlanetsTask;
	private DialogFragment calcDialog;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native double[] planetUpData(double d1, double d2, int p,
			double[] loc, double press, double temp);

	public native double[] utc2jd(int m, int d, int y, int hr, int min,
			double sec);

	public native String jd2utc(double jdate);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.planet_list);

		planetsList = (ListView) findViewById(R.id.listView1);
		refreshButton = (Button) findViewById(R.id.refreshButton);
		viewDate = (TextView) findViewById(R.id.text01);

		planetsList.setVisibility(View.INVISIBLE);

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
		}

		cr = getApplicationContext().getContentResolver();
		getSupportLoaderManager().initLoader(UP_LOADER, null, this);

		Object retained = getLastCustomNonConfigurationInstance();
		if (retained instanceof ComputePlanetsTask) {
			Log.i(TAG, "Reclaiming previous background task.");
			computePlanetsTask = (ComputePlanetsTask) retained;
			computePlanetsTask.setActivity(this);
		} else {
			Log.i(TAG, "Creating new background task.");
			computePlanetsTask = new ComputePlanetsTask(this, g, offset);
			computePlanetsTask.execute();
		}

		refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// re-calculates the planet's positions
				planetsList.setVisibility(View.INVISIBLE);
				computePlanetsTask = new ComputePlanetsTask(ViewWhatsUp.this,
						g, offset);
				computePlanetsTask.execute();
			}

		});

		planetsList.setOnItemClickListener(new PlanetSelectedListener());
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		computePlanetsTask.setActivity(null);
		return computePlanetsTask;
	}

	private void fillData(int list) {
		Cursor plCursor;
		if (list == 1) {
			plCursor = cr.query(PlanetsDbProvider.PLANETS_URI, projection,
					"alt > 0.0", null, null);
		} else {
			plCursor = cr.query(PlanetsDbProvider.PLANETS_URI, projection,
					"alt > 0.0 AND mag <= 6", null, null);
		}
		String[] from = new String[] { "name", "az", "alt", "mag" };
		int[] to = new int[] { R.id.list2, R.id.list3, R.id.list4, R.id.list5 };
		cursorAdapter = new SimpleCursorAdapter(getApplicationContext(),
				R.layout.planet_row, plCursor, from, to, 0);
		planetsList.setAdapter(cursorAdapter);
	}

	private class ComputePlanetsTask extends AsyncTask<Void, Void, Void> {
		double[] data = null, time;
		private double offset;
		private double[] g = new double[3];
		private long startTime;
		private Calendar c;
		private ContentValues values;
		private ViewWhatsUp activity;
		private boolean completed = false;

		public ComputePlanetsTask(ViewWhatsUp activity, double[] g, double off) {
			this.activity = activity;
			this.g = g;
			this.offset = off;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			values = new ContentValues();
			c = Calendar.getInstance();
			startTime = c.getTimeInMillis();
			// convert local time to utc
			c.add(Calendar.MINUTE, (int) (offset * -60));

			time = utc2jd(c.get(Calendar.MONTH) + 1,
					c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.YEAR),
					c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
					c.get(Calendar.SECOND));
			if (time == null) {
				Log.e("Position error", "utc2jd error");
				activity.finish();
			}
			// jdTT = time[0];
			// jdUT = time[1];
			calcDialog = CalcDialog.newInstance(R.string.up_dialog);
			calcDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
			calcDialog.show(getSupportFragmentManager(), "calcDialog");
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 0; i < 10; i++) {
				values.clear();
				data = planetUpData(time[0], time[1], i, g, 0.0, 0.0);
				if (data == null) {
					Log.e("Position error", "ViewWhatsUp - planetUpData error");
					activity.finish();
				}
				String[] dateArr = jd2utc(data[6]).split("_");

				c.set(Integer.parseInt(dateArr[1]),
						Integer.parseInt(dateArr[2]) - 1,
						Integer.parseInt(dateArr[3]),
						Integer.parseInt(dateArr[4]),
						Integer.parseInt(dateArr[5]));
				c.set(Calendar.MILLISECOND,
						(int) (Double.parseDouble(dateArr[6]) * 1000));
				// convert utc to local time
				c.add(Calendar.MINUTE, (int) (offset * 60));

				values.put("name", planetNames[i]);
				values.put("ra", data[0]);
				values.put("dec", data[1]);
				values.put("az", data[3]);
				values.put("alt", data[4]);
				values.put("dis", data[2]);
				values.put("mag", Math.round(data[5]));
				values.put("setT", c.getTimeInMillis());

				cr.update(Uri.withAppendedPath(PlanetsDbProvider.PLANETS_URI,
						String.valueOf(i)), values, null, null);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			completed = true;
			notifyActivityTaskCompleted();
		}

		private void setActivity(ViewWhatsUp activity) {
			this.activity = activity;
			if (completed) {
				notifyActivityTaskCompleted();
			}
		}

		/**
		 * Helper method to notify the activity that this task was completed.
		 */
		private void notifyActivityTaskCompleted() {
			if (null != activity) {
				activity.onTaskCompleted(startTime);
			}
		}
	}

	private void onTaskCompleted(long time) {
		Log.i(TAG, "Activity " + this
				+ " has been notified the task is complete.");
		// Remove the dialog if it is visible.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(
				"calcDialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		viewDate.setText("What's up on "
				+ DateFormat.format("MMM d @ hh:mm aa", cal));
		planetsList.setVisibility(View.VISIBLE);
		fillData(filter);
	}

	private void showPlanetData(int num) {
		bundle = new Bundle();
		bundle.putInt("planetNum", num);
		Intent i = new Intent(this, PlanetData.class);
		i.putExtras(bundle);
		startActivityForResult(i, PLANET_DATA);
	}

	public class PlanetSelectedListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
			showPlanetData((int) id);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu2) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.whats_up_menu, menu2);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.id_menu_up_help:
			// Show Help Screen
			Bundle b = new Bundle();
			b.putInt("res", R.string.up_help);
			Intent i = new Intent(this, About.class);
			i.putExtras(b);
			startActivityForResult(i, PLANET_DATA);
			return true;
		case R.id.id_menu_up_filter:
			// Filter the planets by magnitude
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.filter_prompt);
			final ArrayAdapter<CharSequence> adapter = ArrayAdapter
					.createFromResource(this, R.array.filter_array,
							android.R.layout.select_dialog_singlechoice);
			builder.setSingleChoiceItems(adapter, filter,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							fillData(item);
							filter = item;
							dialog.dismiss();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case PLANET_DATA:
			fillData(1);
			return;
		}
	}

	// *** Loader Manager methods ***
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cursorLoader = new CursorLoader(this,
				PlanetsDbProvider.PLANETS_URI, projection, "alt > 0.0", null,
				null);
		return cursorLoader;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
	}
}

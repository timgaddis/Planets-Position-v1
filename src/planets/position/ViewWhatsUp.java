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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ViewWhatsUp extends Activity {

	private ListView planetsList;
	private double offset;
	private double[] g = new double[3];
	private int filter = 1;
	private String[] planetNames = { "Sun", "Moon", "Mercury", "Venus", "Mars",
			"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto" };
	private Bundle bundle;
	private PlanetsDbAdapter planetDbHelper;
	private final int PLANET_DATA = 0;

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

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
		}

		planetDbHelper = new PlanetsDbAdapter(this, "planets");
		// planetDbHelper.open();

		// computePlanets();
		new ComputePlanetsTask().execute();
		// fillData(1);
		planetsList.setOnItemClickListener(new PlanetSelectedListener());
	}

	private void fillData(int list) {
		Cursor plCursor;
		planetDbHelper.open();
		if (list == 1) {
			plCursor = planetDbHelper.fetchAllList();
		} else {
			plCursor = planetDbHelper.fetchEyeList();
		}
		startManagingCursor(plCursor);
		String[] from = new String[] { PlanetsDbAdapter.KEY_NAME, "az", "alt",
				"mag" };
		int[] to = new int[] { R.id.list2, R.id.list3, R.id.list4, R.id.list5 };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter loc = new SimpleCursorAdapter(this,
				R.layout.planet_row, plCursor, from, to);
		planetsList.setAdapter(loc);
		planetDbHelper.close();
	}

	private class ComputePlanetsTask extends AsyncTask<Void, Void, Void> {
		ProgressDialog dialog;
		double[] data = null, time;
		Calendar c;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			planetDbHelper.open();
			c = Calendar.getInstance();
			// set the title of the activity with the current date and time
			ViewWhatsUp.this.setTitle("What's up on "
					+ DateFormat.format("MMM d @ hh:mm aa", c));
			// convert local time to utc
			c.add(Calendar.MINUTE, (int) (offset * -60));

			time = utc2jd(c.get(Calendar.MONTH) + 1,
					c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.YEAR),
					c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
					c.get(Calendar.SECOND));
			if (time == null) {
				Log.e("Position error", "utc2jd error");
				Toast.makeText(getApplicationContext(),
						"Date conversion error,\nplease restart the activity",
						Toast.LENGTH_SHORT).show();
				ViewWhatsUp.this.finish();
			}
			// jdTT = time[0];
			// jdUT = time[1];
			dialog = ProgressDialog.show(ViewWhatsUp.this, "",
					"Calculating planets,\nplease wait...", true);
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 0; i < 10; i++) {
				data = planetUpData(time[0], time[1], i, g, 0.0, 0.0);
				if (data == null) {
					Log.e("Position error", "planetUpData error");
					Toast.makeText(
							getApplicationContext(),
							"Planet calculation error,\nplease restart the activity",
							Toast.LENGTH_SHORT).show();
					dialog.dismiss();
					ViewWhatsUp.this.finish();
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

				planetDbHelper.updatePlanet(i, planetNames[i], data[0],
						data[1], data[3], data[4], data[2],
						Math.round(data[5]), c.getTimeInMillis());
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			planetDbHelper.close();
			dialog.dismiss();
			fillData(1);
		}
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
		Bundle b;
		Intent i;
		switch (item.getItemId()) {
		case R.id.id_menu_up_help:
			// Show Help Screen
			b = new Bundle();
			b.putInt("res", R.string.up_help);
			i = new Intent(this, About.class);
			i.putExtras(b);
			startActivity(i);
			return true;
		case R.id.id_menu_up_update:
			new ComputePlanetsTask().execute();
			// computePlanets();
			// fillData(1);
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
			new ComputePlanetsTask().execute();
			// computePlanets();
			// fillData(1);
			return;
		}
	}
}

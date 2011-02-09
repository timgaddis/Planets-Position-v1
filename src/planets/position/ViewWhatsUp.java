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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ViewWhatsUp extends ListActivity {

	private double offset;
	double[] g = new double[3];
	private String[] planetNames = { "Sun", "Moon", "Mercury", "Venus", "Mars",
			"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto" };
	private Bundle bundle;
	private Calendar gc, utc;
	private PlanetsDbAdapter planetDbHelper;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native double[] planetRADec(double d1, double d2, int p,
			double[] loc, double press, double temp);

	public native double[] utc2jd(int m, int d, int y, int hr, int min,
			double sec);

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.planet_list);

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
		}

		planetDbHelper = new PlanetsDbAdapter(this, "planets");
		planetDbHelper.open();

		computePlanets();
		fillData();
		registerForContextMenu(getListView());
	}

	private void fillData() {
		Cursor plCursor = planetDbHelper.fetchAllList();
		startManagingCursor(plCursor);
		String[] from = new String[] { PlanetsDbAdapter.KEY_NAME, "az", "alt" };
		int[] to = new int[] { R.id.list2, R.id.list3, R.id.list4 };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter loc = new SimpleCursorAdapter(this,
				R.layout.planet_row, plCursor, from, to);
		setListAdapter(loc);
	}

	private void computePlanets() {
		double[] data = null, time;
		int m;

		// convert local time to utc
		Calendar c = Calendar.getInstance();
		m = (int) (offset * 60);
		c.add(Calendar.MINUTE, m * -1);

		time = utc2jd(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
				c.get(Calendar.YEAR), c.get(Calendar.HOUR_OF_DAY),
				c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
		if (time == null) {
			System.out.println("date error");
			return;
		}
		// jdTT = data[0];
		// jdUT = data[1];

		// run calculations on all 10 solar system objects
		// save data to database
		for (int i = 0; i < 10; i++) {
			data = planetRADec(time[0], time[1], i, g, 0.0, 0.0);
			if (data == null) {
				System.out.println("position error");
				return;
			}
			planetDbHelper.updatePlanet(i, planetNames[i], data[0], data[1],
					data[3], data[4], data[2]);
		}
	}

	private void showPlanetData(long num) {
		Cursor planetCur = planetDbHelper.fetchEntry(num);
		startManagingCursor(planetCur);
		String data = "";
		data += "Right Ascension: "
				+ convertRaDec(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("ra")), 0);
		data += "\nDeclination: "
				+ convertRaDec(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("dec")), 1);
		data += "\nAzimuth: "
				+ convertRaDec(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("az")), 3);
		data += "\nAltitude: "
				+ convertRaDec(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("alt")), 3);
		data += "\nDistance: "
				+ convertRaDec(planetCur.getDouble(planetCur
						.getColumnIndexOrThrow("dis")), 2);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
				.setTitle(
						planetCur.getString(planetCur
								.getColumnIndexOrThrow("name")) + " Data")
				.setMessage(data).setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = alertDialogBuilder.create();
		planetCur.deactivate();
		alert.show();
	}

	private String convertRaDec(double value, int type) {
		double ra, dec, ras;
		int rah, ram, decd, decm, decs;
		char decSign;

		if (type == 0) {
			// RA value
			ra = value;
			// convert ra to hours
			ra = ra / 15;

			rah = (int) ra;
			ra -= rah;
			ra *= 60;
			ram = (int) ra;
			ra -= ram;
			ras = ra * 60;
			return String.format("%dh %dm %.1fs", rah, ram, ras);

		} else if (type == 1) {
			// Dec value
			dec = value;
			if (dec < 0) {
				decSign = '-';
				dec *= -1;
			} else {
				decSign = '+';
			}
			decd = (int) dec;
			dec -= decd;
			dec *= 60;
			decm = (int) dec;
			dec -= decm;
			dec *= 60;
			decs = (int) dec;
			return String.format("%c%d\u00b0 %d\' %d\"", decSign, decd, decm,
					decs);
		} else if (type == 2) {
			// Distance value
			return String.format("%.4f AU", value);
		} else if (type == 3) {
			// Az or Alt value
			return String.format("%.1f\u00b0", value);
		} else {
			return "";
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		showPlanetData(id);
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
			// showHelpDialog();
			b = new Bundle();
			b.putInt("res", R.string.up_help);
			i = new Intent(this, About.class);
			i.putExtras(b);
			startActivity(i);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
}

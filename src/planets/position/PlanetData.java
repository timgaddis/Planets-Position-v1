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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.widget.TextView;

public class PlanetData extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private TextView planetName, raText, decText, azText, altText, disText,
			magText, setTimeText;
	private int planetNum;
	private Bundle bundle;

	private static final int PLANET_LOADER = 1;
	private String[] projection = { PlanetsDbAdapter.KEY_ROWID, "name", "ra",
			"dec", "az", "alt", "dis", "mag", "setT" };
	private ContentResolver cr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.planet_data);

		planetName = (TextView) findViewById(R.id.info_name_text);
		raText = (TextView) findViewById(R.id.info_ra_text);
		decText = (TextView) findViewById(R.id.info_dec_text);
		azText = (TextView) findViewById(R.id.info_az_text);
		altText = (TextView) findViewById(R.id.info_alt_text);
		disText = (TextView) findViewById(R.id.info_dis_text);
		magText = (TextView) findViewById(R.id.info_mag_text);
		setTimeText = (TextView) findViewById(R.id.info_setTime_text);

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			planetNum = bundle.getInt("planetNum", 0);
		}
		cr = getApplicationContext().getContentResolver();
		getSupportLoaderManager().initLoader(PLANET_LOADER, null, this);

		fillData();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		setResult(RESULT_OK);
		finish();
	}

	private void fillData() {
		Calendar c = Calendar.getInstance();
		Cursor plCursor = cr.query(
				Uri.withAppendedPath(PlanetsDbProvider.PLANETS_URI,
						String.valueOf(planetNum)), projection, null, null,
				null);
		plCursor.moveToFirst();
		planetName.setText(plCursor.getString(plCursor
				.getColumnIndexOrThrow("name")));
		raText.setText(convertRaDec(
				plCursor.getDouble(plCursor.getColumnIndexOrThrow("ra")), 0));
		decText.setText(convertRaDec(
				plCursor.getDouble(plCursor.getColumnIndexOrThrow("dec")), 1));
		azText.setText(convertRaDec(
				plCursor.getDouble(plCursor.getColumnIndexOrThrow("az")), 3));
		altText.setText(convertRaDec(
				plCursor.getDouble(plCursor.getColumnIndexOrThrow("alt")), 3));
		disText.setText(convertRaDec(
				plCursor.getDouble(plCursor.getColumnIndexOrThrow("dis")), 2));
		magText.setText(plCursor.getString(plCursor
				.getColumnIndexOrThrow("mag")));
		c.setTimeInMillis(plCursor.getLong(plCursor
				.getColumnIndexOrThrow("setT")));
		setTimeText.setText(DateFormat.format("MMM dd h:mm aa", c));
	}

	private String convertRaDec(double value, int type) {
		double ra, dec, ras;
		int rah, ram, decd, decm, decs;
		char decSign;
		switch (type) {
		case 0:
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
		case 1:
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
		case 2:
			// Distance value
			return String.format("%.4f AU", value);
		case 3:
			// Az or Alt value
			return String.format("%.1f\u00b0", value);
		default:
			return "";
		}

	}

	// *** Loader Manager methods ***
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cursorLoader = new CursorLoader(this,
				PlanetsDbProvider.PLANETS_URI, projection, null, null, null);
		return cursorLoader;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
	}
}

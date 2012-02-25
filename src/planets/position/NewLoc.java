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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class NewLoc extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private Button saveLocButton;
	private EditText newLongText, newLatText, newElevationText;
	private Spinner newOffsetSpin;
	private long date = 0;
	// private PlanetsDbAdapter planetDbHelper;
	private int ioffset = 13;
	private double elevation = 0, latitude = 0, longitude = 0, offset = 0;
	static final String[] timeZones = new String[] { "-12:00", "-11:00",
			"-10:00", "-9:00", "-8:00", "-7:00", "-6:00", "-5:00", "-4:00",
			"-3:30", "-3:00", "-2:00", "-1:00", "0:00", "+1:00", "+2:00",
			"+3:00", "+3:30", "+4:00", "+4:30", "+5:00", "+5:30", "+5:45",
			"+6:00", "+7:00", "+8:00", "+9:00", "+9:30", "+10:00", "+11:00",
			"+12:00" };
	private static final int LOC_LOADER = 1;
	private String[] projection = { PlanetsDbAdapter.KEY_ROWID, "lat", "lng",
			"elevation", "ioffset" };
	private ContentResolver cr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_loc);

		saveLocButton = (Button) findViewById(R.id.saveLocButton);
		newElevationText = (EditText) findViewById(R.id.newElevationText);
		newLatText = (EditText) findViewById(R.id.newLatText);
		newLongText = (EditText) findViewById(R.id.newLongText);
		newOffsetSpin = (Spinner) findViewById(R.id.newOffsetSpin);

		cr = getApplicationContext().getContentResolver();
		getSupportLoaderManager().initLoader(LOC_LOADER, null, this);

		saveLocButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// save location data
				if (saveLocation() == 0) {
					setResult(RESULT_OK);
					finish();
				}
			}
		});

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, timeZones);
		adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
		newOffsetSpin.setAdapter(adapter);
		newOffsetSpin.setSelection(ioffset);
		newOffsetSpin.setOnItemSelectedListener(new OffsetSelectedListener());
		loadData();
	}

	/**
	 * Loads location data into the fields.
	 */
	private void loadData() {
		Cursor locCur = cr.query(
				Uri.withAppendedPath(PlanetsDbProvider.LOCATION_URI,
						String.valueOf(0)), projection, null, null, null);
		locCur.moveToFirst();
		latitude = locCur.getDouble(locCur.getColumnIndexOrThrow("lat"));
		if (latitude != -1.0) {
			newLatText.setText(locCur.getString(locCur
					.getColumnIndexOrThrow("lat")));
			newLongText.setText(locCur.getString(locCur
					.getColumnIndexOrThrow("lng")));
			newElevationText.setText(locCur.getString(locCur
					.getColumnIndexOrThrow("elevation")));
			newOffsetSpin.setSelection(locCur.getInt(locCur
					.getColumnIndexOrThrow("ioffset")));
		}
	}

	private int saveLocation() {
		ContentValues values = new ContentValues();
		if (!newLatText.getText().toString().equals(""))
			latitude = Double.parseDouble(newLatText.getText().toString());
		else {
			Toast.makeText(NewLoc.this, "Enter a value for the latitude",
					Toast.LENGTH_LONG).show();
			return 1;
		}
		if (!newLongText.getText().toString().equals(""))
			longitude = Double.parseDouble(newLongText.getText().toString());
		else {
			Toast.makeText(NewLoc.this, "Enter a value for the longitude",
					Toast.LENGTH_LONG).show();
			return 1;
		}
		if (!newElevationText.getText().toString().equals(""))
			elevation = Double.parseDouble(newElevationText.getText()
					.toString());
		else {
			Toast.makeText(NewLoc.this, "Enter a value for the elevation",
					Toast.LENGTH_LONG).show();
			return 1;
		}

		date = Calendar.getInstance().getTimeInMillis();

		values.put("lat", latitude);
		values.put("lng", longitude);
		values.put("temp", 0.0);
		values.put("pressure", 0.0);
		values.put("elevation", elevation);
		values.put("date", date);
		values.put("offset", offset);
		values.put("ioffset", ioffset);

		cr.update(
				Uri.withAppendedPath(PlanetsDbProvider.LOCATION_URI,
						String.valueOf(0)), values, null, null);
		return 0;
	}

	// offset spinner selection
	public class OffsetSelectedListener implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			NewLoc.this.ioffset = pos;
			String tz[] = parent.getItemAtPosition(pos).toString().split(":");
			double h = Double.parseDouble(tz[0]);
			double m = Double.parseDouble(tz[1]);
			m /= 60.0;
			if (h >= 0)
				h += m;
			else
				h -= m;
			NewLoc.this.offset = h;
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	}

	// *** Loader Manager methods ***

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cursorLoader = new CursorLoader(this,
				PlanetsDbProvider.LOCATION_URI, projection, null, null, null);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}
}

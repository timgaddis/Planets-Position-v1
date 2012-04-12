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

import planets.position.UserLocation.LocationResult;
import planets.position.data.PlanetsDbAdapter;
import planets.position.data.PlanetsDbProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NewLoc extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private Button saveLocButton, offsetButton, gpsButton;
	private EditText newLongText, newLatText, newElevationText;
	private long date = 0;
	private Location loc;
	private UserLocation userLocation = new UserLocation();
	private double elevation = 0, latitude = 0, longitude = 0, offset = 0;
	private static final int LOC_LOADER = 1;
	private String[] projection = { PlanetsDbAdapter.KEY_ROWID, "lat", "lng",
			"elevation", "offset" };
	private ContentResolver cr;
	private DialogFragment offsetDialog, gpsDialog;
	private GetGPSTask gpsTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_loc);

		gpsButton = (Button) findViewById(R.id.gpsButton);
		saveLocButton = (Button) findViewById(R.id.saveLocButton);
		offsetButton = (Button) findViewById(R.id.offsetButton);
		newElevationText = (EditText) findViewById(R.id.newElevationText);
		newLatText = (EditText) findViewById(R.id.newLatText);
		newLongText = (EditText) findViewById(R.id.newLongText);

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

		offsetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				offsetDialog = PlanetListDialog.newInstance(R.array.gps_array,
						0, R.string.loc_gmt, 0);
				offsetDialog.show(getSupportFragmentManager(), "offsetDialog");
			}
		});

		gpsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getLocation();
			}
		});
		loadData();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (gpsTask != null)
			gpsTask.cancel(true);
	}

	/**
	 * Gets the GPS location of the device or loads test values.
	 */
	private void getLocation() {
		// get lat/long from GPS
		loc = null;
		gpsTask = new GetGPSTask();
		gpsTask.execute();
		boolean result = userLocation.getLocation(this, locationResult);
		if (!result) {
			loc = new Location(LocationManager.PASSIVE_PROVIDER);
		}
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
			newLatText.setText(String.format("%.8f",
					locCur.getDouble(locCur.getColumnIndexOrThrow("lat"))));
			newLongText.setText(String.format("%.8f",
					locCur.getDouble(locCur.getColumnIndexOrThrow("lng"))));
			newElevationText
					.setText(String.format("%.1f", locCur.getDouble(locCur
							.getColumnIndexOrThrow("elevation"))));
			offset = locCur.getDouble(locCur.getColumnIndexOrThrow("offset"));
			String off = "";
			if (offset < 0)
				off += "-";
			else if (offset > 0)
				off += "+";
			int h = (int) Math.abs(offset);
			off += h + ":";
			int m = (int) ((Math.abs(offset) - h) * 60);
			if (m > 0)
				off += m;
			else
				off += "00";
			offsetButton.setText(off);
		}
	}

	private int saveLocation() {
		ContentValues values = new ContentValues();
		if (!newLatText.getText().toString().equals("")) {
			try {
				latitude = Double.parseDouble(newLatText.getText().toString());
			} catch (NumberFormatException ex) {
				Toast.makeText(NewLoc.this, "Enter a number for the latitude",
						Toast.LENGTH_LONG).show();
				return 1;
			}
		} else {
			Toast.makeText(NewLoc.this, "Enter a value for the latitude",
					Toast.LENGTH_LONG).show();
			return 1;
		}
		if (!newLongText.getText().toString().equals("")) {
			try {
				longitude = Double
						.parseDouble(newLongText.getText().toString());
			} catch (NumberFormatException ex) {
				Toast.makeText(NewLoc.this, "Enter a number for the longitude",
						Toast.LENGTH_LONG).show();
				return 1;
			}
		} else {
			Toast.makeText(NewLoc.this, "Enter a value for the longitude",
					Toast.LENGTH_LONG).show();
			return 1;
		}
		if (!newElevationText.getText().toString().equals("")) {
			try {
				elevation = Double.parseDouble(newElevationText.getText()
						.toString());
			} catch (NumberFormatException ex) {
				Toast.makeText(NewLoc.this, "Enter a number for the elevation",
						Toast.LENGTH_LONG).show();
				return 1;
			}
		} else {
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
		values.put("ioffset", 0);

		cr.update(
				Uri.withAppendedPath(PlanetsDbProvider.LOCATION_URI,
						String.valueOf(0)), values, null, null);
		return 0;
	}

	public void loadOffset(String time) {
		offsetButton.setText(time);
		String tz[] = time.split(":");
		double h = Double.parseDouble(tz[0]);
		double m = Double.parseDouble(tz[1]);
		m /= 60.0;
		if (h >= 0)
			h += m;
		else
			h -= m;
		offset = h;
	}

	/**
	 * Loads a dialog box in a separate thread for the GPS location and
	 * processes the location when finished.
	 * 
	 * @author tgaddis
	 * 
	 */
	private class GetGPSTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			gpsDialog = CalcDialog.newInstance(R.string.location_dialog);
			gpsDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
			gpsDialog.show(getSupportFragmentManager(), "gpsDialogLoc");
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (true) {
				if (loc != null || this.isCancelled())
					break;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (loc.getTime() > 0) {
				latitude = loc.getLatitude();
				longitude = loc.getLongitude();
				elevation = loc.getAltitude();
				date = Calendar.getInstance().getTimeInMillis();
				offset = Calendar.getInstance().getTimeZone().getOffset(date) / 3600000.0;
				newLatText.setText(String.format("%.8f", latitude));
				newLongText.setText(String.format("%.8f", longitude));
				newElevationText.setText(String.format("%.1f", elevation));
				String off = "";
				if (offset < 0)
					off += "-";
				else if (offset > 0)
					off += "+";
				int h = (int) Math.abs(offset);
				off += h + ":";
				int m = (int) ((Math.abs(offset) - h) * 60);
				if (m > 0)
					off += m;
				else
					off += "00";
				offsetButton.setText(off);

			} else {
				Toast.makeText(NewLoc.this,
						"Unable to download location data.\nPlease try again",
						Toast.LENGTH_LONG).show();
			}
			// gpsDialog.dismiss();
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			Fragment prev = getSupportFragmentManager().findFragmentByTag(
					"gpsDialogLoc");
			if (prev != null) {
				ft.remove(prev);
			}
			ft.commit();
		}
	}

	public LocationResult locationResult = new LocationResult() {
		@Override
		public void gotLocation(final Location location) {
			// Log.i("Location", "Got Location");
			loc = location;
		};
	};

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

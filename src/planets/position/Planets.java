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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import planets.position.UserLocation.LocationResult;
import planets.position.data.PlanetsDbAdapter;
import planets.position.data.PlanetsDbProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Planets extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private Button positionButton, whatupButton, solarButton, lunarButton,
			locationButton, realButton, titleButton;
	private long date = 0;
	private double elevation, latitude, longitude, offset;
	private Location loc;
	private UserLocation userLocation = new UserLocation();
	private InputStream myInput;
	private OutputStream myOutput;
	private boolean DEBUG = false;
	private DialogFragment locationDialog, gpsDialog, copyDialog;
	private GetGPSTask gpsTask;
	private CopyFilesTask copyFilesTask;

	private static final int PLANET_LOADER = 1;
	private String[] projection = { PlanetsDbAdapter.KEY_ROWID, "date", "lat",
			"lng", "elevation", "offset" };
	private ContentResolver cr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_linear);

		titleButton = (Button) findViewById(R.id.titleButton);
		positionButton = (Button) findViewById(R.id.positionButton);
		whatupButton = (Button) findViewById(R.id.whatupButton);
		solarButton = (Button) findViewById(R.id.solarEclButton);
		lunarButton = (Button) findViewById(R.id.lunarEclButton);
		locationButton = (Button) findViewById(R.id.locationButton);
		realButton = (Button) findViewById(R.id.realButton);

		cr = getApplicationContext().getContentResolver();
		getSupportLoaderManager().initLoader(PLANET_LOADER, null, this);

		if (!(checkFiles("semo_18.se1") && checkFiles("sepl_18.se1"))) {
			// copy files thread
			copyFilesTask = new CopyFilesTask();
			copyFilesTask.execute();
		} else
			loadLocation();

		titleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// about description
				Bundle b = new Bundle();
				b.putInt("res", R.string.main_about);
				Intent i = new Intent(Planets.this, About.class);
				i.putExtras(b);
				startActivity(i);
			}
		});

		positionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Launch the Planet Position activity
				if (checkLocation()) {
					Bundle b = new Bundle();
					b.putDouble("Lat", latitude);
					b.putDouble("Long", longitude);
					b.putDouble("Elevation", elevation);
					b.putDouble("Offset", offset);
					Intent i = new Intent(Planets.this, Position.class);
					i.putExtras(b);
					startActivity(i);
				} else {
					loadLocation();
				}
			}
		});

		locationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				enterLocManual();
			}

		});

		whatupButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Launch the What's Up Now activity
				if (checkLocation()) {
					Bundle b = new Bundle();
					b.putDouble("Lat", latitude);
					b.putDouble("Long", longitude);
					b.putDouble("Elevation", elevation);
					b.putDouble("Offset", offset);
					Intent i = new Intent(Planets.this, ViewWhatsUp.class);
					i.putExtras(b);
					startActivity(i);
				} else
					loadLocation();
			}

		});

		realButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (checkLocation()) {
					Bundle b = new Bundle();
					b.putDouble("Lat", latitude);
					b.putDouble("Long", longitude);
					b.putDouble("Elevation", elevation);
					b.putDouble("Offset", offset);
					Intent i = new Intent(Planets.this, LivePosition.class);
					i.putExtras(b);
					startActivity(i);
				} else {
					loadLocation();
				}
			}

		});

		solarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Launch the Solar Eclipse activity
				if (checkLocation()) {
					Bundle b = new Bundle();
					b.putDouble("Lat", latitude);
					b.putDouble("Long", longitude);
					b.putDouble("Elevation", elevation);
					b.putDouble("Offset", offset);
					Intent i = new Intent(Planets.this, SolarEclipse.class);
					i.putExtras(b);
					startActivity(i);
				} else {
					loadLocation();
				}
			}

		});

		lunarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Launch the Lunar Eclipse activity
				if (checkLocation()) {
					Bundle b = new Bundle();
					b.putDouble("Lat", latitude);
					b.putDouble("Long", longitude);
					b.putDouble("Elevation", elevation);
					b.putDouble("Offset", offset);
					Intent i = new Intent(Planets.this, LunarEclipse.class);
					i.putExtras(b);
					startActivity(i);
				} else {
					loadLocation();
				}
			}

		});

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (copyFilesTask != null)
			copyFilesTask.cancel(false);
		if (gpsTask != null)
			gpsTask.cancel(true);
	}

	/**
	 * Gets the GPS location of the device or loads test values.
	 */
	public void getLocation() {
		// Remove the location alert dialog if it is visible.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(
				"locDialogMain");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.commit();
		// get lat/long from GPS
		if (DEBUG) {
			// Test data to use with the emulator
			latitude = 32.221743;
			longitude = -110.926479;
			elevation = 713.0;
			date = Calendar.getInstance().getTimeInMillis();
			offset = -7.0;
			saveLocation();
		} else {
			loc = null;
			gpsTask = new GetGPSTask();
			gpsTask.execute();
			boolean result = userLocation.getLocation(this, locationResult);
			if (!result) {
				loc = new Location(LocationManager.PASSIVE_PROVIDER);
			}
		}
	}

	/**
	 * Checks to see if there is a location saved in the DB.
	 * 
	 * @return boolean true if location saved.
	 */
	private boolean checkLocation() {
		long locDate;
		Cursor locCur = cr.query(
				Uri.withAppendedPath(PlanetsDbProvider.LOCATION_URI,
						String.valueOf(0)), projection, null, null, null);
		locCur.moveToFirst();
		locDate = locCur.getLong(locCur.getColumnIndexOrThrow("date"));
		return (locDate > 0);
	}

	/**
	 * Loads the device location from the DB, or shows the location alert dialog
	 * box.
	 */
	private void loadLocation() {
		if (checkLocation()) {
			Cursor locCur = cr.query(
					Uri.withAppendedPath(PlanetsDbProvider.LOCATION_URI,
							String.valueOf(0)), projection, null, null, null);
			locCur.moveToFirst();
			latitude = locCur.getDouble(locCur.getColumnIndexOrThrow("lat"));
			longitude = locCur.getDouble(locCur.getColumnIndexOrThrow("lng"));
			offset = locCur.getDouble(locCur.getColumnIndexOrThrow("offset"));
			elevation = locCur.getDouble(locCur
					.getColumnIndexOrThrow("elevation"));
		} else {
			// showLocationDataAlert();
			locationDialog = LocationDialog.newInstance();
			locationDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
			locationDialog.show(getSupportFragmentManager(), "locDialogMain");
		}
	}

	/**
	 * Saves the device location to the DB.
	 */
	private void saveLocation() {
		// update location
		ContentValues values = new ContentValues();
		values.put("lat", latitude);
		values.put("lng", longitude);
		values.put("temp", 0.0);
		values.put("pressure", 0.0);
		values.put("elevation", elevation);
		values.put("date", date);
		values.put("offset", offset);

		cr.update(
				Uri.withAppendedPath(PlanetsDbProvider.LOCATION_URI,
						String.valueOf(0)), values, null, null);
		loadLocation();
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
			gpsDialog.show(getSupportFragmentManager(), "gpsDialogMain");
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
				saveLocation();
			} else {
				Toast.makeText(Planets.this,
						"Unable to download location data.\nPlease try again",
						Toast.LENGTH_LONG).show();
			}
			// gpsDialog.dismiss();
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			Fragment prev = getSupportFragmentManager().findFragmentByTag(
					"gpsDialogMain");
			if (prev != null) {
				ft.remove(prev);
			}
			ft.commit();
		}
	}

	/**
	 * AsyncTask to copy files from the assets directory to the sdcard.
	 * 
	 * @author tgaddis
	 * 
	 */
	private class CopyFilesTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			copyDialog = CalcDialog.newInstance(R.string.copy_dialog);
			copyDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
			copyDialog.show(getSupportFragmentManager(), "copyDialogMain");
		}

		@Override
		protected Void doInBackground(Void... params) {
			// copy the ephermeris files from assets folder to the sd card.
			try {
				// copyFile("seas_18.se1"); // 225440
				copyFile("semo_18.se1"); // 1305686
				copyFile("sepl_18.se1"); // 484065
			} catch (IOException e) {
				// e.printStackTrace();
				Log.e("CopyFile error", e.getMessage());
				Toast.makeText(Planets.this, "Error copying assets files",
						Toast.LENGTH_LONG).show();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// copyDialog.dismiss();
			FragmentTransaction ft = getSupportFragmentManager()
					.beginTransaction();
			Fragment prev = getSupportFragmentManager().findFragmentByTag(
					"copyDialogMain");
			if (prev != null) {
				ft.remove(prev);
			}
			ft.commit();
			loadLocation();
		}

	}

	/**
	 * Checks to see if the given file exists on the sdcard.
	 * 
	 * @param name
	 *            file name to check
	 * @return true if exists, false otherwise
	 */
	private boolean checkFiles(String name) {
		File sdCard = Environment.getExternalStorageDirectory();
		File f = new File(sdCard.getAbsolutePath() + "/ephemeris/" + name);
		return f.exists();
	}

	/**
	 * copies the given files from the assets folder to the ephermeris folder on
	 * the sdcard.
	 */
	private void copyFile(String filename) throws IOException {
		// check if ephemeris dir is on sdcard, if not create dir
		// Log.d("Copy Files", filename);
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + "/ephemeris");
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		// Log.d("File Dir", dir.getCanonicalPath());
		// check if ephemeris file is on sdcard, if not copy form assets folder
		File f = new File(dir + "/" + filename);
		// Log.d("File Exists", "" + f.exists());
		if (!f.exists()) {

			myInput = this.getAssets().open(filename);
			// Log.d("InputStream Open", "" + f.exists());

			myOutput = new FileOutputStream(f);
			// Log.d("OutputStream Open", "" + f.exists());

			byte[] buffer = new byte[1024];
			int length = 0;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}
			// Close the streams
			myOutput.flush();
			myOutput.close();
			myInput.close();

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Bundle b;
		Intent i;
		switch (item.getItemId()) {
		case R.id.id_menu_about:
			// about description
			b = new Bundle();
			b.putInt("res", R.string.main_about);
			i = new Intent(this, About.class);
			i.putExtras(b);
			startActivity(i);
			return true;
		case R.id.id_menu_help:
			b = new Bundle();
			b.putInt("res", R.string.main_help);
			i = new Intent(this, About.class);
			i.putExtras(b);
			startActivity(i);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	/**
	 * Launches the manual entry location activity
	 */
	public void enterLocManual() {
		// Remove the location alert dialog if it is visible.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(
				"locDialogMain");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.commit();
		// Launch the location activity
		Intent i = new Intent(this, NewLoc.class);
		startActivity(i);
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
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}
}

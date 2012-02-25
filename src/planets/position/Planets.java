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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Planets extends FragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private Button positionButton, whatupButton, downloadButton, manualButton,
			liveButton, solarButton, lunarButton;
	private Spinner planetNameSpinner;
	private TextView locationText;
	private long date = 0, locDate = 0;
	private double elevation, latitude, longitude, offset;
	private Bundle bundle;
	private int planetNum = 0;
	private String planetName;
	private Location loc;
	private UserLocation userLocation = new UserLocation();
	private InputStream myInput;
	private OutputStream myOutput;
	private boolean DEBUG = false;

	private static final int LOCATION_MANUAL = 0;
	private static final int PLANET_LOADER = 1;
	private String[] projection = { PlanetsDbAdapter.KEY_ROWID, "date", "lat",
			"lng", "elevation", "offset" };
	private ContentResolver cr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		planetNameSpinner = (Spinner) findViewById(R.id.planetSpin);
		liveButton = (Button) findViewById(R.id.goButton);
		positionButton = (Button) findViewById(R.id.positionButton);
		manualButton = (Button) findViewById(R.id.manualEnterButton);
		whatupButton = (Button) findViewById(R.id.whatupButton);
		downloadButton = (Button) findViewById(R.id.downloadButton);
		solarButton = (Button) findViewById(R.id.solarEclButton);
		lunarButton = (Button) findViewById(R.id.lunarEclButton);
		locationText = (TextView) findViewById(R.id.locationData);

		cr = getApplicationContext().getContentResolver();
		getSupportLoaderManager().initLoader(PLANET_LOADER, null, this);

		if (!(checkFiles("semo_18.se1") && checkFiles("sepl_18.se1"))) {
			// copy files thread
			new CopyFilesTask().execute();
		} else
			loadLocation();

		downloadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getLocation();
			}

		});

		positionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// save data
				launchPosition();
			}

		});

		manualButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				enterLocManual();
			}

		});

		whatupButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				launchWhatsUp();
			}

		});

		liveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				launchLivePos();
			}

		});

		solarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				launchSolar();
			}

		});

		lunarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				launchLunar();
			}

		});

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.planets_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		planetNameSpinner.setAdapter(adapter);

		planetNameSpinner
				.setOnItemSelectedListener(new PlanetNameSelectedListener());

	}

	/**
	 * Gets the GPS location of the device or loads test values.
	 */
	private void getLocation() {
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
			new GetGPSTask().execute();
			boolean result = userLocation.getLocation(this, locationResult);
			if (!result) {
				loc = new Location(LocationManager.PASSIVE_PROVIDER);
			}
		}
	}

	/**
	 * Launch the Solar Eclipse activity.
	 */
	private void launchSolar() {
		bundle = new Bundle();
		bundle.putDouble("Lat", latitude);
		bundle.putDouble("Long", longitude);
		bundle.putDouble("Elevation", elevation);
		bundle.putDouble("Offset", offset);
		Intent i = new Intent(this, SolarEclipse.class);
		i.putExtras(bundle);
		startActivity(i);
	}

	/**
	 * Launch the Lunar Eclipse activity.
	 */
	private void launchLunar() {
		bundle = new Bundle();
		bundle.putDouble("Lat", latitude);
		bundle.putDouble("Long", longitude);
		bundle.putDouble("Elevation", elevation);
		bundle.putDouble("Offset", offset);
		Intent i = new Intent(this, LunarEclipse.class);
		i.putExtras(bundle);
		startActivity(i);
	}

	/**
	 * Launch the Planet Position activity.
	 */
	private void launchPosition() {
		bundle = new Bundle();
		bundle.putDouble("Lat", latitude);
		bundle.putDouble("Long", longitude);
		bundle.putDouble("Elevation", elevation);
		bundle.putDouble("Offset", offset);
		Intent i = new Intent(this, Position.class);
		i.putExtras(bundle);
		startActivity(i);
	}

	/**
	 * Launch the What's Up Now activity.
	 */
	private void launchWhatsUp() {
		bundle = new Bundle();
		bundle.putDouble("Lat", latitude);
		bundle.putDouble("Long", longitude);
		bundle.putDouble("Elevation", elevation);
		bundle.putDouble("Offset", offset);
		Intent i = new Intent(this, ViewWhatsUp.class);
		i.putExtras(bundle);
		startActivity(i);
	}

	/**
	 * Launch the Live Position activity.
	 */
	private void launchLivePos() {
		bundle = new Bundle();
		bundle.putDouble("Lat", latitude);
		bundle.putDouble("Long", longitude);
		bundle.putDouble("Elevation", elevation);
		bundle.putDouble("Offset", offset);
		bundle.putInt("planet", planetNum);
		bundle.putString("name", planetName);
		Intent i = new Intent(this, LivePosition.class);
		i.putExtras(bundle);
		startActivity(i);
	}

	/**
	 * Loads the device location from the DB, or shows the location alert dialog
	 * box.
	 */
	private void loadLocation() {
		Cursor locCur = cr.query(
				Uri.withAppendedPath(PlanetsDbProvider.LOCATION_URI,
						String.valueOf(0)), projection, null, null, null);
		locCur.moveToFirst();
		locDate = locCur.getLong(locCur.getColumnIndexOrThrow("date"));
		if (locDate > 0) {
			latitude = locCur.getDouble(locCur.getColumnIndexOrThrow("lat"));
			longitude = locCur.getDouble(locCur.getColumnIndexOrThrow("lng"));
			offset = locCur.getDouble(locCur.getColumnIndexOrThrow("offset"));
			elevation = locCur.getDouble(locCur
					.getColumnIndexOrThrow("elevation"));
			String data = "";
			data += "Latitude: " + latitude;
			data += "\nLongitude: " + longitude;
			data += "\nElevation: " + elevation;
			data += "\nGMT offset: " + offset;
			locationText.setText(data);
		} else {
			showLocationDataAlert();
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
	 * Dialog box prompting the user to download the location or manually enter
	 * it.
	 */
	private void showLocationDataAlert() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
				.setMessage(
						"There is no location data saved.  "
								+ "You can either download the data or "
								+ "manually enter the data.")
				.setCancelable(false)
				.setPositiveButton("Download",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								getLocation();
							}
						});
		alertDialogBuilder.setNeutralButton("Manual",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						enterLocManual();
					}
				});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}

	/**
	 * Loads a dialog box in a separate thread for the GPS location and
	 * processes the location when finished.
	 * 
	 * @author tgaddis
	 * 
	 */
	private class GetGPSTask extends AsyncTask<Void, Void, Void> {
		ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(Planets.this, "",
					"Downloading Location.\nPlease wait...", true);
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (true) {
				if (loc != null)
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
			dialog.dismiss();
		}
	}

	/**
	 * AsyncTask to copy files from the assets directory to the sdcard.
	 * 
	 * @author tgaddis
	 * 
	 */
	private class CopyFilesTask extends AsyncTask<Void, Void, Void> {
		ProgressDialog copyDialog;

		@Override
		protected void onPreExecute() {
			copyDialog = ProgressDialog.show(Planets.this, "",
					"Copying files. Please wait...", true);
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
			copyDialog.dismiss();
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

	/**
	 * Planet name selection for the live position activity
	 * 
	 * @author tgaddis
	 * 
	 */
	public class PlanetNameSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			planetNum = pos;
			planetName = (String) planetNameSpinner.getItemAtPosition(pos);
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
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
			// showHelpDialog();
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
	private void enterLocManual() {
		Intent i = new Intent(this, NewLoc.class);
		startActivityForResult(i, LOCATION_MANUAL);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case LOCATION_MANUAL:
			loadLocation();
			return;
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
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}
}

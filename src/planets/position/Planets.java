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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import planets.position.UserLocation.LocationResult;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

public class Planets extends Activity {

	private Button positionButton, whatupButton, downloadButton, manualButton,
			liveButton, solarButton;// , lunarButton;
	private Spinner planetNameSpinner;
	private TextView locationText;
	private long date = 0, locDate = 0;
	private PlanetsDbAdapter planetDbHelper;
	private double elevation, latitude, longitude, offset;
	private Bundle bundle;
	private int planetNum = 0;
	private String planetName;
	private Location loc;
	private UserLocation userLocation = new UserLocation();
	private InputStream myInput;
	private OutputStream myOutput;

	private static final int LOCATION_MANUAL = 0;
	private static final int WIFI_STATUS = 1;
	private static final int WIFI_ALERT = 2;

	private boolean DEBUG = false;

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
		// lunarButton = (Button) findViewById(R.id.lunarEclButton);
		locationText = (TextView) findViewById(R.id.locationData);

		planetDbHelper = new PlanetsDbAdapter(this, "location");
		planetDbHelper.open();

		if (!(checkFiles("semo_18.se1") && checkFiles("sepl_18.se1"))) {
			// copyDialog = ProgressDialog.show(Planets.this, "",
			// "Copying files. Please wait...", true);
			// copy files thread
			new CopyFilesTask().execute();
		} else
			loadLocation();

		// checkWiFi();

		manualButton.setEnabled(true);
		downloadButton.setEnabled(true);

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

		// lunarButton.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View view) {
		// launchLunar();
		// }
		//
		// });

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.planets_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		planetNameSpinner.setAdapter(adapter);
		// planetNameSpinner.setSelection(planetNum);

		planetNameSpinner
				.setOnItemSelectedListener(new PlanetNameSelectedListener());

	}

	private void checkWiFi() {
		if (!(checkFiles("semo_18.se1") && checkFiles("sepl_18.se1"))) {
			// check wifi for connection
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (!mWifi.isConnected()) {
				startActivityForResult(new Intent(this, WiFiAlert.class),
						WIFI_ALERT);
			} else {
				DownloadFile DownloadFile = new DownloadFile();
				DownloadFile
						.execute("http://www.astro.com/ftp/swisseph/ephe/archive/sweph_18.zip");
			}
		} else
			loadLocation();
	}

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
			boolean result;
			loc = null;
			new GetGPSTask().execute();
			result = userLocation.getLocation(this, locationResult);
			if (!result) {
				loc = new Location(LocationManager.PASSIVE_PROVIDER);
			}
		}
	}

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

	private void loadLocation() {
		Cursor locCur = planetDbHelper.fetchEntry(0);
		startManagingCursor(locCur);
		locDate = Long.parseLong(locCur.getString(locCur
				.getColumnIndexOrThrow("date")));
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

	private void saveLocation() {
		// update location
		planetDbHelper.updateLocation(0, latitude, longitude, 0.0, 0.0, date,
				offset, 13, elevation);
		loadLocation();
	}

	private class DownloadFile extends AsyncTask<String, Integer, String> {

		private ProgressDialog progressDialog;
		private File sdCard, dir;
		private BufferedInputStream input;
		private BufferedOutputStream output;

		@Override
		protected void onPreExecute() {
			sdCard = Environment.getExternalStorageDirectory();
			dir = new File(sdCard.getAbsolutePath() + "/ephemeris");
			if (!dir.isDirectory()) {
				dir.mkdirs();
			}
			progressDialog = new ProgressDialog(Planets.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Downloading File...");
			progressDialog.setCancelable(false);
			progressDialog.show();
		}

		@Override
		protected String doInBackground(String... url) {
			int count;

			try {
				URL url1 = new URL(url[0]);
				URLConnection conexion = url1.openConnection();
				conexion.connect();

				File f = new File(dir + "/" + url[0].split("/")[7]);

				int lenghtOfFile = conexion.getContentLength();
				progressDialog.setMax(lenghtOfFile);

				// downlod the file
				input = new BufferedInputStream(conexion.getInputStream());
				output = new BufferedOutputStream(new FileOutputStream(f));

				byte data[] = new byte[1024];
				int total = 0;
				while ((count = input.read(data)) != -1) {
					total += count;
					publishProgress(total);
					output.write(data, 0, count);
				}
				output.flush();
				output.close();
				input.close();
			} catch (Exception e) {
				// Log.e("DownloadTest error", "" + e);
			}
			return null;
		}

		@Override
		public void onProgressUpdate(Integer... args) {
			progressDialog.setProgress(args[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			progressDialog.dismiss();
			UnZipFile unzip = new UnZipFile();
			unzip.execute();
		}

	}

	private class UnZipFile extends AsyncTask<Void, Void, Void> {

		private ProgressDialog progressDialog;
		private File sdCard, dir, file;

		@Override
		protected void onPreExecute() {
			sdCard = Environment.getExternalStorageDirectory();
			dir = new File(sdCard.getAbsolutePath() + "/ephemeris");
			file = new File(dir + "/sweph_18.zip");

			progressDialog = new ProgressDialog(Planets.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage("Extracting Files...");
			progressDialog.setCancelable(false);
			progressDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			Unzip uzip = new Unzip(file.getAbsolutePath(),
					dir.getAbsolutePath() + "/");
			uzip.unzip();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
			file.delete();
			loadLocation();
		}
	}

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

	private class GetGPSTask extends AsyncTask<Void, Void, Void> {
		ProgressDialog dialog;

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
				offset = Calendar.getInstance().getTimeZone().getRawOffset() / 3600000.0;
				date = Calendar.getInstance().getTimeInMillis();
				saveLocation();
			} else {
				Toast.makeText(Planets.this,
						"Unable to download location data.\nPlease try again",
						Toast.LENGTH_LONG).show();
			}
			dialog.dismiss();
		}

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(Planets.this, "",
					"Downloading Location.\nPlease wait...", true);
		}
	}

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

	// Planet name selection for the live position activity
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
		case WIFI_ALERT:
			if (resultCode == 1) {
				// go to wifi settings
				Intent callWiFiSettingIntent = new Intent(
						android.provider.Settings.ACTION_WIFI_SETTINGS);
				startActivityForResult(callWiFiSettingIntent, WIFI_STATUS);
			} else if (resultCode == 2) {
				// continue downloading
				DownloadFile DownloadFile = new DownloadFile();
				DownloadFile
						.execute("http://www.astro.com/ftp/swisseph/ephe/archive/sweph_18.zip");
			} else if (resultCode == Activity.RESULT_CANCELED) {
				Planets.this.finish();
			}
			return;
		case WIFI_STATUS:
			checkWiFi();
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
}

package planets.position;

/*
 * Copyright (C) 2010 Tim Gaddis
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
import java.net.URI;
import java.util.Calendar;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Planets extends Activity {

	private Button positionButton, whatupButton, downloadButton, manualButton;
	private TextView locationText;
	private InputStream myInput;
	private OutputStream myOutput;
	private long date = 0, locDate = 0;
	private LocationManager lm;
	private LocationListener ll;
	private PlanetsDbAdapter planetDbHelper;
	private ParsedLocationData locationData;
	private double elevation, latitude, longitude, temp, pressure, offset;
	private boolean running = false;
	private Bundle bundle;
	private ProgressDialog copyDialog;

	private static final int ACTIVITY_CREATE = 0;
	private static final int ABOUT_ID = 2;
	private static final int HELP_ID = 3;

	private boolean DEBUG = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		positionButton = (Button) findViewById(R.id.positionButton);
		manualButton = (Button) findViewById(R.id.manualEnterButton);
		whatupButton = (Button) findViewById(R.id.whatupButton);
		downloadButton = (Button) findViewById(R.id.downloadButton);
		locationText = (TextView) findViewById(R.id.locationData);

		positionButton.setEnabled(false);
		whatupButton.setEnabled(false);
		manualButton.setEnabled(false);
		downloadButton.setEnabled(false);

		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		planetDbHelper = new PlanetsDbAdapter(this, "location");
		planetDbHelper.open();

		if (!(checkFiles("semo_18.se1") && checkFiles("sepl_18.se1"))) {
			copyDialog = ProgressDialog.show(Planets.this, "",
					"Copying files. Please wait...", true);
			// copy files thread
			new CopyFilesTask().execute();
		} else
			loadLocation();

		manualButton.setEnabled(true);
		downloadButton.setEnabled(true);

		downloadButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				getGPS();
			}

		});

		positionButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				// save data
				launchPosition();
			}

		});

		manualButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				enterLocManual();
			}

		});

		whatupButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				launchWhatsUp();
			}

		});

	}

	private void getGPS() {
		// get lat/long from GPS
		running = true;
		new GetGPSWeatherTask().execute();

		if (DEBUG) {
			// Test data to use with the emulator
			latitude = 32.221743;
			longitude = -110.926479;
			elevation = 713.0;
			date = Calendar.getInstance().getTimeInMillis();
			getWeatherData();
			loadWeatherData();
			saveLocation();
			positionButton.setEnabled(true);
			whatupButton.setEnabled(true);
			running = false;
		} else {
			if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				showGPSDisabledAlertToUser();
			} else {
				ll = new mylocationlistener();
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
						ll);
			}
		}
	}

	private void launchPosition() {
		bundle = new Bundle();
		bundle.putDouble("Lat", latitude);
		bundle.putDouble("Long", longitude);
		bundle.putDouble("Elevation", elevation);
		bundle.putDouble("Temp", temp);
		bundle.putDouble("Pressure", pressure);
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
		bundle.putDouble("Temp", temp);
		bundle.putDouble("Pressure", pressure);
		bundle.putDouble("Offset", offset);
		Intent i = new Intent(this, ViewWhatsUp.class);
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
			temp = locCur.getDouble(locCur.getColumnIndexOrThrow("temp"));
			pressure = locCur.getDouble(locCur
					.getColumnIndexOrThrow("pressure"));
			offset = locCur.getDouble(locCur.getColumnIndexOrThrow("offset"));
			elevation = locCur.getDouble(locCur
					.getColumnIndexOrThrow("elevation"));
			String data = "";
			data += "Latitude: " + latitude;
			data += "\nLongitude: " + longitude;
			data += "\nElevation: " + elevation;
			data += "\nTemperature: " + temp;
			data += "\nPressure: " + pressure;
			data += "\nGMT offset: " + offset;
			locationText.setText(data);
			positionButton.setEnabled(true);
			whatupButton.setEnabled(true);
		} else {
			showLocationDataAlert();
		}
	}

	private void saveLocation() {
		// update location
		planetDbHelper.updateLocation(0, latitude, longitude, temp, pressure,
				date, offset, 13, elevation);
	}

	private class CopyFilesTask extends AsyncTask<Void, Void, Void> {
		// ProgressDialog dialog;

		@Override
		protected Void doInBackground(Void... params) {
			// copy the ephermeris files from assets folder to the sd card.
			try {
				// copyFile("seas_18.se1"); // 225440
				copyFile("semo_18.se1"); // 1305686
				copyFile("sepl_18.se1"); // 484065
			} catch (IOException e) {
				// e.printStackTrace();
				Log.d("CopyFile error", e.getMessage());
				Toast.makeText(Planets.this, "Error copying assets files",
						Toast.LENGTH_LONG).show();
				running = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			copyDialog.dismiss();
			loadLocation();
		}

		@Override
		protected void onPreExecute() {
			// dialog = ProgressDialog.show(Planets.this, "",
			// "Copying files. Please wait...", true);
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
		Log.d("Copy Files", filename);
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + "/ephemeris");
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		Log.d("File Dir", dir.getCanonicalPath());
		// check if ephemeris file is on sdcard, if not copy form assets folder
		File f = new File(dir + "/" + filename);
		Log.d("File Exists", "" + f.exists());
		if (!f.exists()) {

			myInput = this.getAssets().open(filename);
			Log.d("InputStream Open", "" + f.exists());

			myOutput = new FileOutputStream(f);
			Log.d("OutputStream Open", "" + f.exists());

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

	private class mylocationlistener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				elevation = location.getAltitude();
				date = Calendar.getInstance().getTimeInMillis();
				lm.removeUpdates(ll);
				// download weather data if older than 3 hours
				if (date - locDate > (3 * 60 * 60 * 1000)) {
					getWeatherData();
					loadWeatherData();
					saveLocation();
				}
				running = false;
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

	}

	private void getWeatherData() {
		// download weather data xml files

		// http://ws.geonames.org/findNearByWeatherXML?lat=43&lng=-2
		// http://ws.geonames.org/timezone?lat=47.01&lng=10.2

		// http://www.worldweatheronline.com/feed/weather.ashx
		// ?format=xml&num_of_days=1&key=77241f817e062244102410&q=32.00,-110.00

		// http://www.worldweatheronline.com/feed/tz.ashx
		// ?format=xml&key=77241f817e062244102410&q=32.00,-110.00

		try {
			ParsedLocationData locationDataSet = new ParsedLocationData();

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();

			XMLReader xr = sp.getXMLReader();
			XMLDataHandler dataHandler = new XMLDataHandler(locationDataSet);
			xr.setContentHandler(dataHandler);

			xr.parse(new InputSource(
					getData("http://ws.geonames.org/findNearByWeatherXML?lat="
							+ latitude + "&lng=" + longitude)));

			xr.parse(new InputSource(
					getData("http://ws.geonames.org/timezone?lat=" + latitude
							+ "&lng=" + longitude)));

			locationData = dataHandler.getParsedData();

			// check to see if an error was returned from Geonames
			if (locationData.getErrCode() >= 10) {
				// if error from Geonames, call worldweatheronline.com for data
				xr.parse(new InputSource(
						getData("http://www.worldweatheronline.com/feed/weather.ashx?format=xml&num_of_days=1&key=77241f817e062244102410&q="
								+ latitude + "," + longitude)));
				xr.parse(new InputSource(
						getData("http://www.worldweatheronline.com/feed/tz.ashx?format=xml&key=77241f817e062244102410&q="
								+ latitude + "," + longitude)));
				locationData = dataHandler.getParsedData();
			}
		} catch (Exception e) {
			Toast.makeText(Planets.this,
					"The following parsing error occured:\n" + e,
					Toast.LENGTH_LONG).show();
		}
	}

	private void loadWeatherData() {
		temp = locationData.getTemp();
		pressure = locationData.getPressure();
		offset = locationData.getOffset();
		String data = "";
		data += "Latitude: " + latitude;
		data += "\nLongitude: " + longitude;
		data += "\nElevation: " + elevation;
		data += "\nTemperature: " + temp;
		data += "\nPressure: " + pressure;
		data += "\nGMT offset: " + offset;
		locationText.setText(data);
		positionButton.setEnabled(true);
		whatupButton.setEnabled(true);
	}

	private InputStream getData(String url) throws Exception {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(new URI(url));
		HttpResponse response = client.execute(request);
		return response.getEntity().getContent();
	}

	private void showGPSDisabledAlertToUser() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
				.setMessage(
						"GPS is disabled in your device. Would you like to enable it?")
				.setCancelable(false)
				.setPositiveButton("Goto Settings Page To Enable GPS",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent callGPSSettingIntent = new Intent(
										android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								startActivity(callGPSSettingIntent);
							}
						});
		alertDialogBuilder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}

	private void showLocationDataAlert() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
				.setMessage(
						"There is no location data saved.  "
								+ "You can either download the data using GPS or "
								+ "manually enter the data.")
				.setCancelable(false)
				.setPositiveButton("GPS",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								getGPS();
							}
						});
		alertDialogBuilder.setNeutralButton("Manual",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						enterLocManual();
					}
				});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}

	private class GetGPSWeatherTask extends AsyncTask<Void, Void, Void> {
		ProgressDialog dialog;

		@Override
		protected Void doInBackground(Void... params) {
			while (true) {
				if (!running)
					break;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
		}

		@Override
		protected void onPreExecute() {
			dialog = ProgressDialog.show(Planets.this, "",
					"Downloading data. Please wait...", true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, HELP_ID, 0, R.string.menu_help);
		menu.add(0, ABOUT_ID, 0, R.string.menu_about);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Bundle b;
		Intent i;
		switch (item.getItemId()) {
		case ABOUT_ID:
			// about description
			b = new Bundle();
			b.putInt("res", R.string.main_about);
			i = new Intent(this, About.class);
			i.putExtras(b);
			startActivity(i);
			return true;
		case HELP_ID:
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
		startActivityForResult(i, ACTIVITY_CREATE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		loadLocation();
	}
}

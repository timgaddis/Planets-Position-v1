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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Planets extends Activity {

	private Button positionButton, whatupButton, downloadButton, manualButton;
	private TextView locationText;
	private long date = 0, locDate = 0;
	private LocationManager lm;
	private LocationListener ll;
	private PlanetsDbAdapter planetDbHelper;
	private ParsedLocationData locationData;
	private double elevation, latitude, longitude, offset;
	private boolean running = false;
	private Bundle bundle;

	private static final int LOCATION_MANUAL = 0;
	private static final int WIFI_STATUS = 1;
	private static final int WIFI_ALERT = 2;

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

		checkWiFi();

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
			getXMLData();
			loadXMLData();
			saveLocation();
			positionButton.setEnabled(true);
			whatupButton.setEnabled(true);
			running = false;
		} else {
			if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				ll = new mylocationlistener();
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
						ll);
			} else {
				ll = new mylocationlistener();
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
						0, ll);
			}
		}
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
			positionButton.setEnabled(true);
			whatupButton.setEnabled(true);
		} else {
			showLocationDataAlert();
		}
	}

	private void saveLocation() {
		// update location
		planetDbHelper.updateLocation(0, latitude, longitude, 0.0, 0.0, date,
				offset, 13, elevation);
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
				Log.d("DownloadTest error", "" + e);
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

	private class mylocationlistener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				elevation = location.getAltitude();
				date = Calendar.getInstance().getTimeInMillis();
				lm.removeUpdates(ll);
				saveLocation();
				// download time zone data
				getXMLData();
				loadXMLData();
				saveLocation();
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

	private void getXMLData() {
		// download time zone data xml files

		// http://api.geonames.org/timezone?lat=47.01&lng=10.2&username=tgaddis

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
					getData("http://api.geonames.org/timezone?lat=" + latitude
							+ "&lng=" + longitude + "&username=tgaddis")));

			locationData = dataHandler.getParsedData();

			// check to see if an error was returned from Geonames
			if (locationData.getErrCode() >= 10) {
				// if error from Geonames, call worldweatheronline.com for data
				xr.parse(new InputSource(
						getData("http://www.worldweatheronline.com/feed/tz.ashx?"
								+ "format=xml&key=77241f817e062244102410&q="
								+ latitude + "," + longitude)));
				locationData = dataHandler.getParsedData();
			}
		} catch (Exception e) {
			Log.d("Parse Error", "" + e);
			Toast.makeText(Planets.this,
					"The following parsing error occured:\n" + e,
					Toast.LENGTH_LONG).show();
		}
	}

	private void loadXMLData() {
		offset = locationData.getOffset();
		String data = "";
		data += "Latitude: " + latitude;
		data += "\nLongitude: " + longitude;
		data += "\nElevation: " + elevation;
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

}

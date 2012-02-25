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

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class LivePosition extends Activity {

	private TextView pRAText, pDecText, pMagText, pRiseText, pCountText, pRise;
	private TextView pAzText, pAltText, pDistText, pDateText, pTimeText,
			pNameText;

	double[] g = new double[3];
	private Calendar utc, c;
	private String planetName;
	private int planetNum = 0;
	private double offset;
	private long timeLeft;
	private Bundle bundle;
	private UpdatePosition updatePos;
	private Countdown counter;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native double[] planetLiveData(double d1, double d2, int p,
			double[] loc, double press, double temp);

	public native double[] utc2jd(int m, int d, int y, int hr, int min,
			double sec);

	public native String jd2utc(double jdate);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.live_position);

		pNameText = (TextView) findViewById(R.id.count_name_text);
		pDateText = (TextView) findViewById(R.id.count_date_text);
		pTimeText = (TextView) findViewById(R.id.count_time_text);
		pAzText = (TextView) findViewById(R.id.count_az_text);
		pAltText = (TextView) findViewById(R.id.count_alt_text);
		pRAText = (TextView) findViewById(R.id.count_ra_text);
		pDecText = (TextView) findViewById(R.id.count_dec_text);
		pDistText = (TextView) findViewById(R.id.count_dis_text);
		pMagText = (TextView) findViewById(R.id.count_mag_text);
		pRiseText = (TextView) findViewById(R.id.count_riseTime_text);
		pCountText = (TextView) findViewById(R.id.count_setTime_text);
		pRise = (TextView) findViewById(R.id.count_riseTime);

		utc = Calendar.getInstance();

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
			planetNum = bundle.getInt("planet");
			pNameText.setText(bundle.getString("name"));
		}

		updatePos = new UpdatePosition();
		updatePos.execute();
	}

	// stops the UpdatePosition thread and countdown before you leave this
	// activity
	@Override
	protected void onDestroy() {
		super.onDestroy();
		counter.cancel();
		updatePos.cancel(true);
	}

	private void resetTimer() {
		String[] dateArr;
		double[] data;
		int m;

		pRiseText.setText("");
		pCountText.setText("");

		m = (int) (offset * 60);
		utc.add(Calendar.MINUTE, m * -1);

		data = utc2jd(utc.get(Calendar.MONTH) + 1,
				utc.get(Calendar.DAY_OF_MONTH), utc.get(Calendar.YEAR),
				utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE),
				utc.get(Calendar.SECOND));
		if (data == null) {
			Log.e("Date error", "pos date error");
			Toast.makeText(getApplicationContext(),
					"Date conversion error,\nplease restart the activity",
					Toast.LENGTH_SHORT).show();
			this.finish();
		}
		// jdTT = data[0];
		// jdUT = data[1];

		data = planetLiveData(data[0], data[1], planetNum, g, 0.0, 0.0);
		if (data == null) {
			// if error is returned by planetLiveData then return to the main
			// screen
			Log.e("Position error", "planetUpData error");
			Toast.makeText(getApplicationContext(),
					"Planet calculation error,\nplease restart the activity",
					Toast.LENGTH_SHORT).show();
			this.finish();
		}
		// set - data[6]
		// rise - data[7]

		// check if planet has risen
		if (data[4] >= 0.0) {
			pRise.setText("Set Time");
			dateArr = jd2utc(data[6]).split("_");
		} else {
			pRise.setText("Rise Time");
			dateArr = jd2utc(data[7]).split("_");
		}

		utc.set(Integer.parseInt(dateArr[1]), Integer.parseInt(dateArr[2]) - 1,
				Integer.parseInt(dateArr[3]), Integer.parseInt(dateArr[4]),
				Integer.parseInt(dateArr[5]));
		utc.set(Calendar.MILLISECOND,
				(int) (Double.parseDouble(dateArr[6]) * 1000));
		// convert utc to local time
		utc.add(Calendar.MINUTE, m);
		pRiseText.setText(DateFormat.format("MM/dd h:mm aa", utc));
		c = Calendar.getInstance();
		timeLeft = utc.getTimeInMillis() - c.getTimeInMillis();
		counter = new Countdown(timeLeft, 1000);
		counter.start();
	}

	private class UpdatePosition extends AsyncTask<Void, Double, Void> {

		double[] data;
		double ra, dec, ras, decSign;
		double rah, ram, decd, decm, decs;
		int m, i = 0;

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPreExecute() {
			utc = Calendar.getInstance();
			// initial calculation to get rise/set time
			resetTimer();
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (true) {
				try {
					utc = Calendar.getInstance();

					// convert local time to UTC time
					m = (int) (offset * 60);
					utc.add(Calendar.MINUTE, m * -1);

					data = utc2jd(utc.get(Calendar.MONTH) + 1,
							utc.get(Calendar.DAY_OF_MONTH),
							utc.get(Calendar.YEAR),
							utc.get(Calendar.HOUR_OF_DAY),
							utc.get(Calendar.MINUTE), utc.get(Calendar.SECOND));
					if (data == null) {
						Log.e("Date error", "pos date error");
						Toast.makeText(
								getApplicationContext(),
								"Date conversion error,\nplease restart the activity",
								Toast.LENGTH_SHORT).show();
						this.cancel(true);
					}
					// jdTT = data[0];
					// jdUT = data[1];

					data = planetLiveData(data[0], data[1], planetNum, g, 0.0,
							0.0);
					if (data == null) {
						Log.e("Position error", "planetUpData error");
						Toast.makeText(
								getApplicationContext(),
								"Planet calculation error,\nplease restart the activity",
								Toast.LENGTH_SHORT).show();
						this.cancel(true);
					}
					ra = data[0];
					dec = data[1];

					// convert ra to hours
					ra = ra / 15;

					rah = (int) ra;
					ra -= rah;
					ra *= 60;
					ram = (int) ra;
					ra -= ram;
					ras = ra * 60;

					if (dec < 0) {
						decSign = -1.0;
						dec *= -1;
					} else {
						decSign = 1.0;
					}
					decd = (int) dec;
					dec -= decd;
					dec *= 60;
					decm = (int) dec;
					dec -= decm;
					dec *= 60;
					decs = (int) dec;

					publishProgress(rah, ram, ras, decSign, decd, decm, decs,
							data[2], data[3], data[4], data[5]);

					// Sleep for 1 sec.
					Thread.currentThread();
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.e("Position Thread Error", e.getMessage());
					e.printStackTrace();
				}
			}
		}

		@Override
		protected void onProgressUpdate(Double... values) {
			char ds;
			// convert UTC time to local time
			utc.add(Calendar.MINUTE, m);
			pDateText.setText(DateFormat.format("M/dd/yyyy", utc));
			pTimeText.setText(DateFormat.format("h:mmaa", utc));
			// update the rise/set time and the count down timer
			if (i > 60) {
				counter.cancel();
				resetTimer();
				i = 0;
			}
			i++;
			if (values[3] < 0.0) {
				ds = '-';
			} else {
				ds = '+';
			}
			pRAText.setText(String.format("%.0fh %.0fm %.1fs", values[0],
					values[1], values[2]));
			pDecText.setText(String.format("%c%.0f\u00b0 %.0f\' %.0f\"", ds,
					values[4], values[5], values[6]));
			pDistText.setText(String.format("%.4f AU", values[7]));
			pAzText.setText(String.format("%.2f\u00b0", values[8]));
			pAltText.setText(String.format("%.2f\u00b0", values[9]));
			pMagText.setText(Math.round(values[10]) + "");

		}

		@Override
		protected void onPostExecute(Void result) {
		}
	}

	public class Countdown extends CountDownTimer {

		public Countdown(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() {
			resetTimer();
		}

		@Override
		public void onTick(long millisUntilFinished) {
			long h, m, s, t;
			h = millisUntilFinished / 3600000;
			t = millisUntilFinished - (h * 3600000);
			m = t / 60000;
			t = t - (m * 60000);
			s = t / 1000;
			pCountText.setText(String.format("%02d:%02d:%02d", h, m, s));
		}
	}

}

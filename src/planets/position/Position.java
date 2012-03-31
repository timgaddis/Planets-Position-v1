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
import java.util.GregorianCalendar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Position extends FragmentActivity {

	private Button dateButton, timeButton, nameButton;
	private TextView pRAText, pDecText, pMagText, pRiseText, pSetText;
	private TextView pAzText, pAltText, pBelowText, pDistText;
	private Bundle bundle;
	private int mYear, mMonth, mDay, mHour, mMinute, planetNum = -1;
	private double offset;
	private double mSec;
	double[] g = new double[3];
	private Calendar gc, utc;
	private String planetName;
	private DialogFragment planetDialog, dateTimeDialog;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native double[] planetPosData(double d1, double d2, int p,
			double[] loc, double press, double temp);

	public native double[] utc2jd(int m, int d, int y, int hr, int min,
			double sec);

	public native String jd2utc(double jdate);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.position);

		dateButton = (Button) findViewById(R.id.pos_date_button);
		timeButton = (Button) findViewById(R.id.pos_time_button);
		nameButton = (Button) findViewById(R.id.pos_name_button);
		pAzText = (TextView) findViewById(R.id.pos_az_text);
		pAltText = (TextView) findViewById(R.id.pos_alt_text);
		pRAText = (TextView) findViewById(R.id.pos_ra_text);
		pDecText = (TextView) findViewById(R.id.pos_dec_text);
		pBelowText = (TextView) findViewById(R.id.pos_below_text);
		pDistText = (TextView) findViewById(R.id.pos_dis_text);
		pMagText = (TextView) findViewById(R.id.pos_mag_text);
		pRiseText = (TextView) findViewById(R.id.pos_riseTime_text);
		pSetText = (TextView) findViewById(R.id.pos_setTime_text);

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
		}

		// get the current date, time
		final Calendar c = Calendar.getInstance();
		mHour = c.get(Calendar.HOUR_OF_DAY);
		mMinute = c.get(Calendar.MINUTE);
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);

		// display the current date (this method is below)
		updateDisplay();
		showPlanetDialog();

		dateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dateTimeDialog = DateTimeDialog.newInstance(1, mHour, mMinute,
						mDay, mMonth, mYear);
				dateTimeDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
				dateTimeDialog.show(getSupportFragmentManager(), "dtDialog");
			}
		});

		nameButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showPlanetDialog();
			}
		});

		timeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dateTimeDialog = DateTimeDialog.newInstance(0, mHour, mMinute,
						mDay, mMonth, mYear);
				dateTimeDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
				dateTimeDialog.show(getSupportFragmentManager(), "dtDialog");
			}
		});

		computeLocation();
	}

	private void computeLocation() {
		if (planetNum >= 0 && planetNum < 10) {
			double[] data;
			double ra, dec, ras;
			int rah, ram, decd, decm, decs, m;
			char decSign;
			String[] dateArr;

			nameButton.setText(planetName);
			utc = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute, 0);
			m = (int) (offset * 60);
			utc.add(Calendar.MINUTE, m * -1);

			data = utc2jd(utc.get(Calendar.MONTH) + 1,
					utc.get(Calendar.DAY_OF_MONTH), utc.get(Calendar.YEAR),
					utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE),
					utc.get(Calendar.SECOND));
			if (data == null) {
				Log.e("Date error", "pos date error");
				return;
			}
			// jdTT = data[0];
			// jdUT = data[1];

			data = planetPosData(data[0], data[1], planetNum, g, 0.0, 0.0);
			if (data == null) {
				Log.e("Position error", "planetUpData error");
				return;
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

			pRAText.setText(String.format("%dh %dm %.1fs", rah, ram, ras));
			pDecText.setText(String.format("%c%d\u00b0 %d\' %d\"", decSign,
					decd, decm, decs));

			pDistText.setText(String.format("%.4f AU", data[2]));
			pAzText.setText(String.format("%.1f\u00b0", data[3]));
			pAltText.setText(String.format("%.1f\u00b0", data[4]));
			pMagText.setText(Math.round(data[5]) + "");

			dateArr = jd2utc(data[6]).split("_");
			utc.set(Integer.parseInt(dateArr[1]),
					Integer.parseInt(dateArr[2]) - 1,
					Integer.parseInt(dateArr[3]), Integer.parseInt(dateArr[4]),
					Integer.parseInt(dateArr[5]));
			utc.set(Calendar.MILLISECOND,
					(int) (Double.parseDouble(dateArr[6]) * 1000));
			// convert utc to local time
			utc.add(Calendar.MINUTE, m);
			pSetText.setText(DateFormat.format("MM/dd h:mm aa", utc));

			dateArr = jd2utc(data[7]).split("_");
			utc.set(Integer.parseInt(dateArr[1]),
					Integer.parseInt(dateArr[2]) - 1,
					Integer.parseInt(dateArr[3]), Integer.parseInt(dateArr[4]),
					Integer.parseInt(dateArr[5]));
			utc.set(Calendar.MILLISECOND,
					(int) (Double.parseDouble(dateArr[6]) * 1000));
			// convert utc to local time
			utc.add(Calendar.MINUTE, m);
			pRiseText.setText(DateFormat.format("MM/dd h:mm aa", utc));

			if (data[4] <= 0.0) {
				// below horizon
				pBelowText.setVisibility(View.VISIBLE);
			} else {
				// above horizon
				pBelowText.setVisibility(View.INVISIBLE);
			}
		}

	}

	// updates the date and time in the TextView
	private void updateDisplay() {
		gc = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute, 0);
		dateButton.setText(DateFormat.format("M/dd/yyyy", gc));
		timeButton.setText(DateFormat.format("h:mm aa", gc));
		computeLocation();
	}

	public void loadPlanet(String name, int num) {
		planetNum = num;
		planetName = name;
		computeLocation();
	}

	private void showPlanetDialog() {
		planetDialog = PlanetListDialog.newInstance(R.array.planets_array, 1,
				R.string.planet_prompt, 0);
		planetDialog.show(getSupportFragmentManager(), "planetDialog");
	}

	public void loadTime(int hourOfDay, int minute) {
		mHour = hourOfDay;
		mMinute = minute;
		updateDisplay();
	}

	public void loadDate(int year, int monthOfYear, int dayOfMonth) {
		mYear = year;
		mMonth = monthOfYear;
		mDay = dayOfMonth;
		updateDisplay();
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
			b.putInt("res", R.string.pos_help);
			i = new Intent(this, About.class);
			i.putExtras(b);
			startActivity(i);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
}

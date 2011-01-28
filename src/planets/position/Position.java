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

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

public class Position extends Activity {

	private TextView pDateText, pTimeText, pPlanetName;
	private TextView pRAText, pDecText;
	private TextView pAzText, pAltText, pBelowText, pDistText;
	// private Button showPosButton;
	private Bundle bundle;
	private int mYear, mMonth, mDay, mHour, mMinute, planetNum = -1;
	private double offset;
	private double mSec, pLat, pLong, pAltitude, pAz, pAlt;
	double[] g = new double[3];
	private Calendar gc, utc;
	private String planetName;
	static final int DATE_DIALOG_ID = 0;
	static final int TIME_DIALOG_ID = 1;
	private static final int HELP_ID = 3;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native double[] planetRADec(double d1, double d2, int p,
			double[] loc, double press, double temp);

	public native double[] utc2jd(int m, int d, int y, int hr, int min,
			double sec);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.position);

		// showPosButton = (Button) findViewById(R.id.pShowPos);
		pDateText = (TextView) findViewById(R.id.pDateText);
		pTimeText = (TextView) findViewById(R.id.pTimeText);
		pPlanetName = (TextView) findViewById(R.id.pPlanetName);
		pAzText = (TextView) findViewById(R.id.pAzText);
		pAltText = (TextView) findViewById(R.id.pAltText);
		pRAText = (TextView) findViewById(R.id.pRAText);
		pDecText = (TextView) findViewById(R.id.pDecText);
		pBelowText = (TextView) findViewById(R.id.pBelowText);
		pDistText = (TextView) findViewById(R.id.pDistText);

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

		pPlanetName.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				planetDialog();
			}
		});

		pDateText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});

		pTimeText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID);
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

			utc = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute, 0);
			m = (int) (offset * 60);
			utc.add(Calendar.MINUTE, m * -1);

			data = utc2jd(utc.get(Calendar.MONTH) + 1,
					utc.get(Calendar.DAY_OF_MONTH), utc.get(Calendar.YEAR),
					utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE),
					utc.get(Calendar.SECOND));
			if (data == null) {
				System.out.println("date error");
				return;
			}
			// jdTT = data[0];
			// jdUT = data[1];

			data = planetRADec(data[0], data[1], planetNum, g, 0.0, 0.0);
			if (data == null) {
				System.out.println("position error");
				return;
			}
			ra = data[0];
			dec = data[1];
			pAz = data[3];
			pAlt = data[4];

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

			if (data[4] <= 0.0) {
				// below horizon
				// showPosButton.setVisibility(View.GONE);
				pBelowText.setVisibility(View.VISIBLE);
			} else {
				// above horizon
				// showPosButton.setVisibility(View.VISIBLE);
				pBelowText.setVisibility(View.INVISIBLE);
			}
		}

	}

	// updates the date and time in the TextView
	private void updateDisplay() {
		gc = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute, 0);
		pDateText.setText(DateFormat.format("M/dd/yyyy", gc));
		pTimeText.setText(DateFormat.format("h:mmaa", gc));
		computeLocation();
	}

	private void planetDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose a planet");
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, R.array.planets_array,
						android.R.layout.select_dialog_item);
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				planetNum = item;
				planetName = (String) adapter.getItem(item);
				pPlanetName.setText(adapter.getItem(item));
				computeLocation();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			updateDisplay();
		}
	};

	// the callback received when the user "sets" the time in the dialog
	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHour = hourOfDay;
			mMinute = minute;
			updateDisplay();
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth,
					mDay);
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeSetListener, mHour, mMinute,
					false);
		}
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, HELP_ID, 0, R.string.menu_help);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Bundle b;
		Intent i;
		switch (item.getItemId()) {
		case HELP_ID:
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

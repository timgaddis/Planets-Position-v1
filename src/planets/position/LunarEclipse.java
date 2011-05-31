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

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Toast;

public class LunarEclipse extends Activity {

	private Button prevEclButton, nextEclButton;
	private CheckBox localEclCheck;
	private ListView list;
	private Bundle bundle;
	double[] g = new double[3];
	private double offset;
	private long startEcl, endEcl;
	private PlanetsDbAdapter planetDbHelper;
	private Calendar c;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lunar_main);

		prevEclButton = (Button) findViewById(R.id.prevLEclButton);
		nextEclButton = (Button) findViewById(R.id.nextLEclButton);
		localEclCheck = (CheckBox) findViewById(R.id.localLEclCheck);
		list = (ListView) findViewById(R.id.lunarEclList);

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			offset = bundle.getDouble("Offset", 0);
			g[1] = bundle.getDouble("Lat", 0);
			g[0] = bundle.getDouble("Long", 0);
			g[2] = bundle.getDouble("Elevation", 0);
		}

		planetDbHelper = new PlanetsDbAdapter(this, "solarEcl");
		planetDbHelper.open();

		c = Calendar.getInstance();
		startEcl = c.getTimeInMillis();

		computeEclipses(startEcl, 0, false);

		prevEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(getApplicationContext(), "Previous 10 eclipses",
						Toast.LENGTH_SHORT).show();
			}
		});

		nextEclButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(getApplicationContext(), "Next 10 eclipses",
						Toast.LENGTH_SHORT).show();
			}
		});

		localEclCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (arg1) {
					Toast.makeText(getApplicationContext(), "Selected",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getApplicationContext(), "Not selected",
							Toast.LENGTH_SHORT).show();
				}
			}

		});

	}

	private void computeEclipses(long startTime, int backward, boolean local) {
		// compute next or previous 10 eclipses and save them to the DB
		for (int i = 0; i < 10; i++) {

		}
	}
}

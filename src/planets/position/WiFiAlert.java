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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WiFiAlert extends Activity {

	private Button button1, button2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alert);

		button1 = (Button) findViewById(R.id.buttonWifi);
		button2 = (Button) findViewById(R.id.buttonContinue);

		button1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Wifi
				setResult(1);
				finish();
			}
		});

		button2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Continue
				setResult(2);
				finish();
			}
		});
	}
}

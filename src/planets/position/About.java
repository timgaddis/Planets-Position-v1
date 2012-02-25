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

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class About extends Activity {

	private TextView aboutText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		aboutText = (TextView) findViewById(R.id.aboutText);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			aboutText.setText(bundle.getInt("res"));
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		setResult(RESULT_OK);
		finish();
	}
}

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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class LocationDialog extends DialogFragment {

	static LocationDialog newInstance() {
		return new LocationDialog();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.location_dialog, container, false);

		Button downAlertButton = (Button) v.findViewById(R.id.locDownAlertBtn);
		Button manualAlertButton = (Button) v.findViewById(R.id.locManAlertBtn);

		downAlertButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				((Planets) getActivity()).getLocation();
			}
		});

		manualAlertButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				((Planets) getActivity()).enterLocManual();
			}
		});

		return v;
	}
}

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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class PlanetListDialog extends DialogFragment {

	public static PlanetListDialog newInstance(int list, int which, int title,
			int filter) {
		PlanetListDialog frag = new PlanetListDialog();
		Bundle args = new Bundle();
		args.putInt("list", list);
		args.putInt("which", which);
		args.putInt("title", title);
		args.putInt("filter", filter);
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int list = getArguments().getInt("list");
		final int which = getArguments().getInt("which");
		int title = getArguments().getInt("title");
		int filter = getArguments().getInt("filter");
		final Resources res = getResources();

		if (which == 3) {
			// filter planets in What's Up activity
			return new AlertDialog.Builder(getActivity())
					.setTitle(title)
					.setSingleChoiceItems(list, filter,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									((ViewWhatsUp) getActivity())
											.loadFilter(item);
								}
							}).create();
		} else {
			return new AlertDialog.Builder(getActivity()).setTitle(title)
					.setItems(list, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							// switch for LivePosition, Position, NewLoc
							// activities
							switch (which) {
							case 0:
								// gps offset
								String[] gpsArray = res
										.getStringArray(R.array.gps_array);
								((NewLoc) getActivity())
										.loadOffset(gpsArray[item]);
								break;
							case 1:
								// sky position in Planets
								String[] planetArray1 = res
										.getStringArray(R.array.planets_array);
								((Planets) getActivity()).loadPlanets(
										planetArray1[item], item, 1);
								break;
							case 4:
								// sky position in Position
								String[] planetArray2 = res
										.getStringArray(R.array.planets_array);
								((Position) getActivity()).loadPlanet(
										planetArray2[item], item);
								break;
							case 2:
								// real time position
								String[] planetLiveArray = res
										.getStringArray(R.array.planets_array);
								((Planets) getActivity()).loadPlanets(
										planetLiveArray[item], item, 2);
								break;
							}
						}
					}).create();
		}
	}
}

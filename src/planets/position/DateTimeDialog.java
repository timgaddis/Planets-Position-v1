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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class DateTimeDialog extends DialogFragment {

	public static DateTimeDialog newInstance(int id, int hour, int min,
			int day, int month, int year) {
		DateTimeDialog frag = new DateTimeDialog();
		Bundle args = new Bundle();
		args.putInt("id", id);
		args.putInt("hour", hour);
		args.putInt("min", min);
		args.putInt("day", day);
		args.putInt("month", month);
		args.putInt("year", year);
		frag.setArguments(args);
		return frag;
	}

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			((Position) getActivity()).loadDate(year, monthOfYear, dayOfMonth);
		}
	};

	// the callback received when the user "sets" the time in the dialog
	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			((Position) getActivity()).loadTime(hourOfDay, minute);
		}
	};

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int id = getArguments().getInt("id");
		int hour = getArguments().getInt("hour");
		int min = getArguments().getInt("min");
		int month = getArguments().getInt("month");
		int day = getArguments().getInt("day");
		int year = getArguments().getInt("year");

		if (id == 0) {
			// time dialog
			return new TimePickerDialog(getActivity(), mTimeSetListener, hour,
					min, false);
		} else {
			// date dialog
			return new DatePickerDialog(getActivity(), mDateSetListener, year,
					month, day);
		}
	}

}

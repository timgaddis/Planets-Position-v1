package planets.position;

import java.util.Date;

import android.app.Dialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class PlanetDialog extends Dialog {
	private Button closeButton;
	private TextView planetName, raText, decText, azText, altText, disText,
			magText, setTimeText;

	public PlanetDialog(Context context, String name, double[] data, long setT) {
		super(context);

		// 'Window.FEATURE_NO_TITLE' - Used to hide the title
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.info);

		planetName = (TextView) findViewById(R.id.info_name);
		raText = (TextView) findViewById(R.id.info_ra_text);
		decText = (TextView) findViewById(R.id.info_dec_text);
		azText = (TextView) findViewById(R.id.info_az_text);
		altText = (TextView) findViewById(R.id.info_alt_text);
		disText = (TextView) findViewById(R.id.info_dis_text);
		magText = (TextView) findViewById(R.id.info_mag_text);
		setTimeText = (TextView) findViewById(R.id.info_setTime_text);
		closeButton = (Button) findViewById(R.id.closeButton);

		planetName.setText(name);
		raText.setText(convertRaDec(data[0], 0));
		decText.setText(convertRaDec(data[1], 1));
		azText.setText(convertRaDec(data[2], 3));
		altText.setText(convertRaDec(data[3], 3));
		disText.setText(convertRaDec(data[4], 2));
		magText.setText(Math.round(data[5]) + "");
		Date d = new Date(setT);
		setTimeText.setText(DateFormat.format("MM/dd h:mm aa", d));

		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				closeDialog(view);
			}

		});

	}

	private void closeDialog(View v) {
		// When Button is clicked, dismiss the dialog
		if (v == closeButton)
			dismiss();
	}

	private String convertRaDec(double value, int type) {
		double ra, dec, ras;
		int rah, ram, decd, decm, decs;
		char decSign;
		switch (type) {
		case 0:
			// RA value
			ra = value;
			// convert ra to hours
			ra = ra / 15;

			rah = (int) ra;
			ra -= rah;
			ra *= 60;
			ram = (int) ra;
			ra -= ram;
			ras = ra * 60;
			return String.format("%dh %dm %.1fs", rah, ram, ras);
		case 1:
			// Dec value
			dec = value;
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
			return String.format("%c%d\u00b0 %d\' %d\"", decSign, decd, decm,
					decs);
		case 2:
			// Distance value
			return String.format("%.4f AU", value);
		case 3:
			// Az or Alt value
			return String.format("%.1f\u00b0", value);
		default:
			return "";
		}

	}

}

package planets.position;

import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.ImageView;
import android.widget.TextView;

public class PlanetInfo extends Activity {

	private ImageView image;
	private TextView raText, decText, azText, altText, disText, magText,
			setTimeText;
	private Bundle bundle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);

		image = (ImageView) findViewById(R.id.backImage);
		raText = (TextView) findViewById(R.id.ra_text);
		decText = (TextView) findViewById(R.id.dec_text);
		azText = (TextView) findViewById(R.id.az_text);
		altText = (TextView) findViewById(R.id.alt_text);
		disText = (TextView) findViewById(R.id.dis_text);
		magText = (TextView) findViewById(R.id.mag_text);
		setTimeText = (TextView) findViewById(R.id.set_text);

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			this.setTitle(bundle.getString("name"));
			image.setImageResource(bundle.getInt("image"));
			raText.setText(convertRaDec(bundle.getDouble("ra"), 0));
			decText.setText(convertRaDec(bundle.getDouble("dec"), 1));
			azText.setText(convertRaDec(bundle.getDouble("az"), 3));
			altText.setText(convertRaDec(bundle.getDouble("alt"), 3));
			disText.setText(convertRaDec(bundle.getDouble("dis"), 2));
			magText.setText(bundle.getDouble("mag") + "");
			Date d = new Date(bundle.getLong("setT"));
			setTimeText.setText(DateFormat.format("h:mm aa", d));
		}
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

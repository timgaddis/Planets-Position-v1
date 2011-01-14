package planets.position;

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

}

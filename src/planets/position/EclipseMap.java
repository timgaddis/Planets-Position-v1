package planets.position;

import android.os.Bundle;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class EclipseMap extends MapActivity {

	private MapView mapView;
	private TextView eclipseDateText;// , eclipseTypeText;
	private double latitude = 0, longitude = 0, startDate, endDate;
	private GeoPoint p1, p2;

	// load c library
	static {
		System.loadLibrary("planets_swiss");
	}

	// c function prototypes
	public native double[] solarDataPos(double d2);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.eclipse_map);
		Bundle bundle;

		eclipseDateText = (TextView) findViewById(R.id.eclDateMapText);
		// eclipseTypeText = (TextView) findViewById(R.id.eclTypeMapText);
		mapView = (MapView) findViewById(R.id.mapImport);
		mapView.setBuiltInZoomControls(true);
		mapView.getOverlays().clear();

		// load bundle from previous activity
		bundle = getIntent().getExtras();
		if (bundle != null) {
			latitude = bundle.getDouble("Lat", 0);
			longitude = bundle.getDouble("Long", 0);
			startDate = bundle.getDouble("start", 0);
			endDate = bundle.getDouble("end", 0);
			eclipseDateText.setText(bundle.getCharSequence("date") + "\n"
					+ bundle.getCharSequence("type"));
			// eclipseTypeText.setText();
		}

		// device location
		p1 = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
		mapView.getOverlays().add(new RouteOverlay(p1, p1, 1, this));
		computePath();
		mapView.invalidate();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void computePath() {
		double[] data;
		double interval = (endDate - startDate) / 80.0;
		double date = startDate + interval;

		// eclipse path
		data = solarDataPos(startDate);
		p2 = new GeoPoint((int) (data[1] * 1E6), (int) (data[0] * 1E6));

		for (int i = 0; i < 80; i++) {
			p1 = p2;
			data = solarDataPos(date);
			p2 = new GeoPoint((int) (data[1] * 1E6), (int) (data[0] * 1E6));
			mapView.getOverlays().add(new RouteOverlay(p1, p2, 2));
			date += interval;
		}
	}
}

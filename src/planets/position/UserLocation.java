package planets.position;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class UserLocation {
	Timer timer1;
	LocationManager lm;
	LocationResult locationResult;
	private Location gpsLoc, netLoc;
	boolean gps_enabled = false;
	boolean network_enabled = false;

	public boolean getLocation(Context context, LocationResult result) {
		// Log.i("LocationTest", "In getLocation");
		gpsLoc = null;
		netLoc = null;
		locationResult = result;
		if (lm == null)
			lm = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);

		// exceptions will be thrown if provider is not permitted.
		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}
		try {
			network_enabled = lm
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception ex) {
		}

		// don't start listeners if no provider is enabled
		if (!gps_enabled && !network_enabled)
			return false;
		// Log.i("LocationTest", "GPS enabled check");
		if (gps_enabled)
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
					locationListenerGps);
		// Log.i("LocationTest", "Net enabled check");
		if (network_enabled)
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
					locationListenerNetwork);

		// Log.i("LocationTest", "Start counter");
		timer1 = new Timer();
		timer1.schedule(new GetLastLocation(), 20000);
		return true;
	}

	LocationListener locationListenerGps = new LocationListener() {
		public void onLocationChanged(Location location) {
			gpsLoc = location;
			// Log.i("LocationTest", "GPS location found");
			lm.removeUpdates(this);
			// processLocation();
			if (network_enabled && netLoc != null) {
				if (gpsLoc.getTime() > netLoc.getTime())
					locationResult.gotLocation(gpsLoc);
				else
					locationResult.gotLocation(netLoc);
				timer1.cancel();
				return;
			} else if (!network_enabled) {
				locationResult.gotLocation(gpsLoc);
				timer1.cancel();
				return;
			}
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	LocationListener locationListenerNetwork = new LocationListener() {
		public void onLocationChanged(Location location) {
			netLoc = location;
			// Log.i("LocationTest", "Net location found");
			lm.removeUpdates(this);
			// processLocation();
			if (gps_enabled && gpsLoc != null) {
				if (gpsLoc.getTime() > netLoc.getTime())
					locationResult.gotLocation(gpsLoc);
				else
					locationResult.gotLocation(netLoc);
				timer1.cancel();
				return;
			} else if (!gps_enabled) {
				locationResult.gotLocation(netLoc);
				timer1.cancel();
				return;
			}
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	class GetLastLocation extends TimerTask {
		@Override
		public void run() {
			// Log.i("LocationTest", "** Countdown finished **");
			lm.removeUpdates(locationListenerGps);
			lm.removeUpdates(locationListenerNetwork);

			Location net_loc = null, gps_loc = null;

			if (gps_enabled) {
				// Log.i("LocationTestrun", "GPS enabled");
				gps_loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			}

			if (network_enabled) {
				// Log.i("LocationTestrun", "Net enabled");
				net_loc = lm
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}

			// if there are both values use the latest one
			if (gps_loc != null && net_loc != null) {
				// Log.i("LocationTestrun", "Both not null");
				if (gps_loc.getTime() > net_loc.getTime())
					locationResult.gotLocation(gps_loc);
				else
					locationResult.gotLocation(net_loc);
				return;
			}

			if (gps_loc != null) {
				// Log.i("LocationTestrun", "GPS not null");
				locationResult.gotLocation(gps_loc);
				return;
			}
			if (net_loc != null) {
				// Log.i("LocationTestrun", "Net not null");
				locationResult.gotLocation(net_loc);
				return;
			}
			locationResult.gotLocation(new Location(
					LocationManager.PASSIVE_PROVIDER));
		}
	}

	public static abstract class LocationResult {
		public abstract void gotLocation(Location location);
	}
}

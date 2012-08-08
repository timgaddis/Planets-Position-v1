package planets.position;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class RouteOverlay extends Overlay {

	private GeoPoint gp1;
	private GeoPoint gp2;
	private int mode = 0;
	private Context mContext = null;

	public RouteOverlay(GeoPoint gp1, GeoPoint gp2, int mode, Context context) {
		this.gp1 = gp1;
		this.gp2 = gp2;
		this.mode = mode;
		mContext = context;
	}

	public RouteOverlay(GeoPoint gp1, GeoPoint gp2, int mode) {
		this.gp1 = gp1;
		this.gp2 = gp2;
		this.mode = mode;
	}

	public int getMode() {
		return mode;
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		Projection projection = mapView.getProjection();
		if (shadow == false) {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			Point point = new Point();
			projection.toPixels(gp1, point);
			if (mode == 1) {
				Bitmap bmp1 = BitmapFactory.decodeResource(
						mContext.getResources(), R.drawable.marker);
				Bitmap bmp2 = BitmapFactory.decodeResource(
						mContext.getResources(), R.drawable.shadow);
				canvas.drawBitmap(bmp1, point.x - bmp1.getWidth() / 2, point.y
						- bmp1.getHeight(), null);
				canvas.drawBitmap(bmp2, point.x - 15,
						point.y - bmp2.getHeight(), null);
			} else if (mode == 2) {
				paint.setColor(Color.RED);
				Point point2 = new Point();
				projection.toPixels(gp2, point2);
				paint.setStrokeWidth(5);
				paint.setAlpha(120);
				canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);
			}
		}
		return super.draw(canvas, mapView, shadow, when);
	}

}

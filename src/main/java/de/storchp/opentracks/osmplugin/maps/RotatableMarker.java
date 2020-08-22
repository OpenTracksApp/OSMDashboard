package de.storchp.opentracks.osmplugin.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.layer.overlay.Marker;

import de.storchp.opentracks.osmplugin.compass.VectorCompass;
import de.storchp.opentracks.osmplugin.utils.ArrowMode;
import de.storchp.opentracks.osmplugin.utils.MapMode;

public class RotatableMarker extends Marker {

    private static final String TAG = RotatableMarker.class.getSimpleName();

    private final android.graphics.Bitmap markerBitmap;

    public RotatableMarker(final LatLong latLong, final android.graphics.Bitmap markerBitmap) {
        super(latLong, createRotatedMarkerBitmap(markerBitmap, 0), 0, 0);
        this.markerBitmap = markerBitmap;
    }

    private static Bitmap createRotatedMarkerBitmap(final android.graphics.Bitmap markerBitmap, final float degrees) {
        final Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return new AndroidBitmap(android.graphics.Bitmap.createBitmap(markerBitmap, 0, 0, markerBitmap.getWidth(), markerBitmap.getHeight(), matrix, true));
    }

    public static android.graphics.Bitmap getBitmapFromVectorDrawable(final Context context, final int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        drawable = (DrawableCompat.wrap(drawable)).mutate();

        final android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void rotateTo(final float degrees) {
        setBitmap(createRotatedMarkerBitmap(markerBitmap, degrees));
    }

    public void rotateWith(final ArrowMode arrowMode, final MapMode mapMode, final MovementDirection movementDirection, final VectorCompass compass) {
        Log.d(TAG, "Map heading: " + mapMode.getHeading(movementDirection, compass) + ", Arrow degrees: " + arrowMode.getDegrees(movementDirection, compass));
        if ((arrowMode == ArrowMode.COMPASS && mapMode == MapMode.COMPASS)
            || arrowMode == ArrowMode.NORTH) {
            rotateTo(0);
        } else if (arrowMode == ArrowMode.DIRECTION && mapMode == MapMode.DIRECTION) {
            rotateTo(mapMode.getHeading(movementDirection, compass));
        } else if (arrowMode == ArrowMode.DIRECTION && mapMode == MapMode.COMPASS) {
            rotateTo(arrowMode.getDegrees(movementDirection, compass));
        } else {
            rotateTo(arrowMode.getDegrees(movementDirection, compass) + mapMode.getHeading(movementDirection, compass) % 360);
        }
    }

}

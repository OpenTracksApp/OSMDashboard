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

import de.storchp.opentracks.osmplugin.compass.Compass;
import de.storchp.opentracks.osmplugin.utils.ArrowMode;
import de.storchp.opentracks.osmplugin.utils.MapMode;

public class RotatableMarker extends Marker {

    private static final String TAG = RotatableMarker.class.getSimpleName();
    private final android.graphics.Bitmap markerBitmap;
    private float currentDegrees = 0;

    public RotatableMarker(final LatLong latLong, final android.graphics.Bitmap markerBitmap) {
        super(latLong, createRotatedMarkerBitmap(markerBitmap, 0), 0, 0);
        this.markerBitmap = markerBitmap;
    }

    private static Bitmap createRotatedMarkerBitmap(final android.graphics.Bitmap markerBitmap, final float degrees) {
        final Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        // TODO: think about destroying / reusing bitmaps
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

    private boolean rotateTo(final float degrees) {
        if (Math.abs(currentDegrees - degrees) > 1) {
            // only create a new Marker Bitmap if it is at least 1 degree different
            Log.d(TAG, "CurrentDegrees=" + currentDegrees + ", degrees=" + degrees);
            setBitmap(createRotatedMarkerBitmap(markerBitmap, degrees));
            currentDegrees = degrees;
            return true;
        }
        return false;
    }

    public boolean rotateWith(final ArrowMode arrowMode, final MapMode mapMode, final MovementDirection movementDirection, final Compass compass) {
        if ((arrowMode == ArrowMode.COMPASS && mapMode == MapMode.COMPASS)
            || arrowMode == ArrowMode.NORTH) {
            return rotateTo(0);
        } else if (arrowMode == ArrowMode.DIRECTION && mapMode == MapMode.DIRECTION) {
            return rotateTo(mapMode.getHeading(movementDirection, compass));
        } else if (arrowMode == ArrowMode.DIRECTION && mapMode == MapMode.COMPASS) {
            return rotateTo(arrowMode.getDegrees(movementDirection, compass));
        } else {
            return rotateTo(arrowMode.getDegrees(movementDirection, compass) + mapMode.getHeading(movementDirection, compass) % 360);
        }
    }

}

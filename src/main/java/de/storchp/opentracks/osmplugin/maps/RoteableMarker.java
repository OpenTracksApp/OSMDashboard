package de.storchp.opentracks.osmplugin.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.layer.overlay.Marker;

public class RoteableMarker extends Marker {

    private final android.graphics.Bitmap markerBitmap;

    public RoteableMarker(final LatLong latLong, final android.graphics.Bitmap markerBitmap, final float degrees) {
        super(latLong, createRotatedMarkerBitmap(markerBitmap, degrees), 0, 0);
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

    public void rotateTo(final float degrees) {
        setBitmap(createRotatedMarkerBitmap(markerBitmap, degrees));
    }

}

package de.storchp.opentracks.osmplugin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import org.mapsforge.map.android.view.MapView;

public class MapsforgeMapView extends MapView {

    private final GestureDetector gestureDetector;
    private MapDragListener onDragListener;

    public MapsforgeMapView(final Context context, final AttributeSet attributeSet) {
        super(context, attributeSet);

        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    public void setOnMapDragListener(final MapDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }

    /**
     * Notifies the parent class when a MapView has been dragged
     */
    public interface MapDragListener {

        void onDrag();

    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return true;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                                final float distanceX, final float distanceY) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

}

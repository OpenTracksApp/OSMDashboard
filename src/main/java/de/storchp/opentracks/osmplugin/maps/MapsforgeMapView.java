package de.storchp.opentracks.osmplugin.maps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import org.mapsforge.map.android.view.MapView;

public class MapsforgeMapView extends MapView {

    private final GestureDetector gestureDetector;
    private MapDragListener onDragListener;

    public MapsforgeMapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    public void setOnMapDragListener(MapDragListener onDragListener) {
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
        public boolean onDoubleTap(MotionEvent e) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

}

package de.storchp.opentracks.osmplugin.compass;

import java.util.ArrayList;
import java.util.List;

/**
 * Derived from: <a href="https://github.com/kylecorry31/Trail-Sense/blob/master/app/src/main/java/com/kylecorry/trail_sense/shared/sensors/AbstractSensor.kt">...</a>
 */
public abstract class AbstractSensor {

    private final List<SensorListener> listeners = new ArrayList<>();
    private boolean started = false;

    public void start(SensorListener listener){
        listeners.add(listener);
        if (started) {
            return;
        }
        startImpl();
        started = true;
    }

    public void stop(SensorListener listener){
        synchronized(listeners) {
            if (listener != null) {
                listeners.remove(listener);
            } else {
                listeners.clear();
            }
        }
        if (!listeners.isEmpty()) {
            return;
        }
        if (!started) {
            return;
        }

        stopImpl();
        started = false;
    }

    protected abstract void startImpl();
    protected abstract void stopImpl();

    protected void notifyListeners(){
        synchronized(listeners) {
            listeners.stream().filter(e -> !e.updateSensor()).forEach(this::stop);
        }
    }

}

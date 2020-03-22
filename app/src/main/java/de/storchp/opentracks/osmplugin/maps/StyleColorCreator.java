package de.storchp.opentracks.osmplugin.maps;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

/**
 * Creates continues distinguished colors via golden ration.
 * Adapted from: https://github.com/dennisguse/TheKarte/blob/master/src/StyleColorCreator.js
 * Code for color generation was taken partly from https://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
 */
public class StyleColorCreator {

    public static final double GOLDEN_RATIO_CONJUGATE = 0.618033988749895;
    private double h;

    public StyleColorCreator(double start) {
        this.h = start;
    }

    public StyleColorCreator() {
        this(0);
    }

    private int convertHSVtoColorRGB(int alpha, double hue, double saturation, double value) {
        double i = Math.floor(hue * 6);
        double f = hue * 6 - i;
        double p = value * (1 - saturation);
        double q = value * (1 - f * saturation);
        double t = value * (1 - (1 - f) * saturation);
        double red = 0;
        double green = 0;
        double blue = 0;
        switch ((int) (i % 6)) {
            case 0:
                red = value;
                green = t;
                blue = p;
                break;
            case 1:
                red = q;
                green = value;
                blue = p;
                break;
            case 2:
                red = p;
                green = value;
                blue = t;
                break;
            case 3:
                red = p;
                green = q;
                blue = value;
                break;
            case 4:
                red = t;
                green = p;
                blue = value;
                break;
            case 5:
                red = value;
                green = p;
                blue = q;
                break;
        }

        return AndroidGraphicFactory.INSTANCE.createColor(alpha, (int) (red * 255), (int) (green * 255), (int) (blue * 255));
    }

    /**
     * Go to next color.
     *
     * @param alpha The opacity (alpha channel) of the color.
     * @return The color.
     */
    public int nextColor(int alpha) {
        this.h += GOLDEN_RATIO_CONJUGATE;
        this.h %= 1;

        return convertHSVtoColorRGB(alpha, this.h, 0.99, 0.99);
    }

    /**
     * Go to next color.
     *
     * @return The color.
     */
    public int nextColor() {
        return nextColor(255);
    }

}
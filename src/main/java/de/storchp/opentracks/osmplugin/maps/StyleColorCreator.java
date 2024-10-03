package de.storchp.opentracks.osmplugin.maps;

import org.oscim.backend.canvas.Color;

/**
 * Creates continues distinguished colors via golden ration.
 * Adapted from: <a href="https://github.com/dennisguse/TheKarte/blob/master/src/StyleColorCreator.js">...</a>
 * Code for color generation was taken partly from <a href="https://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/">...</a>
 */
public class StyleColorCreator {

    public static final double GOLDEN_RATIO_CONJUGATE = 0.618033988749895;
    private double h;

    public StyleColorCreator(double start) {
        this.h = start;
    }

    /**
     * @noinspection SameParameterValue
     */
    private int convertHSVtoColorRGB(double hue, double saturation, double value) {
        double i = Math.floor(hue * 6);
        double f = hue * 6 - i;
        double p = value * (1 - saturation);
        double q = value * (1 - f * saturation);
        double t = value * (1 - (1 - f) * saturation);
        double red = 0;
        double green = 0;
        double blue = 0;
        switch ((int) (i % 6)) {
            case 0 -> {
                red = value;
                green = t;
                blue = p;
            }
            case 1 -> {
                red = q;
                green = value;
                blue = p;
            }
            case 2 -> {
                red = p;
                green = value;
                blue = t;
            }
            case 3 -> {
                red = p;
                green = q;
                blue = value;
            }
            case 4 -> {
                red = t;
                green = p;
                blue = value;
            }
            case 5 -> {
                red = value;
                green = p;
                blue = q;
            }
        }

        return Color.get((int) (red * 255), (int) (green * 255), (int) (blue * 255));
    }

    /**
     * Go to next color.
     *
     * @return The color.
     */
    public int nextColor() {
        this.h += GOLDEN_RATIO_CONJUGATE;
        this.h %= 1;

        return convertHSVtoColorRGB(this.h, 0.99, 0.99);
    }

}
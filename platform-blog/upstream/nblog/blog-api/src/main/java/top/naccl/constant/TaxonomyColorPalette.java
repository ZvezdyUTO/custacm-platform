package top.naccl.constant;

import java.awt.Color;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

// Author: huangbingrui.awa
public final class TaxonomyColorPalette {
    public static final String DEFAULT_COLOR = "#8B1E3F";
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private TaxonomyColorPalette() {}

    public static String normalize(String color) {
        return color != null && HEX_COLOR.matcher(color).matches()
                ? color.toUpperCase(Locale.ROOT)
                : DEFAULT_COLOR;
    }

    public static String randomDark() {
        return randomDark(ThreadLocalRandom.current());
    }

    static String randomDark(RandomGenerator random) {
        float hue = random.nextFloat();
        float saturation = 0.65f + random.nextFloat() * 0.30f;
        float brightness = 0.32f + random.nextFloat() * 0.12f;
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}

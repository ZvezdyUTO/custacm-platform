package top.naccl.constant;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Author: huangbingrui.awa
class TaxonomyColorPaletteTest {
    @Test
    void acceptsArbitraryHexForCustomCategoryColors() {
        assertEquals("#12ABEF", TaxonomyColorPalette.normalize("#12abef"));
        assertEquals(TaxonomyColorPalette.DEFAULT_COLOR, TaxonomyColorPalette.normalize("orange"));
    }

    @Test
    void generatesManyDeepTagColorsInsteadOfSelectingFromAPresetPalette() {
        Random random = new Random(42);
        var colors = java.util.stream.IntStream.range(0, 100)
                .mapToObj(ignored -> TaxonomyColorPalette.randomDark(random))
                .toList();

        assertTrue(colors.stream().distinct().count() > 90);
        assertTrue(colors.stream().allMatch(color -> color.matches("#[0-9A-F]{6}")));
        assertTrue(colors.stream().map(color -> Color.decode(color))
                .allMatch(color -> Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue())) <= 113));
    }
}

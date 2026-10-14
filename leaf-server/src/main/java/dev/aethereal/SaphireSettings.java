package dev.aethereal;

public final class SaphireSettings {

    public static boolean disableAdvancements = true;
    public static boolean disableBooks = true;
    public static boolean disableCactusFlowerGrowth = true;
    public static boolean disableLocatorBar = true;
    public static boolean disableFireSpread = true;

    static {
        register();
    }

    private SaphireSettings() {
    }

    public static void register() {
        SaphireConfig.register(SaphireSettings::reload);
    }

    private static void reload() {
        disableAdvancements = SaphireConfig.getBoolean(
            "gameplay.disable-advancements",
            true,
            "Fully disable advancements"
        );
        disableBooks = SaphireConfig.getBoolean(
            "gameplay.disable-books",
            true,
            "Fully disable books"
        );
        disableCactusFlowerGrowth = SaphireConfig.getBoolean(
            "gameplay.disable-cactus-flower-growth",
            true,
            "Fully disable cactus flower growth"
        );
        disableLocatorBar = SaphireConfig.getBoolean(
            "gameplay.disable-locator-bar",
            true,
            "Fully disable minecraft locator bar"
        );
        disableFireSpread = SaphireConfig.getBoolean(
            "gameplay.disable-fire-spreed",
            true,
            "Fully disable fire spreed"
        );
    }
}

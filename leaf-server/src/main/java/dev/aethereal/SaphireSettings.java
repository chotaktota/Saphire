package dev.aethereal;

import java.util.List;

/**
 * Central declaration of Saphrr fork settings.
 *
 * <p>Every option is a plain {@code static} field. Values are resolved once by the static block
 * below (which registers a settings block with {@link SaphireConfig}) and then cached for the whole
 * server lifetime — reading a setting from a tick or packet handler is just a field read.
 *
 * <p>To add a new option: declare a static field with its default, then add one line in
 * {@link #register()} that reads it via the matching accessor. On first start the key is written
 * to {@code config/saphire.yml} together with its comment.
 */
public final class SaphireSettings {

    // ------------------------------------------------------------------
    // Example option: a boolean switch (safe-performance default = true)
    // ------------------------------------------------------------------

    /** Path: {@code performance.optimize-something.enabled} */
    public static boolean OPTIMIZE_SOMETHING = true;

    // ------------------------------------------------------------------
    // Example option: an integer with a bounded range
    // ------------------------------------------------------------------

    /** Path: {@code performance.goal-update-interval} — how often (in ticks) mob goals refresh. */
    public static int GOAL_UPDATE_INTERVAL = 1;

    // ------------------------------------------------------------------
    // Example option: a double (fraction) with descriptive comment
    // ------------------------------------------------------------------

    /** Path: {@code gameplay.mob-spawn-chance} — spawn chance percentage, 0.0 - 100.0. */
    public static double MOB_SPAWN_CHANCE = 100.0;

    // ------------------------------------------------------------------
    // Example option: a string with a fixed set of accepted values
    // ------------------------------------------------------------------

    /** Path: {@code misc.region-format} — one of {@code MCA}, {@code B_LINEAR}, {@code LINEAR_V2}. */
    public static String REGION_FORMAT = "MCA";

    // ------------------------------------------------------------------
    // Example option: an enum (strongly typed)
    // ------------------------------------------------------------------

    public enum RegionFormat { MCA, B_LINEAR, LINEAR_V2 }

    /** Path: {@code misc.region-format-enum} */
    public static RegionFormat REGION_FORMAT_ENUM = RegionFormat.MCA;

    // ------------------------------------------------------------------
    // Example option: a string list
    // ------------------------------------------------------------------

    /** Path: {@code misc.disabled-worlds} */
    public static List<String> DISABLED_WORLDS = List.of("example_world");

    static {
        register();
    }

    private SaphireSettings() {
    }

    /** Wire each static field to its YAML key. Called once; also re-run on every reload. */
    public static void register() {
        SaphireConfig.register(SaphireSettings::reload);
    }

    private static void reload() {
        OPTIMIZE_SOMETHING = SaphireConfig.getBoolean(
            "performance.optimize-something.enabled",
            OPTIMIZE_SOMETHING,
            "Enable the example optimization. Cached at startup.");

        GOAL_UPDATE_INTERVAL = SaphireConfig.clamp(
            "performance.goal-update-interval",
            SaphireConfig.getInt(
                "performance.goal-update-interval",
                GOAL_UPDATE_INTERVAL,
                "How often, in ticks, mob goals are refreshed. 1 = vanilla."),
            1, 100, 1);

        MOB_SPAWN_CHANCE = SaphireConfig.getDouble(
            "gameplay.mob-spawn-chance",
            MOB_SPAWN_CHANCE,
            "Chance (0.0 - 100.0) that a natural spawn attempt succeeds. 100 = vanilla.");

        REGION_FORMAT = SaphireConfig.getString(
            "misc.region-format",
            REGION_FORMAT,
            "Region file format: MCA, B_LINEAR or LINEAR_V2.");

        REGION_FORMAT_ENUM = SaphireConfig.getEnum(
            "misc.region-format-enum",
            RegionFormat.class,
            REGION_FORMAT_ENUM,
            "Region file format (typed). Valid: MCA, B_LINEAR, LINEAR_V2.");

        DISABLED_WORLDS = SaphireConfig.getStringList(
            "misc.disabled-worlds",
            DISABLED_WORLDS,
            "Worlds where fork features are disabled.");
    }
}
package dev.aethereal;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SaphireConfig — lean, self-contained configuration base for the Saphrr fork.
 *
 * <p>Design goals:
 * <ul>
 *   <li><b>Zero classpath scanning</b> — no reflection, no package walking. Settings are declared
 *       explicitly as {@code static} fields and resolved once at load time.</li>
 *   <li><b>Cached reads</b> — every option lives in a plain static primitive/enum field. Hot paths
 *       never touch the YAML layer, so ticking code pays no lookup cost.</li>
 *   <li><b>Single file</b> — everything is stored in {@code config/saphire.yml}.</li>
 *   <li><b>Defaults are self-documenting</b> — on first run the file is generated with comments and
 *       the recommended (safe-performance) defaults, so a fresh server is already tuned.</li>
 * </ul>
 *
 * <p>This class is the base only. Feature modules register their settings blocks through
 * {@link #register(Runnable)} and read their cached values through the typed accessors.
 */
public final class SaphireConfig {

    /** Folder that holds the fork config file. */
    public static final File CONFIG_DIR = new File("config");

    /** Fork config file name. */
    public static final String FILE_NAME = "saphire.yml";

    /** Config schema version. Bump together with an entry in {@link #applyMigrations}. */
    public static final int CONFIG_VERSION = 1;

    private static final Logger LOGGER = Logger.getLogger("Saphire");

    private static final File CONFIG_FILE = new File(CONFIG_DIR, FILE_NAME);

    /** The live YAML-backed file. Only used during load/save, never in hot paths. */
    private static YamlConfiguration yaml;

    /**
     * Registered loaders. Each entry populates its own cached static fields from the loaded file.
     * Add new feature configs here (or via {@link #register}) so they participate in reloads.
     */
    private static final List<Runnable> LOADERS = new ArrayList<>();

    private static boolean loaded = false;

    /**
     * Settings holder classes to initialize on load. Touching each class runs its static initializer,
     * which calls {@link #register(Runnable)}. Add new holders here — explicit, no scanning.
     */
    private static final Class<?>[] HOLDERS = {
        SaphireSettings.class
    };

    private SaphireConfig() {
    }

    // ------------------------------------------------------------------
    // Bootstrap
    // ------------------------------------------------------------------

    /**
     * Register a settings block. Safe to call before {@link #load()}; the loader runs immediately
     * on the next load/reload. Order of registration is preserved.
     */
    public static synchronized void register(final Runnable loader) {
        LOADERS.add(loader);
        if (loaded) {
            // Apply late registrations against the already-parsed file.
            runLoader(loader);
        }
    }

    /**
     * Load (or create) {@code config/saphire.yml}, then resolve every registered settings block
     * into its cached fields. Safe to call again to reload.
     */
    public static synchronized void load() {
        final long begin = System.nanoTime();
        try {
            ensureDirectory();

            final boolean firstRun = !CONFIG_FILE.isFile();
            yaml = YamlConfiguration.loadConfiguration(CONFIG_FILE);
            yaml.options().setHeader(header());

            if (firstRun) {
                LOGGER.info("Creating default configuration " + CONFIG_FILE.getPath());
            }

            applyMigrations();

            // A throwaway view of the defaults so newly added keys get sensible values on save.
            yaml.set("config-version", CONFIG_VERSION);

            initHolders();

            for (final Runnable loader : LOADERS) {
                runLoader(loader);
            }

            loaded = true;
            save();

            if (firstRun) {
                LOGGER.info("Saphire configuration generated. Review " + CONFIG_FILE.getPath()
                    + " and restart to apply tuning.");
            } else {
                LOGGER.info("Loaded Saphire configuration in " + ((System.nanoTime() - begin) / 1_000_000L) + "ms.");
            }
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load Saphire configuration", e);
        }
    }

    /** Persist the current file to disk. Never throws — logs on failure. */
    public static synchronized void save() {
        try {
            ensureDirectory();
            yaml.save(CONFIG_FILE);
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save Saphire configuration", e);
        }
    }

    /** The backing {@link YamlConfiguration}. Intended for module loaders only. */
    public static YamlConfiguration raw() {
        return yaml;
    }

    private static void runLoader(final Runnable loader) {
        try {
            loader.run();
        } catch (final RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to apply a Saphire settings block", e);
        }
    }

    /** Force each settings holder to initialize so its static block registers its loaders. */
    private static void initHolders() {
        for (final Class<?> holder : HOLDERS) {
            try {
                Class.forName(holder.getName(), true, holder.getClassLoader());
            } catch (final Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to initialize settings holder " + holder.getName(), t);
            }
        }
    }

    private static void ensureDirectory() throws IOException {
        if (!CONFIG_DIR.isDirectory() && !CONFIG_DIR.mkdirs()) {
            throw new IOException("Could not create config directory: " + CONFIG_DIR.getAbsolutePath());
        }
    }

    // ------------------------------------------------------------------
    // Migrations / header
    // ------------------------------------------------------------------

    /**
     * Schema migrations. Each block upgrades the on-disk file from an older {@code config-version}
     * to the current one. Extend as the schema evolves.
     */
    private static void applyMigrations() {
        final int from = yaml.getInt("config-version", CONFIG_VERSION);
        if (from >= CONFIG_VERSION) {
            return;
        }
        // Example for future use:
        // if (from < 2) { yaml.set("performance.old-key", null); }
    }

    private static List<String> header() {
        final List<String> lines = new ArrayList<>();
        lines.add("Saphire configuration");
        lines.add("");
        lines.add("This file controls fork-specific behaviour and performance tuning.");
        lines.add("Defaults are chosen to be safe and fast out of the box. Edit with care —");
        lines.add("most options are read once at startup and cached, so a restart is required");
        lines.add("to apply changes.");
        lines.add("");
        lines.add("Docs: (add your fork documentation link here)");
        return lines;
    }

    // ------------------------------------------------------------------
    // Typed accessors used by module loaders
    //
    // Each of these writes the default into the file if the key is missing, then returns the
    // resolved value. Modules assign the result straight into their cached static fields.
    // ------------------------------------------------------------------

    public static boolean getBoolean(final String path, final boolean def, final String comment) {
        comment(path, comment);
        return defineDefault(path, def);
    }

    public static boolean getBoolean(final String path, final boolean def) {
        return defineDefault(path, def);
    }

    public static int getInt(final String path, final int def, final String comment) {
        comment(path, comment);
        return defineDefault(path, def);
    }

    public static int getInt(final String path, final int def) {
        return defineDefault(path, def);
    }

    public static long getLong(final String path, final long def, final String comment) {
        comment(path, comment);
        return defineDefault(path, def);
    }

    public static double getDouble(final String path, final double def, final String comment) {
        comment(path, comment);
        return defineDefault(path, def);
    }

    public static String getString(final String path, final String def, final String comment) {
        comment(path, comment);
        return defineDefault(path, def);
    }

    public static String getString(final String path, final String def) {
        return defineDefault(path, def);
    }

    public static List<String> getStringList(final String path, final List<String> def, final String comment) {
        comment(path, comment);
        final Object existing = yaml.get(path);
        if (existing instanceof List<?> list) {
            final List<String> out = new ArrayList<>(list.size());
            for (final Object element : list) {
                out.add(String.valueOf(element));
            }
            return out;
        }
        yaml.set(path, def);
        return def;
    }

    /**
     * Resolve an enum value by name, falling back to {@code def} (and logging) on an unknown name.
     * Written back into the file when missing so the valid options are discoverable.
     */
    public static <E extends Enum<E>> E getEnum(final String path, final Class<E> type, final E def, final String comment) {
        comment(path, comment);
        final String name = defineDefault(path, def.name());
        for (final E candidate : type.getEnumConstants()) {
            if (candidate.name().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        LOGGER.warning("Unknown value '" + name + "' for " + path + ", falling back to " + def.name());
        return def;
    }

    /**
     * Read a typed value, writing the default into the file first when the key is absent.
     * This keeps the generated {@code saphire.yml} fully populated and self-documenting.
     */
    @SuppressWarnings("unchecked")
    private static <T> T defineDefault(final String path, final T def) {
        if (!yaml.isSet(path)) {
            yaml.set(path, def);
            return def;
        }
        final Object value = yaml.get(path);
        if (def instanceof Boolean && value instanceof Boolean) {
            return (T) value;
        }
        if (def instanceof Integer && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        if (def instanceof Long && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        }
        if (def instanceof Double && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        }
        if (def instanceof String) {
            return (T) String.valueOf(value);
        }
        return (T) value;
    }

    /**
     * Clamp a numeric setting into an inclusive range, warning when the value is out of bounds.
     */
    public static int clamp(final String path, final int value, final int min, final int max, final int fallback) {
        if (value < min || value > max) {
            LOGGER.warning(path + " = " + value + " is out of range [" + min + ", " + max + "]; using " + fallback);
            return fallback;
        }
        return value;
    }

    /** Attach a block comment to a config path. No-op when {@code comment} is null/blank. */
    public static void comment(final String path, final String comment) {
        if (comment == null || comment.isEmpty()) {
            return;
        }
        final List<String> lines = new ArrayList<>();
        for (final String line : comment.split("\n", -1)) {
            lines.add(line);
        }
        yaml.setComments(path, lines);
    }
}
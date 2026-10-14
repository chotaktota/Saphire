package dev.aethereal;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
public final class SaphireConfig {

    public static final File CONFIG_DIR = new File("config");

    public static final String FILE_NAME = "saphire.yml";

    public static final int CONFIG_VERSION = 1;

    private static final Logger LOGGER = Logger.getLogger("Saphire");

    private static final File CONFIG_FILE = new File(CONFIG_DIR, FILE_NAME);

    private static YamlConfiguration yaml;

    private static final List<Runnable> LOADERS = new ArrayList<>();

    private static boolean loaded = false;

    private static final Class<?>[] HOLDERS = {
        SaphireSettings.class
    };

    private SaphireConfig() {
    }

    public static synchronized void register(final Runnable loader) {
        LOADERS.add(loader);
        if (loaded) {
            runLoader(loader);
        }
    }

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

    public static synchronized void save() {
        try {
            ensureDirectory();
            yaml.save(CONFIG_FILE);
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save Saphire configuration", e);
        }
    }

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

    private static void applyMigrations() {
        final int from = yaml.getInt("config-version", CONFIG_VERSION);
        if (from >= CONFIG_VERSION) {
            return;
        }
    }

    private static List<String> header() {
        final List<String> lines = new ArrayList<>();
        lines.add("Saphire configuration");
        lines.add("");
        return lines;
    }

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

    public static int clamp(final String path, final int value, final int min, final int max, final int fallback) {
        if (value < min || value > max) {
            LOGGER.warning(path + " = " + value + " is out of range [" + min + ", " + max + "]; using " + fallback);
            return fallback;
        }
        return value;
    }

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

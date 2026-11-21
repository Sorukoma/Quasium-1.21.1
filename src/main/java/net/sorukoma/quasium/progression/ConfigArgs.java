package net.sorukoma.quasium.progression;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.sorukoma.quasium.Config;
import net.sorukoma.quasium.ecology.EcologyConfig;
import net.sorukoma.quasium.magic.page.PageConfig;
import net.sorukoma.quasium.mind.MindConfig;
import net.sorukoma.quasium.ritual.RitualConfig;
import net.sorukoma.quasium.selection.SelectionConfig;
import net.sorukoma.quasium.smokelands.SmokelandsConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ConfigArgs {
    public static final List<Class<?>> SOURCES = List.of(Config.class, SelectionConfig.class, EcologyConfig.class, MindConfig.class,
            RitualConfig.class, SmokelandsConfig.class, PageConfig.class);
    private static final String MISSING = "?";
    private static final Map<ModConfigSpec.ConfigValue<?>, String> NAMES = new ConcurrentHashMap<>();
    private static volatile Map<String, String> serverValues = Map.of();

    private ConfigArgs() {
    }

    public static boolean exists(String name) {
        return field(name).isPresent();
    }

    public static String format(String name) {
        String fromServer = serverValues.get(name);
        return fromServer != null ? fromServer : field(name).map(ConfigArgs::printField).orElse(MISSING);
    }

    public static int synced(ModConfigSpec.IntValue value) {
        String fromServer = serverValues.get(NAMES.computeIfAbsent(value, ConfigArgs::nameOf));
        return fromServer != null ? Integer.parseInt(fromServer) : value.get();
    }

    public static double synced(ModConfigSpec.DoubleValue value) {
        String fromServer = serverValues.get(NAMES.computeIfAbsent(value, ConfigArgs::nameOf));
        return fromServer != null ? Double.parseDouble(fromServer) : value.get();
    }

    public static boolean synced(ModConfigSpec.BooleanValue value) {
        String fromServer = serverValues.get(NAMES.computeIfAbsent(value, ConfigArgs::nameOf));
        return fromServer != null ? Boolean.parseBoolean(fromServer) : value.get();
    }

    public static List<Integer> synced(ModConfigSpec.ConfigValue<List<? extends Integer>> value) {
        String fromServer = serverValues.get(NAMES.computeIfAbsent(value, ConfigArgs::nameOf));
        if (fromServer == null) return List.copyOf(value.get());
        return fromServer.isEmpty() ? List.of() : Arrays.stream(fromServer.split("/")).map(Integer::valueOf).toList();
    }

    public static Map<String, String> localValues() {
        return SOURCES.stream().flatMap(source -> Arrays.stream(source.getFields())).filter(ConfigArgs::isConfigValue)
                .collect(Collectors.toMap(Field::getName, ConfigArgs::printField, (first, second) -> first));
    }

    public static void useServerValues(Map<String, String> values) {
        serverValues = Map.copyOf(values);
    }

    public static List<String> names(Class<?> source) {
        return Arrays.stream(source.getFields()).filter(ConfigArgs::isConfigValue).map(Field::getName).toList();
    }

    static String print(Object value) {
        if (value instanceof List<?> list) return list.stream().map(ConfigArgs::print).collect(Collectors.joining("/"));
        if (value instanceof Double number && number == Math.rint(number)) return String.valueOf(number.longValue());
        return String.valueOf(value);
    }

    private static String printField(Field field) {
        return print(read(field).get());
    }

    private static String nameOf(ModConfigSpec.ConfigValue<?> value) {
        return SOURCES.stream().flatMap(source -> Arrays.stream(source.getFields())).filter(ConfigArgs::isConfigValue)
                .filter(field -> read(field) == value).map(Field::getName).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(value.getPath() + " is not a public field of " + SOURCES));
    }

    private static Optional<Field> field(String name) {
        return SOURCES.stream().flatMap(source -> Arrays.stream(source.getFields()))
                .filter(field -> field.getName().equals(name) && isConfigValue(field)).findFirst();
    }

    private static boolean isConfigValue(Field field) {
        return Modifier.isStatic(field.getModifiers()) && ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType());
    }

    private static ModConfigSpec.ConfigValue<?> read(Field field) {
        try {
            return (ModConfigSpec.ConfigValue<?>) field.get(null);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(field + " is a public config value and must be readable", exception);
        }
    }
}

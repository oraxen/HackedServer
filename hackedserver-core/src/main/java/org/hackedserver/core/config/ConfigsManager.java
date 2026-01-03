package org.hackedserver.core.config;

import org.hackedserver.core.HackedServer;
import org.hackedserver.core.exceptions.ParsingException;
import org.hackedserver.core.forge.ForgeConfig;
import org.jetbrains.annotations.NotNull;
import org.tomlj.Toml;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigsManager {

    private static final ClassLoader classLoader = ConfigsManager.class.getClassLoader();

    public static void init(File folder) {
        loadConfigs(folder);
    }

    public static void reload(File folder) {
        HackedServer.clear();
        loadConfigs(folder);
    }

    private static void loadConfigs(File folder) {
        folder.mkdirs();
        try {
            Config.setParseResult(getConfig("config.toml", new File(folder, "config.toml")));
            String langFile = Config.LANG_FILE.getStringOrDefault("english");
            Message.setFallbackParseResult(loadFallbackMessages(langFile));
            Message.setParseResult(getConfig("languages/" + langFile + ".toml",
                    new File(new File(folder, "languages"), langFile + ".toml")));
            loadActions(Objects.requireNonNull(
                    getConfig("actions.toml", new File(folder, "actions.toml"))));
            loadGenericChecks(Objects.requireNonNull(
                    getConfig("generic.toml", new File(folder, "generic.toml"))));
            LunarConfig.load(getConfig("lunar.toml", new File(folder, "lunar.toml")));
            ForgeConfig.load(getConfig("forge.toml", new File(folder, "forge.toml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static InputStream getResource(@NotNull String name) throws IOException {
        URL url = classLoader.getResource(name);
        if (url == null)
            return null;
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    public static TomlParseResult getConfig(@NotNull String name, File target) throws IOException {
        try {
            if (!target.exists()) {
                target.getParentFile().mkdirs();
                java.nio.file.Files.copy(
                        Objects.requireNonNull(getResource(name)),
                        target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            TomlParseResult result = Toml.parse(Path.of(target.toURI()));
            for (TomlParseError error : result.errors())
                throw new ParsingException(error.toString());
            return result;
        } catch (IOException | ParsingException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static TomlParseResult loadFallbackMessages(String langFile) {
        if (langFile == null || langFile.isBlank()) {
            langFile = "english";
        }
        TomlParseResult fallback = getResourceConfig("languages/" + langFile + ".toml");
        if (fallback == null && !"english".equalsIgnoreCase(langFile)) {
            fallback = getResourceConfig("languages/english.toml");
        }
        return fallback;
    }

    private static TomlParseResult getResourceConfig(@NotNull String name) {
        try (InputStream input = getResource(name)) {
            if (input == null)
                return null;
            TomlParseResult result = Toml.parse(input);
            for (TomlParseError error : result.errors())
                throw new ParsingException(error.toString());
            return result;
        } catch (IOException | ParsingException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadActions(@NotNull TomlParseResult result) {
        for (String key : result.keySet()) {
            TomlTable table = result.getTable(key);
            assert table != null;
            Action action = new Action(key);
            if (table.isString("send_alert"))
                action.setAlert(table.getString("send_alert"));
            if (table.isLong("delay_ticks"))
                action.setDelayTicks(table.getLong("delay_ticks"));
            if (table.isTable("commands")) {
                TomlTable commandsTable = table.getTable("commands");
                assert commandsTable != null;
                if (commandsTable.isArray("console"))
                    action.setConsoleCommands(
                            (List<String>) (Object) Objects.requireNonNull(commandsTable.getArray("console")).toList());
                if (commandsTable.isArray("player"))
                    action.setPlayerCommands(
                            (List<String>) (Object) Objects.requireNonNull(commandsTable.getArray("player")).toList());
                if (commandsTable.isArray("opped_player"))
                    action.setOppedPlayerCommands((List<String>) (Object) Objects
                            .requireNonNull(commandsTable.getArray("opped_player")).toList());
            }
            HackedServer.registerAction(action);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadGenericChecks(@NotNull TomlParseResult result) {
        if (!Boolean.TRUE.equals(result.getBoolean("enabled")))
            return;

        for (String key : result.keySet()) {
            if (!result.isTable(key))
                continue;
            TomlTable table = result.getTable(key);
            assert table != null;
            List<Action> actions = new ArrayList<>();
            for (Object actionName : Objects.requireNonNull(
                    Objects.requireNonNull(table.getArray("actions")).toList())) {
                Action action = HackedServer.getAction((String) actionName);
                if (action != null)
                    actions.add(action);
            }
            HackedServer.registerCheck(new GenericCheck(key,
                    table.getString("name"),
                    (List<String>) (Object) table.getArray("channels").toList(),
                    table.getString("message_has"),
                    actions));
        }
    }

}

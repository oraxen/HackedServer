package org.hackedserver.core.config;

import org.hackedserver.core.HackedServer;
import org.hackedserver.core.exceptions.ExceptionHandler;
import org.hackedserver.core.exceptions.ParsingException;
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
import java.util.logging.Logger;

public class ConfigsManager {

    private static final ClassLoader classLoader = ConfigsManager.class.getClassLoader();
    private static Logger logger;

    public static void init(Logger logger, File folder) {
        ConfigsManager.logger = logger;
        folder.mkdirs();
        try {
            Config.setParseResult(getConfig("config.toml", new File(folder, "config.toml")));
            Message.setParseResult(getConfig("languages/" + Config.LANG_FILE + ".toml",
                    new File(new File(folder, "languages"), Config.LANG_FILE + ".toml")));
            loadActions(Objects.requireNonNull(
                    getConfig("actions.toml", new File(folder, "actions.toml"))));
            loadGenericChecks(Objects.requireNonNull(
                    getConfig("generic.toml", new File(folder, "generic.toml"))));
            getConfig("actions.toml", new File(folder, "actions.toml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reload(Logger logger, File folder) {
        ConfigsManager.logger = logger;
        HackedServer.clear();
        folder.mkdirs();
        try {
            Config.setParseResult(getConfig("config.toml", new File(folder, "config.toml")));
            Message.setParseResult(getConfig("languages/" + Config.LANG_FILE + ".toml",
                    new File(new File(folder, "languages"), Config.LANG_FILE + ".toml")));
            loadActions(Objects.requireNonNull(
                    getConfig("actions.toml", new File(folder, "actions.toml"))));
            loadGenericChecks(Objects.requireNonNull(
                    getConfig("generic.toml", new File(folder, "generic.toml"))));
            getConfig("actions.toml", new File(folder, "actions.toml"));
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
            new ExceptionHandler(exception).fire(logger);
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

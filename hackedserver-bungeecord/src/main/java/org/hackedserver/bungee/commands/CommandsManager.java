package org.hackedserver.bungee.commands;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.hackedserver.bungee.HackedServerPlugin;
import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.hackedserver.core.config.ConfigsManager;
import org.hackedserver.core.config.Message;

import java.io.File;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CommandsManager extends Command {

    private final ProxyServer server;
    private final Logger logger;
    private final File dataFolder;

    public CommandsManager(ProxyServer server, Logger logger, File dataFolder) {
        super("hackedserver", "hackedserver.command", "hs");
        this.server = server;
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Audience audience = HackedServerPlugin.get().getAudiences().sender(sender);
        if (args.length == 0) {
            Message.COMMANDS_HELP.send(audience);
            return;
        }

        switch (args[0]) {
            case "reload" -> {
                ConfigsManager.reload(dataFolder);
                Message.COMMANDS_RELOAD_SUCCESS.send(audience);
                server.getPlayers().forEach(player -> HackedServer.registerPlayer(player.getUniqueId()));
            }
            case "check" -> {
                try {
                    ProxiedPlayer player = server.getPlayer(args[1]);
                    HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
                    if (hackedPlayer.getGenericChecks().isEmpty())
                        Message.CHECK_NO_MODS.send(audience);
                    else {
                        Message.CHECK_MODS.send(audience);
                        for (String checkId : hackedPlayer.getGenericChecks()) {
                            Message.MOD_LIST_FORMAT.send(audience,
                                    Placeholder.parsed("mod", HackedServer.getCheck(checkId).getName()));
                        }
                    }
                } catch (Exception exception) {

                }
            }
            case "list" -> {
                var playersWithChecks = HackedServer.getPlayers().stream()
                        .filter(player -> !player.getGenericChecks().isEmpty())
                        .collect(Collectors.toList());

                if (playersWithChecks.isEmpty()) {
                    Message.CHECK_PLAYERS_EMPTY.send(audience);
                    return;
                }

                Message.CHECK_PLAYERS.send(audience);
                playersWithChecks.forEach(hackedPlayer -> {
                    Message.PLAYER_LIST_FORMAT.send(audience,
                            Placeholder.parsed("player",
                                    server.getPlayer(hackedPlayer.getUuid()).getName()));
                });
            }
            default -> {
            }
        }
    }
}

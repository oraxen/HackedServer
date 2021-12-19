package org.hackedserver.bungee.commands;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.Template;
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
            case "reload":
                ConfigsManager.reload(logger, dataFolder);
                Message.COMMANDS_RELOAD_SUCCESS.send(audience);
                break;

            case "check":
                try {
                    ProxiedPlayer player = server.getPlayer(args[1]);
                    HackedPlayer hackedPlayer = HackedServer.getPlayer(player.getUniqueId());
                    if (hackedPlayer.getGenericChecks().isEmpty())
                        Message.CHECK_NO_MODS.send(audience);
                    else {
                        Message.CHECK_MODS.send(audience);
                        for (String checkId : hackedPlayer.getGenericChecks()) {
                            Message.MOD_LIST_FORMAT.send(audience,
                                    Template.of("mod", HackedServer.getCheck(checkId).getName()));
                        }
                    }
                } catch (Exception exception) {

                }
                break;

            default:
                break;
        }
    }
}

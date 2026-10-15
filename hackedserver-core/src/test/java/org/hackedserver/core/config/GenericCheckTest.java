package org.hackedserver.core.config;

import org.hackedserver.core.HackedPlayer;
import org.hackedserver.core.HackedServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tomlj.Toml;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericCheckTest {

    private final HackedPlayer player = new HackedPlayer(UUID.randomUUID());

    @AfterEach
    void reset() {
        Config.setParseResult(null);
        HackedServer.clear();
    }

    private static GenericCheck check(String id, String has, String notHas, String... channels) {
        return new GenericCheck(id, id, List.of(channels), has, notHas, "loader", List.of());
    }

    @Test
    void matchesChannelAndMessageCaseInsensitively() {
        GenericCheck fabric = check("fabric", "fabric", null, "MC|Brand", "minecraft:brand");
        assertTrue(fabric.pass(player, "minecraft:brand", "\u0006Fabric"));
        assertFalse(fabric.pass(player, "minecraft:register", "fabric"));
        assertFalse(fabric.pass(player, "minecraft:brand", "\u0007vanilla"));
    }

    @Test
    void honoursMessageNotHas() {
        GenericCheck forge = check("forge", "forge", "neoforge", "minecraft:brand");
        assertTrue(forge.pass(player, "minecraft:brand", "forge"));
        assertFalse(forge.pass(player, "minecraft:brand", "neoforge"));
    }

    @Test
    void toleratesNullChannelAndMessage() {
        GenericCheck any = check("labymod", null, null, "LMC");
        assertFalse(any.pass(player, null, null));
        assertTrue(any.pass(player, "LMC", null));
        assertFalse(check("wdl", "wdl", null, "minecraft:register").pass(player, "minecraft:register", null));
    }

    @Test
    void duplicatesAreScopedPerCheck() {
        Config.setParseResult(Toml.parse("[settings]\nskip_duplicates = true\n"));
        String register = "wdl:init\0meteor-client:main";
        GenericCheck wdl = check("wdl", "wdl", null, "minecraft:register");
        GenericCheck meteor = check("meteor", "meteor-client", null, "minecraft:register");

        // One payload matching two checks must trigger both
        assertTrue(wdl.pass(player, "minecraft:register", register));
        assertTrue(meteor.pass(player, "minecraft:register", register));
        // The same payload sent again is a duplicate for each check
        assertFalse(wdl.pass(player, "minecraft:register", register));
        assertFalse(meteor.pass(player, "minecraft:register", register));
    }

    @Test
    void configDefaultsWhenNotLoaded() {
        Config.setParseResult(null);
        assertTrue(Config.AUTO_DOWNLOAD_DEPENDENCIES.toBool(true));
        assertFalse(Config.DEBUG.toBool());
        assertTrue(Config.ACTION_DELAY_TICKS.toLong(20L) == 20L);
    }
}

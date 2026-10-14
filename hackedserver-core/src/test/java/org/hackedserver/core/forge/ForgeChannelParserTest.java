package org.hackedserver.core.forge;

import org.hackedserver.core.HackedPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeChannelParserTest {

    @AfterEach
    void reset() {
        ForgeConfig.load(null);
    }

    @Test
    void parsesClientTypeFromBrandPayload() {
        // minecraft:brand payloads are VarInt length-prefixed strings
        assertEquals(ForgeClientType.NEOFORGE, ForgeChannelParser.parseClientType("\u0008neoforge"));
        assertEquals(ForgeClientType.FORGE, ForgeChannelParser.parseClientType("\u0005forge"));
        assertEquals(ForgeClientType.FORGE, ForgeChannelParser.parseClientType("fml,forge"));
        assertNull(ForgeChannelParser.parseClientType("\u0006fabric"));
        assertNull(ForgeChannelParser.parseClientType(null));
    }

    @Test
    void recognisesVanillaBrandWithLengthPrefix() {
        assertTrue(ForgeChannelParser.isVanillaBrand("\u0007vanilla"));
        assertTrue(ForgeChannelParser.isVanillaBrand("minecraft"));
        assertFalse(ForgeChannelParser.isVanillaBrand("\u0006fabric"));
        assertFalse(ForgeChannelParser.isVanillaBrand(null));
    }

    @Test
    void extractsModNamespacesFromRegisterPayload() {
        String payload = "minecraft:debug\0jei:main\0fabric-screen-handler-api-v1:open_screen\0"
                + "fabric:registry/sync\0jei:other\0fabrication:x\0c:tags\0meteor-client:main";
        List<String> ids = ForgeChannelParser.parseRegisteredChannels(payload).stream()
                .map(ForgeModInfo::getModId)
                .collect(Collectors.toList());
        assertEquals(List.of("jei", "fabrication", "meteor-client"), ids);
    }

    @Test
    void acceptsSpaceSeparatedDebugFormat() {
        List<ForgeModInfo> mods = ForgeChannelParser.parseRegisteredChannels("xaero:main wdl:init");
        assertEquals(2, mods.size());
        assertTrue(ForgeChannelParser.parseRegisteredChannels("").isEmpty());
        assertTrue(ForgeChannelParser.parseRegisteredChannels((String) null).isEmpty());
    }

    @Test
    void detectsFabricChannels() {
        assertTrue(ForgeChannelParser.containsFabricChannels("jei:main\0fabric-networking-api-v1:early"));
        assertTrue(ForgeChannelParser.containsFabricChannels("fabric:registry/sync"));
        assertFalse(ForgeChannelParser.containsFabricChannels("fabrication:x\0jei:main"));
    }

    @Test
    void flagsVanillaBrandWithFabricChannelsOnce() {
        ForgeConfig.load(null); // defaults: spoofing detection enabled
        HackedPlayer player = new HackedPlayer(UUID.randomUUID());
        player.setBrand("\u0007vanilla");
        assertFalse(ForgeSpoofingDetector.detect(player));

        player.setFabricChannelsDetected(true);
        assertTrue(ForgeSpoofingDetector.detect(player));
        assertFalse(ForgeSpoofingDetector.detect(player), "must only trigger once per player");
        assertTrue(player.hasGenericCheck(ForgeSpoofingDetector.CHECK_ID));
    }
}

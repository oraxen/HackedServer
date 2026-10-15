package org.hackedserver.core.lunar;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import com.lunarclient.apollo.player.v1.ModMessage;
import com.lunarclient.apollo.player.v1.PlayerHandshakeMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LunarApolloHandshakeParserTest {

    @Test
    void parsesInstalledMods() {
        PlayerHandshakeMessage handshake = PlayerHandshakeMessage.newBuilder()
                .addInstalledMods(ModMessage.newBuilder().setId("sodium").setName("Sodium").setVersion("0.6")
                        .setType(ModMessage.Type.TYPE_FABRIC_EXTERNAL))
                .addInstalledMods(ModMessage.newBuilder().setId("").setName("blank id is skipped"))
                .addInstalledMods(ModMessage.newBuilder().setId("jei").setType(ModMessage.Type.TYPE_FORGE_EXTERNAL))
                .build();

        Optional<List<LunarModInfo>> mods = LunarApolloHandshakeParser.parseMods(Any.pack(handshake).toByteArray());

        assertTrue(mods.isPresent());
        assertEquals(2, mods.get().size());
        LunarModInfo sodium = mods.get().get(0);
        assertEquals("sodium", sodium.getId());
        assertEquals("Sodium", sodium.getDisplayName());
        assertEquals("0.6", sodium.getVersion());
        assertTrue(sodium.isFabric());
        assertTrue(mods.get().get(1).isForge());
    }

    @Test
    void emptyHandshakeYieldsEmptyModList() {
        byte[] payload = Any.pack(PlayerHandshakeMessage.getDefaultInstance()).toByteArray();
        assertEquals(Optional.of(List.of()), LunarApolloHandshakeParser.parseMods(payload));
    }

    @Test
    void ignoresOtherMessagesAndGarbage() {
        assertFalse(LunarApolloHandshakeParser.parseMods(Any.pack(StringValue.of("x")).toByteArray()).isPresent());
        assertFalse(LunarApolloHandshakeParser.parseMods(new byte[]{(byte) 0xFF, 0x01, 0x02}).isPresent());
        assertFalse(LunarApolloHandshakeParser.parseMods(new byte[0]).isPresent());
        assertFalse(LunarApolloHandshakeParser.parseMods(null).isPresent());
    }
}

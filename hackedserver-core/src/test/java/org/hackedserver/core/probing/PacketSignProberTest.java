package org.hackedserver.core.probing;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PacketSignProberTest {

    private static final TranslationCheck METEOR = new TranslationCheck("meteor_client", "Meteor Client",
            "key.category.meteor-client.meteor-client", true, List.of());
    private static final TranslationCheck WURST = new TranslationCheck("wurst", "Wurst Client",
            "key.wurst.zoom", false, List.of());

    @Test
    void legacyClientsGetJsonStrings() {
        NBTCompound sign = PacketSignProber.buildSignNBT(List.of(METEOR, WURST), false);
        NBTList<?> messages = (NBTList<?>) sign.getCompoundTagOrNull("front_text").getTagOrNull("messages");

        assertEquals(NBTType.STRING, messages.getTagsType());
        assertEquals(4, messages.size());
        assertEquals("{\"translate\":\"%s\",\"with\":[{\"translate\":\"key.category.meteor-client.meteor-client\"}]}",
                ((NBTString) messages.getTag(0)).getValue());
        assertEquals("{\"translate\":\"key.wurst.zoom\"}", ((NBTString) messages.getTag(1)).getValue());
        assertEquals("{\"text\":\"\"}", ((NBTString) messages.getTag(3)).getValue());
    }

    @Test
    void modernClientsGetStructuredComponents() {
        NBTCompound sign = PacketSignProber.buildSignNBT(List.of(METEOR, WURST), true);
        NBTList<?> messages = (NBTList<?>) sign.getCompoundTagOrNull("front_text").getTagOrNull("messages");

        assertEquals(NBTType.COMPOUND, messages.getTagsType());
        assertEquals(4, messages.size());

        NBTCompound bypass = (NBTCompound) messages.getTag(0);
        assertEquals("%s", bypass.getStringTagValueOrNull("translate"));
        NBTCompound inner = (NBTCompound) ((NBTList<?>) bypass.getTagOrNull("with")).getTag(0);
        assertEquals("key.category.meteor-client.meteor-client", inner.getStringTagValueOrNull("translate"));

        NBTCompound plain = (NBTCompound) messages.getTag(1);
        assertEquals("key.wurst.zoom", plain.getStringTagValueOrNull("translate"));
        assertNull(plain.getTagOrNull("with"));

        assertEquals("", ((NBTCompound) messages.getTag(2)).getStringTagValueOrNull("text"));
        NBTList<?> back = (NBTList<?>) sign.getCompoundTagOrNull("back_text").getTagOrNull("messages");
        assertEquals(NBTType.COMPOUND, back.getTagsType());
        assertEquals(4, back.size());
    }

    @Test
    void escapesTranslationKeysInJson() {
        TranslationCheck quoted = new TranslationCheck("q", "Q", "a\"b\\c", false, List.of());
        assertEquals("{\"translate\":\"a\\\"b\\\\c\"}", PacketSignProber.translatableJson(quoted));
    }
}

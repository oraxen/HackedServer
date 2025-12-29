package org.hackedserver.core.lunar;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.lunarclient.apollo.player.v1.ModMessage;
import com.lunarclient.apollo.player.v1.PlayerHandshakeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LunarApolloHandshakeParser {

    public static final String CHANNEL = "lunar:apollo";

    private LunarApolloHandshakeParser() {
    }

    public static Optional<List<LunarModInfo>> parseMods(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return Optional.empty();
        }

        Any any;
        try {
            any = Any.parseFrom(payload);
        } catch (InvalidProtocolBufferException ex) {
            return Optional.empty();
        }

        if (!any.is(PlayerHandshakeMessage.class)) {
            return Optional.empty();
        }

        PlayerHandshakeMessage message;
        try {
            message = any.unpack(PlayerHandshakeMessage.class);
        } catch (InvalidProtocolBufferException ex) {
            return Optional.empty();
        }

        List<LunarModInfo> mods = new ArrayList<>();
        for (ModMessage mod : message.getInstalledModsList()) {
            if (mod == null || mod.getId() == null || mod.getId().isBlank()) {
                continue;
            }
            String type = mod.getType() != null ? mod.getType().name() : null;
            mods.add(new LunarModInfo(mod.getId(), mod.getName(), mod.getVersion(), type));
        }

        return Optional.of(mods);
    }
}

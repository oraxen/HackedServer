package org.hackedserver.spigot.protocol;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import org.hackedserver.core.config.Config;
import org.hackedserver.spigot.probing.SignTranslationProber;
import org.hackedserver.spigot.utils.logs.Logs;

import java.util.UUID;

/**
 * Consumes probe sign replies (UPDATE_SIGN) at the packet level so they never reach the server.
 * Kept separate from {@link SignTranslationProber} so the Bukkit listener has no PacketEvents types.
 */
final class PacketEventsSignListener extends PacketListenerAbstract {

    private final SignTranslationProber prober;

    PacketEventsSignListener(SignTranslationProber prober) {
        super(PacketListenerPriority.LOW);
        this.prober = prober;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) {
            return;
        }
        UUID uuid = event.getUser().getUUID();
        if (uuid == null) {
            return;
        }
        WrapperPlayClientUpdateSign updateSign = new WrapperPlayClientUpdateSign(event);
        if (Config.DEBUG.toBool()) {
            Logs.logInfo("HackedServer | Received UPDATE_SIGN packet from " + uuid + " lines: [\""
                    + String.join("\", \"", updateSign.getTextLines()) + "\"]");
        }
        Vector3i pos = updateSign.getBlockPosition();
        if (prober.handleResponse(uuid, pos.x, pos.y, pos.z, updateSign.getTextLines())) {
            event.setCancelled(true);
        }
    }
}

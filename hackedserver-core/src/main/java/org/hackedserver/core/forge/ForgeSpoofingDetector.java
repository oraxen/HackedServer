package org.hackedserver.core.forge;

import org.hackedserver.core.HackedPlayer;

public final class ForgeSpoofingDetector {

    public static final String CHECK_ID = "spoofed_brand";
    public static final String CHECK_NAME = "Spoofed Brand (Fabric)";

    private ForgeSpoofingDetector() {
    }

    public static boolean detect(HackedPlayer hackedPlayer) {
        if (!ForgeConfig.isSpoofingDetectionEnabled()
                || hackedPlayer.hasGenericCheck(CHECK_ID)
                || !ForgeChannelParser.isVanillaBrand(hackedPlayer.getBrand())
                || !hackedPlayer.hasFabricChannelsDetected()) {
            return false;
        }

        hackedPlayer.addGenericCheck(CHECK_ID);
        return true;
    }
}

package org.hackedserver.core.bedrock;

import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/**
 * Bedrock detection using the Floodgate API.
 * This class is only loaded when Floodgate is present on the classpath.
 */
final class FloodgateProvider implements BedrockProvider {

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String getSourceName() {
        return "Floodgate";
    }
}

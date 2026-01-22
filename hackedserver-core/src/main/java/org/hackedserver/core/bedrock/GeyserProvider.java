package org.hackedserver.core.bedrock;

import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

/**
 * Bedrock detection using the Geyser API.
 * This class is only loaded when Geyser is present on the classpath.
 */
final class GeyserProvider implements BedrockProvider {

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        try {
            return GeyserApi.api().isBedrockPlayer(uuid);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String getSourceName() {
        return "Geyser";
    }
}

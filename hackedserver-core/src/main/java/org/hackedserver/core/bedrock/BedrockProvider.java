package org.hackedserver.core.bedrock;

import java.util.UUID;

/**
 * Interface for bedrock player detection providers.
 * Implementations wrap specific APIs (Geyser, Floodgate).
 */
interface BedrockProvider {

    /**
     * Check if the player with the given UUID is a Bedrock player.
     * @param uuid the player's UUID
     * @return true if the player is a Bedrock player
     */
    boolean isBedrockPlayer(UUID uuid);

    /**
     * Get the source name for this provider (e.g., "Geyser", "Floodgate").
     * @return the source name
     */
    String getSourceName();
}

package indi.etern.musichud.bukkit.beans;

import java.util.UUID;

/**
 * Information about who pushed a music track to the queue.
 */
public record PusherInfo(long uid, UUID playerUUID, String playerName) {
    public static final PusherInfo EMPTY = new PusherInfo(0L, new UUID(0L, 0L), "");
}

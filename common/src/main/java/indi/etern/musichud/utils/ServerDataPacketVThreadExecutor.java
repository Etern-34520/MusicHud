package indi.etern.musichud.utils;

import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.MusicHud;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

public class ServerDataPacketVThreadExecutor {
    public static <T extends CustomPacketPayload> NetworkManager.NetworkReceiver<T> execute(
            BiConsumer<T, ServerPlayer> consumer
    ) {
        return (payload, context) -> {
            MusicHud.EXECUTOR.execute(() -> {
                Thread.currentThread().setName("Datapack VProcessor");
                if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    try {
                        consumer.accept(payload, serverPlayer);
                    } catch (Exception e) {
                        MusicHud.getLogger(payload.getClass()).error(e);
                        //noinspection CallToPrintStackTrace
                        e.printStackTrace();
                    }
                } else {
                    throw new IllegalStateException("Player must be a server player");
                }
            });
        };
    }
}

package indi.etern.musichud.network.pushMessages.c2s;

import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.RegisterMark;
import indi.etern.musichud.network.C2SPayload;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.server.api.MusicPlayerServerService;
import indi.etern.musichud.utils.ServerDataPacketVThreadExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AddPlaylistToIdlePlaySourceMessage(long playlistId) implements C2SPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, AddPlaylistToIdlePlaySourceMessage> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            AddPlaylistToIdlePlaySourceMessage::playlistId,
            AddPlaylistToIdlePlaySourceMessage::new
    );

    @RegisterMark
    public static class RegisterImpl implements CommonRegister {
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    AddPlaylistToIdlePlaySourceMessage.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((message, player) -> {
                        MusicPlayerServerService.getInstance().addIdlePlaySource(message.playlistId, player);
                    })
            );
        }
    }
}
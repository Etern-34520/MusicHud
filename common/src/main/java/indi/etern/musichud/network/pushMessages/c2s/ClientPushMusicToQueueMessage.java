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

public record ClientPushMusicToQueueMessage(long id) implements C2SPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, ClientPushMusicToQueueMessage> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            ClientPushMusicToQueueMessage::id,
            ClientPushMusicToQueueMessage::new
    );

    @RegisterMark
    public static class RegisterImpl implements CommonRegister {
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    ClientPushMusicToQueueMessage.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((message, player) -> {
                        MusicPlayerServerService.getInstance().pushMusicToQueue(message.id, player);
                    })
            );
        }
    }
}

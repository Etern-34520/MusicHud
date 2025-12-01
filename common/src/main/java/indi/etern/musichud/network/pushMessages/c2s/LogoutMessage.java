package indi.etern.musichud.network.pushMessages.c2s;

import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.C2SPayload;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.server.api.LoginApiService;
import indi.etern.musichud.server.api.MusicPlayerServerService;
import indi.etern.musichud.utils.ServerDataPacketVThreadExecutor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

@NoArgsConstructor(access = AccessLevel.NONE)
public class LogoutMessage implements C2SPayload {
    public static final LogoutMessage MESSAGE = new LogoutMessage();
    public static final StreamCodec<RegistryFriendlyByteBuf, LogoutMessage> CODEC = StreamCodec.unit(MESSAGE);

    @ForceLoad
    public static class Register implements CommonRegister {
        @Override
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    LogoutMessage.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((message, player) -> {
                        LoginApiService.getInstance().logout(player);
                    })
            );
        }
    }
}

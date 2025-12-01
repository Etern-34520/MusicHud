package indi.etern.musichud.network.requestResponseCycle;

import indi.etern.musichud.MusicHud;
import indi.etern.musichud.Version;
import indi.etern.musichud.client.services.LoginService;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.network.S2CPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import static indi.etern.musichud.MusicHud.LOGGER;

public record ConnectResponse(boolean accepted, Version serverVersion) implements S2CPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectResponse> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ConnectResponse::accepted,
                    Version.PACKET_CODEC,
                    ConnectResponse::serverVersion,
                    ConnectResponse::new
            );

    @ForceLoad
    public static class RegisterImpl implements CommonRegister {//TODO test
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    ConnectResponse.class, CODEC,
                    (payload, context) -> {
                        LOGGER.info("Connecting {}", payload.accepted() ? "accepted" : "denied");
                        MusicHud.setConnected(true);
                        LoginService.getInstance().loginToServer();
                    }
            );
        }
    }
}
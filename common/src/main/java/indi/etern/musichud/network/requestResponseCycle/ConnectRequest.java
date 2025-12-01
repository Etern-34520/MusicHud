package indi.etern.musichud.network.requestResponseCycle;

import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.Version;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.C2SPayload;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.server.api.LoginApiService;
import indi.etern.musichud.server.api.MusicPlayerServerService;
import indi.etern.musichud.utils.ServerDataPacketVThreadExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

@ForceLoad
public record ConnectRequest(Version clientVersion) implements C2SPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, ConnectRequest> CODEC =
            StreamCodec.composite(Version.PACKET_CODEC, ConnectRequest::clientVersion, ConnectRequest::new);

    @ForceLoad
    public static class RegisterImpl implements CommonRegister {
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    ConnectRequest.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((startQRLoginRequest, serverPlayer) -> {
                        ConnectResponse response = new ConnectResponse(Version.capableWith(startQRLoginRequest.clientVersion()), Version.current);
                        NetworkManager.sendToPlayer(serverPlayer, response);
                        LoginApiService instance = LoginApiService.getInstance();
                        instance.joinUnlogged(serverPlayer);
                        MusicPlayerServerService.getInstance().sendSyncPlayingStatusToPlayer(serverPlayer);
                    })
            );
        }
    }
}
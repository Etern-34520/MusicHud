package indi.etern.musichud.network.requestResponseCycle;

import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.RegisterMark;
import indi.etern.musichud.network.C2SPayload;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.server.api.LoginApiService;
import indi.etern.musichud.utils.ServerDataPacketVThreadExecutor;
import lombok.EqualsAndHashCode;
import net.minecraft.network.codec.StreamCodec;

@EqualsAndHashCode
public class StartQRLoginRequest implements C2SPayload {
    public static final StartQRLoginRequest REQUEST = new StartQRLoginRequest();
    public static final StreamCodec<Object, StartQRLoginRequest> CODEC = StreamCodec.unit(REQUEST);

    @RegisterMark
    public static class RegisterImpl implements CommonRegister {//TODO test
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    StartQRLoginRequest.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((startQRLoginRequest, serverPlayer) -> {
                        var qrLoginInfo = LoginApiService.getInstance().startQRLoginByPlayer(serverPlayer);
                        NetworkManager.sendToPlayer(serverPlayer,new StartQRLoginResponse(qrLoginInfo.data().qrimg()));
                    })
            );
        }
    }

}

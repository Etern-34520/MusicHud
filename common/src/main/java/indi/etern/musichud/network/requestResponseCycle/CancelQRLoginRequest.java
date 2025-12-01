package indi.etern.musichud.network.requestResponseCycle;

import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.C2SPayload;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.server.api.LoginApiService;
import indi.etern.musichud.utils.ServerDataPacketVThreadExecutor;
import lombok.EqualsAndHashCode;
import net.minecraft.network.codec.StreamCodec;

@ForceLoad
@EqualsAndHashCode
public class CancelQRLoginRequest implements C2SPayload {
    public static final CancelQRLoginRequest REQUEST = new CancelQRLoginRequest();
    public static final StreamCodec<Object, CancelQRLoginRequest> CODEC = StreamCodec.unit(REQUEST);

    @ForceLoad
    public static class RegisterImpl implements CommonRegister {//TODO test
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    CancelQRLoginRequest.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((cancelQRLoginRequest, serverPlayer) -> {
                        LoginApiService.getInstance().cancelQRLoginByPlayer(serverPlayer);
                    })
            );
        }
    }

}

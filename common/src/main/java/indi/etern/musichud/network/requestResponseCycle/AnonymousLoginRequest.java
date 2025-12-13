package indi.etern.musichud.network.requestResponseCycle;

import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.RegisterMark;
import indi.etern.musichud.network.C2SPayload;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.server.api.LoginApiService;
import indi.etern.musichud.utils.ServerDataPacketVThreadExecutor;
import lombok.EqualsAndHashCode;
import net.minecraft.network.codec.StreamCodec;

@EqualsAndHashCode
public class AnonymousLoginRequest implements C2SPayload {
    public static final AnonymousLoginRequest REQUEST = new AnonymousLoginRequest();
    public static final StreamCodec<Object, AnonymousLoginRequest> CODEC = StreamCodec.unit(REQUEST);

    @RegisterMark
    public static class RegisterImpl implements CommonRegister {

        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    AnonymousLoginRequest.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((anonymousLoginRequest, serverPlayer) -> {
                        LoginApiService.getInstance().loginAsAnonymous(serverPlayer, true);
                    })
            );
        }
    }

}

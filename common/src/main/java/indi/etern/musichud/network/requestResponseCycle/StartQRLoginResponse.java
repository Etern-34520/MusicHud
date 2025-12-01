package indi.etern.musichud.network.requestResponseCycle;

import indi.etern.musichud.client.services.LoginService;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.network.S2CPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@ForceLoad
public record StartQRLoginResponse(String base64QRImg) implements S2CPayload {
    public static final StreamCodec<ByteBuf, StartQRLoginResponse> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    StartQRLoginResponse::base64QRImg,
                    StartQRLoginResponse::new
            );

    @ForceLoad
    public static class RegisterImpl implements CommonRegister {
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    StartQRLoginResponse.class, CODEC,
                    LoginService.getInstance().getQrLoginResponseReceiver()
            );
        }
    }
}

package indi.etern.musichud.network.pushMessages.s2c;

import icyllis.modernui.ModernUI;
import icyllis.modernui.widget.Toast;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.Codecs;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.network.S2CPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CommonErrorMessage(String message, Class<? extends Throwable> cause) implements S2CPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, CommonErrorMessage> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    CommonErrorMessage::message,
                    Codecs.THROWABLE_CODEC,
                    CommonErrorMessage::cause,
                    CommonErrorMessage::new
            );

    @ForceLoad
    public static class RegisterImpl implements CommonRegister {
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    CommonErrorMessage.class, CODEC,
                    (commonErrorMessage, context) -> {
                        String className = commonErrorMessage.cause.getSimpleName();
                        String message = commonErrorMessage.message.isBlank() ? "null" : commonErrorMessage.message;
                        MusicHud.LOGGER.error("Receive error message: {}:{}", className, message);
                        Toast.makeText(ModernUI.getInstance(), message, Toast.LENGTH_SHORT).show();
                    }
            );
        }
    }
}

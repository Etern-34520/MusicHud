package indi.etern.musichud.network;

import icyllis.modernui.annotation.NonNull;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface IPayload extends CustomPacketPayload {
    @Override
    @NonNull
    default Type<? extends IPayload> type() {
        return NetworkRegisterUtil.getType(getClass());
    }
}

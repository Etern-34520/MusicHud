package indi.etern.musichud.beans.login;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum LoginType {
    PHONE_PASSWORD, EMAIL_PASSWORD, QR_CODE, DEVICE_CODE, ANONYMOUS, UNLOGGED;
    public final static StreamCodec<ByteBuf, LoginType> PACKET_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    LoginType::name,
                    LoginType::valueOf
            );
}

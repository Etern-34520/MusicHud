package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonValue;
import indi.etern.musichud.network.Codecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum Quality {
    /**
     * standard => 标准
     * higher   => 较高
     * exhigh   => 极高
     * lossless => 无损
     * hires    => Hi-Res
     * jyeffect => 高清环绕声
     * sky      => 沉浸环绕声
     * dolby    => 杜比全景声
     * jymaster => 超清母带
     *
     */
    STANDARD, HIGHER, EX_HIGH, LOSSLESS, HIRES, JY_EFFECT, SKY, DOLBY, JY_MASTER, NONE;

    public static final StreamCodec<RegistryFriendlyByteBuf, Quality> CODEC = Codecs.ofEnum(Quality.class);

    @JsonValue
    public String getValueName() {
        return this.name().replace("_", "").toLowerCase();
    }
}

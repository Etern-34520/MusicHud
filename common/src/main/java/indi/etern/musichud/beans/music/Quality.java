package indi.etern.musichud.beans.music;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import indi.etern.musichud.network.Codecs;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum Quality {
    /**
     * standard => 标准
     * higher   => 较高
     * exhigh   => 极高
     * lossless => 无损 (高清臻音)
     * hires    => Hi-Res
     * jyeffect => 高清环绕声
     * sky      => 沉浸环绕声
     * dolby    => 杜比全景声
     * jymaster => 超清母带
     *
     */
    STANDARD, HIGHER, EX_HIGH, LOSSLESS, HIRES, JY_EFFECT, SKY, DOLBY, JY_MASTER, NONE;

    public static final StreamCodec<RegistryFriendlyByteBuf, Quality> CODEC = Codecs.ofEnum(Quality.class);

    @Override
    public String toString() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            return I18n.get("music_hud.config.common.primaryChosenQuality." + this.name());
        } else {
            return this.name();
        }
    }
}

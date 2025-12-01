package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivilegeInfo {
    public static final StreamCodec<RegistryFriendlyByteBuf, PrivilegeInfo> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PrivilegeInfo::isCloudSource,
            ByteBufCodecs.INT,
            PrivilegeInfo::getCopyrightStatus,
            ByteBufCodecs.BOOL,
            PrivilegeInfo::isDisabledAsCopyrightProtect,
            Quality.CODEC,
            PrivilegeInfo::getMaxBrLevel,
            Quality.CODEC,
            PrivilegeInfo::getPlayMaxBrLevel,
            Quality.CODEC,
            PrivilegeInfo::getDownloadMaxBrLevel,
            PrivilegeInfo::new
    );
    public static final PrivilegeInfo NONE = new PrivilegeInfo();
    @JsonProperty("cs")
    boolean cloudSource;
    @JsonProperty("st")
    int copyrightStatus;//0 is normal, less than 0 means no copyright
    @JsonProperty("toast")
    boolean disabledAsCopyrightProtect;
    Quality maxBrLevel = Quality.NONE;
    Quality playMaxBrLevel = Quality.NONE;
    Quality downloadMaxBrLevel = Quality.NONE;
}

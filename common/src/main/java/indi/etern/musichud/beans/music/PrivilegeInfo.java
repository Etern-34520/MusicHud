package indi.etern.musichud.beans.music;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
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
    @Getter
    @SerializedName("cs")
    boolean cloudSource;
    @Getter
    @SerializedName("st")
    int copyrightStatus;//0 is normal, less than 0 means no copyright
    @Getter
    @SerializedName("toast")
    boolean disabledAsCopyrightProtect;
    Quality maxBrLevel = Quality.NONE;
    Quality playMaxBrLevel = Quality.NONE;
    Quality downloadMaxBrLevel = Quality.NONE;

    public Quality getDownloadMaxBrLevel() {
        return Objects.requireNonNullElse(downloadMaxBrLevel, Quality.NONE);
    }

    public Quality getPlayMaxBrLevel() {
        return Objects.requireNonNullElse(playMaxBrLevel, Quality.NONE);
    }

    public Quality getMaxBrLevel() {
        return Objects.requireNonNullElse(maxBrLevel, Quality.NONE);
    }
}

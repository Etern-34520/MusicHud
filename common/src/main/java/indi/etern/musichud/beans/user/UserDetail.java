package indi.etern.musichud.beans.user;

import lombok.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@Getter
@Setter(AccessLevel.PACKAGE)
//@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class UserDetail {
    public static final StreamCodec<RegistryFriendlyByteBuf, UserDetail> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    UserDetail::getLevel,
                    ByteBufCodecs.INT,
                    UserDetail::getListenSongs,
                    Profile.STREAM_CODEC,
                    UserDetail::getProfile,
                    UserDetail::new
            );
    int code;
    int level;
    int listenSongs;
    Profile profile;
    public static final UserDetail ANONYMOUS = new UserDetail();

    public UserDetail(int level, int listenSongs, Profile profile) {
        code = 0;
        this.level = level;
        this.listenSongs = listenSongs;
        this.profile = profile;
    }
}

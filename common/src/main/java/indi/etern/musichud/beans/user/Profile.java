package indi.etern.musichud.beans.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Profile {
    public static final StreamCodec<RegistryFriendlyByteBuf, Profile> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    Profile::getNickname,
                    ByteBufCodecs.STRING_UTF8,
                    Profile::getAvatarUrl,
                    ByteBufCodecs.STRING_UTF8,
                    Profile::getBackgroundUrl,
                    ByteBufCodecs.LONG,
                    Profile::getUserId,
                    Profile::new
            );
    public static final Profile ANONYMOUS = new Profile("anonymous", "", "", 0);
    @Getter
    @Setter
    private static volatile Profile current;
    String nickname;
    String avatarUrl;
    String backgroundUrl;
    long userId;
}

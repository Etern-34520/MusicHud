package indi.etern.musichud.beans.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

@Getter
@Setter(AccessLevel.PACKAGE)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class AccountDetail {
    public static final StreamCodec<RegistryFriendlyByteBuf, AccountDetail> STREAM_CODEC = StreamCodec.composite(
            Account.STREAM_CODEC,
            AccountDetail::getAccount,
            Profile.STREAM_CODEC,
            AccountDetail::getProfile,
            AccountDetail::new
    );
    int code;
    Account account;
    @Setter
    Profile profile;

    public AccountDetail(Account account, Profile profile) {
        code = 0;
        this.account = account;
        this.profile = profile;
    }
}

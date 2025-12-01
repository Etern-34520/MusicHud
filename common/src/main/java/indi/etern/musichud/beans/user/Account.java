package indi.etern.musichud.beans.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@Getter
@Setter(AccessLevel.PACKAGE)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Account {
    public static final StreamCodec<RegistryFriendlyByteBuf, Account> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            Account::getId,
            ByteBufCodecs.STRING_UTF8,
            Account::getUserName,
            ByteBufCodecs.INT,
            Account::getType,
            ByteBufCodecs.INT,
            Account::getStatus,
            VipType.STREAM_CODEC,
            Account::getVipType,
            ByteBufCodecs.BOOL,
            Account::isAnonymous,
            Account::new
    );
    long id;
    // actually "1_[8 masked phone num]xxx"
    String userName;
    //unclear
    int type;
    //unclear
    int status;
    VipType vipType;
    @JsonProperty("anonimousUser")
    boolean anonymous;
}
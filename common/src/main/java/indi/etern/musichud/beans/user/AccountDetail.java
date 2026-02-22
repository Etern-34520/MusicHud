package indi.etern.musichud.beans.user;

import lombok.*;

@Getter
@Setter(AccessLevel.PACKAGE)
//@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class AccountDetail {
    int code;
    Account account;
    @Setter
    Profile profile;
}

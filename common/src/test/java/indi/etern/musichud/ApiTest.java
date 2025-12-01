package indi.etern.musichud;

import indi.etern.musichud.beans.user.AccountDetail;
import indi.etern.musichud.server.api.MusicApiService;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.server.api.ServerApiMeta;
import indi.etern.musichud.utils.JsonUtil;
import indi.etern.musichud.utils.http.ApiClient;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static indi.etern.musichud.MusicHud.getLogger;

public class ApiTest {
    private static final Logger LOGGER = getLogger(ApiTest.class);
    private final MusicApiService musicApiService = MusicApiService.getInstance();

    @SneakyThrows
    @Test
    public void testSearch() {
        LOGGER.info("test search");
        List<MusicDetail> searchResult = musicApiService.search("Hideaway Feint");
        assert !searchResult.isEmpty();
        LOGGER.info(JsonUtil.objectMapper.writeValueAsString(searchResult));
    }

    @SneakyThrows
    @Test
    public void testGetDetail() {
        LOGGER.info("test get detail");
        List<MusicDetail> detailByIds = musicApiService.getMusicDetailByIds(List.of(1827011682L));
        assert !detailByIds.isEmpty();
        LOGGER.info(JsonUtil.objectMapper.writeValueAsString(detailByIds));
    }

    @SneakyThrows
    @Test
    public void testGetUser() {
        LOGGER.info("test get user");
        AccountDetail accountDetail = ApiClient.get(ServerApiMeta.User.ACCOUNT, "MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/openapi/clientlog;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/eapi/feedback;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/weapi/clientlog;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/neapi/clientlog;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/wapi/clientlog;;NMTID=00O_u7iKzl2VJkaxk59r2UaxRvrk7IAAAGakS16JA; Max-Age=315360000; Expires=Thu, 15 Nov 2035 09:37:48 GMT; Path=/;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/api/clientlog;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/eapi/feedback;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/eapi/clientlog;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/wapi/feedback;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/wapi/feedback;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/wapi/clientlog;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/neapi/feedback;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/neapi/clientlog;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/weapi/clientlog;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/api/feedback;;MUSIC_U=001FF4379A3CFFAE601B4C3B9B4E1DF99CAF70D1159CE8C0C0A282B637694C84C7B96F544502935D1C7AEC9639C3332E41314850F9B5A7FE9B4CCBDE15556DCA70E0059A5B95F927B240F0928C1524DDF235FAE9205672F6D7410ED13EE907CC3CC64D0EF313B15EBDD8B66F267FA629291D024A16FF2DC0F43F0F7E68A2E09B6A0B37E0E3688C744CE2812F616AC6E109ACEFD1A837FE7EF0EC8282B9AC00F14373353971AAE174875AE1819C6454D67B23CE42FEB6045F684521C0CA23E689A99E88615E17EFE62FF28C1E992C6FA431B543F91A94E3826C911F882235EE751E0525195F4B8EA93D2693E150E19DCB9F8B543CC015FD0754FEB66634689BC8D91163ED1261E40113166E97633DA795F27463DADB1D2DFEC102E9CEA6062AAF58C8130773920515DCC620E32B52B5BE5024885AA876E359D94CC371E57CDF8458F09B2FB4FDD25404349267C53C8A9235914B34991949C99882A4B52F5C771C0785DEE3021A2512798322ED59F65F0138157C082147002791E534972AB2EDDA73979B44568DD0DB12C6391B8107DF4863; Max-Age=15552000; Expires=Sat, 16 May 2026 09:37:48 GMT; Path=/;;MUSIC_SNS=; Max-Age=0; Expires=Mon, 17 Nov 2025 09:37:48 GMT; Path=/;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/api/clientlog;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/api/feedback;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/weapi/feedback;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/openapi/clientlog;;MUSIC_R_U=00BE272DD089DAAEDA205404C15681A9AC18BB4192840B50B277E6284076779E7A4E73BE42E23FF806238DC9A0CE21449A0704FB3811905065E52D38B62063A07B9E3833B468F4174839A44C4AB66D6937; Max-Age=15552000; Expires=Sat, 16 May 2026 09:37:48 GMT; Path=/eapi/login/token/refresh;;MUSIC_R_U=00BE272DD089DAAEDA205404C15681A9AC18BB4192840B50B277E6284076779E7A4E73BE42E23FF806238DC9A0CE21449A0704FB3811905065E52D38B62063A07B9E3833B468F4174839A44C4AB66D6937; Max-Age=15552000; Expires=Sat, 16 May 2026 09:37:48 GMT; Path=/api/login/token/refresh;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/eapi/clientlog;;MUSIC_A_T=1760528123462; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/neapi/feedback;;__csrf=6cd6cdfa2fb94f49dbe0ff0bb2e138b7; Max-Age=1296010; Expires=Tue, 02 Dec 2025 09:37:58 GMT; Path=/;;MUSIC_R_T=1760528195197; Max-Age=2147483647; Expires=Sat, 05 Dec 2093 12:51:55 GMT; Path=/weapi/feedback;");
        LOGGER.info(JsonUtil.objectMapper.writeValueAsString(accountDetail));
    }
}
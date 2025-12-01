package indi.etern.musichud.server.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.*;
import indi.etern.musichud.beans.user.Profile;
import indi.etern.musichud.interfaces.IntegerCodeEnum;
import indi.etern.musichud.utils.http.ApiClient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MusicApiService {
    private static volatile MusicApiService musicApiService;
    private final LoginApiService loginApiService = LoginApiService.getInstance();
    private static final Logger logger = MusicHud.getLogger(MusicApiService.class);

    private static final Cache<Long, Playlist> playlistCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(50)
            .build();
    private static final Cache<Long, MusicDetail> musicDetailCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(400)
            .build();

    public static MusicApiService getInstance() {
        if (musicApiService == null) {
            synchronized (MusicApiService.class) {
                if (musicApiService == null) {
                    musicApiService = new MusicApiService();
                }
            }
        }
        return musicApiService;
    }

    record IdRequest(long id) {}
    public Playlist getPlaylistDetail(long id, @Nullable ServerPlayer serverPlayer) {
        Playlist cached = playlistCache.getIfPresent(id);
        if (cached != null) {
            return cached;
        } else {
            String rawCookie = null;
            LoginApiService.PlayerLoginInfo loginInfo = LoginApiService.getInstance().loginedPlayerInfoMap.get(serverPlayer);
            if (loginInfo != null) {
                rawCookie = loginInfo.loginCookieInfo.rawCookie();
            }
            PlaylistResponse playlistResponse = ApiClient.post(ServerApiMeta.Playlist.DETAIL, new IdRequest(id), rawCookie);
            Playlist playlist = playlistResponse.getPlaylist();
            playlistCache.put(id, playlist);
            return playlist;
        }
    }

    record SearchRequestBody(String keywords, int limit, int offset, String cookie, SearchType type) {
        public enum SearchType implements IntegerCodeEnum {
            MUSIC(1),
            ALBUM(10),
            ARTIST(100),
            PLAYLIST(1000),
            USER(1002),
            MV(1004),
            LYRICS(1006),
            RADIO(1009),
            VIDEO(1014),
            COMPREHENSIVE(1018),
            SOUND(2000);
            @Getter
            private final int code;
            SearchType(int code) {
                this.code = code;
            }
        }
    }
    public record SearchResponseBody(
        MusicDetailResponse result
    ) {}

    public List<MusicDetail> search(String keywords) {
        try {
            var requestBody = new SearchRequestBody(keywords, 30, 0, null, SearchRequestBody.SearchType.MUSIC);
            var response = ApiClient.post(ServerApiMeta.Search.CLOUD, requestBody, null);
            return response.result().getMusicDetails();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    record GetDetailsRequestBody(List<String> ids, String cookie) {}
    record GetDetailRequestBody(String ids, String cookie) {}

    public List<MusicDetail> getMusicDetailByIds(List<Long> ids) {
        List<Long> uncachedIds = new ArrayList<>();
        List<MusicDetail> result = new ArrayList<>(ids.size());
        for (long id : ids) {
            MusicDetail cached = musicDetailCache.getIfPresent(id);
            if (cached != null) {
                result.add(cached);
            } else {
                uncachedIds.add(id);
            }
        }
        if (uncachedIds.isEmpty()) {
            return result;
        } else {
            try {
                Object requestBody;
                if (ids.size() > 1) {
                    requestBody = new GetDetailsRequestBody(uncachedIds.stream().map(String::valueOf).toList(), null);
                } else if (ids.size() == 1) {
                    requestBody = new GetDetailRequestBody(String.valueOf(ids.getFirst()), null);
                } else {
                    return List.of();
                }
                String userCookie = loginApiService.randomVipCookieOr(null);
                var response = ApiClient.post(ServerApiMeta.Music.DETAIL, requestBody, userCookie);
                List<MusicDetail> musicDetails = response.getMusicDetails();
                result.addAll(musicDetails);
                for (MusicDetail musicDetail : musicDetails) {
                    musicDetailCache.put(musicDetail.getId(), musicDetail);
                }
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    record GetDirectResourceUrlRequest(long id, boolean unblock, Quality level) {}
    public record GetDirectResourceUrlResponse(int code, List<MusicResourceInfo> data) {}

    public MusicResourceInfo getResourceInfo(MusicDetail musicDetail/*, PrivilegeInfo privilegeInfo*/) {
        if (musicDetail == null || musicDetail.equals(MusicDetail.NONE)) {
            return MusicResourceInfo.NONE;
        } else {
            var request = new GetDirectResourceUrlRequest(musicDetail.getId(), false, Quality.LOSSLESS);
            var response = ApiClient.post(ServerApiMeta.Music.URL, request, loginApiService.randomVipCookieOr(null));
            if (response.code == 200) {
                MusicResourceInfo musicResourceInfo = response.data.getFirst();
                // 30 seconds trial
                if (musicResourceInfo.getTime() == 30040) {
                    var unblockRequest = new GetDirectResourceUrlRequest(musicDetail.getId(), true, Quality.LOSSLESS);
                    var unblockResponse = ApiClient.post(ServerApiMeta.Music.URL, unblockRequest, loginApiService.randomVipCookieOr(null));
                    musicResourceInfo = unblockResponse.data.getFirst();
                }
                try {
                    LyricInfo lyricInfo = getLyricInfo(musicDetail.getId());
                    musicResourceInfo.setLyricInfo(lyricInfo);
                } catch (Exception e) {
                    logger.warn("Failed to get lyric for music id: " + musicDetail.getId(), e);
                }
                return musicResourceInfo;
            } else {
                logger.error("Failed to get lyric for music id: " + musicDetail.getId());
                return null;
            }
        }
    }
    public record RequestDataWithUID(long uid) {}
    public List<Playlist> getPlayersUserPlaylists(ServerPlayer player) {
        LoginApiService.PlayerLoginInfo loginInfo = loginApiService.getLoginInfoByServerPlayer(player);
        Profile profile = loginInfo.profile;
        if (profile == null) {
            return List.of();
        } else {
            PlaylistsResponse playlistData = ApiClient.post(
                    ServerApiMeta.User.PLAYLIST,
                    new RequestDataWithUID(profile.getUserId()),
                    loginInfo.loginCookieInfo.rawCookie()
            );
            return playlistData.getPlaylists();
        }
    }

    public LyricInfo getLyricInfo(long musicId) {
        try {
            var response = ApiClient.post(ServerApiMeta.Music.LYRIC, new IdRequest(musicId), null);
            if (response.getCode() == 200) {
                return response;
            } else {
                throw new RuntimeException("Failed to get lyric for music id: " + musicId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
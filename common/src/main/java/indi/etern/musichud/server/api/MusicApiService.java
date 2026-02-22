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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class MusicApiService {
    private static final Logger logger = MusicHud.getLogger(MusicApiService.class);
    private static final Cache<Long, Playlist> playlistCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(50)
            .build();
    private static final Cache<Long, MusicDetail> musicDetailCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(400)
            .build();
    private static volatile MusicApiService musicApiService;
    private final LoginApiService loginApiService = LoginApiService.getInstance();

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

    public Playlist getPlaylistDetail(long id, @Nullable ServerPlayer serverPlayer) {
        Playlist cached = playlistCache.getIfPresent(id);
        if (cached != null) {
            return cached;
        } else {
            String rawCookie;
            LoginApiService.PlayerLoginInfo loginInfo = LoginApiService.getInstance().loginedPlayerInfoMap.get(serverPlayer);
            if (loginInfo != null) {
                rawCookie = loginInfo.loginCookieInfo.rawCookie();
            } else {
                rawCookie = loginApiService.getAnonymousCookie();
            }
            PlaylistResponse playlistResponse = ApiClient.post(ServerApiMeta.Playlist.DETAIL, new IdRequest(id), rawCookie);
            if (playlistResponse.getCode() == 200) {
                Playlist playlist = playlistResponse.getPlaylist();
                playlistCache.put(id, playlist);
                return playlist;
            } else {
                logger.error("Failed to get playlist detail of player: {} (response code: {})", Objects.requireNonNull(serverPlayer).getName().getString(), playlistResponse.getCode());
                return null;
            }
        }
    }

    public List<MusicDetail> search(String keywords) {
        try {
            var requestBody = new SearchRequestBody(keywords, 30, 0, null, SearchRequestBody.SearchType.MUSIC);
            var response = ApiClient.post(ServerApiMeta.Search.CLOUD, requestBody, null);
            return response.result().getMusicDetails();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    public MusicResourceInfo getResourceInfo(MusicDetail musicDetail, Quality quality) {
        if (musicDetail == null || musicDetail.equals(MusicDetail.NONE)) {
            return MusicResourceInfo.NONE;
        } else {
            MusicResourceInfo musicResourceInfo;
            int retryCount = 0;
            boolean available;
            do {
                if (retryCount >= 5) {
                    logger.error("Failed to load music resource for \"{}\"(id:{}), as resource url is not available", musicDetail.getName(), musicDetail.getId());
                    return MusicResourceInfo.NONE;
                }
                var request = new GetDirectResourceUrlRequest(musicDetail.getId(), false, quality);
                var response = ApiClient.post(ServerApiMeta.Music.URL, request, loginApiService.randomVipCookieOr(null));
                if (response.code == 200) {
                    musicResourceInfo = response.data.getFirst();
                    // 30 seconds trial or have no copyright
                    if (musicResourceInfo.getTime() <= 30040 || musicResourceInfo.getUrl() == null) {
                        musicResourceInfo = getMusicResourceInfoFromMatcher(musicDetail);
                    }
                    completeLyricInfo(musicDetail);
                } else {
                    logger.warn("Failed to get resource for music: {} (ID: {}), trying substitute", musicDetail.getName(), musicDetail.getId());
                    try {
                        musicResourceInfo = getMusicResourceInfoFromMatcher(musicDetail);
                        completeLyricInfo(musicDetail);
                    } catch (Exception e) {
                        logger.error("Failed to get resource for music from substitute: {} (ID: {})", musicDetail.getName(), musicDetail.getId());
                        musicResourceInfo = MusicResourceInfo.NONE;
                    }
                }
                available = ApiClient.checkUrlAvailable(musicResourceInfo.getUrl(), 10000);
                retryCount++;
            } while (!available);
            return musicResourceInfo;
        }
    }

    private void completeLyricInfo(MusicDetail musicDetail) {
        try {
            LyricInfo lyricInfo = getLyricInfo(musicDetail);
            musicDetail.setLyricInfo(lyricInfo);
        } catch (Exception e) {
            logger.warn("Failed to get lyric for music: {} (ID: {})", musicDetail.getName(), musicDetail.getId(), e);
        }
    }

    private @NotNull MusicResourceInfo getMusicResourceInfoFromMatcher(MusicDetail musicDetail) {
        var unblockRequest = new GetMatchResourceUrlRequest(musicDetail.getId(), null);
        var unblockResponse = ApiClient.post(ServerApiMeta.Music.UNBLOCK, unblockRequest, loginApiService.randomVipCookieOr(null));
        return MusicResourceInfo.from(unblockResponse.data, musicDetail);
    }

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

    public LyricInfo getLyricInfo(MusicDetail musicDetail) {
        var response = ApiClient.post(ServerApiMeta.Music.WORD_BY_WORD_LYRIC, new IdRequest(musicDetail.getId()), loginApiService.randomVipCookieOr(loginApiService::getAnonymousCookie));
        if (response.getCode() == 200) {
            return response;
        } else {
            throw new RuntimeException("Failed to get lyric for music: " + musicDetail.getName() + " (ID: " + musicDetail.getId() + "), response code:" + response.getCode());
        }
    }

    record IdRequest(long id) {
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
    ) {
    }

    record GetDetailsRequestBody(List<String> ids, String cookie) {
    }

    record GetDetailRequestBody(String ids, String cookie) {
    }

    record GetDirectResourceUrlRequest(long id, boolean unblock, Quality level) {
    }

    record GetMatchResourceUrlRequest(long id, String source) {
    }

    public record GetDirectResourceUrlResponse(int code, List<MusicResourceInfo> data) {
    }

    public record GetMatchResourceUrlResponse(int code, String data) {
    }

    public record RequestDataWithUID(long uid) {
    }
}
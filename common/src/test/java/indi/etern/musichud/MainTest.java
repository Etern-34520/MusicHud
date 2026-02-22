package indi.etern.musichud;

import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.server.api.MusicApiService;
import indi.etern.musichud.utils.JsonUtil;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static indi.etern.musichud.MusicHud.getLogger;

public class MainTest {
    private static final Logger LOGGER = getLogger(MainTest.class);
    private final MusicApiService musicApiService = MusicApiService.getInstance();

    @SneakyThrows
    @Test
    public void testSearch() {
        LOGGER.info("test search");
        List<MusicDetail> searchResult = musicApiService.search("Hideaway Feint");
        assert !searchResult.isEmpty();
        LOGGER.info(JsonUtil.gson.toJson(searchResult));
    }

    @SneakyThrows
    @Test
    public void testGetDetail() {
        LOGGER.info("test get detail");
        List<MusicDetail> detailByIds = musicApiService.getMusicDetailByIds(List.of(1827011682L));
        assert !detailByIds.isEmpty();
        LOGGER.info(JsonUtil.gson.toJson(detailByIds));
    }

    @SneakyThrows
    @Test
    public void testPlaylistDetail() {
        LOGGER.info("test get playlist detail");
        Playlist detailByIds = musicApiService.getPlaylistDetail(975427390, null);
        LOGGER.info(JsonUtil.gson.toJson(detailByIds));
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        Playlist.CODEC.encode(buf, detailByIds);
    }

    @SneakyThrows
    @Test
    public void testVersion() {
        assert Version.capableWith(new Version(1, 0, 0, Version.BuildType.Alpha));
    }

}
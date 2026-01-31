package indi.etern.musichud;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.beans.user.AccountDetail;
import indi.etern.musichud.server.api.MusicApiService;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.server.api.ServerApiMeta;
import indi.etern.musichud.utils.JsonUtil;
import indi.etern.musichud.utils.http.ApiClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
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
    public void testPlaylistDetail() {
        LOGGER.info("test get playlist detail");
        Playlist detailByIds = musicApiService.getPlaylistDetail(975427390, null);
        LOGGER.info(JsonUtil.objectMapper.writeValueAsString(detailByIds));
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        Playlist.CODEC.encode(buf, detailByIds);
    }

    @SneakyThrows
    @Test
    public void testVersion() {
        assert Version.capableWith(new Version(1, 0, 0, Version.BuildType.Alpha));
    }

}
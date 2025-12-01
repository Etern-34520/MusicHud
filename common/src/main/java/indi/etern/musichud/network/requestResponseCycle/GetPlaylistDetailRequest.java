package indi.etern.musichud.network.requestResponseCycle;

import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.C2SPayload;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.server.api.MusicApiService;
import indi.etern.musichud.utils.ServerDataPacketVThreadExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GetPlaylistDetailRequest(long id) implements C2SPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, GetPlaylistDetailRequest> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG,
            GetPlaylistDetailRequest::id,
            GetPlaylistDetailRequest::new
    );

    @ForceLoad
    public static class RegisterImpl implements CommonRegister {
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    GetPlaylistDetailRequest.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((playlistDetailRequest, player) -> {
                        Playlist playlistDetail = MusicApiService.getInstance().getPlaylistDetail(playlistDetailRequest.id, player);
                        NetworkManager.sendToPlayer(player,new GetPlaylistDetailResponse(playlistDetail));
                    })
            );
        }
    }
}

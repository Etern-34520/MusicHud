package indi.etern.musichud.network.requestResponseCycle;

import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.network.S2CPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public record GetPlaylistDetailResponse(Playlist playlist) implements S2CPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, GetPlaylistDetailResponse> CODEC = StreamCodec.composite(
            Playlist.CODEC,
            GetPlaylistDetailResponse::playlist,
            GetPlaylistDetailResponse::new
    );

    static Map<Long, Consumer<GetPlaylistDetailResponse>> consumerMap = new HashMap<>();
    public static void setReceiver(long id, Consumer<GetPlaylistDetailResponse> consumer) {
        GetPlaylistDetailResponse.consumerMap.put(id, consumer);
    }

    @ForceLoad
    public static class RegisterImpl implements CommonRegister {
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    GetPlaylistDetailResponse.class, CODEC,
                    (playlistDetailRequest, context) -> {
                        Consumer<GetPlaylistDetailResponse> consumer = consumerMap.remove(playlistDetailRequest.playlist().getId());
                        if (consumer != null) {
                            consumer.accept(playlistDetailRequest);
                        }
                    }
            );
        }
    }
}

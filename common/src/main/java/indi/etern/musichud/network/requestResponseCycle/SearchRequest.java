package indi.etern.musichud.network.requestResponseCycle;

import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.C2SPayload;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.server.api.MusicApiService;
import indi.etern.musichud.utils.ServerDataPacketVThreadExecutor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record SearchRequest(String query) implements C2SPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, SearchRequest> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SearchRequest::query,
            SearchRequest::new
    );

    @ForceLoad
    public static class Register implements CommonRegister {
        @Override
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(SearchRequest.class, CODEC,
                    ServerDataPacketVThreadExecutor.execute((message, player) -> {
                        List<MusicDetail> result = MusicApiService.getInstance().search(message.query);
                        if (result == null) {
                            result = List.of();
                        }
                        NetworkManager.sendToPlayer(player, new SearchResponse(result));
                    })
            );
        }
    }
}

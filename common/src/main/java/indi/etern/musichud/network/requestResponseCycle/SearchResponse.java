package indi.etern.musichud.network.requestResponseCycle;

import icyllis.modernui.mc.MuiModApi;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.client.ui.pages.SearchView;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.Codecs;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.network.S2CPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record SearchResponse(List<MusicDetail> result) implements S2CPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, SearchResponse> CODEC = StreamCodec.composite(
            Codecs.ofList(MusicDetail.CODEC),
            SearchResponse::result,
            SearchResponse::new
    );

    @ForceLoad
    public static class Register implements CommonRegister {
        @Override
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(SearchResponse.class, CODEC,
                    (message, context) -> {
                        MuiModApi.postToUiThread(() -> {
                            SearchView.getInstance().setResult(message.result());
                        });
                    }
            );
        }
    }
}

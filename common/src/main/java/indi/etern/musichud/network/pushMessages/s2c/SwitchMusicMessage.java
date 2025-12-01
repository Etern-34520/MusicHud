package indi.etern.musichud.network.pushMessages.s2c;

import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.MusicDetail;
import indi.etern.musichud.beans.music.MusicResourceInfo;
import indi.etern.musichud.client.services.MusicService;
import indi.etern.musichud.interfaces.CommonRegister;
import indi.etern.musichud.interfaces.ForceLoad;
import indi.etern.musichud.network.NetworkRegisterUtil;
import indi.etern.musichud.network.S2CPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record SwitchMusicMessage(MusicDetail musicDetail, MusicResourceInfo resourceInfo, MusicDetail next, MusicResourceInfo nextResource) implements S2CPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchMusicMessage> CODEC = StreamCodec.composite(
            MusicDetail.CODEC,
            SwitchMusicMessage::musicDetail,
            MusicResourceInfo.CODEC,
            SwitchMusicMessage::resourceInfo,
            MusicDetail.CODEC,
            SwitchMusicMessage::next,
            MusicResourceInfo.CODEC,
            SwitchMusicMessage::nextResource,
            SwitchMusicMessage::new
    );

    @ForceLoad
    public static class RegisterImpl implements CommonRegister {
        public void register() {
            NetworkRegisterUtil.autoRegisterPayload(
                    SwitchMusicMessage.class, CODEC,
                    (message, context) -> {
                        MusicHud.EXECUTOR.execute(() -> {
                            MusicService musicService = MusicService.getInstance();
                            musicService.switchMusic(message.musicDetail, message.resourceInfo, null);
                            if (!message.next.equals(MusicDetail.NONE)) {
                                musicService.loadResource(message.next);
                            }
                        });
                    }
            );
        }
    }
}

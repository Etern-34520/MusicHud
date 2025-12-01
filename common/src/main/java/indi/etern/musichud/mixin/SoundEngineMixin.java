package indi.etern.musichud.mixin;

import indi.etern.musichud.client.config.ClientConfigDefinition;
import indi.etern.musichud.utils.music.StreamAudioPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At("HEAD"), cancellable = true)
    private void onPlaySound(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (soundInstance.getSource() == SoundSource.MUSIC && ClientConfigDefinition.disableVanillaMusic.get() && StreamAudioPlayer.getInstance().isPlaying()) {
            cir.cancel();
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}

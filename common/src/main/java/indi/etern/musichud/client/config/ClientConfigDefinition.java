package indi.etern.musichud.client.config;

import indi.etern.musichud.MusicHud;
import indi.etern.musichud.client.ui.hud.metadata.HPosition;
import indi.etern.musichud.client.ui.hud.metadata.VPosition;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ClientConfigDefinition {
    public static Pair<ClientConfigDefinition, ModConfigSpec> configure;
    public static ModConfigSpec.ConfigValue<Boolean> enable;
    public static ModConfigSpec.ConfigValue<Boolean> disableVanillaMusic;
    public static ModConfigSpec.ConfigValue<Boolean> hideHudWhenNotPlaying;
    public static ModConfigSpec.ConfigValue<String> hudVerticalPosition;
    public static ModConfigSpec.ConfigValue<String> hudHorizontalPosition;
    public static ModConfigSpec.ConfigValue<Integer> hudOffsetX;
    public static ModConfigSpec.ConfigValue<Integer> hudOffsetY;
    public static ModConfigSpec.ConfigValue<Integer> hudWidth;
    public static ModConfigSpec.ConfigValue<Integer> hudHeight;
    public static ModConfigSpec.ConfigValue<Integer> hudCornerRadius;
    public static ModConfigSpec.ConfigValue<String> clientCookie;
    public static ModConfigSpec.ConfigValue<String> clientAccountConfig;
    public static final String ENABLE_KEY = MusicHud.MOD_ID + ".config.enable";
    public static final String DISABLE_VANILLA_MUSIC_KEY = MusicHud.MOD_ID + ".config.disableVanillaMusic";
    public static final String HIDE_HUD_WHEN_NOT_PLAYING_KEY = MusicHud.MOD_ID + ".config.hideHudWhenNotPlaying";
    public static final String VERTICAL_POSITION_KEY = MusicHud.MOD_ID + ".config.verticalPosition";
    public static final String HORIZONTAL_POSITION_KEY = MusicHud.MOD_ID + ".config.horizontalPosition";
    public static final String OFFSET_X_KEY = MusicHud.MOD_ID + ".config.offsetX";
    public static final String OFFSET_Y_KEY = MusicHud.MOD_ID + ".config.offsetY";
    public static final String HUD_WIDTH_KEY = MusicHud.MOD_ID + ".config.hudWidth";
    public static final String HUD_HEIGHT_KEY = MusicHud.MOD_ID + ".config.hudHeight";
    public static final String HUD_CORNER_RADIUS_KEY = MusicHud.MOD_ID + ".config.hudCornerRadius";
    public static final String CLIENT_COOKIE_KEY = MusicHud.MOD_ID + ".clientCookie";
    public static final String CLIENT_ACCOUNT_CONFIG = MusicHud.MOD_ID + ".clientAccountConfig";

    ClientConfigDefinition(ModConfigSpec.Builder builder) {
        ClientConfigDefinition.enable = builder
                .comment("Enable Music Hud Functions")
                .translation(ENABLE_KEY)
                .define("enable", true);
        ClientConfigDefinition.disableVanillaMusic = builder
                .comment("Disable vanilla game music")
                .translation(DISABLE_VANILLA_MUSIC_KEY)
                .define("disableVanillaMusic", true);
        ClientConfigDefinition.hideHudWhenNotPlaying = builder
                .comment("Hide hud when not playing music")
                .translation(HIDE_HUD_WHEN_NOT_PLAYING_KEY)
                .define("hideHudWhenNotPlaying", true);
        ClientConfigDefinition.hudVerticalPosition = builder
                .comment("Vertical position (TOP|CENTER|BOTTOM)")
                .translation(VERTICAL_POSITION_KEY)
                .define("verticalPosition", VPosition.TOP.name());
        ClientConfigDefinition.hudHorizontalPosition = builder
                .comment("Horizontal position (LEFT|CENTER|RIGHT)")
                .translation(HORIZONTAL_POSITION_KEY)
                .define("horizontalPosition", HPosition.LEFT.name());
        ClientConfigDefinition.hudOffsetX = builder
                .comment("Hud offset x")
                .translation(OFFSET_X_KEY)
                .define("hudOffsetX", 16);
        ClientConfigDefinition.hudOffsetY = builder
                .comment("Hud offset y")
                .translation(OFFSET_Y_KEY)
                .define("hudOffsetY", 16);
        ClientConfigDefinition.hudWidth = builder
                .comment("Hud width")
                .translation(HUD_WIDTH_KEY)
                .define("hudWidth", 150);
        ClientConfigDefinition.hudHeight = builder
                .comment("Hud height")
                .translation(HUD_HEIGHT_KEY)
                .define("hudHeight", 44);
        ClientConfigDefinition.hudCornerRadius = builder
                .comment("Hud rounded corner radius")
                .translation(HUD_CORNER_RADIUS_KEY)
                .define("hudCornerRadius", 8);
        ClientConfigDefinition.clientCookie = builder
                .comment("Client NCM cookie json")
                .translation(CLIENT_COOKIE_KEY)
                .define("clientCookie", "");
        ClientConfigDefinition.clientAccountConfig = builder
                .comment("Client account config json")
                .translation(CLIENT_ACCOUNT_CONFIG)
                .define("clientAccountConfig", "");
    }

    public static void configure() {
        if (configure == null) {
            configure = new ModConfigSpec.Builder().configure(ClientConfigDefinition::new);
        }
    }

    static {
        configure();
    }
}
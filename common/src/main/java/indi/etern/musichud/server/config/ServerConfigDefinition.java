package indi.etern.musichud.server.config;

import indi.etern.musichud.server.api.ServerApiMeta;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfigDefinition {
    public static final Pair<ServerConfigDefinition, ModConfigSpec> configure;
    public final ModConfigSpec.ConfigValue<String> serverApiBaseUrl;

    public ServerConfigDefinition(ModConfigSpec.Builder builder) {
        this.serverApiBaseUrl = builder
                .comment("Server API Base URL configuration")
                .translation("music_hud.serverApiBaseUrl")
                .define("serverApiBaseUrl", ServerApiMeta.DEFAULT_API_BASE_URL);
    }

    static {
         configure = new ModConfigSpec.Builder().configure(ServerConfigDefinition::new);
    }
}

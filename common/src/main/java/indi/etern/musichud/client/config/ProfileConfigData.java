package indi.etern.musichud.client.config;

import indi.etern.musichud.beans.user.Profile;
import indi.etern.musichud.utils.JsonUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProfileConfigData {
    private static volatile ProfileConfigData instance;
    Profile profile;
    Set<Long> idlePlaySourcePlaylistIds = new HashSet<>();

    @SneakyThrows
    public void saveToConfig() {
        ClientConfigDefinition.clientAccountConfig.set(JsonUtil.objectMapper.writeValueAsString(this));
        ClientConfigDefinition.clientAccountConfig.save();
    }

    @SneakyThrows
    public static ProfileConfigData getInstance() {
        if (instance == null) {
            synchronized (ProfileConfigData.class) {
                if (instance == null) {
                    String json = ClientConfigDefinition.clientAccountConfig.get();
                    if (json == null || json.isEmpty()) {
                        instance = new ProfileConfigData();
                    } else {
                        instance = JsonUtil.objectMapper.readValue(json, ProfileConfigData.class);
                    }
                }
            }
        }
        return instance;
    }
}
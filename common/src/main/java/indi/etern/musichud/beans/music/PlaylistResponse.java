package indi.etern.musichud.beans.music;

import com.google.gson.annotations.SerializedName;
import indi.etern.musichud.interfaces.PostProcessable;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

public class PlaylistResponse implements PostProcessable {
    @Getter
    int code;
    @SerializedName("playlist")
    Playlist playlist;
    List<PrivilegeInfo> privileges = List.of();

    public void postProcess() {
        if (code == 200) {
            int index = 0;
            List<PrivilegeInfo> privileges = null;
            if (this.privileges.isEmpty()) {
                if (!playlist.privileges.isEmpty()) {
                    privileges = playlist.privileges;
                }
            } else {
                privileges = this.privileges;
                playlist.privileges = this.privileges;
            }
            if (playlist.tracks != null && privileges != null)
                for (MusicDetail musicDetail : playlist.tracks) {
                    musicDetail.privilege = privileges.get(index++);
                }
        }
    }

    public Playlist getPlaylist() {
        return Objects.requireNonNullElse(playlist, Playlist.EMPTY);
    }

    public List<PrivilegeInfo> getPrivileges() {
        if (privileges == null || privileges.isEmpty()) {
            return List.of();
        }
        return privileges.stream().filter(Objects::nonNull).toList();
    }
}

package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import indi.etern.musichud.interfaces.PostProcessable;
import lombok.Getter;

import java.util.List;

@Getter
public class PlaylistResponse implements PostProcessable {
    int code;
    @JsonProperty("playlist")
    Playlist playlist;
    @JsonSetter(nulls = Nulls.SKIP)
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
}

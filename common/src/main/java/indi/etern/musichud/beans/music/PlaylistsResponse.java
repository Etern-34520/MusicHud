package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import indi.etern.musichud.interfaces.PostProcessable;
import lombok.Getter;

import java.util.List;

@Getter
public class PlaylistsResponse implements PostProcessable {
    int code;
    @JsonProperty("playlist")
    List<Playlist> playlists = List.of();

    public void postProcess() {
        int index = 0;
        for (Playlist playlist : playlists) {
            if (playlist.tracks != null)
                for (MusicDetail musicDetail : playlist.tracks) {
                    musicDetail.privilege = playlist.privileges.get(index++);
                }
        }
    }
}

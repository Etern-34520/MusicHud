package indi.etern.musichud.beans.music;

import com.google.gson.annotations.SerializedName;
import indi.etern.musichud.interfaces.PostProcessable;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

public class PlaylistsResponse implements PostProcessable {
    @Getter
    int code;
    @SerializedName("playlist")
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

    public List<Playlist> getPlaylists() {
        if (playlists == null || playlists.isEmpty()) {
            return List.of();
        }
        return playlists.stream().filter(Objects::nonNull).toList();
    }
}

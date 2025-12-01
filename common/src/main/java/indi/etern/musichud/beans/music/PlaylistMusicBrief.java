package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlaylistMusicBrief {
    String name;
    long id;
    @JsonProperty("at")
    long sourcePlaylistId;
    @JsonProperty("uid")
    long playlistCreatorId;
}

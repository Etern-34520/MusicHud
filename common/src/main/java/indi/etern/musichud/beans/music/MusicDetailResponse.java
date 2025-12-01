package indi.etern.musichud.beans.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import indi.etern.musichud.interfaces.PostProcessable;
import lombok.Getter;

import java.util.List;

@Getter
public class MusicDetailResponse implements PostProcessable {
    int code;
    @JsonProperty("songs")
    List<MusicDetail> musicDetails;
    @JsonSetter(nulls = Nulls.SKIP)
    List<PrivilegeInfo> privileges = List.of();

    public void postProcess() {
        int index = 0;
        if (musicDetails != null && privileges != null && musicDetails.size() == privileges.size()) {
            for (MusicDetail musicDetail : musicDetails) {
                musicDetail.privilege = privileges.get(index++);
            }
        } else {
            throw new IllegalStateException();
        }
    }
}

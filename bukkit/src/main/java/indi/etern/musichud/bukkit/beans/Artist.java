package indi.etern.musichud.bukkit.beans;

import java.util.Objects;

/**
 * Represents an artist in the music system.
 */
public class Artist {
    private long id;
    private String name;
    
    public Artist() {
        this.name = "";
    }
    
    public Artist(long id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return id == artist.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return name;
    }
}

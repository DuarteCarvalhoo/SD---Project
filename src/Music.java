import java.io.Serializable;

public class Music implements Serializable {
    private String path,title,composer,artist,album,genre;
    private int duration;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComposer() {
        return composer;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Music(String path, String title, String composer, String artist, String duration, String album, String genre) {
        this.path = path;
        this.title = title;
        this.composer = composer;
        this.artist = artist;
        this.duration = Integer.parseInt(duration);
        this.album = album;
        this.genre = genre;
    }

    public String toString(){
        return getArtist() +" - " + getTitle();
    }
}

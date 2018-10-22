public class Music {
    private String composer,artist,title,duration,style;

    public String getComposer() {
        return composer;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }

    public String getStyle() {
        return style;
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

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Music(String composer, String artist, String title, String duration, String style) {
        this.composer = composer;
        this.artist = artist;
        this.title = title;
        this.duration = duration;
        this.style = style;
    }
}

import java.io.Serializable;

public class Music implements Serializable {
    private String title;
    private int length;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Music(String path, String title, String composer, String artist, String length, String album, String genre) {
        this.title = title;
        this.length = Integer.parseInt(length);
    }

    public String toString(){
        return getTitle();
    }
}

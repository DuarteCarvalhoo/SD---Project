import java.io.Serializable;

public class Music implements Serializable {
    private String title;
    private int length;
    private int id;

    public Music(String s) {
        this.title = s;
    }

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

    public Music(String title,int length) {
        this.title = title;
        this.length = length;
    }

    public String toString(){
        return getTitle();
    }
}

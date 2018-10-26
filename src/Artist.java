import java.io.Serializable;

public class Artist implements Serializable {
    private String name,description, genre;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Artist(String name, String genre, String description) {
        this.name = name;
        this.description = description;
        this.genre = genre;
    }

    public String toString(){
        return getName()+"-"+getGenre()+"-"+getDescription();
    }
}

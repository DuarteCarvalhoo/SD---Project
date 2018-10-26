import java.io.Serializable;
import java.util.ArrayList;

public class Artist implements Serializable {
    private String name,description, genre;
    private ArrayList<Album> albums = new ArrayList<>();

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

    public String printAlbums(ArrayList<Album> albums) {
        String finalString = "";
        if(albums.isEmpty()){
            finalString += "No albums to show.";
        }
        else {
            for (Album album : albums) {
                finalString += album.getName();
                finalString += "\n";
            }
        }
        return finalString;
    }

    public ArrayList<Album> getAlbums() {
        return albums;
    }

    public Artist(String name, String genre, String description) {
        this.name = name;
        this.description = description;
        this.genre = genre;
    }

    public Artist(){}

    public String toString(){
        return "Name: "+getName()
                +"\nGenre: "+getGenre()
                +"\nDescription: "+getDescription()
                +"\nAlbums: "+printAlbums(this.albums);
    }
}

import java.io.Serializable;
import java.util.ArrayList;

public abstract class Artist implements Serializable {
    private String name,description;
    private boolean isComposer = false ,isSongwriter = false ,isMusician = false , isBand = false;
    private ArrayList<Album> albums = new ArrayList<>();
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isComposer() {
        return isComposer;
    }

    public boolean isSongwriter() {
        return isSongwriter;
    }

    public boolean isMusician() {
        return isMusician;
    }

    public boolean isBand() {
        return isBand;
    }

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

    public Artist(String name,String description) {
        this.name = name;
        this.description = description;
    }

    public Artist(){}

    @Override
    public String toString(){
        return "Name: "+getName()
                +"\nDescription: "+getDescription()
                +"\nAlbums: "+printAlbums(this.albums);
    }
}

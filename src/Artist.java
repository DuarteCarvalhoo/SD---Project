public class Artist {
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

    public Artist(String name, String description, String genre) {
        this.name = name;
        this.description = description;
        this.genre = genre;
    }
}

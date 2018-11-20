public class Musician extends Artist{
    private boolean isMusician;

    public boolean isMusician() {
        return isMusician;
    }

    public Musician(String name, String genre, String description) {
        super(name, genre, description);
        this.isMusician = true;
    }

    public Musician() {
    }
}

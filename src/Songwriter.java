public class Songwriter extends Artist{
    private boolean isSongwriter = false;

    public boolean isSongwriter() {
        return isSongwriter;
    }

    public Songwriter(String name, String genre, String description) {
        super(name, genre, description);
        this.isSongwriter = true;
    }

    public Songwriter() {
    }
}

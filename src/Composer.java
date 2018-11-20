public class Composer extends Artist {
    private boolean isComposer = false;

    public boolean isComposer() {
        return isComposer;
    }

    public Composer(String name, String genre, String description) {
        super(name, genre, description);
        this.isComposer = true;
    }

    public Composer() {
    }
}

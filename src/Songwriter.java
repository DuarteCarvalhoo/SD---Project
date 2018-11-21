public class Songwriter extends Artist{
    private boolean isSongwriter = false;

    @Override
    public boolean isSongwriter() {
        return isSongwriter;
    }

    public Songwriter(String name, String description) {
        super(name, description);
        this.isSongwriter = true;
    }

    public Songwriter() {
    }
}

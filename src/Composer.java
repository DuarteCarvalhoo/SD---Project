public class Composer extends Artist {
    private boolean isComposer = false;

    @Override
    public boolean isComposer() {
        return isComposer;
    }

    public Composer(String name, String description) {
        super(name, description);
        this.isComposer = true;
    }

    public Composer() {
    }
}

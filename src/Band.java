public class Band extends Artist {
    private boolean isBand = false;

    @Override
    public boolean isBand() {
        return isBand;
    }

    public Band(String name, String description) {
        super(name, description);
        this.isBand = true;
    }

    public Band() {
    }
}

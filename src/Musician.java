public class Musician extends Artist{
    private boolean isMusician = false;

    @Override
    public boolean isMusician() {
        return isMusician;
    }

    public Musician(String name, String description) {
        super(name, description);
        this.isMusician = true;
    }

    public Musician() {
    }
}

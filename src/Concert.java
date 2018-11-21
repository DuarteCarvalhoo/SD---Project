public class Concert {
    private String name, location;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Concert(String location,String name) {
        this.location = location;
        this.name = name;
    }
}

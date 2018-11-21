public class FileArchive {
    private String path;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public FileArchive(String path) {
        this.path = path;
    }
}

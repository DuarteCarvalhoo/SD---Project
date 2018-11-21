public class Notification {
    private String text;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Notification(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}

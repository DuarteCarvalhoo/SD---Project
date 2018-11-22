import java.io.Serializable;

public class Critic implements Serializable {
    private double score;
    private String text;
    private String user;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public double getScore() {
        return score;
    }

    public String getText() {
        return text;
    }

    public String toString(){
        return getUser() + ", " + getText() + ", "+ getScore();
    }

    public Critic(double score, String text, String user) {
        this.score = score;
        this.text = text;
        this.user = user;
    }
}

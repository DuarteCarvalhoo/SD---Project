import java.io.Serializable;

public class Critic implements Serializable {
    private double score;
    private String text;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getScore() {
        return score;
    }

    public String getText() {
        return text;
    }

    public String toString(){
        return getScore() + " - " + getText();
    }

    public Critic(double score, String text) {
        this.score = score;
        this.text = text;
    }
}

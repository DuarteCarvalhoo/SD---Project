public class Critic {
    private double score;
    private String text;

    public double getScore() {
        return score;
    }

    public String getText() {
        return text;
    }

    public Critic(double score, String text) {
        this.score = score;
        this.text = text;
    }
}

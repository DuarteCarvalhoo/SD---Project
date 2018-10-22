import java.util.*;

public class Album {
    private ArrayList<Music> musicsList;
    private ArrayList<Critic> criticsList = new ArrayList<>();
    private double sum;
    private Artist artist;
    private String description;


    public void addCritic(Critic c){
        this.criticsList.add(c);
    }

    public void changeDescription(String d){
        this.description = d;
    }

    public ArrayList<Music> getMusicsList() {
        return musicsList;
    }

    public ArrayList<Critic> getCriticsList() {
        return criticsList;
    }

    public Artist getArtist() {
        return artist;
    }

    public double getAverageScore() {
        if(criticsList.isEmpty()){
            return 0;
        }
        else{
            for(Critic critic : criticsList){
                sum+=critic.getScore();
            }
        }
        return (sum/criticsList.size());
    }

    public String getDescription() {
        return description;
    }

    public Album(ArrayList<Music> musicsList, Artist artist, String description) {
        this.musicsList = musicsList;
        this.artist = artist;
        this.description = description;
    }
}

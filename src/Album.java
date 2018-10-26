import java.io.Serializable;
import java.util.*;

public class Album implements Serializable{
    private ArrayList<Music> musicsList = new ArrayList<>();
    private ArrayList<Critic> criticsList = new ArrayList<>();
    private double sum;
    private String name;
    private Artist artist;
    private String description;
    private String duracao;


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

    public String getName(){
        return name;
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

    public void addMusic(Music m){
        this.musicsList.add(m);
    }

    public void removeMusic(Music m){
        int i;
        for(i=0;i<this.musicsList.size();i++){
            if(this.musicsList.get(i).equals(m)){
                this.musicsList.remove(this.musicsList.get(i));
            }
        }
    }

    public String getDescription() {
        return description;
    }

    public void setMusicsList(ArrayList<Music> musicsList) {
        this.musicsList = musicsList;
    }

    public void setCriticsList(ArrayList<Critic> criticsList) {
        this.criticsList = criticsList;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public String getDuracao() {
        return duracao;
    }

    public void setDuracao(String duracao) {
        this.duracao = duracao;
    }

    public void setDescription(String description) {
        this.description = description;
    }





    public Album(String name, Artist artist, String description, String duracao) {
        this.name = name;
        this.artist = artist;
        this.description = description;
        this.duracao = duracao;
    }

    public Album(){

    }
}

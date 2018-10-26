import java.io.Serializable;
import java.util.*;

public class Album implements Serializable{
    private ArrayList<Music> musicsList = new ArrayList<>();
    private ArrayList<Critic> criticsList = new ArrayList<>();
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
        double soma = 0;
        for(Critic c : criticsList){
            soma+=c.getScore();
        }
        return (soma/criticsList.size());
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

    public String printMusics(ArrayList<Music> musics){
        String finalString = "";
        if(musics.isEmpty()){
            finalString += "No musics to show.";
        }
        else{
            for(Music music : musics){
                finalString += music.toString();
                finalString+="\n";
            }
        }
        return finalString;
    }

    public String printCritics(ArrayList<Critic> critics){
        String finalString = "";
        if(critics.isEmpty()){
            finalString += "No critics to show.";
        }
        else {
            for (Critic critic : critics) {
                finalString += critic.toString();
                finalString += "\n";
            }
        }
        return finalString;
    }

    @Override
    public String toString(){
        return
                "Name: "+getName()+"\n"
                +"Artist: "+getArtist().getName()+"\n"
                +"Description: "+getDescription()+"\n"
                +"Score: "+getAverageScore()+"\n"
                +"Duration: "+getDuracao()+" segundos\n\n"
                +printCritics(this.criticsList)+"\n\n"
                +printMusics(this.musicsList);
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

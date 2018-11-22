import sun.plugin.javascript.navig.Array;

import java.io.Serializable;
import java.util.*;

public class Album implements Serializable{
    private int id;
    private String name;
    private String description;
    private int length;
    private String genre;
    private String artist;
    private String publisher;
    private double score;
    private ArrayList<Critic> criticsList = new ArrayList<>();
    private ArrayList<Music> musicsList = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void changeDescription(String d){
        this.description = d;
    }

    public String getName(){
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String printMusics(ArrayList<Music> musics){
        String finalString = "";
        if(musics.isEmpty()){
            finalString += "No musics to show.";
        }
        else {
            int i = 1;
            for (Music music : musics) {
                finalString += i+". ";
                finalString += music.toString();
                finalString += "\n";
                i++;
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
            int i = 1;
            for (Critic critic : critics) {
                finalString += i+". ";
                finalString += critic.toString();
                finalString += "\n";
                i++;
            }
        }
        return finalString;
    }

    @Override
    public String toString() {
        return "Name: " + name + "\n" +
                "Artist: " + artist + "\n" +
                "Description:" + description + "\n" +
                "Length: " + length + "\n" +
                "Genre: " + genre + "\n" +
                "Score: "+ score + "\n" +
                "Publisher: "+ publisher + "\n" +
                "Critics: \n" + printCritics(criticsList) + "\n" +
                "Musics: \n" + printMusics(musicsList);
    }

    public Album(String name, String artist, String description, int length, String genre, Double score, ArrayList<Critic> criticsList, ArrayList<Music>musicsList, String publisher) {
        this.name = name;
        this.artist = artist;
        this.description = description;
        this.length = length;
        this.genre = genre;
        this.score = score;
        this.criticsList = criticsList;
        this.musicsList = musicsList;
        this.publisher = publisher;
    }

    public Album(){

    }
}

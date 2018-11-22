import sun.plugin.javascript.navig.Array;

import java.io.Serializable;
import java.util.*;

public class Album implements Serializable{
    private int id;
    private String name;
    private String description;
    private String length;
    private String genre;


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




    public Album(String name, String description, String length, String genre) {
        this.name = name;
        this.description = description;
        this.length = length;
        this.genre = genre;
    }

    public Album(){

    }
}

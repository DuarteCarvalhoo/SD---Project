import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable{
    private String username;
    private String password;
    private int id;
    private boolean editor = false;
    private ClientHello clientInterface;
    private ArrayList<String> downloadableMusics = new ArrayList<>();
    private ArrayList<String> notifications = new ArrayList<>();

    public void setClientInterface(ClientHello aux){
        clientInterface = aux;
    }

    public String getUsername() {
        return username;
    }

    public ClientHello getInterface(){
        return clientInterface;
    }

    public String printDownloadableMusicsLogin(){
        String finalString = "";
        if(getDownloadableMusics().isEmpty()){
            finalString += "No musics to show.";
        }
        else{
            for(String music : getDownloadableMusics()){
                finalString += music;
                finalString += "|";
            }
        }
        return finalString;
    }

    public String printDownloadableMusics(){
        String finalString = "";
        if(getDownloadableMusics().isEmpty()){
            finalString += "No musics to show.";
        }
        else{
            for(int i=0;i<getDownloadableMusics().size();i++){
                if(i==getDownloadableMusics().size()-1){
                    finalString += getDownloadableMusics().get(i);
                }
                else{
                    finalString += getDownloadableMusics().get(i);
                    finalString += ",";
                }
            }
        }
        return finalString;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEditor(boolean editor) {
        this.editor = editor;
    }

    public void addDownloadableMusic(String musicName){
        this.downloadableMusics.add(musicName);
    }

    public ArrayList<String> getDownloadableMusics() {
        return downloadableMusics;
    }

    public void addNotification(String newNotif){
        notifications.add(newNotif);
    }

    public ArrayList<String> getNotifications(){
        return notifications;
    }

    public int getId() {
        return id;
    }

    public ClientHello getClientInterface() {
        return clientInterface;
    }

    public void cleanNotification (){
        notifications = new ArrayList<String>();
    }

    public boolean checkPassword(String password){
        if(getPassword().equals(password)){
            return true;
        }
        return false;
    }

    public void makeEditor() {
        this.editor = true;
    }

    public boolean isEditor(){
        return editor;
    }

    public String toString(){
        return "Username: " + getUsername() + " Password: " + getPassword();
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(int id, String username, boolean editor){
        this.id = id;
        this.username = username;
        this.editor = editor;
    }

    public User(){
        this.username = "none";
        this.password = "none";
    }

}
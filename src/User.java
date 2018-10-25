import java.io.Serializable;

public class User implements Serializable{
    private String username;
    private String password;
    private boolean online = false;
    private boolean editor = false;


    public String getUsername() {
        return username;
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

    public boolean isOnline(){
        return online;
    }

    public void setOffline() {
        this.online = false;
    }

    public void setOnline(){
        this.online = true;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, boolean editor, boolean online) {
        this.username = username;
        this.password = password;
        this.editor = editor;
        this.online = online;
    }

    public User(String username, String password, boolean online) {
        this.username = username;
        this.password = password;
        this.online = online;
    }

    public User(){
        this.username = "none";
        this.password = "none";
    }
}
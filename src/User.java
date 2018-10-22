import java.io.Serializable;

public class User implements Serializable{
    private String username;
    private String password;
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

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
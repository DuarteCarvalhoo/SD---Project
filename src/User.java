import java.io.Serializable;

public class User implements Serializable{
    private final String username;
    private final String password;
    private boolean editor = false;


    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
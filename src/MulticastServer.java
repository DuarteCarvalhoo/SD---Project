import java.io.IOException;
import java.net.*;
import java.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

public class MulticastServer extends Thread implements Serializable {
    private String MULTICAST_ADDRESS = "224.0.224.0";
    private int PORT = 4321;
    private long SLEEP_TIME = 5000;
    private ArrayList<User> usersList = new ArrayList<>();
    private ArrayList<User> usersLogged = new ArrayList<>();

    public static void main(String[] args){
        MulticastServer server = new MulticastServer();
        server.start();
    }

    public MulticastServer(){ super ("Server " + (long) (Math.random()*1000));}

    public void run(){
        MulticastSocket socket = null;
        Runtime.getRuntime().addShutdownHook(new catchCtrlC(usersList));
        //System.out.println(this.getName() + "run...");

        try {
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            usersList = readFiles();
            if(usersList.isEmpty()){
                System.out.println("Arraylist empty!");
            }
            while(true){
                System.out.println("INICIO");
                byte[] bufferRec = new byte[256];
                DatagramPacket packetRec = new DatagramPacket(bufferRec, bufferRec.length);
                socket.setLoopbackMode(false);
                socket.receive(packetRec);
                System.out.print("De: "+ packetRec.getAddress().getHostAddress() + ":" + packetRec.getPort() + " com a mensagem: ");
                String msg = new String(packetRec.getData(), 0, packetRec.getLength());
                System.out.println(msg);
                //try { sleep((long) (Math.random() * SLEEP_TIME)); } catch (InterruptedException e) { }
                String[] aux = msg.split(";");
                switch (aux[0]){
                    case "type|login":
                        String [] loginUsernameParts = aux[1].split("\\|");
                        String [] loginPasswordParts = aux[2].split("\\|");
                        String user = loginUsernameParts[1];
                        String pass = loginPasswordParts[1];
                        System.out.println("USERNAME: " + user + " PASSWORD: " + pass);
                        if(!usersLogged.isEmpty()) {
                            for (User u : usersLogged) {
                                if (user.equals(u.getUsername())) {
                                    sendMsg(socket, "type|loginFail");
                                    System.out.println("ERRO: User already logged in.");
                                }
                            }
                        }
                        else {
                            boolean loggedInSuccessfully = checkUsernameLogin(user, pass);
                            if (!loggedInSuccessfully) {
                                sendMsg(socket, "type|loginFail");
                                System.out.println("ERRO: Login não completo.");
                            } else {
                                usersLogged.add(new User(user,pass));
                                sendMsg(socket, "type|loginComplete");
                                System.out.println("SUCESSO: Login Completo");
                            }
                        }
                        //funçao passa como argumentos o user e pw
                        //funçao pra confirmar se o user existe, se a pw ta certa e por fim enviar a resposta
                        break;
                    case "type|register":
                        String aux2 = aux[1];
                        String [] registerUsernameParts = aux2.split("\\|");
                        String [] registerPasswordParts = aux[2].split("\\|");
                        String username = registerUsernameParts[1];
                        String password = registerPasswordParts[1];
                        System.out.println("USERNAME: " + username + " PASSWORD: " + password);
                        int usernameUsed = checkUsernameRegister(username);
                        if(usernameUsed == 1) {
                            sendMsg(socket, "type|usernameUsed");
                            System.out.println("ERRO: Username já usado.");
                        }
                        else if(usernameUsed == -1){
                            User newUser = new User(username, password);
                            newUser.makeEditor();
                            System.out.println("Editor permisions given.");
                            usersList = writeFilesUser(usersList,newUser);
                            System.out.println("SUCCESS: User added to database with username: '" + username + "' and password '" + password +"'");
                            sendMsg(socket, "type|registComplete");
                        }
                        else {
                            User newUser = new User(username, password);
                            usersList = writeFilesUser(usersList,newUser);
                            System.out.println("SUCESSO: Adicionou ao arraylist com user '" + username + "' e password '" + password +"'");
                            sendMsg(socket, "type|registComplete");
                        }
                        //funçao passa como argumentos o user e pw
                        //na funçao verificar se nao ha users iguais, se nao guardar no arraylist (se usarmos 2 pws ver se sao iguais) e enviar a resposta
                        break;
                    case "type|pesquisa":
                        //dentro da funçao decidir o que pesquisa artista, estilo ou album
                        break;
                    default:
                        System.out.println("Default");
                        break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    public boolean checkUsernameLogin(String username, String password){
        for (User user : usersList) {
            if(user.getUsername().equals(username)){
                if(user.checkPassword(password)){
                    return true;
                }
            }
        }
        return false;
    }

    public int checkUsernameRegister(String username){
        if(usersList.isEmpty()){
            return -1;
        }
        else {
            for (User user : usersList) {
                if (user.getUsername().equals(username)) {
                    return 1;
                }
            }
        }
        return 0;
    }

    private void sendMsg(MulticastSocket socket, String msg) throws IOException {
        byte[] buffer = msg.getBytes();
        socket.setLoopbackMode(true);

        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
        socket.send(packet);
    }

    private ArrayList<User> readFiles(){
        System.out.println("Reading.");
        ArrayList<User> users = new ArrayList<>();
        try {
            ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream("data.bin")));
            users = (ArrayList) objectIn.readObject();
            objectIn.close();
            System.out.println("Read users file successfully.");
            return users;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Empty file!");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return users;
    }

    public ArrayList<User> writeFilesUser(ArrayList<User> usersList, User newUser){
        System.out.println("Writing.");
        usersList.add(newUser);
        try{
            File file = new File("data.bin");
            FileOutputStream out = new FileOutputStream(file);
            ObjectOutputStream fout = new ObjectOutputStream(out);
            fout.writeObject(usersList);
            fout.close();
            out.close();
        }
        catch (FileNotFoundException ex) {
            Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("File not found!");
        } catch (IOException ex) {
            Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Users list wrote successfully.");
        return usersList;
    }
}

class catchCtrlC extends Thread {
    ArrayList<User> users = new ArrayList<>();
    public catchCtrlC(ArrayList<User> usersList) {
        this.users = usersList;
    }

    @Override
    public void run() {
        for(User user : users) {
            if (user == null) {
                System.out.println("rip");
            } else {
                System.out.println(user);

            }
        }
        //MulticastServer.writeFiles(users);
        System.out.println("Escreveu no ficheiro de objetos.");
    }
}
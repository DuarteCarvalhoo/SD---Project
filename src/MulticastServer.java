import java.io.*;
import java.net.*;
import java.util.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MulticastServer extends Thread implements Serializable {
    private String MULTICAST_ADDRESS = "224.0.224.0";
    private int PORT = 4321;
    private long SLEEP_TIME = 5000;
    public ArrayList<User> usersList = new ArrayList<>();

    public static void main(String[] args){
        MulticastServer server = new MulticastServer();
        server.start();
    }

    public MulticastServer(){ super ("Server " + (long) (Math.random()*1000));}

    public void run() {
        MulticastSocket socket = null;
        Runtime.getRuntime().addShutdownHook(new catchCtrlC(usersList));
        //System.out.println(this.getName() + "run...");

        try {
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            usersList = readFiles();
            if (usersList.isEmpty()) {
                System.out.println("Arraylist empty!");
            }
            String aux2= "";
            Socket socketHelp= null;
            while (true) {
                System.out.println("INICIO");
                byte[] bufferRec = new byte[256];
                DatagramPacket packetRec = new DatagramPacket(bufferRec, bufferRec.length);
                socket.receive(packetRec);
                System.out.print("De: " + packetRec.getAddress().getHostAddress() + ":" + packetRec.getPort() + " com a mensagem: ");
                String msg = new String(packetRec.getData(), 0, packetRec.getLength());
                System.out.println(msg);
                //try { sleep((long) (Math.random() * SLEEP_TIME)); } catch (InterruptedException e) { }
                String[] aux = msg.split(";");
                if (!(aux[0].equals("type|login")) && !(aux[0].equals("type|register")) && !(aux[0].equals("type|logout"))) {
                    System.out.println("Rejected");
                } else {
                    switch (aux[0]) {
                        case "type|login":
                            boolean flag = false;
                            String[] loginUsernameParts = aux[1].split("\\|");
                            String[] loginPasswordParts = aux[2].split("\\|");
                            String user = loginUsernameParts[1];
                            String pass = loginPasswordParts[1];
                            System.out.println("USERNAME: " + user + " PASSWORD: " + pass);
                            if (usersList.isEmpty()) {
                                sendMsg("type|loginFail");
                                System.out.println("ERRO: No users on the database.");
                            } else {
                                for (User u : usersList) {
                                    if (user.equals(u.getUsername())) {
                                        if (u.isOnline()) {
                                            sendMsg("type|loginFail");
                                            System.out.println("ERRO: User already logged in.");
                                        } else {
                                            boolean loggedInSuccessfully = checkUsernameLogin(user, pass);
                                            if (!loggedInSuccessfully) {
                                                sendMsg("type|loginFail");
                                                System.out.println("ERRO: Login não completo.");
                                            } else {
                                                u.setOnline();
                                                sendMsg("type|loginComplete;username|" + u.getUsername() + ";password|" + u.getPassword() + ";editor|" + u.isEditor() + ";online|" + u.isOnline());
                                                System.out.println("SUCESSO: Login Completo");
                                                flag = true;
                                            }
                                        }
                                    }
                                }
                                if (!flag) {
                                    System.out.println("Username not found.");
                                    sendMsg("type|loginFail");
                                }
                            }
                            //funçao passa como argumentos o user e pw
                            //funçao pra confirmar se o user existe, se a pw ta certa e por fim enviar a resposta
                            break;
                        case "type|register":
                            aux2 = aux[1];
                            String[] registerUsernameParts = aux2.split("\\|");
                            String[] registerPasswordParts = aux[2].split("\\|");
                            String username = registerUsernameParts[1];
                            String password = registerPasswordParts[1];
                            System.out.println("USERNAME: " + username + " PASSWORD: " + password);
                            int usernameUsed = checkUsernameRegister(username);
                            if (usernameUsed == 1) {
                                sendMsg("type|usernameUsed");
                                System.out.println("ERRO: Username já usado.");
                            } else if (usernameUsed == -1) {
                                User newUser = new User(username, password);
                                newUser.makeEditor();
                                System.out.println("Editor permisions given.");
                                usersList = writeFilesUser(usersList, newUser);
                                System.out.println("SUCCESS: User added to database with username: '" + username + "' and password '" + password + "'");
                                sendMsg("type|registComplete");
                            } else {
                                User newUser = new User(username, password);
                                usersList = writeFilesUser(usersList, newUser);
                                System.out.println("SUCESSO: Adicionou ao arraylist com user '" + username + "' e password '" + password + "'");
                                sendMsg("type|registComplete");
                            }
                            //funçao passa como argumentos o user e pw
                            //na funçao verificar se nao ha users iguais, se nao guardar no arraylist (se usarmos 2 pws ver se sao iguais) e enviar a resposta
                            break;
                        case "type|turnOnSocket":
                            aux2 = aux[1];
                            String[] address = aux2.split("\\|");
                            socketHelp = ligarSocket(address[1]);
                            //String[] musicName = aux[2].split("\\|");
                            //receiveMusic(address[1], musicName[1]);
                            sendMsg("Music saving on the server.");
                            break;
                        case "type|sendMusic":
                            aux2 = aux[1];
                            String[] musicName = aux2.split("\\|");
                            receiveMusic(socketHelp, musicName[1]);
                            sendMsg("it worked out");
                        case "type|logout":
                            boolean flag1 = false;
                            System.out.println("tou a dar logout");
                            String[] logoutUsername = aux[1].split("\\|");
                            String logoutUser = logoutUsername[1];
                            if (!logoutUser.equals("none")) {
                                for (User u : usersList) {
                                    if (u.getUsername().equals(logoutUser)) {
                                        u.setOffline();
                                        sendMsg("type|logoutComplete");
                                        flag1 = true;
                                    }
                                }
                                if (!flag1) {
                                    System.out.println("erro nos ficheiros");
                                    sendMsg("type|logoutFail");
                                }
                            } else {
                                System.out.println("No user is logged in.");
                                sendMsg("type|logoutFail");
                            }
                            break;
                        //dentro da funçao decidir o que pesquisa artista, estilo ou album
                        default:
                            System.out.println("Default");
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    private Socket ligarSocket(String address) throws IOException {
        Socket socket = new Socket(address,5000);
        return socket;
    }

    private void receiveMusic(Socket socket, String musicName) throws IOException {
        byte[] b= new byte[2002];
        InputStream is = socket.getInputStream();
        FileOutputStream fOutStream = new FileOutputStream("C:\\Users\\Duarte\\Desktop\\SD\\PROJETO\\META 1\\SD---Project\\musicasServer\\" + musicName);
        is.read(b, 0, b.length);
        fOutStream.write(b, 0, b.length);
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

    private void sendMsg(String msg) throws IOException {
        MulticastSocket socket = new MulticastSocket();
        byte[] buffer = msg.getBytes();

        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        socket.send(packet);
        socket.close();
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
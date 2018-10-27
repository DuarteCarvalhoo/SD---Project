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
    private ArrayList<User> usersList = new ArrayList<>();
    private ArrayList<Artist> artistsList = new ArrayList<>();
    private ArrayList<Album> albunsList = new ArrayList<>();
    private ArrayList<Music> musicsList = new ArrayList<>();

    public static void main(String[] args){
        MulticastServer server = new MulticastServer();
        server.start();
    }

    public MulticastServer(){ super ("Server " + (long) (Math.random()*1000));}

    public void run() {
        MulticastSocket socket = null;
        //System.out.println(this.getName() + "run...");

        try {
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            readFiles();
            if (usersList.isEmpty()) {
                System.out.println("Users arraylist empty!");
            }
            if(artistsList.isEmpty()){
                System.out.println("Artists arraylist empty!");
            }
            if(albunsList.isEmpty()){
                System.out.println("Albuns arraylist empty!");
            }
            String aux2= "";
            Socket socketHelp= null;
            ServerSocket auxSocket = null;
            while (true) {
                System.out.println("_______________________________________________________________________________________________________");
                byte[] bufferRec = new byte[256];
                DatagramPacket packetRec = new DatagramPacket(bufferRec, bufferRec.length);
                socket.receive(packetRec);
                System.out.print("De: " + packetRec.getAddress().getHostAddress() + ":" + packetRec.getPort() + " com a mensagem: ");
                String msg = new String(packetRec.getData(), 0, packetRec.getLength());
                System.out.println(msg);
                //try { sleep((long) (Math.random() * SLEEP_TIME)); } catch (InterruptedException e) { }
                String[] aux = msg.split(";");
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
                                            sendMsg("type|loginComplete;username|" + u.getUsername() + ";password|" + u.getPassword() + ";editor|" + u.isEditor() + ";online|" + u.isOnline()+";Downloads|"+u.printDownloadableMusicsLogin());
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
                        String username;
                        String password;
                        try {
                            aux2 = aux[1];
                            String[] registerUsernameParts = aux2.split("\\|");
                            String[] registerPasswordParts = aux[2].split("\\|");
                            username = registerUsernameParts[1];
                            password = registerPasswordParts[1];
                            System.out.println("USERNAME: " + username + " PASSWORD: " + password);
                        }catch (Exception e){
                            return;
                        }
                        int usernameUsed = checkUsernameRegister(username);
                        if (usernameUsed == 1) {
                            sendMsg("type|usernameUsed");
                            System.out.println("ERRO: Username já usado.");
                        } else if (usernameUsed == -1) {
                            User newUser = new User(username, password);
                            newUser.makeEditor();
                            System.out.println("Editor permisions given.");
                            usersList.add(newUser);
                            writeFiles();
                            System.out.println("SUCCESS: User added to database with username: '" + username + "' and password '" + password + "'");
                            sendMsg("type|registComplete");
                        } else {
                            User newUser = new User(username, password);
                            usersList.add(newUser);
                            writeFiles();
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
                        User loggedUser;
                        Artist artisT;
                        String[] loggedUserParts = aux[8].split("\\|");
                        String[] pathParts = aux[1].split("\\|");
                        String[] nameParts = aux[2].split("\\|");
                        String[] composerParts = aux[3].split("\\|");
                        String[] artistParts = aux[4].split("\\|");
                        String[] durationParts = aux[5].split("\\|");
                        String[] albumParts = aux[6].split("\\|");
                        String[] genreParts = aux[7].split("\\|");
                        Music music = new Music(pathParts[1],nameParts[1],composerParts[1],artistParts[1],durationParts[1],albumParts[1],genreParts[1]);
                        if(checkUsernameRegister(loggedUserParts[1])!=1){
                            sendMsg("type|userNotFound");
                        }
                        else if(!checkArtistExists(artistParts[1])){
                            sendMsg("type|artistNotFound");
                        }
                        else if(!checkAlbumExists(albumParts[1],artistParts[1])){
                            sendMsg("type|albumNotFound");
                        }
                        else if(checkMusicExists(nameParts[1],artistParts[1])){
                            sendMsg("type|musicExists");
                        }
                        else{
                            musicsList.add(music);
                            artisT = returnsArtist(artistParts[1]);
                            for(Album album2 : artisT.getAlbums()){
                                if(album2.getName().trim().equals(albumParts[1])){
                                    album2.addMusic(music);
                                    System.out.println("Music added to artist's album.");
                                }
                            }
                            loggedUser = returnsUser(loggedUserParts[1]);
                            loggedUser.addDownloadableMusic(music.getTitle());
                            System.out.println("Music added to downloadable musics");
                            writeFiles();
                            receiveMusic(socketHelp, nameParts[1]);
                            sendMsg("type|sendMusicComplete;MusicAdded|"+music.getTitle());
                        }
                        break;
                    case "type|shareMusic":
                        User userW;
                        String[] userParts = aux[1].split("\\|");
                        String[] musicParts = aux[2].split("\\|");
                        userW = returnsUser(userParts[1]);
                        boolean flagS = false;
                        for(String musicS : userW.getDownloadableMusics()){
                            if(musicS.trim().equals(musicParts[1])){
                                sendMsg("type|isAlreadyDownloadable");
                                System.out.println("ERRO: User already has permission to do that.");
                                flagS = true;
                            }
                        }
                        if(!flagS){
                            userW.addDownloadableMusic(musicParts[1]);
                            sendMsg("type|musicShareCompleted");
                        }
                        break;
                    case "type|openSocket":
                        auxSocket = openSocket();
                        System.out.println("oi");
                        sendMsg("ServerSocket inicializada");
                        break;
                    case"type|downloadMusic":
                        aux2 = aux[1];
                        String[] direc = aux2.split("\\|");
                        sendMsg(sendMusicMulticast(direc[1], auxSocket));
                        break;
                    case "type|makeEditor":
                        boolean flagEditor = false;
                        String []parts = aux[1].split("\\|");
                        System.out.println("User: "+parts[1]);
                        if(usersList.isEmpty()){
                            sendMsg("type|makingEditorFail");
                            System.out.println("ERROR: No users on the database.");
                        }
                        else{
                            for(User u : usersList){
                                if(u.getUsername().equals(parts[1])){
                                    if(u.isEditor()){
                                        sendMsg("type|makingEditorFail");
                                        System.out.println("ERROR: "+parts[1]+" is already an editor.");
                                        flagEditor = true;
                                    }
                                    else{
                                        u.makeEditor();
                                        sendMsg("type|makingEditorComplete");
                                        System.out.println("SUCCESS: User "+parts[1]+" made editor.");
                                        flagEditor = true;
                                    }
                                }
                            }
                            if(!flagEditor){
                                System.out.println("Username not found.");
                                sendMsg("type|makingEditorFail");
                            }
                        }
                        break;
                    case "type|createArtist":
                        String[] nameParts1 = aux[1].split("\\|");
                        String[] genreParts1 = aux[2].split("\\|");
                        String[] descriptionParts = aux[3].split("\\|");
                        boolean artistExists = checkArtistExists(nameParts1[1]);
                        if (artistExists) {
                            sendMsg("type|artistExists");
                            System.out.println("ERRO: Artist already exists.");
                        }
                        else {
                            Artist newArtist = new Artist(nameParts1[1],descriptionParts[1],genreParts1[1]);
                            artistsList.add(newArtist);
                            writeFiles();
                            System.out.println("SUCESSO: Adicionou ao arraylist com nome '" + nameParts1[1] + "', genre '" + genreParts1[1] + "' e descrição '"+descriptionParts[1]+"'");
                            sendMsg("type|createArtistComplete");
                        }
                        break;
                    case "type|createAlbum":
                        Artist artist = new Artist();
                        String[] namePa = aux[1].split("\\|");
                        String[] aName = aux[2].split("\\|");
                        String[] descripParts = aux[3].split("\\|");
                        String[] duracaoParts = aux[4].split("\\|");
                        if(checkAlbumExists(namePa[1],aName[1])){
                            sendMsg("type|albumExists");
                            System.out.println("ERRO: Album already exists.");
                        }
                        else{
                            boolean flagAlbum = false;
                            for(Artist a : artistsList){
                                if(a.getName().equals(aName[1])){
                                    artist = a;
                                    flagAlbum=true;
                                }
                            }
                            if(!flagAlbum){
                                sendMsg("type|albumNotFound");
                            }
                            else {
                                Album newAlbum = new Album(namePa[1], artist, descripParts[1], duracaoParts[1]);
                                albunsList.add(newAlbum);

                                boolean flagAddToArtist = false;
                                for (Artist a : artistsList) {
                                    if (a.getName().equals(aName[1])) {
                                        a.getAlbums().add(newAlbum);
                                        sendMsg("type|createAlbumComplete");
                                        flagAddToArtist = true;
                                    }
                                }
                                if(!flagAddToArtist){
                                    sendMsg("type|createAlbumFailed");
                                    System.out.println("Falhou.");
                                }
                            }
                        }
                        break;
                    case "type|editArtistName":
                        String[] nameBeforeParts = aux[1].split("\\|");
                        String[] nameAfterParts = aux[2].split("\\|");
                        if(!checkArtistExists(nameBeforeParts[1])){
                            sendMsg("type|nameNotChanged");
                            System.out.println("ERROR: Artist Not Found -> Name Not Found.");
                        }
                        else{
                            for(Artist a : artistsList){
                                if(a.getName().equals(nameBeforeParts[1])){
                                    a.setName(nameAfterParts[1]);
                                    sendMsg("type|nameChanged");
                                    System.out.println("SUCCESS: Name Changed.");
                                }
                            }
                        }
                        break;
                    case "type|editArtistGenre":
                        String[] artistNameParts = aux[1].split("\\|");
                        String[] genreAfterParts = aux[2].split("\\|");
                        if(!checkArtistExists(artistNameParts[1])){
                            sendMsg("type|genreNotChanged");
                            System.out.println("ERROR: Artist Not Found -> Genre Not Found.");
                        }
                        else{
                            for(Artist a : artistsList){
                                if(a.getName().equals(artistNameParts[1])){
                                    a.setGenre(genreAfterParts[1]);
                                    sendMsg("type|genreChanged");
                                    System.out.println("SUCCESS: Genre Changed.");
                                }
                            }
                        }
                        break;
                    case "type|editArtistDescription":
                        String[] artistNamePartss = aux[1].split("\\|");
                        String[] descriptionAfterParts = aux[2].split("\\|");
                        if(!checkArtistExists(artistNamePartss[1])){
                            sendMsg("type|descriptionNotChanged");
                            System.out.println("ERROR: Artist Not Found -> Description Not Changed.");
                        }
                        else{
                            for(Artist a : artistsList){
                                if(a.getName().equals(artistNamePartss[1])){
                                    a.setDescription(descriptionAfterParts[1]);
                                    sendMsg("type|descriptionChanged");
                                    System.out.println("SUCCESS: Description Changed.");
                                }
                            }
                        }
                        break;
                    case "type|deleteArtist":
                        String[] nameP = aux[1].split("\\|");
                        String name = nameP[1];
                        if(!checkArtistExists(nameP[1])){
                            sendMsg("type|artistNotFound");
                            System.out.println("ERROR: Artist Not Found.");
                        }
                        else{
                            int i;
                            for(i=0;i<artistsList.size();i++){
                                if(artistsList.get(i).getName().equals(name)){
                                    artistsList.remove(artistsList.get(i));
                                    sendMsg("type|deleteArtistComplete");
                                    System.out.println("SUCCESS: Artist deleted.");
                                }
                            }
                        }
                        break;
                    case "type|showArtist":
                        /*String[] nameArtist = aux[1].split("\\|");
                        String n = nameArtist[1];
                        if(!checkArtistExists(n)){
                            sendMsg("type|showArtistFail");
                            System.out.println("ERROR: Artist Not Found.");
                        }
                        else{
                            String albuns = "-No albuns";
                            for(Artist a : artistsList){
                                if(a.getName().equals(n)){
                                    int i;
                                    if(a.getAlbums().size()>0){
                                        String[] nomesAlbuns = new String[a.getAlbums().size()];
                                        for(i=0;i<a.getAlbums().size();i++){
                                            nomesAlbuns[i] = a.getAlbums().get(i).getName();
                                        }
                                        albuns = printAlbunsProtocol(nomesAlbuns);
                                    }
                                    sendMsg("type|showArtistComplete;"+"Name|"+a.getName()+";Genre|"+a.getGenre()+";Description|"+a.getDescription()+";Album|"+albuns);
                                    System.out.println("SUCCESS: Artist Shown.");
                                }
                            }
                        }*/
                        String[] nameArtist = aux[1].split("\\|");
                        String n = nameArtist[1];
                        Artist art = new Artist();
                        if(artistsList.isEmpty()){
                            sendMsg("type|showArtistFail");
                            System.out.println("ERROR: No Artists on the database.");
                        }
                        else {
                            if (!checkArtistExists(n)) {
                                sendMsg("type|showArtistFail");
                                System.out.println("ERROR: Artist Not Found.");
                            } else {
                                for(Artist a : artistsList){
                                    if(a.getName().equals(n)){
                                        art = a;
                                    }
                                }
                                sendMsg("type|showArtistComplete;Artist|"+art);
                            }
                        }
                        break;
                    case "type|showArtistAlbums":
                        String[] nameA = aux[1].split("\\|");
                        String nA = nameA[1];
                        Artist artista = new Artist();
                        if(!checkArtistExists(nA)){
                            sendMsg("type|showArtistAlbumsFail");
                            System.out.println("ERROR: Artist Not Found.");
                        }
                        else{
                            for(Artist a : artistsList){
                                if(a.getName().equals(nA)){
                                    artista = a;
                                }
                            }
                            sendMsg("type|showArtistAlbumsComplete"+";Albums|"+artista.printAlbums(artista.getAlbums()));
                            System.out.println("SUCCESS: Artist Albums Shown.");
                        }
                        break;
                    case "type|makeCritic":
                        Album newAlbum = new Album();
                        String[] scoreParts = aux[1].split("\\|");
                        String[] textParts = aux[2].split("\\|");
                        String[] albumParts1 = aux[3].split("\\|");
                        Critic c = new Critic(Double.parseDouble(scoreParts[1]),textParts[1]);
                        if(albunsList.isEmpty()){
                            sendMsg("type|criticFail");
                            System.out.println("ERROR: No Albums in the database.");
                        }
                        else {
                            for (Album a : albunsList) {
                                if (a.getName().equals(albumParts1[1])) {
                                    newAlbum = a;
                                }
                            }
                            if(checkAlbumExists(albumParts1[1],newAlbum.getArtist().getName())){
                                newAlbum.addCritic(c);
                                sendMsg("type|criticComplete");
                                System.out.println("Critic Complete.");
                            }
                            else{
                                sendMsg("type|criticFail");
                                System.out.println("Critic not Complete. Album not found.");
                            }
                        }
                        break;
                    case "type|showAlbum":
                        Album album = new Album();
                        String[] albumNameParts = aux[1].split("\\|");
                        String albumName = albumNameParts[1];
                        if(albunsList.isEmpty()){
                            sendMsg("type|showAlbumFail");
                            System.out.println("ERROR: No Albums in the database.");
                        }
                        else {
                            for (Album a : albunsList) {
                                if (a.getName().equals(albumName)) {
                                    album = a;
                                }
                            }
                            if(checkAlbumExists(albumName, album.getName())){
                                sendMsg("type|albumAlreadyExists");
                                System.out.println("ERROR: Album Not Found.");
                            }
                            else{
                                sendMsg("type|showAlbumComplete"+";Album|"+album);
                            }
                        }
                        break;
                    case "type|logout":
                        boolean flagLogout = false;
                        System.out.println("Logging out.");
                        String[] logoutUsername = aux[1].split("\\|");
                        String logoutUser = logoutUsername[1];
                        if (!logoutUser.equals("none")) {
                            for (User u : usersList) {
                                if (u.getUsername().equals(logoutUser)) {
                                    u.setOffline();
                                    //readFiles();
                                    writeFiles();
                                    sendMsg("type|logoutComplete");
                                    flagLogout = true;
                                }
                            }
                            if (!flagLogout) {
                                System.out.println("User not found");
                                sendMsg("type|logoutFail");
                            }
                        } else {
                            System.out.println("No user is logged in.");
                            sendMsg("type|logoutFail");
                        }
                        break;
                    //dentro da funçao decidir o que pesquisa artista, estilo ou album
                    default:
                        System.out.println("Feedback above.");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    public ServerSocket openSocket() throws IOException {
        ServerSocket socket = new ServerSocket(5041);
        return socket;
    }

    public static String sendMusicMulticast(String direc, ServerSocket socket) throws IOException {
        Socket socketAcept = socket.accept();
        File file = new File(direc);
        FilePermission permission = new FilePermission(direc, "read");
        FileInputStream fInStream = new FileInputStream(file);
        OutputStream outStream = socketAcept.getOutputStream();

        byte b[];
        int current =0;
        long len = file.length();
        while(current!=len){
            int size = 1024;
            if(len - current >= size)
                current += size;
            else{
                size = (int)(len - current);
                current = (int) len;
            }
            b = new byte[size];
            fInStream.read(b, 0, size);
            outStream.write(b);
            System.out.println("Sending file ... "+(current*100)/len+"% complete!");

        }
        fInStream.close();
        outStream.flush();
        outStream.close();
        socketAcept.close();
        socket.isClosed();
        return "tudo okay no download";
    }

    private Socket ligarSocket(String address) throws IOException {
        Socket socket = new Socket(address,5000);
        return socket;
    }

    private String printAlbunsProtocol(String[] array){
        String stringFinal = "";
        int i;
        for(i=0;i<array.length;i++){
            stringFinal += "|"+array[i];
        }
        return stringFinal;
    }

    private User returnsUser(String username){
        for(User u : usersList){
            if(u.getUsername().trim().equals(username)){
                return u;
            }
        }
        return null;
    }

    private Artist returnsArtist(String artistName){
        for(Artist artist : artistsList){
            if(artist.getName().trim().equals(artistName)){
                return artist;
            }
        }
        return null;
    }

    private void receiveMusic(Socket socket, String musicName) throws IOException {
        byte[] b= new byte[1024];
        System.out.println("1");
        InputStream is = socket.getInputStream();
        System.out.println("2");
        FileOutputStream fOutStream = new FileOutputStream("./musicasServer/" + musicName);
        System.out.println("3");
        BufferedOutputStream bOutStream = new BufferedOutputStream(fOutStream);

        int aux= 0;
        int cont= 0;
        System.out.println("4");
        while ((aux = is.read(b))!=-1){
            System.out.println(cont++);
            bOutStream.write(b, 0, aux);
            System.out.println("4.1");
            if(is.available()==0){
                break;
            }
        }
        System.out.println("5");
        bOutStream.flush();
        socket.close();

        System.out.println("ficheiro 100% completo");
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

    public boolean checkArtistExists(String name){
        if(artistsList.isEmpty()){
            return false;
        }
        else {
            for (Artist artist : artistsList) {
                if (artist.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkMusicExists(String name,String artistName){
        if(musicsList.isEmpty()){
            return false;
        }
        else {
            for (Music music : musicsList) {
                if (music.getTitle().equals(name)){
                    if(music.getArtist().equals(artistName)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkAlbumExists(String name, String artist){
        if(albunsList.isEmpty()){
            return false;
        }
        else {
            for (Album album : albunsList) {
                if (album.getName().equals(name)) {
                    if(album.getArtist().getName().equals(artist)){
                        return true;
                    }
                }
            }
        }
        return false;
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

    private void readFiles(){
        System.out.println("Reading.");
        ArrayList<User> users = new ArrayList<>();
        try {
            ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream("data.bin")));
            this.usersList = (ArrayList) objectIn.readObject();
            this.artistsList = (ArrayList) objectIn.readObject();
            this.albunsList = (ArrayList) objectIn.readObject();
            this.musicsList = (ArrayList) objectIn.readObject();
            objectIn.close();
            System.out.println("Read file successfully.");
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Empty file!");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void writeFiles(){
        System.out.println("Writing.");
        try{
            File file = new File("data.bin");
            FileOutputStream out = new FileOutputStream(file);
            ObjectOutputStream fout = new ObjectOutputStream(out);
            fout.writeObject(this.usersList);
            fout.writeObject(this.artistsList);
            fout.writeObject(this.albunsList);
            fout.writeObject(this.musicsList);
            fout.close();
            out.close();
        }
        catch (FileNotFoundException ex) {
            Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("File not found!");
        } catch (IOException ex) {
            Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Lists wrote successfully.");
    }


}
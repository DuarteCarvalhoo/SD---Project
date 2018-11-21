import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MulticastServer extends Thread implements Serializable {
    private String MULTICAST_ADDRESS = "224.0.224.0";
    private int PORT = 4321;
    private ArrayList<User> usersList = new ArrayList<>();
    private ArrayList<Artist> artistsList = new ArrayList<>();
    private ArrayList<Album> albunsList = new ArrayList<>();
    private ArrayList<Music> musicsList = new ArrayList<>();
    private Connection connection = null;

    public static void main(String[] args){
        MulticastServer server = new MulticastServer();
        server.start();
    }

    public MulticastServer(){ super ("Server " + (long) (Math.random()*1000));}

    ////////////// RECEBER E TRATAR O PROTOCOL /////////////
    public void run() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/BD/SD","postgres", "fabiogc1998");
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                String[] aux = msg.split(";");
                switch (aux[0]) {
                    case "type|login":
                        boolean flag = false;
                        String[] loginUsernameParts = aux[1].split("\\|");
                        String[] loginPasswordParts = aux[2].split("\\|");
                        String user = loginUsernameParts[1];
                        String pass = loginPasswordParts[1];
                        try {
                            if (userDatabaseEmpty()) {
                                sendMsg("type|loginFail");
                                System.out.println("ERROR: No users on the database.");
                            } else {
                                User u = checkUsernameLogin(user, pass);
                                if(u.getId() != 0){
                                    sendMsg("type|loginComplete;id|" + u.getId()+";editor|"+u.isEditor());//
                                    System.out.println("SUCESSO: Login Completo");
                                    flag = true;
                                }
                            }
                                if (!flag) {
                                    System.out.println("Username not found.");
                                    sendMsg("type|loginFail");
                                }
                        }
                        catch (org.postgresql.util.PSQLException e){
                            System.out.println(e);
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
                        }catch (Exception e){
                            return;
                        }
                        if(userDatabaseEmpty()){
                            connection.setAutoCommit(false);
                            User u = new User(username,password);
                            PreparedStatement stmt = connection.prepareStatement("INSERT INTO utilizador(id,username,password,iseditor)" +
                                    "VALUES (DEFAULT,?,?,true)");
                            stmt.setString(1,u.getUsername());
                            stmt.setString(2,u.getPassword());
                            stmt.executeUpdate();

                            stmt.close();
                            connection.commit();
                            sendMsg("type|registComplete");
                        }
                        else{
                            try{
                                connection.setAutoCommit(false);
                                User u = new User(username,password);
                                PreparedStatement stmt = connection.prepareStatement("INSERT INTO utilizador(id,username,password,iseditor)" +
                                        "VALUES (DEFAULT,?,?,false)");
                                stmt.setString(1,u.getUsername());
                                stmt.setString(2,u.getPassword());
                                stmt.executeUpdate();

                                stmt.close();
                                connection.commit();
                                
                                sendMsg("type|registComplete");
                            } catch (org.postgresql.util.PSQLException e){
                                System.out.println("Something went wrong.");
                                sendMsg("type|usernameUsed");
                            }
                        }
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
                    case"type|addNotification":
                        aux2 = aux[1];
                        String mensagem = aux[2];
                        String[] nameUser = aux2.split("\\|");
                        String[] notif = mensagem.split("\\|");
                        for (int i=0; i<usersList.size(); i++){
                            if(usersList.get(i).getUsername().equals(nameUser[1])){
                                usersList.get(i).addNotification(notif[1]);
                                for(int j=0;j<usersList.get(i).getNotifications().size();j++){
                                    System.out.println(usersList.get(i).getNotifications().get(j));
                                }
                                System.out.println(usersList.get(i).getUsername());
                                System.out.println("check");
                                break;
                            }
                        }
                        break;
                    /*case"type|sendNotif":
                        aux2 = aux[1];
                        String[] nome = aux2.split("\\|");
                        for (int i=0; i<usersList.size(); i++) {
                            if (usersList.get(i).getUsername().equals(nome[1])) {
                                for(int j=0; j<user)
                                sendMsg(usersList.get(i);
                            }
                        }
                     */
                    case "type|createSongwriter":
                        String[] nameParts1 = aux[1].split("\\|");
                        String[] descriptionParts = aux[2].split("\\|");
                        PreparedStatement stmtSongwriter = null;
                        try {
                            Songwriter a = new Songwriter(nameParts1[1],descriptionParts[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtSongwriter = connection.prepareStatement("INSERT INTO artist (id,name,description,musician_ismusician,group_isgroup,songwriter_issongwriter,composer_iscomposer)"
                                    + "VALUES (DEFAULT,?,?,?,?,?,?);");
                            stmtSongwriter.setString(1,a.getName());
                            stmtSongwriter.setString(2,a.getDescription());
                            stmtSongwriter.setBoolean(3,a.isMusician());
                            stmtSongwriter.setBoolean(4,a.isBand());
                            stmtSongwriter.setBoolean(5,a.isSongwriter());
                            stmtSongwriter.setBoolean(6,a.isComposer());
                            stmtSongwriter.executeUpdate();

                            stmtSongwriter.close();
                            connection.commit();

                        } catch (org.postgresql.util.PSQLException e) {
                            sendMsg("type|stmtSongwriter");
                            System.out.println("ERRO: Artist already exists.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createSongwriterComplete");
                        break;
                    case "type|createMusician":
                        String[] namePartsMusician = aux[1].split("\\|");
                        String[] descriptionPartsMusician = aux[2].split("\\|");
                        PreparedStatement stmtMusician = null;
                        try {
                            Musician a = new Musician(namePartsMusician[1],descriptionPartsMusician[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtMusician = connection.prepareStatement("INSERT INTO artist (id,name,description,musician_ismusician,group_isgroup,songwriter_issongwriter,composer_iscomposer)"
                                    + "VALUES (DEFAULT,?,?,?,?,?,?);");
                            stmtMusician.setString(1,a.getName());
                            stmtMusician.setString(2,a.getDescription());
                            stmtMusician.setBoolean(3,a.isMusician());
                            stmtMusician.setBoolean(4,a.isBand());
                            stmtMusician.setBoolean(5,a.isSongwriter());
                            stmtMusician.setBoolean(6,a.isComposer());
                            stmtMusician.executeUpdate();

                            stmtMusician.close();
                            connection.commit();

                        } catch (org.postgresql.util.PSQLException e) {
                            sendMsg("type|musicianExists");
                            System.out.println("ERRO: Artist already exists.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createMusicianComplete");
                        break;
                    case "type|createComposer":
                        String[] namePartsComposer = aux[1].split("\\|");
                        String[] descriptionPartsComposer = aux[2].split("\\|");
                        PreparedStatement stmtComposer = null;
                        try {
                            Composer a = new Composer(namePartsComposer[1],descriptionPartsComposer[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtComposer = connection.prepareStatement("INSERT INTO artist (id,name,description,musician_ismusician,group_isgroup,songwriter_issongwriter,composer_iscomposer)"
                                    + "VALUES (DEFAULT,?,?,?,?,?,?);");
                            stmtComposer.setString(1,a.getName());
                            stmtComposer.setString(2,a.getDescription());
                            stmtComposer.setBoolean(3,a.isMusician());
                            stmtComposer.setBoolean(4,a.isBand());
                            stmtComposer.setBoolean(5,a.isSongwriter());
                            stmtComposer.setBoolean(6,a.isComposer());
                            stmtComposer.executeUpdate();

                            stmtComposer.close();
                            connection.commit();

                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|composerExists");
                            System.out.println("ERRO: Composer already exists.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createComposerComplete");
                        break;
                    case "type|createBand":
                        String[] namePartsBand = aux[1].split("\\|");
                        String[] descriptionPartsBand = aux[2].split("\\|");
                        PreparedStatement stmtBand = null;
                        try {
                            Band a = new Band(namePartsBand[1],descriptionPartsBand[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtBand = connection.prepareStatement("INSERT INTO artist (id,name,description,musician_ismusician,group_isgroup,songwriter_issongwriter,composer_iscomposer)"
                                    + "VALUES (DEFAULT,?,?,?,?,?,?);");
                            stmtBand.setString(1,a.getName());
                            stmtBand.setString(2,a.getDescription());
                            stmtBand.setBoolean(3,a.isMusician());
                            stmtBand.setBoolean(4,a.isBand());
                            stmtBand.setBoolean(5,a.isSongwriter());
                            stmtBand.setBoolean(6,a.isComposer());
                            stmtBand.executeUpdate();

                            stmtBand.close();
                            connection.commit();

                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|bandExists");
                            System.out.println("ERRO: Band already exists.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createBandComplete");
                        break;
                    case "type|createAlbum":
                        Artist artist = new Musician();
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
                        Artist art = new Musician();

                        PreparedStatement stmt = null;
                        try {
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmt = connection.prepareStatement("SELECT * FROM artist WHERE name = ?;");
                            stmt.setString(1,n);
                            ResultSet rs = stmt.executeQuery();
                            while (rs.next()) {
                                String id = rs.getString("id");
                                String  name1 = rs.getString("name");
                                String description = rs.getString("description");
                                boolean isMusician = rs.getBoolean("musician_ismusician");
                                boolean isBand = rs.getBoolean("group_isgroup");
                                boolean isSongwriter = rs.getBoolean("songwriter_issongwriter");
                                boolean isComposer = rs.getBoolean("composer_iscomposer");
                                art = new Musician(name1,description);
                            }
                            rs.close();
                            stmt.close();
                            
                        } catch ( Exception e ) {
                            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
                            System.exit(0);
                        }
                        System.out.println("Operation done successfully");
                        sendMsg("type|showArtistComplete;Artist|"+art);
                        /*if(artistsList.isEmpty()){
                            sendMsg("type|showArtistFail");
                            System.out.println("ERROR: No Artists on the database.");
                        }
                        else {
                            /*if (!checkArtistExists(n)) {
                                sendMsg("type|showArtistFail");
                                System.out.println("ERROR: Artist Not Found.");
                            } else {
                                for(Artist a : artistsList){
                                    if(a.getName().equals(n)){
                                        art = a;
                                    }
                                }
                            }*/
                        break;
                    case "type|showArtistAlbums":
                        String[] nameA = aux[1].split("\\|");
                        String nA = nameA[1];
                        Artist artista = new Musician();
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
                    default:
                        System.out.println("Feedback above.");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    private boolean isEditor(String username) {

        try {
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM utilizador WHERE username = ?;");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            boolean isEditorDB = false;
            while (rs.next()) {
                isEditorDB = rs.getBoolean("iseditor");
            }

            stmt.close();
            connection.commit();
            if (isEditorDB) {
                return true;
            }
        } catch (
                SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    private void makeEditor(String username) throws SQLException {
        connection.setAutoCommit(false);
        PreparedStatement stmt = connection.prepareStatement("UPDATE utilizador SET iseditor = true WHERE username = ?");
        stmt.setString(1,username);
        stmt.executeUpdate();

        stmt.close();
        connection.commit();
    }

    ////////////// DOWNLOAD E UPLOAD /////////////
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
        socket.close();
        return "tudo okay no download";
    }

    private Socket ligarSocket(String address) throws IOException {
        Socket socket = new Socket(address,5000);
        return socket;
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

    ////////////// ENVIO DO PROTOCOL /////////////
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
    ////////////// FUNÇOES AUXILIAR /////////////
    private boolean userDatabaseEmpty() throws SQLException {
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM utilizador");
            return !rs.next();
        }
        catch (org.postgresql.util.PSQLException e){
            return false;
        }
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

    public User checkUsernameLogin(String username, String password){
        PreparedStatement stmt = null;
        try {
            String userDB="",passDB="";
            boolean isEditorDB=false;
            int id=0;

            connection.setAutoCommit(false);
            stmt = connection.prepareStatement("SELECT * FROM utilizador WHERE username = ?;");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                id = rs.getInt("id");
                userDB = rs.getString("username");
                passDB = rs.getString("password");
                isEditorDB = rs.getBoolean("iseditor");
            }

            stmt.close();
            connection.commit();
            User u = new User(id,username,isEditorDB);
            if(userDB.equals(username) && passDB.equals(password)){
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (new User(0,"none",false));
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

    ////////////// READ AND WRITE FILES /////////////
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
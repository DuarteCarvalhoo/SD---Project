import com.sun.org.apache.xpath.internal.operations.Bool;

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
                        String[] loggedUserParts = aux[8].split("\\|");
                        String[] pathParts = aux[1].split("\\|");
                        String[] titleParts = aux[2].split("\\|");
                        String[] composerParts = aux[3].split("\\|");
                        String[] artistParts = aux[4].split("\\|");
                        String[] sParts = aux[5].split("\\|");
                        String[] durationParts = aux[6].split("\\|");
                        String[] albumParts = aux[7].split("\\|");

                        connection.setAutoCommit(false);
                        PreparedStatement stmtUpload = connection.prepareStatement("INSERT INTO music(id, title, length)"
                        + "VALUES(DEFAULT,?,?);");
                        stmtUpload.setString(1,titleParts[1]);
                        stmtUpload.setInt(2,Integer.parseInt(durationParts[1]));
                        stmtUpload.executeUpdate();

                        stmtUpload = connection.prepareStatement("INSERT INTO album_music(album_id, music_id)"
                        + "VALUES(?,?);");
                        stmtUpload.setInt(1,getAlbumIdByName(albumParts[1]));
                        stmtUpload.setInt(2,getMusicIdByName(titleParts[1]));
                        stmtUpload.executeUpdate();

                        stmtUpload = connection.prepareStatement("INSERT INTO filearchive(id,path, music_id,utilizador_id)"
                        + "VALUES(DEFAULT,?,?,?);");
                        stmtUpload.setString(1,pathParts[1]);
                        stmtUpload.setInt(2,getMusicIdByName(titleParts[1]));
                        stmtUpload.setInt(3,Integer.parseInt(loggedUserParts[1]));
                        stmtUpload.executeUpdate();

                        stmtUpload = connection.prepareStatement("INSERT INTO utilizador_filearchive(utilizador_id, filearchive_id)"
                        + "VALUES(?,?);");

                        stmtUpload.setInt(2,getFileArchiveByPath(pathParts[1]));
                        stmtUpload.setInt(1,Integer.parseInt(loggedUserParts[1]));
                        stmtUpload.executeUpdate();

                        stmtUpload = connection.prepareStatement("INSERT INTO composer_music(artista_id, music_id)"
                        + "VALUES(?,?);");
                        stmtUpload.setInt(1,getArtistIdByName(composerParts[1]));
                        stmtUpload.setInt(2,getMusicIdByName(titleParts[1]));
                        stmtUpload.executeUpdate();

                        stmtUpload = connection.prepareStatement("INSERT INTO music_songwriter(music_id, artista_id)"
                                + "VALUES(?,?);");
                        stmtUpload.setInt(2,getArtistIdByName(sParts[1]));
                        stmtUpload.setInt(1,getMusicIdByName(titleParts[1]));
                        stmtUpload.executeUpdate();

                        stmtUpload = connection.prepareStatement("INSERT INTO artista_music(artista_id, music_id)"
                                + "VALUES(?,?);");
                        stmtUpload.setInt(1,getArtistIdByName(sParts[1]));
                        stmtUpload.setInt(2,getMusicIdByName(titleParts[1]));
                        stmtUpload.executeUpdate();

                        int lengthA = getAlbumLengthById(getAlbumIdByName(albumParts[1]));
                        int newLength = lengthA + Integer.parseInt(durationParts[1]);
                        stmtUpload = connection.prepareStatement("UPDATE album SET length = ? WHERE id = ?;");
                        stmtUpload.setInt(1,newLength);
                        stmtUpload.setInt(2,getAlbumIdByName(albumParts[1]));
                        stmtUpload.executeUpdate();

                        stmtUpload.close();
                        connection.commit();
                        receiveMusic(socketHelp,titleParts[1]);
                        sendMsg("type|sendMusicComplete");
                        break;
                    case "type|shareMusic":
                        String[] musicParts = aux[2].split("\\|");
                        String[] shareUserParts = aux[1].split("\\|");

                        int musicId = getMusicIdByName(musicParts[1]);

                        connection.setAutoCommit(false);
                        PreparedStatement stmtShare = connection.prepareStatement("INSERT INTO utilizador_filearchive(utilizador_id, filearchive_id)"
                        + "VALUES(?,?);");

                        stmtShare.setInt(2,getFileArchiveByMusicId(musicId));
                        stmtShare.setInt(1,getUserIdByName(shareUserParts[1]));
                        stmtShare.executeUpdate();

                        stmtShare.close();
                        connection.commit();
                        sendMsg("type|musicShareCompleted");
                        break;
                    case "type|getMusicsList":
                        String[] userParts = aux[1].split("\\|");
                        ArrayList<Music> UploadedMusics;

                        UploadedMusics = getAvailableMusicsByUserId(userParts[1]);
                        sendMsg("type|getUploadedMusicsCompleted;"+"Musics|"+printMusics(UploadedMusics));
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
                        if(userDatabaseEmpty()){
                            sendMsg("type|makingEditorFail");
                            System.out.println("ERROR: No users on the database.");
                        }
                        else {
                            makeEditor(parts[1]);
                            sendMsg("type|makingEditorComplete");
                            System.out.println("SUCCESS: User "+parts[1]+" made editor.");

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
                        String[] songwriterParts = aux[3].split("\\|");
                        String[] isComposerParts = aux[4].split("\\|");
                        String[] isBandParts = aux[5].split("\\|");
                        PreparedStatement stmtMusician = null;
                        try {
                            Musician a = new Musician(namePartsMusician[1],descriptionPartsMusician[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtMusician = connection.prepareStatement("INSERT INTO artista (id,name,description,musician_ismusician,band_isband,songwriter_issongwriter,composer_iscomposer)"
                                    + "VALUES (DEFAULT,?,?,?,?,?,?);");
                            stmtMusician.setString(1,a.getName());
                            stmtMusician.setString(2,a.getDescription());
                            stmtMusician.setBoolean(3,!(Boolean.parseBoolean(isBandParts[1])));
                            stmtMusician.setBoolean(4,Boolean.parseBoolean(isBandParts[1]));
                            stmtMusician.setBoolean(5,Boolean.parseBoolean(songwriterParts[1]));
                            stmtMusician.setBoolean(6,Boolean.parseBoolean(isComposerParts[1]));
                            stmtMusician.executeUpdate();

                            stmtMusician.close();
                            connection.commit();

                        } catch (org.postgresql.util.PSQLException e) {
                            sendMsg("type|musicianExists");
                            System.out.println("ERRO: Something went wrong.");
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

                            stmtComposer = connection.prepareStatement("INSERT INTO artista (id,name,description,musician_ismusician,group_isgroup,songwriter_issongwriter,composer_iscomposer)"
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
                        String[] gParts = aux[2].split("\\|");
                        String[] descripParts = aux[3].split("\\|");
                        String[] aName = aux[4].split("\\|");
                        String[] pName = aux[5].split("\\|");
                        PreparedStatement stmtAlbum = null;
                        boolean flagAlbum = false;

                        try{
                            connection.setAutoCommit(false);
                            System.out.println("Open database successfully!");
                            int publisherId = getPublisherById(pName[1]);

                            System.out.println("0");
                            stmtAlbum = connection.prepareStatement("INSERT INTO album(id,name,genre,description,length,publisher_id)"
                                                        + "VALUES (DEFAULT,?,?,?,0,?);");
                            stmtAlbum.setString(1,namePa[1]);
                            stmtAlbum.setString(2,gParts[1]);
                            stmtAlbum.setString(3,descripParts[1]);
                            stmtAlbum.setInt(4,publisherId);
                            stmtAlbum.executeUpdate();
                            System.out.println("1");

                            int artistId = getArtistIdByName(aName[1]);
                            int albumId = getAlbumIdByName(namePa[1]);
                            stmtAlbum = connection.prepareStatement("INSERT INTO artista_album(artista_id, album_id)"
                            + "VALUES (?,?);");
                            stmtAlbum.setInt(1,artistId);
                            stmtAlbum.setInt(2,albumId);
                            stmtAlbum.executeUpdate();
                            System.out.println("2");

                            stmtAlbum.close();
                            connection.commit();
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println(e.getMessage());
                            sendMsg("type|createAlbumFailed");
                            System.out.println("ERRO: Album creation failed.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createAlbumComplete");
                        System.out.println("Worked");
                        break;
                    case "type|createConcert":
                        String[] concertLocation = aux[1].split("\\|");
                        String[] concertName = aux[2].split("\\|");
                        String[] concertDescription = aux[3].split("\\|");
                        PreparedStatement stmtConcert = null;
                        try {
                            Concert a = new Concert(concertLocation[1],concertName[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtConcert = connection.prepareStatement("INSERT INTO concert (id,name,description,location)"
                                    + "VALUES (DEFAULT,?,?,?);");
                            stmtConcert.setString(3,a.getLocation());
                            stmtConcert.setString(2,concertDescription[1]);
                            stmtConcert.setString(1,a.getName());
                            stmtConcert.executeUpdate();

                            stmtConcert.close();
                            connection.commit();

                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|createConcertFailed");
                            System.out.println("ERRO: Concert already exists.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createConcertComplete");
                        break;
                    case "type|concertAssociation":
                        String[] concertBandMusician = aux[2].split("\\|");
                        String[] concertN = aux[1].split("\\|");

                        int concertId = getConcertIdByName(concertN[1]);
                        int bandId = getArtistIdByName(concertBandMusician[1]);
                        PreparedStatement stmtConcertA = null;
                        try {
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtConcertA = connection.prepareStatement("INSERT INTO concert_artista (concert_id, artista_id)"
                                    + "VALUES (?,?);");
                            stmtConcertA.setInt(1,concertId);
                            stmtConcertA.setInt(2,bandId);
                            stmtConcertA.executeUpdate();

                            stmtConcertA.close();
                            connection.commit();

                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|concertExists");
                            System.out.println("ERRO: Concert already exists.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createConcertComplete");
                        break;
                    case "type|createPublisher":
                        String[] publisherName = aux[1].split("\\|");
                        PreparedStatement stmtPublisher = null;
                        try {
                            Publisher a = new Publisher(publisherName[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtPublisher = connection.prepareStatement("INSERT INTO publisher (id,name)"
                                    + "VALUES (DEFAULT,?);");
                            stmtPublisher.setString(1,a.getName());
                            stmtPublisher.executeUpdate();

                            stmtPublisher.close();
                            connection.commit();

                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|publisherExists");
                            System.out.println("ERRO: Publisher already exists.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createPublisherComplete");
                        break;
                    case "type|createPlaylist":
                        String[] playlistName = aux[1].split("\\|");
                        String[] playlistUser = aux[2].split("\\|");
                        PreparedStatement stmtPlaylist = null, stmtUserConnection=null;
                        try {
                            Playlist a = new Playlist(playlistName[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtPlaylist = connection.prepareStatement("INSERT INTO playlist (id,name,utilizador_id)"
                                    + "VALUES (DEFAULT,?,?);");
                            stmtPlaylist.setString(1,a.getName());
                            stmtPlaylist.setInt(2,Integer.parseInt(playlistUser[1]));
                            stmtPlaylist.executeUpdate();

                            stmtPlaylist.close();
                            connection.commit();
                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|createPlaylistFailed");
                            System.out.println("ERRO: Something went wrong.");
                        }
                        System.out.println("Records created successfully");
                        sendMsg("type|createPlaylistComplete");
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
                        String[] nameArtist = aux[1].split("\\|");
                        String n = nameArtist[1];
                        int id1 = 0;
                        String  name1="";
                        String description1="";
                        boolean isMusician=false;
                        boolean isBand=false;
                        boolean isSongwriter=false;
                        boolean isComposer=false;
                        ArrayList<String> albNames = new ArrayList<>();
                        ArrayList<Integer> albIds = new ArrayList<>();

                        PreparedStatement stmt = null;
                        try {
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmt = connection.prepareStatement("SELECT * FROM artista WHERE name = ?;");
                            stmt.setString(1,n);
                            ResultSet rs = stmt.executeQuery();
                            while (rs.next()) {
                                id1 = rs.getInt("id");
                                name1 = rs.getString("name");
                                description1 = rs.getString("description");
                                isMusician = rs.getBoolean("musician_ismusician");
                                isBand = rs.getBoolean("band_isband");
                                isSongwriter = rs.getBoolean("songwriter_issongwriter");
                                isComposer = rs.getBoolean("composer_iscomposer");
                            }

                            stmt = connection.prepareStatement("SELECT * FROM artista_album WHERE artista_id = ?;");
                            stmt.setInt(1,id1);
                            rs = stmt.executeQuery();
                            while(rs.next()){
                                int albid = rs.getInt("album_id");
                                albIds.add(albid);
                            }


                            for(Integer ID : albIds){
                                stmt = connection.prepareStatement("SELECT * FROM album WHERE id = ?;");
                                stmt.setInt(1, ID);
                                rs = stmt.executeQuery();

                                while(rs.next()){
                                    albNames.add(rs.getString("name"));
                                }
                            }

                            rs.close();
                            stmt.close();
                        } catch ( Exception e ) {
                            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
                            System.exit(0);
                        }
                        System.out.println("Operation done successfully");
                        sendMsg("type|showArtistComplete;Name|"+name1+";Description|"+description1+";Functions|"+printFunctions(isMusician,isBand,isSongwriter,isComposer)+";Albums|"+printAlbuns(albNames));
                        break;
                    case "type|showArtistAlbums":
                        ArrayList<Integer> album_ids = new ArrayList<>();
                        ArrayList<String> album_names = new ArrayList<>();
                        String[] nameA = aux[1].split("\\|");
                        String nA = nameA[1];
                        //proteção de db vazia
                        try{
                            PreparedStatement stmtShowArtistAlbum = connection.prepareStatement("SELECT * FROM artista_album WHERE artista_id = ?;");
                            stmtShowArtistAlbum.setInt(1,getArtistIdByName(nA));

                            ResultSet res = stmtShowArtistAlbum.executeQuery();
                            while(res.next()){
                                album_ids.add(res.getInt("album_id"));
                            }

                            for(Integer i : album_ids){
                                stmtShowArtistAlbum = connection.prepareStatement("SELECT * FROM album WHERE id = ?;");
                                stmtShowArtistAlbum.setInt(1, i);
                                res = stmtShowArtistAlbum.executeQuery();

                                while(res.next()){
                                    album_names.add(res.getString("name"));
                                }
                            }
                            sendMsg("type|showArtistAlbumsComplete;Albums|"+printAlbuns(album_names));
                        }
                        catch (org.postgresql.util.PSQLException e){
                            sendMsg("type|showArtistAlbumsFailed");
                            System.out.println(e.getMessage());
                        }
                        break;
                    case "type|makeCritic":
                        String[] scoreParts = aux[1].split("\\|");
                        String[] textParts = aux[2].split("\\|");
                        String[] albName = aux[3].split("\\|");
                        String[] userId = aux[4].split("\\|");

                        if(albumDatabaseEmpty()){
                            sendMsg("type|makeCriticFail");
                            System.out.println("ERROR: No Albuns in the database.");
                        }
                        else{
                            PreparedStatement stmtCritic = null;

                            try{
                                int albumId = getAlbumIdByName(albName[1]);
                                connection.setAutoCommit(false);
                                stmtCritic = connection.prepareStatement("INSERT INTO critic(id, score, text, album_id, utilizador_id) "
                                                + "VALUES(DEFAULT,?,?,?,?);");
                                stmtCritic.setDouble(1,Double.parseDouble(scoreParts[1]));
                                stmtCritic.setString(2,textParts[1]);
                                stmtCritic.setInt(3,albumId);
                                stmtCritic.setInt(4,Integer.parseInt(userId[1]));
                                stmtCritic.executeUpdate();
                                stmtCritic.close();
                                connection.commit();
                                sendMsg("type|criticComplete");
                            }catch(org.postgresql.util.PSQLException e){
                                sendMsg("type|criticFail");
                                System.out.println("Something went wrong.");
                            }
                        }
                        break;
                    case "type|showAlbum":
                        String[] albuName = aux[1].split("\\|");
                        Album album = new Album();
                        if(albumDatabaseEmpty()){
                            sendMsg("type|showAlbumFailed");
                            System.out.println("ERROR: No albuns on database.");
                        }
                        else {
                            PreparedStatement stmtShowAlbum = null;
                            String nome = "";
                            String genre = "";
                            String description = "";
                            String publisherN = "";
                            String artistName = "";
                            ArrayList<Critic> criticsList = new ArrayList<>();
                            ArrayList<Music> musicsList = new ArrayList<>();
                            int length = 0;
                            int publisherId = 0;
                            int id = 0;
                            int artistId = 0;
                            double scoreFinal = 0;

                            try {
                                connection.setAutoCommit(false);
                                System.out.println("Opened database successfully");
                                stmtShowAlbum = connection.prepareStatement("SELECT * FROM album WHERE name = ?;");
                                stmtShowAlbum.setString(1, albuName[1]);
                                ResultSet rs = stmtShowAlbum.executeQuery();
                                while (rs.next()) {
                                    id = rs.getInt("id");
                                    nome = rs.getString("name");
                                    genre = rs.getString("genre");
                                    description = rs.getString("description");
                                    length = rs.getInt("length");
                                    publisherId = rs.getInt("publisher_id");
                                }

                                stmtShowAlbum.close();

                                publisherN = getPublisherNameById(publisherId);
                                stmtShowAlbum = connection.prepareStatement("SELECT * FROM artista_album WHERE album_id = ?;");
                                stmtShowAlbum.setInt(1, id);
                                rs = stmtShowAlbum.executeQuery();
                                while (rs.next()) {
                                    artistId = rs.getInt("artista_id");
                                }
                                artistName = getArtistNameById(artistId);
                                criticsList = getCriticsByAlbumId(id);
                                musicsList = getMusicsByAlbumId(id);
                                scoreFinal = calculateScore(criticsList);


                            }catch(org.postgresql.util.PSQLException e){
                                System.out.println("except");
                                sendMsg("type|showAlbumFailed");
                            }
                            sendMsg("type|showAlbumComplete" + ";AlbumName|" + nome +";ArtistName|"+artistName
                        + ";Description|"+description+";Length|"+length+";Genre|"+genre+";ScoreFinal|"+scoreFinal
                            +";CriticsList|"+printCritics(criticsList)+";MusicsList|"+printMusics(musicsList)+";Publisher|"+publisherN);
                            //sendMsg("type|showAlbumComplete" + ";Album|" + album);
                        }
                        break;
                    default:
                        System.out.println("Feedback above.");
                        break;
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    private String printFunctions(boolean isMusician, boolean isBand, boolean isSongwriter, boolean isComposer) {
        String finalString = "";

        if(isMusician){
            finalString += "Musician";
        }
        if(isSongwriter){
            finalString += ",Songwriter";
        }
        if(isComposer){
            finalString += ",Composer";
        }
        if(isBand){
            finalString += "Band";
        }

        return finalString;
    }

    private String printAlbuns(ArrayList<String> album_names) {
        String finalString = "";
        if(album_names.isEmpty()){
            finalString += "No albuns to show.";
        }
        else{
            for(int i = 0;i<album_names.size();i++){
                if(i == (album_names.size()-1)){
                    finalString += album_names.get(i);
                }
                else{
                    finalString += album_names.get(i);
                    finalString += ",";
                }
            }
        }
        return finalString;
    }

    private int getAlbumLengthById(int albumId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM album WHERE id = ?;");
            stmt.setInt(1,albumId);
            ResultSet rs = stmt.executeQuery();

            int albumLength = 0;
            while(rs.next()){
                albumLength = rs.getInt("length");
            }
            return albumLength;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getPathByMusicId(int musicId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM filearchive WHERE music_id = ?;");
            stmt.setInt(1,musicId);
            ResultSet rs = stmt.executeQuery();

            String path = "";
            while(rs.next()){
                path = rs.getString("path");
            }
            return path;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int getUserIdByName(String shareUserPart) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM utilizador WHERE username = ?;");
            stmt.setString(1,shareUserPart);
            ResultSet rs = stmt.executeQuery();

            int userId = 0;
            while(rs.next()){
                userId = rs.getInt("id");
            }
            return userId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private ArrayList<Music> getAvailableMusicsByUserId(String userPart) {
        ArrayList<Music> m = new ArrayList<>();

        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM utilizador_filearchive WHERE utilizador_id = ?;");
            stmt.setInt(1,Integer.parseInt(userPart));
            ResultSet rs = stmt.executeQuery();


            while(rs.next()){
                int fileId = rs.getInt("filearchive_id");
                String musicName = getMusicNameByFile(fileId);
                m.add(new Music(musicName));
            }

            return m;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private String getMusicNameByFile(int fileId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM filearchive WHERE id = ?;");
            stmt.setInt(1,fileId);
            ResultSet rs = stmt.executeQuery();

            String musicName = "";
            int musicId = 0;
            while(rs.next()){
                musicId = rs.getInt("music_id");
            }

            connection.setAutoCommit(false);
            stmt = connection.prepareStatement("SELECT * FROM music WHERE id = ?;");
            stmt.setInt(1,musicId);
            rs = stmt.executeQuery();


            while(rs.next()){
                musicName = rs.getString("title");
            }

            return musicName;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int getFileArchiveByMusicId(int musicId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM filearchive WHERE music_id = ?;");
            stmt.setInt(1,musicId);
            ResultSet rs = stmt.executeQuery();

            int fileId = 0;
            while(rs.next()){
                fileId = rs.getInt("id");
            }
            return fileId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getFileArchiveByPath(String pathPart) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM filearchive WHERE path = ?;");
            stmt.setString(1,pathPart);
            ResultSet rs = stmt.executeQuery();

            int fileId = 0;
            while(rs.next()){
                fileId = rs.getInt("id");
            }
            return fileId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String printMusics(ArrayList<Music> m) {
        String finalString = "";
        if(m.isEmpty()){
            finalString += "No musics to show.";
        }
        else{
            for(int i = 0;i<m.size();i++){
                if(i == (m.size()-1)){
                    finalString += m.get(i).toString();
                }
                else{
                    finalString += m.get(i).toString();
                    finalString += ",";
                }
            }
        }


        return finalString;
    }

    private String printCritics(ArrayList<Critic> c){
        String finalString = "";
        if(c.isEmpty()){
            finalString += "No critics to show.";
        }
        else{
            for(int i = 0;i<c.size();i++){
                if(i == (c.size()-1)){
                    finalString += c.get(i).toString();
                }
                else{
                    finalString += c.get(i).toString();
                    finalString += "!";
                }
            }
        }


        return finalString;
    }
    private double calculateScore(ArrayList<Critic> criticsList) {
        double score=0;
        for(Critic c : criticsList){
            score += c.getScore();
        }
        return score/criticsList.size();
    }

    private ArrayList<Critic> getCriticsByAlbumId(int id) {
        ArrayList<Critic> c = new ArrayList<>();
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM critic WHERE album_id = ?;");
            stmt.setInt(1,id);
            ResultSet rs = stmt.executeQuery();


            while(rs.next()){
                Double score = rs.getDouble("score");
                String text = rs.getString("text");
                int userId = rs.getInt("utilizador_id");
                Critic cr = new Critic(score,text,getUserNameById(userId));
                c.add(cr);
            }
            return c;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return c;
    }

    private ArrayList<Music> getMusicsByAlbumId(int id) {
        ArrayList<Music> m = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM album_music WHERE album_id = ?;");
            stmt.setInt(1,id);
            ResultSet rs = stmt.executeQuery();


            while(rs.next()){
                int musicId = rs.getInt("music_id");
                ids.add(musicId);
            }

            for(int i : ids){
                stmt = connection.prepareStatement("SELECT * FROM music WHERE id = ?;");
                stmt.setInt(1,i);
                rs = stmt.executeQuery();

                while(rs.next()){
                    String title = rs.getString("title");
                    int length = rs.getInt("length");

                    //String tit = title.replaceAll(".mp3","");
                    m.add(new Music(title,length));
                }
            }
            return m;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private String getUserNameById(int userId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM utilizador WHERE id = ?;");
            stmt.setInt(1,userId);
            ResultSet rs = stmt.executeQuery();

            String userName = "";
            while(rs.next()){
                userName = rs.getString("username");
            }
            return userName;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int getAlbumIdByName(String s) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM album WHERE name = ?;");
            stmt.setString(1,s);
            ResultSet rs = stmt.executeQuery();

            int albumId = 0;
            while(rs.next()){
                albumId = rs.getInt("id");
            }
            return albumId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getMusicIdByName(String s) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM music WHERE title = ?;");
            stmt.setString(1,s);
            ResultSet rs = stmt.executeQuery();

            int musicId = 0;
            while(rs.next()){
                musicId = rs.getInt("id");
            }
            return musicId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getPublisherById(String s) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM publisher WHERE name = ?;");
            stmt.setString(1,s);
            ResultSet rs = stmt.executeQuery();

            int publisherId = 0;
            while(rs.next()){
                publisherId = rs.getInt("id");
            }
            return publisherId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getPublisherNameById(int id) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM publisher WHERE id = ?;");
            stmt.setInt(1,id);
            ResultSet rs = stmt.executeQuery();

            String publisherName = "";
            while(rs.next()){
                publisherName = rs.getString("name");
            }
            return publisherName;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int getConcertIdByName(String concertN) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM concert WHERE name = ?;");
            stmt.setString(1,concertN);
            ResultSet rs = stmt.executeQuery();

            int concertId = 0 ;
            while (rs.next()) {
                concertId = rs.getInt("id");
            }

            return concertId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getConcertNameById(int concertId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM concerts WHERE id = ?;");
            stmt.setInt(1,concertId);
            ResultSet rs = stmt.executeQuery();

            String concertName = "" ;
            while (rs.next()) {
                concertName = rs.getString("name");
            }

            return concertName;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private int getArtistIdByName(String bandMusicianName) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM artista WHERE name = ?;");

            stmt.setString(1,bandMusicianName);
            ResultSet rs = stmt.executeQuery();

            int concertId = 0 ;
            while (rs.next()) {
                concertId = rs.getInt("id");
            }

            return concertId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getArtistNameById(int id) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM artista WHERE id = ?;");

            stmt.setInt(1,id);
            ResultSet rs = stmt.executeQuery();

            String artistName = "";
            while (rs.next()) {
                artistName = rs.getString("name");
            }
            stmt.close();
            return artistName;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
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

    private boolean albumDatabaseEmpty() throws SQLException {
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM album");
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
                    /*if(music.getArtist().equals(artistName)){
                        return true;
                    }*/
                }
            }
        }
        return false;
    }

    public boolean checkAlbumExists(String name, String artist){
        //Album a = getAlbumByName();
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
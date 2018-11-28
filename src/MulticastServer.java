import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MulticastServer extends Thread implements Serializable {
    private String MULTICAST_ADDRESS = "224.0.224.0";
    private int PORT = 4321;
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
        MulticastSocket socket = null;
        //System.out.println(this.getName() + "run...");

        try {
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            System.out.println("Multicast working!");
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
                        break;
                    case "type|register":
                        connection = initConnection();
                        String username;
                        String password;
                        try {
                            aux2 = aux[1];
                            String[] registerUsernameParts = aux2.split("\\|");
                            String[] registerPasswordParts = aux[2].split("\\|");
                            username = registerUsernameParts[1].trim();
                            password = registerPasswordParts[1].trim();
                        }catch (Exception e){
                            connection.close();
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
                            connection.close();
                            sendMsg("type|registComplete");
                        }
                        else{
                            try{
                                connection.setAutoCommit(false);
                                User u = new User(username,password);
                                if(checkUsernameRegister(username) == 1){
                                    sendMsg("type|usernameUsed");
                                    System.out.println("Username already used.");
                                } else {
                                    PreparedStatement stmt = connection.prepareStatement("INSERT INTO utilizador(id,username,password,iseditor)" +
                                            "VALUES (DEFAULT,?,?,false)");
                                    stmt.setString(1,u.getUsername());
                                    stmt.setString(2,u.getPassword());
                                    stmt.executeUpdate();

                                    stmt.close();
                                    connection.commit();
                                    connection.close();
                                    sendMsg("type|registComplete");
                                }
                            } catch (org.postgresql.util.PSQLException e){
                                System.out.println("Something went wrong.");
                                sendMsg("type|somethingWentWrong");
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
                        try{
                            connection = initConnection();
                            String[] loggedUserParts = aux[8].split("\\|");
                            String[] pathParts = aux[1].split("\\|");
                            String[] titleParts = aux[2].split("\\|");
                            String[] composerParts = aux[3].split("\\|");
                            String[] artistParts = aux[4].split("\\|");
                            String[] sParts = aux[5].split("\\|");
                            String[] durationParts = aux[6].split("\\|");
                            String[] albumParts = aux[7].split("\\|");

                            connection.setAutoCommit(false); // + " " +
                            PreparedStatement stmtUpload = null;
                            if( checkDuplicatedUpload(Integer.parseInt(loggedUserParts[1]),titleParts[1])==1 || checkArtistExists(artistParts[1]) != 1 || checkIfSongwriterValid(sParts[1]) != 1 || checkIfComposerValid(composerParts[1]) != 1 || checkAlbumExists(albumParts[1]) != 1){
                                if(checkDuplicatedUpload(Integer.parseInt(loggedUserParts[1]),titleParts[1]) == 1){
                                    sendMsg("type|duplicatedUpload");
                                    System.out.println("Music already uploaded.");
                                }
                                else if(checkArtistExists(artistParts[1]) == 0){
                                    connection.close();
                                    sendMsg("type|artistNotFound");
                                    System.out.println("Artist not found.");
                                }
                                else if(checkIfSongwriterValid(sParts[1]) == 0){
                                    connection.close();
                                    sendMsg("type|songwriterNotFound");
                                    System.out.println("Songwriter not found.");
                                }
                                else if(checkIfSongwriterValid(sParts[1]) == 2){
                                    connection.close();
                                    sendMsg("type|songwriterNotValid");
                                    System.out.println("Not a valid songwriter.");
                                }
                                else if(checkIfComposerValid(composerParts[1]) == 0){
                                    connection.close();
                                    sendMsg("type|composerNotFound");
                                    System.out.println("Composer not found.");
                                }
                                else if(checkIfComposerValid(composerParts[1]) == 2){
                                    connection.close();
                                    sendMsg("type|composerNotValid");
                                    System.out.println("Not a valid composer.");
                                }
                                else if(checkAlbumExists(albumParts[1]) == 0){
                                    connection.close();
                                    sendMsg("type|albumNotFound");
                                    System.out.println("Album not found.");
                                }
                                else{
                                    System.out.println(checkDuplicatedUpload(Integer.parseInt(loggedUserParts[1]),titleParts[1]) + " " + checkArtistExists(artistParts[1]) + " " + checkIfSongwriterValid(sParts[1]) + " " + checkIfComposerValid(composerParts[1]) + " " + checkAlbumExists(albumParts[1]));
                                    sendMsg("type|somethingWentWrong");
                                }
                            }
                            else if(checkDuplicatedUpload(Integer.parseInt(loggedUserParts[1]),titleParts[1])==0){
                                System.out.println("Same music -> Other user");
                                stmtUpload = connection.prepareStatement("INSERT INTO filearchive(path, music_id,utilizador_id)"
                                        + "VALUES(?,?,?);");
                                stmtUpload.setString(1,pathParts[1]);
                                stmtUpload.setInt(2,getMusicIdByName(titleParts[1]));
                                stmtUpload.setInt(3,Integer.parseInt(loggedUserParts[1]));
                                stmtUpload.executeUpdate();

                                stmtUpload = connection.prepareStatement("INSERT INTO utilizador_filearchive(utilizador_id, filearchive_utilizador_id, filearchive_music_id)"
                                        + "VALUES(?,?,?);");

                                stmtUpload.setInt(1,Integer.parseInt(loggedUserParts[1]));
                                stmtUpload.setInt(2,Integer.parseInt(loggedUserParts[1]));
                                stmtUpload.setInt(3,getMusicIdByName(titleParts[1]));
                                stmtUpload.executeUpdate();

                                connection.commit();
                                sendMsg("type|sendMusicComplete");
                            }
                            else if(checkDuplicatedUpload(Integer.parseInt(loggedUserParts[1]),titleParts[1])==-1){
                                stmtUpload = connection.prepareStatement("INSERT INTO music(id, title, length)"
                                        + "VALUES(DEFAULT,?,?);");
                                stmtUpload.setString(1,titleParts[1]);
                                stmtUpload.setInt(2,Integer.parseInt(durationParts[1]));
                                stmtUpload.executeUpdate();

                                stmtUpload = connection.prepareStatement("INSERT INTO album_music(album_id, music_id)"
                                        + "VALUES(?,?);");
                                stmtUpload.setInt(1,getAlbumIdByName(albumParts[1]));
                                stmtUpload.setInt(2,getMusicIdByName(titleParts[1]));
                                stmtUpload.executeUpdate();

                                stmtUpload = connection.prepareStatement("INSERT INTO filearchive(path, music_id,utilizador_id)"
                                        + "VALUES(?,?,?);");
                                stmtUpload.setString(1,pathParts[1]);
                                stmtUpload.setInt(2,getMusicIdByName(titleParts[1]));
                                stmtUpload.setInt(3,Integer.parseInt(loggedUserParts[1]));
                                stmtUpload.executeUpdate();

                                stmtUpload = connection.prepareStatement("INSERT INTO utilizador_filearchive(utilizador_id, filearchive_utilizador_id, filearchive_music_id)"
                                        + "VALUES(?,?,?);");

                                stmtUpload.setInt(1,Integer.parseInt(loggedUserParts[1]));
                                stmtUpload.setInt(2,Integer.parseInt(loggedUserParts[1]));
                                stmtUpload.setInt(3,getMusicIdByName(titleParts[1]));
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
                                stmtUpload.setInt(1,getArtistIdByName(artistParts[1]));
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
                                connection.close();
                                receiveMusic(socketHelp,titleParts[1]);
                                sendMsg("type|sendMusicComplete");
                            }
                        } catch(org.postgresql.util.PSQLException e){
                            System.out.println(e.getMessage());
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|shareMusic":
                        connection = initConnection();
                        String[] musicParts = aux[2].split("\\|");
                        String[] shareUserParts = aux[1].split("\\|");
                        String[] sharingUserParts = aux[3].split("\\|");

                        int musicId = getMusicIdByName(musicParts[1]);
                        if(musicId==0 || getUserIdByName(shareUserParts[1]) == 0 || checkHasAccess(musicParts[1],shareUserParts[1])==1){
                            if(musicId==0){
                                sendMsg("type|invalidMusic");
                                System.out.println("Invalid music.");
                            }
                            else if(checkHasAccess(musicParts[1],shareUserParts[1])==1){
                                sendMsg("type|isAlreadyDownloadable");
                                System.out.println("Already has access to that music.");
                            }
                            else{
                                sendMsg("type|invalidUser");
                                System.out.println("Invalid user.");
                            }
                        }
                        else{
                            connection.setAutoCommit(false);
                            PreparedStatement stmtShare = connection.prepareStatement("INSERT INTO utilizador_filearchive(utilizador_id, filearchive_utilizador_id, filearchive_music_id)"
                                    + "VALUES(?,?,?);");

                            stmtShare.setInt(3,musicId);
                            stmtShare.setInt(1,getUserIdByName(shareUserParts[1]));
                            stmtShare.setInt(2,Integer.parseInt(sharingUserParts[1]));
                            stmtShare.executeUpdate();

                            stmtShare.close();
                            connection.commit();
                            connection.close();
                            sendMsg("type|musicShareCompleted");
                        }
                        break;
                    case "type|removeMusicsPlaylist":
                        connection = initConnection();
                        String[] nP = aux[1].split("\\|");
                        String[] nomeMusica = aux[2].split("\\|");

                        try{
                            if(playlistDataBaseEmpty() || getPlaylistIdByName(nP[1])==0){
                                if(playlistDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|playlistDatabaseEmpty");
                                    System.out.println("Playlist database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|playlistNotFound");
                                    System.out.println("Playlist not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtRemoveMusic = connection.prepareStatement("DELETE FROM playlist_music WHERE music_id = ? AND playlist_id=?;");
                                stmtRemoveMusic.setInt(1,getMusicIdByName(nomeMusica[1]));
                                stmtRemoveMusic.setInt(2,getPlaylistIdByName(nP[1]));
                                stmtRemoveMusic.executeUpdate();

                                connection.commit();
                                connection.close();
                                sendMsg("type|removeMusicCompleted");
                                System.out.println("Music removed.");
                            }

                        }catch (org.postgresql.util.PSQLException e) {
                            sendMsg("type|somethingWentWrong");
                            System.out.println("ERRO: Something went wrong");
                        }

                        break;
                    case "type|editAlbumName":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] ANameB = aux[1].split("\\|");
                        String[] ANameA = aux[2].split("\\|");

                        try{
                            if(albumDatabaseEmpty() || getAlbumIdByName(ANameB[1])==0){
                                if(albumDatabaseEmpty()){
                                    connection.close();
                                    sendMsg("type|albumDatabaseEmpty");
                                    System.out.println("Album database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|albumNotFound");
                                    System.out.println("Album not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditPub = connection.prepareStatement("UPDATE album SET name = ? WHERE name = ?;");
                                stmtEditPub.setString(1,ANameA[1]);
                                stmtEditPub.setString(2,ANameB[1]);
                                stmtEditPub.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|nameChanged");
                                System.out.println("Name changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editAlbumDescription":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] ANB = aux[1].split("\\|");
                        String[] ADescripA = aux[2].split("\\|");

                        try{
                            if(albumDatabaseEmpty() || getAlbumIdByName(ANB[1])==0){
                                if(albumDatabaseEmpty()){
                                    connection.close();
                                    sendMsg("type|albumDatabaseEmpty");
                                    System.out.println("Album database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|albumNotFound");
                                    System.out.println("Album not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditAlb = connection.prepareStatement("UPDATE album SET description = ? WHERE name = ?;");
                                stmtEditAlb.setString(2,ANB[1]);
                                stmtEditAlb.setString(1,ADescripA[1]);
                                stmtEditAlb.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|descriptionChanged");
                                System.out.println("Description changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editAlbumGenre":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] ANaB = aux[1].split("\\|");
                        String[] AGenreA = aux[2].split("\\|");

                        try{
                            if(albumDatabaseEmpty() || getAlbumIdByName(ANaB[1])==0){
                                if(albumDatabaseEmpty()){
                                    connection.close();
                                    sendMsg("type|albumDatabaseEmpty");
                                    System.out.println("Album database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|albumNotFound");
                                    System.out.println("Album not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditPub = connection.prepareStatement("UPDATE album SET genre = ? WHERE name = ?;");
                                stmtEditPub.setString(2,ANaB[1]);
                                stmtEditPub.setString(1,AGenreA[1]);
                                stmtEditPub.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|genreChanged");
                                System.out.println("Genre changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|getMusicsList":
                        connection = initConnection();
                        String[] userParts = aux[1].split("\\|");
                        ArrayList<Music> UploadedMusics;

                        UploadedMusics = getAvailableMusicsByUserId(userParts[1]);
                        sendMsg("type|getUploadedMusicsCompleted;"+"Musics|"+printMusics(UploadedMusics));
                        break;
                    case "type|openSocket":
                        auxSocket = openSocket();
                        System.out.println("ServerSocket inicializada");
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
                        /*aux2 = aux[1];
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
                        }*/
                        break;
                    case "type|createSongwriter":
                        connection = initConnection();
                        String[] nameParts1 = aux[1].split("\\|");
                        String[] descriptionParts = aux[2].split("\\|");
                        PreparedStatement stmtSongwriter = null;
                        try {
                            if(checkArtistExists(nameParts1[1]) == 1){
                                connection.close();
                                sendMsg("type|artistExists");
                                System.out.println("Artist already exists.");
                            }
                            else{
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
                                connection.close();
                                System.out.println("Records created successfully");
                                sendMsg("type|createSongwriterComplete");
                            }
                        } catch (org.postgresql.util.PSQLException e) {
                            sendMsg("type|somethingWentWrong");
                            System.out.println("ERRO: Something went wrong");
                        }
                        break;
                    case "type|createMusician":
                        connection = initConnection();
                        String[] namePartsMusician = aux[1].split("\\|");
                        String[] descriptionPartsMusician = aux[2].split("\\|");
                        String[] songwriterParts = aux[3].split("\\|");
                        String[] isComposerParts = aux[4].split("\\|");
                        String[] isBandParts = aux[5].split("\\|");
                        PreparedStatement stmtMusician = null;
                        try {
                            if(checkArtistExists(namePartsMusician[1]) == 1){
                                connection.close();
                                sendMsg("type|artistExists");
                                System.out.println("Artist already exists.");
                            }
                            else{
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
                                connection.close();
                                System.out.println("Records created successfully");
                                sendMsg("type|createMusicianComplete");
                            }
                        } catch (org.postgresql.util.PSQLException e) {
                            sendMsg("type|musicianExists");
                            System.out.println("ERRO: Something went wrong.");
                        }
                        break;
                    case "type|createComposer":
                        connection = initConnection();
                        String[] namePartsComposer = aux[1].split("\\|");
                        String[] descriptionPartsComposer = aux[2].split("\\|");
                        PreparedStatement stmtComposer = null;
                        try {
                            if(checkArtistExists(namePartsComposer[1]) == 1){
                                connection.close();
                                sendMsg("type|artistExists");
                                System.out.println("Artist already exists.");
                            }
                            else{
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
                                connection.close();
                                System.out.println("Records created successfully");
                                sendMsg("type|createComposerComplete");
                            }

                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|composerExists");
                            System.out.println("ERRO: Composer already exists.");
                        }
                        break;
                    case "type|createBand":
                        connection = initConnection();
                        String[] namePartsBand = aux[1].split("\\|");
                        String[] descriptionPartsBand = aux[2].split("\\|");
                        PreparedStatement stmtBand = null;
                        try {
                            if(checkArtistExists(namePartsBand[1]) == 1){
                                connection.close();
                                sendMsg("type|artistExists");
                                System.out.println("Artist already exists.");
                            }
                            else{
                                Band a = new Band(namePartsBand[1],descriptionPartsBand[1]);
                                connection.setAutoCommit(false);
                                System.out.println("Opened database successfully");

                                stmtBand = connection.prepareStatement("INSERT INTO artista (id,name,description,musician_ismusician,band_isband,songwriter_issongwriter,composer_iscomposer)"
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
                                connection.close();
                                System.out.println("Records created successfully");
                                sendMsg("type|createBandComplete");
                            }
                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|bandExists");
                            System.out.println("ERRO: Band already exists.");
                        }
                        break;
                    case "type|createAlbum":
                        connection = initConnection();
                        Artist artist = new Musician();
                        String[] namePa = aux[1].split("\\|");
                        String[] gParts = aux[2].split("\\|");
                        String[] descripParts = aux[3].split("\\|");
                        String[] aName = aux[4].split("\\|");
                        String[] pName = aux[5].split("\\|");
                        PreparedStatement stmtAlbum = null;
                        boolean flagAlbum = false;

                        try{
                            if(checkAlbumExists(namePa[1]) == 1){
                                connection.close();
                                sendMsg("type|albumExists");
                                System.out.println("Album already exists.");
                            }else{
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

                                int artistId = getArtistIdByName(aName[1]);
                                int albumId = getAlbumIdByName(namePa[1]);
                                stmtAlbum = connection.prepareStatement("INSERT INTO artista_album(artista_id, album_id)"
                                        + "VALUES (?,?);");
                                stmtAlbum.setInt(1,artistId);
                                stmtAlbum.setInt(2,albumId);
                                stmtAlbum.executeUpdate();

                                stmtAlbum.close();
                                connection.commit();
                                connection.close();
                                System.out.println("Records created successfully");
                                sendMsg("type|createAlbumComplete");
                            }

                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println(e.getMessage());
                            sendMsg("type|createAlbumFailed");
                            System.out.println("ERRO: Album creation failed.");
                        }
                        break;
                    case "type|createConcert":
                        connection = initConnection();
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


                            stmtConcert.setString(1,a.getName());
                            stmtConcert.setString(2,concertDescription[1]);
                            stmtConcert.setString(3,a.getLocation());
                            stmtConcert.executeUpdate();

                            stmtConcert.close();
                            connection.commit();
                            connection.close();
                            System.out.println("Records created successfully");
                            sendMsg("type|createConcertComplete");

                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|createConcertFailed");
                            System.out.println("ERRO: Concert already exists.");
                        }
                        break;
                    case "type|concertAssociation":
                        connection = initConnection();
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
                            connection.close();
                            System.out.println("Records created successfully");
                            sendMsg("type|createConcertComplete");

                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|concertExists");
                            System.out.println("ERRO: Concert already exists.");
                        }
                        break;
                    case "type|createPublisher":
                        connection = initConnection();
                        String[] publisherName = aux[1].split("\\|");
                        PreparedStatement stmtPublisher = null;
                        try {
                            if(checkPublisherExists(publisherName[1]) == 1){
                                connection.close();
                                sendMsg("type|publisherExists");
                                System.out.println("Publisher already exists.");
                            }
                            Publisher a = new Publisher(publisherName[1]);
                            connection.setAutoCommit(false);
                            System.out.println("Opened database successfully");

                            stmtPublisher = connection.prepareStatement("INSERT INTO publisher (id,name)"
                                    + "VALUES (DEFAULT,?);");
                            stmtPublisher.setString(1,a.getName());
                            stmtPublisher.executeUpdate();

                            stmtPublisher.close();
                            connection.commit();
                            connection.close();
                            System.out.println("Records created successfully");
                            sendMsg("type|createPublisherComplete");

                        }catch(org.postgresql.util.PSQLException e){
                            sendMsg("type|somthingWentWrong");
                            System.out.println("ERRO: Publisher already exists.");
                        }
                        break;
                    case "type|createPlaylist":
                        connection = initConnection();
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
                            connection.close();
                            System.out.println("Records created successfully");
                            sendMsg("type|createPlaylistComplete");
                        }catch(org.postgresql.util.PSQLException e) {
                            sendMsg("type|createPlaylistFailed");
                            System.out.println("ERRO: Something went wrong.");
                        }
                        break;
                    case "type|editArtistName":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] nameB = aux[1].split("\\|");
                        String[] nameAfter = aux[2].split("\\|");

                        try{
                            if(artistDataBaseEmpty() || getArtistIdByName(nameB[1])==0 || getArtistIdByName(nameAfter[1])==1){
                                if(artistDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|artistDatabaseEmpty");
                                    System.out.println("Artist database empty.");
                                }
                                else if(getArtistIdByName(nameB[1])==0){
                                    connection.close();
                                    sendMsg("type|artistNotFound");
                                    System.out.println("Artist not found.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|nameAlreadyTaken");
                                    System.out.println("There's already an artist with that name.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditName = connection.prepareStatement("UPDATE artista SET name = ? WHERE name =?;");
                                stmtEditName.setString(1,nameAfter[1]);
                                stmtEditName.setString(2,nameB[1]);
                                stmtEditName.executeUpdate();

                                connection.commit();
                                connection.close();
                                sendMsg("type|nameChanged");
                                System.out.println("Name changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editArtistType":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] NameP = aux[1].split("\\|");
                        String[] typePart = aux[2].split("\\|");

                        try{
                            if(artistDataBaseEmpty() || getArtistIdByName(NameP[1])==0){
                                if(artistDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|artistDatabaseEmpty");
                                    System.out.println("Artist database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|artistNotFound");
                                    System.out.println("Artist not found.");
                                }
                            }
                            else{
                                if(typePart[1].equals("songwriter")){
                                    PreparedStatement stmtEditType = connection.prepareStatement("UPDATE artista SET songwriter_issongwriter = true WHERE name = ?;");
                                    stmtEditType.setString(1,NameP[1]);
                                    stmtEditType.executeUpdate();
                                    System.out.println("Changes applied.");
                                    sendMsg("type|changesApplied");
                                }
                                else if(typePart[1].equals("composer")){
                                    PreparedStatement stmtEditType = connection.prepareStatement("UPDATE artista SET composer_iscomposer = true WHERE name = ?;");
                                    stmtEditType.setString(1,NameP[1]);
                                    stmtEditType.executeUpdate();
                                    System.out.println("Changes applied.");
                                    sendMsg("type|changesApplied");
                                }
                                else if(typePart[1].equals("both")){
                                    PreparedStatement stmtEditType = connection.prepareStatement("UPDATE artista SET composer_iscomposer = true WHERE name = ?;");
                                    stmtEditType.setString(1,NameP[1]);
                                    stmtEditType.executeUpdate();

                                    stmtEditType = connection.prepareStatement("UPDATE artista SET songwriter_issongwriter = true WHERE name = ?;");
                                    stmtEditType.setString(1,NameP[1]);
                                    stmtEditType.executeUpdate();

                                    connection.commit();
                                    connection.close();
                                    System.out.println("Changes applied.");
                                    sendMsg("type|changesApplied");
                                }
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editArtistDescription":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] artistNamePartss = aux[1].split("\\|");
                        String[] descriptionAfterParts = aux[2].split("\\|");

                        try{
                            if(artistDataBaseEmpty() || getArtistIdByName(artistNamePartss[1])==0){
                                if(artistDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|artistDatabaseEmpty");
                                    System.out.println("Artist database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|artistNotFound");
                                    System.out.println("Artist not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditDesc = connection.prepareStatement("UPDATE artista SET description = ? WHERE name = ?;");
                                stmtEditDesc.setString(1,descriptionAfterParts[1]);
                                stmtEditDesc.setString(2,artistNamePartss[1]);
                                stmtEditDesc.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|descriptionChanged");
                                System.out.println("Description changed.");
                            }

                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editPublisherName":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] PNameB = aux[1].split("\\|");
                        String[] PNameA = aux[2].split("\\|");

                        try{
                            if(publisherDataBaseEmpty() || getPublisherById(PNameB[1])==0 || getPublisherById(PNameA[1])!=0){
                                if(publisherDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|publisherDatabaseEmpty");
                                    System.out.println("Publisher database empty.");
                                }
                                else if(getPublisherById(PNameB[1])==0){
                                    connection.close();
                                    sendMsg("type|publisherNotFound");
                                    System.out.println("Publisher not found.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|nameAlreadyTaken");
                                    System.out.println("Name already taken by another publisher.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditPub = connection.prepareStatement("UPDATE publisher SET name = ? WHERE name = ?;");
                                stmtEditPub.setString(1,PNameA[1]);
                                stmtEditPub.setString(2,PNameB[1]);
                                stmtEditPub.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|nameChanged");
                                System.out.println("Name changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|addMusicPlaylist":
                        String[] playlist = aux[1].split("\\|");
                        String[] music = aux[2].split("\\|");
                        String[] userIds = aux[3].split("\\|");

                        connection = initConnection();
                        connection.setAutoCommit(false);

                        try{
                            if(playlistDataBaseEmpty() || getPlaylistIdByName(playlist[1])==0){
                                if(playlistDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|playlistDatabaseEmpty");
                                    System.out.println("Playlist database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|playlistNotFound");
                                    System.out.println("Playlist not found.");
                                }
                            }
                            else{
                                int musicI = getMusicIdByName(music[1]);
                                int playId = getPlaylistIdByName(playlist[1]);

                                PreparedStatement stmtAddMusic = connection.prepareStatement("INSERT INTO playlist_music(playlist_id, playlist_utilizador_id, music_id)"
                                        +"VALUES(?,?,?)");
                                stmtAddMusic.setInt(1,playId);
                                stmtAddMusic.setInt(2,Integer.parseInt(userIds[1]));
                                stmtAddMusic.setInt(3,musicI);
                                stmtAddMusic.executeUpdate();


                                connection.commit();
                                connection.close();
                                sendMsg("type|musicAddCompleted");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editPlaylistName":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] PlNameB = aux[1].split("\\|");
                        String[] PlNameA = aux[2].split("\\|");

                        try{
                            if(playlistDataBaseEmpty() || getPlaylistIdByName(PlNameB[1])==0){
                                if(playlistDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|playlistDatabaseEmpty");
                                    System.out.println("Playlist database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|playlistNotFound");
                                    System.out.println("Playlist not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditPub = connection.prepareStatement("UPDATE playlist SET name = ? WHERE name = ?;");
                                stmtEditPub.setString(1,PlNameA[1]);
                                stmtEditPub.setString(2,PlNameB[1]);
                                stmtEditPub.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|nameChanged");
                                System.out.println("Name changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editConcertName":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] CNameB = aux[1].split("\\|");
                        String[] CNameA = aux[2].split("\\|");

                        try{
                            if(concertDataBaseEmpty() || getConcertIdByName(CNameB[1])==0){
                                if(concertDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|concertDatabaseEmpty");
                                    System.out.println("Concert database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|concertNotFound");
                                    System.out.println("Concert not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditPub = connection.prepareStatement("UPDATE concert SET name = ? WHERE name = ?;");
                                stmtEditPub.setString(1,CNameA[1]);
                                stmtEditPub.setString(2,CNameB[1]);
                                stmtEditPub.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|nameChanged");
                                System.out.println("Name changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editConcertLocation":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] CName = aux[1].split("\\|");
                        String[] CLocationA = aux[2].split("\\|");

                        try{
                            if(concertDataBaseEmpty() || getConcertIdByName(CName[1])==0){
                                if(concertDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|concertDatabaseEmpty");
                                    System.out.println("Concert database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|concertNotFound");
                                    System.out.println("Concert not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditPub = connection.prepareStatement("UPDATE concert SET location = ? WHERE name = ?;");
                                stmtEditPub.setString(1,CLocationA[1]);
                                stmtEditPub.setString(2,CName[1]);
                                stmtEditPub.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|locationChanged");
                                System.out.println("Location changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|editConcertDescription":
                        connection = initConnection();
                        connection.setAutoCommit(false);
                        String[] CN = aux[1].split("\\|");
                        String[] CDescriptionA = aux[2].split("\\|");

                        try{
                            if(concertDataBaseEmpty() || getConcertIdByName(CN[1])==0){
                                if(concertDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|concertDatabaseEmpty");
                                    System.out.println("Concert database empty.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|concertNotFound");
                                    System.out.println("Concert not found.");
                                }
                            }
                            else{
                                PreparedStatement stmtEditPub = connection.prepareStatement("UPDATE concert SET description = ? WHERE name = ?;");
                                stmtEditPub.setString(1,CDescriptionA[1]);
                                stmtEditPub.setString(2,CN[1]);
                                stmtEditPub.executeUpdate();

                                connection.commit();
                                connection.close();

                                sendMsg("type|descriptionChanged");
                                System.out.println("Description changed.");
                            }
                        }catch(org.postgresql.util.PSQLException e){
                            System.out.println("Something went wrong.");
                            sendMsg("type|somethingWentWrong");
                        }
                        break;
                    case "type|showMusic":
                        ArrayList<String> partialM  = new ArrayList<>();
                        connection = initConnection();
                        String[] nameMusic = aux[1].split("\\|");
                        String nM = nameMusic[1];
                        int mId = getMusicIdByName(nM);
                        int partialMu = 0;
                        String title = "";
                        int lengthM = 0;

                        PreparedStatement stmtMusic = null;
                        try{
                            if(musicDataBaseEmpty()){
                                connection.close();
                                sendMsg("type|musicDatabaseEmpty");
                                System.out.println("Musics database empty.");
                            }
                            else{
                                connection.setAutoCommit(false);
                                System.out.println("Opened database successfully");
                                String partialSearch = "%"+nM+"%";
                                stmtMusic = connection.prepareStatement("SELECT * FROM music WHERE title LIKE ?");
                                stmtMusic.setString(1,partialSearch);
                                ResultSet rs = stmtMusic.executeQuery();
                                while (rs.next()) {
                                    mId = rs.getInt("id");
                                    title = rs.getString("title");
                                    lengthM = rs.getInt("length");
                                    if(title.equals(nM)){
                                        partialMu = 1;
                                    }
                                    else{
                                        partialM.add(title);
                                    }
                                }
                                System.out.println(partialMu);
                                if(partialMu == 0){
                                    if(partialM.isEmpty()){
                                        sendMsg("type|noMatchesFound");
                                    }
                                    else{
                                        sendMsg("type|partialSearchComplete;Found|"+printAlbuns(partialM));
                                        System.out.println("Partial search returned.");
                                    }
                                }
                                else{
                                    String composer = getComposerByMusicId(mId);
                                    String songwriter = getSongwriterByMusicId(mId);
                                    String album = getAlbumByMusicId(mId);
                                    String artista = getArtistNameByMusicId(mId);

                                    connection.close();
                                    System.out.println("Operation done successfully");
                                    sendMsg("type|notPartialSearchComplete;Name|"+title+";Artist|"+artista+";Composer|"+composer+";Songwriter|"+songwriter+";Album|"+album+";Length|"+lengthM);

                                }
                            }
                        }catch (org.postgresql.util.PSQLException e){
                            sendMsg("type|somethingWentWrong");
                            System.out.println(e.getMessage());
                        }
                        break;
                    case "type|showComposerMusics":
                        connection = initConnection();
                        String[] composerName = aux[1].split("\\|");
                        ArrayList<Integer> music_idss = new ArrayList<>();
                        ArrayList<String> music_namess = new ArrayList<>();
                        String nC = composerName[1];
                        try{
                            if(musicDataBaseEmpty() || checkArtistExists(nC)!=1){
                                if(musicDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|musicDatabaseEmpty");
                                    System.out.println("Musics database empty.");
                                }
                                else if(checkArtistExists(nC)==0){
                                    connection.close();
                                    sendMsg("type|composerNotFound");
                                    System.out.println("Composer not found.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|somethingWentWrong");
                                    System.out.println("Something went wrong.");
                                }
                            }
                            else{
                                PreparedStatement stmtShowArtistAlbum = connection.prepareStatement("SELECT * FROM composer_music WHERE artista_id = ?;");
                                stmtShowArtistAlbum.setInt(1,getArtistIdByName(nC));

                                ResultSet res = stmtShowArtistAlbum.executeQuery();
                                while(res.next()){
                                    music_idss.add(res.getInt("music_id"));
                                }

                                for(Integer i : music_idss){
                                    stmtShowArtistAlbum = connection.prepareStatement("SELECT * FROM music WHERE id = ?;");
                                    stmtShowArtistAlbum.setInt(1, i);
                                    res = stmtShowArtistAlbum.executeQuery();

                                    while(res.next()){
                                        music_namess.add(res.getString("title"));
                                    }
                                }

                                connection.close();
                                sendMsg("type|showComposerMusicsComplete;Albums|"+printAlbuns(music_namess));
                            }
                        }
                        catch (org.postgresql.util.PSQLException e){
                            sendMsg("type|somethingWentWrong");
                            System.out.println(e.getMessage());
                        }
                        break;
                    case "type|showSongwriterMusics":
                        connection = initConnection();
                        String[] songName = aux[1].split("\\|");
                        ArrayList<Integer> music_ids = new ArrayList<>();
                        ArrayList<String> music_names = new ArrayList<>();
                        String nS = songName[1];
                        try{
                            if(musicDataBaseEmpty() || checkArtistExists(nS)!=1){
                                if(musicDataBaseEmpty()){
                                    connection.close();
                                    sendMsg("type|musicDatabaseEmpty");
                                    System.out.println("Musics database empty.");
                                }
                                else if(checkArtistExists(nS)==0){
                                    connection.close();
                                    sendMsg("type|songwriterNotFound");
                                    System.out.println("Songwriter not found.");
                                }
                                else{
                                    connection.close();
                                    sendMsg("type|somethingWentWrong");
                                    System.out.println("Something went wrong.");
                                }
                            }
                            else{
                                PreparedStatement stmtShowArtistAlbum = connection.prepareStatement("SELECT * FROM music_songwriter WHERE artista_id = ?;");
                                stmtShowArtistAlbum.setInt(1,getArtistIdByName(nS));

                                ResultSet res = stmtShowArtistAlbum.executeQuery();
                                while(res.next()){
                                    music_ids.add(res.getInt("music_id"));
                                }

                                for(Integer i : music_ids){
                                    stmtShowArtistAlbum = connection.prepareStatement("SELECT * FROM music WHERE id = ?;");
                                    stmtShowArtistAlbum.setInt(1, i);
                                    res = stmtShowArtistAlbum.executeQuery();

                                    while(res.next()){
                                        music_names.add(res.getString("title"));
                                    }
                                }

                                connection.close();
                                sendMsg("type|showSongwriterMusicsComplete;Albums|"+printAlbuns(music_names));
                            }
                        }
                        catch (org.postgresql.util.PSQLException e){
                            sendMsg("type|somethingWentWrong");
                            System.out.println(e.getMessage());
                        }

                        break;
                    case "type|showArtist":
                        connection = initConnection();
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
                        ArrayList<String> partial = new ArrayList<>();
                        int partialS = 0;

                        PreparedStatement stmt = null;
                        try {
                            if(artistDataBaseEmpty()){
                                connection.close();
                                sendMsg("type|artistDatabaseEmpty");
                                System.out.println("Artists database empty.");
                            }
                            else{
                                connection.setAutoCommit(false);
                                System.out.println("Opened database successfully");
                                String partialSearch = "%"+n+"%";
                                stmt = connection.prepareStatement("SELECT * FROM artista WHERE name LIKE ?");
                                stmt.setString(1,partialSearch);
                                ResultSet rs = stmt.executeQuery();
                                while (rs.next()) {
                                    id1 = rs.getInt("id");
                                    name1 = rs.getString("name");
                                    description1 = rs.getString("description");
                                    isMusician = rs.getBoolean("musician_ismusician");
                                    isBand = rs.getBoolean("band_isband");
                                    isSongwriter = rs.getBoolean("songwriter_issongwriter");
                                    isComposer = rs.getBoolean("composer_iscomposer");
                                    if(name1.equals(n)){
                                        partialS = 1;
                                    }
                                    else{
                                        partial.add(name1);
                                    }
                                }
                                System.out.println(partialS);
                                if(partialS == 0){
                                    if(partial.isEmpty()){
                                        sendMsg("type|noMatchesFound");
                                    }
                                    else{
                                        sendMsg("type|partialSearchComplete;Found|"+printAlbuns(partial));
                                        System.out.println("Partial search returned.");
                                    }

                                }

                                albIds = getArtistAlbumsIdByArtistId(id1);


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
                                connection.close();

                                System.out.println("Operation done successfully");
                                sendMsg("type|notPartialSearchComplete;Name|"+name1+";Description|"+description1+";Functions|"+printFunctions(isMusician,isBand,isSongwriter,isComposer)+";Albums|"+printAlbuns(albNames));
                            }
                        } catch ( Exception e ) {
                            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
                            System.exit(0);
                        }
                        break;
                    case "type|showArtistAlbums":
                        connection = initConnection();
                        ArrayList<Integer> album_ids = new ArrayList<>();
                        ArrayList<String> album_names = new ArrayList<>();
                        String[] nameA = aux[1].split("\\|");
                        String nA = nameA[1];
                        try{
                            if(artistDataBaseEmpty()){
                                connection.close();
                                sendMsg("type|artistDatabaseEmpty");
                                System.out.println("Artist database empty.");
                            }
                            else{
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

                                connection.close();
                                sendMsg("type|showArtistAlbumsComplete;Albums|"+printAlbuns(album_names));
                            }
                        }
                        catch (org.postgresql.util.PSQLException e){
                            sendMsg("type|somethingWentWrong");
                            System.out.println(e.getMessage());
                        }
                        break;
                    case "type|makeCritic":
                        connection = initConnection();
                        String[] scoreParts = aux[1].split("\\|");
                        String[] textParts = aux[2].split("\\|");
                        String[] albName = aux[3].split("\\|");
                        String[] userId = aux[4].split("\\|");
                        double score = Double.parseDouble(scoreParts[1]);

                        if(albumDatabaseEmpty()){
                            connection.close();
                            sendMsg("type|makeCriticFail");
                            System.out.println("ERROR: No Albuns in the database.");
                        }
                        else{
                            PreparedStatement stmtCritic = null;

                            try{
                                int albumId = getAlbumIdByName(albName[1]);
                                if(albumId == 0 || albumDatabaseEmpty() || (score<0 && score>10)){
                                    if(albumId==0){
                                        sendMsg("type|albumNotFound");
                                        System.out.println("Album not found.");
                                    }
                                    else if((score<0 && score>10)){
                                        sendMsg("type|invalidScore");
                                        System.out.println("Invalid score.");
                                    }
                                    else{
                                        sendMsg("type|albumDatabaseEmpty");
                                        System.out.println("Album database empty.");
                                    }
                                }
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
                                connection.close();
                                sendMsg("type|criticComplete");
                            }catch(org.postgresql.util.PSQLException e){
                                sendMsg("type|somethingWentWrong");
                                System.out.println("Something went wrong.");
                            }
                        }
                        break;
                    case "type|showAlbum":
                        connection = initConnection();
                        String[] albuName = aux[1].split("\\|");
                        Album album = new Album();
                        if(albumDatabaseEmpty()){
                            connection.close();
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
                            ArrayList<String> partialNames = new ArrayList<>();
                            int length = 0;
                            int publisherId = 0;
                            int id = 0;
                            int artistId = 0;
                            double scoreFinal = 0;
                            int partialA = 0;


                            try {
                                connection.setAutoCommit(false);
                                System.out.println("Opened database successfully");
                                String partialN = "%"+albuName[1]+"%";
                                stmtShowAlbum = connection.prepareStatement("SELECT * FROM album WHERE name LIKE ?;");
                                stmtShowAlbum.setString(1, partialN);
                                ResultSet rs = stmtShowAlbum.executeQuery();
                                while (rs.next()) {
                                    id = rs.getInt("id");
                                    nome = rs.getString("name");
                                    genre = rs.getString("genre");
                                    description = rs.getString("description");
                                    length = rs.getInt("length");
                                    publisherId = rs.getInt("publisher_id");
                                    if(nome.equals(albuName[1])){
                                        partialA = 1;
                                    }
                                    else{
                                        partialNames.add(nome);
                                    }
                                }

                                stmtShowAlbum.close();
                                if(partialA==0){
                                    if(partialNames.isEmpty()){
                                        sendMsg("type|noMatchesFound");
                                    }
                                    else{
                                        sendMsg("type|partialSearchAlbumComplete;Found|"+printAlbuns(partialNames));
                                        System.out.println("Partial search complete.");
                                    }

                                }

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

                                connection.close();

                                sendMsg("type|notPartialSearchAlbumComplete" + ";AlbumName|" + nome +";ArtistName|"+artistName
                                        + ";Description|"+description+";Length|"+length+";Genre|"+genre+";ScoreFinal|"+scoreFinal
                                        +";CriticsList|"+printCritics(criticsList)+";MusicsList|"+printMusics(musicsList)+";Publisher|"+publisherN);
                            }catch(org.postgresql.util.PSQLException e){
                                System.out.println("except");
                                sendMsg("type|showAlbumFailed");
                            }
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

    private int checkHasAccess(String musicPart, String shareUserPart) {
        connection = initConnection();
        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM utilizador_filearchive WHERE utilizador_id = ? AND filearchive_music_id = ?;");
            stmt.setInt(1,getUserIdByName(shareUserPart));
            stmt.setInt(2,getMusicIdByName(musicPart));
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return 1;
            }
            else{
                return 0;
            }
        } catch (SQLException e) {
            System.out.println("Something went wrong verifying if exists.");
        }
        return 0;
    }

    private int getPlaylistIdByName(String s) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM playlist WHERE name = ?;");
            stmt.setString(1,s);
            ResultSet rs = stmt.executeQuery();

            int playListId = 0;
            while(rs.next()){
                playListId = rs.getInt("id");
            }
            return playListId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private boolean playlistDataBaseEmpty() {
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM playlist");
            return !rs.next();
        }
        catch (org.postgresql.util.PSQLException e){
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getArtistNameByMusicId(int mId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM artista_music WHERE music_id = ?;");
            stmt.setInt(1,mId);
            ResultSet rs = stmt.executeQuery();

            int artistId = 0;
            while(rs.next()){
                artistId = rs.getInt("artista_id");
            }
            return getArtistNameById(artistId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getAlbumByMusicId(int mId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM album_music WHERE music_id = ?;");
            stmt.setInt(1,mId);
            ResultSet rs = stmt.executeQuery();

            int albumId = 0;
            while(rs.next()){
                albumId = rs.getInt("album_id");
            }
            return getAlbumNameById(albumId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getAlbumNameById(int albumId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM album WHERE id = ?;");
            stmt.setInt(1,albumId);
            ResultSet rs = stmt.executeQuery();

            String albumName = "";
            while(rs.next()){
                albumName = rs.getString("name");
            }
            return albumName;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getSongwriterByMusicId(int mId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM music_songwriter WHERE music_id = ?;");
            stmt.setInt(1,mId);
            ResultSet rs = stmt.executeQuery();

            int artistId = 0;
            while(rs.next()){
                artistId = rs.getInt("artista_id");
            }
            return getArtistNameById(artistId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getComposerByMusicId(int mId) {
        try{
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM composer_music WHERE music_id = ?;");
            stmt.setInt(1,mId);
            ResultSet rs = stmt.executeQuery();

            int artistId = 0;
            while(rs.next()){
                artistId = rs.getInt("artista_id");
            }
            return getArtistNameById(artistId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private ArrayList<Integer> getArtistAlbumsIdByArtistId(int id1) {
        ArrayList<Integer> albIds = new ArrayList<>();
        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM artista_album WHERE artista_id = ?;");
            stmt.setInt(1,id1);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                int albid = rs.getInt("album_id");
                albIds.add(albid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return albIds;
    }

    private boolean concertDataBaseEmpty() {
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM concert");
            return !rs.next();
        }
        catch (org.postgresql.util.PSQLException e){
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean publisherDataBaseEmpty() {
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM publisher");
            return !rs.next();
        }
        catch (org.postgresql.util.PSQLException e){
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean musicDataBaseEmpty() {
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM music");
            return !rs.next();
        }
        catch (org.postgresql.util.PSQLException e){
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean artistDataBaseEmpty() {
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM artista");
            return !rs.next();
        }
        catch (org.postgresql.util.PSQLException e){
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private int checkIfSongwriterValid(String sPart) {
            try{
                boolean isSongwriter = false;
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM artista WHERE name =?;");
                stmt.setString(1,sPart);
                ResultSet rs = stmt.executeQuery();

                if(checkArtistExists(sPart)==1){
                    while(rs.next()){
                        isSongwriter = rs.getBoolean("songwriter_issongwriter");
                    }

                    if(isSongwriter){
                        return 1;
                    }
                    else{
                        return 2;
                    }
                }
                else{
                    return 0;
                }
            } catch (SQLException e) {
                System.out.println(e.getSQLState());
            }
        return -1;
    }

    private int checkIfComposerValid(String composerPart) {
        try{
            boolean isComposer = false;
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM artista WHERE name =?;");
            stmt.setString(1,composerPart);
            ResultSet rs = stmt.executeQuery();

            if(checkArtistExists(composerPart)==1){
                while(rs.next()){
                    isComposer = rs.getBoolean("composer_iscomposer");
                }

                if(isComposer){
                    return 1;
                }
                else{
                    return 2;
                }
            }
            else{
                return 0;
            }
        } catch (SQLException e) {
            System.out.println(e.getSQLState());
        }
        return -1;
    }

    private Connection initConnection() {
        try{
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/BD/SD","postgres", "fabiogc1998");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return connection;
    }

    private int checkDuplicatedUpload(int loggedUser, String title) {
        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM music WHERE title = ?;");
            stmt.setString(1,title);
            ResultSet rs = stmt.executeQuery();

            if(rs.next()){
                int musicId = getMusicIdByName(title);
                stmt = connection.prepareStatement("SELECT * FROM filearchive WHERE music_id = ?");
                stmt.setInt(1,musicId);
                rs = stmt.executeQuery();

                if(rs.next()){
                    int user_id = rs.getInt("utilizador_id");
                    System.out.println("user check"+user_id+loggedUser+"="+(user_id==loggedUser));
                    if(user_id == loggedUser){
                        return 1;
                    }
                    else{
                        return 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getSQLState());
        }
        return -1;
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
                int musicId = rs.getInt("filearchive_music_id");
                String musicName = getMusicNameById(musicId);
                m.add(new Music(musicName));
            }

            return m;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private String getMusicNameById(int musicId) {
        String musicName = "";

        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM music WHERE id = ?;");
            stmt.setInt(1,musicId);

            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                musicName = rs.getString("title");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return musicName;
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
        connection = initConnection();
        connection.setAutoCommit(false);
        PreparedStatement stmt = connection.prepareStatement("UPDATE utilizador SET iseditor = true WHERE username = ?");
        stmt.setString(1,username);
        stmt.executeUpdate();

        stmt.close();
        connection.commit();
        connection.close();
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
        InputStream is = socket.getInputStream();
        FileOutputStream fOutStream = new FileOutputStream("./musicasServer/" + musicName);
        BufferedOutputStream bOutStream = new BufferedOutputStream(fOutStream);

        int aux= 0;
        int cont= 0;
        while ((aux = is.read(b))!=-1){
            System.out.println(cont++);
            bOutStream.write(b, 0, aux);
            if(is.available()==0){
                break;
            }
        }
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
        connection = initConnection();
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
        connection = initConnection();
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM album");
            return !rs.next();
        }
        catch (org.postgresql.util.PSQLException e){
            return false;
        }
    }

    public User checkUsernameLogin(String username, String password){
        connection = initConnection();
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
        connection = initConnection();
        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM utilizador WHERE username = ?;");
            stmt.setString(1,username);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return 1;
            }
            else{
                return 0;
            }
        } catch (SQLException e) {
            System.out.println("Something went wrong verifying if exists.");
        }
        return 0;
    }

    public int checkArtistExists(String name){
        connection = initConnection();
        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM artista WHERE name = ?;");
            stmt.setString(1,name);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return 1;
            }
            else{
                return 0;
            }
        } catch (SQLException e) {
            System.out.println("Something went wrong verifying if exists.");
        }
        return -1;
    }

    public int checkAlbumExists(String name){
        connection = initConnection();
        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM album WHERE name = ?;");
            stmt.setString(1,name);
            ResultSet rs = stmt.executeQuery();

            if(rs.next()){
                return 1;
            }
            else{
                return 0;
            }

        } catch (SQLException e) {
            System.out.println(e.getSQLState());
        }
        return -1;
    }

    public int checkPublisherExists(String name){
        connection = initConnection();
        try{
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM publisher WHERE name = ?;");
            stmt.setString(1,name);
            ResultSet rs = stmt.executeQuery();

            if(rs.next()){
                return 1;
            }
            else{
                return 0;
            }

        } catch (SQLException e) {
            System.out.println(e.getSQLState());
        }
        return -1;
    }
}
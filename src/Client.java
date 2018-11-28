/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 * Neither the name of Oracle nor the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

import com.sun.org.apache.xpath.internal.SourceTree;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;
import java.rmi.registry.Registry;
import java.net.*;

public class Client extends UnicastRemoteObject implements ClientHello{

    private static User loggedUser;
    private Client() throws RemoteException {}
    private static ClientHello client;

    static {
        try {
            client = new Client();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, NotBoundException {
        String text = "";
        Hello rmi = null;
        //String host = (args.length < 1) ? null : args[0];
        Scanner reader = new Scanner(System.in);
        System.getProperties().put("java.security.policy", "policy.all");
        System.setSecurityManager(new RMISecurityManager());
        try {
            Registry registry = LocateRegistry.getRegistry("localhost",7000);
            rmi =(Hello) registry.lookup("Hello");
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        while(!text.trim().equals("quit")){
            try{
                System.out.println("Escreva a sua mensagem:");
                text = reader.nextLine();
                rmi.msgInput(text);
                switch(text.trim()){
                    case "/login":
                        login(rmi, reader);
                        break;
                    case "/register":
                        registo(rmi, reader);
                        break;
                    default:
                        if(!text.trim().equals("quit")){
                            System.out.println("Este comando não faz nada. Para sair escreva 'quit'");
                        }
                }
            }
            catch(RemoteException e){
                rmi = changeRMI();
                //por aqui pra ler as opçoes
            }
        }
        reader.close();
        System.out.println("Finished");
        return ;
    }

            ///////////// REGISTO, LOGIN, LOGOUT /////////////
    public static void registo(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Insert your data('username-password')");
        boolean flagOK = false;
        String txt = "";
        String[] txtSplit;
        while(!flagOK){
            txt = reader.nextLine();
            txtSplit = txt.split("-");
            if(txtSplit.length==2){
                if(txtSplit[0].trim().equals("")){
                    System.out.println("Insert valida data ('username-password')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert valida data ('username-password')");
            }
        }
        txt = rmi.checkRegister(txt);
        switch (txt){
            case "type|usernameUsed":
                System.out.println("That username already exists.");
                break;
            case "type|registComplete":
                System.out.println("Successful register.");
                break;
            default:
                System.out.println("Something went wrong.");
        }
    }

    public static void login(Hello rmi, Scanner reader) throws IOException, NotBoundException {
        System.out.println("Insert your login('username-password'):");
        boolean flagOK = false;
        String userData = "";
        String[] userDataParts = new String[2];
        while(!flagOK){
            userData = reader.nextLine();
            userDataParts = userData.split("-");
            if(userDataParts.length==2){
                if(userDataParts[0].trim().equals("")){
                    System.out.print("Insert your login('username-password'): ");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your login('username-password'):");
            }
        }
        String txt = rmi.checkLogin(userData);
        String[] txtSplit = txt.split(";");
        switch (txtSplit[0].trim()){
            case "type|loginComplete":
                String[] id = txtSplit[1].split("\\|");
                String[] editor = txtSplit[2].split("\\|");
                loggedUser = new User(Integer.parseInt(id[1]),userDataParts[0],Boolean.parseBoolean(editor[1]));
                loggedUser.setClientInterface(client);
                System.out.println("Welcome!");

                rmi.addOnlineUser(loggedUser);
                menuPrincipal(rmi,reader);
                break;
            case "type|userLogged":
                System.out.println("User already logged in.");
                break;
            case "type|loginFail":
                System.out.println("Login failed.");
                break;
            default:
                System.out.println("Something went wrong.");
        }
    }

    public static void logout(Hello rmi) throws RemoteException{
        rmi.checkLogout(loggedUser);
        System.out.println("Logged out successfully.");
        loggedUser = new User();
    }

            ////////////// MENU PRINCIPAL COM A FUNÇÕES INICIAIS /////////////
    public static void menuPrincipal(Hello rmi, Scanner reader) throws IOException, NotBoundException {
        boolean flag = false;
        while(true){
            try{
                if(loggedUser.isEditor()){
                    System.out.println("\n\nMENU PRINCIPAL:\n" +
                            "Search\n" +
                            "Edit\n" +
                            "Make editor\n" +
                            "Make Critic\n"+
                            "Share music\n"+
                            "Playlists\n"+
                            "Upload\n" +
                            "Download\n\n" +
                            "Choose an option: ");
                }
                else{
                    System.out.println("MENU PRINCIPAL:\n" +
                            "Search\n" +
                            "Make Critic\n"+
                            "Share music\n"+
                            "Upload\n" +
                            "Download\n\n" +
                            "Choose an option: ");}

                String text = reader.nextLine();
                while(!flag){
                    if(text.trim().equals("/login") || text.trim().equals("/register")){
                        System.out.println("To login into another account or register another user, please logout first!");
                        text = reader.nextLine();
                    }
                    else{
                        flag=true;
                    }
                }
                rmi.msgInput(text);
                switch(text.trim()){
                    case "/search":
                        menuDePesquisa(rmi, reader);
                        break;
                    case "/share":
                        ArrayList<Music> musicsList = new ArrayList<>();
                        String music ="";
                        String userName="";
                        boolean flagOK = false;
                        System.out.println("Which music you wanna share?");
                        String response = rmi.getMusicList(loggedUser.getId());
                        String[] rSplit = response.split(";");
                        String[] musicParts = rSplit[1].split("\\|");
                        musicsList = createMusicsList(musicParts[1].split(","));
                        int i = 1;
                        for (Music m : musicsList) {
                            System.out.println(i+". "+m.toString());
                            i++;
                        }

                        while(!flagOK){
                            music = reader.nextLine();
                            if(!music.trim().equals("")){
                                flagOK=true;
                            }
                            else{
                                System.out.println("Which music you wanna share?");
                            }
                        }

                        flagOK = false;
                        System.out.println("With who you wanna share it?");
                        while(!flagOK){
                            userName = reader.nextLine();
                            if(!userName.trim().equals("")){
                                flagOK=true;
                            }
                            else{
                                System.out.println("With who you wanna share it?");
                            }
                        }
                        String resposta = rmi.shareMusic(music,userName,loggedUser.getId());
                        switch (resposta){
                            case "type|isAlreadyDownloadable":
                                System.out.println("User already has access to the song.");
                                break;
                            case "type|musicShareCompleted":
                                System.out.println("Music shared.");
                                break;
                            case "type|invalidMusic":
                                System.out.println("Insert a valid music.");
                                break;
                            case "type|invalidUser":
                                System.out.println("Insert a valid user.");
                                break;
                            default:
                                System.out.println("Something went wrong.");
                        }
                        break;
                    case "/playlist":
                        menuPlaylist(rmi,reader);
                        break;
                    case "/edit":
                        if(loggedUser.isEditor()){
                            editorMenu(rmi, reader);
                        }
                        else{
                            System.out.println("You don't have permission to do that.");
                        }
                        break;
                    case "/logout":
                        logout(rmi);
                        return;
                    case "/makeeditor":
                        if(loggedUser.isEditor()){
                            makeEditor(rmi,reader);
                        }
                        else{
                            System.out.println("You don't have permission to do that.");
                        }
                        break;
                    case "/makecritic":
                        makeCritic(rmi,reader);
                        break;
                    case "/download":
                        System.out.println(downloadMusic(rmi, reader));
                        break;
                    case "/upload":
                        String[] musicInfo = new String[7];
                        while(true){
                            musicInfo = sendMusic(rmi,reader);
                            break;
                        }
                        if(musicInfo == null){

                        }
                        else {
                            String resp = rmi.sendMusicRMI(musicInfo, loggedUser.getId());
                            String[] responseSpli = resp.split(";");
                            switch (responseSpli[0]) {
                                case "type|duplicatedUpload":
                                    System.out.println("ERROR: Music already uploaded by you.");
                                    break;
                                case "type|composerNotValid":
                                    System.out.println("ERROR: Not a valid composer.");
                                    break;
                                case "type|composerNotFound":
                                    System.out.println("ERROR: Composer not found.");
                                    break;
                                case "type|artistNotFound":
                                    System.out.println("ERROR: Artist not found..");
                                    break;
                                case "type|songwriterNotValid":
                                    System.out.println("ERROR: Not a valid songwriter.");
                                    break;
                                case "type|songwriterNotFound":
                                    System.out.println("ERROR: Songwriter not found.");
                                    break;
                                case "type|albumNotFound":
                                    System.out.println("ERROR: Album not found.");
                                    break;
                                case "type|sendMusicComplete":
                                    System.out.println("Upload complete.");
                                    break;
                                case "type|somethingWentWrong":
                                    System.out.println("Something went wrong.");
                                    break;
                            }
                        }
                }
            }
            catch(RemoteException e){
                rmi = changeRMI();
            }
        }
    }

    private static void menuPlaylist(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("What do you want to do: Create or Edit?");
        String resp = reader.nextLine();
        switch(resp){
            case "/edit":
                editPlaylist(rmi,reader);
                break;
            case "/create":
                createPlaylist(rmi,reader);
                break;
            case "/delete":
                deletePlaylist(rmi,reader);
                break;
            default:
                System.out.println("Inser a valid answer.");
        }
    }

    private static void deletePlaylist(Hello rmi, Scanner reader) {
        System.out.println("Which playlist do you wanna delete? ");
        boolean flagOK = false;
        String playlist = "";
        String nameAfter="";
        while(!flagOK) {
            playlist = reader.nextLine();
            if (!playlist.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which playlist do you wanna delete?");
            }
        }


    }

    ////////////// MENU DE PESQUISA COM AS SUAS FUNÇÕES/////////////
    public static void menuDePesquisa(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to search: Artist, Music, Album?");
        boolean flagOK = false;
        String text = "";
        while(!flagOK){
            text = reader.nextLine();
            if(!text.trim().equals("")){
                flagOK=true;
            }
            else{
                System.out.println("What do you want to search: Artist, Music, Album?");
            }
        }
        switch (text.trim()) {
            case "/artist":
                searchArtist(rmi, reader);
                break;
            case "/album":
                System.out.println("How do you wanna search for albums: Album Name or Artist Name?");
                String answer = reader.nextLine();
                switch (answer.trim()){
                    case "/albumname":
                        searchAlbumName(rmi, reader);
                        break;
                    case "/artistname":
                        int i= searchAlbumByArtistName(rmi, reader);
                        if(i==1){
                            searchAlbumName(rmi,reader);
                        }
                        break;
                    default:
                        System.out.println("Insert valid answer.");

                }
                break;
            case "/music":
                System.out.println("How do you wanna search for musics: Name, Songwriter, Composer");
                String ans = reader.nextLine();
                switch (ans){
                    case "/name":
                        searchMusicName(rmi,reader);
                        break;
                    case "/songwriter":
                        int i = searchMusicSongwriter(rmi,reader);
                        if(i==1){
                            searchMusicName(rmi,reader);
                        }
                        break;
                    case "/composer":
                        int j = searchMusicComposer(rmi,reader);
                        if(j==1){
                            searchMusicName(rmi,reader);
                        }
                        break;
                    default:
                        System.out.println("Insert valid answer.");
                        break;
                }
                break;
            default:
                System.out.println("Inseriu mal o comando. Por favor volte a tentar.");
        }
    }

    private static int searchMusicSongwriter(Hello rmi, Scanner reader) throws RemoteException {
        System.out.print("Insert songwriters's name: ");
        boolean flagK = false;
        String nameA = "";
        while (!flagK) {
            nameA = reader.nextLine();
            if (!nameA.trim().equals("")) {
                flagK = true;
            } else {
                System.out.print("Insert songwriters's name: ");
            }
        }
        String re = rmi.showSongwriterMusics(nameA);
        String[] responseSplit = re.trim().split(";");
        switch(responseSplit[0]){
            case "type|musicDatabaseEmpty":
                System.out.println("No musics in the database.");
                break;
            case "type|songwriterNotFound":
                System.out.println("Songwriter not found.");
                break;
            case "type|showSongwriterMusicsComplete":
                String[] musicsPartss = responseSplit[1].split("\\|");
                String[] musicsNamess = musicsPartss[1].split(",");
                if (!musicsNamess[0].equals("No musics to show.")) {
                    String albunsNamesFinais = "";
                    int n = 1;
                    for (int i=0;i<musicsNamess.length;i++) {
                        albunsNamesFinais += n+". ";
                        albunsNamesFinais += musicsNamess[i];
                        albunsNamesFinais += "\n";
                        n++;
                    }
                    System.out.println("\nMusics:\n"+albunsNamesFinais);
                    return 1;
                }
                else{
                    System.out.println("This songwriter has no musics.");
                    return 0;
                }
            default:
                System.out.println("Something went wrong.");
        }
        return 0;
    }

    private static int searchMusicComposer(Hello rmi, Scanner reader) throws RemoteException {
        System.out.print("Insert composer's name: ");
        boolean flagK = false;
        String nameA = "";
        while (!flagK) {
            nameA = reader.nextLine();
            if (!nameA.trim().equals("")) {
                flagK = true;
            } else {
                System.out.print("Insert composer's name: ");
            }
        }
        String re = rmi.showComposerMusics(nameA);
        String[] responseSplit = re.trim().split(";");
        switch(responseSplit[0]){
            case "type|musicDatabaseEmpty":
                System.out.println("No musics in the database.");
                break;
            case "type|composerNotFound":
                System.out.println("Composer not found.");
                break;
            case "type|type|showComposerMusicsComplete":
                String[] musicsParts = responseSplit[1].split("\\|");
                String[] musicsNames = musicsParts[1].split(",");
                if (!musicsNames[0].equals("No musics to show.")) {
                    String albunsNamesFinais = "";
                    int n = 1;
                    for (int i=0;i<musicsNames.length;i++) {
                        albunsNamesFinais += n+". ";
                        albunsNamesFinais += musicsNames[i];
                        albunsNamesFinais += "\n";
                        n++;
                    }
                    System.out.println("\nMusics:\n"+albunsNamesFinais);
                    return 1;
                }
                else{
                    System.out.println("This composer has no musics.");
                    return 0;
                }
            default:
                System.out.println("Something went wrong.");
        }
        return 0;
    }

    private static void searchMusicName(Hello rmi, Scanner reader) throws RemoteException {
        boolean flagOK;
        flagOK = false;
        String name = "";
        System.out.println("Which music you wanna search? ");
        while(!flagOK){
            name = reader.nextLine();
            if(!name.trim().equals("")){
                flagOK=true;
            }
            else{
                System.out.println("Which music you wanna show? ");
            }
        }
        String response = rmi.showMusic(name);
        String[] responseSplit = response.trim().split(";");
        if(!(responseSplit.length>1)){
            System.out.println("Showing music failed.");
            return;
        }

        switch (responseSplit[0]) {
            case "type|notPartialSearchComplete":
                String[] nameParts = responseSplit[1].split("\\|");
                String[] artistParts = responseSplit[2].split("\\|");
                String[] composerParts = responseSplit[3].split("\\|");
                String[] songwriterParts = responseSplit[4].split("\\|");
                String[] albParts = responseSplit[5].split("\\|");
                String[] lenParts = responseSplit[6].split("\\|");
                System.out.println(
                        "Name: "+ nameParts[1] +
                        "\nArtist: " + artistParts[1]+
                        "\nComposer: "+composerParts[1]
                        +   "\nSongwriter: " + songwriterParts[1]
                        +   "\nAlbum: " + albParts[1]
                        +   "\nLength: "+ lenParts[1]
                );
                break;
            case "type|partialSearchComplete":
                String[] resultsParts = responseSplit[1].split("\\|");
                String[] results = resultsParts[1].split(",");
                System.out.println("Which one did you mean?\n"+printAlbuns(results));
                searchMusicName(rmi,reader);
                break;
            case "type|showMusicFail":
                System.out.println("Music not shown.");
                break;
        }
    }

    public static int searchAlbumByArtistName(Hello rmi, Scanner reader) throws RemoteException {
        System.out.print("Insert artist's name: ");
        boolean flagK = false;
        String nameA = "";
        while (!flagK) {
            nameA = reader.nextLine();
            if (!nameA.trim().equals("")) {
                flagK = true;
            } else {
                System.out.print("Insert artist's name: ");
            }
        }
        String re = rmi.showArtistAlbums(nameA);
        String[] responseSplit = re.trim().split(";");
        String[] albunsParts = responseSplit[1].split("\\|");
        String[] albunsNames = albunsParts[1].split(",");
        if (!albunsNames[0].equals("No albuns to show.")) {
            String albunsNamesFinais = "";
            int n = 1;
            for (int i=0;i<albunsNames.length;i++) {
                albunsNamesFinais += n+". ";
                albunsNamesFinais += albunsNames[i];
                albunsNamesFinais += "\n";
                n++;
            }
            System.out.println("\nAlbums:\n"+albunsNamesFinais);
            return 1;
        }
        else{
            System.out.println("This artist has no albums.");
            return 0;
        }
    }

    public static void searchArtist(Hello rmi, Scanner reader) throws RemoteException {
        boolean flagOK;
        flagOK = false;
        String name = "";
        System.out.println("Which artist you wanna search? ");
        while(!flagOK){
            name = reader.nextLine();
            if(!name.trim().equals("")){
                flagOK=true;
            }
            else{
                System.out.println("Which artist you wanna show? ");
            }
        }
        String response = rmi.showArtist(name);
        String[] responseSplit = response.trim().split(";");
        if(!(responseSplit.length>1)){
            System.out.println("Showing artist failed.");
            return;
        }


        switch (responseSplit[0]) {
            case "type|notPartialSearchComplete":
                String[] nameParts = responseSplit[1].split("\\|");
                String[] descParts = responseSplit[2].split("\\|");
                String[] funcParts = responseSplit[3].split("\\|");
                String[] funcs = funcParts[1].split(",");
                String[] albParts = responseSplit[4].split("\\|");
                String[] albs = albParts[1].split(",");
                System.out.println(
                    "Name: "+ nameParts[1] + " (" + printFuncs(funcs)+")"
                +   "\nDescription: " + descParts[1]
                +   "\nAlbums: \n" + printAlbuns(albs)
                        );
                break;
            case "type|partialSearchComplete":
                String[] resultsParts = responseSplit[1].split("\\|");
                String[] results = resultsParts[1].split(",");
                System.out.println("Which one did you mean?\n"+printAlbuns(results));
                searchArtist(rmi,reader);
                break;
            case "type|showArtistFail":
                System.out.println("Artist not Shown.");
                break;
        }
    }

    private static String printAlbuns(String[] albs) {
        if (!albs[0].equals("No albuns to show.")) {
            String albunsNamesFinais = "";
            int n = 1;
            for (int i=0;i<albs.length;i++) {
                albunsNamesFinais += n+". ";
                albunsNamesFinais += albs[i];
                albunsNamesFinais += "\n";
                n++;
            }
            return albunsNamesFinais;
        }
        else{
            return "This artist has no albums.";
        }
    }

    private static String printFuncs(String[] funcs) {
        String finalString = "";
        for(int i=0;i<funcs.length;i++){
            if(i!=(funcs.length-1)){
                finalString += funcs[i];
                finalString+=",";
            }
            else{
                finalString += funcs[i];
            }
        }

        return finalString;
    }

    public static void searchAlbumName(Hello rmi, Scanner reader) throws RemoteException {
        boolean flagOK;
        flagOK = false;
        String n = "";
        System.out.println("Which album you wanna show? ");
        while(!flagOK){
            n = reader.nextLine();
            if(!n.trim().equals("")){
                flagOK=true;
            }
            else{
                System.out.println("Which album you wanna show? ");
            }
        }
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
        String resp = rmi.showAlbum(n);
        String[] respSplit = resp.trim().split(";");

        switch(respSplit[0]){
            case "type|notPartialSearchAlbumComplete":
                String[] nomeParts = respSplit[1].split("\\|");
                String[] ArtistName = respSplit[2].split("\\|");
                String[] Description = respSplit[3].split("\\|");
                String[] Length = respSplit[4].split("\\|");
                String[] Genre = respSplit[5].split("\\|");
                String[] ScoreFinal = respSplit[6].split("\\|");
                String[] Critics = respSplit[7].split("\\|");
                String[] CriticByCritic = Critics[1].split("!");
                if(CriticByCritic.length != 0){
                    criticsList = createCriticsList(CriticByCritic);
                }
                String[] Musics = respSplit[8].split("\\|");
                String[] MusicByMusic = Musics[1].split(",");
                if(MusicByMusic.length != 0){
                    musicsList = createMusicsList(MusicByMusic);
                }
                String[] Publisher = respSplit[9].split("\\|");
                Album album = new Album();
                album = new Album(nomeParts[1],ArtistName[1],Description[1],Integer.parseInt(Length[1]),Genre[1],Double.parseDouble(ScoreFinal[1]),criticsList,musicsList,Publisher[1]);
                System.out.println(album);
                break;
            case "type|partialSearchAlbumComplete":
                String[] resultsParts = respSplit[1].split("\\|");
                String[] results = resultsParts[1].split(",");
                System.out.println("Which one did you mean?\n"+printAlbuns(results));
                searchAlbumName(rmi,reader);
                break;
            default:
                System.out.println("No matches found.");
        }
    }

    private static ArrayList<Music> createMusicsList(String[] musicByMusic) {
        ArrayList<Music> m = new ArrayList<>();

        for(int i=0;i<musicByMusic.length;i++){
            m.add(new Music(musicByMusic[i]));
        }
        return m;
    }

    private static ArrayList<Critic> createCriticsList(String[] criticByCritic) {
        ArrayList<Critic> c = new ArrayList<>();
        String[] Critics1;

        for(int i=0;i<criticByCritic.length;i++){
            Critics1 = criticByCritic[i].split(",");
            if(Critics1.length == 3){
                c.add(new Critic(Double.parseDouble(Critics1[2]),Critics1[1],Critics1[0]));

            }
        }

        return c;
    }

    ////////////// MENU DE EDITOR COM AS SUAS FUNÇÕES INICIAIS/////////////
    public static void editorMenu(Hello rmi,Scanner reader) throws RemoteException{
        if(!loggedUser.isEditor()){
            System.out.println("You don't have permission to do this.");
        }
        else{
            System.out.println("What do you want to do: Create, Edit?");
            String response = reader.nextLine();
            switch (response.trim()){
                case "/create":
                    createMenu(rmi,reader);
                    break;
                case "/edit":
                    editMenu(rmi,reader);
                    break;
                default:
                    System.out.println("Insert a valid answer.");
                    break;
            }
        }
    }

            ////////////// MENU DE EDITAR COM AS SUAS FUNÇÕES/////////////
    public static void editMenu(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to edit: Artist, Album, Concert, Publisher?");
        String response = reader.nextLine();
        switch(response.trim()){
            case "/artist":
                editArtist(rmi,reader);
                break;
            case "/publisher":
                editPublisher(rmi,reader);
                break;
            case "/concert":
                editConcert(rmi,reader);
                break;
            case "/album":
                editAlbum(rmi,reader);
                break;
            default:
                //Something;
        }
    }

    private static void editAlbum(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("What do you want to change: Name, Description, Genre");
        String reposta = reader.nextLine();
        switch (reposta){
            case "/name":
                changeAlbumName(rmi,reader);
                break;
            case "/description":
                changeAlbumDescription(rmi,reader);
                break;
            case "/genre":
                changeAlbumGenre(rmi,reader);
                break;
        }
    }

    private static void changeAlbumGenre(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Which album do you wanna change? ");
        boolean flagOK = false;
        String album = "";
        String genreAfter="";
        while(!flagOK) {
            album = reader.nextLine();
            if (!album.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which album do you wanna change? ");
            }
        }
        System.out.println("To what genre you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            genreAfter = reader.nextLine();
            if (!genreAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what description you wanna change it? ");
            }
        }
        String response = rmi.editAlbumGenre(album,genreAfter);
        switch(response.trim()){
            case "type|genreChanged":
                System.out.println("Genre changed.");
                break;
            case "type|albumDatabaseEmpty":
                System.out.println("No albums on the database.");
                break;
            case "type|albumNotFound":
                System.out.println("Album not found.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    private static void changeAlbumDescription(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Which album do you wanna change? ");
        boolean flagOK = false;
        String album = "";
        String descriptionAfter="";
        while(!flagOK) {
            album = reader.nextLine();
            if (!album.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which album do you wanna change? ");
            }
        }
        System.out.println("To what description you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            descriptionAfter = reader.nextLine();
            if (!descriptionAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what description you wanna change it? ");
            }
        }
        String response = rmi.editAlbumDescription(album,descriptionAfter);
        switch(response.trim()){
            case "type|descriptionChanged":
                System.out.println("Description changed.");
                break;
            case "type|albumDatabaseEmpty":
                System.out.println("No albums on the database.");
                break;
            case "type|albumNotFound":
                System.out.println("Album not found.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    private static void changeAlbumName(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Which album do you wanna change? ");
        boolean flagOK = false;
        String album = "";
        String nameAfter="";
        while(!flagOK) {
            album = reader.nextLine();
            if (!album.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which album do you wanna change? ");
            }
        }
        System.out.println("To what name you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            nameAfter = reader.nextLine();
            if (!nameAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what name you wanna change it? ");
            }
        }
        String response = rmi.editAlbumName(album,nameAfter);
        switch(response.trim()){
            case "type|nameChanged":
                System.out.println("Name changed.");
                break;
            case "type|albumDatabaseEmpty":
                System.out.println("No albums on the database.");
                break;
            case "type|albumNotFound":
                System.out.println("Album not found.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    private static void editPlaylist(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("What do you want to do: Change name, Add Music, Remove Music");
        String reposta = reader.nextLine();
        switch (reposta){
            case "/name":
                changePlaylistName(rmi,reader);
                break;
            case "/addmusic":
                playlistAdd(rmi,reader);
                break;
            case "/removemusic":
                playlistMusicRemove(rmi,reader);
                break;
        }
    }

    private static void playlistMusicRemove(Hello rmi, Scanner reader) throws RemoteException {
        ArrayList<Music> musicsList = new ArrayList<>();
        String music ="";
        String playlist="";
        boolean flagOK = false;
        System.out.println("From which playlist you wanna remove a music?");
        while(!flagOK){
            playlist = reader.nextLine();
            if(!playlist.trim().equals("")){
                flagOK=true;
            }
            else{
                System.out.println("From which playlist you wanna remove a music?");
            }
        }


        flagOK = false;
        System.out.println("Which music do you wanna remove?");
        while(!flagOK){
            music = reader.nextLine();
            if(!music.trim().equals("")){
                flagOK=true;
            }
            else{
                System.out.println("Which music do you wanna remove?");
            }
        }
        String resposta = rmi.removeMusicPlaylist(music,playlist,loggedUser.getId());
        switch (resposta){
            case "type|removeMusicCompleted":
                System.out.println("Music removed.");
                break;
            case "type|invalidMusic":
                System.out.println("Insert a valid music.");
                break;
            case "type|invalidPlaylist":
                System.out.println("Insert a valid playlist.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    private static void playlistAdd(Hello rmi, Scanner reader) throws RemoteException {
        ArrayList<Music> musicsList = new ArrayList<>();
        String music ="";
        String playlist="";
        boolean flagOK = false;
        System.out.println("Which music you wanna add?");
        String response = rmi.getMusicList(loggedUser.getId());
        String[] rSplit = response.split(";");
        String[] musicParts = rSplit[1].split("\\|");
        musicsList = createMusicsList(musicParts[1].split(","));
        int i = 1;
        for (Music m : musicsList) {
            System.out.println(i+". "+m.toString());
            i++;
        }

        while(!flagOK){
            music = reader.nextLine();
            if(!music.trim().equals("")){
                flagOK=true;
            }
            else{
                System.out.println("Which music you wanna add?");
            }
        }

        flagOK = false;
        System.out.println("To what playlist you wanna add?");
        while(!flagOK){
            playlist = reader.nextLine();
            if(!playlist.trim().equals("")){
                flagOK=true;
            }
            else{
                System.out.println("To what playlist you wanna add?");
            }
        }
        String resposta = rmi.addMusicPlaylist(music,playlist,loggedUser.getId());
        switch (resposta){
            case "type|musicAddCompleted":
                System.out.println("Music added.");
                break;
            case "type|invalidMusic":
                System.out.println("Insert a valid music.");
                break;
            case "type|invalidPlaylist":
                System.out.println("Insert a valid playlist.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    private static void changePlaylistName(Hello rmi, Scanner reader) throws RemoteException {
            System.out.println("Which playlist do you wanna change? ");
            boolean flagOK = false;
            String playlist = "";
            String nameAfter="";
            while(!flagOK) {
                playlist = reader.nextLine();
                if (!playlist.trim().equals("")){
                    flagOK = true;
                }
                else{
                    System.out.println("Which playlist do you wanna change? ");
                }
            }
            System.out.println("To what name you wanna change it? ");
            flagOK = false;
            while(!flagOK) {
                nameAfter = reader.nextLine();
                if (!nameAfter.trim().equals("")){
                    flagOK = true;
                }
                else{
                    System.out.println("To what name you wanna change it? ");
                }
            }
            String response = rmi.editPlaylistName(playlist,nameAfter);
            switch(response.trim()){
                case "type|nameChanged":
                    System.out.println("Name changed.");
                    break;
                case "type|playlistDatabaseEmpty":
                    System.out.println("No playlists on the database.");
                    break;
                case "type|playlistNotFound":
                    System.out.println("Playlist not found.");
                    break;
                default:
                    System.out.println("Something went wrong.");
                    break;
            }
    }

    private static void editConcert(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("What do you wanna change: Name, Location, Description");
        String text = reader.nextLine();
        switch(text.trim()){
            case "/name":
                editConcertName(rmi,reader);
                break;
            case "/location":
                editConcertLocation(rmi,reader);
                break;
            case "/description":
                editConcertDescription(rmi,reader);
                break;
            default:
                //something();
        }
    }

    private static void editConcertName(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Which concert do you wanna change? ");
        boolean flagOK = false;
        String concert = "";
        String nameAfter="";
        while(!flagOK) {
            concert = reader.nextLine();
            if (!concert.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which concert do you wanna change? ");
            }
        }
        System.out.println("To what name you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            nameAfter = reader.nextLine();
            if (!nameAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what name you wanna change it? ");
            }
        }
        String response = rmi.editConcertName(concert,nameAfter);
        switch(response.trim()){
            case "type|nameChanged":
                System.out.println("Name changed.");
                break;
            case "type|concertDatabaseEmpty":
                System.out.println("No concerts on the database.");
                break;
            case "type|concertNotFound":
                System.out.println("Concert not found.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    private static void editConcertLocation(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Which concert do you wanna change? ");
        boolean flagOK = false;
        String concert = "";
        String locationAfter="";
        while(!flagOK) {
            concert = reader.nextLine();
            if (!concert.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which concert do you wanna change? ");
            }
        }
        System.out.println("To what location you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            locationAfter = reader.nextLine();
            if (!locationAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what location you wanna change it? ");
            }
        }
        String response = rmi.editConcertLocation(concert,locationAfter);
        switch(response.trim()){
            case "type|locationChanged":
                System.out.println("Location changed.");
                break;
            case "type|concertDatabaseEmpty":
                System.out.println("No concerts on the database.");
                break;
            case "type|concertNotFound":
                System.out.println("Concert not found.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    private static void editConcertDescription(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Which concert do you wanna change? ");
        boolean flagOK = false;
        String concert = "";
        String descriptionAfter="";
        while(!flagOK) {
            concert = reader.nextLine();
            if (!concert.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which concert do you wanna change? ");
            }
        }
        System.out.println("To what description you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            descriptionAfter = reader.nextLine();
            if (!descriptionAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what description you wanna change it? ");
            }
        }
        String response = rmi.editConcertDescription(concert,descriptionAfter);
        switch(response.trim()){
            case "type|descriptionChanged":
                System.out.println("Description changed.");
                break;
            case "type|concertDatabaseEmpty":
                System.out.println("No concerts on the database.");
                break;
            case "type|concertNotFound":
                System.out.println("Concert not found.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    private static void editPublisher(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Which publisher do you wanna change? ");
        boolean flagOK = false;
        String publisher = "";
        String nameAfter="";
        while(!flagOK) {
            publisher = reader.nextLine();
            if (!publisher.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which publisher do you wanna change? ");
            }
        }
        System.out.println("To what name you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            nameAfter = reader.nextLine();
            if (!nameAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what name you wanna change it? ");
            }
        }
        String response = rmi.editPublisherName(publisher,nameAfter);
        switch(response.trim()){
            case "type|nameChanged":
                System.out.println("Name changed.");
                break;
            case "type|publisherDatabaseEmpty":
                System.out.println("No publishers on the database.");
                break;
            case "type|publisherNotFound":
                System.out.println("Publisher not found.");
                break;
            case "type|nameAlreadyTaken":
                System.out.println("Name already taken by another publisher.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    public static void editArtist(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("What do you wanna change: Name, Type, Description");
        String text = reader.nextLine();
        switch(text.trim()){
            case "/name":
                editName(rmi,reader);
                break;
            case "/type":
                editType(rmi,reader);
                break;
            case "/description":
                editDescription(rmi,reader);
                break;
            default:
                //something();
        }
    }

    public static void editName(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("Which artist you wanna change? ");
        boolean flagOK = false;
        String artist = "";
        String nameAfter="";
        while(!flagOK) {
            artist = reader.nextLine();
            if (!artist.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which artist you wanna change? ");
            }
        }
        System.out.println("To what name you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            nameAfter = reader.nextLine();
            if (!nameAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what name you wanna change it? ");
            }
        }
        String response = rmi.editArtistName(artist,nameAfter);
        switch(response.trim()){
            case "type|nameChanged":
                System.out.println("Name changed.");
                break;
            case "type|artistDatabaseEmpty":
                System.out.println("No artists on the database.");
                break;
            case "type|artistNotFound":
                System.out.println("Artist not found.");
                break;
            case "type|nameAlreadyTaken":
                System.out.println("Name already taken by another artist.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    public static void editType(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("Which artist you wanna change? ");
        boolean flagOK = false;
        String artist = "";
        String typeAfter="";
        while(!flagOK) {
            artist = reader.nextLine();
            if (!artist.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which artist you wanna change? ");
            }
        }
        System.out.println("To what musician type (Songwriter, Composer) you wanna change it? ");//Options -> songwriter, composer and both
        flagOK = false;
        while(!flagOK) {
            typeAfter = reader.nextLine();
            if (typeAfter.trim().equals("songwriter") || typeAfter.trim().equals("composer") || typeAfter.trim().equals("both")){
                flagOK = true;
            }
            else{
                System.out.println("To what musician type (Songwriter, Composer) you wanna change it? ");//Options -> songwriter, composer and both
            }
        }
        String response = rmi.editArtistType(artist,typeAfter);
        switch(response.trim()){
            case "type|changesApplied":
                System.out.println("Type changed.");
                break;
            case "type|artistDatabaseEmpty":
                System.out.println("No artists on the database.");
                break;
            case "type|artistNotFound":
                System.out.println("Artist not found.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

    public static void editDescription(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("Which artist you wanna change? ");
        boolean flagOK = false;
        String artist = "";
        String description="";
        while(!flagOK) {
            artist = reader.nextLine();
            if (!artist.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which artist you wanna change? ");
            }
        }
        System.out.println("To what description you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            description = reader.nextLine();
            if (!description.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what description you wanna change it? ");
            }
        }

        String response = rmi.editArtistDescription(artist,description);
        switch(response.trim()){
            case "type|descriptionChanged":
                System.out.println("Description changed.");
                break;
            case "type|artistDatabaseEmpty":
                System.out.println("No artists on the database.");
                break;
            case "type|artistNotFound":
                System.out.println("Artist not found.");
                break;
            default:
                System.out.println("Something went wrong.");
                break;
        }
    }

            ////////////// MENU DE CRIAR COM AS SUAS FUNÇÕES/////////////
    public static void createMenu(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to create: Artist, Album, Concert, Publisher?");
        String response = reader.nextLine();
        switch(response.trim()){
            case "/artist":
                createArtist(rmi,reader);
                break;
            case "/album":
                createAlbum(rmi,reader);
                break;
            case "/concert":
                createConcert(rmi,reader);
                break;
            case "/publisher":
                createPublisher(rmi,reader);
                break;
            default:
                //Something;
        }
    }

    public static void createArtist(Hello rmi,Scanner reader) throws RemoteException{
        boolean isComposer = false, isSongwriter = false, isBand = false;
        System.out.println("Is the new artist a Musician, Songwriter, a Composer or a Group? (Yes/no camps)");
        String response = reader.nextLine();
        String[] responseSplit = response.split("-");
        if(responseSplit[0].equals("yes")){
            isSongwriter = true;
        }
        if(responseSplit[1].equals("yes")){
            isComposer = true;
        }
        if(responseSplit[2].equals("yes")){
            isComposer = false;
            isSongwriter = false;
        }
        createMusician(rmi,reader,isSongwriter,isComposer, isBand);
    }

    public static void createSongwriter(Hello rmi,Scanner reader) throws RemoteException{
        boolean flagOK = false;
        System.out.println("Insert your data('name-description')");
        String text = "";
        String[]data = new String[2];
        while(!flagOK){
            text = reader.nextLine();
            data = text.trim().split("-");
            if(data.length == 2){
                if(data[0].trim().equals("") || data[1].trim().equals("")){
                    System.out.println("Insert your data('name-description')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your data('name-description')");
            }
        }
        String response = rmi.createSongwriter(data[0], data[1]);
        switch (response.trim()){
            case "type|songWriterExists":
                System.out.println("Songwriter already exists.");
                break;
            case "type|createSongwriterComplete":
                System.out.println("SUCCESS: Songwriter created successfully.");
                break;
            default:
                //something;
        }
    }

    public static void createMusician(Hello rmi,Scanner reader, boolean isSongwriter, boolean isComposer, boolean isBand) throws RemoteException{
        boolean flagOK = false;
        System.out.println("Insert your data('name-description')");
        String text = "";
        String[]data = new String[2];
        while(!flagOK) {
            text = reader.nextLine();
            data = text.trim().split("-");
            if(data.length == 2){
                if(data[0].trim().equals("") || data[1].trim().equals("")){
                    System.out.println("Insert your data('name-description')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your data('name-description')");
            }
        }
        String response = rmi.createMusician(data[0], data[1], isSongwriter, isComposer, isBand);
        switch (response.trim()){
            case "type|musicianExists":
                System.out.println("Artist already exists.");
                break;
            case "type|createMusicianComplete":
                System.out.println("SUCCESS: Artist created successfully.");
                break;
            default:
                //something;
        }
    }

    public static void createComposer(Hello rmi,Scanner reader) throws RemoteException{
        boolean flagOK = false;
        System.out.println("Insert your data('name-description')");
        String text = "";
        String[]data = new String[2];
        while(!flagOK) {
            text = reader.nextLine();
            data = text.trim().split("-");
            if(data.length == 2){
                if(data[0].trim().equals("") || data[1].trim().equals("")){
                    System.out.println("Insert your data('name-description')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your data('name-description')");
            }
        }
        String response = rmi.createComposer(data[0], data[1]);
        switch (response.trim()){
            case "type|composerExists":
                System.out.println("Composer already exists.");
                break;
            case "type|createComposerComplete":
                System.out.println("SUCCESS: Composer created successfully.");
                break;
            default:
                //something;
        }
    }

    public static void createBand(Hello rmi,Scanner reader) throws RemoteException{
        boolean flagOK = false;
        System.out.println("Insert your data('name-description')");
        String text = "";
        String[]data = new String[2];
        while(!flagOK) {
            text = reader.nextLine();
            data = text.trim().split("-");
            if(data.length == 2){
                if(data[0].trim().equals("") || data[1].trim().equals("")){
                    System.out.println("Insert your data('name-description')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your data('name-description')");
            }
        }
        String response = rmi.createBand(data[0], data[1]);
        switch (response.trim()){
            case "type|bandExists":
                System.out.println("Band already exists.");
                break;
            case "type|createBandComplete":
                System.out.println("SUCCESS: Band created successfully.");
                break;
            default:
                //something;
        }
    }

    public static void createAlbum(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("Insert your data('name-genre-description-artist-publisher')");
        boolean flagOK = false;
        String text = "";
        String[]data = new String[5];
        while(!flagOK) {
            text = reader.nextLine();
            data = text.split("-");
            if(data.length == 5){
                if(data[0].trim().equals("") || data[1].trim().equals("") || data[2].trim().equals("") || data[3].trim().equals("") || data[4].trim().equals("")){
                    System.out.println("Insert your data('name-genre-description-artist-publisher')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your data('name-artist-description-artist-publisher')");
            }
        }
        String response = rmi.createAlbum(data[0], data[1], data[2], data[3], data[4]);
        switch (response.trim()){
            case "type|albumExists":
                System.out.println("Album already exists.");
                break;
            case "type|userNotFound":
                System.out.println("User not found -> Album not created.");
                break;
            case "type|createAlbumComplete":
                System.out.println("SUCCESS: Album created successfully.");
                break;
            default:
                //something;
        }
    }

    public static void createConcert(Hello rmi,Scanner reader) throws RemoteException{
        boolean flagOK = false;
        System.out.println("Insert your data('location-name-band/musician-description')");
        String text = "";
        String[]data = new String[4];
        while(!flagOK) {
            text = reader.nextLine();
            data = text.trim().split("-");
            if(data.length == 4){
                if(data[0].trim().equals("") || data[1].trim().equals("") || data[2].trim().equals("") || data[3].trim().equals("")){
                    System.out.println("Insert your data('location-name-band/musician-description')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your data('location-name-band/musician-description')");
            }
        }
        String response1 = rmi.createConcert(data[0],data[1],data[3]);
        switch (response1.trim()){
            case "type|createConcertFailed":
                System.out.println("Concert creation failed.");
                break;
            case "type|createConcertComplete":
                System.out.println("SUCCESS: Concert created successfully.");
                break;
            default:
                //something;
        }
        String response2 = rmi.concertAssociation(data[1],data[2]);
        switch (response2.trim()){
            case "type|concertExists":
                System.out.println("Concert already exists.");
                break;
            case "type|createConcertComplete":
                System.out.println("SUCCESS: Successful association.");
                break;
            default:
                //something;
        }
    }

    private static void createPublisher(Hello rmi, Scanner reader) throws RemoteException {
        boolean flagOK = false;
        System.out.println("Insert your data('Name')");
        String text = "";
        while(!flagOK) {
            text = reader.nextLine();
            if(text.trim().equals("")){
                System.out.println("Insert your data('Name')");
            }
            else{
                flagOK = true;
            }
        }
        String response = rmi.createPublisher(text);
        switch (response.trim()){
            case "type|publisherExists":
                System.out.println("Publisher already exists.");
                break;
            case "type|createPublisherComplete":
                System.out.println("SUCCESS: Publisher created successfully.");
                break;
            default:
                //something;
        }
    }

    public static void createPlaylist(Hello rmi,Scanner reader) throws RemoteException{
        boolean flagOK = false;
        System.out.println("Insert your data('name')");
        String text = "";
        while(!flagOK) {
            text = reader.nextLine();
            if(text.trim().equals("")){
                System.out.println("Insert your data('name')");
            }
            else {
                flagOK = true;
            }
        }
        String response1 = rmi.createPlaylist(text,loggedUser.getId()); //User-Playlist association
        switch (response1.trim()){
            case "type|createPlaylistFailed":
                System.out.println("Playlist not created.");
                break;
            case "type|createPlaylistComplete":
                System.out.println("SUCCESS: Playlist created successfully.");
                break;
            default:
                //something;
        }
    }


            ////////////// DAR PREMISSÕES /////////////
    public static void makeEditor(Hello rmi, Scanner reader) throws RemoteException{
        System.out.print("Insert user's name: ");
        boolean flagOK = false;
        String name = "";
        while(!flagOK){
            name = reader.nextLine();
            if(!name.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.print("Insert user's name: ");
            }
        }

        String response = rmi.checkEditorMaking(name, rmi);
        switch (response.trim()){
            case "type|makingEditorComplete":
                System.out.println(name +" is now an editor.");
                break;
            case "type|makingEditorFail":
                System.out.println("Making "+name+" an Editor didn't work.");
                break;
            default:
                System.out.println("Something went wrong.");
        }
    }

            ////////////// FAZER UMA CRITICA /////////////
    public static void makeCritic(Hello rmi, Scanner reader) throws  RemoteException{
        System.out.println("Which album you wanna make a critic to? ");
        String album = "";
        String criticText = "";
        double score = 0.0;
        boolean flagOK = false;
        while(!flagOK){
            album = reader.nextLine();
            if(!album.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which album you wanna make a critic to? ");
            }
        }
        flagOK = false;
        while(!flagOK){
            System.out.print("Type your critic: ");
            String text = reader.nextLine();
            if(!text.trim().equals("")){
                flagOK = true;
                criticText = text;
            }
            else{
                System.out.println("Insert a valid critic: ");
            }
        }
        while(true){
            System.out.println("Insert a valid decimal number for score: ");
            try {
                String pont = reader.next();
                score = Double.parseDouble(pont.trim());
                break;
            } catch (NumberFormatException ignore) {
                System.out.println("Invalid input!");
            }
        }

        String response = rmi.makeCritic(score,criticText,album,loggedUser);
        switch (response.trim()){
            case "type|albumNotFound":
                System.out.println("Album not found.");
                break;
            case "type|invalidScore":
                System.out.println("Invalid score.");
                break;
            case "type|albumDatabaseEmpty":
                System.out.println("Album database empty.");
                break;
            case "type|criticComplete":
                System.out.println("SUCCESS: Critic made.");
                break;
            case "type|criticFail":
                System.out.println("ERROR: Critic not made.");
                break;
        }

    }

            ////////////// UPLOAD E DOWNLOAD DE MUSICAS /////////////
    private static String[] sendMusic(Hello rmi,Scanner reader) throws IOException, NotBoundException {
        String[] musicInfo = new String[7];
        ServerSocket socket = new ServerSocket(5000);
        Socket socketAcept = null;
        File file;
        FileInputStream fInStream = null;
        OutputStream outStream = null;
        try {
            int auxi = 0;
            System.out.println("check1");
            String[] aux = socket.getLocalSocketAddress().toString().split("/");
            rmi.startSocket(aux[0]);
            socketAcept = socket.accept();
            System.out.println("Write down the directory of your music: (example:'C:\\music\\example.wav').");
            Scanner direc = new Scanner(System.in);
            String auxx;
            while (true) {
                auxx = direc.nextLine();
                break;
            }
            musicInfo[auxi] = auxx;
            auxi++;
            file = new File(auxx);
            String[] aux1 = auxx.split("\\\\");
            //System.out.println("can it split?");
            //System.out.println(aux1[aux1.length - 1]);
            FilePermission permission = new FilePermission(auxx, "read");
            //System.out.println(permission);
            fInStream = new FileInputStream(file);
            //System.out.println("too much bytes?");
            outStream = socketAcept.getOutputStream();
            byte b[];
            //System.out.println("no");
            int current = 0;
            long len = file.length();
            while (current != len) {
                int size = 1024;

                if (len - current >= size)
                    current += size;
                else {
                    size = (int) (len - current);
                    current = (int) len;
                }
                b = new byte[size];
                //System.out.println("antes de fstream");
                fInStream.read(b, 0, size);
                //System.out.println("antes de outstream");
                outStream.write(b);
                //System.out.println("Sending file ... " + (current * 100) / len + "% complete!");

            }
            System.out.println("File uploaded.");
            fInStream.close();
            outStream.flush();
            outStream.close();
            socketAcept.close();
            socket.close();
            musicInfo[auxi] = aux1[aux1.length - 1];
            auxi++;
            boolean flagOK = false;
            String composer = "";
            while (!flagOK) {
                System.out.print("Insert composer: ");
                composer = reader.nextLine();
                if (!composer.trim().equals("")) {
                    flagOK = true;
                }
            }
            musicInfo[auxi] = composer;
            auxi++;
            flagOK = false;
            String artist = "";
            while (!flagOK) {
                System.out.print("Insert artist: ");
                artist = reader.nextLine();
                if (!artist.trim().equals("")) {
                    flagOK = true;
                }
            }
            musicInfo[auxi] = artist;
            auxi++;

            flagOK = false;
            String songwriter = "";
            while (!flagOK) {
                System.out.print("Insert songwriter: ");
                songwriter = reader.nextLine();
                if (!songwriter.trim().equals("")) {
                    flagOK = true;
                }
            }
            musicInfo[auxi] = songwriter;
            auxi++;

            flagOK = false;
            String duration = "";
            while (!flagOK) {
                System.out.print("Insert duration(seconds): ");
                duration = reader.nextLine();
                if (!duration.trim().equals("")) {
                    flagOK = true;
                }
            }
            musicInfo[auxi] = duration;
            auxi++;

            flagOK = false;
            String album = "";
            while (!flagOK) {
                System.out.print("Insert album: ");
                album = reader.nextLine();
                if (!album.trim().equals("")) {
                    flagOK = true;
                }
            }
            musicInfo[auxi] = album;
            auxi++;
        }catch (RemoteException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            socket.close();
            System.out.println("Insert valid file!");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return musicInfo;
    }

    private static String downloadMusic(Hello rmi, Scanner reader) throws IOException {
        ArrayList<Music> musicsList;
        String response = rmi.getMusicList(loggedUser.getId());
        String[] rSplit = response.split(";");
        String[] musicParts = rSplit[1].split("\\|");
        musicsList = createMusicsList(musicParts[1].split(","));

        if(musicParts[1].equals("No musics to show.")){
           return "You can't download any musics.";
        }

        System.out.println("Choose one song:");
        int i = 1;
        for (Music m : musicsList) {
            System.out.println(i+". "+m.toString());
            i++;
        }

        boolean flagEditor = false;
        String escolha = "";
        while(!flagEditor){
            escolha = reader.nextLine();
            for(Music music : musicsList){
                if(music.getTitle().equals(escolha)){
                    flagEditor = true;
                }
            }
            if(!flagEditor){
                System.out.println("Choose one valid song: ");
            }
        }


        boolean boo = new File("./musicasServer/" + loggedUser.getUsername()).mkdirs();
        String musicaEscolhida = "./musicasServer/" + escolha;

        //fazer o socket ligar primeiro no multi mas sem pedir pra aceitar
        rmi.startServerSocket(); //-> print "ServerSocket Inicialiazada"
        Socket socket = new Socket("0.0.0.0", 5041);
        String print = rmi.downloadMusicRMI(musicaEscolhida); //-> print "tudo ok no download"
        byte[] b = new byte [1024];
        InputStream is = socket.getInputStream();
        FileOutputStream fOutStream = new FileOutputStream("./musicasServer/"+loggedUser.getUsername()+"/"+escolha);
        BufferedOutputStream bOutStream = new BufferedOutputStream(fOutStream);

        int aux= 0;
        int cont=0;
        while ((aux = is.read(b))!=-1){
            bOutStream.write(b, 0, aux);
            if(is.available()==0){
                break;
            }
        }
        is.close();
        bOutStream.flush();
        bOutStream.close();
        fOutStream.close();
        socket.close();

        if(print.equals("tudo okay no download")){
            return "Download complete.";
        }
        else{
            return "Something went wrong.";
        }
    }

           ////////////// FAILOVER /////////////
    private static Hello changeRMI() throws RemoteException, NotBoundException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();//MUDAR ISTO PARA UM FICHEIRO DE OBJETOS PARA NAO PERDER
        } //GUARDAR AS CENAS
        Hello rmi;
        Registry registry = LocateRegistry.getRegistry(7000);
        rmi =(Hello) registry.lookup("Hello");
        System.out.println("Por favor repita o ultimo input");
        return rmi;
    }

            ////////////// CALLBACKS /////////////
    public void msg(String aux){
        System.out.println(aux);
    }
}

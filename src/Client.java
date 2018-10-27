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

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Scanner;
import java.rmi.registry.Registry;
import java.net.*;

public class Client implements ClientHello{

    private static User loggedUser = new User();
    private static ArrayList<User> users = new ArrayList<>();
    private Client() {}

    public static void main(String[] args) throws IOException, NotBoundException {
        String text = "";
        Hello rmi = null;
        ClientHello rmi2 = null;
        //String host = (args.length < 1) ? null : args[0];
        Scanner reader = new Scanner(System.in);
        try {
            Registry registry = LocateRegistry.getRegistry("localhost",7000);
            rmi =(Hello) registry.lookup("Hello");
            //rmi2 = (ClientHello) registry.lookup("ClientHello");
            Hello stub = (Hello) registry.lookup("Hello");
            String response = stub.sayHello();
            System.out.println("response: " + response);
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
                    case "/user":
                        System.out.println(loggedUser.getUsername());
                        break;
                    default:
                        System.out.println("Este comando não faz nada. Para sair escreva 'quit'");
                }
            }
            catch(RemoteException e){
                rmi = changeRMI();
                //por aqui pra ler as opçoes
            }
        }
        reader.close();
        System.out.println("Finished");
    }

    private static Hello changeRMI() throws RemoteException, NotBoundException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Hello rmi;
        Registry registry = LocateRegistry.getRegistry(7000);
        rmi =(Hello) registry.lookup("Hello");
        System.out.println("Por favor repita o ultimo input");
        return rmi;
    }

    private static String downloadMusic(Hello rmi, Scanner reader) throws IOException {
        ArrayList<String> directorias = new ArrayList<>(); // é suposto ir buscar ao multicastServer;
        /*directorias.add("C:\\Users\\Duarte\\Desktop\\SD\\PROJETO\\META 1\\SD---Project\\musicasServer\\a.mp3");
        directorias.add("C:\\Users\\Duarte\\Desktop\\SD\\PROJETO\\META 1\\SD---Project\\musicasServer\\b.mp3");
        directorias.add("C:\\Users\\Duarte\\Desktop\\SD\\PROJETO\\META 1\\SD---Project\\musicasServer\\c.mp3");*/

        System.out.println("Choose one song:");
        if(loggedUser.printDownloadableMusics().equals("No musics to show.")){
                return "Can't download any musics.";
        }
        else{
            System.out.println(loggedUser.printDownloadableMusics());
        }

        String escolha = reader.nextLine();

        boolean boo = new File("./musicasServer/" + loggedUser.getUsername()).mkdirs();
        String musicaEscolhida = "./musicasServer/" + escolha + ".mp3";

        //fazer o socket ligar primeiro no multi mas sem pedir pra aceitar
        System.out.println(rmi.startServerSocket());
        Socket socket = new Socket("0.0.0.0", 5041);
        String print = rmi.downloadMusicRMI(musicaEscolhida);
        byte[] b = new byte [1024];
        InputStream is = socket.getInputStream();
        FileOutputStream fOutStream = new FileOutputStream("./musicasServer/"+loggedUser.getUsername()+"/"+escolha+".mp3");
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

        System.out.println("ficheiro 100% completo");

        return print;
    }

    private static String[] sendMusic(Hello rmi,Scanner reader) throws IOException {
        String[] musicInfo = new String[7];
        int auxi = 0;
        ServerSocket socket = new ServerSocket(5000);
        System.out.println("check1");
        String[] aux = socket.getLocalSocketAddress().toString().split("/");
        rmi.startSocket(aux[0]);
        Socket socketAcept = socket.accept();
        System.out.println("Write down the directory of your music: (example:'C:\\music\\example.wav').");
        Scanner direc = new Scanner(System.in);
        String auxx = direc.nextLine();
        musicInfo[auxi] = auxx;
        auxi++;
        File file = new File(auxx);
        String[] aux1 = auxx.split("\\\\");
        System.out.println("can it split?");
        System.out.println(aux1[aux1.length-1]);
        FilePermission permission = new FilePermission(auxx, "read");
        FileInputStream fInStream= new FileInputStream(file);
        System.out.println("too much bytes?");
        OutputStream outStream = socketAcept.getOutputStream();
        byte b[];
        System.out.println("no");
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
            System.out.println("antes de fstream");
            fInStream.read(b, 0, size);
            System.out.println("antes de outstream");
            outStream.write(b);
            System.out.println("Sending file ... "+(current*100)/len+"% complete!");

        }
        System.out.println("uff");
        fInStream.close();
        outStream.flush();
        outStream.close();
        socketAcept.close();
        socket.close();
        musicInfo[auxi] = aux1[aux1.length-1];
        auxi++;
        boolean flagOK = false;
        String composer = "";
        while(!flagOK){
            System.out.print("Insert composer: ");
            composer = reader.nextLine();
            if(!composer.trim().equals("")){
                flagOK=true;
            }
        }
        musicInfo[auxi] = composer;
        auxi++;
        flagOK=false;
        String artist = "";
        while(!flagOK){
            System.out.print("Insert artist: ");
            artist = reader.nextLine();
            if(!artist.trim().equals("")){
                flagOK=true;
            }
        }
        musicInfo[auxi] = artist;
        auxi++;

        flagOK=false;
        String duration = "";
        while(!flagOK){
            System.out.print("Insert duration(seconds): ");
            duration = reader.nextLine();
            if(!duration.trim().equals("")){
                flagOK=true;
            }
        }
        musicInfo[auxi] = duration;
        auxi++;

        flagOK=false;
        String album = "";
        while(!flagOK){
            System.out.print("Insert album: ");
            album = reader.nextLine();
            if(!album.trim().equals("")){
                flagOK=true;
            }
        }
        musicInfo[auxi] = album;
        auxi++;

        flagOK=false;
        String genre = "";
        while(!flagOK){
            System.out.print("Insert genre: ");
            genre = reader.nextLine();
            if(!genre.trim().equals("")){
                flagOK=true;
            }
        }
        musicInfo[auxi] = genre;
        return musicInfo;
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
                if (txtSplit.length == 5) {
                    String[] username = txtSplit[1].split("\\|");
                    String[] password = txtSplit[2].split("\\|");
                    String[] editor = txtSplit[3].split("\\|");
                    String[] online = txtSplit[4].split("\\|");
                    loggedUser = new User(username[1], password[1], Boolean.parseBoolean(editor[1]), Boolean.parseBoolean(online[1]));
                }
                System.out.println("Welcome!");
                menuPrincipal(rmi,reader);
                break;
            case "type|loginFail":
                System.out.println("Login failed.");
                break;
            default:
                System.out.println("Something went wrong.");
        }
    }

    public static void logout(Hello rmi, Scanner reader) throws RemoteException{
        String txt = rmi.checkLogout(loggedUser);
        switch(txt.trim()){
            case "type|logoutComplete":
                System.out.println("Logged out successfully.");
                loggedUser = new User();
                break;
            case "type|logoutFail":
                System.out.println("Logout failed.");
                break;
            default:
                System.out.println("Something went wrong.");
        }
    }

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
                            "Upload\n" +
                            "Download\n\n" +
                            "Choose an option: ");
                }
                else{
                    System.out.println("MENU PRINCIPAL:\n" +
                        "Search\n" +
                        "Upload\n" +
                        "Make Critic\n"+
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
                    case "/edit":
                        if(loggedUser.isEditor()){
                            editorMenu(rmi, reader);
                        }
                        else{
                            System.out.println("You don't have permission to do that.");
                        }
                        break;
                    case "/logout":
                        logout(rmi,reader);
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
                        String[] musicInfo = sendMusic(rmi,reader);
                        String response = rmi.sendMusicRMI(musicInfo,loggedUser.getUsername());
                        String[] responseSpli = response.split(";");
                        switch (responseSpli[0]){
                            case "type|userNotFound":
                                System.out.println("ERROR: User Not Found.");
                                break;
                            case "type|artistNotFound":
                                System.out.println("ERROR: Artist Not Found.");
                                break;
                            case"type|sendMusicComplete":
                                if(responseSpli.length>1){
                                    String[] s = responseSpli[1].split("\\|");
                                    loggedUser.addDownloadableMusic(s[1]);
                                }
                                break;
                        }
                        break;
                    default:
                        System.out.println("Este comando não faz nada. Para sair escreva '/logout'");
                }
            }
            catch(RemoteException e){
                rmi = changeRMI();
                //e.printStackTrace();
            }
        }
    }

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

        String response = rmi.makeCritic(score,criticText,album);
        switch (response.trim()){
            case "type|criticComplete":
                System.out.println("SUCCESS: Critic made.");
                break;
            case "type|criticFail":
                System.out.println("ERROR: Critic not made.");
                break;
        }

    }

    public String notificationEditor(){
        return "no erro";
    }

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
            default:
                System.out.println("Inseriu mal o comando. Por favor volte a tentar.");
        }
    }

    public static int searchAlbumByArtistName(Hello rmi, Scanner reader) throws RemoteException {
        System.out.print("Insert user's name: ");
        boolean flagK = false;
        String nameA = "";
        while (!flagK) {
            nameA = reader.nextLine();
            if (!nameA.trim().equals("")) {
                flagK = true;
            } else {
                System.out.print("Insert user's name: ");
            }
        }
        String re = rmi.showArtistAlbums(nameA);
        String[] responseSplit = re.trim().split(";");
        if (responseSplit.length > 1) {
            String[] albunsParts = responseSplit[1].split("\\|");
            String albunsNamesFinais = "";
            int i;
            for(i=1;i<albunsParts.length;i++){
                if(i!=albunsParts.length-1){
                    albunsNamesFinais += (albunsParts[i] + ",");
                }
                else{
                    albunsNamesFinais += (albunsParts[i]);
                }
            }
            System.out.println("\nAlbums:"+albunsNamesFinais);
            return 1;
        }
        else{
            System.out.println("No albums");
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
        String[] artistParts = responseSplit[1].trim().split("\\|");
        switch (responseSplit[0]) {
            case "type|showArtistComplete":
                System.out.println(artistParts[1]); //easy way, i don't if i can use it
                /*if (responseSplit.length > 3) {
                    String[] nome = responseSplit[1].split("\\|");
                    String[] genre = responseSplit[2].split("\\|");
                    String[] description = responseSplit[3].split("\\|");
                    String[] albunsParts = responseSplit[4].split("\\|");
                    String albunsNamesFinais = "";
                    int i;
                    for(i=2;i<albunsParts.length;i++){
                        albunsNamesFinais += (albunsParts[i] + ",");
                    }
                    System.out.println("\nName: "+nome[1]+"\nGenre: "+genre[1]+"\nDescription: "+description[1]+"\nAlbums:"+albunsNamesFinais);
                }
                else{
                    String[] nome = responseSplit[1].split("\\|");
                    String[] genre = responseSplit[2].split("\\|");
                    String[] description = responseSplit[3].split("\\|");
                    System.out.println(nome[1]+"-"+genre[1]+"-"+description[1]);
                }*/
                break;
            case "type|showArtistFail":
                System.out.println("Artist not Shown.");
                break;
        }
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
        String resp = rmi.showAlbum(n);
        String[] respSplit = resp.trim().split(";");
        if(respSplit.length>1){
            String[] s = respSplit[1].split("\\|");
            System.out.println(s[1]);
        }
        else{
            System.out.println("ERROR: Show Album Failed.");
        }
    }

    public static void editorMenu(Hello rmi,Scanner reader) throws RemoteException{
        if(!loggedUser.isEditor()){
            System.out.println("You don't have permission to do this.");
        }
        else{
            System.out.println("What do you want to do: Create, Edit, Delete?");
            String response = reader.nextLine();
            switch (response.trim()){
                case "/create":
                    createMenu(rmi,reader);
                    break;
                case "/edit":
                    editMenu(rmi,reader);
                    break;
                case "/delete":
                    deleteMenu(rmi,reader);
                    break;
                default:
                    break;
            }
        }
    }

    public static void createMenu(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to create: Artist, Music, Album?");
        String response = reader.nextLine();
        switch(response.trim()){
            case "/artist":
                createArtist(rmi,reader);
                break;
            case "/album":
                createAlbum(rmi,reader);
                break;
            default:
                //Something;
        }
    }

    public static void editMenu(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to edit: Artist, Music, Album?");
        String response = reader.nextLine();
        switch(response.trim()){
            case "/artist":
                editArtist(rmi,reader);
                break;
            case "/music":
                //createMusic();
                break;
            case "/album":
                //createAlbum();
                break;
            default:
                //Something;
        }
    }

    public static void deleteMenu(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to delete: Artist, Music, Album");
        String response = reader.nextLine();
        switch(response.trim()){
            case "/artist":
                deleteArtist(rmi,reader);
                break;
            case "/music":
                //createMusic();
                break;
            case "/album":
                //createAlbum();
                break;
            default:
                //Something;
        }
    }

    public static void createArtist(Hello rmi,Scanner reader) throws RemoteException{
        boolean flagOK = false;
        System.out.println("Insert your data('name-genre-description')");
        String text = "";
        String[]data = new String[3];
        while(!flagOK) {
            text = reader.nextLine();
            data = text.trim().split("-");
            if(data.length == 3){
                if(data[0].trim().equals("") || data[1].trim().equals("") || data[2].trim().equals("")){
                    System.out.println("Insert your data('name-genre-description')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your data('name-genre-description')");
            }
        }
        String response = rmi.createArtist(data[0], data[1], data[2]);
        switch (response.trim()){
            case "type|artistExists":
                System.out.println("Artist already exists.");
                break;
            case "type|createArtistComplete":
                System.out.println("SUCCESS: Artist created successfully.");
                break;
            default:
                //something;
        }
    }

    public static void createAlbum(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("Insert your data('name-artist-description-duration')");
        boolean flagOK = false;
        String text = "";
        String[]data = new String[4];
        while(!flagOK) {
            text = reader.nextLine();
            data = text.split("-");
            if(data.length == 4){
                if(data[0].trim().equals("") || data[1].trim().equals("") || data[2].trim().equals("") || data[3].trim().equals("")){
                    System.out.println("Insert your data('name-genre-description-duration')");
                }
                else {
                    flagOK = true;
                }
            }
            else{
                System.out.println("Insert your data('name-artist-description-duracao')");
            }
        }
        String response = rmi.createAlbum(data[0], data[1], data[2], data[3]);
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

    public static void editArtist(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("What do you wanna change: Name, Genre, Description");
        String text = reader.nextLine();
        switch(text.trim()){
            case "/name":
                editName(rmi,reader);
                break;
            case "/genre":
                editGenre(rmi,reader);
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
            case "type|nameNotChanged":
                System.out.println("Name not changed.");
                break;
            default:
                //something();
        }
    }

    public static void editGenre(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("Which artist you wanna change? ");
        boolean flagOK = false;
        String artist = "";
        String genreAfter="";
        while(!flagOK) {
            artist = reader.nextLine();
            if (!artist.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("Which artist you wanna change? ");
            }
        }
        System.out.println("To what music genre you wanna change it? ");
        flagOK = false;
        while(!flagOK) {
            genreAfter = reader.nextLine();
            if (!genreAfter.trim().equals("")){
                flagOK = true;
            }
            else{
                System.out.println("To what music genre you wanna change it? ");
            }
        }
        String response = rmi.editArtistGenre(artist,genreAfter);
        switch(response.trim()){
            case "type|genreChanged":
                System.out.println("Genre changed.");
                break;
            case "type|genreNotChanged":
                System.out.println("Genre not changed.");
                break;
            default:
                //something();
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
        /*System.out.println("Which artist you wanna change? ");
        String artist = reader.nextLine();
        System.out.println("To what description you wanna change it? ");
        String description = reader.nextLine();*/
        String response = rmi.editArtistDescription(artist,description);
        switch(response.trim()){
            case "type|descriptionChanged":
                System.out.println("Description changed.");
                break;
            case "type|descriptionNotChanged":
                System.out.println("Description not changed.");
                break;
            default:
                //something();
        }
    }

    public static void deleteArtist(Hello rmi,Scanner reader) throws RemoteException{
        System.out.print("Insert user's name: ");
        boolean flagK = false;
        String name = "";
        while (!flagK) {
            name = reader.nextLine();
            if (!name.trim().equals("")) {
                flagK = true;
            } else {
                System.out.print("Insert user's name: ");
            }
        }
        String response = rmi.deleteArtist(name);
        switch (response.trim()){
            case "type|artistNotFound":
                System.out.println("Artist not found.");
                break;
            case "type|deleteArtistComplete":
                System.out.println("SUCCESS: Artist deleted successfully.");
                break;
            default:
                //something;
        }
    }

    }

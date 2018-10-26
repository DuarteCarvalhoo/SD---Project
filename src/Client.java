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
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import java.io.*;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Scanner;
import java.rmi.registry.Registry;
import java.net.*;

public class Client {

    private static User loggedUser = new User();
    private static ArrayList<User> users = new ArrayList<>();
    private Client() {}

    public static void main(String[] args) throws IOException, NotBoundException {
        String text = "";
        Hello rmi = null;
        //String host = (args.length < 1) ? null : args[0];
        Scanner reader = new Scanner(System.in);
        try {
            Registry registry = LocateRegistry.getRegistry(7000);
            rmi =(Hello) registry.lookup("Hello");
            Hello stub = (Hello) registry.lookup("Hello");
            String response = stub.sayHello();
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        while(!text.equals("quit")){
            try{
                System.out.println("Escreva a sua mensagem:");
                text = reader.nextLine();
                rmi.msgInput(text);
                switch(text){
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

    /*public static void downloadMusic() throws IOException {
        String adress = "localhost";
        Socket s= new Socket(adress,5000);
    }*/

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
        directorias.add("C:\\Users\\Duarte\\Desktop\\SD\\PROJETO\\META 1\\SD---Project\\musicasServer\\a.mp3");
        directorias.add("C:\\Users\\Duarte\\Desktop\\SD\\PROJETO\\META 1\\SD---Project\\musicasServer\\b.mp3");
        directorias.add("C:\\Users\\Duarte\\Desktop\\SD\\PROJETO\\META 1\\SD---Project\\musicasServer\\c.mp3");

        System.out.println("Choose one song:");
        //aqui fazia-se print da lista de musicas na base de dados..

        String escolha = reader.nextLine();

        new File("C:\\\\Users\\\\Duarte\\\\Desktop\\\\SD\\\\PROJETO\\\\META 1\\\\SD---Project\\\\musicasServer\\\\"+loggedUser.getUsername()).mkdirs();
        String musicaEscolhida = "C:\\\\Users\\\\Duarte\\\\Desktop\\\\SD\\\\PROJETO\\\\META 1\\\\SD---Project\\\\musicasServer\\\\" +escolha + ".mp3";

        //fazer o socket ligar primeiro no multi mas sem pedir pra aceitar
        System.out.println(rmi.startServerSocket());
        Socket socket = new Socket("0.0.0.0", 5041);
        String print = rmi.downloadMusicRMI(musicaEscolhida);
        byte[] b = new byte [1024];
        InputStream is = socket.getInputStream();
        FilePermission permission = new FilePermission("C:\\\\Users\\\\Duarte\\\\Desktop\\\\SD\\\\PROJETO\\\\META 1\\\\SD---Project\\\\musicasServer\\\\"+loggedUser.getUsername(),"write");
        FileOutputStream fOutStream = new FileOutputStream("C:\\\\Users\\\\Duarte\\\\Desktop\\\\SD\\\\PROJETO\\\\META 1\\\\SD---Project\\\\musicasServer\\\\"+loggedUser.getUsername());
        BufferedOutputStream bOutStream = new BufferedOutputStream(fOutStream);

        int aux= 0;
        while ((aux = is.read(b))!=-1){
            System.out.println(b.length);
            bOutStream.write(b, 0, aux);
            if(is.available()==0){
                break;
            }
        }

        try{
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        is.close();
        fOutStream.close();
        bOutStream.flush();
        bOutStream.close();
        socket.close();

        System.out.println("ficheiro 100% completo");

        return print;
    }

    private static String sendMusic(Hello rmi) throws IOException {

        ServerSocket socket = new ServerSocket(5000);
        System.out.println("check1");
        String[] aux = socket.getLocalSocketAddress().toString().split("/");
        rmi.startSocket(aux[0]);
        Socket socketAcept = socket.accept();
        System.out.println("Write down the directory of your music: (example:'C:\\Duarte\\example.wav').");
        Scanner direc = new Scanner(System.in);
        String auxx = direc.nextLine();
        File file = new File(auxx);
        String[] aux1 = auxx.split("\\\\");
        System.out.println("can it split?");
        System.out.println(aux1[aux1.length-1]);
        FilePermission permission = new FilePermission(auxx, "read");
        FileInputStream fInStream= new FileInputStream(file);
        System.out.println("tou much bytes?");
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
        return aux1[aux1.length-1];
    }

    public static void login(Hello rmi, Scanner reader) throws IOException, NotBoundException {
        System.out.println("Insert your login('username-password'):");
        String userData = reader.nextLine();
        String txt = rmi.checkLogin(userData);
        String[] txtSplit = txt.split(";");
        switch (txtSplit[0]){
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
        switch(txt){
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
        String txt = reader.nextLine();
        txt = rmi.checkRegister(txt);
        switch (txt){
            case "type|usernameUsed":
                System.out.println("That username already exists.");
                break;
            case "type|registComplete":
                System.out.println("Successful register.");
                //menuPrincipal(rmi,reader);
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
                    System.out.println("MENU PRINCIPAL:\n" +
                            "Search\n" +
                            "Edit\n" +
                            "Make editor\n" +
                            "Upload\n" +
                            "Download\n\n" +
                            "Choose an option: ");
                }
                else{
                    System.out.println("MENU PRINCIPAL:\n" +
                        "Search\n" + "Upload\n" +
                        "Download\n\n" +
                        "Choose an option: ");}

                String text = reader.nextLine();
                while(!flag){
                    if(text.equals("/login") || text.equals("/register")){
                        System.out.println("To login into another account or register another user, please logout first!");
                        text = reader.nextLine();
                    }
                    else{
                        flag=true;
                    }
                }
                rmi.msgInput(text);
                switch(text){
                    case "/search":
                        menuDePesquisa(rmi, reader);
                        break;
                    case "/edit":
                        editorMenu(rmi, reader);
                        break;
                    case "/logout":
                        logout(rmi,reader);
                        return;
                    case "/makeeditor":
                        makeEditor(rmi,reader);
                        break;
                    case "/download":
                        System.out.println(downloadMusic(rmi, reader));
                        break;
                    case "/upload":
                        String musicName = sendMusic(rmi);
                        System.out.println(rmi.sendMusicRMI(musicName));
                        break;
                    default:
                        System.out.println("Este comando não faz nada. Para sair escreva 'leave'");
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
        String name = reader.nextLine();
        String response = rmi.checkEditorMaking(name);

        switch (response){
            case "type|makingEditorComplete":
                System.out.println(name +" is now an editor.");
                break;
            case "type|makingEditorFail":
                System.out.println("Making "+name+" an Editor didn't work.");
                break;
        }
    }

    public static void menuDePesquisa(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to search: Artist, Music, Album?");
        String text = reader.nextLine();
        switch (text) {
            case "/artist":
                System.out.println("Which artist you wanna show? ");
                String name = reader.nextLine();
                String response = rmi.showArtist(name);
                String[] responseSplit = response.split(";");
                switch (responseSplit[0]) {
                    case "type|showArtistComplete":
                        if (responseSplit.length == 4) {
                            String[] nome = responseSplit[1].split("\\|");
                            String[] genre = responseSplit[2].split("\\|");
                            String[] description = responseSplit[3].split("\\|");
                            System.out.println(nome[1]+"-"+genre[1]+"-"+description[1]);
                        }
                        break;
                    case "type|showArtistFail":
                        System.out.println("Artist not Shown.");
                        break;
                }
            case "Genero":
                break;
            case "Album":
                break;
            default:
                System.out.println("Inseriu mal o comando. Por favor volte a tentar.");
        }
    }

    public static void editorMenu(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("What do you want to do: Create, Edit, Delete?");
        String response = reader.nextLine();
        switch (response){
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

    public static void createMenu(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to create: Artist, Music, Album?");
        String response = reader.nextLine();
        switch(response){
            case "/artist":
                createArtist(rmi,reader);
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

    public static void editMenu(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("What do you want to edit: Artist, Music, Album?");
        String response = reader.nextLine();
        switch(response){
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
        switch(response){
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
        System.out.println("Insert your data('name-genre-description')");
        String text = reader.nextLine();
        String[] data = text.split("-");
        String response = rmi.createArtist(data[0], data[1], data[2]);
        switch (response){
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

    public static void editArtist(Hello rmi,Scanner reader) throws RemoteException{
        System.out.println("What do you wanna change: Name, Genre, Description");
        String text = reader.nextLine();
        switch(text){
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
        String artist = reader.nextLine();
        System.out.println("To what name you wanna change it? ");
        String nameAfter = reader.nextLine();
        String response = rmi.editArtistName(artist,nameAfter);
        switch(response){
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
        String artist = reader.nextLine();
        System.out.println("To what music genre you wanna change it? ");
        String genre = reader.nextLine();
        String response = rmi.editArtistGenre(artist,genre);
        switch(response){
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
        String artist = reader.nextLine();
        System.out.println("To what description you wanna change it? ");
        String description = reader.nextLine();
        String response = rmi.editArtistDescription(artist,description);
        switch(response){
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
        System.out.println("Insert your data('name')");
        String name = reader.nextLine();
        String response = rmi.deleteArtist(name);
        switch (response){
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

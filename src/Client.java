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
import java.net.Socket;
import java.rmi.NotBoundException;
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

    private static String sendMusic(Hello rmi) throws IOException {

        ServerSocket socket = new ServerSocket(5000);
        System.out.println("check1");
        String[] aux = socket.getLocalSocketAddress().toString().split("/");
        rmi.startSocket(aux[0]);
        Socket socketAcept = socket.accept();
        System.out.println("Write down the directory of your music: (example:'C:\\Duarte\\example.wav').");
        Scanner direc = new Scanner(System.in);
        String auxx = direc.nextLine();
        String[] aux1 = auxx.split("\\\\");
        System.out.println("can it split?");
        System.out.println(aux1[aux1.length-1]);
        FilePermission permission = new FilePermission(auxx, "read");
        FileInputStream fInStream= new FileInputStream(auxx);
        System.out.println("tou much bytes?");
        byte b[] = new byte [2002];
        System.out.println("no");
        fInStream.read(b, 0, b.length);
        System.out.println("too big too read");
        OutputStream outStream = socketAcept.getOutputStream();
        System.out.println("is this it?");
        outStream.write(b, 0, b.length);
        System.out.println("too big too write");
        outStream.flush();
        socket.isClosed();
        return aux1[aux1.length-1];
    }

    public static void login(Hello rmi, Scanner reader) throws IOException, NotBoundException {
        System.out.println("Insert your login('username-password'):");
        String userData = reader.nextLine();
        String txt = rmi.checkLogin(userData);
        String[] txtSplit = txt.split(";");
        System.out.println(txt);
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
                        //editMenu(rmi, reader);
                        break;
                    case "/logout":
                        logout(rmi,reader);
                        return;
                    case "/download":
                        //downloadMusic();
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

    public static void menuPrincipalEditor(Hello rmi, Scanner reader){
    }

    public static void menuDePesquisa(Hello rmi, Scanner reader) throws RemoteException{
        System.out.println("O que deseja pesquisar?\n" +
                "Artista\n" +
                "Genero\n" +
                "Album");
        String text = reader.nextLine();
        rmi.msgInput(text);
        switch (text){
            case "Artista":
                break;
            case "Genero":
                break;
            case "Album":
                break;
            default:
                System.out.println("Inseriu mal o comando. Por favor volte a tentar.");
        }
    }
}

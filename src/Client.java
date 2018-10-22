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

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}

    public static void main(String[] args) {
        String text = "";
        Hello rmi = null;
        String host = (args.length < 1) ? null : args[0];
        Scanner reader = new Scanner(System.in);
        try {
            Registry registry = LocateRegistry.getRegistry(host);
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
                    default:
                        System.out.println("Este comando não faz nada. Para sair escreva 'quit'");
                }
            }
            catch(RemoteException e){
                e.printStackTrace();
            }
        }
        reader.close();
        System.out.println("Finished");
    }

    public static void login(Hello rmi, Scanner reader) throws RemoteException {
        System.out.println("Insert your login('username-password'):");
        String txt = reader.nextLine();
        txt = rmi.checkLogin(txt);
        switch (txt){
            case "type|loginComplete":
                System.out.println("Welcome!");
                //menuPrincipal(rmi, reader);
                break;
            case "type|loginFail":
                System.out.println("Login failed.");
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

    /*public static void menuPrincipal(Hello rmi, Scanner reader){
        while(!reader.equals("leave")){
            try{
                System.out.println("MENU PRINCIPAL:\n" +
                        "Pesquisar\n" +
                        "Upload\n" +
                        "Download\n\n" +
                        "Escolha a sua opcao.");
                String text = reader.nextLine();
                rmi.msgInput(text);
                switch(text){
                    case "Pesquisar":
                        menuDePesquisa(rmi, reader);
                        break;
                    case "Uploda":
                        registo(rmi, reader);
                        break;
                    case "Download":
                        break;
                    default:
                        System.out.println("Este comando não faz nada. Para sair escreva 'leave'");
                }
            }
            catch(RemoteException e){
                e.printStackTrace();
            }
        }
    }

    public static void menuPrincipalEditor(Hello rmi, Scanner reader){
        System.out.println("MENU PRINCIPAL:\n" +
                "Pesquisar\n" +
                "Gerir" +
                "Upload\n" +
                "Download");
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
    }*/
}

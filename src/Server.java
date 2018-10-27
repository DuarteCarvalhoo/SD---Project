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

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Server implements Hello {
    private String MULTICAST_ADDRESS = "224.0.224.0";
    private int PORT = 4321;


    public Server() {
    }

    public String sayHello() {
        return "Hello, world!";
    }

    public String msgInput(String text) {
        System.out.println("escreveu uma mensagem: " + text);
        return " ";
    }

    public String startSocket(String clientAddress){
        MulticastSocket socket = null;
        //envia pra o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|turnOnSocket;address|"+clientAddress; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
        return receiveMulticast();
    }

    public String sendMusicRMI(String[] musicInfo,String loggedUser){
        MulticastSocket socket = null;
        //envia pra o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|sendMusic;musicPath|"+musicInfo[0]+";musicName|"+musicInfo[1]+";musicComposer|"+musicInfo[2]+";musicArtist|"+musicInfo[3]+";musicDuration|"+musicInfo[4]+";musicAlbum|"+musicInfo[5]+";musicGenre|"+musicInfo[6]+";loggedUser|"+loggedUser;//protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
        String msg = receiveMulticast();
        return msg;
    }

    public String startServerSocket(){
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|openSocket;";
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
        return receiveMulticast();
    }

    public String downloadMusicRMI(String direc){
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|downloadMusic;musicAddress|"+direc;
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
        return receiveMulticast();
    }

    public String checkEditorMaking(String name){
        MulticastSocket socket = null;
        //envia pra o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|makeEditor;User|"+name; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
        String msg = receiveMulticast();
        return msg;
    }

    public String createArtist(String name, String genre, String description){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|createArtist;Name|"+name+";Description|"+description+";Genre|"+genre; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;

        return null;
    }

    public String createAlbum(String name, String artistName, String description, String duracao){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|createAlbum;Name|"+name+";ArtistName|"+artistName+";Description|"+description+";Duracao|"+duracao; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;

        return null;
    }

    public String editArtistName(String nameBefore, String nameAfter){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|editArtistName;NameBefore|"+nameBefore+";NameAfter|"+nameAfter; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;

        return null;
    }

    public String editArtistGenre(String name, String newGenre){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|editArtistGenre;Name|"+name+";genreAfter|"+newGenre; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;

        return null;
    }

    public String editArtistDescription(String name, String newDescription){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|editArtistDescription;Name|"+name+";descriptionAfter|"+newDescription; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;

        return null;
    }

    public String deleteArtist(String name){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|deleteArtist;Name|"+name; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;

        return null;
    }

    public String showArtist(String name){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|showArtist;Name|"+name; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;
        return null;
    }

    public String showArtistAlbums(String name){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|showArtistAlbums;Name|"+name; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;
        return null;
    }

    public String showAlbum(String name){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|showAlbum;Name|"+name; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;
        return null;
    }

    public String makeCritic(double score, String text, String album){
        MulticastSocket socket = null;
        //envia para o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|makeCritic;Score|"+score+";Text|"+text+";Album|"+album; //protocol
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        String msg = receiveMulticast();
        if (msg != null) return msg;
        return null;
    }

    public String checkLogin(String login) {
        System.out.println("Entrou no Login");
        System.out.println(login);
        String[] newLogin = login.split("-");
        MulticastSocket socket = null;
        //envia pra o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|login;username|" + newLogin[0] + ";password|" + newLogin[1]; //protocol
            System.out.println(aux); //ver como ficou
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
            //recebe do multicast
            String msg = receiveMulticast();
            return msg;
            //socket.leaveGroup(group);
            //falta receber a resposta se ja existe ou nao, se a pw ta bem etc...
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            socket.close();
        }

        return "ups";
    }

    private String receiveMulticast() {
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            byte[] buffer = new byte[2002];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(), 0, packet.getLength());
            System.out.println(msg);

            socket.close();
            return msg;

        } catch (IOException e) {
            e.printStackTrace();
        }
        //finally {
        //    socket.close();
        //}
        return null;
    }

    public String checkLogout(User user) {
        System.out.println("Entrou no logout");
        System.out.println(user.getUsername());
        MulticastSocket socket = null;
        //envia pra o multicast
        try {
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|logout;username|" + user.getUsername(); //protocol
            System.out.println(aux); //ver como ficou
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
            //recebe do multicast
            String msg = receiveMulticast();
            if (msg != null) return msg;
            socket.leaveGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }



        return "ups";
    }

    public String checkRegister(String register) {
            System.out.println("Está no registo.");
            String[] newRegisto = register.split("-");
            MulticastSocket socket = null;

            try {
                socket = new MulticastSocket();
                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                socket.joinGroup(group);
                String aux = "type|register;username|" + newRegisto[0] + ";password|" + newRegisto[1]; //protocol
                System.out.println(aux); //ver como ficou
                byte[] buffer = aux.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
                //recebe do multicast
                String msg = receiveMulticast();
                if (msg != null) return msg;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                socket.close();
            }


        /*
        if(newRegisto.length != 3){
            return "preencheu mal os campos por favor tente de novo";
        }
        if (usernamesTodos.contains(newRegisto[0])) {
            return "usado";
        } else if (!newRegisto[1].equals(newRegisto[2])) {
            return "wrongPW";
        } else if (newRegisto[1].equals(newRegisto[2])) {
            usernamesTodos.add(newRegisto[0]);
            //registar o user
            return "registado";
        }*/
            return "ups";
    }

    public String ping() {
            return "pong";
        }

    public static String readIPFile() {
        String line = null;
        String ip = "";
        try {
            FileReader fileR = new FileReader("ip local.txt");
            BufferedReader bufferedR = new BufferedReader(fileR);
            while((line=bufferedR.readLine()) !=null){
                ip = line;
            }
        } catch (FileNotFoundException e){
            System.out.println("Not found");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ip;
    }
    public static void main (String[]args){
        String ip = readIPFile();
        //System.setProperty("localhost", "192.168.1.74");
        System.setProperty("java.rmi.server.hostname", ip);
        int aux = 0;
        while (aux < 1) {
            try {
                Hello connect = (Hello) LocateRegistry.getRegistry(7000).lookup("Hello");
                connect.ping();
                System.out.println("Pong");
                aux = 0;
            } catch (NotBoundException e) {
                System.out.println("Not bound.");
                e.printStackTrace();
            } catch (RemoteException e) {
                System.out.println("test try fail");
                aux++;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Server obj = new Server();
            Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.createRegistry(7000);
            registry.rebind("Hello", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }


}
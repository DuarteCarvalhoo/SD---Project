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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Server implements Hello {
    private String MULTICAST_ADDRESS = "224.0.224.0";
    private int PORT = 4321;


    public Server() {}

    public String sayHello() {
        return "Hello, world!";
    }

    public String msgInput(String text) {
        System.out.println("escreveu uma mensagem: " + text);
        return " ";
    }

    public String checkLogin(String login){
        System.out.println("Entrou no Login");
        System.out.println(login);
        String[] newLogin = login.split("-");
        MulticastSocket socket = null;
        //envia pra o multicast
        try{
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|login;username|" + newLogin[0] + ";password|" + newLogin[1]; //protocol
            System.out.println(aux); //ver como ficou
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
            System.out.println("test1");
            //falta receber a resposta se ja existe ou nao, se a pw ta bem etc...
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        try{
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength());
            System.out.println(msg);

            return msg;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }

        return "ups";
    }

  public String checkRegister(String register){
        System.out.println("Está no registo.");
        String[] newRegisto = register.split("-");
        MulticastSocket socket = null;

        try{
            socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            String aux = "type|register;username|" + newRegisto[0] + ";password|" + newRegisto[1]; //protocol
            System.out.println(aux); //ver como ficou
            byte[] buffer = aux.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
            System.out.println("test2");
            //falta receber a resposta se ja existe ou nao, se a pw ta bem etc...
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            socket.close();
        }

        //recebe do multicast
        try{
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength());
            System.out.println(msg);

            return msg;
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

    public static void main(String[] args) {

        //usernamesTodos.add("duarte");
        try {
            Server obj = new Server();
            Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Hello", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

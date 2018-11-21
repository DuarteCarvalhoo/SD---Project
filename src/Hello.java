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

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Hello extends Remote {

    void addOnlineUser(User aux) throws RemoteException;

    void removeOnlineUser(User aux) throws RemoteException;

    String downloadMusicRMI(String direc) throws RemoteException;

    String startServerSocket() throws RemoteException;

    String startSocket(String clientAddress) throws RemoteException;

    String msgInput(String text) throws RemoteException;

    String checkLogin(String login) throws RemoteException;

    String checkRegister(String register) throws RemoteException;

    String checkLogout(User user) throws RemoteException;

    String sendMusicRMI(String[] musicInfo, String loggedUser) throws RemoteException;

    String ping() throws RemoteException;

    String checkEditorMaking(String name, Hello rmi) throws RemoteException;

    String createSongwriter(String name, String description) throws RemoteException;

    String createMusician(String name, String description) throws RemoteException;

    String createComposer(String name, String description) throws RemoteException;

    String createBand(String name, String description) throws RemoteException;

    String deleteArtist(String name) throws RemoteException;

    String editArtistName(String nameBefore, String nameAfter) throws RemoteException;

    String editArtistGenre(String name, String newGenre) throws RemoteException;

    String showArtist(String name) throws RemoteException;

    String editArtistDescription(String name, String newDescription) throws RemoteException;

    String createAlbum(String name, String artistName, String description, String duracao) throws RemoteException;

    String showAlbum(String name) throws RemoteException;

    String makeCritic(double score, String text, String album) throws RemoteException;

    String showArtistAlbums(String name) throws RemoteException;

    String shareMusic(String music, String userName) throws RemoteException;
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.beinlich.markus.musicsystem.guifx;

import de.beinlich.markus.musicsystem.model.net.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Markus
 */
public class AddLocalIpToServerPool {
    public static void main(String[] args) {
        ServerPool serverPool = ServerPool.getInstance("Client");
        try {
            serverPool.addServer("Local", new ServerAddr(50001, InetAddress.getLocalHost().getHostAddress(), "Local", true));
        } catch (UnknownHostException ex) {
            Logger.getLogger(AddLocalIpToServerPool.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

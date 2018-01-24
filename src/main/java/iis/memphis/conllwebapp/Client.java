/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iis.memphis.conllwebapp;

import java.io.*;
import java.net.*;

/**
 *
 * @author noah
 */
public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    
    public void startConnection(String ip, int port){
        try{
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e){
            System.out.println("Client-side connection could not be started.");
        }
        
    }
    
    public String sendMessage(String msg){
        try{
            out.println(msg);
            String resp = in.readLine();
            return resp;
        } catch (IOException e){
            System.out.println("Message could not be sent to server.");
            return "Message could not be sent to server.";
        }
        
    }
    
    public void stopConnection(){
        try{
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e){
            System.out.println("Connection could not be successfully closed.");
        }
    }
    
    
}

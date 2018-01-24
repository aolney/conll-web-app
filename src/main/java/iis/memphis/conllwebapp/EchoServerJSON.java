/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iis.memphis.conllwebapp;

import java.io.*;
import java.net.*;
import org.json.simple.JSONObject;

/**
 *
 * @author noah
 */
public class EchoServerJSON {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    
    //start the server and wait for input
    public void start(int port){
        try{
            serverSocket = new ServerSocket(port);
            

            System.out.println("JSON echo server started " + serverSocket.getInetAddress().getHostAddress() + " on port " + serverSocket.getLocalPort() );
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null){
                if (".".equals(inputLine)){
                    out.println("Good bye.");
                    break;
                }
                //codify new object as json and return it in plain text
                JSONObject object = new JSONObject();
                object.put("result", inputLine);
                StringWriter jsonOut = new StringWriter();
                object.writeJSONString(jsonOut);
                String jsonText = jsonOut.toString();
                out.println(jsonText);
            }
        } catch(IOException e) {
            System.out.println("Something has gone wrong.");
        }
    }
    
    //Initialize the server and begin listening on port 4444
    public static void main(String[] args){
        EchoServerJSON server = new EchoServerJSON();
        server.start(4444);
    }
}
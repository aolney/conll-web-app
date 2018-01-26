/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.memphis.iis.conllwebapp;

import java.io.*;
import java.net.*;
import org.json.simple.JSONObject;

//import processors here
import edu.memphis.iis.MatePlusProcessor;
import edu.memphis.iis.CoNLL09MemoryWriter;
import org.clulab.processors.corenlp.CoreNLPProcessor;


import java.util.ArrayList;
import java.util.List;
import static junit.framework.Assert.assertTrue;
import org.clulab.processors.Document;
import org.clulab.processors.Processor;
import org.clulab.processors.Sentence;

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
    public void start(int port) throws Exception{
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

                out.println("MatePlus processing beginning");
                String output = mateplusProcess();  
                out.println("MatePlus exitted successfully");
                
                out.println("CoreNLP processing beginning");
                //TODO
                
                //codify new object as json and return it in plain text
                JSONObject object = new JSONObject();
                object.put("result", output);
                StringWriter jsonOut = new StringWriter();
                object.writeJSONString(jsonOut);
                String jsonText = jsonOut.toString();
                out.println(jsonText);
            }
        } catch(IOException e) {
            System.out.println("Something has gone wrong.");
        }
    }
    
    
    public String mateplusProcess() throws Exception{
        
        MatePlusProcessor processor = new MatePlusProcessor();
        processor.initModels();

        CoNLL09MemoryWriter writer = new CoNLL09MemoryWriter();
        writer.debug = true;

        List<Double> times = new ArrayList<Double>();

        ClassLoader classLoader = getClass().getClassLoader();
        // Line below should be changed based on what type of input we want to feed the program
        File file = new File(classLoader.getResource("input.txt").getFile());
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String ret = "";
        String line;
        while ((line = br.readLine()) != null) {
            long startTime = System.nanoTime();
            //writer.write(processor.parse(line));
            ret += processor.parse(line);
            double elapsedMs = (System.nanoTime() - startTime) / 1000000.0;
            assertTrue(elapsedMs > 0.0);
            times.add(elapsedMs);
        }
        br.close();

        assertTrue(times.size() > 1);

        double min = times.get(0);
        double max = min;
        double sum = 0.0;
        for(double d: times) {
            if (d < min) min = d;
            if (d > max) max = d;
            sum += d;
        }
        double mean = sum / times.size();

        double sumSqErr = 0.0;
        for(double d: times) {
            sumSqErr += Math.pow(mean - d, 2.0);
        }
        double stdev = Math.sqrt(sumSqErr / times.size());


        System.out.println("Count: " + times.size());
        System.out.println(String.format("Min:%.2f Mean/SD:%.2f,%.2f Max:%.2f", min, mean, stdev, max));
        
        return ret;
    }
    
    public void corenlpProcess(){
   
        Processor proc = new CoreNLPProcessor(true, true, 1, 1000);
        Document doc = proc.annotate("No one actually knows what drives reef resilience or even what a coral reef looks like as it is rebounding.", false);
        int sentenceCount = 0;
        for (Sentence sentence: doc.sentences()) {
            System.out.println("Sentence #" + sentenceCount + ":");
            System.out.println("Tokens: " + mkString(sentence.words(), " "));
            System.out.println("Start character offsets: " + mkString(sentence.startOffsets(), " "));
            System.out.println("End character offsets: " + mkString(sentence.endOffsets(), " "));

            // these annotations are optional, so they are stored using Option objects, hence the isDefined checks
            if(sentence.lemmas().isDefined()){
                System.out.println("Lemmas: " + mkString(sentence.lemmas().get(), " "));
            }
            if(sentence.tags().isDefined()){
                System.out.println("POS tags: " + mkString(sentence.tags().get(), " "));
            }
            if(sentence.chunks().isDefined()){
                System.out.println("Chunks: " + mkString(sentence.chunks().get(), " "));
            }
            if(sentence.entities().isDefined()){
                System.out.println("Named entities: " + mkString(sentence.entities().get(), " "));
            }
            if(sentence.norms().isDefined()){
                System.out.println("Normalized entities: " + mkString(sentence.norms().get(), " "));
            }
            sentenceCount += 1;
            System.out.println("\n");
        }          
    }
    //functions to help with the output from corenlpProcess
    public static String mkString(String [] sa, String sep) {
        StringBuilder os = new StringBuilder();
        for(int i = 0; i < sa.length; i ++) {
            if(i > 0) os.append(sep);
            os.append(sa[i]);
        }
        return os.toString();
    }
    public static String mkString(int [] sa, String sep) {
        StringBuilder os = new StringBuilder();
        for(int i = 0; i < sa.length; i ++) {
            if(i > 0) os.append(sep);
            os.append(Integer.toString(sa[i]));
        }
        return os.toString();
    }
    
    
    //Initialize the server and begin listening on port 4444
    public static void main(String[] args) throws Exception{
        EchoServerJSON server = new EchoServerJSON();
        server.start(4444);
    }
}
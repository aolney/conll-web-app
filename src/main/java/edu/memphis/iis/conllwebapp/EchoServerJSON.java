/*
 * Copyright (C) 2018 noahcoomer <nbcoomer@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package edu.memphis.iis.conllwebapp;

// Java imports
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import static junit.framework.Assert.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;

// JSON
import org.json.simple.JSONObject;

// Mateplus poc
import edu.memphis.iis.MatePlusProcessor;
import edu.memphis.iis.CoNLL09MemoryWriter;

// Processors
import org.clulab.processors.corenlp.CoreNLPProcessor;
import org.clulab.struct.CorefMention;
import org.clulab.struct.DirectedGraphEdgeIterator;
import org.clulab.processors.Document;
import org.clulab.processors.Processor;
import org.clulab.processors.Sentence;
import org.clulab.swirl2.Reader;

// Project
import edu.memphis.iis.conllwebapp.CoNLLObject;


/**
 *
 * @author noahcoomer <nbcoomer@gmail.com>
 * @created-on Feb 9, 2018
 * @project-name CoNLLWebApp
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
            
            out.println("Welcome to MatePlus-Processors");

            String inputLine;
            while ((inputLine = in.readLine()) != null){
                if (".".equals(inputLine)){
                    out.println("Good bye.");
                    break;
                }
                
                out.println("MatePlus processing started...");
                String mateplus_output = mateplusProcess(inputLine);
                out.println("MatePlus exitted successfully.");
                
                //System.out.println(mateplus_output);
                
                out.println("CoreNLP processing started...");
                Document corenlp_output = corenlpProcess(mateplus_output);
                out.println("CoreNLP exitted successfully.");
                
                //codify new object as json and return it in plain text
                /**
                JSONObject object = new JSONObject();
                object.put("result", output);
                StringWriter jsonOut = new StringWriter();
                object.writeJSONString(jsonOut);
                String jsonText = jsonOut.toString();
                out.println(jsonText);
                * **/
            }
        } catch(IOException e) {
            System.out.println("Could not start server on port: " + port);
            System.out.println("Exitting process on input.");
            System.in.read();
            return;     
        }
    }
    
    public String mateplusProcess(String inputStr) throws Exception{
        
        // initialize processor, models, and conll writer
        MatePlusProcessor processor = new MatePlusProcessor();
        processor.initModels();
        CoNLL09MemoryWriter writer = new CoNLL09MemoryWriter();
        
        // separate each line for parsing by punctuation marks
        String formattedInputStr = "";
        for (int x = 0; x < inputStr.length();x++){
            formattedInputStr += inputStr.charAt(x);
            if(inputStr.charAt(x) == '.' || inputStr.charAt(x) == '?' || inputStr.charAt(x) == '!'){
                formattedInputStr += "\n";
            }
        }
        
        // split then parse individual sentences
        String lines[] = formattedInputStr.split("\n");
        for(String line: lines){
            writer.write(processor.parse(line));
        }
        
        // format string buffer into single string with punctuation delimitation
        List<String> mateplus = writer.getBuffer();
        String ret = "";
        for(String line: mateplus){
            ret += line;
            ret += "\n\n";
        }

        return ret;
    }
    
    // run the coreNlpProcessor
    public Document corenlpProcess(String inputStr) throws FileNotFoundException, IOException{

        Processor proc = new CoreNLPProcessor(true, true, 1, 200);
        Reader annotator = new Reader();
        
        // Processor needs a file, so create a temporary file with inputStr
        File file = File.createTempFile("temp",".txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(inputStr);
        writer.close();
        
        Document doc = annotator.read(file, proc, true);
        
        boolean debug1 = false;
        if (debug1){
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
                if(sentence.dependencies().isDefined()) {
                    System.out.println("Syntactic dependencies:");
                    DirectedGraphEdgeIterator<String> iterator = new
                        DirectedGraphEdgeIterator<String>(sentence.dependencies().get());
                    while(iterator.hasNext()) {
                        scala.Tuple3<Object, Object, String> dep = iterator.next();
                        // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                        System.out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                    }
                }
                if(sentence.semanticRoles().isDefined()){
                    System.out.println("Semantic dependencies:");
                    DirectedGraphEdgeIterator<String> iterator = new
                        DirectedGraphEdgeIterator<String>(sentence.semanticRoles().get());
                    while(iterator.hasNext()) {
                        scala.Tuple3<Object, Object, String> dep = iterator.next();
                        // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                        System.out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                    }

                }
                if(sentence.syntacticTree().isDefined()) {
                    System.out.println("Constituent tree: " + sentence.syntacticTree().get());
                    // see the org.clulab.struct.Tree class for more information
                    // on syntactic trees, including access to head phrases/words
                }
                sentenceCount += 1;
                System.out.println("\n");
            }
        }
        return doc;
    }
    
    // function to help with the output from corenlpProcess
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
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

// Stanford Sentence Tokenizer
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.ling.HasWord;


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
    MatePlusProcessor processor;
    CoNLL09MemoryWriter writer;
    Processor proc;
    Reader annotator;
    
    public EchoServerJSON() throws IOException{
        // Initialize Clulab NLP Processor and CoNLL Reader
        proc = new CoreNLPProcessor(true, true, 1, 200);
        annotator = new Reader();
        
        // Initialize Mate+ Processor and models and memory writer
        processor = new MatePlusProcessor();
        processor.initModels();
        writer = new CoNLL09MemoryWriter();       
    }
    
    //start the server and wait for input
    public void start(int port) throws Exception{     
        try{
            serverSocket = new ServerSocket(port);
        
            System.out.println("JSON echo server started " + serverSocket.getInetAddress().getHostAddress() + " on port " + serverSocket.getLocalPort());
            //System.out.println("Connection accepted.");
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            out.println("Welcome to the Syntactic and Semantic Role Labelling Pipeline.");
            out.println("Enter a text that you would like to add annotations to: ");

            String inputLine;
            while ((inputLine = in.readLine()) != null){
                if (".".equals(inputLine)){
                    out.println("Good bye.");
                    break;
                }
                System.out.println(inputLine);
                
                
                // Testing pipeline
                boolean test = true;
                if (test){
                    File file = new File("/home/noah/NetBeansProjects/wsj_traversal/output_large_sentences.txt");
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null){
                        // split our input up into sentences
                        List<List> sentences = sentenceTokenizer(line);

                        // Run Mate and return a CoNLL formatted string
                        out.println("MatePlus processing started...");
                        String mateplus_output = mateplusProcess(sentences);
                        out.println("MatePlus exitted successfully.");

                        //System.out.println(mateplus_output);
                        // Run processors and add semantic role labels to processor output
                        out.println("CoreNLP processing started...");
                        Document corenlp_output = corenlpProcess(mateplus_output);
                        out.println("CoreNLP exitted successfully.");
                    }
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
                
                // Preserved Execution Pipeline
                else{
                    //out.println("you fuckt up");
                    List<List> sentences = sentenceTokenizer(inputLine);

                    // Run Mate and return a CoNLL formatted string
                    out.println("MatePlus processing started...");
                    String mateplus_output = mateplusProcess(sentences);
                    out.println("MatePlus exitted successfully.");

                    //System.out.println(mateplus_output);
                    // Run processors and add semantic role labels to processor output
                    out.println("CoreNLP processing started...");
                    Document corenlp_output = corenlpProcess(mateplus_output);
                    out.println("CoreNLP exitted successfully.");
                }
            }
        } catch(IOException e) {
            System.out.println("Could not start server on port: " + port);
            System.out.println("Exitting process on input.");
            System.in.read();
            return;     
        }
    }
    
    
    
    
    // Stanford DocumentPreProcessor sentence-level tokenization
    // Need this function in order to pass large inputs to Mate+ b/c it assumes one sentence per line
    public List<List> sentenceTokenizer(String inputStr) throws IOException{
        
        //write our input string to a file because DocPreProcessor
        File file = File.createTempFile("temp",".txt");
        BufferedWriter writer1 = new BufferedWriter(new FileWriter(file));
        writer1.write(inputStr);
        writer1.close();
        String path = file.getAbsolutePath();
        
        DocumentPreprocessor dp = new DocumentPreprocessor(path);
        List<List> sentences = new ArrayList<>();
        for (List<HasWord> sentence : dp) {
            List<String> small = new ArrayList<>();
            for (HasWord s: sentence){
                small.add(s.word());                
            }
            sentences.add(small);
        }
        
        //clean up cache and return
        file.delete();
        return sentences;
    }
    
    // Run Mate+, return CoNLL09 formatted string where each sentence is \n\n delimited
    // 
    public String mateplusProcess(List<List> sentences) throws Exception{                   
        StringBuilder sb = new StringBuilder();
        for (List<String> sentence : sentences) {
            for (String word: sentence) {               
                sb.append(word + " ");
            }
            sb.append("\n\n");
        }
        String formattedInput = sb.toString();
        String[] splitInput = formattedInput.split("\n\n");
        
        for (String sentence: splitInput)
            writer.write(processor.parse(sentence));
        
        List<String> buffer = writer.getBuffer();
        StringBuilder ret = new StringBuilder();
        for(String element: buffer){
            // Each element is an entire sentence in CoNLL formatting
            ret.append(element);
            ret.append("\n\n");
        }
        writer.clear();
        return ret.toString();      
    }
    
    // Run Processors and add syntactic role labelling
    public Document corenlpProcess(String inputStr) throws FileNotFoundException, IOException{

        // inputStr is CoNLL formatted sentences, each sentence is separated by two new lines
        String[] splitInput = inputStr.split("\n\n");
        
        // Processor needs a file, so create a temporary file and write our input string to that file
        File file = File.createTempFile("temp",".txt");        
        BufferedWriter writer1 = new BufferedWriter(new FileWriter(file));       
        for (String str: splitInput){
            writer1.write(str);
            writer1.write("\n\n");
        }
        writer1.close();
        
        Document doc = annotator.read(file, proc, true);
        
        // Print out all our info to the system console
        boolean debug1 = true;
        if (debug1)
            debugConsole(doc);
            
        // clean up cache and return
        file.delete();
        return doc;
    }
    
    /*
    public String mateTest(List<List> sentences) throws IOException{

    }
    
    public Document procTest(File file) throws IOException{
        
    }
    */
    
    /* <----------------------- MAIN FUNCTION ------------------------> */
    
    //Initialize the server and begin listening on port 4444
    public static void main(String[] args) throws Exception{
        EchoServerJSON server = new EchoServerJSON();
        server.start(4444);
    }
    
    
    /* <----------- HELPER FUNCTIONS FOR PROCESSORS ------------------> */
    
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
    
    public void debugConsole(Document doc){
        int sentenceCount = 0;
        for (Sentence sentence : doc.sentences()) {
            System.out.println("Sentence #" + sentenceCount + ":");
            System.out.println("Tokens: " + mkString(sentence.words(), " "));
            System.out.println("Start character offsets: " + mkString(sentence.startOffsets(), " "));
            System.out.println("End character offsets: " + mkString(sentence.endOffsets(), " "));
            // these annotations are optional, so they are stored using Option objects, hence the isDefined checks
            if (sentence.lemmas().isDefined()) {
                System.out.println("Lemmas: " + mkString(sentence.lemmas().get(), " "));
            }
            if (sentence.tags().isDefined()) {
                System.out.println("POS tags: " + mkString(sentence.tags().get(), " "));
            }
            if (sentence.chunks().isDefined()) {
                System.out.println("Chunks: " + mkString(sentence.chunks().get(), " "));
            }
            if (sentence.entities().isDefined()) {
                System.out.println("Named entities: " + mkString(sentence.entities().get(), " "));
            }
            if (sentence.norms().isDefined()) {
                System.out.println("Normalized entities: " + mkString(sentence.norms().get(), " "));
            }
            if (sentence.dependencies().isDefined()) {
                System.out.println("Syntactic dependencies:");
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<String>(sentence.dependencies().get());
                while (iterator.hasNext()) {
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                    System.out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                }
            }
            if (sentence.semanticRoles().isDefined()) {
                System.out.println("Semantic dependencies:");
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<String>(sentence.semanticRoles().get());
                while (iterator.hasNext()) {
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                    System.out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                }

            }
            if (sentence.syntacticTree().isDefined()) {
                System.out.println("Constituent tree: " + sentence.syntacticTree().get());
                // see the org.clulab.struct.Tree class for more information
                // on syntactic trees, including access to head phrases/words
            }
            sentenceCount += 1;
            System.out.println("\n");
        }       
    }
    
}
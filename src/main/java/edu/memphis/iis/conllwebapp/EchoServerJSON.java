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
import org.clulab.discourse.rstparser.DiscourseTree;

// Apache Sentence Tokenizer
import opennlp.tools.sentdetect.*;

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
    File modelFile;
    SentenceModel model;
    SentenceDetectorME sentenceDetector;
    
    public EchoServerJSON() throws IOException{
        // Initialize Clulab NLP Processor and CoNLL Reader
        proc = new CoreNLPProcessor(true, true, 1, 200);
        annotator = new Reader();
        
        // Initialize Mate+ Processor and models and memory writer
        processor = new MatePlusProcessor();
        processor.initModels();
        writer = new CoNLL09MemoryWriter();
        
        // Sentence Tokenizer Models
        modelFile = new File("/home/noah/NetBeansProjects/conll-web-app/en-sent.bin");
        model = new SentenceModel(modelFile);
        sentenceDetector = new SentenceDetectorME(model);
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
            
            out.println("Sucessfully connected to Discourse, Semantic, and Syntactic Parsing Agent.");
            out.println("Written by: Noah Coomer, Andrew Olney, & Craig Kelly");
            out.println("Enter a sentence you would like to have annotated: ");

            String inputLine;
            while ((inputLine = in.readLine()) != null){
                if (".".equals(inputLine)){
                    out.println("Good bye.");
                    break;
                }
                System.out.println(inputLine);
                
                // consider declaring these methods outside this loop so there is no memory leak
                String[] sentences = opennlpSentenceTokenizer(inputLine);

                out.println("MatePlus started (Semantic Parser, POS tagger, Lemmatizer)...");
                long t1 = System.currentTimeMillis();
                String mateplus_output = mateTest(sentences);
                long t2 = System.currentTimeMillis();
                out.println("MatePlus exitted successfully.");
                out.println("Time: " + (t2 - t1) + " ms");

                //out.println(mateplus_output);
                out.println("CoreNLP processing pipeline started (Syntactic, Discourse, Constituency, Coreference Parsing)...");
                t1 = System.currentTimeMillis();
                Document corenlp_output = corenlpProcess(mateplus_output);
                t2 = System.currentTimeMillis();
                out.println("CoreNLP exitted successfully.");
                out.println("Time: " + (t2 - t1) + " ms");

                //debugInput(corenlp_output, out);
                
                
               
                
            }
        } catch(IOException e) {
            System.out.println("Could not start server on port: " + port);
            System.out.println("Exitting process on input.");
            System.in.read();
            return;     
        }
    }
    
    
    /* <----------------------- PIPELINE FUNCTIONS ------------------------> */
    
    public String[] opennlpSentenceTokenizer(String inputStr) throws IOException{ 
        // separate the lines sentences into an array of Strings
        String[] sentences = sentenceDetector.sentDetect(inputStr);
        return sentences;     
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
        proc.parse(doc);
        proc.resolveCoreference(doc);
        proc.discourse(doc);
        
        
        // Print out all our info to the system console
        boolean debug1 = false;
        if (debug1)
            debugConsole(doc);
            
        // clean up cache and return
        file.delete();
        return doc;
    }
    
     public String mateTest(String[] corpus) throws Exception{                   
        for (String sentence: corpus)
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
     
     
    /* <----------------------- TEST FUNCTIONS ------------------------> */ 
     
    public void testPipeline() throws FileNotFoundException, IOException, Exception
    {
        BufferedReader br = new BufferedReader(new FileReader("/home/noah/NetBeansProjects/wsj_traversal/output_large_sentences.txt"));
        String line;
        File file = new File("test_data_final.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write("MateTokens, CluTokens, Total Sentences, Pipeline Time, Sentence Tokenization Time, Mate Time, Clu Time");
        bw.newLine();
        bw.close();
        long pipelineStart;
        long pipelineEnd;
        long sentenceStart;
        long sentenceEnd;
        long mateStart;
        long mateEnd;
        long cluStart;
        long cluEnd;
        String mateplus_output;
        Document doc;
        double sentenceTime;
        double mateTime;
        double cluTime;
        double pipelineTime;
        int sentenceCount;
        int cluTokens;
        int mateTokens;
        //int totalSentences = 0;
        while ((line = br.readLine()) != null) {

            pipelineStart = System.currentTimeMillis();

            sentenceStart = System.currentTimeMillis();
            String[] sentences = opennlpSentenceTokenizer(line);
            sentenceEnd = System.currentTimeMillis();

            mateStart = System.currentTimeMillis();
            mateplus_output = mateTest(sentences);
            mateEnd = System.currentTimeMillis();

            cluStart = System.currentTimeMillis();
            doc = corenlpProcess(mateplus_output);
            cluEnd = System.currentTimeMillis();

            pipelineEnd = System.currentTimeMillis();

            // Get specific pipeline timings
            sentenceTime = (sentenceEnd - sentenceStart);
            mateTime = (mateEnd - mateStart);
            cluTime = (cluEnd - cluStart);
            pipelineTime = (pipelineEnd - pipelineStart);

            // Get total sentences processed in time steps above
            sentenceCount = sentences.length;

            // NOTE ON TOKENIZATION TESTS
            //      WE CAN FIND A SENTENCE LEVEL TOKENIZATION VIA A LIST IMPLEMENTATION
            //      GO BACK TO THIS METHOD IF MY DATA IS REALLY FAR OFF
            // Get the amount of processed tokens in clu
            cluTokens = 0;
            for (Sentence s : doc.sentences()) {
                cluTokens += s.words().length;
            }

            // Get the amount of processed tokens in mate
            mateTokens = 0;
            String[] inter = mateplus_output.split("\n\n");
            for (String s : inter) {
                String[] info = s.split("\n");
                mateTokens += info.length;
            }

            writeInfoToTestFile(file, sentenceTime, mateTime, cluTime, pipelineTime, sentenceCount, cluTokens, mateTokens);

            sentences = null;
            inter = null;
        }
        //bw.write("Total sentences: " + totalSentences);
        bw.close();
        br.close();
                
    }
    
    public void writeInfoToTestFile(File destination, double sentenceTime, 
        double mateTime, double cluTime, double pipelineTime, int sentenceCount, int cluTokens, int mateTokens) throws IOException{
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(destination, true));
        String delimiter = "\t";
        String st = String.valueOf(sentenceTime);
        String mt = String.valueOf(mateTime);
        String ct = String.valueOf(cluTime);
        String pt = String.valueOf(pipelineTime);
        String sc = String.valueOf(sentenceCount);
        String clu = String.valueOf(cluTokens);
        String mate = String.valueOf(mateTokens);
        
        // Write the sentences output in the form mateTokens, cluTokens, total sentences, time to run total sentences through pipeline,
        //      time to format input, time to run mate, time to run clu
        bw.write(mate + delimiter + clu + delimiter + sc + delimiter + pt + delimiter + st + delimiter + mt + delimiter + ct);
        bw.newLine();
        bw.close();
    }
    
    
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
            if(doc.coreferenceChains().isDefined()) {
                // these are scala.collection Iterator and Iterable (not Java!)
                scala.collection.Iterator<scala.collection.Iterable<CorefMention>> chains = doc.coreferenceChains().get().getChains().iterator();
                while(chains.hasNext()) {
                    scala.collection.Iterator<CorefMention> chain = chains.next().iterator();
                    System.out.println("Found one coreference chain containing the following mentions:");
                    while(chain.hasNext()) {
                        CorefMention mention = chain.next();
                        // note that all these offsets start at 0 too
                        System.out.println("\tsentenceIndex:" + mention.sentenceIndex() +
                            " headIndex:" + mention.headIndex() +
                            " startTokenOffset:" + mention.startOffset() +
                            " endTokenOffset:" + mention.endOffset());
                    }
                }
            }
            if(doc.discourseTree().isDefined()) {
                DiscourseTree tree = doc.discourseTree().get();
                System.out.println("Discourse tree:\n" + tree);
            }
            
            sentenceCount += 1;
            System.out.println("\n");
        }       
    }
    public void debugInput(Document doc, PrintWriter out){
        int sentenceCount = 0;
        for (Sentence sentence : doc.sentences()) {
            out.println("Sentence #" + sentenceCount + ":");
            out.println("Tokens: " + mkString(sentence.words(), " "));
            out.println("Start character offsets: " + mkString(sentence.startOffsets(), " "));
            out.println("End character offsets: " + mkString(sentence.endOffsets(), " "));
            // these annotations are optional, so they are stored using Option objects, hence the isDefined checks
            if (sentence.lemmas().isDefined()) {
                out.println("Lemmas: " + mkString(sentence.lemmas().get(), " "));
            }
            if (sentence.tags().isDefined()) {
                out.println("POS tags: " + mkString(sentence.tags().get(), " "));
            }
            if (sentence.chunks().isDefined()) {
                out.println("Chunks: " + mkString(sentence.chunks().get(), " "));
            }
            if (sentence.entities().isDefined()) {
                out.println("Named entities: " + mkString(sentence.entities().get(), " "));
            }
            if (sentence.norms().isDefined()) {
                out.println("Normalized entities: " + mkString(sentence.norms().get(), " "));
            }
            if (sentence.dependencies().isDefined()) {
                out.println("Syntactic dependencies:");
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<String>(sentence.dependencies().get());
                while (iterator.hasNext()) {
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                    out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                }
            }
            if (sentence.semanticRoles().isDefined()) {
                out.println("Semantic dependencies:");
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<String>(sentence.semanticRoles().get());
                while (iterator.hasNext()) {
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                    out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                }

            }
            if (sentence.syntacticTree().isDefined()) {
                out.println("Constituent tree: " + sentence.syntacticTree().get());
                // see the org.clulab.struct.Tree class for more information
                // on syntactic trees, including access to head phrases/words
            }                  
            if(doc.coreferenceChains().isDefined()) {
                // these are scala.collection Iterator and Iterable (not Java!)
                scala.collection.Iterator<scala.collection.Iterable<CorefMention>> chains = doc.coreferenceChains().get().getChains().iterator();
                while(chains.hasNext()) {
                    scala.collection.Iterator<CorefMention> chain = chains.next().iterator();
                    out.println("Found one coreference chain containing the following mentions:");
                    while(chain.hasNext()) {
                        CorefMention mention = chain.next();
                        // note that all these offsets start at 0 too
                        out.println("\tsentenceIndex:" + mention.sentenceIndex() +
                            " headIndex:" + mention.headIndex() +
                            " startTokenOffset:" + mention.startOffset() +
                            " endTokenOffset:" + mention.endOffset());
                    }
                }
            }
            if(doc.discourseTree().isDefined()) {
                DiscourseTree tree = doc.discourseTree().get();
                out.println("Discourse tree:\n" + tree);
            }
            
            sentenceCount += 1;
            out.println("\n");
        }       
    }
    
}
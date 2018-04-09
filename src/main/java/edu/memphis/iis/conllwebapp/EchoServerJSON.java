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
import se.lth.cs.srl.corpus.*;
import se.lth.cs.srl.corpus.Predicate;


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
import org.clulab.discourse.rstparser.RelationDirection;

import org.json.simple.*;

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
    List<List> predicates;
    
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
        
        predicates = new ArrayList<>();
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
                
                // conll 09 debug
                out.print(mateplus_output);

                out.println(mateplus_output);
                out.println("CoreNLP processing pipeline started (Syntactic, Discourse, Constituency, Coreference Parsing)...");
                t1 = System.currentTimeMillis();
                Document corenlp_output = corenlpProcess(mateplus_output);
                t2 = System.currentTimeMillis();
                out.println("CoreNLP exitted successfully.");
                out.println("Time: " + (t2 - t1) + " ms");
                
                // this is the return, modify to however you need
                JSONObject magic = Jsonify(corenlp_output);
                
                // debug
                out.print(magic.toJSONString());
                out.println();
                if (corenlp_output.discourseTree().isDefined())
                    out.print(corenlp_output.discourseTree().get().visualizerJSON(true, true, true));
                
                // more debug
                debugConsole(corenlp_output);
                debugInput(corenlp_output, out);
                
                
                predicates.clear();
                       
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
        for (String sentence: corpus){
            se.lth.cs.srl.corpus.Sentence magic = processor.parse(sentence);
            writer.write(magic);
            
            if (!magic.getPredicates().isEmpty()){
                List<Predicate> preds = magic.getPredicates();
                
                //preds.get(0).
                predicates.add(preds);
            }
        }
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
    
    public JSONObject Jsonify(Document doc){
        JSONArray jArray = new JSONArray();
        
        int sentenceCount = 0;
        int predCount = 0;
        for (Sentence s : doc.sentences()){
            JSONObject obj = new JSONObject();
            
            // tokenized text
            obj.put("sentence", (mkString(s.words(), " ")));
            obj.put("id", sentenceCount);
            //char start and end offsets
            obj.put("start_offsets", mkString(s.startOffsets(), " "));
            obj.put("end_offsets", mkString(s.endOffsets(), " "));
 
            if (s.lemmas().isDefined()){
                obj.put("lemmas", mkString(s.lemmas().get(), " "));
            }
            if (s.tags().isDefined()){
                obj.put("pos_tags", mkString(s.tags().get(), " "));
            }
            if (s.chunks().isDefined()){
                obj.put("chunks", mkString(s.chunks().get(), " "));
            }
            if (s.entities().isDefined()){
                obj.put("named_entities", mkString(s.entities().get(), " "));
            }
            if (s.norms().isDefined()){
                obj.put("normalized_entities", mkString(s.norms().get(), " "));
            }
            // Syntactic dependencies
            if (s.dependencies().isDefined()){
                JSONArray synDeps = new JSONArray();   
                // create and iterator to loop through the directed graph of syn deps
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<>(s.dependencies().get()); 
                // while there is an edge to go to
                while (iterator.hasNext()) {
                    //create a new json object
                    JSONObject jDep = new JSONObject();
                    // convert annotation into a triple
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    
                    jDep.put("head", dep._1());
                    jDep.put("modifier", dep._2());
                    jDep.put("label", dep._3());
                    
                    synDeps.add(jDep);
                }
                obj.put("syntactic_deps", synDeps);
            }
            // Semantic dependencies
            if (s.semanticRoles().isDefined()){
                JSONArray synDeps = new JSONArray();   
                // create and iterator to loop through the directed graph of sem deps
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<>(s.semanticRoles().get()); 
                
                // while there is an edge to go to
                while (iterator.hasNext()) {
                    //create a new json object
                    JSONObject jDep = new JSONObject();
                    // convert annotation into a triple
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    Predicate pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                    try{                      
                        if (pre.getIdx()-1 != (int)dep._1()){
                            predCount++;
                            pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                        }
                    } catch (IndexOutOfBoundsException e){
                        predCount--;
                        pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                    }
                                     
                    jDep.put("pred", pre.getSense());
                    jDep.put("head", dep._1());
                    jDep.put("modifier", dep._2());
                    jDep.put("label", dep._3());
                   
                    //jDep.put("root", )
                    synDeps.add(jDep);
                }
                obj.put("semantic_deps", synDeps);
            }
            // Constituency Tree
            //TO DO: see the org.clulab.struct.Tree class for more information
            // on syntactic trees, including access to head phrases/words
            if (s.syntacticTree().isDefined()) {             
                obj.put("constituent_tree", s.syntacticTree().get());   
            }
            
            jArray.add(obj);
            sentenceCount++;
            predCount = 0;
        }     
        
        //initialize return object and add the sentence level annotations
        JSONObject ret = new JSONObject();
        ret.put("sentences", jArray);
        
        // document wide analysis of coref chains and discourse tree
        
        if (doc.coreferenceChains().isDefined()){
            JSONArray jChains = new JSONArray();
            scala.collection.Iterator<scala.collection.Iterable<CorefMention>> chains = doc.coreferenceChains().get().getChains().iterator();
            while (chains.hasNext()) {
                JSONArray jChain = new JSONArray();
                scala.collection.Iterator<CorefMention> chain = chains.next().iterator();
                while (chain.hasNext()) {
                    JSONObject link = new JSONObject();
                    CorefMention mention = chain.next();
                    // note that all these offsets start at 0 too
                    link.put("sentence_index", mention.sentenceIndex());
                    link.put("head_index", mention.headIndex());
                    link.put("start_offset", mention.startOffset());
                    link.put("end_offset", mention.endOffset());
                    link.put("chain_id", mention.chainId());
                    link.put("length", mention.length());
                    jChain.add(link);
                }
                jChains.add(jChain);
            }
            ret.put("coref_chains", jChains);
        }
        
        if (doc.discourseTree().isDefined()){            
            DiscourseTree tree = doc.discourseTree().get();
            int depth = 0;
            JSONObject parentObj = new JSONObject();
            JSONObject retObj = dTreeHelper(tree, parentObj, depth);
            ret.put("discourse_trees", retObj);
        }
        return ret;
    }
    
    public JSONObject dTreeHelper(DiscourseTree tree, JSONObject jObj, int depth){
        // Nucleus or Satellite
        jObj.put("kind", tree.kind());
        // make sure this is okay and working
        jObj.put("depth", depth);
        //relLabel and relDir
        if (tree.relationLabel().length() > 0) {
            jObj.put("relLabel", tree.relationLabel());
            if (tree.relationDirection() != RelationDirection.None()) {
                jObj.put("relDir", tree.relationDirection().toString());
            }
        }      
        //token offsets
        if (tree.rawText() != null) {
            jObj.put("text", tree.rawText());
        }
        if (tree.firstToken() != null){
            jObj.put("first_token", tree.firstToken());
        }
        if (tree.lastToken() != null){
            jObj.put("last_token", tree.lastToken());
        }
        
        if (!tree.isTerminal()) {
            DiscourseTree[] kids = tree.children();
            if (kids.length > 0) {
                JSONArray jKids = new JSONArray();
                for (DiscourseTree kid : kids) {
                    JSONObject childObj = new JSONObject();
                    jKids.add(dTreeHelper(kid, childObj, depth+1));
                }
                jObj.put("kids", jKids);
            }                  
        }
        return jObj;
    }
 
    /*
    // 
    public void dTreeHelper(DiscourseTree t, JSONArray jArr){
        JSONObject dObj = new JSONObject();
        // Nucleus or Satellite
        dObj.put("kind", t.kind());
        
        //relLabel and relDir
        if (t.relationLabel().length() > 0) {
            dObj.put("relLabel", t.relationLabel());
            if (t.relationDirection() != RelationDirection.None()) {
                dObj.put("relDir", t.relationDirection().toString());
            }
        }     
        //token offsets
        if (t.rawText() != null) {
            dObj.put("text", t.rawText());
        }       
        //recursive callback for children
        if (!t.isTerminal()) {
            DiscourseTree[] kids = t.children();
            if (kids.length > 0) {
                JSONArray jKids = new JSONArray();
                for (DiscourseTree kid : kids) {                   
                    dTreeHelper(kid, jKids);
                }
                dObj.put("kids", jKids);
            }                  
        }
        jArr.add(dObj);
    }
    */
    public void debugConsole(Document doc){
        int sentenceCount = 0;
        int predCount = 0;
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
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<>(sentence.dependencies().get());
                while (iterator.hasNext()) {
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                    System.out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                }
            }
            if (sentence.semanticRoles().isDefined()) {
                System.out.println("Semantic dependencies:");
                
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<>(sentence.semanticRoles().get());
                while (iterator.hasNext()) {
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    Predicate pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                    try{                      
                        if (pre.getIdx()-1 != (int)dep._1()){
                            predCount++;
                            pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                        }
                    } catch (IndexOutOfBoundsException e){
                        predCount--;
                        pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                    }
                    // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                    System.out.println("pred: " + pre.getSense() + " head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                    
                }
            }
            if (sentence.syntacticTree().isDefined()) {
                System.out.println("Constituent tree: " + sentence.syntacticTree().get());
                // see the org.clulab.struct.Tree class for more information
                // on syntactic trees, including access to head phrases/words
            }                             
            
            sentenceCount += 1;
            predCount = 0;
            System.out.println("\n");
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
        if (doc.discourseTree().isDefined()) {
            DiscourseTree tree = doc.discourseTree().get();
            System.out.println("Discourse tree:\n" + tree);
        }
    }
    public void debugInput(Document doc, PrintWriter out){
        int sentenceCount = 0;
        int predCount = 0;
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
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<>(sentence.dependencies().get());
                while (iterator.hasNext()) {
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                    out.println(" head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                }
            }
            if (sentence.semanticRoles().isDefined()) {
                out.println("Semantic dependencies:");
                DirectedGraphEdgeIterator<String> iterator = new DirectedGraphEdgeIterator<>(sentence.semanticRoles().get());
                
                while (iterator.hasNext()) {
                    scala.Tuple3<Object, Object, String> dep = iterator.next();
                    Predicate pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                    try{                      
                        if (pre.getIdx()-1 != (int)dep._1()){
                            predCount++;
                            pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                        }
                    } catch (IndexOutOfBoundsException e){
                        predCount--;
                        pre = (Predicate)predicates.get(sentenceCount).get(predCount);
                    }
                    // note that we use offsets starting at 0 (unlike CoreNLP, which uses offsets starting at 1)
                    out.println("pred: " + pre.getSense() + " head:" + dep._1() + " modifier:" + dep._2() + " label:" + dep._3());
                }

            }
            if (sentence.syntacticTree().isDefined()) {
                out.println("Constituent tree: " + sentence.syntacticTree().get());
                // see the org.clulab.struct.Tree class for more information
                // on syntactic trees, including access to head phrases/words
            }
            predCount =0;
            sentenceCount += 1;
            out.println("\n");
        }
        if (doc.coreferenceChains().isDefined()) {
            // these are scala.collection Iterator and Iterable (not Java!)
            scala.collection.Iterator<scala.collection.Iterable<CorefMention>> chains = doc.coreferenceChains().get().getChains().iterator();
            while (chains.hasNext()) {
                scala.collection.Iterator<CorefMention> chain = chains.next().iterator();
                out.println("Found one coreference chain containing the following mentions:");
                while (chain.hasNext()) {
                    CorefMention mention = chain.next();
                    // note that all these offsets start at 0 too
                    out.println("\tsentenceIndex:" + mention.sentenceIndex()
                            + " headIndex:" + mention.headIndex()
                            + " startTokenOffset:" + mention.startOffset()
                            + " endTokenOffset:" + mention.endOffset());
                }
            }
        }
        if (doc.discourseTree().isDefined()) {
            DiscourseTree tree = doc.discourseTree().get();
            out.println("Discourse tree:\n" + tree);
        }   
    }       
       
}
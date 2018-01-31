/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.memphis.iis.conllwebapp;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.clulab.processors.*;
import org.clulab.struct.*;
import org.slf4j.LoggerFactory;
import scala.collection.mutable.*;
import scala.io.Source;
//import Reader._
import org.clulab.serialization.*;

/**
 *
 * @author noah
 */
public class CoNLLReader {
    
    int argConflictCount = 0;
    int multiPredCount = 0;
    int argCount = 0;
    int predCount = 0;
    int tokenCount = 0;
    int sentCount = 0;
    int hyphCount = 0;
    
    class CoNLLToken{
        String word;
        // pos might be an int
        String pos;
        String lemma;
        int pred;
        Map<Integer, String> dep;
        String frameBits[];
        /* May or may not need this function based on the input of our program
        public String toString(String in){
            
        }
        */
    }
    
    //output is of Document type for now, likely will be changed before final version
    public List<List> readCoNLL(String filepath) throws FileNotFoundException, IOException{
        
        //open the file
        File file = new File(filepath);
        //Source magic = Source.fromFile(file, filepath);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        
         // create an array of all sentences of the document
            // this should be an array of arrays that contain CoNLLToken
        // create an array of of CoNLLToken that will represent each individual sentence
        
        List<CoNLLToken> sentence = new ArrayList<CoNLLToken>();
        List<List> sentences = new ArrayList<List>();
        
        //set all base values to 0
        argConflictCount = 0;
        multiPredCount = 0;
        argCount = 0;
        predCount = 0;
        tokenCount = 0;
        sentCount = 0;
        hyphCount = 0;
        
        // read all the sentences and collapse all the hyphens
        String line;
        while((line = br.readLine()) != null){
            line = line.trim();
            // if the line is not empty, tokenize the line and add it to sentence list
            if (line.length() > 0){
                String[] bits = line.split("\\t");
                assert(bits.length >= 11); //may need to do some additional testing
                CoNLLToken token = mkToken(bits);
                sentence.add(token);
                tokenCount += 1;
                if(token.pos.equals("HYPH")) hyphCount += 1; //this is messy, may or may not be what is intended
            }
            else{
                // collapse hyphens, add sentence to sentences list, reset sentence array
                sentences.add(collapseHyphens(sentence));
                sentence = new ArrayList<>();
                sentCount += 1;
            }
        }
        System.out.println("Read " + tokenCount + " tokens, grouped into " + sentCount + " sentenes.");
        System.out.println("Found " + hyphCount + " hyphens.");
        System.out.println("In hyphenated phrases, found " + multiPredCount + " multi predicates and " + argConflictCount + " argument conflicts.");
        
        return sentences;
        
        // construct semantic roles of CoNLL tokens
        
        //construct one Document for the entire corpus and annotate it
        
        // assing semantic roles to sentences in new Document
        
        // debug and return Document
    }
    
    /*
    public Document mkDocument(CoNLLToken[][] sentences, Processor proc){
        
    }*/
    
    // this is the function that may need to be edited a bit to work with our server
    // 
    //
    public CoNLLToken mkToken(String[] bits){
        
        CoNLLToken output = new CoNLLToken();
        output.word = bits[1];
        output.pos = bits[4]; // might be 3
        output.lemma = bits[2];
        
        // ---> FLAG <----
        //might run into a trainwreck here
        // this is a new feature, but i think it will work
        int head = Integer.parseInt(bits[8]);
        String depLabel = bits[10];
        output.dep.put(head, depLabel);
        
        // -----> FLAG THIS <------
        // what if there is no bits[13]
        // i think this will work, conll2009 reference matches
        try{
            String test = bits[13]; //10
            if(test.equals("_"))
                output.pred = 0;
            else
                output.pred = 1;
        }
        catch (IndexOutOfBoundsException e){
            System.out.println("Error processing predicate, indices out of bounds");
            output.pred = 0;
        }
        // take all members of bits that are [14] and up and put them in a new array
        // --> FLAG <---
        // this part may be problematic
        String[] tmp = Arrays.copyOfRange(bits, 14, bits.length);
        output.frameBits = tmp;
        
        return output;
    }
    
    // ------> BIG FLAG <-----------
    // does collapsing the hyphens give us the intended output?
    //
    // COME BACK LATER
    public List<CoNLLToken> collapseHyphens(List<CoNLLToken> origSentence){
        /*
        if (USE_CONLL_TOKENIZATION) return origSentence
        */
        List<CoNLLToken> output = new ArrayList<CoNLLToken>();
        int start = 0;
        while(start < origSentence.size()){
            int end = findEnd(origSentence, start);
            if (end > start + 1){
                CoNLLToken token = mergeTokens(origSentence, start, end);
                output.add(token);
            } else {
                output.add(origSentence.get(start));
            }
            start = end;
        }
        return output;
    }
    
    // find the end of the hyphenated phrase and return it
    public int findEnd(List<CoNLLToken> sent, int start){
        int end = start + 1;
        while (end < sent.size()){
            if (!sent.get(end).pos.equals("HYPH")) return end; // might run into problems here
            else end = end + 2;
        }
        return sent.size();
    }
    
    // take the start and the end of the hyphenated phrase and combine them into one token
    public CoNLLToken mergeTokens(List<CoNLLToken> sent, int start, int end){
        CoNLLToken output = new CoNLLToken();
        // make sure this is copying correctly
        List<CoNLLToken> phrase = sent.subList(start, end);
        int ending = phrase.size()-1; // ending index, might be incorrect here
        String word = phrase.get(0).word + phrase.get(ending).word;   
        String lemma = phrase.get(0).lemma + phrase.get(ending).lemma;
        int pred = mergePredicates(phrase);
        String frameBits[] = mergeFrames(phrase);
        
        output.word = word;
        output.pos = phrase.get(ending).pos;
        output.lemma = lemma;
        output.pred = pred;
        output.frameBits = frameBits;
        output.dep = sent.get(start).dep;
        
        return output;
    }
    
    public int mergePredicates(List<CoNLLToken> phrase){
        // might be wrong here
        int ending = phrase.size() -1;
        int total = phrase.get(0).pred + phrase.get(ending).pred;
        if (total > 1){
            System.out.println("Found MULTI PREDICATE in hyphenated phrase: " + phrase);
            multiPredCount += 1;
        }
        return total;
    }
    
    public String[] mergeFrames(List<CoNLLToken> phrase){
        String frameBits[] = new String[phrase.get(0).frameBits.length];
        for(int x = 0; x < frameBits.length; x++){
            frameBits[x] = mergeFrame(phrase, x);
        }
        return frameBits;
    }
    
    public String mergeFrame(List<CoNLLToken> phrase, int pos){
        String arg = "_";
        int count = 0;
        for(int x = phrase.size()-1; x >= 0; x--){
            if(!phrase.get(x).frameBits[pos].equals("_")){
                if(arg.equals("_")) arg = phrase.get(x).frameBits[pos];
                count += 1;
            }
        }
        if(count>1){
            argConflictCount += 1;
        }
        return arg;
    }
}

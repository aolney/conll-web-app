/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.memphis.iis.conllwebapp;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
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
    public Document readCoNLL(String filepath, Document doc) throws FileNotFoundException, IOException{
        
        //open the file
        File file = new File(filepath);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        
         // create an array of all sentences of the document
            // this should be an array of arrays that contain CoNLLToken
        // create an array of of CoNLLToken that will represent each individual sentence
        
        CoNLLToken sentence[];
        CoNLLToken sentences[][];
        
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
            if (line.length() > 0){
                String[] bits = line.split("\\t");
                assert(bits.length >= 11); //may need to do some additional testing
                
            }
        }
        
        // close the file and print some debug information
        
        // construct semantic roles of CoNLL tokens
        
        //construct one Document for the entire corpus and annotate it
        
        // assing semantic roles to sentences in new Document
        
        // debug and return Document
    }
    
    public Document mkDocument(CoNLLToken[][] sentences, Processor proc){
        
    }
    
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
    
}

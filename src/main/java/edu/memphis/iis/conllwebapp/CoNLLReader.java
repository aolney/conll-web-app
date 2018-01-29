/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.memphis.iis.conllwebapp;

import java.io.*;
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
    
    public Document readCoNLL(File file, Document doc){
        
        //open the file
        
        // create an array of all sentences of the document
            // this should be an array of arrays that contain CoNLLToken
        // create an array of of CoNLLToken that will represent each individual sentence
        
        //set all base values to 0
        
        // read all the sentences and collapse all the hyphens
        
        // close the file and print some debug information
        
        // construct semantic roles of CoNLL tokens
        
        //construct one Document for the entire corpus and annotate it
        
        // assing semantic roles to sentences in new Document
        
        // debug and return Document
    }
    
    public Document mkDocument(CoNLLToken[][] sentences, Processor proc){
        
    }
    
    
}

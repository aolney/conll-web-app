/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.memphis.iis.conllwebapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.clulab.processors.corenlp.CoreNLPProcessor;
import org.clulab.struct.CorefMention;
import org.clulab.struct.DirectedGraphEdgeIterator;
import org.clulab.processors.Document;
import org.clulab.processors.Processor;
import org.clulab.processors.Sentence;
import org.clulab.swirl2.Reader;
import static org.junit.Assert.*;
import scala.Console;

/**
 *
 * @author noah
 */
public class EchoServerJSONTest {
    
    public EchoServerJSONTest() throws Exception {

    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of start method, of class EchoServerJSON.
     
    @Test
    public void testStart() throws Exception {
        System.out.println("start");
        int port = 0;
        EchoServerJSON instance = new EchoServerJSON();
        instance.start(port);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of mateplusProcess method, of class EchoServerJSON.
     * @throws java.lang.Exception
     
    @Test
    public void testMateplusProcess() throws Exception {
        System.out.println("mateplusProcess Test");
        
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("test.txt").getFile());
        
        EchoServerJSON instance = new EchoServerJSON();
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line=br.readLine()) != null)
            sb.append(line);
        
        String inputLine = sb.toString();
        String output = instance.mateplusProcess(inputLine);
        
        System.out.println(output);
    }*/

    /**
     * Test of corenlpProcess method, of class EchoServerJSON.
     */
    @Test
    public void testCorenlpProcess() throws Exception {

        //instance.corenlpProcess(inputLine);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of main method, of class EchoServerJSON.
     
    @Test
    public void testMain() throws Exception {
        System.out.println("main");
        String[] args = null;
        EchoServerJSON.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.memphis.iis.conllwebapp;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author noah
 */
public class EchoServerJSONTest {
    
    public EchoServerJSONTest() {
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
     
    @Test
    public void testMateplusProcess() throws Exception {
        System.out.println("mateplusProcess");
        EchoServerJSON instance = new EchoServerJSON();
        String expResult = "";
        String result = instance.mateplusProcess();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of corenlpProcess method, of class EchoServerJSON.
     */
    @Test
    public void testCorenlpProcess() throws Exception {
        System.out.println("corenlpProcess test");
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("conll-txt.txt").getFile());
        EchoServerJSON instance = new EchoServerJSON();
        instance.start(4444);
        instance.corenlpProcess(file);
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

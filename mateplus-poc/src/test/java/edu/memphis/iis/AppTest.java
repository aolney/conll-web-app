package edu.memphis.iis;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit test for simple App.
 */
public class AppTest  extends TestCase {
    public AppTest(String testName) {
        super( testName );
    }

    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    // Attention: you probably need to tweak your IDE's memory settings to
    // get these tests to run (I use 2GB min and 4GB max).

    public void testSingle() throws Exception {
        MatePlusProcessor processor = new MatePlusProcessor();
        processor.initModels();

        CoNLL09MemoryWriter writer = new CoNLL09MemoryWriter();
        writer.debug = true;

        assertEquals(0, writer.getBuffer().size());
        writer.write(processor.parse("I would like to teach a student-taught course."));
        assertEquals(1, writer.getBuffer().size());
    }

    public void testBigFile() throws Exception {
        MatePlusProcessor processor = new MatePlusProcessor();
        processor.initModels();

        CoNLL09MemoryWriter writer = new CoNLL09MemoryWriter();
        writer.debug = true;

        List<Double> times = new ArrayList<Double>();

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("input.txt").getFile());
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        while ((line = br.readLine()) != null) {
            long startTime = System.nanoTime();
            writer.write(processor.parse(line));
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
    }
}

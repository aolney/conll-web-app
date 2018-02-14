package edu.memphis.iis;

import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.io.SentenceWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoNLL09MemoryWriter implements SentenceWriter {
    protected List<String> buffer = new ArrayList<String>();

    public boolean debug = false;

    public List<String> getBuffer() {
        return Collections.unmodifiableList(buffer);
    }

    public void clear() {
        buffer.clear();
    }

    protected String newItem(String s) {
        if (debug) {
            System.out.println(s);
            System.out.println("");
            System.out.flush();
        }
        return s;
    }

    @Override
    public void write(Sentence sentence) {
        buffer.add(newItem(sentence.toString()));
    }

    @Override
    public void close() {
        if (debug) {
            System.out.println("CoNLL09 Memory Writer CLOSE");
            System.out.flush();
        }
    }

    @Override
    public void specialwrite(Sentence sentence) {
        buffer.add(newItem(sentence.toSpecialString()));
    }
}

package edu.memphis.iis;

import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;
import is2.parser.Parser;
import is2.tag.Tagger;
import is2.util.DB;
import org.apache.commons.io.IOUtils;
import se.lth.cs.srl.CompletePipeline;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.io.CoNLL09Writer;
import se.lth.cs.srl.io.SentenceWriter;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;
import se.lth.cs.srl.options.FullPipelineOptions;
import se.lth.cs.srl.options.ParseOptions;
import se.lth.cs.srl.pipeline.Reranker;
import se.lth.cs.srl.preprocessor.PipelinedPreprocessor;
import se.lth.cs.srl.preprocessor.Preprocessor;
import se.lth.cs.srl.preprocessor.tokenization.StanfordPTBTokenizer;
import se.lth.cs.srl.preprocessor.tokenization.Tokenizer;
import se.lth.cs.srl.preprocessor.tokenization.WhiteSpaceTokenizer;
import se.lth.cs.srl.util.BohnetHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class MatePlusProcessor {
    protected CompletePipeline pipeline;

    public MatePlusProcessor() {
        Language.setLanguage(Language.L.eng);
    }

    public void initModels() throws IOException {
        File lemmaModel = extractMateModel("/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model");
        File parserModel = extractMateModel("/CoNLL2009-ST-English-ALL.anna-3.3.parser.model");
        File taggerModel = extractMateModel("/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model");
        File srlModel = extractMateModel("/srl-EMNLP14+fs-eng.model");

        CompletePipelineCMDLineOptions parseOptions = new CompletePipelineCMDLineOptions();
        parseOptions.lemmatizer = lemmaModel;
        parseOptions.parser = parserModel;
        parseOptions.tagger = taggerModel;
        parseOptions.reranker = true;
        parseOptions.srl = srlModel;
        parseOptions.loadPreprocessorWithTokenizer = true;
        parseOptions.skipPI = false;
        parseOptions.desegment = false;

        try {
            pipeline = CompletePipeline.getCompletePipeline(parseOptions);
        }
        catch(ClassNotFoundException e) {
            throw new IOException("Invalid model file used for MATE+", e);
        }
    }

    public Sentence parse(String one) {
        try {
            return pipeline.parse(one);
        }
        catch(Exception e) {
            throw new RuntimeException("MATE+ pipelined parser failed", e);
        }
    }

    // Extract the model specified from the mate-models package.
    //
    // We use the warning class from that package in case we are running in an
    // environment with multiple class loaders (like some web app servers).
    // Since we're using class.getResourceAsStream, all resource paths should
    // begin with a forward slash.
    //
    // See defaultRun for example usage
    protected File extractMateModel(String resourcePath) throws IOException {
        // Figure out temp file name - if it's already there just return it
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        File tempFile = new File(baseDir, "mate-" + resourcePath.replaceAll("/", "_") + ".model");
        if (tempFile.exists() && tempFile.length() > 0) {
            DB.println("Using pre-existing model file: " + tempFile.getCanonicalPath());
            return tempFile;
        }

        // Start reading resource
        InputStream readStream = edu.memphis.iis.warning.class.getResourceAsStream(resourcePath);
        if (readStream == null) {
            throw new IOException("Resource-based MATE Model cannot be found: " + resourcePath);
        }

        // Write to the temp file
        DB.println("Writing model file to: " + tempFile.getCanonicalPath());
        FileOutputStream writeStream = new FileOutputStream(tempFile);
        try {
            IOUtils.copy(readStream, writeStream);
        }
        finally {
            writeStream.close();
        }

        // All done
        return tempFile;
    }
}

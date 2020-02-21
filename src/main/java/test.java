import info.debatty.java.lsh.LSHSuperBit;
import io.anserini.index.IndexReaderUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.FSDirectory;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;


public class test {

    public static void main(String[] args) throws IOException, ParseException {


        /*

        StructuredReranker.reduce_result_file("C:\\resources\\constrained_phrases\\0.55_structured_eval.txt",
                "C:\\resources\\constrained_phrases\\measures\\0.55_structured_eval_P@10.txt", "P_10");
         */

        EmbeddingSpace space = new EmbeddingSpace("C:\\resources\\small_words.txt");


        TranslationMatrix testMatrix = TranslationMatrix.read("C:\\resources\\matrices\\similarity_matrix_0.55");


        String INDEX_DIR = "C:\\resources\\index-robust04-20191213";


        IndexReader anseriniReader = IndexReaderUtils.getReader(INDEX_DIR);

        SortedMap<Integer, Map<String, String>> topics = StructuredReranker.get_topic_map("C:\\resources\\topics.robust04.txt");

        SortedMap<Integer, Map<String, String>> structuredTopics =
                StructuredReranker.get_topic_map("C:\\resources\\structured_topics.robust04.txt");


        Analyzer analyzer = new StandardAnalyzer();
        LSHSuperBit lsh = new LSHSuperBit(2, 8, 100);

        String basicOutputPath = "C:\\resources\\constrained_phrases\\0.55_basic_results.txt";
        String languageOutputPath = "C:\\resources\\constrained_phrases\\0.55_language_results.txt";
        String structuredOutputPath = "C:\\resources\\constrained_phrases\\0.55_structured_results.txt";

        String inputPath = "C:\\resources\\run.robust04.bm25.txt";
        StructuredReranker.generate_scores(anseriniReader, testMatrix, lsh, topics, structuredTopics,
                inputPath, basicOutputPath, languageOutputPath, structuredOutputPath,
                analyzer, space, "raw", StructuredDocumentScorer.CalculationType.COSINE,
                2500.0, 0.7, 0.0, 0.55);
    }
}

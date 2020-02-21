import info.debatty.java.lsh.LSHSuperBit;
import io.anserini.index.IndexReaderUtils;
import io.anserini.rerank.Result;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SimpleSearcher;
import io.anserini.search.topicreader.TrecTopicReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import java.io.*;
import java.nio.file.Paths;
import java.util.Map;
import java.util.SortedMap;


public class StructuredReranker {

    TranslationMatrix translations;
    EmbeddingSpace space;
    LSHSuperBit lsh;
    StructuredDocumentScorer scorer;
    Analyzer analyzer;
    Double mu;
    Double alpha;
    Double beta;
    IndexReader indexReader;
    int corpusSize;
    Double tolerance;


    public StructuredReranker(IndexReader indexReader,
                              TranslationMatrix translations,
                              EmbeddingSpace space,
                              LSHSuperBit lsh,
                              Analyzer analyzer,
                              Double mu,
                              Double alpha,
                              Double beta,
                              Double tolerance) throws IOException, ParseException {

        this.translations = translations;
        this.space = space;
        this.lsh = lsh;
        this.analyzer = analyzer;

        this.mu = mu;
        this.alpha = alpha;
        this.beta = beta;

        this.indexReader = indexReader;
        corpusSize = get_corpus_size(indexReader);
        this.tolerance = tolerance;


        this.scorer = new StructuredDocumentScorer(indexReader, translations,
                space, lsh, mu, alpha, beta, corpusSize, tolerance);
    }

    /**
     * Calculate and return the number of words across the entire index
     * @param indexReader the index we will observe
     * @return total number of words over the given index
     * @throws IOException
     * @throws ParseException
     */
    public static int get_corpus_size(IndexReader indexReader) throws IOException, ParseException {

        /*
        int result = 0;

        Iterator termIterator = IndexReaderUtils.getTerms(indexReader);
        IndexReaderUtils.IndexTerm nextTerm;
        Map<String, Long> termCounts;

        while(termIterator.hasNext()) {

            nextTerm = (IndexReaderUtils.IndexTerm) termIterator.next();

            try {

                termCounts = IndexReaderUtils.getTermCounts(indexReader, nextTerm.getTerm());
                result += termCounts.get("collectionFreq");

            } catch(Exception e) {

            }

        }


         */ // UNCOMMENT !!!!
        return 240634932;
    }
    public SimpleSearcher.Result[] rerank_simple_results_basic_dirichlet(
            StructuredQuery query,
            SimpleSearcher.Result[] results,
            StructuredDocumentScorer.CalculationType type,
            String docFieldName,
            Analyzer analyzer) throws IOException, ParseException {


        // re-score each document with structured query
        for(int resultIndex = 0; resultIndex < results.length;
            resultIndex++) {

            // set current document within our scorer

            scorer.set_document(indexReader.document(results[resultIndex].ldocid),
                    docFieldName, analyzer);

            results[resultIndex].score = scorer.score_document_text_basic_dirichlet(query, results[resultIndex].content,
                    analyzer, type).floatValue();


        }

        return results;

    }

    /**
     * Rerank results with dirilet + translation language model
     * @param query the query we will score documents against
     * @param results array of results we will be reranking
     * @param type type of calculation we will do
     * @param docFieldName name of field we will be searching
     * @param analyzer lucene/anserini analyzer we will be using
     * @return array of results reranked accordingly
     * @throws IOException
     * @throws ParseException
     */
    public SimpleSearcher.Result[] rerank_simple_results_language_dirichlet(
            StructuredQuery query,
            SimpleSearcher.Result[] results,
            StructuredDocumentScorer.CalculationType type,
            String docFieldName,
            Analyzer analyzer) throws IOException, ParseException {


        // re-score each document with structured query
        for(int resultIndex = 0; resultIndex < results.length;
            resultIndex++) {

            // set current document within our scorer

            scorer.set_document(indexReader.document(results[resultIndex].ldocid),
                    docFieldName, analyzer);

            results[resultIndex].score = scorer.score_document_text_dirichlet(query, results[resultIndex].content,
                    analyzer, type).floatValue();
        }

        return results;

    }

    public SimpleSearcher.Result[] rerank_simple_results(StructuredQuery query,
                                                         SimpleSearcher.Result[] results,
                                                         StructuredDocumentScorer.CalculationType type,
                                                         String docFieldName,
                                                         Analyzer analyzer) throws IOException, ParseException {



        // re-score each document with structured query
        for(int resultIndex = 0; resultIndex < results.length;
            resultIndex++) {

            // set current document within our scorer

            scorer.set_document(indexReader.document(results[resultIndex].ldocid),
                    docFieldName, analyzer);

            results[resultIndex].score = scorer.score_document_text(query, results[resultIndex].content,
                    analyzer, type).floatValue();
        }

        return results;
    }

    public Result[] rerank_results_full(StructuredQuery query,
                                                       Result[] results,
                                                       StructuredDocumentScorer.CalculationType type) throws IOException, ParseException {

        Document currentDoc;

        // re-score each document with structured query
        for(int resultIndex = 0; resultIndex < results.length;
            resultIndex++) {

            currentDoc = results[resultIndex].document;
            results[resultIndex].score = scorer.score_document_full(query, currentDoc,
                    analyzer, type).floatValue();
        }

        return results;
    }

    public Result[] rerank_results_field(StructuredQuery query,
                                               Result[] results,
                                               StructuredDocumentScorer.CalculationType type,
                                               String fieldName) throws IOException, ParseException {

        Document currentDoc;

        // re-score each document with structured query
        for(int resultIndex = 0; resultIndex < results.length;
            resultIndex++) {

            currentDoc = results[resultIndex].document;
            results[resultIndex].score = scorer.score_document_field(query, fieldName,
                    currentDoc, analyzer, type).floatValue();
        }

        return results;
    }

    /**
     *
     * @param scoredDocs
     * @param type
     * @return
     */
    public ScoredDocuments rerank_scored_full(StructuredQuery query,
                                       ScoredDocuments scoredDocs,
                                  StructuredDocumentScorer.CalculationType type) throws IOException, ParseException {

        Document currentDoc;

        // re-score each document with structured query
        for(int documentIndex = 0; documentIndex < scoredDocs.documents.length;
            documentIndex++) {

            currentDoc = scoredDocs.documents[documentIndex];
            scoredDocs.scores[documentIndex] = scorer.score_document_full(query, currentDoc,
                    analyzer, type).floatValue();
        }

        return scoredDocs;
    }

    public ScoredDocuments rerank_scored_field(StructuredQuery query,
                                        ScoredDocuments scoredDocs,
                                        StructuredDocumentScorer.CalculationType type,
                                        String fieldName) throws IOException, ParseException {

        Document currentDoc;

        // re-score each document with structured query
        for(int documentIndex = 0; documentIndex < scoredDocs.documents.length;
            documentIndex++) {

            currentDoc = scoredDocs.documents[documentIndex];
            scoredDocs.scores[documentIndex] = scorer.score_document_field(query, fieldName,
                    currentDoc, analyzer, type).floatValue();
        }


        return scoredDocs;
    }


    /**
     * Generates query result files for experimentation.
     * At basicDiriletPath, languageDiriletPath and structuredDiriletPath,
     * text files will be created that give the query scores in the format of a given
     * file at inputPath. An example of such an input file is run.robust04.bm25.txt.
     * The output files will be the same as the input file, but with the document scores
     * recalculated with the appropriate method.
     * @param indexReader
     * @param translationMatrix
     * @param lsh
     * @param topicReaderMap
     * @param structuredTopicReaderMap
     * @param inputPath
     * @param basicDiriletOutputPath
     * @param languageDiriletOutputPath
     * @param structuredDiriletOutputPath
     * @param analyzer
     * @param space
     * @param fieldName
     * @param type the type of calculation we will do (COSINE, HASH or ALL_HASH
     * @param mu
     * @param alpha
     * @param beta
     * @param tolerance tolerance level of calculation for similarities
     * @throws IOException
     * @throws ParseException
     */
    public static void generate_scores(IndexReader indexReader,
                                       TranslationMatrix translationMatrix,
                                       LSHSuperBit lsh,
                                       SortedMap<Integer, Map<String, String>> topicReaderMap,
                                       SortedMap<Integer, Map<String, String>> structuredTopicReaderMap,
                                       String inputPath,
                                       String basicDiriletOutputPath,
                                       String languageDiriletOutputPath,
                                       String structuredDiriletOutputPath,
                                       Analyzer analyzer,
                                       EmbeddingSpace space,
                                       String fieldName,
                                       StructuredDocumentScorer.CalculationType type,
                                       Double mu,
                                       Double alpha,
                                       Double beta,
                                       Double tolerance) throws IOException, ParseException {



        // read each line of our file and as we do,
        // perform relevant searches and write to new file


        // Note: format of each line is
        // qid  iter  docno  rank  sim  run_id
        BufferedReader br = new BufferedReader(new FileReader(inputPath));
        String line;

        // prepare to write to new files
        PrintWriter basicWriter = new PrintWriter(basicDiriletOutputPath, "UTF-8");
        PrintWriter languageWriter = new PrintWriter(languageDiriletOutputPath, "UTF-8");
        PrintWriter structuredWriter = new PrintWriter(structuredDiriletOutputPath, "UTF-8");

        while((line = br.readLine()) != null) {

            // split line by space
            String[] splited = line.split("\\s+");

            // obtain the topic number
            Integer topicNumber = Integer.parseInt(splited[0]);

            // obtain the trec ID and then the index ID and the document concerned
            String trecDocID = splited[2];
            Integer indexDocID = IndexReaderUtils.convertDocidToLuceneDocid(indexReader, trecDocID);
            Document document = indexReader.document(indexDocID);

            // obtain our new query text
            // we will only be using title in this case
            String queryText = topicReaderMap.get(topicNumber).get("title");

            String structuredQueryText = structuredTopicReaderMap.get(topicNumber).get("title");

            // construct structured query from given text
            StructuredQueryGenerator generator = new StructuredQueryGenerator(space);

            // if null pointer exception occurs, we must use old scores
            StructuredQuery query = null;
            StructuredQuery structuredQuery = null;

            Boolean wordNotFound = false;
            try {
                query = generator.buildQuery(fieldName, analyzer, queryText);

                structuredQuery = generator.buildQuery(fieldName, analyzer, structuredQueryText);
            } catch (NullPointerException e) {

                wordNotFound = true;
            }

            String[] basicSplited = splited.clone();
            String[] languageSplited = splited.clone();
            String[] structuredSplited = splited.clone();

            // if a query contains a word that is not found,
            // we must use the old score since we cannot perform
            // operations on it that involve a word vector in any way
            // since we have no vector for it
            if(!wordNotFound) {
                // construct document scorer
                StructuredDocumentScorer scorer = new StructuredDocumentScorer(indexReader,
                        translationMatrix, space, lsh, mu, alpha, beta, get_corpus_size(indexReader), tolerance);

                // set the current document
                scorer.set_document(indexReader.document(indexDocID), fieldName, analyzer);

                Double basicDiriletScore = scorer.score_document_text_basic_dirichlet(query,
                        document.get(fieldName), analyzer, type);


                basicSplited[4] = basicDiriletScore.toString();

                Double languageDiriletScore = scorer.score_document_text_dirichlet(query,
                        document.get(fieldName), analyzer, type);

                languageSplited[4] = languageDiriletScore.toString();


                Double structuredDiriletScore = scorer.score_document_text_dirichlet(structuredQuery,
                        document.get(fieldName), analyzer, type);


                structuredSplited[4] = structuredDiriletScore.toString();

            }

            // writer new scores to new results document
            write_splited_line(basicSplited, basicWriter, " ");
            write_splited_line(languageSplited, languageWriter, " ");
            write_splited_line(structuredSplited, structuredWriter, " ");
        }

        // close print writers
        basicWriter.close();
        languageWriter.close();
        structuredWriter.close();

        br.close();
    }

    /**
     * Will write a line to the given PrintWriter, composed of each string
     * in the `splited` array, seperated by the input delimeter
     * @param splited
     * @param writer
     * @param delimiter
     */
    public static void write_splited_line(String[] splited, PrintWriter writer, String delimiter) {

        // create string seperated by delimeter
        String writeString = "";
        for(String item : splited) {

            writeString = writeString + item + delimiter;
        }

        // remove extra end delimeter
        writeString = writeString.substring(0, writeString.length() - delimiter.length());

        // write to file
        writer.println(writeString);
    }

    public static SortedMap<Integer, Map<String, String>> get_topic_map(String topicPath) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(topicPath));
        TrecTopicReader topicReader = new TrecTopicReader(Paths.get(topicPath));
        return topicReader.read(bufferedReader);
    }

    /**
     * inputPath should be a path to the output of anserini evaluation. Written to outputPath
     * will be the same input file, but with only lines preserved with the given resultType.
     * If for example 'P_10' is the input resultType, the output file will contain all the P_10
     * from the input file.
     * @param inputPath
     * @param outputPath
     * @param resultType
     * @throws IOException
     */
    public static void reduce_result_file(String inputPath, String outputPath,
                                          String resultType) throws IOException {

        PrintWriter writer = new PrintWriter(outputPath, "UTF-8");

        BufferedReader br = new BufferedReader(new FileReader(inputPath));
        String line;

        while((line = br.readLine()) != null) {

            String[] splited = line.split("\\s+");
            if(splited[0].compareTo(resultType) == 0) {

                // we want to save this line
                writer.println(line);
            }

        }

        writer.close();
        br.close();
    }

    /**
     * args[0] : path to translation matrix
     * args[1] : path to index
     * args[2] : path to embedding space
     * args[3] : path to topics
     *
     * args[4] : basic results output
     * args[5] : TLM results output
     * args[6] : structured results output
     *
     * args[7] : input results file (from anserini search results)
     *
     *
     */
    public static void main(String[] args) throws IOException, ParseException {

        EmbeddingSpace space = new EmbeddingSpace(args[2]);

        TranslationMatrix testMatrix = TranslationMatrix.read(args[0]);

        IndexReader anseriniReader = IndexReaderUtils.getReader(args[1]);

        SortedMap<Integer, Map<String, String>> topics = StructuredReranker.get_topic_map(args[3]);

        SortedMap<Integer, Map<String, String>> structuredTopics =
                StructuredReranker.get_topic_map(args[3]);


        Analyzer analyzer = new StandardAnalyzer();
        LSHSuperBit lsh = new LSHSuperBit(2, 8, 100);

        String basicOutputPath = args[4];
        String languageOutputPath = args[5];
        String structuredOutputPath = args[6];

        String inputPath = args[7];
        StructuredReranker.generate_scores(anseriniReader, testMatrix, lsh, topics, structuredTopics,
                inputPath, basicOutputPath, languageOutputPath, structuredOutputPath,
                analyzer, space, "raw", StructuredDocumentScorer.CalculationType.COSINE,
                2500.0, 0.7, 0.0, 0.55);
    }
}

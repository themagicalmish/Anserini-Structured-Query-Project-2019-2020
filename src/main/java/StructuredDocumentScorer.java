import info.debatty.java.lsh.LSHSuperBit;
import io.anserini.analysis.AnalyzerUtils;
import io.anserini.index.IndexReaderUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import java.io.IOException;
import java.util.List;


public class StructuredDocumentScorer {

    /**
     * Different types of calculations we can do,
     *
     * HASH: use locality sensitive hashing for calculating similarities
     * not stored in our translation matrix
     *
     * COSINE: use cosine similarity for calculating similarities
     * not stored in our translation matrix
     *
     * HASH_ALL: use locality sensitive hashing for calculating all similarities...
     * even those for which we have an entry in our translation matrix
     */
    public enum CalculationType  {
        HASH, COSINE, HASH_ALL
    }

    IndexReader indexReader;
    TranslationMatrix translations;
    EmbeddingSpace embeddingSpace;
    LSHSuperBit lsh;
    Double mu;
    Double alpha;
    Double beta;
    Document currentDoc;
    String currentFieldName;
    DocumentStatistic currentStats;
    Integer corpusSize;
    Double tolerance;
    Integer counter = 0;


    public StructuredDocumentScorer(IndexReader indexReader,
                                    TranslationMatrix translationMatrix,
                                    EmbeddingSpace embeddingSpace,
                                    LSHSuperBit lsh,
                                    Double mu,
                                    Double alpha,
                                    Double beta,
                                    Integer corpusSize,
                                    Double tolerance) {

        set_index_reader(indexReader);
        set_lsh(lsh);
        set_translations(translationMatrix);
        set_embeddings(embeddingSpace);
        set_mu(mu);
        set_alpha(alpha);
        set_beta(beta);

        this.corpusSize = corpusSize;
        this.tolerance = tolerance;
    }


    /**
     * Scores a document based on the entire document as a string
     * @param query
     * @param document
     * @param analyzer
     * @param type
     * @return the document's score
     * @throws IOException
     * @throws ParseException
     */
    public Double score_document_full(StructuredQuery query,
                                      Document document,
                                      Analyzer analyzer,
                                      CalculationType type) throws IOException, ParseException {

        return score_document_text(query, document.toString(), analyzer, type);
    }

    /**
     * Scores a document based on the text of one of its fields
     * @param query
     * @param fieldName
     * @param document
     * @param analyzer
     * @param type
     * @return the document's score
     * @throws IOException
     * @throws ParseException
     */
    public Double score_document_field(StructuredQuery query,
                                       String fieldName,
                                       Document document,
                                       Analyzer analyzer,
                                       CalculationType type) throws IOException, ParseException {

        String text = document.getField(fieldName).stringValue();
        return score_document_text(query, text, analyzer, type);
    }

    /**
     * Calculates and returns the document's score (similarity) against
     * the query with dirichlet smoothing applied to the non-language
     * model score.
     * @param query the query we will score text against
     * @param text text we will score query against
     * @param analyzer analyzer we will process text with
     * @param type type of similarity scores we will use
     * @return the document's score against the given query
     * @throws IOException
     * @throws ParseException
     */
    public Double score_document_text_basic_dirichlet(StructuredQuery query,
                                                      String text,
                                                      Analyzer analyzer,
                                                      CalculationType type) throws IOException, ParseException {

        Double result = 0.0;

        // we must score each part of the query against our chosen field

        Double documentProbability;


        // score regular terms
        for(QueryObject queryObject : query.basic) {


            Double corpusDensity = get_object_corpus_density(queryObject);

            documentProbability = (currentStats.get_count(queryObject.word).doubleValue()
                    + mu * get_object_corpus_density(queryObject))
                    / (currentStats.get_word_count() + mu);

            result += Math.log((documentProbability + beta) / (alpha *
                    corpusDensity));
        }

        // not adding phrases since basic query does not take phrasing into account
        result += Math.log(alpha) * (query.basic.size());
        return result;
    }


    /**
     * Calculates and returns the document's score (similarity) against
     * the query with dirichlet smoothing applied to the language
     * model score.
     * @param query the query we will score text against
     * @param text text we will score query against
     * @param analyzer analyzer we will process text with
     * @param type type of similarity scores we will use
     * @return the document's score against the given query
     * @throws IOException
     * @throws ParseException
     */
    public Double score_document_text_dirichlet(StructuredQuery query,
                                                String text,
                                                Analyzer analyzer,
                                                CalculationType type) throws IOException, ParseException {

        // obtain document size
        Double docSize = currentStats.get_word_count().doubleValue();

        Double result = 0.0;

        // we must score each part of the query against our chosen field

        // obtain desired document field, tokenized
        List<String> tokens = AnalyzerUtils.tokenize(analyzer, text);

        Double documentProbability;

        // score phrases
        for(Phrase phrase : query.phrases) {

            documentProbability = 0.0;

            // score each phrase against each term in our field
            for(String term : tokens) {

                documentProbability += get_phrase_similarity(term, phrase, type) *
                        get_word_document_density(currentDoc, term);

            }

            // apply smoothing

            Double corpusDensity = get_phrase_corpus_density(phrase);

            documentProbability = (Math.sqrt(documentProbability * docSize) + mu * corpusDensity)
                    / (currentStats.get_word_count() + mu);

            result += Math.log((documentProbability + beta) / (alpha *
                    corpusDensity));

        }

        // score regular terms
        for(QueryObject queryObject : query.basic) {

            documentProbability = 0.0;
            // score each object against each term in our field

            for(String term : tokens) {

                documentProbability += get_object_similarity(term, queryObject, type) *
                        get_word_document_density(currentDoc, term);
            }

            // apply smoothing

            Double corpusDensity = get_object_corpus_density(queryObject);

            documentProbability = (Math.sqrt(documentProbability * docSize) + mu * corpusDensity)
                    / (currentStats.get_word_count() + mu);


            result += Math.log((documentProbability + beta) / (alpha *
                    corpusDensity));

        }

        result += Math.log(alpha) * (query.phrases.size() + query.basic.size());


        return result;
    }


    /**
     * Calculates and returns the score of some given text
     * against a given StructuredQuery
     * @param query the query we will test
     * @param text what we will score our query against
     * @param analyzer the lucene analyzer we will use
     * @param type the type of calculation we will do -
     *             HASH uses LSH hash to compare new vectors
     *             COSINE uses cosine similarity to compare new vectors
     *             HASH_ALL uses hash to compare all vectors (not just new ones)
     * @return the calculated score of this query on this document
     */
    public Double score_document_text(StructuredQuery query,
                                       String text,
                                       Analyzer analyzer,
                                       CalculationType type) throws IOException, ParseException {

        Double result = 0.0;


        // we must score each part of the query against our chosen field

        // obtain desired document field, tokenized
        List<String> tokens = AnalyzerUtils.tokenize(analyzer, text);

        Double documentProbability = 0.0;

        // score phrases
        for(Phrase phrase : query.phrases) {

            documentProbability = 0.0;

            // score each phrase against each term in our field
            for(String term : tokens) {


                documentProbability += get_phrase_similarity(term, phrase, type) *
                        get_word_document_density(currentDoc, term);

            }

            result += Math.log((Math.sqrt(documentProbability) + beta) / (alpha *
                    get_phrase_corpus_density(phrase)));
        }

        // score regular terms
        for(QueryObject queryObject : query.basic) {

            documentProbability = 0.0;
            // score each object against each term in our field
            for(String term : tokens) {

                documentProbability += get_object_similarity(term, queryObject, type) *
                        get_word_document_density(currentDoc, term);
            }

            result += Math.log((Math.sqrt(documentProbability) + beta) / (alpha *
                    get_object_corpus_density(queryObject)));

        }


        result += Math.log(alpha) * (query.phrases.size() + query.basic.size());
        return result;
    }

    /**
     * Calculates and returns the similarity between a QueryObject
     * and a term (word). If the given CalculationType (type) is
     * HASH_ALL, will use hash to calculate. Otherwise, the similarity
     * will be obtain from the translation matrix if not negated, and if
     * the query object is negated, it will use cosine similarity
     * if type is COSINE and LSH hash if type is HASH
     * @param term the word we will compare our query term to
     * @param queryObject contains the term form a query
     * @param type the type of calculation we will be doing
     * @return the similarity between the queryObject and the term
     */
    public Double get_object_similarity(String term, QueryObject queryObject,
                                        CalculationType type) {

        Double result = 0.0;

        // first check if we have vector for term in our embeddingSpace
        if(embeddingSpace.get_vector(term) == null) {

            return 0.0;
        }

        WordVector termVector = embeddingSpace.get_vector(term);
        WordVector queryVector =  queryObject.get_vector();
        // if type is HASH_ALL, calculate with hash
        if(type == CalculationType.HASH_ALL) {

            result = WordVector.compare_hash(termVector, queryVector, lsh);
        } else {

            // if negated, use calculate with relevant type
            // otherwise, get similarity from translation matrix
            if(queryObject.isNegated()) {

                if(type == CalculationType.HASH) {

                    // use locality sensitive hash
                    result = WordVector.compare_hash(termVector, queryVector, lsh);
                } else if(type == CalculationType.COSINE) {

                    // use cosine similarity
                    result = WordVector.cosine_similarity(termVector, queryVector, tolerance);
                }
            } else {

                // not negated, get similarity from matrix
                result = translations.get_similarity(queryObject.getWord(), term);
            }
        }

        return result;
    }

    /**
     * Calculates and returns the similarity between a given
     * phrase and a given fieldTerm. If given CalculationType
     * is HASH, similarity will be calculated with locality
     * sensitive hash. If given Calculation type is COSINE,
     * similarity will be calculated with cosine similarity.
     * @param fieldTerm word from a document
     * @param phrase phrase from a query
     * @param type the type of calculation we will perform
     * @return the similarity of the phrase and the fieldTerm
     */
    public Double get_phrase_similarity(String fieldTerm, Phrase phrase,
                                        CalculationType type) {

        Double result = 0.0;

        // get vector representation of phrase
        WordVector phraseVector = phrase.get_vector();

        // first check if we have vector for term in our embeddingSpace
        if(embeddingSpace.get_vector(fieldTerm) == null) {

            return 0.0;
        }

        // get vector representation of fieldTerm
        WordVector termVector = embeddingSpace.get_vector(fieldTerm);

        if(type == CalculationType.HASH) {

            // compare via Hash with given LSHSuperBit
            result = WordVector.compare_hash(phraseVector, termVector, lsh);
        } else if (type == CalculationType.COSINE) {

            // compare with cosine similarity
            result = WordVector.cosine_similarity(phraseVector, termVector, tolerance);
        }

        return result;
    }

    
    public void set_lsh(LSHSuperBit lsh) {

        this.lsh = lsh;
    }

    public void set_translations(TranslationMatrix translations) {

        this.translations = translations;
    }

    public void set_embeddings(EmbeddingSpace embeddingSpace) {

        this.embeddingSpace = embeddingSpace;
    }

    public void set_mu(Double mu) {

        this.mu = mu;
    }

    public void set_index_reader(IndexReader indexReader) {

        this.indexReader = indexReader;
    }

    /**
     * Sets the alpha constant to the given double
     * @param alpha
     */
    public void set_alpha(Double alpha) {

        this.alpha = alpha;
    }

    /**
     *
     * @param document
     * @param word
     * @return the density of the given word in the current document
     * (that is p(word | document))
     */
    public Double get_word_document_density(Document document, String word) {

        return currentStats.get_count(word).doubleValue() / currentStats.get_word_count().doubleValue();
    }

    /**
     * Sets the current document we will use to score queries against
     * @param document
     * @param fieldName
     * @param analyzer
     * @throws IOException
     */
    public void set_document(Document document, String fieldName, Analyzer analyzer) throws IOException {


        currentDoc = document;
        currentStats = new DocumentStatistic(document, fieldName);
        currentFieldName = fieldName;
        currentStats.process_counts(analyzer);
    }

    /**
     *
     * @param word
     * @return the density of the given string in the corpus.
     * The density is the number of occurrences of the word in the
     * corpus divided by the total number of terms in the corpus.
     * @throws IOException
     * @throws ParseException
     */
    public double get_corpus_word_density(String word) throws IOException, ParseException {

        Long collectionFreq = IndexReaderUtils.getTermCounts(indexReader, word).get("collectionFreq");
        return (collectionFreq).doubleValue()  / corpusSize.doubleValue();
    }

    /**
     *
     * @param phrase
     * @return the corpus density for this phrase (that is, p(phrase | corpus))
     * this is calculated by returning the minimum density for all objects
     * within this phrase.
     * @throws IOException
     * @throws ParseException
     */
    public Double get_phrase_corpus_density(Phrase phrase) throws IOException, ParseException {

        Double result = Double.MAX_VALUE;

        for(QueryObject object : phrase.content) {

            result = Double.min(result, get_object_corpus_density(object));
        }

        return result;
    }

    /**
     *
     * @param object
     * @return returns the corpus density of the given query object.
     * The corpus density is the number of occurences of the object in the corpus
     * divided by the total number of terms in the corpus.
     * @throws IOException
     * @throws ParseException
     */
    public Double get_object_corpus_density(QueryObject object) throws IOException, ParseException {

        return get_corpus_word_density(object.word);
    }

    /**
     * Sets the beta constant to the given double.
     * This is mainly for debug purposes.
     * @param beta given beta
     */
    public void set_beta(Double beta) {

        this.beta = beta;
    }
}

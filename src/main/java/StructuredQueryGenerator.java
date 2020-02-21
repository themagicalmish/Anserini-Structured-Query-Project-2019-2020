import io.anserini.analysis.AnalyzerUtils;
import io.anserini.search.query.QueryGenerator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanQuery;
import java.util.ArrayList;
import java.util.List;

public class StructuredQueryGenerator extends QueryGenerator {

    ArrayList<Phrase> phrases = new ArrayList<>();
    ArrayList<QueryObject> basic = new ArrayList<>();
    EmbeddingSpace embeddingSpace;

    public StructuredQueryGenerator(EmbeddingSpace embeddingSpace) {

        this.embeddingSpace = embeddingSpace;
    }
    /**
     * Replaces `phrases` and `basic` member variables with original
     * content after going through with given analyzers
     * @param analyzer the analyzer we will subject our text to
     */
    public void filter_words(Analyzer analyzer) {

        ArrayList<Phrase> newPhrases = new ArrayList<>();
        ArrayList<QueryObject> newBasic = new ArrayList<>();

        Phrase newPhrase;
        QueryObject newQueryObject;

        String filteredWord;

        // filter each phrase through given analyzer
        for(int phraseIndex = 0; phraseIndex < phrases.size();
            phraseIndex++) {

            // get current phrase
            Phrase currentPhrase = phrases.get(phraseIndex);

            // prepare a new phrase
            newPhrase = new Phrase(currentPhrase.isNegated());

            // put each element of our phrase through our given analyzer
            ArrayList<QueryObject> phraseContent = currentPhrase.getContent();

            for(int phraseWordIndex = 0; phraseWordIndex < currentPhrase.size;
                phraseWordIndex++) {

                filteredWord = get_analyzed_string(analyzer, phraseContent.get(phraseWordIndex).word);

                // if our filtered word is not an empty string,
                // add it back to our phrase
                if(filteredWord.length() != 0) {

                    // create QueryObject to store it and add to our phrase
                    newQueryObject = new QueryObject(filteredWord, phraseContent.get(phraseWordIndex).isNegated());
                    newPhrase.addQueryObject(newQueryObject);
                }
            }

            // if the new phrase is not empty, add it to our phrases
            newPhrases.add(newPhrase);
        }

        // filter each basic word through given analyzer
        for(int wordIndex = 0; wordIndex < basic.size();
            wordIndex++) {


            filteredWord = get_analyzed_string(analyzer, basic.get(wordIndex).word);

            if(filteredWord.length() != 0) {

                QueryObject newWord = new QueryObject(filteredWord, basic.get(wordIndex).isNegated());
                newBasic.add(newWord);
            }


        }
        // reset phrase member variable
        phrases = newPhrases;
        basic = newBasic;
    }

    /**
     * Parses given text into its parts i.e. `not` parts, phrases, etc.
     * Doesn't use any Analyzer on text.
     * @param queryText the text of the query we will parse
     */
    public void get_words_unfiltered(String queryText) {

        // we must go through the entire query and check for
        // phrases and `not` expressions

        // to keep track of what kind of expressions we are in
        boolean inPhrase = false;
        boolean negated = false;

        // keep track of phrase words
        Phrase currentPhrase = null;

        // to keep track of the current word and its properties
        QueryObject currentObject;
        String currentWord;

        // split query by spaces and loop through each word
        String[] words = queryText.split("\\s+");

        for(int wordIndex = 0; wordIndex < words.length;
            wordIndex++) {

            // get current word
            currentWord = words[wordIndex];

            if(inPhrase) {

                // we are in a phrase, check if the
                // current word is to be negated
                if(currentWord.charAt(0) == '~') {

                    // our word is to be negated
                    // discard first char

                    negated = true;
                } else {

                    negated = false;
                }

                // create QueryObject and add to phrase
                currentObject = new QueryObject(currentWord, negated);
                currentPhrase.addQueryObject(currentObject);

                // check if we are at the end of the phrase
                if(currentWord.charAt(currentWord.length() - 1) == '"') {

                    // add to phrases
                    phrases.add(currentPhrase);

                    // no longer in a phrase
                    inPhrase = false;

                }

            } else {

                // not in a phrase, check if a phrase has begun
                if(currentWord.charAt(0) == '"') {

                    // we are now in a phrase
                    inPhrase = true;

                    currentPhrase = new Phrase(false);

                    // check if negated
                    if(currentWord.charAt(1) == '~') {

                        // this word is negated
                        negated = true;
                    } else {

                        negated = false;
                    }

                    // create object for our word
                    currentObject = new QueryObject(currentWord, negated);
                    currentPhrase.addQueryObject(currentObject);

                } else {

                    // check if negated phrase or word has begun
                    if(currentWord.charAt(0) == '~') {

                        // check if negated word or negated phrase
                        if(currentWord.charAt(1) == '"') {

                            inPhrase = true;
                            // in a negated phrase
                            currentPhrase = new Phrase(true);

                            // check if first word is negated
                            if(currentWord.charAt(2) == '~') {

                                negated = true;
                            } else {

                                negated = false;
                            }

                            currentObject = new QueryObject(currentWord, negated);
                            currentPhrase.addQueryObject(currentObject);
                        } else {

                            // negated word
                            currentObject = new QueryObject(currentWord, true);
                            basic.add(currentObject);
                        }

                    } else {

                        // non-negated word
                        currentObject = new QueryObject(currentWord, false);
                        basic.add(currentObject);
                    }
                }
            }
        }
    }


    /**
     * @param analyzer
     * @param input
     * @return the given string as processed by the given analyzer
     */
    public String get_analyzed_string(Analyzer analyzer, String input) {

        String result = "";
        List<String> tokens = AnalyzerUtils.tokenize(analyzer, input);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String t : tokens) {

            result = result + t + " ";
        }

        if(result.length() != 0) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * for each phrase currently stored,
     * calculate its vector
     */
    public void generate_phrase_vectors() {

        Phrase currentPhrase;
        for(int phraseIndex = 0; phraseIndex < phrases.size();
            phraseIndex++) {

            currentPhrase = phrases.get(phraseIndex);
            currentPhrase.calculate_vector(this.embeddingSpace);
            phrases.set(phraseIndex, currentPhrase);
        }
    }

    /**
     * for each stored term, obtain the corresponding vectors
     * from the embedding space
     */
    public void set_term_vectors() {

        WordVector newVector;
        QueryObject newTerm;
        for(int termIndex = 0; termIndex < basic.size();
            termIndex++) {

            newTerm = basic.get(termIndex);
            newVector = embeddingSpace.get_vector(newTerm.getWord());

            // apply negation if needed
            if(newTerm.isNegated()) {

                newVector = newVector.get_inverse();
            }

            newTerm.set_vector(newVector);
        }
    }

    /**
     *
     * @param field
     * @param analyzer
     * @param queryText
     * @return the structured query constructed based on
     * the given text and analyzer
     */
    public StructuredQuery buildQuery(String field, Analyzer analyzer,
                                      String queryText) {

        // clear member variables
        basic = new ArrayList<>();
        phrases = new ArrayList<>();
        // obtain word types without filtering

        get_words_unfiltered(queryText);

        // filter our words
        filter_words(analyzer);

        // calculate phrase vectors
        generate_phrase_vectors();
        set_term_vectors();

        return new StructuredQuery(phrases, basic);
    }
}

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import java.io.IOException;
import java.util.TreeMap;

public class DocumentStatistic {

    Document document;
    String fieldName;

    TreeMap<String, Integer> counts;
    Integer wordCount;

    public DocumentStatistic(Document document, String fieldName) {

        counts = new TreeMap<>();
        set_document(document);
        set_field(fieldName);
    }

    public void set_document(Document document) {

        this.document = document;
    }

    /**
     * Loop through document, count and store occurrences of words
     * and total document word length
     * @param analyzer
     * @throws IOException
     */
    public void process_counts(Analyzer analyzer) throws IOException {

        String text = document.getField(fieldName).stringValue();

        TokenStream tokenStream = analyzer.tokenStream(fieldName, text);
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();

        Integer wordCount = 0;
        // for each word
        while(tokenStream.incrementToken()) {

            // increment the occurrences for this word
            increment_count(attr.toString());

            // increment the total document length
            wordCount++;
        }

        tokenStream.close();

        this.wordCount = wordCount;
    }

    /**
     *
     * @param word
     * @return the number of times the given word
     * occurres in the document
     */
    public Integer get_count(String word) {

        // if no entry, return zero
        if(counts.containsKey(word)) {

            return counts.get(word);
        } else {

            return 0;
        }
    }

    /**
     * Increment the occurrences in the document
     * for the given word
     * @param word
     */
    private void increment_count(String word) {

        // if no entry for this word exists,
        // create one and set to 1
        // if entry exists, increment
        if(counts.containsKey(word)) {

            Integer count = counts.get(word);
            count++;
            counts.remove(word);
            counts.put(word, count);
        } else {

            counts.put(word, 1);
        }
    }

    public void set_field(String fieldName) {

        this.fieldName = fieldName;
    }

    public Integer get_word_count() {

        return this.wordCount;
    }
}

import org.apache.lucene.search.Query;
import java.util.ArrayList;


public class StructuredQuery extends Query {

    // the phrases of the query are the objects for words that occur
    // within quotation marks and will be treated collectively as one vector
    ArrayList<Phrase> phrases;

    // the query objects are all other objects of the query
    ArrayList<QueryObject> basic;

    public StructuredQuery(ArrayList<Phrase> phrases, ArrayList<QueryObject> basic) {

        this.phrases = phrases;
        this.basic = basic;
    }

    @Override
    public String toString(String s) {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

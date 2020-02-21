

public class QueryObject {

    String word = null;
    boolean negated = false;
    WordVector vector;

    public WordVector get_vector() {

        return this.vector;
    }

    public void set_vector(WordVector vector) {

        this.vector = vector;
    }

    public QueryObject() {

        setWord("");
        setNegated(false);
    }

    public QueryObject(String word, boolean negated) {

        setWord(word);
        setNegated(negated);
    }

    public void setWord(String word) {

        this.word = word;
    }
    public void setNegated(boolean negated) {

        this.negated = negated;
    }
    public boolean isNegated() {

        return negated;
    }

    public String getWord() {

        return word;
    }
}

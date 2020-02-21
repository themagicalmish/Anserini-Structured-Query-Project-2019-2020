import java.util.ArrayList;

public class Phrase {

    ArrayList<QueryObject> content = null;
    int size = 0;
    boolean negated = false;
    WordVector vector;


    public Phrase(boolean negated) {

        content = new ArrayList<>();
        setNegated(negated);
    }


    /**
     * Set the WordVector member variable, which gives the vector
     * representation of this phrase, based on the given embedding space.
     * @param embeddingSpace the embedding space we will construct the
     *                       WordVector for this phrase with
     */
    public void calculate_vector(EmbeddingSpace embeddingSpace) {

        WordVector result = embeddingSpace.get_vector(content.get(0).getWord());

        // apply negation to term if needed
        if(content.get(0).isNegated()) {

            result = result.get_inverse();
        }

        WordVector nextVector;

        // add all vectors accordingly
        for(int termIndex = 1; termIndex < size;
            termIndex++) {


            nextVector = embeddingSpace.get_vector(content.get(termIndex).getWord());

            // apply negation to term if needed
            if(content.get(termIndex).isNegated()) {

                nextVector = nextVector.get_inverse();
            }

            result = WordVector.add_vectors(result, nextVector);
        }

        // apply negation to result if needed
        if(this.isNegated()) {

            result = result.get_inverse();
        }
        this.vector = result;
    }

    public WordVector get_vector() {

        return this.vector;
    }


    public String toString() {

        String result = "";
        for(int i = 0; i < size; i++) {

            result = result + content.get(i).getWord();

            if(i < size - 1) {

                result = result + " ";
            }
        }

        return result;
    }

    public void addQueryObject(QueryObject newObject) {

        content.add(newObject);
        this.size++;
    }

    public void setNegated(boolean negated) {

        this.negated = negated;
    }

    public boolean isNegated() {

        return this.negated;
    }

    public ArrayList<QueryObject> getContent() {

        return this.content;
    }

    public int getSize() {

        return this.size;
    }

}

import info.debatty.java.lsh.LSHSuperBit;
import java.io.*;
import java.lang.Math;

public class WordVector implements Serializable {

    String term;
    Double[] contents;
    int dimensions;


    public WordVector() {
    }

    public WordVector(Double[] newContents, String term) {

        this.contents = newContents;
        this.term = term;
        dimensions = newContents.length;
    }

    /**
     *
     * @return the vector contents of this WordVector,
     * inverted (each component multiplied by -1)
     */
    public Double[] get_inverse_contents() {

        Double[] inverseContents = new Double[dimensions];

        for(int i = 0; i < dimensions; i++) {

            inverseContents[i] = -1.0 * contents[i];
        }

        return inverseContents;
    }

    /**
     *
     * @return a WordVector object that represents the
     * inverse of this word vector (pointing in
     * the opposite direction)
     */
    public WordVector get_inverse() {

        Double[] resultContents = this.get_inverse_contents();
        String resultPhrase = "~\"" + this.get_term() + "\"";
        return new WordVector(resultContents, resultPhrase);
    }

    public void set_dimensions(int newDimensions) {

        this.dimensions = newDimensions;
    }

    public Double[] get_contents() {

        return this.contents;
    }

    /**
     *
     * @return the contents of this vector as an array
     * of the primitive type (double) rather than Double
     */
    public double[] get_primitive_contents() {

        double[] result = new double[get_dimensions()];
        for(int dimension = 0; dimension < get_dimensions();
            dimension++) {

            result[dimension] = contents[dimension];
        }

        return result;
    }

    public int get_dimensions() {

        return this.dimensions;
    }

    public void set_contents(Double[] newContents) {

        this.contents = newContents;
    }

    public String get_term() {

        return this.term;
    }

    public void set_term(String term) {

        this.term = term;
    }

    /**
     *
     * @return the norm (magnitude) of this vector's contents
     */
    public double get_norm() {

        double sum = 0;

        for(int dimension = 0; dimension < contents.length;
            dimension++) {

            sum += contents[dimension] * contents[dimension];
        }

        return Math.sqrt(sum);
    }


    /**
     *
     * @param vec1
     * @param vec2
     * @param tolerance the tolerance level of the calculation, meaning
     *                  if the cosine simiilarity is below this tolerance
     *                  level, we will return zero.
     * @return the cosine similarity of vec1 and vec2
     */
    public static double cosine_similarity(WordVector vec1, WordVector vec2, Double tolerance) {


        Double result = dot_product(vec1, vec2) / (vec1.get_norm() * vec2.get_norm());

        return result >= tolerance ? result : 0.0;
    }

    /**
     *
     * @param vec1
     * @param vec2
     * @return the dot product of the contents of each given vector
     */
    public static double dot_product(WordVector vec1, WordVector vec2) {

        double result = 0;

        Double[] contents1 = vec1.get_contents();
        Double[] contents2 = vec2.get_contents();

        for(int dimension = 0; dimension < vec1.get_dimensions();
            dimension++) {

            result += contents1[dimension] * contents2[dimension];
        }

        return result;
    }

    /**
     *
     * @param vec1 first vector we will add
     * @param vec2 second vector we will add
     * @return array of Doubles giving the addition
     * of the contents of each given vector
     */
    public static Double[] add_vector_contents(WordVector vec1, WordVector vec2) {

        int dimensions = vec1.get_dimensions();
        Double[] resultContents = new Double[dimensions];

        Double[] contents1 = vec1.get_contents();
        Double[] contents2 = vec2.get_contents();

        for(int dimension = 0; dimension < dimensions;
            dimension++) {

            resultContents[dimension] = contents1[dimension] + contents2[dimension];
        }

        return resultContents;
    }

    /**
     *
     * @param vec1 first WordVector object we will add
     * @param vec2 second WordVector object we will add
     * @return a WordVector object representing the addition of
     * each given vector
     */
    public static WordVector add_vectors(WordVector vec1, WordVector vec2) {

        // string of new vector will be each word occuring in together
        // space seperated (like a phrase)
        String resultPhrase = vec1.get_term() + " " + vec2.get_term();

        // contents of WordVector is addition of contents
        // of each given vector
        Double[] resultContents = add_vector_contents(vec1, vec2);

        return new WordVector(resultContents, resultPhrase);
    }

    /**
     *
     * @param vec2 vector involved in addition
     * @return a WordVector object representing the addition
     * of the given vector and this vector object.
     */
    public WordVector addTo(WordVector vec2) {

        return add_vectors(this, vec2);
    }


    /**
     * @param lsh locality sensitive hashing object
     * @return the hash of this vector, with given paramaters
     */
    public int[] get_locality_hash(LSHSuperBit lsh) {

        return lsh.hash(this.get_primitive_contents());
    }

    /**
     * Compares the Locality Sensitive Hash of the given vectors
     * @param vecA
     * @param vecB
     * @param lsh the Locality Sensitive Hashing object we will use
     * @return 1.0 if vecA and vecB have the same Hash, 0.0 otherwise
     */
    public static double compare_hash(WordVector vecA, WordVector vecB,
                                      LSHSuperBit lsh) {

        int[] hashA = vecA.get_locality_hash(lsh);
        int[] hashB = vecB.get_locality_hash(lsh);

        for(int stageIndex = 0; stageIndex < hashA.length;
            stageIndex++) {

            if (hashA[stageIndex] != hashB[stageIndex]) {

                return 0.0;
            }
        }

        return 1.0;
    }
}

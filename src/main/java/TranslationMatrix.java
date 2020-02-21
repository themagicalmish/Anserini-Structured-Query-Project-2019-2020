import java.io.*;
import java.util.HashMap;
import java.util.TreeMap;

public class TranslationMatrix implements Serializable {

    // HashMap of HashMaps we will maintain to store similarity of vectors
    // in its normal distribution
    HashMap<String, HashMap<String, Double>> translations;

    // HashMap of TreeMaps we will maintain to store similarity of vectors
    // in an inverted distribution... this will be used for getting,
    // for example, the 10 most similar words to some word
    HashMap<String, TreeMap<Double, String>> inverseTranslations;

    public TranslationMatrix() {

        translations = new HashMap<>();
        inverseTranslations = new HashMap<>();
    }

    /**
     * @param X the first arbitrary word
     * @param Y the second arbitrary word
     * @return returns the similarity value of these words
     * if it exists, otherwise, returns zero.
     */
    public Double get_similarity(String X, String Y) {

        if(translations.containsKey(X)) {

            if(translations.get(X).containsKey(Y)) {

                return translations.get(X).get(Y);
            } else {

                return 0.0;
            }

        } else {

            return 0.0;
        }
    }

    /**
     * Initializes translation entry for a given string, that is
     * creates the initial HashMap / TreeMap inside each distribution
     * of translation probabilities.
     * @param X
     */
    public void init_entry(String X) {

        // first string in normal distribution
        if(!translations.containsKey(X)) {

            // string X isn't in translations yet...
            // make new HashMap for this string
            translations.put(X, new HashMap<String, Double>());
        }

        // do same for inverse distributions
        if(!inverseTranslations.containsKey(X)) {

            inverseTranslations.put(X, new TreeMap<Double, String>());
        }
    }

    /**
     * Adds the similarity for these two words
     * to this translation matrix
     * @param X our first word
     * @param Y our second word
     * @param similarity the similarity value between
     *                   these two words
     */
    public void put(String X, String Y, Double similarity) {

        // enter into normal distribution

        // get HashMaps that we will be updating
        HashMap<String, Double> distX = translations.get(X);
        HashMap<String, Double> distY = translations.get(Y);


        // edit each then add back after removing old ones
        distX.put(Y, similarity);
        distY.put(X, similarity);

        translations.remove(X);
        translations.remove(Y);
        translations.put(X, distX);
        translations.put(Y, distY);

        // do same for inverse distribution
        TreeMap<Double, String> invDistX = inverseTranslations.get(X);
        TreeMap<Double, String> invDistY = inverseTranslations.get(Y);

        invDistX.put(similarity, Y);
        invDistY.put(similarity, X);

        inverseTranslations.remove(X);
        inverseTranslations.remove(Y);
        inverseTranslations.put(X, invDistX);
        inverseTranslations.put(Y, invDistY);
    }


    /**
     *
     * @param X
     * @param Y
     * @return the first string out of X and Y if they are
     * ordered lexicographically
     */
    public String first_string(String X, String Y) {

        if(X.compareTo(Y) > 0) {

            // Y is first
            return Y;
        } else if (X.compareTo(Y) < 0) {

            // X is first
            return X;
        } else {

            // words are equal, return X
            return X;
        }
    }

    /**
     *
     * @param X
     * @param Y
     * @return the second string out of X and Y if they are
     * ordered lexicographically
     */
    public String second_string(String X, String Y) {

        if(X.compareTo(Y) < 0) {

            // Y is second
            return Y;
        } else if (X.compareTo(Y) > 0) {

            // X is second
            return X;
        } else {

            // words are equal, return X
            return Y;
        }
    }


    /**
     *
     * @param filePath a file path where a serialized translation
     *                 matrix is (hopefully) stored
     * @return a translation matrix that was stored
     * at `filePath`
     */
    public static TranslationMatrix read(String filePath) {

        System.out.println("Loading matrix from file...");
        TranslationMatrix spaceInFile = new TranslationMatrix();
        try {

            FileInputStream fis = new FileInputStream(filePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            spaceInFile = (TranslationMatrix) ois.readObject();
            ois.close();
            fis.close();
        } catch(Exception e) {}

        System.out.println("Matrix loaded.");
        return spaceInFile;
    }

    /**
     * Serializes this translation matrix to the
     * given file path
     * @param filePath
     */
    public void save(String filePath) {

        try {

            FileOutputStream fos = new FileOutputStream(filePath);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(this);
            oos.flush();
            oos.close();
            fos.close();
        } catch(Exception e) {}
    }
}

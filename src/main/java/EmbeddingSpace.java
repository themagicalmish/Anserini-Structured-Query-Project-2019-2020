
import java.io.*;
import java.util.*;

public class EmbeddingSpace implements Serializable {

    HashMap<String, WordVector> vectors;
    int dimensions;
    int size;

    /**
     * Creates and returns a TranslationMatrix object that stores a maximum
     * of `maxEntries` entries in each column/row - these entries will be those
     * with the highest similarity value and therefore the most relevant.
     * @param maxEntries the maximum number of entries we will store
     *                   for a row/column of the matrix
     * @return TranslationMatrix with at most `maxEntries` entries in each
     * row/column.
     */
    public TranslationMatrix generate_matrix_max_entries(int maxEntries) {

        // initialize our translation matrix
        TranslationMatrix result = new TranslationMatrix();

        // initialize each entry of the matrix
        Iterator entryIterator = vectors.entrySet().iterator();

        while(entryIterator.hasNext()) {

            Map.Entry entry = (Map.Entry) entryIterator.next();
            result.init_entry((String) entry.getKey());
        }

        // now we must re-iterate through each word embedding and compare
        // it with each other vector

        Double similarity;
        Map.Entry entryA, entryB;
        WordVector vecA, vecB;
        Object[] entries = vectors.entrySet().toArray();

        int innerIndex;

        // for each row, we must determine a distribution of similarities
        // we can only record `maxEntries` similarities for each row
        // therefore, for each row, we will calculate all similarities
        // and then at the end take the `maxEntries` highest

        // temporary distribution of similarities
        TreeMap<Double, String> distribution;

        for(int outerIndex = 0; outerIndex < entries.length;
            outerIndex++) {

            // initialize distribution for this word
            distribution = new TreeMap<>();

            if(outerIndex % 100 == 0) {

                double percent = (outerIndex / 94000.00) * 100.00;
                System.out.print("Progress: ");
                System.out.print(percent);
                System.out.println("%");
            }

            // obtain our first vector
            entryA = (Map.Entry) entries[outerIndex];
            vecA = (WordVector) entryA.getValue();

            for(innerIndex = 0; innerIndex < entries.length;
                innerIndex++) {

                // obtain our first vector
                entryB = (Map.Entry) entries[innerIndex];
                vecB = (WordVector) entryB.getValue();

                similarity = WordVector.cosine_similarity(vecA, vecB, 0.0);

                // add to temporary distribution
                distribution.put(similarity, vecB.term);
            }

            // we now have a full distribution for one word
            // (that is, one row in our matrix)
            // get out the top entries
            Map.Entry<Double, String> nextEntry;
            for(int entryIndex = 0; entryIndex < maxEntries;
                entryIndex++) {

                // get the first entry
                nextEntry = distribution.pollLastEntry();

                // add it to our result
                result.put(vecA.get_term(), nextEntry.getValue(), nextEntry.getKey());
                result.put(nextEntry.getValue(), vecA.get_term(), nextEntry.getKey());

            }
        }


        return result;
    }

    /**
     * Constructs TranslationMatrix from this embedding space, only
     * recording similarities that are greater than or equal than
     * the given minimum similarity. For others, there will be
     * no recorded similarity.
     * @param minSimilarity the lowest similarity we will record
     * @return a translation matrix only storing similarities that are
     * greater than or equal to the given `minSimilarity`
     */
    public TranslationMatrix generate_matrix_min_similarity(Double minSimilarity) {

        // initialize our translation matrix
        TranslationMatrix result = new TranslationMatrix();

        // initialize each entry of the matrix
        Iterator entryIterator = vectors.entrySet().iterator();

        while(entryIterator.hasNext()) {

            Map.Entry entry = (Map.Entry) entryIterator.next();
            result.init_entry((String) entry.getKey());
        }

        // now we must re-iterate through each word embedding and compare
        // it with each other vector

        Double similarity;
        Map.Entry entryA, entryB;
        WordVector vecA, vecB;
        Object[] entries = vectors.entrySet().toArray();

        int innerIndex;

        for(int outerIndex = 0; outerIndex < entries.length;
            outerIndex++) {

            if(outerIndex % 100 == 0) {

                double percent = (outerIndex / 94000.00) * 100.00;
                System.out.print("Progress: ");
                System.out.print(percent);
                System.out.println("%");
            }

            // obtain our first vector
            entryA = (Map.Entry) entries[outerIndex];
            vecA = (WordVector) entryA.getValue();

            for(innerIndex = outerIndex; innerIndex < entries.length;
                innerIndex++) {

                // obtain our first vector
                entryB = (Map.Entry) entries[innerIndex];
                vecB = (WordVector) entryB.getValue();

                similarity = WordVector.cosine_similarity(vecA, vecB, 0.0);

                // only record this entry if similarity is greater
                // than or equal to `minSimilarity`
                if(similarity >= minSimilarity) {
                    result.put(vecA.get_term(), vecB.get_term(), similarity);
                    result.put(vecB.get_term(), vecA.get_term(), similarity);
                }
            }
        }


        return result;
    }

    /**
     *
     * @return full translation matrix with no restrictions
     * ... this will be very big (good luck having enough heap space)
     */
    public TranslationMatrix generate_matrix() {

        // initialize our translation matrix
        TranslationMatrix result = new TranslationMatrix();

        // initialize each entry of the matrix
        Iterator entryIterator = vectors.entrySet().iterator();

        while(entryIterator.hasNext()) {

            Map.Entry entry = (Map.Entry) entryIterator.next();
            result.init_entry((String) entry.getKey());
        }

        // now we must re-iterate through each word embedding and compare
        // it with each other vector

        Double similarity;
        Map.Entry entryA, entryB;
        WordVector vecA, vecB;
        Object[] entries = vectors.entrySet().toArray();

        int innerIndex;

        for(int outerIndex = 0; outerIndex < entries.length;
            outerIndex++) {

            if(outerIndex % 100 == 0) {

                double percent = (outerIndex / 94000.00) * 100.00;
                System.out.print("Progress: ");
                System.out.print(percent);
                System.out.println("%");
            }

            // obtain our first vector
            entryA = (Map.Entry) entries[outerIndex];
            vecA = (WordVector) entryA.getValue();

            for(innerIndex = outerIndex; innerIndex < entries.length;
                innerIndex++) {

                // obtain our first vector
                entryB = (Map.Entry) entries[innerIndex];
                vecB = (WordVector) entryB.getValue();

                similarity = WordVector.cosine_similarity(vecA, vecB, 0.0);

                result.put(vecA.get_term(), vecB.get_term(), similarity);
                result.put(vecB.get_term(), vecA.get_term(), similarity);

            }
        }

        return result;
    }

    /**
     *
     * @param term the word we want the representation of
     * @return the vector representation of the given word
     */
    public WordVector get_vector(String term) {

        return vectors.get(term);
    }

    public EmbeddingSpace() {

        vectors = new HashMap<>();
        dimensions = 0;
    }

    /**
     * Construct embedding space from word vectors in file
     * @param filePath
     * @throws IOException
     */
    public EmbeddingSpace(String filePath) throws IOException {

        vectors = new HashMap<>();
        load_model(filePath);
    }

    /**
     *
     * @param word1
     * @param word2
     * @return the cosine similarity of the two given words
     */
    public double word_vector_similarity(String word1, String word2, double tolerance) {

        // attempt to obtain vectors for each word
        WordVector vec1 = get_vector(word1);
        WordVector vec2 = get_vector(word2);

        return WordVector.cosine_similarity(vec1, vec2, tolerance);
    }


    /**
     * Loads embeddings from given file path into this EmbeddingSpace.
     * After being successfully called, the `vectors` member variable
     * will contain word embeddings for all words in given corpus.
     * The `dimensions` member variable will store the dimensionality of
     * our space, and the `size` member variable will store the size
     * of our given corpus.
     * @param filePath the filePath where we will look for embeddings
     * @throws IOException
     */
    public void load_model(String filePath) throws IOException {

        File f = new File(filePath+"_matrix.ser");

        if(f.exists()) {
            /* load the matrix that has been serialised to disk */
            System.out.println("Loading matrix from file");
            //this.readW2VSerialised(f.getAbsolutePath());
            //TODO: to replace with appropriate method
        } else {


            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line = null;
            int count = 0;
            int numberofdimensions = 0;
            int foundterms = 0;
            while ((line = br.readLine()) != null) {

                if(count == 0) {
                    //this is the first line: it says how many words and how many dimensions
                    String[] input = line.split(" ");
                    numberofdimensions = Integer.parseInt(input[1]);
                    count++;
                    continue;
                }

                String[] input = line.split(" ");
                String term = input[0];

                // create a new vector for this word
                WordVector newVector = new WordVector();

                foundterms++;
                int dimension = 0;
                Double[] vector = new Double[numberofdimensions];

                for(int i = 1; i < input.length; i++) {

                    vector[dimension] = Double.parseDouble(input[i]);
                    dimension++;
                }

                set_dimensions(dimension);
                newVector.set_dimensions(dimension);

                newVector.set_term(term);
                newVector.set_contents(vector);
                vectors.put(term, newVector);

                count++;
            }

            set_size(count);

            System.out.println("Terms founds in word2vec: " + foundterms);
            br.close();
        }
    }

    public void set_dimensions(int dimensions) {

        this.dimensions = dimensions;
    }

    public int get_dimensions() {

        return this.dimensions;
    }

    public int get_size() {

        return this.size;
    }

    public void set_size(int size) {

        this.size = size;
    }

    /**
     * Serializes this object to given path
     * @param filePath location where we will save
     */
    public void save(String filePath) {

        try {

            FileOutputStream fos=new FileOutputStream(filePath);
            ObjectOutputStream oos=new ObjectOutputStream(fos);

            oos.writeObject(this);
            oos.flush();
            oos.close();
            fos.close();
        } catch(Exception e) {}
    }

    /**
     * Attempts to read and return an Embedding space serialized
     * at the given filePath
     * @param filePath location of EmbeddingSpace object
     * @return the EmbeddingSpace object read from file
     */
    public static EmbeddingSpace read(String filePath) {

        System.out.println("Loading embedding space from file...");
        EmbeddingSpace spaceInFile = new EmbeddingSpace();
        try {
            FileInputStream fis = new FileInputStream(filePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            spaceInFile = (EmbeddingSpace) ois.readObject();
            ois.close();
            fis.close();
        } catch(Exception e) {}

        System.out.println("Embedding space loaded.");
        return spaceInFile;
    }
}

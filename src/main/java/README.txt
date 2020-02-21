This is an implementation of Dirichlet smoothed search methods using a translation language
model, implemented using anserini/lucene. A 'structured query' search is also implemented.
This is where a query can have, for example, a word whose vector is negated, or a phrase
in which the vectors are added together and treated as one object. 

For any meaningful use of this, you must provide an index readable by anserini and word embeddings.
Examples of such indexes can be found at https://git.uwaterloo.ca/jimmylin/anserini-indexes. Obtaining 
an index once downloaded and extracted can be done with the code:

IndexReader anseriniReader = IndexReaderUtils.getReader(INDEX_DIR);

Where `INDEX_DIR` is the path of the index. Examples of word vectors (embedding space) can be found at http://zuccon.net/ntlm.html. Obtaining an embedding space once downloaded and extracted
can be done with the code:

EmbeddingSpace space = new EmbeddingSpace(EMBEDDINGS_DIR);

Where `EMBEDDINGS_DIR` is the path of the embedding space. Once an embedding space is obtained,
you may generate a translation matrix. There are various options for translation matrix generation,
which can be observed in the java documentation in the code. 

With a translation matrix, word embedding space, index and other relevant objects, the `generate_scores`
method in the StructuredReranker class will run and produce results for three types of queries:
	- Dirichlet without translation language model
	- Dirichlet with translation language model
	- Dirichlet with translation language mdoel with structured query

Running this successfully will produce three files which can be used for anserini evaluations. Note that
you must provide an anserini results file. An example of such a file is `run.robust04.bm25.txt` in the 
robust04 tutorial in the online notebooks for anseirni (found at:
https://colab.research.google.com/drive/1s44ylhEkXDzqNgkJSyXDYetGIxO9TWZn#scrollTo=YZm8e5LrwIt5).
The output files of the method will be in the same format of this file. 

Once you have done anserini evaluation of the output files, for easier analysis, you may use the
method in StructuredReranker, `reduce_result_file`. The workings of this method are explained in
the java documentation, however, note that depending on the analysis you are doing, you may want to
remove the last line of the output file, since this may be the average of all queries (and therefore
shouldn't, for example, be included in a t-test). 

The `test` class contains a main function which is an example of using the generate scores method
on another machine. 

Note that within the code, there are several unused methods. These have been left unremoved only since
they may be useful in the future. 

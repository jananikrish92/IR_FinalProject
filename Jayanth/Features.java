import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

public class Features {
  public static Map<String, Double> term_distrib(IndexReader index,
      Map<String, Double> fbterms, List<SearchResult> fbresults,
      List<String> query_terms, LuceneQLSearcher searcher, int numfbdocs)
      throws IOException {

    double total_fbtermfreq = 0.0;
    Map<String, Double> dist = new HashMap<String, Double>();
    List<Integer> fbdocs = new ArrayList<Integer>();
    FileDocLengthReader flen =
        new FileDocLengthReader(new File("index_trec123"), "content");

    for (SearchResult sr : fbresults) {
      int docid = sr.getDocid();
      fbdocs.add(docid);
    }

    // considering all terms in feedback documents for normalization factor in
    // denominator
    /*
     * Set<String> voc = new HashSet<String>(); for (SearchResult sr :
     * fbresults) { int docid = sr.getDocid(); fbdocs.add(docid); int len =
     * flen.getLength(docid);
     * 
     * // System.out.print(len + " ");
     * 
     * String[] text = index.document(docid).get("content").split(" ");
     * 
     * for (String t : text) { if (searcher.stopwords.contains(t)) len--; else
     * voc.add(t);
     * 
     * } // System.out.println(len);
     * 
     * total_fbtermfreq += len;
     * 
     * } System.out.println(voc.size());
     */
    // System.out.println("Total : " + total_fbtermfreq);
    // considering query terms for normalization factor in denominator

    /*
     * for (String qterm : query_terms) { LuceneTermPostingList l = new
     * LuceneTermPostingList(index, "content", qterm);
     * 
     * while (!l.end()) { if (fbdocs.contains(l.doc())) { int freq = l.freq();
     * total_fbtermfreq += freq; } l.next(); } }
     * 
     */
    Terms vector = null;
    Map<String, Double> vocabulary = new HashMap<String, Double>();
    double doclen[] = new double[numfbdocs];
    double denom = 0;
    // System.out.println("Results size : " + results.size());
    for (int i = 0; i < Math.min(numfbdocs, fbresults.size()); i++) {

      int dnum = fbresults.get(i).getDocid();
      vector = searcher.index.getTermVector(dnum, "content"); // Read the
                                                              // document's
      // term vector.

      TermsEnum te = vector.iterator();

      BytesRef term;

      while ((term = te.next()) != null) {
        if (searcher.stopwords.contains(term.utf8ToString()) != true) {
          String termstr = term.utf8ToString(); // Get the text string of the
          // term.
          double freq = te.totalTermFreq(); // Get the frequency of the term in
          // the document.
          if ((Double) freq == null)
            freq = 0.0;

          vocabulary.put(termstr, vocabulary.getOrDefault(termstr, 0.0) + freq);
          doclen[i] += freq;
          denom += freq;
        }
      }
    }
    System.out.println("Denom = " + denom);

    for (String term : fbterms.keySet()) {
      LuceneTermPostingList l =
          new LuceneTermPostingList(index, "content", term);
      int tfInFBdocs = 0;
      while (!l.end()) {
        if (fbdocs.contains(l.doc())) {
          int freq = l.freq();
          tfInFBdocs += freq;
        }
        l.next();
      }
      /*
       * System.out.print(term + " tfInFBdocs = " + tfInFBdocs + " ratio =  " +
       * ((double) tfInFBdocs / (double) denom) + "\n");
       */
      double score = Math.log((double) tfInFBdocs / (double) denom);

      dist.put(term, score);
    }

    return dist;

  }

  public static Map<String, Double> single_cooccur(IndexReader index,
      Map<String, Double> fbterms, List<SearchResult> fbresults) {


    return null;
  }

  public static void main(String[] args) throws IOException {

    String pathIndex = "index_trec123";
    Analyzer analyzer = LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);

    String pathQueries = "queries_trec1-3"; // change it to your
    // query file path
    String pathQrels = "qrels_trec1-3"; // change it to your
    // qrels file path
    String pathStopwords = "stopwords_inquery";
    String field_docno = "docno";
    String field_search = "content";

    // RM searcher = new RM(pathIndex);
    LuceneQLSearcher searcher = new LuceneQLSearcher(pathIndex);
    searcher.setStopwords(pathStopwords);

    Map<String, String> queries = EvalUtils.loadQueries(pathQueries);
    Map<String, Set<String>> qrels = EvalUtils.loadQrels(pathQrels);

    int top = 1000;
    double mu = 1000;

    String query = queries.get("51");// qid);
    List<String> terms = LuceneUtils.tokenize(query, analyzer);
    String[] termsarray = new String[terms.size()];
    for (int k = 0; k < termsarray.length; k++) {
      termsarray[k] = terms.get(k);
    }
    
    /*
     * List<SearchResult> RM1res = searcher.estimateQueryModelRM1(field_search,
     * terms, 1500.0, 0.0, 10, 100);
     * 
     * Map<String, Double> scoresRM1 = searcher.getTFS(field_search, RM1res,
     * 0.0, 1500.0, 10);
     */
     
    
    RM rm = new RM(pathIndex);
    List<SearchResult> RM1res = rm.search("content", terms, 1500.0, 10);
    Map<String, Double> scoresRM1 = searcher.estimateQueryModelRM1(field_search,
        terms, 1500.0, 0.0, 10, Integer.MAX_VALUE);

    System.out.println(scoresRM1.size());

    Map<String, Double> word_distrib =
        term_distrib(searcher.index, scoresRM1, RM1res, terms, searcher, 10);

    for (String key : word_distrib.keySet()) {
      System.out.println(key + " : " + word_distrib.get(key));
    }

  }

}

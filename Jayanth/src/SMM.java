import java.io.IOException;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.*;

public class SMM {

  public Map<String, Double> EM(Map<String, Integer> tfs, int totalfbDocsLen, Map<String, HashMap<Integer, Long>> wfreqs, IndexReader index, double lambda) throws IOException{

    Map<String, Double> T = new HashMap<String, Double>();
    Map<String, Double> P = new HashMap<String, Double>();
    Map<String, Double> prev = new HashMap<String, Double>();

    for (String key : tfs.keySet()) {
      prev.put(key, 0.0);
    }

    do{

      for(String w : tfs.keySet()){
        double pcval =
                lambda * ((double) index.totalTermFreq(new Term("content", w))
                        / (double) index.getSumTotalTermFreq("content"));
        double pval = (1-lambda)*((double)tfs.get(w)/(double)totalfbDocsLen);
        double score = pval / (pval + pcval);

        T.put(w, score);

      }
      double denom = 0.0;

      for (String w : tfs.keySet()) {
        HashMap<Integer, Long> temp = wfreqs.get(w);
        for (Integer d : temp.keySet()) {
          denom += temp.get(d) * T.get(w);
        }
      }

      for (String w : tfs.keySet()) {
        HashMap<Integer, Long> temp = wfreqs.get(w);

        double numer = 0.0;

        for (Integer d : temp.keySet()) {
          numer += temp.get(d) * T.get(w);
        }

        P.put(w, numer / denom);

      }

      int count = 0;

      for (String w : P.keySet()) {
        if (Math.abs(P.get(w) - prev.get(w)) < 0.0001)
          count++;
        else
          prev.put(w, P.get(w));
      }

      if (count == P.size())
        break;

    }while(true);


    Map<String, Double> res = new HashMap<>();
    for(String w : tfs.keySet()){
      double pcval =
              lambda * ((double) index.totalTermFreq(new Term("content", w))
                      / (double) index.getSumTotalTermFreq("content"));
      double pval = (1-lambda)*P.get(w);//((double)tfs.get(w)/(double)totalfbDocsLen);
      res.put(w, pval + pcval);

    }

    //return P;
    return res;

  }

  public Map<String, Double> estimateSMM(String field, List<String> terms,
                                         double lambda, int numfbdocs, int numfbterms) throws IOException {
    LuceneQLSearcher searcher = new LuceneQLSearcher("/Users/jananikrishna/Documents/IRFinalProject/index_robust04");
    List<SearchResult> results = searcher.search(field, terms, 0, numfbdocs);
    Set<String> voc = new HashSet<>();
    for (SearchResult result : results) {
      TermsEnum iterator =
              searcher.index.getTermVector(result.getDocid(), field).iterator();
      BytesRef br;
      while ((br = iterator.next()) != null) {
        if (!searcher.stopwords.contains(br.utf8ToString())) {
          voc.add(br.utf8ToString());
        }
      }
    }

    Map<String, Double> collector = new HashMap<>();

    int totalfbDocsLen = 0;
    int len[] = new int[numfbdocs];
    int i = 0;

    Map<String, HashMap<Integer, Long>> wfreqs =
            new HashMap<String, HashMap<Integer, Long>>();
    Map<String, Integer> tfs = new HashMap<>();
    for (SearchResult result : results) {
      TermsEnum iterator =
              searcher.index.getTermVector(result.getDocid(), field).iterator();

      len[i] = 0;

      BytesRef br;
      while ((br = iterator.next()) != null) {

        if (voc.contains(br.utf8ToString())) {
          tfs.put(br.utf8ToString(), (int) iterator.totalTermFreq()
                  + tfs.getOrDefault(br.utf8ToString(), 0)); // frequency of a term
          // in all feedback
          // documents
          Map<Integer, Long> temp = wfreqs.get(br.utf8ToString());
          if (temp == null) {
            temp = new HashMap<Integer, Long>();

          }

          temp.put(result.getDocid(), iterator.totalTermFreq());
          wfreqs.put(br.utf8ToString(), (HashMap<Integer, Long>) temp); // contains
          // frequency
          // of a
          // word
          // in
          // all
          // feedback
          // documents
        }

        len[i] += iterator.totalTermFreq(); // length of a feedback document
      }
      totalfbDocsLen += len[i]; // combined lengths of all feedback documents
      i++;
    }

    Map<String, Double> res =
            EM(tfs, totalfbDocsLen, wfreqs, searcher.index, lambda);

    /*for (String w : res.keySet()) {
      System.out.println(w + " : " + res.get(w));
    }*/



    Map<String, Double> mle = new HashMap<>();
    for (String term : terms) {
      mle.put(term, mle.getOrDefault(term, 0.0) + 1.0);
    }
    for (String w : mle.keySet()) {
      mle.put(w, mle.get(w) / terms.size());
    }

    Set<String> v = new TreeSet<>();
    v.addAll(terms);
    v.addAll(res.keySet());

    Map<String, Double> finalres = new HashMap<>();
    for (String w : v) {
      finalres.put(w, 0.5 * mle.getOrDefault(w, 0.0)
              + (1.0 - 0.5) * res.getOrDefault(w, 0.0));
    }

    finalres = sortByValue(finalres);
    // System.out.println(numfbterms);

    Map<String, Double> top_n_terms = new TreeMap<>();
    int j = 0;
    if (numfbterms > 0) {
      for (Map.Entry<String, Double> top_terms : finalres.entrySet()) {
        if (j < numfbterms) {
          top_n_terms.put(top_terms.getKey(), top_terms.getValue());
          j++;
        } else
          break;
      }

      return top_n_terms;

    } else {
      return finalres;
    }
   // return finalres;
  }


  public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> unsortMap) {

    List<Map.Entry<K, V>> list =
            new LinkedList<Map.Entry<K, V>>(unsortMap.entrySet());

    Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
      public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
        return (o2.getValue()).compareTo(o1.getValue());
      }
    });

    Map<K, V> result = new LinkedHashMap<K, V>();
    for (Map.Entry<K, V> entry : list) {
      result.put(entry.getKey(), entry.getValue());
    }

    return result;

  }
/*
  public static void  main(String args[])throws  IOException{
    SMM smm = new SMM();


    String pathIndex = "index_trec123";
    Analyzer analyzer = LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);

    String pathQueries = "queries_trec1-3"; // change it to your
    // query file path
    String pathQrels = "qrels_trec1-3"; // change it to your
    // qrels file path
    String pathStopwords = "stopwords_inquery";
    String field_docno = "docno";
    String field_search = "content";
    //String indexName = "index_trec123";
    String outputFolder = "indexTrec1230P\\BaselineRM3";//"C:\\Users\\jannu bhai\\IdeaProjects\\IRProjectVersion2\\indexTrec123OP\\BaseLineRM1"; //change based on setting
    // RM searcher = new RM(pathIndex);
    LuceneQLSearcher searcher = new LuceneQLSearcher(pathIndex);
    searcher.setStopwords(pathStopwords);
    System.out.println("entering main");
    Map<String, String> queries = EvalUtils.loadQueries(pathQueries);
    Map<String, Set<String>> qrels = EvalUtils.loadQrels(pathQrels);
    String qid="51";
    int top = 1000;
    double mu = 1000;
    int numfbdocs = 20;


    String query = queries.get(qid);// qid);
    List<String> terms = LuceneUtils.tokenize(query, analyzer);

    Map<String, Double> res = smm.estimateSMM("content", terms, 0.5, numfbdocs, 80);
    for (String w : res.keySet()) {
      System.out.println(w + " : " + res.get(w));
    }
  }*/

}
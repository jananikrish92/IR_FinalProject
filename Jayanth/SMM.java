import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    return P;
    
  }

  public Map<String, Double> estimateSMM(String field, List<String> terms,
      double lambda, int numfbdocs, int numfbterms) throws IOException {
    LuceneQLSearcher searcher = new LuceneQLSearcher("index_trec123");
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

    for (String w : res.keySet()) {
      System.out.println(w + " : " + res.get(w));
    }

    /*
     * for (SearchResult result : results) { for (String w : voc) {
     * 
     * } }
     */

    /*
     * for (SearchResult result : results) { double ql = result.getScore();
     * double dw = Math.exp(ql); TermsEnum iterator =
     * searcher.index.getTermVector(result.getDocid(), field).iterator();
     * Map<String, Integer> tfs = new HashMap<>(); int len = 0; BytesRef br;
     * while ((br = iterator.next()) != null) { tfs.put(br.utf8ToString(), (int)
     * iterator.totalTermFreq()); len += iterator.totalTermFreq(); } for (String
     * w : voc) { int tf = tfs.getOrDefault(w, 0); double pw = (tf + mufb *
     * searcher.index.totalTermFreq(new Term(field, w)) /
     * searcher.index.getSumTotalTermFreq(field)) / (len + mufb);
     * collector.put(w, collector.getOrDefault(w, 0.0) + pw * dw); } }
     */
    // return Utils.getTop(Utils.norm(collector), numfbterms);
    return null;
  }

}

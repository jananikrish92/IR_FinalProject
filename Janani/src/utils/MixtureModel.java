package utils;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.*;

public class MixtureModel {

    public static Map<String, Double> EM(Map<String, Integer> tfs, int totalfbDocsLen, Map<String, HashMap<Integer, Long>> wfreqs, IndexReader index, double lambda) throws IOException{

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

    public static Map<String, Double> estimateSMM(String field, List<String> terms,
                                           double lambda, int numfbdocs, int numfbterms) throws IOException {
        String pathStopwords = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\stopwords_inquery"; // change to your stop words path
        LuceneQLSearcher searcher = new LuceneQLSearcher("index_trec123");
        searcher.setStopwords(pathStopwords);
        List<SearchResult> results = searcher.search(field, terms, 0, numfbdocs);
        Set<String> voc = new HashSet<>();
        for (SearchResult result : results) {
            TermsEnum iterator =
                    searcher.index.getTermVector(result.getDocid(), field).iterator();
            BytesRef br;
            while ((br = iterator.next()) != null) {
                if (!AbstractQLSearcher.stopwords.contains(br.utf8ToString())) {
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


        res=sortByValue(res);
       // System.out.println(numfbterms);

        Map<String, Double> top_n_terms = new TreeMap<>();
        int j = 0;
        if (numfbterms > 0) {
            for (Map.Entry<String, Double> top_terms : res.entrySet()) {
                if (j < numfbterms) {
                    top_n_terms.put(top_terms.getKey(), top_terms.getValue());
                    j++;
                } else
                    break;
            }

            return top_n_terms;

        } else
            return res;
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
       // return null;
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
}
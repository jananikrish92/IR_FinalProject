import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.*;

/**
 * Created by jph on 19/12/16.
 */
public class DMM {

    public Map<String, Double> estimateDMM(String field, List<String> terms,
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
        //int len[] = new int[numfbdocs];
        Map<Integer, Long> len = new HashMap<>();
        int i = 0;

        Map<String, HashMap<Integer, Long>> wfreqs =
                new HashMap<String, HashMap<Integer, Long>>();
        Map<String, Integer> tfs = new HashMap<>();
        for (SearchResult result : results) {
            TermsEnum iterator =
                    searcher.index.getTermVector(result.getDocid(), field).iterator();



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
                len.put(result.getDocid(), iterator.totalTermFreq());
                //len[i] += iterator.totalTermFreq(); // length of a feedback document
            }
            //totalfbDocsLen += len[i]; // combined lengths of all feedback documents
            i++;
        }

        Map<String, Double> DMMscore = new HashMap<>();

        for(String word : wfreqs.keySet()){
            double tempScore = 0.0;
            Map<Integer, Long> freqs = wfreqs.get(word);
            for(Integer docid : freqs.keySet()){
                long freq = freqs.get(docid);
                long doclen = len.get(docid);

                double p = (double)freq/(double)doclen;

                if(p == 0.0){
                    tempScore += 0.0;
                }
                else
                    tempScore += Math.log(p);
            }

            tempScore /=  numfbdocs;

           double p1 = (double)searcher.index.totalTermFreq(new Term(word))/(double)searcher.index.getSumTotalTermFreq("content");

           double temp1;
           if(p1 == 0.0)
               temp1 = 0.0;

           else
               temp1 = lambda*Math.log(p1);

           tempScore -= temp1;


           tempScore /= (1.0/(1.0-lambda));

           if(tempScore != 0.0)
            tempScore = Math.exp(tempScore);

           DMMscore.put(word, tempScore);

        }


        /*Map<String, Double> res =
                EM(tfs, totalfbDocsLen, wfreqs, searcher.index, lambda);*/

  /*  for (String w : DMMscore.keySet()) {
      System.out.println(w + " : " + DMMscore.get(w));
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
        v.addAll(DMMscore.keySet());

        Map<String, Double> finalres = new HashMap<>();
        for (String w : v) {
            finalres.put(w, 0.5 * mle.getOrDefault(w, 0.0)
                    + (1.0 - 0.5) * DMMscore.getOrDefault(w, 0.0));
        }



        return finalres;



        //return null;
    }

    public static void  main(String args[])throws  IOException{
        DMM dmm = new DMM();


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

        Map<String, Double> res = dmm.estimateDMM("content", terms, 0.5, numfbdocs, 80);

        for (String w : res.keySet()) {
            System.out.println(w + " : " + res.get(w));
        }
    }

}

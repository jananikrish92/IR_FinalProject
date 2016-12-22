import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SetOnce;
import org.apache.lucene.util.StringHelper;

public class Features {
  public static Map<String, Double> term_distrib(IndexReader index,
      Map<String, Double> fbterms, List<SearchResult> fbresults,
      List<String> query_terms, LuceneQLSearcher searcher, int numfbdocs)
      throws IOException {

    double total_fbtermfreq = 0.0;
    Map<String, Double> dist = new HashMap<String, Double>();
    List<Integer> fbdocs = new ArrayList<Integer>();

    for (SearchResult sr : fbresults) {
      int docid = sr.getDocid();
      fbdocs.add(docid);
    }


    Terms vector = null;
    Map<String, Double> vocabulary = new HashMap<String, Double>();
    double doclen[] = new double[numfbdocs];
    double denom = 0;
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
      double score = Math.log((double) tfInFBdocs / (double) denom);

      dist.put(term, score);
    }

    return dist;

  }

  public static void main(String[] args) throws IOException {

    String pathIndex = "/Users/jananikrishna/Documents/IRFinalProject/index_robust04";
    Analyzer analyzer = LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);

    String pathQueries = "/Users/jananikrishna/Documents/IRFinalProject/queries_robust04"; // change it to your
    // query file path
    String pathQrels = "/Users/jananikrishna/Documents/IRFinalProject/qrels_robust04"; // change it to your
    // qrels file path
    String pathStopwords = "/Users/jananikrishna/Documents/IRFinalProject/stopwords_inquery";
    String field_docno = "docno";
    String field_search = "content";
      //String indexName = "index_trec123";
     String outputFolder = "/Users/jananikrishna/Documents/IRFinalProject/indexRobustOP/BaseLineRM3"; //change based on setting
    // RM searcher = new RM(pathIndex);

    LuceneQLSearcher searcher = new LuceneQLSearcher(pathIndex);
    searcher.setStopwords(pathStopwords);
    System.out.println("entering main");
    Map<String, String> queries = EvalUtils.loadQueries(pathQueries);
    Map<String, Set<String>> qrels = EvalUtils.loadQrels(pathQrels);
   // String qid="301";
    int top = 1000;
    double mu = 1000;
    int numfbdocs = 20;
      System.out.println(searcher.index.numDocs());
      int gcount = 0, bcount = 0, ncount = 0,sum = 0;
    for (String qid : queries.keySet()) {
        //if (Integer.parseInt(qid) >= 464) {
            try {
                String query = queries.get(qid);// qid);
                List<String> terms = LuceneUtils.tokenize(query, analyzer);
               File file = new File(outputFolder + "/" + qid + "ext");
                PrintWriter pw = new PrintWriter(file);

                RM rm = new RM(pathIndex);
                if(terms.size()==0) continue;
              Map<String, Double> scoresRM1 = searcher.estimateQueryModelRM1(
                field_search, terms, 1500.0, 0.0, 20, 80);

             Map<String, Double> scoreRM3 = searcher.estimateQueryModelRM3(terms, scoresRM1 , 0.5);

              SMM smm = new SMM();
                Map<String, Double> scoresSMM = smm.estimateSMM("content", terms, 0.5, 20, 80);

                DMM dmm = new DMM();
                Map<String, Double> scoresDMM = dmm.estimateDMM("content", terms, 0.5, 20, 80);

                List<SearchResult> qlRes = rm.search("content", terms, 1500.0, 1000);


                List<String> expansionTermsList = new ArrayList<>(scoresSMM.keySet()); // change scores key set based on the model
                System.out.println(expansionTermsList.size());
                if (expansionTermsList.size() == 0)
                    continue;
                Feature_jan feature = new Feature_jan();
                SearchResult.dumpDocno(searcher.index, "docno", qlRes);

               Map<String, Double> feature1Map = scalingFeatures(feature.feature1(searcher.index, expansionTermsList, qlRes, 20));
                  Map<String,Double> feature2Map=scalingFeatures(feature.feature2(searcher.index,expansionTermsList,searcher));
                Map<String, Double> feature3Map = scalingFeatures(feature.feature3(searcher.index, expansionTermsList, qlRes, terms, 20));
                Map<String, Double> feature5Map = scalingFeatures(feature.feature5(searcher.index, expansionTermsList, qlRes, terms));
                Map<String, Double> feature7Map = scalingFeatures(feature.feature7(searcher.index, expansionTermsList, terms, qlRes));
                Map<String, Double> feature9Map = scalingFeatures(feature.feature9(searcher.index, qlRes, terms, expansionTermsList));
                 Map<String,Double> feature10Map=scalingFeatures(feature.feature10(searcher,terms,expansionTermsList));
                Map<String,Double> feature4Map=scalingFeatures(feature.feature4(expansionTermsList,terms,searcher));
                Map<String,Double> feature8Map=scalingFeatures(feature.feature8(expansionTermsList,terms,searcher));

                double p30 = EvalUtils.precision(qlRes, qrels.get(qid), 30);
                double ap = EvalUtils.avgPrec(qlRes, qrels.get(qid), top);
                double w = 0.01;
                for (String expansionTerms : expansionTermsList) {
                    List<SearchResult> posWeightQLResult = new ArrayList<>();// rm.search("content",
                    // new_terms,
                    // 1500.0, 1000);
                    List<SearchResult> negWeightQLResult = new ArrayList<>();
                    int i = 0;
                    for (; i < qlRes.size(); i++) {
                        int docid = qlRes.get(i).getDocid();
                        double doclen =
                                searcher.getDocLengthReader("content").getLength(docid);

                        TermsEnum iterator =
                                searcher.index.getTermVector(docid, "content").iterator();

                        long freq = 0;

                        BytesRef br;
                        while ((br = iterator.next()) != null) {
                            if (br.utf8ToString().equals(expansionTerms)) {
                                freq = iterator.totalTermFreq();
                                break;
                            }
                        }

                        double score = w * Math.log(freq / doclen);

                        if (freq == 0)
                            score = 0.0;

                        double temp = qlRes.get(i).getScore();
                        SearchResult sr1 = new SearchResult(docid, qlRes.get(i).getDocno(), 0);
                        SearchResult sr2 = new SearchResult(docid, qlRes.get(i).getDocno(), 0);

                        sr1.setScore(temp + score);

                        sr2.setScore(temp - score);
                        posWeightQLResult.add(sr1);
                        negWeightQLResult.add(sr2);

                    }


                    Collections.sort(posWeightQLResult,
                            (o1, o2) -> o2.getScore().compareTo(o1.getScore()));
                    SearchResult.dumpDocno(searcher.index, "docno", posWeightQLResult);

                    double new_p301 =
                            EvalUtils.precision(posWeightQLResult, qrels.get(qid), 30);
                    double new_ap1 =
                            EvalUtils.avgPrec(posWeightQLResult, qrels.get(qid), top);


                    Collections.sort(negWeightQLResult,
                            (o1, o2) -> o2.getScore().compareTo(o1.getScore()));
                    SearchResult.dumpDocno(searcher.index, "docno", negWeightQLResult);

                    double new_p302 =
                            EvalUtils.precision(negWeightQLResult, qrels.get(qid), 30);
                    double new_ap2 =
                            EvalUtils.avgPrec(negWeightQLResult, qrels.get(qid), top);


                    Map<String, String> goodBadExpansionTermMap = new HashMap<>();
                    double posWeightChange = (new_ap1 - ap);
                    double negWeightChange = (new_ap2 - ap);
                    if (posWeightChange > 0 && negWeightChange < 0) {
                        goodBadExpansionTermMap.put(expansionTerms, "+1");
                        gcount++;
                    } else if (posWeightChange < 0 && negWeightChange > 0) {
                        goodBadExpansionTermMap.put(expansionTerms, "-1");
                        bcount++;
                    } else {
                        goodBadExpansionTermMap.put(expansionTerms, "0");
                        ncount++;
                    }



          pw.println(expansionTerms+" "+goodBadExpansionTermMap.get(expansionTerms)+" 1:"+feature1Map.get(expansionTerms)+" 2:"+feature2Map.get(expansionTerms)
          +" 3:"+feature3Map.get(expansionTerms)+" 4:"+feature4Map.get(expansionTerms)+" 5:"+feature5Map.get(expansionTerms)+
          " 6:"+feature7Map.get(expansionTerms)+" 7:"+feature8Map.get(expansionTerms)+" 8:"+feature9Map.get(expansionTerms)+
          " 9:"+feature10Map.get(expansionTerms));
                }
               sum += expansionTermsList.size();

//                 System.out.println(qid+" good words : "+gcount+" bad words : "+bcount+" neutral words : "+ ncount);
                pw.close();

            } catch(Exception e) {
               continue;
            }
       }
      System.out.println("no of good: "+gcount+" no of bad: "+bcount+" no of neutral: "+ncount+" total number of terms: "+sum);

  }

  public static Map<String,Double> getCorrectedExpandedTerms(
          Map<String, Double> ql_map, List<SearchResult> ser,
          LuceneQLSearcher searcher) throws IOException {
    Map<String, Double> CorrectedExpandedTerms = new TreeMap<>();
    for (String expTerms : ql_map.keySet()) {
      int count = 0;
      for (int i = 0; i < Math.min(20,ser.size()); i++) {
        int docid = ser.get(i).getDocid();
        TermsEnum expTermVector =
                searcher.index.getTermVector(docid, "content").iterator();
        BytesRef br;
        double freq = 0;
        while ((br = expTermVector.next()) != null) {
          String term = br.utf8ToString();
          if (term.equals(expTerms)) {
            freq = expTermVector.totalTermFreq();
            break;
          }
        }
        if (freq > 0) {
          count++;
        }
      }
      if (count >= 3) {
        CorrectedExpandedTerms.put(expTerms,ql_map.get(expTerms));
      }
    }

    return CorrectedExpandedTerms;
  }

// function to scale the feature
    public static Map<String,Double> scalingFeatures(Map<String ,Double> features)
    {
        Map<String,Double> feature=new HashMap<>();
        double max=0,min=0;
        max=Collections.max(features.values());
        min=Collections.min(features.values());
        for(Map.Entry<String,Double> entry:features.entrySet())
        {
            double score=0;
            if(max-min!=0)
                score=(entry.getValue()-min)/(max-min);
            feature.put(entry.getKey(),score);
        }
        return feature;
    }

}

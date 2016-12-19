import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SetOnce;

public class Features {
  public static Map<String, Double> term_distrib(IndexReader index,
      Map<String, Double> fbterms, List<SearchResult> fbresults,
      List<String> query_terms, LuceneQLSearcher searcher, int numfbdocs)
      throws IOException {

    double total_fbtermfreq = 0.0;
    Map<String, Double> dist = new HashMap<String, Double>();
    List<Integer> fbdocs = new ArrayList<Integer>();
    FileDocLengthReader flen =
        new FileDocLengthReader(new File("C:\\Users\\jannu bhai\\IdeaProjects\\IRProjectVersion2\\index_trec123"), "content");

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

  public static Map<String, HashMap<String, Double>> single_cooccur(
      IndexReader index,
      Map<String, Double> fbterms, List<SearchResult> fbresults,
      List<String> qterms, double denom) throws IOException {

    List<Integer> fbdocs = new ArrayList<Integer>();

    for (SearchResult sr : fbresults) {
      int docid = sr.getDocid();
      fbdocs.add(docid);
    }

    Map<String, HashMap<String, Double>> cooccur_count =
        new HashMap<String, HashMap<String, Double>>();

    for (String q : qterms) {
      PostingsEnum qposting = MultiFields.getTermDocsEnum(index, "content",
          new BytesRef(q), PostingsEnum.POSITIONS);

      double count = 0.0;

      int qdocid = qposting.nextDoc();
      if (qposting != null) {
        for (String key : fbterms.keySet()) {
          PostingsEnum tposting = MultiFields.getTermDocsEnum(index, "content",
              new BytesRef(key), PostingsEnum.POSITIONS);
          int tdocid = tposting.nextDoc();

          while (qdocid != PostingsEnum.NO_MORE_DOCS
              || tdocid != PostingsEnum.NO_MORE_DOCS) {
            // System.out.println("qterm : " + q + " term : " + key
            // + " qdocid : " + qdocid + " tdocid : " + tdocid);
            if (qdocid == tdocid && fbdocs.contains(tdocid)) {
              int qfreq = qposting.freq();
              int tfreq = tposting.freq();

              for (int i = 0; i < Math.min(qfreq, tfreq); i++) {
                int qpos = qposting.nextPosition();
                int tpos = tposting.nextPosition();
                System.out.println("qterm : " + q + " term : " + key
                    + " qpos : " + qpos + " tpos : " + tpos);


                if (Math.abs(qpos - tpos) <= 12) {
                  count++;
                }

              }

            }
            qdocid = qposting.nextDoc();
            tdocid = tposting.nextDoc();
          }

          HashMap<String, Double> temp = cooccur_count.get(q);
          if (temp == null) {
            Map<String, Double> c = new HashMap<String, Double>();
            c.put(key, Math.log(count / denom));
            cooccur_count.put(q, (HashMap<String, Double>) c);
          }

          else
            cooccur_count.get(q).put(key, Math.log(count / denom));
          // System.out
          // .println("qterm : " + q + " term : " + key + " count : " + count);

        }
      }
    }


    return cooccur_count;
  }

  public static void main(String[] args) throws IOException {

    String pathIndex = "C:\\Users\\jannu bhai\\IdeaProjects\\IRProjectVersion2\\index_trec123";
    Analyzer analyzer = LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);

    String pathQueries = "C:\\Users\\jannu bhai\\IdeaProjects\\IRProjectVersion2\\queries_trec1-3"; // change it to your
    // query file path
    String pathQrels = "C:\\Users\\jannu bhai\\IdeaProjects\\IRProjectVersion2\\qrels_trec1-3"; // change it to your
    // qrels file path
    String pathStopwords = "C:\\Users\\jannu bhai\\IdeaProjects\\IRProjectVersion2\\stopwords_inquery";
    String field_docno = "docno";
    String field_search = "content";
      //String indexName = "index_trec123";
     String outputFolder = "C:\\Users\\jannu bhai\\IdeaProjects\\IRProjectVersion2\\indexTrec123OP\\BaseLineRM1"; //change based on setting
    // RM searcher = new RM(pathIndex);
    LuceneQLSearcher searcher = new LuceneQLSearcher(pathIndex);
    searcher.setStopwords(pathStopwords);
    System.out.println("entering main");
    Map<String, String> queries = EvalUtils.loadQueries(pathQueries);
    Map<String, Set<String>> qrels = EvalUtils.loadQrels(pathQrels);
    //String qid="51";
    int top = 1000;
    double mu = 1000;
    int numfbdocs = 20;
    for (String qid : queries.keySet()) {

      String query = queries.get(qid);// qid);
      List<String> terms = LuceneUtils.tokenize(query, analyzer);
    //  System.out.println(qid);
        File file = new File(outputFolder+"\\"+qid);
        PrintWriter pw = new PrintWriter(file);
      RM rm = new RM(pathIndex);
      Map<String, Double> scoresRM1 = searcher.estimateQueryModelRM1(
          field_search, terms, 1500.0, 0.0, 20, 80);
     // System.out.println(qid);
      List<SearchResult> qlRes = rm.search("content", terms, 1500.0, 1000);
      //System.out.println(qid);
      List<String> expansionTermsList = getCorrectedExpandedTerms(scoresRM1, qlRes, searcher);
      System.out.println(expansionTermsList.size());
        Feature_jan feature=new Feature_jan();
      SearchResult.dumpDocno(searcher.index, "docno", qlRes);
    //  feature.feature3(searcher.index,expansionTermsList,qlRes,terms,20);
     Map<String,Double> feature1Map=scalingFeatures(feature.feature1(searcher.index,expansionTermsList,qlRes,20));
      System.out.print(feature1Map.size());
       Map<String,Double> feature2Map=scalingFeatures(feature.feature2(searcher.index,expansionTermsList,searcher));
      System.out.print(feature2Map.size());
        Map<String,Double> feature3Map=scalingFeatures(feature.feature3(searcher.index,expansionTermsList,qlRes,terms,20));
      System.out.print(feature3Map.size());
       Map<String,Double> feature4Map=scalingFeatures(feature.feature4(expansionTermsList,terms,searcher));
      System.out.print(feature4Map.size());
        Map<String,Double> feature5Map=scalingFeatures(feature.feature5(searcher.index,expansionTermsList,qlRes,terms));
      System.out.print(feature5Map.size());
      //  Map<String,Double> feature6Map=scalingFeatures(feature.feature6(expansionTermsList,terms,searcher));
        Map<String,Double> feature7Map=scalingFeatures(feature.feature7(searcher.index,expansionTermsList,terms,qlRes));
      System.out.print(feature7Map.size());
        Map<String,Double> feature8Map=scalingFeatures(feature.feature8(expansionTermsList,terms,searcher));
      System.out.print(feature8Map.size());
        Map<String,Double> feature9Map=scalingFeatures(feature.feature9(searcher.index,qlRes,terms,expansionTermsList));
      System.out.print(feature9Map.size());
        Map<String,Double> feature10Map=scalingFeatures(feature.feature10(searcher,terms,expansionTermsList));
      System.out.print(feature10Map.size());

       // System.out.println("sssssws");
      double p30 = EvalUtils.precision(qlRes, qrels.get(qid), 30);
      double ap = EvalUtils.avgPrec(qlRes, qrels.get(qid), top);
      System.out.println(qid);
      System.out.println("AP :" + ap + " P@30 " + p30);
      int gcount = 0, bcount = 0, ncount = 0;
      double w = 0.01;
      for (String expansionTerms : expansionTermsList) {
       // System.out.println(expansionTerms);
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
            SearchResult sr2 = new SearchResult(docid, qlRes.get(i).getDocno(),0);

            sr1.setScore(temp+score);

            sr2.setScore(temp-score);
          posWeightQLResult.add(sr1);
          negWeightQLResult.add(sr2);
          // System.out.println("Document id " + docid);

          /*  System.out.println("docid : " + docid + " Original RM1 score : " +
            temp + " +w score : " + posWeightQLResult.get(i).score +
            " -w score : " + negWeightQLResult.get(i).score + " added score "+score);
*/
        }

        // SearchResult.dumpDocno(searcher.index, "docno", RM1res1);
        Collections.sort(posWeightQLResult,
            (o1, o2) -> o2.getScore().compareTo(o1.getScore()));
        SearchResult.dumpDocno(searcher.index, "docno", posWeightQLResult);

          double new_p301 =
            EvalUtils.precision(posWeightQLResult, qrels.get(qid), 30);
        double new_ap1 =
            EvalUtils.avgPrec(posWeightQLResult, qrels.get(qid), top);

        //System.out.println("w = 0.01 AP :" + new_ap1 + " P@30 " + new_p301);

        Collections.sort(negWeightQLResult,
            (o1, o2) -> o2.getScore().compareTo(o1.getScore()));
        SearchResult.dumpDocno(searcher.index, "docno", negWeightQLResult);

        double new_p302 =
            EvalUtils.precision(negWeightQLResult, qrels.get(qid), 30);
        double new_ap2 =
            EvalUtils.avgPrec(negWeightQLResult, qrels.get(qid), top);

        //System.out.println("w = -0.01 AP :" + new_ap2 + " P@30 " + new_p302);
          Map<String,String> goodBadExpansionTermMap = new HashMap<>();
          double posWeightChange = (new_ap1 - ap);
          double negWeightChange = (new_ap2 - ap);
          if( posWeightChange>0 && negWeightChange<0)
          {
              goodBadExpansionTermMap.put(expansionTerms,"+1");
              gcount++;
          }
          else if(  posWeightChange<0 && negWeightChange>0 )
          {
              goodBadExpansionTermMap.put(expansionTerms,"-1");
              bcount++;
          }
          else
          {
              goodBadExpansionTermMap.put(expansionTerms,"0");
              ncount++;
          }

          pw.println(goodBadExpansionTermMap.get(expansionTerms)+" 1:"+feature1Map.get(expansionTerms)+" 2:"+feature2Map.get(expansionTerms)
          +" 3:"+feature3Map.get(expansionTerms)+" 4:"+feature4Map.get(expansionTerms)+" 5:"+feature5Map.get(expansionTerms)+
          " 6:0.0"+" 7:"+feature7Map.get(expansionTerms)+" 8:"+feature8Map.get(expansionTerms)+" 9:"+feature9Map.get(expansionTerms)+
          " 10:"+feature10Map.get(expansionTerms));
      }

   // System.out.println(qid+" good words : "+gcount+" bad words : "+bcount+" neutral words : "+ ncount);
    pw.close();
   }
    /*
     * Map<String, Double> word_distrib = term_distrib(searcher.index,
     * scoresRM1, RM1res, terms, searcher, 10);
     */

    /*
     * for (String key : word_distrib.keySet()) { System.out.println(key + " : "
     * + word_distrib.get(key)); }
     */

    /*
     * Terms vector = null; Map<String, Double> vocabulary = new HashMap<String,
     * Double>(); double doclen[] = new double[numfbdocs]; double denom = 0; //
     * System.out.println("Results size : " + results.size()); for (int i = 0; i
     * < Math.min(numfbdocs, RM1res.size()); i++) {
     * 
     * int dnum = RM1res.get(i).getDocid(); vector =
     * searcher.index.getTermVector(dnum, "content"); // Read the // document's
     * // term vector.
     * 
     * TermsEnum te = vector.iterator();
     * 
     * BytesRef term;
     * 
     * while ((term = te.next()) != null) { if
     * (searcher.stopwords.contains(term.utf8ToString()) != true) { String
     * termstr = term.utf8ToString(); // Get the text string of the // term.
     * double freq = te.totalTermFreq(); // Get the frequency of the term in //
     * the document. if ((Double) freq == null) freq = 0.0;
     * 
     * vocabulary.put(termstr, vocabulary.getOrDefault(termstr, 0.0) + freq);
     * doclen[i] += freq; denom += freq; } } }
     * 
     * Map<String, HashMap<String, Double>> res = single_cooccur(searcher.index,
     * scoresRM1, RM1res, terms, denom);
     * 
     * 
     * for (String key : res.keySet()) { HashMap<String, Double> temp =
     * res.get(key);
     * 
     * for (String key1 : temp.keySet()) System.out.println("query term : " +
     * key + " term : " + key1 + " score : " + temp.get(key1)); }
     * 
     * 
     * SMM smm = new SMM(); smm.estimateSMM("content", terms, 0.8, numfbdocs,
     * Integer.MAX_VALUE);
     */

  }

  public static List<String> getCorrectedExpandedTerms(
          Map<String, Double> ql_map, List<SearchResult> ser,
          LuceneQLSearcher searcher) throws IOException {
    List<String> CorrectedExpandedTerms = new ArrayList<>();
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
        CorrectedExpandedTerms.add(expTerms);
      }
    }

    return CorrectedExpandedTerms;
  }


    public static Map<String,Double> scalingFeatures(Map<String ,Double> features)
    {
        Map<String,Double> feature=new HashMap<>();
        double max=0,min=0;
        max=Collections.max(features.values());
        min=Collections.min(features.values());
        for(Map.Entry<String,Double> entry:features.entrySet())
        {
            double score=(entry.getValue()-min)/(max-min);
            feature.put(entry.getKey(),score);
        }
        return feature;
    }

}

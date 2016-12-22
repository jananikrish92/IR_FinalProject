import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class RM extends AbstractQLSearcher {

  /*
   * public void RM3(Map<String, String> queries, Map<String, Set<String>>
   * qrels) throws IOException {
   * 
   * Analyzer analyzer = LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);
   * double[] p10 = new double[queries.size()]; double[] ap = new
   * double[queries.size()]; int top = 1000;
   * System.out.println("RM3 scores : "); for (double lambdaOrg = 0.0; lambdaOrg
   * <= 1.0; lambdaOrg += 0.1) { // int ix = 0; for (String qid :
   * queries.keySet()) {
   * 
   * String query = queries.get(qid); List<String> terms =
   * LuceneUtils.tokenize(query, analyzer); String[] termsarray = new
   * String[terms.size()]; for (int k = 0; k < termsarray.length; k++) {
   * termsarray[k] = terms.get(k); } Map<String, Double> RM1Score =
   * this.estimateQueryModelRM1("content", terms, 1000.0, 0.0, 10, 100); //
   * List<SearchResult> QLscores = this.search("content", terms, 1000.0, //
   * 1000);
   * 
   * Map<String, Double> RM3Score = new HashMap<>(); Set<String> voc = new
   * HashSet<String>(); voc.addAll(RM1Score.keySet()); voc.addAll(terms);
   * 
   * for (String t : voc) { double score_rm1 = (1.0 - lambdaOrg) *
   * RM1Score.getOrDefault(t, 0.0); double score_ql = lambdaOrg * (((double)
   * Collections.frequency(terms, t) / (double) terms.size()));
   * 
   * RM3Score.put(t, score_rm1 + score_ql); }
   * 
   * List<Entry<String, Double>> fbscores = mapSortByValues(RM3Score);
   * 
   * Map<String, Double> finalScores = new HashMap<String, Double>();
   * 
   * for (int i = 0; i < 100; i++) { Entry<String, Double> temp =
   * fbscores.get(i); finalScores.put(temp.getKey(), temp.getValue()); }
   * 
   * List<SearchResult> res = this.search("content", finalScores, 1000.0, top);
   * 
   * SearchResult.dumpDocno(this.index, "docno", res);
   * 
   * p10[ix] = EvalUtils.precision(res, qrels.get(qid), 10); ap[ix] =
   * EvalUtils.avgPrec(res, qrels.get(qid), top);
   * 
   * ix++;
   * 
   * }
   * 
   * System.out.println("lambdaOrg = " + lambdaOrg);
   * 
   * System.out.printf("%-10s%-25s%10.3f%10.3f\n", "QL", "QL",
   * StatUtils.mean(p10), StatUtils.mean(ap));
   * 
   * } }
   * 
   * // Get top m docs using QL. Run RM1. Then use entire RM1 language model to
   * // re-rank ( so query includes entire RM1 return ) // Step 1: retrieve the
   * top m results using QL.
   * 
   * // Step 2: recalculate the score for each of the m results using //
   * KL-Divergence, where the query model is RM1 (considering all terms in V).
   * 
   * // Step 3: rerank the m results by the new score. public void
   * reranking(Map<String, String> queries, Map<String, Set<String>> qrels)
   * throws IOException {
   * 
   * // Map<String, Double> scoreNew ; Analyzer analyzer =
   * LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);
   * 
   * Fields fields = MultiFields.getFields(this.index);
   * 
   * Terms totalWords = fields.terms("content");
   * 
   * long corpusSize = totalWords.getSumTotalTermFreq();
   * 
   * for (int m = 500; m <= 5000; m += 500) { int ix = 0;
   * 
   * double[] p10RM = new double[queries.size()]; double[] apRM = new
   * double[queries.size()];
   * 
   * double[] p10normal = new double[queries.size()]; double[] apnormal = new
   * double[queries.size()];
   * 
   * for (String qid : queries.keySet()) {
   * 
   * String query = queries.get(qid); List<String> terms =
   * LuceneUtils.tokenize(query, analyzer); String[] termsarray = new
   * String[terms.size()]; for (int k = 0; k < termsarray.length; k++) {
   * termsarray[k] = terms.get(k); }
   * 
   * List<SearchResult> results = this.search("content", terms, 1000.0, m);
   * SearchResult.dumpDocno(this.index, "docno", results); Map<String, Double>
   * scores = this.estimateQueryModelRM1("content", terms, 1000.0, 0.0, 10,
   * Integer.MAX_VALUE);
   * 
   * // load document vectors of top m documents
   * 
   * List<Terms> docVectors = new ArrayList<>(results.size()); List<String>
   * docnos = new ArrayList<>(results.size()); for (int i = 0; i <
   * results.size(); i++) { SearchResult tempRes = results.get(i); String docno
   * = tempRes.getDocno(); // System.out.println("docno = " + docno); int docID
   * = LuceneUtils.findByDocno(this.index, "docno", docno);
   * 
   * docVectors.add(this.index.getTermVector(docID, "content"));
   * docnos.add(docno); }
   * 
   * 
   * // Map<String, Double> KLDscores = new HashMap<String, Double>();
   * 
   * // List<SearchResult> newRankedResults = // this.search("content", scores,
   * 1000.0, m); // new ArrayList<>();
   * 
   * List<SearchResult> newRankedResults = new ArrayList<SearchResult>(); double
   * mu = 1000.0; for (SearchResult sr : results) {
   * 
   * String docno = sr.getDocno(); int docID = sr.getDocid(); double docScore =
   * 0.0; double PtQ = 0.0; Terms vectord = this.index.getTermVector(docID,
   * "content"); TermsEnum ted = vectord.iterator(); BytesRef brd; double doclen
   * = 0.0;
   * 
   * while ((brd = ted.next()) != null) doclen += ted.totalTermFreq();
   * 
   * for (String t : scores.keySet()) { PtQ = scores.get(t); double PtCorpus =
   * (double) this.index.totalTermFreq(new Term("content", t)) / (double)
   * corpusSize; Terms vector = this.index.getTermVector(docID, "content");
   * TermsEnum te = vector.iterator(); BytesRef br; double freq = 0.0;
   * 
   * while ((br = te.next()) != null) { String termstr = br.utf8ToString(); if
   * (t.equals(termstr)) { freq = te.totalTermFreq(); break; } }
   * 
   * docScore += PtQ * Math.log((freq + mu * PtCorpus) / (doclen + mu));
   * 
   * }
   * 
   * SearchResult obj = new SearchResult(docID, docno, docScore);
   * 
   * newRankedResults.add(obj); }
   * 
   * SearchResult.dumpDocno(this.index, "docno", newRankedResults);
   * Collections.sort(newRankedResults);
   * 
   * p10RM[ix] = EvalUtils.precision(newRankedResults, qrels.get(qid), 10);
   * apRM[ix] = EvalUtils.avgPrec(newRankedResults, qrels.get(qid), m);
   * 
   * p10normal[ix] = EvalUtils.precision(results, qrels.get(qid), 10);
   * apnormal[ix] = EvalUtils.avgPrec(results, qrels.get(qid), m); ix++; }
   * 
   * System.out.println(
   * "Mean precision@10 and AP for normal QL with number of top docs = " + m);
   * System.out.printf("%-10s%-25s%10.3f%10.3f\n", "QL", "QL",
   * StatUtils.mean(p10normal), StatUtils.mean(apnormal));
   * 
   * System.out.println("Number of top docs reranked = " + m);
   * System.out.printf("%-10s%-25s%10.3f%10.3f\n", "QL", "QL",
   * StatUtils.mean(p10RM), StatUtils.mean(apRM));
   * 
   * }
   * 
   * }
   * 
   * public static void main(String[] args) { try {
   * 
   * String pathIndex = "index_lucene_robust04"; Analyzer analyzer =
   * LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);
   * 
   * String pathQueries = "queries"; // change it to your // query file path
   * String pathQrels = "qrels"; // change it to your // qrels file path String
   * pathStopwords = "stopwords_inquery"; String field_docno = "docno"; String
   * field_search = "content";
   * 
   * RM searcher = new RM(pathIndex); searcher.setStopwords(pathStopwords);
   * 
   * Map<String, String> queries = EvalUtils.loadQueries(pathQueries);
   * Map<String, Set<String>> qrels = EvalUtils.loadQrels(pathQrels);
   * 
   * int top = 1000; double mu = 1000;
   * 
   * double[] p10 = new double[queries.size()]; double[] ap = new
   * double[queries.size()];
   * 
   * double[] p10RM1 = new double[queries.size()]; double[] apRM1 = new
   * double[queries.size()];
   * 
   * double total = 0;
   * 
   * searcher.reranking(queries, qrels);
   * 
   * // searcher.RM3(queries, qrels);
   * 
   * for (double mu2 = 0.0; mu2 <= 2500.0; mu2 += 500.0) {
   * System.out.println("muFB = " + mu2); int ix = 0; for (String qid :
   * queries.keySet()) {
   * 
   * String query = queries.get(qid); List<String> terms =
   * LuceneUtils.tokenize(query, analyzer); String[] termsarray = new
   * String[terms.size()]; for (int k = 0; k < termsarray.length; k++) {
   * termsarray[k] = terms.get(k); }
   * 
   * List<SearchResult> results = searcher.search(field_search, terms, mu, top);
   * SearchResult.dumpDocno(searcher.index, field_docno, results);
   * 
   * p10[ix] = EvalUtils.precision(results, qrels.get(qid), 10); ap[ix] =
   * EvalUtils.avgPrec(results, qrels.get(qid), top);
   * 
   * Map<String, Double> scoresRM1 = searcher
   * .estimateQueryModelRM1(field_search, terms, 1000.0, mu2, 10, 100);
   * 
   * List<Entry<String, Double>> res = mapSortByValues(scoresRM1);
   * 
   * Map<String, Double> finalScores = new HashMap<String, Double>();
   * 
   * for (int i = 0; i < 100; i++) { Entry<String, Double> temp = res.get(i);
   * finalScores.put(temp.getKey(), temp.getValue()); }
   * 
   * 
   * 
   * List<SearchResult> res1 = searcher.search("content", finalScores, 1000.0,
   * top); // searcher.search(field_search, augmentedQuery, mu, top);
   * SearchResult.dumpDocno(searcher.index, field_docno, res1);
   * 
   * p10RM1[ix] = EvalUtils.precision(res1, qrels.get(qid), 10); apRM1[ix] =
   * EvalUtils.avgPrec(results, qrels.get(qid), top);
   * 
   * ix++; }
   * 
   * System.out.printf("%-10s%-25s%10.3f%10.3f\n", "QL", "QL",
   * StatUtils.mean(p10), StatUtils.mean(ap));
   * 
   * System.out.println("Results for RM1 augmented query");
   * System.out.printf("%-10s%-25s%10.3f%10.3f\n", "QL", "QL",
   * StatUtils.mean(p10RM1), StatUtils.mean(apRM1));
   * 
   * }
   * 
   * searcher.close(); } catch (Exception e) { e.printStackTrace(); } }
   */

  protected File dirBase;
  protected Directory dirLucene;
  protected IndexReader index;
  protected Map<String, DocLengthReader> doclens;

  public RM(String dirPath) throws IOException {
    this(new File(dirPath));
  }

  public RM(File dirBase) throws IOException {
    this.dirBase = dirBase;
    this.dirLucene = FSDirectory.open(this.dirBase.toPath());
    this.index = DirectoryReader.open(dirLucene);
    this.doclens = new HashMap<>();
  }

  public IndexReader getIndex() {
    return this.index;
  }

  public PostingList getPosting(String field, String term) throws IOException {
    return new LuceneTermPostingList(index, field, term);
  }

  public DocLengthReader getDocLengthReader(String field) throws IOException {
    DocLengthReader doclen = doclens.get(field);
    if (doclen == null) {
      doclen = new FileDocLengthReader(this.dirBase, field);
      doclens.put(field, doclen);
    }
    return doclen;
  }

  public void close() throws IOException {
    index.close();
    dirLucene.close();
    for (DocLengthReader doclen : doclens.values()) {
      doclen.close();
    }
  }

  public static double scoreDirich(Map<String, Double> tfs,
      Map<String, Double> tfcs, double dl, double cl) {
    System.out.println(tfs + " " + tfcs + " " + dl + " " + cl);
    double temp = 1.0;
    for (String t : tfs.keySet()) {

      temp *= (tfs.get(t) + 1000 * (tfcs.get(t) / cl)) / (1000 + dl);

    }

    return temp;
  }

  public Map<String, Double> getTFS(String field, List<SearchResult> results,
      double mu2, double mu1, int numfbdocs) throws IOException {

    Terms vector = null;

    Map<String, Double> PTCorpus = new HashMap<String, Double>();
    Map<String, Double[]> scoreFB = new HashMap<String, Double[]>();
    Map<String, Double> vocabulary = new HashMap<String, Double>();
    double doclen[] = new double[numfbdocs];
    // System.out.println("Results size : " + results.size());
    for (int i = 0; i < Math.min(numfbdocs, results.size()); i++) {

      int dnum = results.get(i).getDocid();
      vector = this.index.getTermVector(dnum, field); // Read the document's
      // term vector.

      TermsEnum te = vector.iterator();

      BytesRef term;
      Double[] scoreList = null;
      while ((term = te.next()) != null) {
        if (this.stopwords.contains(term.utf8ToString()) != true) {
          String termstr = term.utf8ToString(); // Get the text string of the
          // term.
          double freq = te.totalTermFreq(); // Get the frequency of the term in
          // the document.
          if ((Double) freq == null)
            freq = 0.0;

          vocabulary.put(termstr, vocabulary.getOrDefault(termstr, 0.0) + freq);
          doclen[i] += freq;
          scoreList = scoreFB.get(termstr);

          if (scoreList == null)
            scoreList = new Double[numfbdocs];

          scoreList[i] = (double) freq;

          scoreFB.put(termstr, scoreList);
        }
      }
    }

    Map<String, Double> PtCorpus = new HashMap<String, Double>();

    Fields fields = MultiFields.getFields(this.index);

    Terms totalWords = fields.terms("content");

    long corpusSize = totalWords.getSumTotalTermFreq();

    for (String t : scoreFB.keySet()) {
      double totaltermfreq = this.index.totalTermFreq(new Term("content", t));
      double temp = totaltermfreq / corpusSize;
      PtCorpus.put(t, mu2 * temp);

    }

    Map<String, Double> finalScores = new HashMap<String, Double>();
    for (String t1 : scoreFB.keySet()) {
      double temp = 0.0;
      double res = 0.0;
      Double scoreArr[] = scoreFB.get(t1);
      for (int i = 0; i < Math.min(scoreArr.length, results.size()); i++) {
        if (scoreArr[i] != null)
          temp = (scoreArr[i] + PtCorpus.get(t1));
        else
          temp = (PtCorpus.get(t1));

        temp /= (doclen[i] + mu2);

        res += temp * results.get(i).getScore();

      }
      finalScores.put(t1, res);
    }

    double normalizer = 0.0;

    for (String term : finalScores.keySet())
      normalizer += finalScores.get(term);

    for (String term : finalScores.keySet()) {
      double t = finalScores.get(term);
      finalScores.put(term, t / normalizer);
    }

    return finalScores;
  }

  public Map<String, Double[]> getQTFS(String field, List<String> queryTerms,
      List<SearchResult> results) throws IOException {
    Map<String, Double[]> scoreQuery = new HashMap<String, Double[]>();

    for (String t : queryTerms) {
      for (int i = 0; i < 10; i++) {

        int dnum = results.get(i).getDocid();
        Terms vector = this.index.getTermVector(dnum, field);
        TermsEnum te = vector.iterator();
        BytesRef term;

        Double[] scoreList = null;
        while ((term = te.next()) != null && t.equals(term.utf8ToString())) {
          String termstr = term.utf8ToString();
          double freq = te.totalTermFreq();

          if ((Double) freq == null)
            freq = 0.0;

          scoreList = scoreQuery.get(termstr);

          if (scoreList == null)
            scoreList = new Double[10];

          scoreList[i] = (double) freq;

          scoreQuery.put(termstr, scoreList);

        }
      }
    }
    return scoreQuery;
  }

  public List<SearchResult> estimateQueryModelRM1(String field,
      List<String> terms, double mu1, double mu2, int numfbdocs, int numfbterms)
      throws IOException {

    List<SearchResult> results = this.search(field, terms, mu2, numfbdocs);


    // Map<String, Double> scores =this.getTFS(field, results, mu2, 1000.0,
    // numfbdocs);

    return results;

  }

  static <K, V extends Comparable<? super V>> List<Entry<K, V>> mapSortByValues(
      Map<K, V> map) {

    List<Entry<K, V>> sortedEntries =
        new ArrayList<Entry<K, V>>(map.entrySet());

    Collections.sort(sortedEntries, new Comparator<Entry<K, V>>() {
      @Override
      public int compare(Entry<K, V> e1, Entry<K, V> e2) {
        return e2.getValue().compareTo(e1.getValue());
      }
    });

    return sortedEntries;
  }

}
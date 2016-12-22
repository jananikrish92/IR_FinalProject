import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LuceneQLSearcher extends AbstractQLSearcher {

  protected File dirBase;
  protected Directory dirLucene;
  protected IndexReader index;
  protected Map<String, DocLengthReader> doclens;

  public LuceneQLSearcher(String dirPath) throws IOException {
    this(new File(dirPath));
  }

  public LuceneQLSearcher(File dirBase) throws IOException {
    this.dirBase = dirBase;
    this.dirLucene = FSDirectory.open(this.dirBase.toPath());
    this.index = DirectoryReader.open(dirLucene);
    this.doclens = new HashMap<>();
    this.setStopwords("/Users/jananikrishna/Documents/IRFinalProject/stopwords_inquery");
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

  public Map<String, Double> estimateQueryModelRM1(String field,
      List<String> terms, double mu, double mufb, int numfbdocs, int numfbterms)
      throws IOException {

    List<SearchResult> results = search(field, terms, mu, numfbdocs);
    Set<String> voc = new HashSet<>();
    for (SearchResult result : results) {
      TermsEnum iterator =
          index.getTermVector(result.getDocid(), field).iterator();
      BytesRef br;
      while ((br = iterator.next()) != null) {
        if (!isStopwords(br.utf8ToString())) {
          voc.add(br.utf8ToString());
        }
      }
    }

    Map<String, Double> collector = new HashMap<>();
    for (SearchResult result : results) {
      double ql = result.getScore();
      double dw = Math.exp(ql);
      TermsEnum iterator =
          index.getTermVector(result.getDocid(), field).iterator();
      Map<String, Integer> tfs = new HashMap<>();
      int len = 0;
      BytesRef br;
      while ((br = iterator.next()) != null) {
        tfs.put(br.utf8ToString(), (int) iterator.totalTermFreq());
        len += iterator.totalTermFreq();
      }
      for (String w : voc) {
        int tf = tfs.getOrDefault(w, 0);
        double pw = (tf + mufb * index.totalTermFreq(new Term(field, w))
            / index.getSumTotalTermFreq(field)) / (len + mufb);
        collector.put(w, collector.getOrDefault(w, 0.0) + pw * dw);
      }
    }
    collector = Features.getCorrectedExpandedTerms(collector,results,this);
    collector = SMM.sortByValue(collector);
    return Utils.getTop(Utils.norm(collector), numfbterms);
  }

  public Map<String, Double> estimateQueryModelRM3(List<String> terms,
      Map<String, Double> rm1, double weight_org) throws IOException {

    Map<String, Double> mle = new HashMap<>();
    for (String term : terms) {
      mle.put(term, mle.getOrDefault(term, 0.0) + 1.0);
    }
    for (String w : mle.keySet()) {
      mle.put(w, mle.get(w) / terms.size());
    }

    Set<String> v = new TreeSet<>();
    v.addAll(terms);
    v.addAll(rm1.keySet());

    Map<String, Double> rm3 = new HashMap<>();
    for (String w : v) {
      rm3.put(w, weight_org * mle.getOrDefault(w, 0.0)
          + (1 - weight_org) * rm1.getOrDefault(w, 0.0));
    }
    List<SearchResult> results = search("content",terms,1500,20);
    rm3 = Features.getCorrectedExpandedTerms(rm3,results,this);
    rm3 = SMM.sortByValue(rm3);

    return rm3;
  }



}
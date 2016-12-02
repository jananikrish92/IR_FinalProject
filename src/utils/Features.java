package utils;

import org.apache.lucene.analysis.payloads.IntegerEncoder;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by jannu bhai on 11/28/2016.
 */
public class Features {

    public void feature1(IndexReader index, Map<String, Double> ql_map, List<SearchResult> ser) throws IOException {
        // System.out.println(ql_map.size());
        double denom = 0;
        // String pathStopwords = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\stopwords_inquery";
        //LuceneQLSearcher searcher=new LuceneQLSearcher(pathStopwords);
        //searcher.setStopwords(pathStopwords);
        double tot_freq = 0;
      /*  for(String expanded:ql_map.keySet())
        {
            System.out.println(expanded);
        }*/

        Map<String, Double> Term_dist_FD = new TreeMap<>();
        Map<Integer, Map<String, Double>> Docwise_wordfreq_map = new TreeMap<>();
        for (int i = 0; i < ser.size(); i++) {
            int doc_id = ser.get(i).getDocid();
            //String docno=ser.get(i).getDocno();
            TermsEnum ter = index.getTermVector(doc_id, "content").iterator();
            Map<String, Double> wordFreq = new TreeMap<String, Double>();

            BytesRef br;
            // form word frquency map
            //   System.out.println(doc_id + "==");
            while ((br = ter.next()) != null) {
                if (!AbstractQLSearcher.stopwords.contains(br.utf8ToString())) {
                    String term = br.utf8ToString();
                    double freq = ter.totalTermFreq();
                    //             docLen += freq;
                    //   System.out.println(term+":"+freq);
                    tot_freq += freq;
                    if (wordFreq.containsKey(term)) {
                        wordFreq.put(term, wordFreq.get(term) + freq);
                    } else {
                        wordFreq.put(term, freq);
                    }
                }
            }
            Docwise_wordfreq_map.put(doc_id, wordFreq);
        }

        for (String expand_terms : ql_map.keySet()) {
            double Exapanded_terms_freq = 0;
            double score = 0;
            for (int i = 0; i < ser.size(); i++) {
                int docid = ser.get(i).getDocid();
                for (Map.Entry<String, Double> word_freq : Docwise_wordfreq_map.get(docid).entrySet()) {
                    if (word_freq.getKey().contains(expand_terms)) {
                        Exapanded_terms_freq += word_freq.getValue();
                    }
                }
            }
            score = Math.log(Exapanded_terms_freq / tot_freq);
            Term_dist_FD.put(expand_terms, score);
        }
        for (Map.Entry<String, Double> map : Term_dist_FD.entrySet()) {
            System.out.println(map.getKey() + "==>" + map.getValue());
        }
        //  return null;
    }

    public void feature3(IndexReader index, Map<String, Double> ql_map, List<SearchResult> ser) throws IOException {
        List<Integer> FeedbackDocList=new ArrayList<>();
        for(int i=0;i<ser.size();i++) {
            FeedbackDocList.add(ser.get(i).getDocid());
        }
      /*  for(String ter:ql_map.keySet())
        {
            System.out.println(ter);
        }*/

      String term="airbus";
       // for (String term : ql_map.keySet()) {
            System.out.println(term+":");
            PostingsEnum posting = MultiFields.getTermDocsEnum(index, "content", new BytesRef(term), PostingsEnum.POSITIONS);
            if (posting != null) { // if the term does not appear in any document, the posting object may be null
                int docid;
                while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    if (FeedbackDocList.contains(docid)) {
                        int freq = posting.freq(); // get the frequency of the term in the current document
                        System.out.printf("%-10d%-10d", docid, freq);
                        for (int i = 0; i < freq; i++) {
                            System.out.print((i > 0 ? "," : "") + posting.nextPosition());
                        }
                        System.out.println();
                    }
                }
           // }
        }
    }

}
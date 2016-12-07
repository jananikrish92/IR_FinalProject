package utils;


import org.apache.commons.math3.util.Pair;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import java.io.IOException;
import java.util.*;

/**
 * Created by jannu bhai on 11/28/2016.
 */
public class Features {
    static Map<String, Map<Integer, List<Integer>>> Qterm_Doc_Positions = new HashMap<>();
    static Map<Pair<String,String>,Integer> CoccuranceMap=new HashMap<>();
    public void feature1(IndexReader index, Map<String, Double> ql_map, List<SearchResult> ser) throws IOException {
        // System.out.println(ql_map.size());
        double denom = 0;
        // String pathStopwords = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\stopwords_inquery";
        //LuceneQLSearcher searcher=new LuceneQLSearcher(pathStopwords);
        //searcher.setStopwords(pathStopwords);

      /*  for(String expanded:ql_map.keySet())
        {
            System.out.println(expanded);
        }*/
        double tot_freq = 0;
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

    public Map<String, Double> feature3(IndexReader index, Map<String, Double> ql_map, List<SearchResult> ser, List<String> qterms) throws IOException {
        List<Integer> FeedbackDocList = new ArrayList<>();
        int numfb = 20;
        double tot_freq = 0;
        for (int i = 0; i < Math.min(numfb, ser.size()); i++) {
            FeedbackDocList.add(ser.get(i).getDocid());
        }
//       System.out.println(ser.size());
        for (int i = 0; i < Math.min(numfb, ser.size()); i++) {
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
                }
            }
        }
        //      System.out.println(tot_freq);

        QueryDocumentPosition(qterms, index, FeedbackDocList);
        Map<String, Double> feature3Map = new HashMap<>();
        for (String term : ql_map.keySet()) {
            //System.out.println(term + ":");
            PostingsEnum posting = MultiFields.getTermDocsEnum(index, "content", new BytesRef(term), PostingsEnum.POSITIONS);
            double score_of_n_qterms = 0;
            for (String queryTerms : Qterm_Doc_Positions.keySet()) {
                int Coccurance_tot=0;
                Map<Integer, List<Integer>> MapList_of_Doc_position = Qterm_Doc_Positions.get(queryTerms);
                int Coccurance_count = 0;
                if (posting != null) { // if the term does not appear in any document, the posting object may be null
                    int docid;
                    while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        if (FeedbackDocList.contains(docid)) {
                            int freq = posting.freq();
                            if (freq >= 3) {
                                for (int i = 0; i < freq; i++) {
                                    int position_of_expansionTerm = posting.nextPosition();
                                    if (MapList_of_Doc_position.containsKey(docid)) {
                                        for (Integer pos : MapList_of_Doc_position.get(docid)) {
                                            if (Math.abs(freq - pos) <= 12) {
                                                Coccurance_count++;
                                            }
                                        }
                                    }
                                }
                            }
                            //System.out.println();
                        }
                    }
                  //  Coccurance_tot+=Coccurance_count;
                }

                CoccuranceMap.put(new Pair<String,String>(queryTerms,term),Coccurance_count);
                score_of_n_qterms += Coccurance_count / tot_freq;
            }
            // System.out.println(score_of_n_qterms);

            double score = Math.log((double) score_of_n_qterms / Qterm_Doc_Positions.keySet().size());
            feature3Map.put(term, score);
            //      System.out.println(term + ":" + score);
        }
        return feature3Map;
    }

    public void QueryDocumentPosition(List<String> qterms,IndexReader index,List<Integer> FBDocList) throws IOException
    {
        for (String Qterm : qterms) {
            PostingsEnum post_qterm = MultiFields.getTermDocsEnum(index, "content", new BytesRef(Qterm), PostingsEnum.POSITIONS);
            Map<Integer, List<Integer>> Doc_Postion_Map = new TreeMap<>();
            if (post_qterm != null) {
                int docId;
                while ((docId = post_qterm.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    List<Integer> PositionList = new ArrayList<>();
                    if (FBDocList.contains(docId)) {
                        int freq = post_qterm.freq();
                        for (int i = 0; i < freq; i++) {
                            PositionList.add(post_qterm.nextPosition());
                        }
                        Doc_Postion_Map.put(docId, PositionList);
                    }

                }
            }
            Qterm_Doc_Positions.put(Qterm, Doc_Postion_Map);
        }
    }
    public void feature5(IndexReader index, Map<String, Double> ql_map, List<SearchResult> ser, List<String> qterms) throws IOException
    {
        List<Integer> FeedbackDocList = new ArrayList<>();
        int numfb=20;
        for (int i = 0; i <Math.min(numfb,ser.size()); i++) {
            FeedbackDocList.add(ser.get(i).getDocid());
        }
        double tot_freq=0;
        for (int i = 0; i <Math.min(numfb,ser.size()); i++) {
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
                }
            }
        }
        QueryDocumentPosition(qterms,index,FeedbackDocList);
        Map<Pair<String,String>,Set<Integer>> pairSetMap=new HashMap<>();
       // Pair<String,String> p=null;
        for(int i=0;i<qterms.size();i++)
        {
            Map<Integer,List<Integer>> Term1_Doc_Pos=Qterm_Doc_Positions.get(qterms.get(i));
            Set<Integer> Term1_DocID_Set=new HashSet<>(Term1_Doc_Pos.keySet());
            for(int j=i+1;j<qterms.size();j++)
            {
                Map<Integer,List<Integer>> Term2_Doc_Pos=Qterm_Doc_Positions.get(qterms.get(j));
                Set<Integer> Term2_DocID_Set=new HashSet<>(Term2_Doc_Pos.keySet());
                Set<Integer> Common_DocID_Set=Term2_DocID_Set;
                Common_DocID_Set.retainAll(Term1_DocID_Set);
                Pair<String,String> p=new Pair<>(qterms.get(i),qterms.get(j));

             pairSetMap.put(p,Common_DocID_Set);
            }
        }
        for(String ExpTerms:ql_map.keySet())
        {
            PostingsEnum postingExpTerm=MultiFields.getTermDocsEnum(index,"content",new BytesRef(ExpTerms),PostingsEnum.POSITIONS);
            double score_of_n_qterms = 0;
            for (Pair p : pairSetMap.keySet()) {
                String term1= String.valueOf(p.getFirst());
                String term2=String.valueOf(p.getSecond());
                Set<Integer> pair_doc_list=pairSetMap.get(p);
                int Coccurance_count = 0;
                if (postingExpTerm != null) { // if the term does not appear in any document, the posting object may be null
                    int docid;
                    while ((docid = postingExpTerm.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        if (FeedbackDocList.contains(docid) && pair_doc_list.contains(docid)) {
                            List<Integer> posTerm1=Qterm_Doc_Positions.get(term1).get(docid);
                            List<Integer> posTerm2=Qterm_Doc_Positions.get(term2).get(docid);
                            int freq = postingExpTerm.freq();
                            if(freq>=3) {
                                for (int i = 0; i < freq; i++) {
                                    int position_of_expansionTerm = postingExpTerm.nextPosition();
                                    int max=0,min=0;
                                    if(posTerm1.size()>posTerm2.size())
                                    {
                                        max=posTerm1.size();
                                        min=posTerm2.size();
                                    }
                                    else
                                    {
                                        max=posTerm2.size();
                                        min=posTerm1.size();
                                    }
                                   for(int k1=0;k1<min;k1++)
                                    {
                                        for(int k2=0;k2<max;k2++)
                                        {
                                            if(Math.abs(k1-position_of_expansionTerm)<=15 && Math.abs(k2-position_of_expansionTerm)<=15)
                                            {
                                                Coccurance_count++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                score_of_n_qterms += Coccurance_count / tot_freq;
            }
            // System.out.println(score_of_n_qterms);
            double score = Math.log(score_of_n_qterms / pairSetMap.size());
            System.out.println(ExpTerms + ":" + score);

        }
    }

    public void feature7(IndexReader index, Map<String, Double> ql_map, List<String> terms,List<SearchResult> ser) throws IOException {
     /*   for(String eterm:ql_map.keySet())
        {
            System.out.println(eterm);
        }*/
             List<Integer> FeedbackDocList = new ArrayList<>();
        int numfb=20;
        for (int i = 0; i <Math.min(numfb,ser.size()); i++) {
            FeedbackDocList.add(ser.get(i).getDocid());
        }
        double tot_freq=0;
        int minDist=12;
        QueryDocumentPosition(terms,index,FeedbackDocList);
        Map<Pair<String,String>,Integer> Qterm_MinDist_FD=new HashMap<>();
        for(String qterm:terms) {
            Map<Integer, List<Integer>> qtermPos = Qterm_Doc_Positions.get(qterm);
            for (String ExpTerm : ql_map.keySet()) {
                PostingsEnum posting = MultiFields.getTermDocsEnum(index, "content", new BytesRef(ExpTerm), PostingsEnum.POSITIONS);
                List<Integer> minDistList=new ArrayList<>();
                if (posting != null) {
                    int docid;
                    while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        if (qtermPos.containsKey(docid) && FeedbackDocList.contains(docid)) {
                            int freq = posting.freq();
                            for (int i = 0; i < freq; i++) {
                                int pos = posting.nextPosition();
                                for (Integer d : qtermPos.get(docid)) {
                                    if (Math.abs(pos - d) <= 12) {
                                        minDistList.add(Math.abs(pos - d));
                                    }
                                }
                            }
                        }
                    }
                }
                //System.out.println(minDistList.size());
                if(minDistList.size()!=0)
                    Qterm_MinDist_FD.put(new Pair<String, String>(qterm,ExpTerm),Collections.min(minDistList));
               // minDist=12;
            }
        }
        Map<String,Double> dummyMap=feature3(index,ql_map,ser,terms);

        for(String expTerm:ql_map.keySet())
        {
            double score=0,numerator=0,denom=0;
            for(Pair<String,String> pair:CoccuranceMap.keySet())
            {
                int dist=Qterm_MinDist_FD.getOrDefault(pair,0);
                if(pair.getSecond().equals(expTerm))
                {
                    numerator+=(CoccuranceMap.get(pair)*dist);
                    denom+=CoccuranceMap.get(pair);
                }
            }
            if(numerator==0)
                score=0;
            else
                score=Math.log(numerator/denom);
            /*if(Double.isNaN(score))
                score=0;*/
           System.out.println(expTerm+":"+score);
        }
        //System.out.println("NAN: "+Math.log(0/0));
    }

    public void feature9(IndexReader index,List<SearchResult> ser,List<String> qterms,Map<String,Double> ql_map) throws IOException {
        List<Integer> FeedbackDocList = new ArrayList<>();
        int numfb=20;
        for (int i = 0; i <Math.min(numfb,ser.size()); i++) {
            FeedbackDocList.add(ser.get(i).getDocid());
        }
        QueryDocumentPosition(qterms,index,FeedbackDocList);

        Map<String,Double> feature9Map=new HashMap<>();
        int i=0;
        Set<Integer> commonDocId=null;
        for(String qterm:Qterm_Doc_Positions.keySet())
        {
            if(i==0)
            {
              commonDocId= Qterm_Doc_Positions.get(qterm).keySet();
            }
            else {
                Set qtermDocid = Qterm_Doc_Positions.get(qterm).keySet();
                commonDocId.retainAll(qtermDocid);//list of docids for all the qterms together
            }
            i++;
        }
        for(String expTerm:ql_map.keySet())
        {
            Set<Integer> expTermDocSet=new HashSet<>();
            PostingsEnum posting=MultiFields.getTermDocsEnum(index,"content",new BytesRef(expTerm));
            if(posting!=null)
            {
                int docid;
                while((docid=posting.nextDoc())!=PostingsEnum.NO_MORE_DOCS)
                {
                    if(FeedbackDocList.contains(docid))
                    {
                        expTermDocSet.add(docid);// list of docids for e
                    }
                }
            }
            expTermDocSet.retainAll(commonDocId);// list of docid for I to pass I=1
            System.out.println("Score for "+expTerm+" : "+Math.log(expTermDocSet.size()+0.5));
        }
    }
}

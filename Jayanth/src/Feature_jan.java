import org.apache.commons.math3.util.Pair;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by jannu bhai on 11/28/2016.
 */
public class Feature_jan {
    static Map<String, Map<Integer, List<Integer>>> Qterm_Doc_Positions = new HashMap<>();
    static Map<Pair<String, String>, Integer> CoccuranceMap = new HashMap<>();

    public Map<String, Double> feature1(IndexReader index, List<String> ExpTermList, List<SearchResult> ser, int numfb) throws IOException {
        double tot_freq = 0;

        Map<String, Double> Term_dist_FD = new TreeMap<>();
        Map<Integer, Map<String, Double>> Docwise_wordfreq_map = new TreeMap<>();
        int count=0;
        for (int i = 0; i < Math.min(ser.size(), numfb); i++) {
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
                    //System.out.println(term+":"+freq);
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

        for (String expand_terms :ExpTermList) {
            double Exapanded_terms_freq = 0;
            double score = 0;
            for (int i = 0; i < ser.size(); i++) {
                int docid = ser.get(i).getDocid();
                //  System.out.println(docid);
                if(Docwise_wordfreq_map.containsKey(docid)) {
                    for (Map.Entry<String, Double> word_freq : Docwise_wordfreq_map.get(docid).entrySet()) {
                        if (word_freq.getKey().contains(expand_terms)) {
                            Exapanded_terms_freq += word_freq.getValue();

                        }
                    }
                }
            }
            if (Exapanded_terms_freq == 0) {
                score = 0;
            } else {
                score = Math.log(Exapanded_terms_freq / tot_freq);
            }
            Term_dist_FD.put(expand_terms, score);
        }
        for (Map.Entry<String, Double> map : Term_dist_FD.entrySet()) {
            // System.out.println(map.getKey() + "==>" + map.getValue());
        }
        return Term_dist_FD;
    }

    public Map<String, Double> feature2(IndexReader index,List<String> expansionTermList, LuceneQLSearcher seracher) throws IOException {
        double tot_freq = 0;
        int count=0;
        Map<String, Double> Term_dist_FD = new TreeMap<>();
        Map<Integer, Map<String, Double>> Docwise_wordfreq_map = new TreeMap<>();
        long docLen = CorpusLength(index, seracher);

        double corpusLength = index.getSumTotalTermFreq("content");
        double score = 0;
        for (String expand_terms :expansionTermList) {
            double corpusTF = index.totalTermFreq(new Term("content", expand_terms));
            score = Math.log(corpusTF / corpusLength);
            Term_dist_FD.put(expand_terms, score);
        }
        return Term_dist_FD;
    }

    private long CorpusLength(IndexReader index, LuceneQLSearcher seracher) throws IOException {

        long docLen = 0;
        for (int i = 0; i < index.numDocs(); i++) {
            int doc_id = i;
            docLen = docLen + seracher.getDocLengthReader("content").getLength(doc_id);
        }
        return docLen;
    }


    public Map<String, Double> feature3(IndexReader index,List<String> expansionTermList, List<SearchResult> ser, List<String> qterms, int numfb) throws IOException {
        List<Integer> FeedbackDocList = new ArrayList<>();
        double tot_freq = 0;
        for (int i = 0; i < Math.min(numfb, ser.size()); i++) {
            FeedbackDocList.add(ser.get(i).getDocid());
        }
        //       System.out.println(ser.size());
        for (int i = 0; i < FeedbackDocList.size(); i++) {
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

        //        QueryDocumentPosition(qterms, index, FeedbackDocList);
        Qterm_Doc_Positions.clear();
        QueryDocumentPosition(qterms, index, FeedbackDocList);

            /*for (String qterm : Qterm_Doc_Positions.keySet()) {
                System.out.println(qterm);
                for (Map.Entry<Integer, List<Integer>> map : Qterm_Doc_Positions.get(qterm).entrySet()) {
                    System.out.println(map.getKey() + ":" + map.getValue());
                }
            }*/

        Map<String, Double> feature3Map = new HashMap<>();
        for (String term : expansionTermList) {
            //System.out.println(term + ":");
            PostingsEnum posting = MultiFields.getTermDocsEnum(index, "content", new BytesRef(term), PostingsEnum.POSITIONS);
            double score_of_n_qterms = 0;
            for (String queryTerms : Qterm_Doc_Positions.keySet()) {
                int Coccurance_tot = 0;
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
                        }
                    }
                }

                CoccuranceMap.put(new Pair<String, String>(queryTerms, term), Coccurance_count);
                score_of_n_qterms += Coccurance_count / tot_freq;
            }
            // System.out.println(score_of_n_qterms);
            double score=0;
            if(score_of_n_qterms==0)
            {
                score=0.0;
            }
            else {
                score = Math.log((double) score_of_n_qterms / Qterm_Doc_Positions.keySet().size());
            }
            feature3Map.put(term, score);
            //   System.out.println(term + ":" + score);
        }
        return feature3Map;
    }

    public Map<String, Double> feature4(List<String> expansionTermList, List<String> qterms, LuceneQLSearcher Searcher) throws IOException {
        double tot_freq = CorpusLength(Searcher.index, Searcher);
        Map<String, Double> feature4 = new HashMap<>();
        List<Integer> DocIdList = new ArrayList<>();
        CoccuranceMap.clear();
        for (int i = 0; i < Searcher.index.maxDoc(); i++) {
            DocIdList.add(i);
        }
        //QueryDocumentPosition(qterms,Searcher.index,DocIdList);

        for (String expTerm : expansionTermList) {
            PostingsEnum posting = MultiFields.getTermDocsEnum(Searcher.index, "content", new BytesRef(expTerm), PostingsEnum.POSITIONS);
            double score_of_n_qterms = 0;
            for (String queryTerms : qterms) {
                int Coccurance_tot = 0;
                PostingsEnum postQTerm = MultiFields.getTermDocsEnum(Searcher.index, "content", new BytesRef(queryTerms), PostingsEnum.POSITIONS);
                Map<Integer, List<Integer>> postingQterm = getPostingPositionMap(postQTerm);
                int Coccurance_count = 0;
                if (posting != null) { // if the term does not appear in any document, the posting object may be null
                    int docid;
                    while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        int freq = posting.freq();
                        if (freq >= 3) {
                            for (int i = 0; i < freq; i++) {

                                int position_of_expansionTerm = posting.nextPosition();
                                if (postingQterm.containsKey(docid)) {
                                    for (Integer pos : postingQterm.get(docid)) {
                                        if (Math.abs(freq - pos) <= 12) {
                                            Coccurance_count++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                CoccuranceMap.put(new Pair<String, String>(queryTerms, expTerm), Coccurance_count);
                score_of_n_qterms += Coccurance_count / tot_freq;
            }
            double score=0;
            if(score_of_n_qterms==0)
            {
                score=0;
            }
            else
                score = Math.log((double) score_of_n_qterms / qterms.size());
            // System.out.println(expTerm + ":" + score);
            feature4.put(expTerm, score);
        }
        // System.out.println(score_of_n_qterms);


        //      System.out.println(term + ":" + score);
        return feature4;
    }


    private Map<Integer,List<Integer>> getPostingPositionMap(PostingsEnum postQTerm) throws IOException {
        Map<Integer, List<Integer>> positionMap = null;
        if (postQTerm != null) {
            positionMap = new HashMap<>();
            int docid;
            while ((docid = postQTerm.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                int freq = postQTerm.freq();
                List<Integer> positionList = new ArrayList<>();
                for (int i = 0; i < freq; i++) {
                    positionList.add(postQTerm.nextPosition());
                }
                positionMap.put(docid, positionList);
            }
        }
        return positionMap;
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
            //System.out.println(Qterm);
            Qterm_Doc_Positions.put(Qterm, Doc_Postion_Map);
        }
    }
    public Map<String,Double> feature5(IndexReader index,List<String> expansionTermList, List<SearchResult> ser, List<String> qterms) throws IOException
    {
        List<Integer> FeedbackDocList = new ArrayList<>();
        int numfb=20;
        Map<String,Double> feature5Map=new HashMap<>();
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
        //  System.out.println(expansionTermList.size());
        for(String ExpTerms:expansionTermList)
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
            double score=0;
            // System.out.println(score_of_n_qterms);
            if(score_of_n_qterms==0)
                score=0;
            else
                score = Math.log(score_of_n_qterms / pairSetMap.size());
            //System.out.println(ExpTerms + ":" + score);
            feature5Map.put(ExpTerms,score);

        }
        return feature5Map;
    }

    //this function should give score for each expansion term cooccuring with the pair of terms in the qtermList
    public Map<String,Double> feature6(List<String> expansionTermList, List<String> terms, LuceneQLSearcher searcher) throws IOException {
        double tot_freq = CorpusLength(searcher.index, searcher);
        double numerator=0,score=0;
        Map<String,Double> feature6Map=new HashMap<>();
        Map<Integer,Integer> docCoocuranceMap = new TreeMap<>();
        Map<Integer,Map<Integer,String>> docQtermPosMap = initialiseMap(terms,searcher);
        Map<Integer,Map<Integer,String>> docExpansiontermPosMap = initialiseMap(expansionTermList,searcher);
        Map<String,Integer> expansionTermCoOccuranceMap = new TreeMap<>();
        int numofPairs=0;

        //
        boolean debug = true;
        for(int i = 0;i<searcher.index.numDocs();i++) {
            int docLen = searcher.getDocLengthReader("content").getLength(i);
            Map<Integer, String> termPosMap = docQtermPosMap.get(i);
            Map<Integer, String> expansionTermPosMap = docExpansiontermPosMap.get(i);

            List<Triplet> pairList = termPosMap != null && termPosMap.size() > 0 ? pairQtermList(termPosMap) : new ArrayList<Triplet>();
            numofPairs += pairList.size();

            List<Triplet> tripletList = expansionTermPosMap != null && expansionTermPosMap.size() > 0 ?
                    tripletExpansionQtermList(pairList, expansionTermPosMap, docLen) : pairList;

            tripletCooccurance(tripletList, expansionTermCoOccuranceMap);
              /* if (debug && expansionTermCoOccuranceMap.size() != 0) {
                   System.out.println("doc No : " + i);
                   debug = false;
               }*/


        }
        //  System.out.println(tot_freq);
        // System.out.println(numofPairs);

        for(String eTerm : expansionTermList){
            feature6Map.put(eTerm,0.0);
        }
        for(Map.Entry<String,Integer> map : expansionTermCoOccuranceMap.entrySet()) {


            score = 0;
            if(numofPairs != 0 && map.getValue()!=0)
                score = Math.log(((double)1/numofPairs)*(map.getValue() / tot_freq));


            feature6Map.put(map.getKey(),score);
        }
        return feature6Map;
    }

    private static void tripletCooccurance(List<Triplet> tripletList,
                                           Map<String,Integer> expansionTermCoOccuranceMap ){
        int tripletCount = 0;
        for(Triplet t:tripletList) {
            if (t.getEpos() != -1){
                int count  = 0;
                if(expansionTermCoOccuranceMap.containsKey(t.getExpansionTerm())){
                    count = expansionTermCoOccuranceMap.get(t.getExpansionTerm()) + 1;
                }
                expansionTermCoOccuranceMap.put(t.getExpansionTerm(), count);
            }
        }
    }

    private static List<Triplet> pairQtermList( Map<Integer,String > termPosMap){
        List<Triplet> tripletList = new ArrayList<Triplet>();
        for(Map.Entry<Integer, String> map : termPosMap.entrySet()){
            int pos = map.getKey();
            for(int i = pos + 1 ;i< (pos + 15) ;i++){
                if(!termPosMap.containsKey(i)){
                    continue;
                }

                Triplet t = new Triplet();
                String term1 = map.getValue();
                t.setTerm1(term1);
                t.setPos1(pos);
                String term2 = termPosMap.get(i);

                if (term1.equals(term2)) // this can be allowed if the same term comes more than once in the query
                    continue;

                t.setTerm2(term2);
                t.setPos2(i);
                tripletList.add(t);
            }
        }
        return tripletList;
    }

    private static List<Triplet> tripletExpansionQtermList(List<Triplet> tripletList,Map<Integer,String > expansionTermPosMap,int docLen){
        for(Triplet t:tripletList){
            int minPos = Math.min(t.getPos1(), t.getPos2());
            int maxPos = Math.max(t.getPos1(), t.getPos2());

            int windowLowerBound = Math.max(0, maxPos -15);
            int windowUpperBound = Math.min(docLen,minPos+15); //min between doc length

            for(int i = windowLowerBound; i < windowUpperBound; i++){
                if(expansionTermPosMap.containsKey(i)){
                    t.setEpos(i);
                    t.setExpansionTerm(expansionTermPosMap.get(i));
                }
            }
        }
        return tripletList;
    }

    private Map<Integer,Map<Integer,String>> initialiseMap(List<String> terms,LuceneQLSearcher searcher) throws IOException {

        Map<Integer,Map<Integer,String>> returnMap = new TreeMap<>();
        Map<Integer,String> termPosMap = new TreeMap<>();
        //   Map<Integer,String> expPosMap = new TreeMap<>();

        for(String qterm:terms){
            PostingsEnum postQterm=MultiFields.getTermDocsEnum(searcher.index,"content",new BytesRef(qterm),PostingsEnum.POSITIONS);
            // BytesRef br;
            if(postQterm!= null)
            {
                int docid;
                while((docid=postQterm.nextDoc())!= PostingsEnum.NO_MORE_DOCS)
                {

                    int freq = postQterm.freq();
                    for(int pos=0;pos<freq;pos++)
                    {
                        int position = postQterm.nextPosition();
                        if( returnMap.containsKey(docid))
                        {
                            termPosMap = returnMap.get(docid);
                        }
                        else
                        {
                            termPosMap = new TreeMap<>();
                        }
                        termPosMap.put(position,qterm);
                        returnMap.put(docid,termPosMap);
                    }
                }
            }
        }
        return returnMap;
    }
    public Map<String,Double> feature7(IndexReader index, List<String> expansionTermList, List<String> terms,List<SearchResult> ser) throws IOException {
        List<Integer> FeedbackDocList = new ArrayList<>();
        int numfb=20;
        Map<String,Double> feature7Map=new HashMap<>();
        for (int i = 0; i <Math.min(numfb,ser.size()); i++) {
            FeedbackDocList.add(ser.get(i).getDocid());
        }
        double tot_freq=0;
        int minDist=12;
        QueryDocumentPosition(terms,index,FeedbackDocList);
        Map<Pair<String,String>,Integer> Qterm_MinDist_FD=new HashMap<>();
        for(String qterm:terms) {
            Map<Integer, List<Integer>> qtermPos = Qterm_Doc_Positions.get(qterm);
            for (String ExpTerm : expansionTermList) {
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
        Map<String,Double> dummyMap=feature3(index,expansionTermList,ser,terms,20);

        for(String expTerm:expansionTermList)
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
            feature7Map.put(expTerm,score);
            // System.out.println(expTerm+":"+score);
        }
        //System.out.println("NAN: "+Math.log(0/0));
        return feature7Map;
    }

    public Map<String,Double> feature8(List<String> expansionTermList, List<String> terms,LuceneQLSearcher searcher) throws IOException {
        double tot_freq = CorpusLength(searcher.index, searcher);
        int minDist=12;
        Map<String,Double> feature8=new HashMap<>();
        //  QueryDocumentPosition(terms,index,FeedbackDocList);
        Map<Pair<String,String>,Integer> Qterm_MinDist_FD=new HashMap<>();
        for(String qterm:terms) {
            PostingsEnum postQterm=MultiFields.getTermDocsEnum(searcher.index,"content",new BytesRef(qterm),PostingsEnum.POSITIONS);
            Map<Integer, List<Integer>> qtermPos = getPostingPositionMap(postQterm);
            for (String ExpTerm : expansionTermList) {
                PostingsEnum posting = MultiFields.getTermDocsEnum(searcher.index, "content", new BytesRef(ExpTerm), PostingsEnum.POSITIONS);
                List<Integer> minDistList=new ArrayList<>();
                if (posting != null) {
                    int docid;
                    while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        if (qtermPos.containsKey(docid)) {
                            int freq = posting.freq();
                            if(freq>=3) {
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
                }
                //System.out.println(minDistList.size());
                if(minDistList.size()!=0)
                    Qterm_MinDist_FD.put(new Pair<String, String>(qterm,ExpTerm),Collections.min(minDistList));
                // minDist=12;
            }
        }
        Map<String,Double> dummyMap=feature4(expansionTermList,terms,searcher);

        for(String expTerm:expansionTermList)
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
            //   System.out.println(expTerm+":"+score);
            feature8.put(expTerm,score);
        }
        //System.out.println("NAN: "+Math.log(0/0));
        return feature8;
    }

    public Map<String,Double> feature9(IndexReader index,List<SearchResult> ser,List<String> qterms,List<String> expansionTermList) throws IOException {
        List<Integer> FeedbackDocList = new ArrayList<>();
        int numfb=20;
        Map<String,Double> feature9Map=new HashMap<>();
        for (int i = 0; i <Math.min(numfb,ser.size()); i++) {
            FeedbackDocList.add(ser.get(i).getDocid());
        }
        QueryDocumentPosition(qterms,index,FeedbackDocList);
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
        for(String expTerm:expansionTermList)
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
            // System.out.println("Score for "+expTerm+" : "+Math.log(expTermDocSet.size()+0.5));
            feature9Map.put(expTerm,Math.log(expTermDocSet.size()+0.5));
        }
        return feature9Map;
    }
    public Map<String,Double> feature10(LuceneQLSearcher searcher,List<String> qterms,List<String> expansionTermList) throws IOException {
        Map<String,Double> feature10Map=new HashMap<>();
        int i=0;
        Set<Integer> commonDocId=null;
        for(String qterm:qterms)
        {
            PostingsEnum postQterm=MultiFields.getTermDocsEnum(searcher.index,"content",new BytesRef(qterm),PostingsEnum.POSITIONS);
            if(i==0)
            {
                commonDocId= getPostingPositionMap(postQterm).keySet();
            }
            else {
                Set qtermDocid =getPostingPositionMap(postQterm).keySet();
                commonDocId.retainAll(qtermDocid);//list of docids for all the qterms together
            }
            i++;
        }
        for(String expTerm:expansionTermList)
        {
            Set<Integer> expTermDocSet=new HashSet<>();
            PostingsEnum posting=MultiFields.getTermDocsEnum(searcher.index,"content",new BytesRef(expTerm));
            if(posting!=null)
            {
                int docid;
                while((docid=posting.nextDoc())!=PostingsEnum.NO_MORE_DOCS)
                {
                    expTermDocSet.add(docid);// list of docids for e
                }
            }
            expTermDocSet.retainAll(commonDocId);// list of docid for I to pass I=1
            //  System.out.println("Score for "+expTerm+" : "+Math.log(expTermDocSet.size()+0.5));
            feature10Map.put(expTerm,Math.log(expTermDocSet.size()+0.5));
        }
        return feature10Map;
    }

}
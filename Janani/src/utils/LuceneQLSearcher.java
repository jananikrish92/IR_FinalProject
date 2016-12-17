    package utils;

    import org.apache.lucene.analysis.Analyzer;
    import org.apache.lucene.index.*;
    import org.apache.lucene.store.Directory;
    import org.apache.lucene.store.FSDirectory;
    import org.apache.lucene.util.BytesRef;
    import org.apache.commons.math3.stat.StatUtils;
    import java.io.File;
    import java.io.IOException;
    import java.util.*;
    import java.util.List;

    public class LuceneQLSearcher extends AbstractQLSearcher {
        //int top = 1000;

        public static void main(String[] args) {
            try {

                String pathIndex = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\index_trec123"; // change it to your own index path
                Analyzer analyzer = LuceneUtils.getAnalyzer( LuceneUtils.Stemming.Krovetz ); // change the stemming setting accordingly

                String pathQueries = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\queries_trec1-3"; // change it to your query file path
                String pathQrels = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\qrels_trec1-3"; // change it to your qrels file path
                String pathStopwords = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\stopwords_inquery"; // change to your stop words path
                String field_docno = "docno";
                String field_search = "content";



                LuceneQLSearcher searcher = new LuceneQLSearcher(pathIndex);
                searcher.setStopwords(pathStopwords);

                Map<String, String> queries = EvalUtils.loadQueries(pathQueries);
                Map<String, Set<String>> qrels = EvalUtils.loadQrels(pathQrels);
                System.out.println(searcher.index.numDocs());

                double mu = 1500;
                int k = 10;
                int n = 80;

                int top = 1000;

                double total = 0;

                double mu2 = 0;
                //for (mu2 = 0; mu2 <= 2500; mu2 = mu2 + 500) {
                //	System.out.println("for mfb=" + mu2);
                    double[] p10 = new double[queries.size()];
                    double[] ap = new double[queries.size()];
                    int ix = 0;
                    String qid="51";
                    List<String> terms = null;
                    //for (String qid : queries.keySet()) {

                        String query = queries.get(qid);
                        terms = LuceneUtils.tokenize(query, analyzer);
                        List<SearchResult> ser = searcher.search(field_search,terms,mu,top);
                        Map<String, Double> ql_map = searcher.estimateQueryModelRM1(field_search, terms, mu, mu2, k, n);
                         Map<String,Double> smmMap=MixtureModel.estimateSMM("content",terms,0.5,20,80);
                         List<String> CorrectedExpandedTerms =getCorrectedExpandedTerms(ql_map,ser,searcher);


                        Features f=new Features();
                        //System.out.println(ql_map.size());
                        Map<String,Double> feature1Map=LuceneQLSearcher.scalingFeatures(f.feature1(searcher.index,CorrectedExpandedTerms,ser,20));
                        Map<String,Double> feature2Map=LuceneQLSearcher.scalingFeatures(f.feature2(searcher.index,CorrectedExpandedTerms,searcher));
                        Map<String,Double> feature3Map=LuceneQLSearcher.scalingFeatures(f.feature3(searcher.index,CorrectedExpandedTerms,ser,terms,20));
                        Map<String,Double> feature4Map=LuceneQLSearcher.scalingFeatures(f.feature4(CorrectedExpandedTerms,terms,searcher));
                        Map<String,Double> feature5Map=LuceneQLSearcher.scalingFeatures(f.feature5(searcher.index,CorrectedExpandedTerms,ser,terms));
                        Map<String,Double> feature6Map=LuceneQLSearcher.scalingFeatures(f.feature6(CorrectedExpandedTerms,terms,searcher));
                        Map<String,Double> feature7Map=LuceneQLSearcher.scalingFeatures(f.feature7(searcher.index,CorrectedExpandedTerms,terms,ser));
                        Map<String,Double> feature8Map=LuceneQLSearcher.scalingFeatures(f.feature8(CorrectedExpandedTerms,terms,searcher));
                        Map<String,Double> feature9Map=LuceneQLSearcher.scalingFeatures(f.feature9(searcher.index,ser,terms,CorrectedExpandedTerms));
                        Map<String,Double> feature10Map=LuceneQLSearcher.scalingFeatures(f.feature10(searcher,terms,CorrectedExpandedTerms));


                        /*
                         Testing if the expanded terms are good by adding it to the original model

                         */
                             SearchResult.dumpDocno(searcher.index, field_docno, ser);  // orginal Query results
                            double p30=0,apValue=0;
                              p30= EvalUtils.precision(ser, qrels.get(qid), 30); // p30 for original query
                        apValue= EvalUtils.avgPrec(ser, qrels.get(qid), top);// ap value for original query
                            System.out.print("P@30: "+ p30);
                        Map<String,Integer> GoodBadTermMap=new TreeMap<>();  // map for good bad terms
                        // running over expansion terms from SMM
                       for(String expTerm:CorrectedExpandedTerms) {
                           List<String> queryTerm=terms;
                           queryTerm.add(expTerm);
                           List<SearchResult> SerExpTerm=searcher.search("content",queryTerm,mu,top);  //search results from original query to add exapnsion term with w=0.01
                           SearchResult.dumpDocno(searcher.index, field_docno, SerExpTerm);
                           double p30_Expanded=0,apValue_Expanded=0;
                           p30_Expanded= EvalUtils.precision(SerExpTerm, qrels.get(qid), 30); // p30 for original query
                           apValue_Expanded= EvalUtils.avgPrec(SerExpTerm, qrels.get(qid), top);
                           System.out.println(expTerm+" : "+apValue_Expanded +" :"+apValue);

                            double diff=(apValue_Expanded-apValue);
                          // double diff=(p30_Expanded-p30)/p30;
                        //   System.out.println(expTerm+":"+diff);
                           if(diff>0.0005)
                            {
                                GoodBadTermMap.put(expTerm,+1);
                            }
                            else if(diff<-0.0005)
                            {
                                GoodBadTermMap.put(expTerm,-1);
                            }
                            else
                                GoodBadTermMap.put(expTerm,0);
                        }
                        int countNeg=0,countPos=0,countNeu=0;
                        for(Map.Entry<String,Integer> t:GoodBadTermMap.entrySet())
                        {
             //               System.out.println("expansionterm: "+t.getKey()+" Goodness :"+t.getValue());
                            if(t.getValue()==-1)
                            {
                                countNeg++;
                            }
                            else if(t.getValue()==0)
                            {
                                countNeu++;
                            }
                            else
                            {
                                countPos++;
                            }
                        }
                        System.out.println("Percent negative terms: "+((double)countNeg/GoodBadTermMap.size())*100+" "+countNeg);
                System.out.println("Percent postive terms: "+((double)countPos/GoodBadTermMap.size())*100+" "+countPos);
                System.out.println("Percent neutra terms: "+((double)countNeu/GoodBadTermMap.size())*100+" "+countNeu);



                //}

                //}

            /*	System.out.println("---------RM3-----------");
                float lamda=0.5;
                mu=1000;
                mu2=0;
                k=10;
                n=100;
                top=1000;
                Map<String,Map<String,Double>> qid_ql_map=new TreeMap<>();
                for(String qid:queries.keySet()) {
                    List<String> terms=null;
                    String query = queries.get(qid);
                    terms = LuceneUtils.tokenize(query, analyzer);
                    Map<String, Double> ql_map = searcher.estimateQueryModelRM1(field_search, terms, mu, mu2, k, n);
                    qid_ql_map.put(qid,ql_map);
                }
                for(int count=0;count<11;count++) {
                    lamda=count/10.0f;
                    System.out.println("lambda"+lamda);
                    double prob_mle=0;

                    double[] p10 = new double[queries.size()];
                    double[] ap = new double[queries.size()];
                    int ix = 0;
                    for(String qid:queries.keySet()) {
                        String query = queries.get(qid);
                        List<String> terms=null;
                        terms = LuceneUtils.tokenize(query, analyzer);
                        Map<String,Double> ql_map=qid_ql_map.get(qid);
                        for (String str : ql_map.keySet()) {
                            double prob_rm1 = ql_map.get(str) * (1 - lamda);
                            if (terms.contains(str)) {
                                prob_mle = lamda*((double)Collections.frequency(terms, str) / (double)terms.size());

                                ql_map.put(str, prob_mle + prob_rm1);
                            } else {
                                ql_map.put(str, prob_rm1);
                            }
                        }
                        ql_map = sortByValue(ql_map);

                        List<SearchResult> res = searcher.search(field_search, ql_map, mu, top);
                        SearchResult.dumpDocno(searcher.index, field_docno, res);
                        p10[ix] = EvalUtils.precision(res, qrels.get(qid), 10);
                        ap[ix] = EvalUtils.avgPrec(res, qrels.get(qid), top);
                        System.out.printf(
                                "%-10s%8.3f%8.3f\n",
                                qid,
                                p10[ix],
                                ap[ix]
                        );
                            ix++;
                    }
                    System.out.printf(
                            "%-10s%-25s%10.3f%10.3f\n",
                            "QL",
                            "QL",
                            StatUtils.mean(p10),StatUtils.mean(ap)
                    );
                //}
    */
                searcher.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static List<String> getCorrectedExpandedTerms(Map<String,Double> ql_map, List<SearchResult> ser,LuceneQLSearcher searcher) throws IOException {
            List<String> CorrectedExpandedTerms=new ArrayList<>();
            for(String expTerms : ql_map.keySet())
            {
                int count=0;
                for(int i=0 ; i<ser.size() ; i++) {
                    int docid = ser.get(i).getDocid();
                    TermsEnum expTermVector = searcher.index.getTermVector(docid, "content").iterator();
                    BytesRef br;
                    double freq=0;
                    while((br=expTermVector.next())!= null)
                    {
                        String term = br.utf8ToString();
                        if(term.equals(expTerms))
                        {
                            freq = expTermVector.totalTermFreq();
                            break;
                        }
                    }
                    if(freq>0){
                        count++;
                    }
                }
                if(count>=3)
                {
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

        public double dirichletLogProbability(String term,Integer docid,double mu,double weight) throws IOException {
            double logProbExpansionGivenFB=0;
            TermsEnum ter=index.getTermVector(docid,"content").iterator();
            BytesRef br;
            double freq=0;
            while((br=ter.next())!=null){
               // System.out.println(br.utf8ToString());
            String terms=br.utf8ToString();
               if(terms.equals(term))
               {
                    freq=ter.totalTermFreq();
                break;
                }
            }
          //  System.out.println(term+":"+freq);
            //if(freq<3)
              //  return 0;
            long docLen= getDocLengthReader("content").getLength(docid);
            long corpusTF = index.totalTermFreq( new Term("content", term ) );
            long corpusLength = index.getSumTotalTermFreq( "content" );
            double pwc = 1.0 * corpusTF / corpusLength;
            logProbExpansionGivenFB=weight*Math.log((freq+mu*pwc)/(docLen+mu));

            return logProbExpansionGivenFB;
        }
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




        public Map<String, Double> estimateQueryModelRM1( String field, List<String> terms, double mu1, double mu2, int numfbdocs, int numfbterms ) throws IOException {

            // fetch results for this query
            List<SearchResult> results = this.search(field, terms, mu1, numfbdocs);

            SearchResult.dumpDocno(this.index, "docno", results);

            double corpusLength = this.index.getSumTotalTermFreq("content");

            Map<String, Double> probOfTermGivenQueryMap = new TreeMap<String, Double>();

            Set<String> vocabulary = new HashSet<String>();

            // Create the vocabulary
            for (int i = 0; i < results.size(); i++) {
                int docid = results.get(i).getDocid();
                BytesRef br;

                // get terms for this document
                TermsEnum ter = this.index.getTermVector(docid, "content").iterator();

                // form word frquency map
                while ((br = ter.next()) != null) {
                    if (!stopwords.contains(br.utf8ToString())) {
                        String term = br.utf8ToString();
                        vocabulary.add(term);
                    }
                }
            }

            // Iterate over top k documents
            double normalization = 0.0;
            for (int i = 0; i < results.size(); i++) {
                int docid = results.get(i).getDocid();
                BytesRef br;

                // get terms for this document
                TermsEnum ter = this.index.getTermVector(docid, "content").iterator();
                long docLen = getDocLengthReader("content").getLength(docid);

                Map<String, Double> wordFreq = new TreeMap<String, Double>();

                // form word frquency map
                while ((br = ter.next()) != null) {
                    if (!stopwords.contains(br.utf8ToString())) {
                        String term = br.utf8ToString();
                        double freq = ter.totalTermFreq();
    //					docLen += freq;
                        if (wordFreq.containsKey(term)) {
                            wordFreq.put(term, wordFreq.get(term) + freq);
                        } else {
                            wordFreq.put(term, freq);
                        }
                    }
                }

                double prob_ql = Math.exp(results.get(i).getScore());

                // over all query terms
                /*for(String term : terms)
                {
                    double ctd = wordFreq.get(term);
                    double corpusTF = index.totalTermFreq(new Term("content", term));
                    double numerator = ctd +  (mu1 * (corpusTF/corpusLength));
                    prob_ql *= numerator/(docLen + mu1);
                }*/

                // over all terms in the vocabulary
                for (String term : vocabulary) {
                    double ctd = 0.0;

                    if (wordFreq.containsKey(term))
                        ctd = wordFreq.get(term);

                    double corpusTF = index.totalTermFreq(new Term("content", term));

                    double prob_fb = (ctd + (mu2 * (corpusTF / corpusLength))) / (docLen + mu2);

                    double probTermGivenQuery = prob_fb * prob_ql;

                    if (probOfTermGivenQueryMap.containsKey(term)) {
                        probOfTermGivenQueryMap.put(term, probOfTermGivenQueryMap.get(term) + probTermGivenQuery);
                    } else {
                        probOfTermGivenQueryMap.put(term, probTermGivenQuery);
                    }

                    normalization += probTermGivenQuery;
                }
            }


            // Normalize
            for (Map.Entry<String, Double> final_score : probOfTermGivenQueryMap.entrySet()) {
                probOfTermGivenQueryMap.put(final_score.getKey(), final_score.getValue() / normalization);
            }

            probOfTermGivenQueryMap = sortByValue(probOfTermGivenQueryMap);
            Map<String, Double> top_n_terms = new TreeMap<>();
            int j = 0;
            if (numfbterms > 0) {
                for (Map.Entry<String, Double> top_terms : probOfTermGivenQueryMap.entrySet()) {
                    if (j < numfbterms) {
                        top_n_terms.put(top_terms.getKey(), top_terms.getValue());
                        j++;
                    } else
                        break;
                }

                return top_n_terms;

            } else
                return probOfTermGivenQueryMap;
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
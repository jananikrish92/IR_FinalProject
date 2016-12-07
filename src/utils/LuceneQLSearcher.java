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


			double mu = 1500;
			int k = 10;
			int n = 0;

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
					List<SearchResult> ser=searcher.search(field_search,terms,mu,top);

					Map<String, Double> ql_map = searcher.estimateQueryModelRM1(field_search, terms, mu, mu2, k, n);

					Features f=new Features();

					//f.feature1(searcher.index,ql_map,ser);
					//f.feature3(searcher.index,ql_map,ser,terms);
					//f.feature5(searcher.index,ql_map,ser,terms);
					//f.feature7(searcher.index,ql_map,terms,ser);
					f.feature9(searcher.index,ser,terms,ql_map);
					List<SearchResult> results = searcher.search(field_search, ql_map, 1000, top);
					SearchResult.dumpDocno(searcher.index, field_docno, results);

					p10[ix] = EvalUtils.precision(results, qrels.get(qid), 30);
					ap[ix] = EvalUtils.avgPrec(results, qrels.get(qid), top);
					System.out.printf(
							"%-10s%8.3f%8.3f\n",
							qid,
							p10[ix],
							ap[ix]
					);

					ix++;
				//}
				System.out.printf(
						"%-10s%-25s%10.3f%10.3f\n",
						"QL",
						"QL",
						StatUtils.mean(p10),
						StatUtils.mean(ap)
				);
			//}

/*			System.out.println("RERANKING:");
		for(int m=500;m<=5000;m=m+500) {
			List<String> terms = null;
			n = 0;
			double[] p10 = new double[queries.size()];
			double[] ap = new double[queries.size()];
			int ix = 0;
			top = 1000;
			System.out.println("for m:"+m);
			Map<String, Map<String, Double>> qid_ql_map = new TreeMap<>();
			for (String qid : queries.keySet()) {
				//List<String> terms=null;
				String query = queries.get(qid);
				terms = LuceneUtils.tokenize(query, analyzer);
				Map<String, Double> ql_map = searcher.estimateQueryModelRM1(field_search, terms, mu, mu2, k, n);
				qid_ql_map.put(qid, ql_map);
			}
			for (String qid : queries.keySet()) {

				String query = queries.get(qid);
				terms = LuceneUtils.tokenize(query, analyzer);
				List<SearchResult> res_QL = searcher.search(field_search, terms, mu, m);
				SearchResult.dumpDocno(searcher.index, field_docno, res_QL);
				Map<String, Double> ql_map = qid_ql_map.get(qid);

				//System.out.println(ql_map.get("crime"));
				//System.out.println(ql_map.get("organized"));

				List<SearchResult> kld_result = searcher.rerank(res_QL, mu, ql_map, terms);
				//SearchResult.dumpDocno(searcher.index, field_docno, kld_result);

				p10[ix] = EvalUtils.precision(kld_result, qrels.get(qid), 10);
				ap[ix] = EvalUtils.avgPrec(kld_result, qrels.get(qid), top);

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
					StatUtils.mean(p10),
					StatUtils.mean(ap)
			);
		}


			System.out.println("---------RM3-----------");
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
		//	for(int count=0;count<11;count++) {
		//		lamda=count/10.0f;
		//		System.out.println("lambda"+lamda);
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
	public List<SearchResult> rerank(List<SearchResult> res_QL, double mu, Map<String,Double> rerankTerms, List<String> qterms1) throws IOException
	{
		Set<String> qterms=rerankTerms.keySet();

		Map<SearchResult, Double> resultUsingKLD = new HashMap<SearchResult, Double>();

		double corpusLength = this.index.getSumTotalTermFreq("content");

		for(int i=0;i<res_QL.size();i++)
		{
			int docid=res_QL.get(i).getDocid();

			String docNo=res_QL.get(i).docno;

			TermsEnum ter=this.index.getTermVector(docid,"content").iterator();
			long docLen = getDocLengthReader("content").getLength(docid);
			Map<String, Double> wordFreq = new TreeMap<String, Double>();

			BytesRef br;
			// form word frquency map
			while ((br = ter.next()) != null) {
				if (!stopwords.contains(br.utf8ToString())) {
					String term = br.utf8ToString();
					double freq = ter.totalTermFreq();
					//             docLen += freq;
					if (wordFreq.containsKey(term)) {
						wordFreq.put(term, wordFreq.get(term) + freq);
					} else {
						wordFreq.put(term, freq);
					}
				}
			}

			double score=0;
			for(String term : qterms)
			{
				double prob_term_given_query=rerankTerms.get(term);

				double ctd=0;

				if(wordFreq.containsKey(term))
				{
					ctd = wordFreq.get(term);
				}

				double corpusTF = index.totalTermFreq(new Term("content", term));

				double probTermGivenDocument = (ctd + (mu * (corpusTF / corpusLength))) / (docLen + mu);
				score += (prob_term_given_query * Math.log(probTermGivenDocument));
			}

			// System.out.println("Document no"+docNo+" Score "+score);
			resultUsingKLD.put(new SearchResult(docid, docNo, score), score);
		}

		Map<SearchResult, Double> resultWithScore = sortByValue(resultUsingKLD);

		List<SearchResult> rerankedDocumentList = new ArrayList<SearchResult>();

		for (Map.Entry<SearchResult, Double> entry : resultWithScore.entrySet()) {
			rerankedDocumentList.add(entry.getKey());
		}

		return rerankedDocumentList;
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
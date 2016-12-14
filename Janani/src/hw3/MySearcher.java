
package hw3;
import org.apache.lucene.document.Field;
import utils.*;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.util.*;

public class MySearcher {
	
	public static void main( String[] args ) {
		try {
			
			String pathIndex = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\index_trec123"; // change it to your own index path
			Analyzer analyzer = LuceneUtils.getAnalyzer( LuceneUtils.Stemming.Krovetz ); // change the stemming setting accordingly
			
			String pathQueries = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\queries_trec1-3"; // change it to your query file path
			String pathQrels = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\qrels_trec1-3"; // change it to your qrels file path
			String pathStopwords = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\stopwords_inquery"; // change to your stop words path
			
			String field_docno = "docno";
			String field_search = "content";
			
			MySearcher searcher = new MySearcher( pathIndex );
			searcher.setStopwords( pathStopwords );
			
			Map<String, String> queries = EvalUtils.loadQueries( pathQueries );
			Map<String, Set<String>> qrels = EvalUtils.loadQrels( pathQrels );

            Map<String,String> Train_Map=new TreeMap<>();
            Map<String,String> Test_Map=new TreeMap<>();
            for (String qid:queries.keySet())
            {
                int quid=Integer.parseInt(qid);
                if(quid>=301 && quid<=450)
                {
                    Train_Map.put(qid,queries.get(qid));
                }
                else if(quid>=601 && quid<=700)
                {
                    Test_Map.put(qid,queries.get(qid));
                }
            }


			int top = 1000;
			double mu = 1000;
			float lambda=0;

            System.out.println("-------------QMDirichlet Smoothing---------------------"+"\n");
			ScoringFunction scoreFunc=null;
            for (mu=500;mu<=5000;mu=mu+500) {

                double[] p10 = new double[Train_Map.size()];
                double[] ap = new double[Train_Map.size()];
                double[] p10_1 = new double[Test_Map.size()];
                double[] ap_1 = new double[Test_Map.size()];

                int ix = 0;
                System.out.println("mu:"+mu+"\n");
                scoreFunc= new QLDirichletSmoothing( mu );
                System.out.println("QMDirichlet Smoothing for Training Set"+"\n");
                for ( String qid : Train_Map.keySet() ) {

                    String query = Train_Map.get( qid );
                    List<String> terms = LuceneUtils.tokenize( query, analyzer );
                    List<SearchResult> results = searcher.search( field_search, terms, scoreFunc, top );
                    SearchResult.dumpDocno( searcher.index, field_docno, results );

                    p10[ix] = EvalUtils.precision( results, qrels.get( qid ), 10 );
                    ap[ix] = EvalUtils.avgPrec( results, qrels.get( qid ), top );

                    ix++;
                }
                System.out.printf(
                        "%-10s%-25s%10.3f%10.3f\n",
                        "QL",
                        "QL",
                        StatUtils.mean( p10 ),
                        StatUtils.mean( ap )
                );

                System.out.println("\n"+"QMDirichlet Smoothing for Testing Set"+"\n");
                ix=0;
                for ( String qid : Test_Map.keySet() ) {

                    String query = Test_Map.get( qid );
                    List<String> terms = LuceneUtils.tokenize( query, analyzer );

                    List<SearchResult> results = searcher.search( field_search, terms, scoreFunc, top );
                    SearchResult.dumpDocno( searcher.index, field_docno, results );

                    p10_1[ix] = EvalUtils.precision( results, qrels.get( qid ), 10 );
                    ap_1[ix] = EvalUtils.avgPrec( results, qrels.get( qid ), top );

                    ix++;
                }

                System.out.printf(
                        "%-10s%-25s%10.3f%10.3f\n",
                        "QL",
                        "QL",
                        StatUtils.mean( p10_1),
                        StatUtils.mean( ap_1 )
                );
            }


           System.out.println("\n"+"---------------------JL Smoothing---------------------------------"+"\n");


			ScoringFunction scoreFunc1=null;
           	for (int count = 1; count <= 9; count += 1) {
                lambda = count / 10.0f;
                double[] p10 = new double[Train_Map.size()];
                double[] ap = new double[Train_Map.size()];
                double[] p10_1 = new double[Test_Map.size()];
                double[] ap_1 = new double[Test_Map.size()];
                double total = 0;
                int ix = 0;
               System.out.println("\nlambda:"+lambda+"\n");
                 scoreFunc1= new QLJMSmoothing(lambda);
                System.out.println("JLSmoothing for Training Set\n");
                for ( String qid : Train_Map.keySet() ) {

                    String query = Train_Map.get( qid );
                    List<String> terms = LuceneUtils.tokenize( query, analyzer );
                    List<SearchResult> results = searcher.search( field_search, terms, scoreFunc1, top );
                    SearchResult.dumpDocno( searcher.index, field_docno, results );

                    p10[ix] = EvalUtils.precision( results, qrels.get( qid ), 10 );
                    ap[ix] = EvalUtils.avgPrec( results, qrels.get( qid ), top );

                    ix++;
                }
                System.out.printf(
                        "%-10s%-25s%10.3f%10.3f\n",
                        "QL",
                        "QL",
                        StatUtils.mean( p10 ),
                        StatUtils.mean( ap )
                );
                System.out.println("\nJLSmoothing for Testing Set\n");
                ix=0;
                for ( String qid : Test_Map.keySet() ) {

                    String query = Test_Map.get( qid );
                    List<String> terms = LuceneUtils.tokenize( query, analyzer );

                    List<SearchResult> results = searcher.search( field_search, terms, scoreFunc1, top );
                    SearchResult.dumpDocno( searcher.index, field_docno, results );

                    p10_1[ix] = EvalUtils.precision( results, qrels.get( qid ), 10 );
                    ap_1[ix] = EvalUtils.avgPrec( results, qrels.get( qid ), top );

                    ix++;
                }

                System.out.printf(
                        "%-10s%-25s%10.3f%10.3f\n",
                        "QL",
                        "QL",
                        StatUtils.mean( p10_1),
                        StatUtils.mean( ap_1 )
                );
            }

			searcher.logpdc(field_docno,"FBIS4-41991");
            searcher.logpdc(field_docno,"FBIS4-67701");
            searcher.logpdc(field_docno,"FT921-7107");
            searcher.logpdc(field_docno,"FR940617-0-00103");
            searcher.logpdc(field_docno,"FR941212-0-00060");
            searcher.logpdc(field_docno,"FBIS3-25118");
			searcher.close();
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}



	protected File dirBase;
	protected Directory dirLucene;
	protected IndexReader index;
	protected Map<String, DocLengthReader> doclens;
	
	protected HashSet<String> stopwords;
	
	public MySearcher( String dirPath ) throws IOException {
		this( new File( dirPath ) );
	}
	
	public MySearcher( File dirBase ) throws IOException {
		this.dirBase = dirBase;
		this.dirLucene = FSDirectory.open( this.dirBase.toPath() );
		this.index = DirectoryReader.open( dirLucene );
		this.doclens = new HashMap<>();
		this.stopwords = new HashSet<>();
	}
	
	public void setStopwords( Collection<String> stopwords ) {
		this.stopwords.addAll( stopwords );
	}
	
	public void setStopwords( String stopwordsPath ) throws IOException {
		setStopwords( new File( stopwordsPath ) );
	}
	
	public void setStopwords( File stopwordsFile ) throws IOException {
		BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( stopwordsFile ), "UTF-8" ) );
		String line;
		while ( ( line = reader.readLine() ) != null ) {
			line = line.trim();
			if ( line.length() > 0 ) {
				this.stopwords.add( line );
			}
		}
		reader.close();
	}
	
	public List<SearchResult> search( String field, List<String> terms, ScoringFunction scoreFunc, int top ) throws IOException {
		
		Map<String, Double> qfreqs = new TreeMap<>();
		for ( String term : terms ) {
			if ( !stopwords.contains( term ) ) {
				qfreqs.put( term, qfreqs.getOrDefault( term, 0.0 ) + 1 );
			}
		}
		
		List<PostingsEnum> postings = new ArrayList<>();
		List<Double> weights = new ArrayList<>();
		List<Double> tfcs = new ArrayList<>();
		for ( String term : qfreqs.keySet() ) {
			PostingsEnum list = MultiFields.getTermDocsEnum( index, field, new BytesRef( term ), PostingsEnum.FREQS );
			if ( list.nextDoc() != PostingsEnum.NO_MORE_DOCS ) {
				postings.add( list );
				weights.add( qfreqs.get( term ) / terms.size() );
				tfcs.add( 1.0 * index.totalTermFreq( new Term( field, term ) ) );
			}
		}
		return search( postings, weights, tfcs, getDocLengthReader( field ), index.getSumTotalTermFreq( field ), scoreFunc, top );
	}
	
	private List<SearchResult> search( List<PostingsEnum> postings, List<Double> weights, List<Double> tfcs, DocLengthReader doclen, double cl, ScoringFunction scoreFunc, int top ) throws IOException {
		
		PriorityQueue<SearchResult> topResults = new PriorityQueue<>( ( r1, r2 ) -> {
			int cp = r1.getScore().compareTo( r2.getScore() );
			if ( cp == 0 ) {
				cp = r1.getDocid() - r2.getDocid();
			}
			return cp;
		} );
		
		List<Double> tfs = new ArrayList<>( weights.size() );
		for ( int ix = 0; ix < weights.size(); ix++ ) {
			tfs.add( 0.0 );
		}
		while ( true ) {
			
			int docid = Integer.MAX_VALUE;
			for ( PostingsEnum posting : postings ) {
				if ( posting.docID() != PostingsEnum.NO_MORE_DOCS && posting.docID() < docid ) {
					docid = posting.docID();
				}
			}
			
			if ( docid == Integer.MAX_VALUE ) {
				break;
			}
			
			int ix = 0;
			for ( PostingsEnum posting : postings ) {
				if ( docid == posting.docID() ) {
					tfs.set( ix, 1.0 * posting.freq() );
					posting.nextDoc();
				} else {
					tfs.set( ix, 0.0 );
				}
				ix++;
			}
			double score = scoreFunc.score( weights, tfs, tfcs, doclen.getLength( docid ), cl );
			
			if ( topResults.size() < top ) {
				topResults.add( new SearchResult( docid, null, score ) );
			} else {
				SearchResult result = topResults.peek();
				if ( score > result.getScore() ) {
					topResults.poll();
					topResults.add( new SearchResult( docid, null, score ) );
				}
			}
		}
		
		List<SearchResult> results = new ArrayList<>( topResults.size() );
		results.addAll( topResults );
		Collections.sort( results, ( o1, o2 ) -> o2.getScore().compareTo( o1.getScore() ) );
		return results;
	}
	
	public DocLengthReader getDocLengthReader( String field ) throws IOException {
		DocLengthReader doclen = doclens.get( field );
		if ( doclen == null ) {
			doclen = new FileDocLengthReader( this.dirBase, field );
			doclens.put( field, doclen );
		}
		return doclen;
	}
	
	public void close() throws IOException {
		index.close();
		dirLucene.close();
		for ( DocLengthReader doclen : doclens.values() ) {
			doclen.close();
		}
	}
	
	public interface ScoringFunction {
		
		/**
		 * @param weights Weight of the query terms, e.g., P(t|q) or c(t,q).
		 * @param tfs     The frequencies of the query terms in documents.
		 * @param tfcs    The frequencies of the query terms in the corpus.
		 * @param dl      The length of the document.
		 * @param cl      The length of the whole corpus.
		 * @return
		 */
		double score( List<Double> weights, List<Double> tfs, List<Double> tfcs, double dl, double cl );
	}
	
	public static class QLJMSmoothing implements ScoringFunction {
		
		protected double lambda;
		
		public QLJMSmoothing( double lambda ) {
			this.lambda = lambda;
		}
		
		public double score( List<Double> weights, List<Double> tfs, List<Double> tfcs, double dl, double cl ) {
       // System.out.println(weights);
        double prob_term_doc=0;
            for(int i=0;i<tfs.size();i++)
            {
                prob_term_doc+=(weights.get(i)*Math.log((tfs.get(i)/dl*(1-lambda))+ ((lambda)*tfcs.get(i)/cl)));
            }
            return prob_term_doc;
            //return 0;
		}
	}
	
	public static class QLDirichletSmoothing implements ScoringFunction {
		
		protected double mu;
		
		public QLDirichletSmoothing( double mu ) {
			this.mu = mu;
		}
		
		public double score( List<Double> weights, List<Double> tfs, List<Double> tfcs, double dl, double cl ) {

            double prob_term_doc=0;
            for(int i=0;i<tfs.size();i++)
            {
                prob_term_doc+=(weights.get(i)*Math.log((tfs.get(i)+(mu*(tfcs.get(i)/cl)))/(dl+mu)));
            }

            return prob_term_doc;
		}
	}

    public void logpdc(String field, String docno) {
        try {
            double res=0,prob;
            double corpusLength = this.index.getSumTotalTermFreq("content");
            //System.out.println("Corpus length"+corpusLength);
            for(int i=0;i<this.index.numDocs();i++) {
                String docNo=this.index.document(i).getField("docno").stringValue();
                if(docNo.equals(docno))
                {
                   // System.out.println(docNo);
                    BytesRef br;
                    TermsEnum terms=this.index.getTermVector(i,"content").iterator();
                    while((br=terms.next())!=null)
                    {
                           double ctd=terms.totalTermFreq();
                           double corpusTF = index.totalTermFreq(new Term("content",br));
                           double log_prob=Math.log(corpusTF/corpusLength);
                          //  System.out.println(br.utf8ToString()+" "+corpusTF+" "+corpusLength+"probability is:"+corpusTF/corpusLength);
                           res=res+(ctd*log_prob);
                    }
				System.out.println(docno+"  "+res);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}

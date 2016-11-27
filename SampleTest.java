import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jannu bhai on 11/23/2016.
 */
public class SampleTest
{
    public static void main(String[] args) throws IOException {
        String pathIndex = "C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\index_robust04";
        String field = "content";
        Analyzer ana=LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);
        String queries="C:\\Users\\jannu bhai\\IdeaProjects\\FinalProject\\queries_robust04";
        Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
        IndexReader ixreader = DirectoryReader.open( dir );
        Map<String, String> query_map = EvalUtils.loadQueries(queries);
        String p=query_map.get("449");
        List<String> query=LuceneUtils.tokenize(p,ana);
// we also print out external ID
        Set<String> fieldset = new HashSet<>();
        fieldset.add( "docno" );

// The following loop iteratively print the lengths of the documents in the index.
        System.out.printf( "%-10s%-15s%-10s\n", "DOCID", "DOCNO", "Length" );
        for ( int docid = 0; docid < ixreader.maxDoc(); docid++ ) {
            String docno = ixreader.document( docid, fieldset ).get( "docno" );
            int doclen = 0;

            BytesRef br;
            TermsEnum ter=ixreader.getTermVector(docid,"content").iterator();
            //while((br=ter.next())!=null) {
                double corpusTF = ixreader.totalTermFreq(new Term("content", new BytesRef(query.get(1))));
//                double ctd = ter.totalTermFreq();
            for(int j=0;j<query.size();j++)
                System.out.println(docno+query.get(j) + " " + ixreader.docFreq(new Term("content",new BytesRef((query.get(j))))));
            //}
            // ately, Lucene does not store document length in its index
            // (because its retrieval model does not rely on document length
            //
            // ).

            // An acceptable but slow solution is that you calculate document length by yourself based on
            // document vector. In case your dataset is static and relatively small (such as about or less
            // than a few million documents), you can simply compute the document lengths and store them in
            // an external file (it takes just a few MB). At running time, you can load all the computed
            // document lengths to avoid loading doc vector and computing length.
      //      TermsEnum termsEnum = ixreader.getTermVector( docid, field ).iterator();
        //    while ( termsEnum.next() != null ) {
          //      doclen += termsEnum.totalTermFreq();
            //}
           // System.out.printf( "%-10d%-15s%-10d\n", docid, docno, doclen );
            //System.out.println(docid+" "+ docno+" "+corpusTF);
        }

        ixreader.close();
        dir.close();


    }



}

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import org.apache.lucene.analysis.Analyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by jph on 20/12/16.
 */
public class Experiments {

    public static Map<String, Double> normalExpTerms = new HashMap<>();
    public static Map<String, Double> svmTerms = new HashMap<>();

    public static double evaluate_all_instances(String Filename1, String folder, String modelFilename, int record_size) throws IOException
    {

        //read data from file

        //read the data from file and put it in the train module

        svm_model model1 = svm.svm_load_model(modelFilename);

        LinkedList<String> Dataset = new LinkedList<String>();//stores the lines from the given file

        try

        {

            //int i=0;

            FileReader fr1 = new FileReader(folder+"/"+Filename1);

            BufferedReader br1 = new BufferedReader(fr1);

            for(String line1 = br1.readLine();line1!=null;line1=br1.readLine())

            {

                Dataset.add(line1);

            }

            br1.close();

        }

        catch(Exception e)

        {

            e.printStackTrace();

        }

        System.out.println("Dataset Size "+Dataset.size());

        record_size = Dataset.size();

        double node_values[][] = new double[record_size][]; //jagged array used to store values

        int node_indexes[][] = new int[record_size][];//jagged array used to store node indexes



        //Now store data values
        List<Double> labels = new ArrayList<>();
        List<String> words = new ArrayList<>();
        for(int i=0;i<Dataset.size();i++)

        {

            try

            {

                String [] data1 = Dataset.get(i).split("\\s");



                LinkedList<Integer> list_indx = new LinkedList<Integer>();

                LinkedList<Double> list_val = new LinkedList<Double>();

                String word = null;
                Double label = 0.0;

                for(int k=0;k<data1.length;k++)

                {

                    String [] tmp_data = data1[k].trim().split(":");

                    if(tmp_data.length==2)

                    {

                        list_indx.add(Integer.parseInt(tmp_data[0].trim()));

                        list_val.add(Double.parseDouble(tmp_data[1].trim()));

                        System.out.println("Index  "+tmp_data[0]+" Value "+tmp_data[1]);

                    }
                    else {

                        if(k == 0) {
                            word = tmp_data[0].trim();
                            words.add(word);
                        }
                        else if (k == 1){
                            label = Double.parseDouble(tmp_data[0].trim());
                            normalExpTerms.put(word, label);
                            labels.add(Double.parseDouble(tmp_data[0].trim()));
                        }
                    }
                }

                if(list_val.size()>0)

                {

                    node_values[i] = new double[list_val.size()];

                    node_indexes[i] = new int[list_indx.size()];

                }

                for(int m=0;m<list_val.size();m++)

                {

                    node_indexes[i][m] = list_indx.get(m);

                    node_values[i][m] = list_val.get(m);

                    System.out.println("List Index value "+list_indx.get(m)+"  <=> List values "+list_val.get(m)+"  list size "+list_indx.size());

                }

            }

            catch(Exception e)

            {

                e.printStackTrace();

            }

        }



        //now identify the class labels for test dataset
        double accuracy = 0.0;
        for(int i=0;i<record_size;i++)

        {

            int tmp_indexes[] = node_indexes[i];

            double tmp_values[] = node_values[i];

            double prediction = evaluate_single_instance(tmp_indexes,tmp_values, model1);
            if(labels.get(i) == prediction)
                accuracy++;

            if(prediction == 1.0){
                svmTerms.put(words.get(i), prediction);
            }

        }

        accuracy /= record_size;

        return accuracy;

    }
    //write the code to test single feature each time by using SVM
    public static double evaluate_single_instance(int [] indexes, double[] values, svm_model model1)
    {

        svm_node[] nodes = new svm_node[values.length];

        for (int i = 0; i < values.length; i++)

        {

            svm_node node = new svm_node();

            node.index = indexes[i];

            node.value = values[i];

            nodes[i] = node;

        }


        int totalClasses = svm.svm_get_nr_class(model1);

        int[] labels = new int[totalClasses];

        svm.svm_get_labels(model1,labels);


        double[] prob_estimates = new double[totalClasses];

        double v = svm.svm_predict_probability(model1, nodes, prob_estimates);


        for (int i = 0; i < totalClasses; i++)
        {

            System.out.print("(" + labels[i] + ":" + prob_estimates[i] + ")");

        }

        System.out.println(" Prediction:" + v );

        return v;

    }



    public static void main(String [] args) throws IOException{
        String expFolder = "/home/jph/cmpsci 646/project/IR final project/IR Results";

        evaluate_all_instances("robust/smm/318ext", expFolder, "indexRobustOP_models/BaseLineSMM/SMM.model", 80);


       System.out.println("************************************");
        for(String w : normalExpTerms.keySet())
            System.out.println(w+" : "+normalExpTerms.get(w));

       System.out.println("************************************");
        for(String w : svmTerms.keySet())
            System.out.println(w+" : "+svmTerms.get(w));
        System.out.println("************************************");

        String pathIndex = "index_robust04";
        Analyzer analyzer = LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);

        String pathQueries = "queries_robust04"; // change it to your
        // query file path
        String pathQrels = "qrels_robust04"; // change it to your
        // qrels file path
        String pathStopwords = "stopwords_inquery";
        String field_docno = "docno";
        String field_search = "content";

        LuceneQLSearcher searcher = new LuceneQLSearcher(pathIndex);
        searcher.setStopwords(pathStopwords);
        System.out.println("entering main");
        Map<String, String> queries = EvalUtils.loadQueries(pathQueries);
        Map<String, Set<String>> qrels = EvalUtils.loadQrels(pathQrels);
        String qid="381";
        int top = 1000;
        double mu = 1000;
        int numfbdocs = 20;


        String query = queries.get(qid);// qid);
        List<String> terms = LuceneUtils.tokenize(query, analyzer);
        List<String> terms1 = terms;
        List<String> terms2 = terms;
        for(String w : normalExpTerms.keySet())
            terms1.add(w);
        for(String w : svmTerms.keySet())
            terms2.add(w);

       List<SearchResult> normalSMMExp = searcher.search("content", terms1, mu, top);

        SearchResult.dumpDocno(searcher.index, "docno", normalSMMExp);

        List<SearchResult> svmSMMExp = searcher.search("content", terms2, mu, top);
        SearchResult.dumpDocno(searcher.index, "docno", svmSMMExp);

        PrintWriter pw = new PrintWriter("Runs/robust/SMM");
        PrintWriter pw1 = new PrintWriter("Runs/robust/SMM+SVM");

        int rank = 1;
        for(SearchResult sr : normalSMMExp){
            pw.println(qid+"\tQ0\t"+sr.getDocno()+"\t"+rank+"\t"+sr.getScore()+"\tSMMRobust");
            rank++;
        }

        rank = 1;
        for(SearchResult sr : svmSMMExp){
            pw1.println(qid+"\tQ0\t"+sr.getDocno()+"\t"+rank+"\t"+sr.getScore()+"\tSMMRobust+SVM");
            rank++;
        }

        pw.close();
        pw1.close();
        double p301 = EvalUtils.precision(normalSMMExp, qrels.get(qid), 30);
        double ap1 = EvalUtils.avgPrec(normalSMMExp, qrels.get(qid), top);

        double p302 = EvalUtils.precision(svmSMMExp, qrels.get(qid), 30);
        double ap2 = EvalUtils.avgPrec(svmSMMExp, qrels.get(qid), top);
        System.out.println("Index : robust04");
        System.out.println("Normal SMM: AP : "+ap1+" P@30 : "+ p301);
        System.out.println("SMM with SVM : AP : "+ap1+" P@30 : "+ p301);

    }
}

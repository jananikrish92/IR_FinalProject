/**
 * Created by jph on 19/12/16.
 */


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import java.io.File;
import java.util.List;

public class SVM_train_test
    {

        //class to train the model
        public svm_model svmTrain(File[] trainingFiles, String folder, int record_size, int feature_count, String modelFilename, String modelFolder ) throws IOException {

            //read the data from file and put it in the train module
            svm_model model = null;
           // System.out.println("File : " + Filename1);

            LinkedList<String> Dataset = new LinkedList<String>();//stores the lines from the given file
        for(File f : trainingFiles)
            {    try

            {
                if(f.getName().equals(".DS_Store"))
                    continue;

                String Filename1 = folder+"/"+f.getName();
                System.out.println("File : " + Filename1);
                FileReader fr1 = new FileReader(Filename1);

                BufferedReader br1 = new BufferedReader(fr1);

                for (String line1 = br1.readLine(); line1 != null; line1 = br1.readLine())

                {

                    Dataset.add(line1);

                }

                br1.close();

            } catch (Exception e)

            {

                e.printStackTrace();

            }
        }
            System.out.println("Dataset Size " + Dataset.size());

            record_size = Dataset.size();

            double node_values[][] = new double[record_size][]; //jagged array used to store values

            int node_indexes[][] = new int[record_size][];//jagged array used to store node indexes

            double node_class_labels[] = new double[record_size];//store class lavels


            //Now store data values

            for (int i = 0; i < Dataset.size(); i++)

            {

                try

                {

                    String[] data1 = Dataset.get(i).split("\\s");

                    node_class_labels[i] = Integer.parseInt(data1[0].trim());

                    System.out.println(Integer.parseInt(data1[0].trim()));
                    LinkedList<Integer> list_indx = new LinkedList<Integer>();

                    LinkedList<Double> list_val = new LinkedList<Double>();


                    for (int k = 0; k < data1.length; k++)

                    {

                        String[] tmp_data = data1[k].trim().split(":");

                        if (tmp_data.length == 2)

                        {

                            list_indx.add(Integer.parseInt(tmp_data[0].trim()));

                            list_val.add(Double.parseDouble(tmp_data[1].trim()));

                            System.out.println("Index  " + tmp_data[0] + " Value " + tmp_data[1]);

                        }

                    }

                    if (list_val.size() > 0)

                    {

                        node_values[i] = new double[list_val.size()];

                        node_indexes[i] = new int[list_indx.size()];

                    }

                    for (int m = 0; m < list_val.size(); m++)

                    {

                        node_indexes[i][m] = list_indx.get(m);

                        node_values[i][m] = list_val.get(m);

                        System.out.println("List Index value " + list_indx.get(m) + "  <=> List values " + list_val.get(m) + "  list size " + list_indx.size());

                    }

                } catch (Exception e)

                {

                    e.printStackTrace();

                }

            }


            svm_problem prob = new svm_problem();

            int dataCount = record_size;

            prob.y = new double[dataCount];

            prob.l = dataCount;

            prob.x = new svm_node[dataCount][];


            for (int i = 0; i < dataCount; i++)

            {

                prob.y[i] = node_class_labels[i];

                double[] values = node_values[i];

                int[] indexes = node_indexes[i];
                //System.out.println("Vals "+node_values[i]);
                prob.x[i] = new svm_node[values.length];

                for (int j = 0; j < values.length; j++)

                {

                    svm_node node = new svm_node();

                    node.index = indexes[j];

                    node.value = values[j];

                    prob.x[i][j] = node;

                }

            }


            svm_parameter param = new svm_parameter();

            param.probability = 1;

            param.gamma = 0.5;

            param.nu = 0.5;

            param.C = 1;

            param.svm_type = svm_parameter.C_SVC;

            param.kernel_type = svm_parameter.RBF;

            param.cache_size = 20000;

            param.eps = 0.001;

            double[] target = new double[record_size];
            svm.svm_cross_validation(prob, param, 5, target);

            model = svm.svm_train(prob, param);


            svm.svm_save_model(modelFolder+"/"+modelFilename, model);

            return model;

        }

        //write code to test all instances from given file
        public double evaluate_all_instances(String Filename1, String folder, String modelFilename, int record_size) throws IOException
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
            for(int i=0;i<Dataset.size();i++)

            {

                try

                {

                    String [] data1 = Dataset.get(i).split("\\s");



                    LinkedList<Integer> list_indx = new LinkedList<Integer>();

                    LinkedList<Double> list_val = new LinkedList<Double>();



                    for(int k=0;k<data1.length;k++)

                    {

                        String [] tmp_data = data1[k].trim().split(":");

                        if(tmp_data.length==2)

                        {

                            list_indx.add(Integer.parseInt(tmp_data[0].trim()));

                            list_val.add(Double.parseDouble(tmp_data[1].trim()));

                            System.out.println("Index  "+tmp_data[0]+" Value "+tmp_data[1]);

                        }
                        else
                            labels.add(Double.parseDouble(tmp_data[0].trim()));

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

            }

            accuracy /= record_size;

            return accuracy;

        }
        //write the code to test single feature each time by using SVM
        public double evaluate_single_instance(int [] indexes, double[] values, svm_model model1)
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

        public static void create_model(File[] trainingfiles, String folder, String modelFile, String modelFolder) throws IOException{

            int numofTrainFiles = trainingfiles.length -10;
            SVM_train_test t = new SVM_train_test();
            File[] trainFiles = Arrays.copyOfRange(trainingfiles, 0, numofTrainFiles);
            //for(int i = 0; i < numofTrainFiles; i++){
               //if(!trainingfiles[i].getName().equals(".DS_Store"))
                t.svmTrain(trainFiles, folder,  80, 9, modelFile, modelFolder);
            //}

           // return null;
        }


        public static void main(String[] args) throws IOException
        {

            SVM_train_test t123 = new SVM_train_test();
            File dir = new File("indexRobustOP/indexRobustOP/BaseLineRM1");
            File[] files = dir.listFiles();

            Arrays.sort(files);

            //create_model(files, "indexWtgOP/indexWtgOP/BaseLineRM3", "RM3.model", "indexWtgOP_models/BaseLineRM3");
            File[] testFiles = Arrays.copyOfRange(files, files.length-10, files.length);
            double accuracy = 0.0;
            for(int i = 0; i < testFiles.length; i++)
                accuracy += t123.evaluate_all_instances(testFiles[i].getName(), "indexRobustOP/indexRobustOP/BaseLineRM1",  "indexRobustOP_models/BaseLineRM1/RM1.model", 80);
                //testFiles[i].getName()
            accuracy /= testFiles.length;
            System.out.println("Average Accuracy : "+accuracy+"\nOperation complete");

        }

    }





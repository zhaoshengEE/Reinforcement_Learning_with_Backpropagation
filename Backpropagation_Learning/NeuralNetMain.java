import java.util.*;

public class NeuralNetMain {
    public static void main(String[] args) {
        int inputA = -1; /* switch to -1 in bipolar input set
                           switch to 0 in binary input set */
        int inputB = 1;
        int inputNum = 2;
        int outputNum = 1;
        int hiddenNum = 4;
        int epochTotal = 0;
        int epochAvg; // average number of epochs that is needed to satisfy the accept error
        double learningRate = 0.2;
        double momentum = 0.9;
        double acceptError = 0.05;

        /* switch between binary test and bipolar test here */
//        String dataType = "Binary";
        String dataType = "Bipolar";

        /* also switch between binary test and bipolar test here */
//        double[][] binaryInput = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
//        double[] binaryExpectedOutput = {0, 1, 1, 0};

        double[][] bipolarInput = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        double[] bipolarExpectedOutput = {-1, 1, 1, -1};

        /* Enter the total number of trials*/
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the number of trails you want to run: ");
        int trialTotal = scanner.nextInt();
        scanner.close();

        NeuralNetwork robot = new NeuralNetwork(inputA, inputB, inputNum, outputNum,
                hiddenNum, learningRate, momentum, dataType);

        for (int trial = 0; trial < trialTotal; trial += 1) {
            robot.initializeWeights();
            robot.zeroWeights();
            int epoch = 0;
            double errorRate = 0.0;

            while(epoch == 0 || errorRate > acceptError){
                errorRate = 0.0;
                /* The for loop indicates the algorithm for one epoch */
                for(int index = 0; index < hiddenNum; index += 1){
                    double[] input = bipolarInput[index]; // switch between binary test and bipolar test here
                    double output = bipolarExpectedOutput[index]; //switch between binary test and bipolar test here
                    errorRate += robot.train(input,output);
                }
                epoch += 1;
                System.out.println("Error at epoch " + epoch + " is " + errorRate);

                if(errorRate <= acceptError){
                    System.out.println("Accepted error reached at " + epoch);
                }
            }
            epochTotal += epoch;
        }

        epochAvg = epochTotal / trialTotal;
        System.out.println("Average convergence rate: " + epochAvg);
    }
}


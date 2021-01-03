import java.lang.Math;
import java.util.*;

public class NeuralNetwork {

    private int inputA;
    private int inputB;
    private int inputNum;
    private int outputNum;
    private int hiddenNum;
    private double learningRate;
    private double momentum;
    private double errorRate;
    private double sum; //used when calculating the delta signals in the hidden neurons
    private String dataType;

    double randomRangeMax = 0.5;
    double randomRangeMin = -0.5;
    Random rand = new Random();

    static int inputSize = 3;  // The additional one is the bias term
    static int hiddenSize = 5; // The additional one is the bias term
    static int outputSize = 1;
    final double bias = 1.0;

    public NeuralNetwork (int inputA, int inputB, int inputNum, int outputNum, int hiddenNum,
                           double learningRate, double momentum, String dataType){
        this.inputA = inputA;
        this.inputB = inputB;
        this.inputNum = inputNum;
        this.outputNum = outputNum;
        this.hiddenNum = hiddenNum;
        this.learningRate = learningRate;
        this.momentum = momentum;
        this.dataType = dataType;
    }

    /* NOTE: the last row of each array is to store bias*/
    private double[][] inputWeight = new double[inputSize][hiddenSize];
    private double[][] hiddenWeight = new double[hiddenSize][outputSize];

    /* The following two arrays is to store the change in input-to-hidden weights and
       hidden-to-output weights. These arrays is going to be used in backpropagation */
    private double[][] inputWeightDelta = new double[inputSize][hiddenSize];
    private double[][] hiddenWeightDelta = new double[hiddenSize][outputSize];

    /* Arrays to store activation in each neuron except the neuron in input stage*/
    private double[] inputNeuron = new double[inputSize];
    private double[] hiddenNeuron = new double[hiddenSize];
    private double[] outputNeuron = new double[outputSize];
    private double[] deltaHidden = new double[hiddenSize];
    private double[] deltaOutput = new double[outputSize];

    public void initializeWeights(){
        /* initialize the inputs' weights */
        for(int i = 0; i <= inputNum; i += 1){
            for(int j = 0; j < hiddenNum; j += 1){
                inputWeight[i][j] = (rand.nextDouble() * (randomRangeMax - randomRangeMin)) + randomRangeMin;
            }
        }

        /* initialize the hidden layers' weights */
        for(int k = 0; k <= hiddenNum; k += 1){
            for(int h = 0; h < outputNum; h += 1){
                hiddenWeight[k][h] = (rand.nextDouble() * (randomRangeMax - randomRangeMin)) + randomRangeMin;
            }
        }
    }

    public void zeroWeights(){
        for(int i = 0; i <= inputNum; i += 1) {
            for (int j = 0; j < hiddenNum; j += 1) {
                inputWeightDelta[i][j] = 0.0;
            }
        }

        for(int k = 0; k <= hiddenNum; k += 1) {
            for (int h = 0; h < outputNum; h += 1) {
                hiddenWeightDelta[k][h] = 0.0;
            }
        }
    }

    public double train(double [] X, double argValue){
        double trainOutput = outputFor(X); // Forward Propagation
        updateWeight(trainOutput, argValue); // Back Propagation
        errorRate = 0.5 * Math.pow((trainOutput - argValue), 2);
        return errorRate;
    }

    public double outputFor(double [] X){
        /* load each element into the input neuron with
            adding bias terms in input neuron and hidden neuron layer respectively*/
        for(int index = 0; index < inputNum; index += 1){
            inputNeuron[index] = X[index];
        }
        inputNeuron[inputNum] = bias;
        hiddenNeuron[hiddenNum] = bias;

        /* calculate the activation in each neuron in hidden layer */
        for(int i = 0; i < hiddenNum; i += 1){
            for(int j = 0; j <= inputNum; j += 1){
                hiddenNeuron[i] += inputWeight[j][i] * inputNeuron[j];
            }
            hiddenNeuron[i] = Sigmoid(hiddenNeuron[i]);
        }

        /* calculate the activation in each neuron in output layer */
        for(int k = 0; k < outputNum; k += 1){
            for(int h = 0; h <= hiddenNum; h += 1){
                outputNeuron[k] += hiddenWeight[h][k] * hiddenNeuron[h];
            }
            outputNeuron[k] = Sigmoid(outputNeuron[k]);
        }

        return outputNeuron[0];
    }

    public double Sigmoid(double x){
        return (inputB - inputA) / (1 + Math.exp(-x)) + inputA;
    }

    private void updateWeight(double trainOutput, double argValue){
        // deltaOutput[0] is the delta signal in the output neuron
        for(int outputIndex = 0; outputIndex < outputNum; outputIndex += 1) {
            if (dataType.equals("Binary")) {
                deltaOutput[outputIndex] = trainOutput * (1 - trainOutput) * (argValue - trainOutput);
            }
            else if (dataType.equals("Bipolar")) {
                deltaOutput[outputIndex] = 0.5 * (1 - Math.pow(trainOutput, 2)) * (argValue - trainOutput);
            }
        }

        // update hidden-to-output weights
        for(int h = 0; h < outputNum; h += 1) {
            for (int k = 0; k <= hiddenNum; k += 1) {
                hiddenWeight[k][h] +=  (momentum * hiddenWeightDelta[k][h]) + (learningRate * deltaOutput[h] * hiddenNeuron[k]);
                hiddenWeightDelta[k][h] = (momentum * hiddenWeightDelta[k][h]) + (learningRate * deltaOutput[h] * hiddenNeuron[k]);
            }
        }

        // deltaHidden[0] - deltaHidden[3] are the delta signals in the hidden neurons
       for(int k = 0; k < hiddenNum; k += 1){
           sum = 0;
           if (dataType.equals("Binary")){
               deltaHidden[k] = hiddenNeuron[k] * (1 - hiddenNeuron[k]);
           }
           else if (dataType.equals("Bipolar")){
               deltaHidden[k] = 0.5 * (1 - Math.pow(hiddenNeuron[k], 2));
           }

           for(int h = 0; h < outputNum; h += 1){
               sum += deltaOutput[h] * hiddenWeight[k][h];
           }
           deltaHidden[k] *= sum;
       }

        // update input-to-hidden weights
        for(int j = 0; j < hiddenNum; j += 1){
            for(int i = 0; i <= inputNum; i += 1){
                inputWeight[i][j] += (momentum * inputWeightDelta[i][j]) + (learningRate * deltaHidden[j] * inputNeuron[i]);
                inputWeightDelta[i][j] = (momentum * inputWeightDelta[i][j]) + (learningRate * deltaHidden[j] * inputNeuron[i]);
            }
        }
    }

}

package com.myRobotV2;

import java.util.*;
import java.io.*;

public class NeuralNetMainV2 {
    public static void main(String[] args) throws IOException{
        int argA = -1;
        int argB = 1;
        int inputNum = 5;
        int outputNum = 1;
        int hiddenNum = 6;

        int HPNum = 3; // Number of types of HP
        int distanceNum = 3; // Number of types of distance
        int actionNum = 5; // 5 kinds of actions in this project
        int numExpectedRows = HPNum * HPNum * distanceNum * distanceNum * actionNum;
        int numExpectedCols = 5;  // 4 states + 1 action

        double learningRate = 0.1;
        double momentum = 0.9;
        double acceptError = 0.1;
        double minOutput, maxOutput;

        String dataType = "Bipolar";

        /* Define input array and output array respectively */
        double [][] inputArray = new double[numExpectedRows][numExpectedCols];
        double [] expectOutput = new double[numExpectedRows];

        /* The array which contains the maximum value for each input */
        int [] inputMaxArray = {HPNum - 1, HPNum - 1, distanceNum - 1, distanceNum - 1, actionNum - 1};

        /* Initialize a Look-up Table and load the value got in last assignment */
        LookUpTableV2 LUT = new LookUpTableV2(HPNum, HPNum, distanceNum, distanceNum, actionNum);
        LUT.load2NN("LUT.dat", inputArray, expectOutput);

        normalizeInput(inputArray, inputMaxArray, argA, argB, numExpectedRows, numExpectedCols);
        maxOutput = getMaxOutput(expectOutput, numExpectedRows);
        minOutput = getMinOutput(expectOutput, numExpectedRows);
        normalizeOutput(expectOutput, maxOutput, minOutput, argA, argB, numExpectedRows);

        NeuralNetworkV2 robotNN = new NeuralNetworkV2(argA, argB, inputNum, outputNum,
                hiddenNum, learningRate, momentum, dataType);

        robotNN.initializeWeights();
        robotNN.zeroWeights();
        int epoch = 0;
        double errorRate = 0.0;
        int maxEpoch = 5000;
        while (epoch == 0 || errorRate > acceptError) {
//        while (epoch < maxEpoch) { /* Used when testing the hyper parameters */
            errorRate = 0.0;
            /* The for loop indicates the algorithm for one epoch */
            for (int index = 0; index < hiddenNum; index += 1) {
                double[] input = inputArray[index]; // switch between binary test and bipolar test here
                double output = expectOutput[index]; //switch between binary test and bipolar test here
                errorRate += robotNN.train(input, output);
            }
            epoch += 1;
            System.out.println("Error at epoch " + epoch + " is " + errorRate);
        }
    }

    /* Normalize input array to the range of [-1, 1] */
    public static void normalizeInput(double [][] inputArray, int [] inputMaxArray,
                                      int argA, int argB, int numExpectedRows, int numExpectedCols){
        int index = 0; /* Shows which input is being normalized */
        for(int col = 0; col < numExpectedCols; col += 1){
            for (int row = 0; row < numExpectedRows; row += 1){
                inputArray[row][col] = (argB - argA) + (inputArray[row][col] - 0) / (inputMaxArray[index]- 0) + argA;
            }
        }
    }

    /* Get the maximum Q value in the whole LUT */
    public static double getMaxOutput(double [] expectOutput, int numExpectedRows){
        double result = 0.0;
        for(int index = 0; index < numExpectedRows; index += 1){
            if(expectOutput[index] > result){
                result = expectOutput[index];
            }
        }
        return result;
    }

    /* Get the minimum Q value in the whole LUT */
    public static double getMinOutput(double [] expectOutput, int numExpectedRows){
        double result = 1.0;
        for(int index = 0; index < numExpectedRows; index += 1){
            if(expectOutput[index] < result){
                result = expectOutput[index];
            }
        }
        return result;
    }

    /* Normalize output array to the range of [-1, 1] */
    public static void normalizeOutput(double [] expectOutput, double maxOutput, double minOutput,
                                       int argA, int argB, int numExpectedRows){
        for(int index = 0; index < numExpectedRows; index += 1){
            expectOutput[index] = (argB - argA) * (expectOutput[index] - minOutput) / (maxOutput - minOutput) + argA;
        }
    }
}


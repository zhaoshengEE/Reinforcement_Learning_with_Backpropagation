package com.myRobot;


import robocode.*;

import java.lang.Math;
import java.util.*;
import java.io.*;

public class LookUpTable {
    private int inputASize;
    private int inputBSize;
    private int inputCSize;
    private int inputDSize;
    private int inputESize;
    private double[][][][][] LUT;
    private int[][][][][] visits; /* used to count the number of each state-
                                     action vector appears during testing */

    /* Constructor */
    public LookUpTable(int inputASize, int inputBSize, int inputCSize, int inputDSize, int inputESize){
        this.inputASize = inputASize;
        this.inputBSize = inputBSize;
        this.inputCSize = inputCSize;
        this.inputDSize = inputDSize;
        this.inputESize = inputESize;
        LUT = new double [inputASize][inputBSize][inputCSize][inputDSize][inputESize];
        visits = new int [inputASize][inputBSize][inputCSize][inputDSize][inputESize];
        this.initialize();
    }

    /* Initialize the Look-up Table and Visit Table */
    public void initialize(){
        for(int A = 0; A < inputASize; A += 1){
            for(int B = 0; B < inputBSize; B += 1){
                for(int C = 0; C < inputCSize; C += 1){
                    for(int D = 0; D < inputDSize; D += 1){
                        for(int E = 0; E < inputESize; E += 1){
                            LUT[A][B][C][D][E] = Math.random();
                            visits[A][B][C][D][E] = 0;
                        }
                    }
                }
            }
        }
    }

    /* Help robot tank select a random action for exploration on that action */
    public int getRandomAction(){
        Random random = new Random();
        return random.nextInt(inputESize);
    }

    /* Help robot tank select the greedy action that corresponds to the maximum Q value */
    public int getBestAction(int A, int B, int C, int D){
        double maxQ = Double.NEGATIVE_INFINITY;
        int bestActionIndex = 0;

        for(int i = 0; i < inputESize; i += 1){
            if(LUT[A][B][C][D][i] > maxQ){
                bestActionIndex = i;
                maxQ = LUT[A][B][C][D][i];
            }
        }

        return bestActionIndex;
    }

    /* Get the Q value from the Look-up Table according to the input indexes */
    public double getQValue(int A, int B, int C, int D, int E){
        return LUT[A][B][C][D][E];
    }

    /* Set the Q value to the Look-up and increase the counts in visits array */
    public void setQValue(double[] x, double argValue){
        int A = (int) x[0];
        int B = (int) x[1];
        int C = (int) x[2];
        int D = (int) x[3];
        int E = (int) x[4];

        visits[A][B][C][D][E] += 1;
        LUT[A][B][C][D][E] = argValue;
    }

    /* Save the LUT in a format useful for training an NN */
    public void save(File fileName) {
        PrintStream saveFile = null;

        try {
            saveFile = new PrintStream( new RobocodeFileOutputStream( fileName ));
        }
        catch (IOException e) {
            System.out.println( "*** Could not create output stream for NN save file.");
        }

        // First line is the number of rows of data
        saveFile.println(inputASize * inputBSize * inputCSize * inputDSize * inputESize);

        // Second line is the number of dimensions per row
        saveFile.println(5);

        for (int a = 0; a < inputASize; a++) {
            for (int b = 0; b < inputBSize; b++) {
                for (int c = 0; c < inputCSize; c++) {
                    for (int d = 0; d < inputDSize; d++) {
                        for (int e = 0; e < inputESize; e++) {
                            // e, d, e2, d2, a, q, visits
                            String row = String.format("%d,%d,%d,%d,%d,%2.5f,%d",
                                    a, b, c, d, e,
                                    LUT[a][b][c][d][e],
                                    visits[a][b][c][d][e]
                            );
                            saveFile.println(row);
                        }
                    }
                }
            }
        }
        saveFile.close();
    }

    /* Load the LUT in a format useful for training an NN */
    public void load(String fileName) throws IOException {

        FileInputStream inputFile = new FileInputStream( fileName );
        BufferedReader inputReader = new BufferedReader(new InputStreamReader( inputFile ));
        int numExpectedRows = inputASize * inputBSize * inputCSize * inputDSize * inputESize;

        // Check the number of rows is compatible
        int numRows = Integer.valueOf( inputReader.readLine() );
        // Check the number of dimensions is compatible
        int numDimensions = Integer.valueOf( inputReader.readLine() );

        if ( numRows != numExpectedRows || numDimensions != 5) {
            System.out.printf (
                    "*** rows/dimensions expected is %s/%s but %s/%s encountered\n",
                    numExpectedRows, 5, numRows, numDimensions
            );
            inputReader.close();
            throw new IOException();
        }

        for (int a = 0; a < inputASize; a++) {
            for (int b = 0; b < inputBSize; b++) {
                for (int c = 0; c < inputCSize; c++) {
                    for (int d = 0; d < inputDSize; d++) {
                        for (int e = 0; e < inputESize; e++) {

                            // Read line formatted like this: <e,d,e2,d2,a,q,visits\n>
                            String line = inputReader.readLine();
                            String tokens[] = line.split(",");
                            int dim1 = Integer.parseInt(tokens[0]);
                            int dim2 = Integer.parseInt(tokens[1]);
                            int dim3 = Integer.parseInt(tokens[2]);
                            int dim4 = Integer.parseInt(tokens[3]);
                            int dim5 = Integer.parseInt(tokens[4]); // actions
                            double q = Double.parseDouble(tokens[5]);
                            int v = Integer.parseInt(tokens[6]);
                            LUT[a][b][c][d][e] = q;
                            visits[a][b][c][d][e] = v;
                        }
                    }
                }
            }
        }
        inputReader.close();
    }
}

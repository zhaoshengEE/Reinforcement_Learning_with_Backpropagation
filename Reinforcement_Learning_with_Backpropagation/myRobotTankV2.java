package com.myRobotV2;

import robocode.*;

import java.awt.*;
import java.util.*;
import java.io.*;
import java.lang.*;

public class myRobotTankV2 extends AdvancedRobot{
    public enum enumHP {low, medium, high};
    public enum enumDistance {veryClose, near, far};
    public enum enumAction {fire, forward, backward, left, right}
    public enum enumOperationMode {scan, performAction};

//    private enumHP currentMyHp = enumHP.high;
//    private enumHP currentEnemyHp = enumHP.high;
//    private enumDistance currentDistance2Enemy = enumDistance.near;
//    private enumDistance currentDistance2Centre = enumDistance.near;
    private double currentMyHp = 100.0;
    private double currentEnemyHp = 100.0;
    private double currentDistance2Enemy = 400.0;
    private double currentDistance2Centre = 200.0;
    private enumAction currentAction = enumAction.forward;

//    private enumHP previousMyHp = enumHP.low;
//    private enumHP previousEnemyHp = enumHP.low;
//    private enumDistance previousDistance2Enemy = enumDistance.veryClose;
//    private enumDistance previousDistance2Centre = enumDistance.veryClose;
    private double previousMyHp = 10.0;
    private double previousEnemyHp = 10.0;
    private double previousDistance2Enemy = 50.0;
    private double previousDistance2Centre = 40.0;
    private enumAction previousAction = enumAction.forward;

    private enumOperationMode operationMode = enumOperationMode.scan;

    public double myX = 0.0;
    public double myY = 0.0;
    public double myHP = 0.0;

    public double enemyBearing = 0.0;
    public double enemyDistance = 0.0;
    public double centreDistance = 0.0;
    public double enemyHP = 0.0;

    public double xCentre;
    public double yCentre;

    public static boolean intermediate = true; /* true: Take intermediateBonus and intermediatePenalty into account
                                                  false: Only consider the terminalBonus and terminalPenalty */
    public static boolean onPolicy = false; /* true: on-policy
                                              false: off-policy */

    private double gamma = 0.9; // discount factor
    private double alpha = 0.1; // learning rate
    private double epsilon = 0.0; // random number for epsilon-greedy policy (i.e. exploration rate)

    /* Bonus and Penalty */
    private final double intermediateBonus = 0.4;
    private final double terminalBonus = 0.7;
    private final double intermediatePenalty = -0.1;
    private final double terminalPenalty = -0.3;

    static int totalRound = 0; // the maximum should be 7000 rounds
    static int winRound = 0;
    static int numRoundsTo20 = 0; // compute the winning rate every 100 rounds
    static double winningRate = 0.0;
    public static double reward = 0.0;
    public static int currentActionIndex;
    public double Q = 0.0;

    static String logFilename = "myRobotTankV2-logfile.log";
    static LogFileV2 log = null;

    static LookUpTableV2 LUT = new LookUpTableV2(
            enumHP.values().length,         // my robot tank's energy
            enumHP.values().length,         // enemy robot tank's energy
            enumDistance.values().length,   // distance to enemy
            enumDistance.values().length,   // distance to the centre of battle field
            enumAction.values().length);    // my robot tank's actions

    public static boolean onlineLearning = true; /* true: learning with neural network
                                                     false: learning with look-up table */
    public static double learningRate = 0.1;
    public static double momentum = 0.9;
    public static int argA = -1;
    public static int argB = 1;
    public static int inputNum = 5; // MyHp, EnemyHp, Distance2Enemy, Distance2Centre, Action
    public static int outputNum = 1;
    public static int hiddenNum = 9;
    public static int actionType = enumAction.values().length;
    public static double error = 0.0;
    public static double errorRate = 0.0;

    static NeuralNetworkV2 neuralNetwork = new NeuralNetworkV2(argA, argB, inputNum, outputNum,
            hiddenNum, learningRate, momentum, "Bipolar");

    public void run(){
        if(onlineLearning){
            neuralNetwork.initializeWeights();
            neuralNetwork.zeroWeights();
            }

        /* Customize the robot tank */
        setBulletColor(Color.orange);
        setGunColor(Color.blue);
        setBodyColor(Color.red);
        setRadarColor(Color.white);

        /* Get the coordinate of the centre of the battle field */
        xCentre = getBattleFieldWidth() / 2;
        yCentre = getBattleFieldHeight() / 2;

        if (log == null) {
            log = new LogFileV2(getDataFile(logFilename));
            log.stream.printf("gamma,   %2.2f\n", gamma);
            log.stream.printf("alpha,   %2.2f\n", alpha);
            log.stream.printf("epsilon, %2.2f\n", epsilon);
            log.stream.printf("badIntermediateReward, %2.2f\n", intermediatePenalty);
            log.stream.printf("badTerminalReward, %2.2f\n", terminalPenalty);
            log.stream.printf("goodIntermediateReward, %2.2f\n", intermediateBonus);
            log.stream.printf("goodTerminalReward, %2.2f\n\n", terminalBonus);
        }

        while (true){
            //epsilon = (totalRound < 20000) ? 0.3 : 0.0;

            switch (operationMode){
                case scan: {
                    reward = 0.0;
                    turnRadarLeft(180);
                    break;
                }
                case performAction: {

                    centreDistance = getDistanceToCentre(myX, myY, xCentre, yCentre);

                    if(onlineLearning){
                        currentActionIndex = (Math.random() <= epsilon)
                                    ? getRandomAction()
                                    : selectBestActionNN(myHP, enemyHP, enemyDistance, centreDistance);
//                                    : selectBestActionNN(getHPType(myHP).ordinal(),
//                                                        getHPType(enemyHP).ordinal(),
//                                                        getDistanceType(enemyDistance).ordinal(),
//                                                        getDistanceType(centreDistance).ordinal());
                    }
                    else {
                        currentActionIndex = (Math.random() <= epsilon)
                                ? LUT.getRandomAction() // explore a random action
                                : LUT.getBestAction(
                                getHPType(myHP).ordinal(),
                                getHPType(enemyHP).ordinal(),
                                getDistanceType(enemyDistance).ordinal(),
                                getDistanceType(centreDistance).ordinal()); // select greedy action
                    }

                    currentAction = enumAction.values()[currentActionIndex];

                    switch(currentAction){
                        case fire:{
                            turnGunRight(getHeading() - getGunHeading() + enemyBearing);
                            fire(3);
                            break;
                        }

                        case forward:{
                            setAhead(100);
                            execute();
                            break;
                        }
                        case backward:{
                            setBack(100);
                            execute();
                            break;
                        }

                        case left:{
                            setTurnLeft(30);
                            setAhead(100);
                            execute();
                            break;
                        }

                        case right:{
                            setTurnRight(30);
                            setAhead(100);
                            execute();
                            break;
                        }
                    }

//                    double[] indexes = new double []{
//                            previousMyHp.ordinal(),
//                            previousEnemyHp.ordinal(),
//                            previousDistance2Enemy.ordinal(),
//                            previousDistance2Centre.ordinal(),
//                            previousAction.ordinal()};
                    double[] indexes = new double []{
                            previousMyHp,
                            previousEnemyHp,
                            previousDistance2Enemy,
                            previousDistance2Centre,
                            previousAction.ordinal()};

                    if(onlineLearning){
                        Q = computeQNN(reward, onPolicy);
//                        error += neuralNetwork.train(indexes, Q);
                    }
//                    else {
//                        Q = computeQ(reward, onPolicy);
//                        LUT.setQValue(indexes, Q);
//                    }
                    operationMode = enumOperationMode.scan;
                }
            }
        }
    }

    /* Help robot tank select a random action for exploration on that action */
    public int getRandomAction(){
        Random random = new Random();
        return random.nextInt(actionType);
    }

    /* Help robot tank select a the action that yields the largest Q value through neural network */
    public int selectBestActionNN(double myHP, double enemyHP, double enemyDistance, double centreDistance){
        double maxQ = Double.NEGATIVE_INFINITY;
        int bestActionIndex = 0;

        for(int index = 0; index < actionType; index += 1){
            double actionIndex = index;
            double [] statesAction = {myHP, enemyHP, enemyDistance, centreDistance, actionIndex};
            if(neuralNetwork.outputFor(statesAction) > maxQ){
                maxQ = neuralNetwork.outputFor(statesAction);
                bestActionIndex = index;
            }
        }

        return bestActionIndex;
    }


    /* Compute the distance to the centre of the battle field */
    public double getDistanceToCentre(double x, double y, double xCentre, double yCentre){
        return Math.sqrt(Math.pow((x - xCentre), 2) + Math.pow((y - yCentre), 2));
    }

    /* Get the specific type of energy (i.e. low, medium, high) from the value */
    public enumHP getHPType(double HP){
        enumHP resultHP = null;

        if(HP <= 33) resultHP = enumHP.low;
        else if(HP <= 67)  resultHP = enumHP.medium;
        else resultHP = enumHP.high;

        return resultHP;
    }

    /* Get the specific type of distance (i.e. veryClose, near, far) from the value */
    public enumDistance getDistanceType(double distance){
        enumDistance resultDistance = null;

        if(distance <= 100) resultDistance = enumDistance.veryClose;
        else if(distance <= 400)  resultDistance = enumDistance.near;
        else resultDistance = enumDistance.far;

        return resultDistance;
    }

    /* Compute the Q value via on-policy / off-policy and neural network */
    public double computeQNN(double reward, boolean onPolicy){
//        double [] previousInput = {previousMyHp.ordinal(),
//                                    previousEnemyHp.ordinal(),
//                                    previousDistance2Enemy.ordinal(),
//                                    previousDistance2Centre.ordinal(),
//                                    previousAction.ordinal()};
//        double [] currentInput = {currentMyHp.ordinal(),
//                                    currentEnemyHp.ordinal(),
//                                    currentDistance2Enemy.ordinal(),
//                                    currentDistance2Centre.ordinal(),
//                                    currentAction.ordinal()};
//        int bestActionIndex = selectBestActionNN(currentMyHp.ordinal(),
//                currentEnemyHp.ordinal(),
//                currentDistance2Enemy.ordinal(),
//                currentDistance2Centre.ordinal());
//
//        double [] bestQInput = {currentMyHp.ordinal(),
//                                currentEnemyHp.ordinal(),
//                                currentDistance2Enemy.ordinal(),
//                                currentDistance2Centre.ordinal(),
//                                bestActionIndex};

        double [] previousInput = {previousMyHp,
                previousEnemyHp,
                previousDistance2Enemy,
                previousDistance2Centre,
                previousAction.ordinal()};
        double [] currentInput = {currentMyHp,
                currentEnemyHp,
                currentDistance2Enemy,
                currentDistance2Centre,
                currentAction.ordinal()};
        int bestActionIndex = selectBestActionNN(currentMyHp,
                currentEnemyHp,
                currentDistance2Enemy,
                currentDistance2Centre);

        double [] bestQInput = {currentMyHp,
                currentEnemyHp,
                currentDistance2Enemy,
                currentDistance2Centre,
                bestActionIndex};

        double previousQ = neuralNetwork.outputFor(previousInput);

        double currentQ = neuralNetwork.outputFor(currentInput);

        double bestActionQ = neuralNetwork.outputFor(bestQInput);

        error += Math.abs(currentQ - bestActionQ);

        double resultQ = (onPolicy)
                ? previousQ + alpha * (reward + gamma * currentQ - previousQ)
                : previousQ + alpha * (reward + gamma * bestActionQ - previousQ);
        return resultQ;
    }

    /* Compute the Q value via on-policy / off-policy */
//    public double computeQ(double reward, boolean onPolicy){
//        double previousQ = LUT.getQValue(previousMyHp.ordinal(),
//                previousEnemyHp.ordinal(),
//                previousDistance2Enemy.ordinal(),
//                previousDistance2Centre.ordinal(),
//                previousAction.ordinal());
//
//        double currentQ = LUT.getQValue(currentMyHp.ordinal(),
//                currentEnemyHp.ordinal(),
//                currentDistance2Enemy.ordinal(),
//                currentDistance2Centre.ordinal(),
//                currentAction.ordinal());
//
//        int bestActionIndex = LUT.getBestAction(currentMyHp.ordinal(),
//                currentEnemyHp.ordinal(),
//                currentDistance2Enemy.ordinal(),
//                currentDistance2Centre.ordinal());
//
//        double bestActionQ = LUT.getQValue(currentMyHp.ordinal(),
//                currentEnemyHp.ordinal(),
//                currentDistance2Enemy.ordinal(),
//                currentDistance2Centre.ordinal(),
//                bestActionIndex);
//
//        double resultQ = (onPolicy)
//                ? previousQ + alpha * (reward + gamma * currentQ - previousQ)
//                : previousQ + alpha * (reward + gamma * bestActionQ - previousQ);
//        return resultQ;
//    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e){
        myX = getX();
        myY = getY();
        myHP = getEnergy();
        enemyHP = e.getEnergy();
        enemyDistance = e.getDistance();
        enemyBearing = e.getBearing();

        previousMyHp = currentMyHp;
        previousEnemyHp = currentEnemyHp;
        previousDistance2Enemy = currentDistance2Enemy;
        previousDistance2Centre = currentDistance2Centre;
        previousAction = currentAction;

//        currentMyHp = getHPType(getEnergy());
//        currentEnemyHp = getHPType(e.getEnergy());
//        currentDistance2Enemy = getDistanceType(e.getDistance());
//        currentDistance2Centre = getDistanceType(getDistanceToCentre(myX, myY, xCentre, yCentre));
        currentMyHp = getEnergy();
        currentEnemyHp = e.getEnergy();
        currentDistance2Enemy = e.getDistance();
        currentDistance2Centre = getDistanceToCentre(myX, myY, xCentre, yCentre);

        operationMode = enumOperationMode.performAction;
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e){
        if(intermediate) reward = intermediatePenalty;
    }

    @Override
    public void onBulletHit(BulletHitEvent e){
        if(intermediate) reward = intermediateBonus;
    }

    @Override
    public void onBulletMissed(BulletMissedEvent e){
        if(intermediate) reward = intermediatePenalty;
    }

    @Override
    public void onHitWall(HitWallEvent e){
        if(intermediate) reward = intermediatePenalty;
    }

    @Override
    public void onWin(WinEvent e){
        saveTable();
        reward = terminalBonus;
        double[] indexes = new double []{
                previousMyHp,
                previousEnemyHp,
                previousDistance2Enemy,
                previousDistance2Centre,
                previousAction.ordinal()};
//        double[] indexes = new double []{
//                previousMyHp.ordinal(),
//                previousEnemyHp.ordinal(),
//                previousDistance2Enemy.ordinal(),
//                previousDistance2Centre.ordinal(),
//                previousAction.ordinal()};
//        Q = computeQ(reward, onPolicy);
//        LUT.setQValue(indexes, Q);
        if(onlineLearning){
            Q = computeQNN(reward, onPolicy);
//            error += neuralNetwork.train(indexes, Q);
        }
//        else {
//            Q = computeQ(reward, onPolicy);
//            LUT.setQValue(indexes, Q);
//        }
        if(numRoundsTo20 < 100){
            numRoundsTo20 += 1;
            totalRound += 1;
            winRound += 1;
        }
        else{
            winningRate = ((double) winRound / numRoundsTo20) * 100;
            errorRate = (error / numRoundsTo20) * 100;
            log.stream.printf("Winning rate: %2.1f Error rate: %2.3f\n", winningRate, errorRate);
            log.stream.flush();
            numRoundsTo20 = 0;
            winRound = 0;
            error = 0.0;
        }
    }

    @Override
    public void onDeath(DeathEvent e){
        saveTable();
        reward = terminalPenalty;
        double[] indexes = new double []{
                previousMyHp,
                previousEnemyHp,
                previousDistance2Enemy,
                previousDistance2Centre,
                previousAction.ordinal()};
//        double[] indexes = new double []{
//                previousMyHp.ordinal(),
//                previousEnemyHp.ordinal(),
//                previousDistance2Enemy.ordinal(),
//                previousDistance2Centre.ordinal(),
//                previousAction.ordinal()};
//        Q = computeQ(reward, onPolicy);
//        LUT.setQValue(indexes, Q);
        if(onlineLearning){
            Q = computeQNN(reward, onPolicy);
//            error += neuralNetwork.train(indexes, Q);
        }
//        else {
//            Q = computeQ(reward, onPolicy);
//            LUT.setQValue(indexes, Q);
//        }

        if(numRoundsTo20 < 100){
            numRoundsTo20 += 1;
            totalRound += 1;
        }
        else{
            winningRate = ((double) winRound / numRoundsTo20) * 100;
            errorRate = (error / numRoundsTo20) * 100;
            log.stream.printf("Winning rate: %2.1f Error rate: %2.3f\n", winningRate, errorRate);
            log.stream.flush();
            numRoundsTo20 = 0;
            winRound = 0;
            error = 0.0;
        }
    }

    public void saveTable() {
        try {
            LUT.save(getDataFile("LUT.dat"));
        } catch (Exception e) {
            System.out.println("Save Error!" + e);
        }
    }

    public void loadTable() {
        try {
            LUT.load("LUT.dat");
        } catch (Exception e) {
            System.out.println("Save Error!" + e);
        }
    }
}

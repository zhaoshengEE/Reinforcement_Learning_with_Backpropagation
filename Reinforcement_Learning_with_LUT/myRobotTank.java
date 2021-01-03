package com.myRobot;

import robocode.*;

import java.awt.*;
import java.util.*;
import java.io.*;
import java.lang.*;

public class myRobotTank extends AdvancedRobot{
    public enum enumHP {low, medium, high};
    public enum enumDistance {veryClose, near, far};
    public enum enumAction {fire, forward, backward, left, right}
    public enum enumOperationMode {scan, performAction};

    private enumHP currentMyHp = enumHP.high;
    private enumHP currentEnemyHp = enumHP.high;
    private enumDistance currentDistance2Enemy = enumDistance.near;
    private enumDistance currentDistance2Centre = enumDistance.near;
    private enumAction currentAction = enumAction.forward;

    private enumHP previousMyHp = enumHP.low;
    private enumHP previousEnemyHp = enumHP.low;
    private enumDistance previousDistance2Enemy = enumDistance.veryClose;
    private enumDistance previousDistance2Centre = enumDistance.veryClose;
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

    public static boolean intermediate = false; /* true: Take intermediateBonus and intermediatePenalty into account
                                                  false: Only consider the terminalBonus and terminalPenalty */
    public static boolean onPolicy = false; /* true: on-policy
                                              false: off-policy */
    private double gamma = 0.1; // discount factor
    private double alpha = 0.1; // learning rate
    private double epsilon = 0.0; // random number for epsilon-greedy policy (i.e. exploration rate)

    /* Bonus and Penalty */
    private final double intermediateBonus = 1.0;
    private final double terminalBonus = 2.0;
    private final double intermediatePenalty = -0.25;
    private final double terminalPenalty = -0.5;

    static int totalRound = 0; // the maximum should be 7000 rounds
    static int winRound = 0;
    static int numRoundsTo20 = 0; // compute the winning rate every 100 rounds
    static double winningRate = 0.0;
    public static double reward = 0.0;
    public static int currentActionIndex;
    public double Q = 0.0;

    static String logFilename = "myRobotTank-logfile.log";
    static LogFile log = null;

    static LookUpTable LUT = new LookUpTable(
            enumHP.values().length,         // my robot tank's energy
            enumHP.values().length,         // enemy robot tank's energy
            enumDistance.values().length,   // distance to enemy
            enumDistance.values().length,   // distance to the centre of battle field
            enumAction.values().length);    // my robot tank's actions

    public void run(){
        /* Customize the robot tank */
        setBulletColor(Color.orange);
        setGunColor(Color.blue);
        setBodyColor(Color.red);
        setRadarColor(Color.white);

        /* Get the coordinate of the centre of the battle field */
        xCentre = getBattleFieldWidth() / 2;
        yCentre = getBattleFieldHeight() / 2;

        if (log == null) {
            log = new LogFile(getDataFile(logFilename));
            log.stream.printf("gamma,   %2.2f\n", gamma);
            log.stream.printf("alpha,   %2.2f\n", alpha);
            log.stream.printf("epsilon, %2.2f\n", epsilon);
            log.stream.printf("badIntermediateReward, %2.2f\n", intermediatePenalty);
            log.stream.printf("badTerminalReward, %2.2f\n", terminalPenalty);
            log.stream.printf("goodIntermediateReward, %2.2f\n", intermediateBonus);
            log.stream.printf("goodTerminalReward, %2.2f\n\n", terminalBonus);
        }

        while (true){

            switch (operationMode){
                case scan: {
                    reward = 0.0;
                    turnRadarLeft(180);
                    break;
                }
                case performAction: {
                    centreDistance = getDistanceToCentre(myX, myY, xCentre, yCentre);

                    currentActionIndex = (Math.random() <= epsilon)
                            ? LUT.getRandomAction() // explore a random action
                            : LUT.getBestAction(
                            getHPType(myHP).ordinal(),
                            getHPType(enemyHP).ordinal(),
                            getDistanceType(enemyDistance).ordinal(),
                            getDistanceType(centreDistance).ordinal()); // select greedy action

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

                    double[] indexes = new double []{
                                        previousMyHp.ordinal(),
                                        previousEnemyHp.ordinal(),
                                        previousDistance2Enemy.ordinal(),
                                        previousDistance2Centre.ordinal(),
                                        previousAction.ordinal()};
                    Q = computeQ(reward, onPolicy);
                    LUT.setQValue(indexes, Q);
                    operationMode = enumOperationMode.scan;
                }
            }
        }
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

    /* Compute the Q value via on-policy / off-policy */
    public double computeQ(double reward, boolean onPolicy){
         double previousQ = LUT.getQValue(previousMyHp.ordinal(),
                                          previousEnemyHp.ordinal(),
                                          previousDistance2Enemy.ordinal(),
                                          previousDistance2Centre.ordinal(),
                                          previousAction.ordinal());

        double currentQ = LUT.getQValue(currentMyHp.ordinal(),
                                        currentEnemyHp.ordinal(),
                                        currentDistance2Enemy.ordinal(),
                                        currentDistance2Centre.ordinal(),
                                        currentAction.ordinal());

         int bestActionIndex = LUT.getBestAction(currentMyHp.ordinal(),
                                                 currentEnemyHp.ordinal(),
                                                 currentDistance2Enemy.ordinal(),
                                                 currentDistance2Centre.ordinal());

         double bestActionQ = LUT.getQValue(currentMyHp.ordinal(),
                                            currentEnemyHp.ordinal(),
                                            currentDistance2Enemy.ordinal(),
                                            currentDistance2Centre.ordinal(),
                                            bestActionIndex);

         double resultQ = (onPolicy)
                 ? previousQ + alpha * (reward + gamma * currentQ - previousQ)
                 : previousQ + alpha * (reward + gamma * bestActionQ - previousQ);
         return resultQ;
    }

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

        currentMyHp = getHPType(getEnergy());
        currentEnemyHp = getHPType(e.getEnergy());
        currentDistance2Enemy = getDistanceType(e.getDistance());
        currentDistance2Centre = getDistanceType(getDistanceToCentre(myX, myY, xCentre, yCentre));

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
                previousMyHp.ordinal(),
                previousEnemyHp.ordinal(),
                previousDistance2Enemy.ordinal(),
                previousDistance2Centre.ordinal(),
                previousAction.ordinal()};
        Q = computeQ(reward, onPolicy);
        LUT.setQValue(indexes, Q);

        if(numRoundsTo20 < 100){
            numRoundsTo20 += 1;
            totalRound += 1;
            winRound += 1;
        }
        else{
            winningRate = ((double) winRound / numRoundsTo20) * 100;
            log.stream.printf("Winning rate: %2.1f\n", winningRate);
            log.stream.flush();
            numRoundsTo20 = 0;
            winRound = 0;
        }
    }

    @Override
    public void onDeath(DeathEvent e){
        saveTable();
        reward = terminalPenalty;
        double[] indexes = new double []{
                previousMyHp.ordinal(),
                previousEnemyHp.ordinal(),
                previousDistance2Enemy.ordinal(),
                previousDistance2Centre.ordinal(),
                previousAction.ordinal()};
        Q = computeQ(reward, onPolicy);
        LUT.setQValue(indexes, Q);

        if(numRoundsTo20 < 100){
            numRoundsTo20 += 1;
            totalRound += 1;
        }
        else{
            winningRate = ((double) winRound / numRoundsTo20) * 100;
            System.out.println("Winning rate: " + winningRate);
            log.stream.printf("Winning rate: %2.1f\n", winningRate);
            log.stream.flush();
            numRoundsTo20 = 0;
            winRound = 0;
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

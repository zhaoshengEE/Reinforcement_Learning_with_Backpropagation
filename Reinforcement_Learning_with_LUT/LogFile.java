package com.myRobot;


import robocode.*;
import java.io.*;

public class LogFile {
    PrintStream stream;

    public LogFile ( File argFile ) {
        try {
            stream = new PrintStream( new RobocodeFileOutputStream( argFile ));
            System.out.println( "--+ Log file created." );
        } catch (IOException e) {
            System.out.println( "*** IO exception during file creation attempt.");
        }
    }

    public void print( String argString ) {
        stream.print( argString );
    }

    public void println( String argString ) {
        stream.println( argString );
    }
}

package com.myRobotV2;

import robocode.*;
import java.io.*;

public class LogFileV2 {
    PrintStream stream;

    public LogFileV2 ( File argFile ) {
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

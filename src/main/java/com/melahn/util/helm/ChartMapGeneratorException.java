package com.melahn.util.helm;

import java.util.UUID;

/*
 *  ChartMapGeneratorException is an exception class designed for use by ChartMapGenerator.
*/

public class ChartMapGeneratorException extends Exception { 
    static final long serialVersionUID = UUID.fromString("5a8dba66-71e1-492c-bf3b-53cceb67b785").getLeastSignificantBits();

    public ChartMapGeneratorException(String message) {
        super(message);
    }
}

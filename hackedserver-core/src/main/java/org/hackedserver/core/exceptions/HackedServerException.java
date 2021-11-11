package org.hackedserver.core.exceptions;

public class HackedServerException extends Exception {

    public final String advice;

    public HackedServerException(String advice, String errorMessage) {
        super(errorMessage);
        this.advice = advice;
    }
}
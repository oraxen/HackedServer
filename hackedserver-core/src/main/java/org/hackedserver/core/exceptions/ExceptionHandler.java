package org.hackedserver.core.exceptions;

import java.util.logging.Logger;

public class ExceptionHandler {

    private final Exception exception;

    public ExceptionHandler(Exception exception) {
        this.exception = exception;
    }

    public void fire(Logger logger) {
        if (exception instanceof HackedServerException guardianException)
            logger.severe("A " + guardianException.getClass() + " was fired. " + guardianException.advice + " Advanced stacktrace:");
        else
            logger.severe("An unexpected error has occurred. Please contact the developer with this stacktrace :");
        exception.printStackTrace();
    }

    public void fire(Logger logger, String advice) {
        logger.severe("An " + exception.getClass() + " has occurred. " + advice + " Advanced stacktrace:");
        exception.printStackTrace();
    }

}

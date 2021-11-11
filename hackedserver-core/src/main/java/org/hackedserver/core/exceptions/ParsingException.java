package org.hackedserver.core.exceptions;

public class ParsingException extends HackedServerException {

    public ParsingException(String errorMessage) {
        super("This error is related to an invalid configuration. It is very unlikely that this is a HackedServer problem, please read the additional information carefully.", errorMessage);
    }

}

package com.likhithraju.sonar.validation;

public class InvalidEventException extends Exception{
    public InvalidEventException(String reason){
        super(reason);
    }
}

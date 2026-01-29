package com.psjw.springbatchex.entity;

public class InvalidPaymentAmountException extends RuntimeException{

    public InvalidPaymentAmountException(String msg) {
        super(msg);
    }
}

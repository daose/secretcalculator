package com.daose.secretcalculator;

/**
 * Created by student on 29/06/16.
 */
public interface KnurldListener{
    public abstract void KnurldSuccess();
    public abstract void KnurldFailure();
    void vocabReceived(String[] vocab);
}

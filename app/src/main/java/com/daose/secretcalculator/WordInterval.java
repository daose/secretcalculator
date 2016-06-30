package com.daose.secretcalculator;

import java.util.Comparator;

public class WordInterval implements Comparator<WordInterval>, Comparable<WordInterval>{

    private int start;
    private int stop;

    public WordInterval(int start, int stop){
        this.start = start;
        this.stop = stop;
    }

    public int getStartTime(){
        return start;
    }

    public int getStopTime(){
        return stop;
    }

    public int getDuration(){
        return stop - start;
    }

    @Override
    public String toString(){
        return String.format("Start: %d ms - Stop: %d ms", start, stop);
    }

    @Override
    public int compare(WordInterval a, WordInterval b){
        return b.getDuration() - a.getDuration();
    }

    @Override
    public int compareTo(WordInterval a){
        return a.getDuration() - this.getDuration();
    }
}

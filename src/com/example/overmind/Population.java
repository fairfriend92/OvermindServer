package com.example.overmind;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class that contains information about a population living inside a terminal
 * @author rodolforocco
 *
 */

public class Population implements Serializable {
    public short numOfNeurons, numOfDendrites, numOfSynapses;
    public int id;
    public ArrayList<Integer> inputIndexes = new ArrayList<>();
    public ArrayList<Integer> outputIndexes = new ArrayList<>();

    public Population(short numOfNeurons, short numOfDendrites, short numOfSynapses) {
        this.numOfNeurons = numOfNeurons;
        this.numOfDendrites = numOfDendrites;
        this.numOfSynapses = numOfSynapses;
        this.id = this.hashCode(); // TODO: Use the 2 ArrayLists?
        
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        else if (obj.getClass().equals(Integer.class)) {
        	return this.id == Integer.valueOf((Integer)obj);
        }
        Population compare = (Population) obj;
        return this.id == compare.id; 
    }
}

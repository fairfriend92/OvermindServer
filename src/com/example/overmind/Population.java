package com.example.overmind;

import java.io.Serializable;

/**
 * Class containing information about the population of neurons living on any given terminal
 */

public class Population implements Serializable {
    public short numOfNeurons, numOfDendrites, numOfSynapses;
    public int id;

    public Population(short numOfNeurons, short numOfDendrites, short numOfSynapses) {
        this.numOfNeurons = numOfNeurons;
        this.numOfDendrites = numOfDendrites;
        this.numOfSynapses = numOfSynapses;
        this.id = this.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        Population compare = (Population) obj;
        return this.id == compare.id;
    }
}

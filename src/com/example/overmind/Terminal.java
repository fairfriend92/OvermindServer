package com.example.overmind;

import java.io.Serializable;
import java.util.ArrayList;

public class Terminal implements Serializable {
    public short numOfNeurons, numOfDendrites, numOfSynapses;
    public String ip;
    public int natPort;
    public ArrayList<Terminal> presynapticTerminals;
    public ArrayList<Terminal> postsynapticTerminals;

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        Terminal compare = (Terminal) obj;
        return compare.ip.equals(ip);
    }

    public synchronized Terminal get() {
        return this;
    }

    public synchronized void update(Terminal updatedLN) {
        this.numOfNeurons = updatedLN.numOfNeurons;
        this.numOfDendrites = updatedLN.numOfDendrites;
        this.numOfSynapses = updatedLN.numOfSynapses;
        this.ip = updatedLN.ip;
        this.natPort = updatedLN.natPort;
        this.presynapticTerminals = new ArrayList<>(updatedLN.presynapticTerminals);
        this.postsynapticTerminals = new ArrayList<>(updatedLN.postsynapticTerminals);
    }
}

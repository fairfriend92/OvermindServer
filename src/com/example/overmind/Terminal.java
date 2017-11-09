package com.example.overmind;

import java.io.Serializable;
import java.util.ArrayList;

public class Terminal implements Serializable {
    public short numOfNeurons, numOfDendrites, numOfSynapses;
    public String serverIP;
    public String ip;
    public int natPort;
    public ArrayList<Terminal> presynapticTerminals;
    public ArrayList<Terminal> postsynapticTerminals;
    public float[] newWeights = new float[0];
    public int[] newWeightsIndexes = new int[0];

    public Terminal () {
        presynapticTerminals = new ArrayList<>();
        postsynapticTerminals = new ArrayList<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        Terminal compare = (Terminal) obj;
        return (compare.ip.equals(this.ip) & compare.natPort == this.natPort);
    }

    public synchronized Terminal get() {
        return this;
    }

    public synchronized void update(Terminal updatedTerminal) {
        this.numOfNeurons = updatedTerminal.numOfNeurons;
        this.numOfDendrites = updatedTerminal.numOfDendrites;
        this.numOfSynapses = updatedTerminal.numOfSynapses;
        this.ip = updatedTerminal.ip;
        this.natPort = updatedTerminal.natPort;
        this.presynapticTerminals = new ArrayList<>(updatedTerminal.presynapticTerminals);
        this.postsynapticTerminals = new ArrayList<>(updatedTerminal.postsynapticTerminals);
        int length = updatedTerminal.newWeights.length;
        newWeights = new float[length];
        newWeightsIndexes = new int[length];
        System.arraycopy(updatedTerminal.newWeights, 0, newWeights, 0, length);
        System.arraycopy(updatedTerminal.newWeightsIndexes, 0, newWeightsIndexes, 0, length);
    }
}

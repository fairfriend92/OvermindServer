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
    public byte[] newWeights = new byte[0];
    public int[] newWeightsIndexes = new int[0];

    public Terminal () {
        presynapticTerminals = new ArrayList<>();
        postsynapticTerminals = new ArrayList<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        Terminal compare = (Terminal) obj;
        if (compare.ip.equals(this.serverIP) || this.ip.equals(this.serverIP))
            return (compare.ip.equals(this.ip));
        else
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
        newWeights = new byte[updatedTerminal.newWeights.length];
        newWeightsIndexes = new int[updatedTerminal.newWeightsIndexes.length];
        System.arraycopy(updatedTerminal.newWeights, 0, newWeights, 0, updatedTerminal.newWeights.length);
        System.arraycopy(updatedTerminal.newWeightsIndexes, 0, newWeightsIndexes, 0, updatedTerminal.newWeightsIndexes.length);
    }
}

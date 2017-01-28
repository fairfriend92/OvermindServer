package com.example.overmind;

import java.io.Serializable;
import java.util.ArrayList;

public class LocalNetwork implements Serializable {
    public short numOfNeurons, numOfDendrites, numOfSynapses;
    public String ip;
    public int natPort;
    public ArrayList<LocalNetwork> presynapticNodes;
    public ArrayList<LocalNetwork> postsynapticNodes;

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        LocalNetwork compare = (LocalNetwork) obj;
        return compare.ip.equals(ip);
    }

    public synchronized LocalNetwork get() {
        return this;
    }

    public synchronized void update(LocalNetwork updatedLN) {
        this.numOfNeurons = updatedLN.numOfNeurons;
        this.numOfDendrites = updatedLN.numOfDendrites;
        this.numOfSynapses = updatedLN.numOfSynapses;
        this.ip = updatedLN.ip;
        this.natPort = updatedLN.natPort;
        this.presynapticNodes = updatedLN.presynapticNodes;
        this.postsynapticNodes = updatedLN.postsynapticNodes;
    }
}

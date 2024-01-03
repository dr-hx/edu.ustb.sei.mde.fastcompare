package edu.ustb.sei.mde.fastcompare.shash;

public interface SimHashValue {
    double similarity(SimHashValue value);
    static double similarity(SimHashValue left, SimHashValue right) {
        return left.similarity(right);
    }
    boolean equals(Object obj);
    int hashCode();
}

package edu.ustb.sei.mde.fastcompare.minhash;

public class MinHashFunction {
    final private int A;
    final private int B;
    final private int P;

    public MinHashFunction(int a, int b, int p) {
        A = a;
        B = b;
        P = p;
    }

    public int compute(int input) {
        final int local = A * input + B;
        return local % P;
    }
}

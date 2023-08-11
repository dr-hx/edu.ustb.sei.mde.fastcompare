package edu.ustb.sei.mde.fastcompare.utils;

public class Triple<L,R,O> {
    public Triple(L left, R right, O origin) {
        this.left = left;
        this.right = right;
        this.origin = origin;
    }

    static public <A,B,C> Triple<A,B,C> make(A l, B r, C o) {
        return new Triple<A,B,C>(l, r, o);
    }

    final public L left;
    final public R right;
    final public O origin;
}

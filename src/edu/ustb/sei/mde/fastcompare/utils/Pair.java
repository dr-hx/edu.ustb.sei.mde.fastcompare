package edu.ustb.sei.mde.fastcompare.utils;

import java.util.Map.Entry;

public class Pair<F,S> implements Entry<F,S> {
    public final F first;
    public final S second;

    public Pair(F f, S s) {
        this.first = f;
        this.second = s;
    }

    public int hashCode() {
        int hf = first == null ? 0 : first.hashCode();
        int hs = second == null ? 0 : second.hashCode();
        return hf ^ hs;
    }

    public boolean equals(Object right) {
        if(right instanceof Pair) {
            return (CommonUtils.ObjectEqual(this.first, ((Pair<?,?>)right).first)
                && CommonUtils.ObjectEqual(this.second, ((Pair<?,?>)right).second))
            || (CommonUtils.ObjectEqual(this.first, ((Pair<?,?>)right).second)
                && CommonUtils.ObjectEqual(this.second, ((Pair<?,?>)right).first));
        } else return false;
    }

    @Override
    public F getKey() {
        return first;
    }

    @Override
    public S getValue() {
        return second;
    }

    @Override
    public S setValue(S arg0) {
        throw new UnsupportedOperationException("Unimplemented method 'setValue'");
    }
}

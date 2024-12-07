package edu.ustb.sei.mde.fastcompare.minhash;

import java.util.Iterator;

public class BiGramIterator implements Iterator<Integer> {
    private NGramIterator iterator = new NGramIterator("", 2, (char)0);

    public BiGramIterator() {
    }

    public void setSource(String source) {
        iterator.setSource(source);
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Integer next() {
        byte[] bytes = iterator.next();
        return bytes[1] * 31 + bytes[0];
    }
}

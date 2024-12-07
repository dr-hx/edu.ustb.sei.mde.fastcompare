package edu.ustb.sei.mde.fastcompare.minhash;

import java.util.Iterator;

public class TokenStreamIterator implements Iterator<Integer> {
    private byte[] bytes;
    private byte[] buffer;
    private String source;

    private int current;
    private byte[] delimiters;
    private boolean useCamel;


    public TokenStreamIterator(int bufferSize, char[] delimiters, boolean useCamel) {
        this.buffer = new byte[bufferSize];
        this.delimiters = new byte[delimiters.length];
        for(int i=0;i<delimiters.length;i++)
            this.delimiters[i] = (byte) delimiters[i];
        this.useCamel = useCamel;
        
        this.bytes = null;
        this.source = null;
        this.current = 0;
    }

    public void setSource(String source) {
        this.source = source;
        this.bytes = source.getBytes();
        this.current = 0;
    }

    @Override
    public boolean hasNext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasNext'");
    }

    @Override
    public Integer next() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'next'");
    }
    
}

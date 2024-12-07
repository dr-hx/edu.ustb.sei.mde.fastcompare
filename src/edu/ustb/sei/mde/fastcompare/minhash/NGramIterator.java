package edu.ustb.sei.mde.fastcompare.minhash;

import java.util.Iterator;

public class NGramIterator implements Iterator<byte[]> {
    protected String hostString;
    private int N;
    private byte padding;
    private byte[] TEMP;
    private int cur = 0;
    private byte[] bytes;

    public NGramIterator(String str, int N, char padding) {
        this.N = N;
        this.padding = (byte) padding;
        this.TEMP = new byte[N];
        this.setSource(str);
    }

    public void setSource(String string) {
        this.hostString = string;
        this.bytes = string.getBytes();
        this.cur = 0;
    }

    private int getLength() {
        if(padding==0) return bytes.length;
        else return bytes.length + N - 1;
    }

    private byte getByte(int index) {
        if(padding == 0) return bytes[index];
        else {
            if(index < N - 1) return padding;
            else return bytes[index - N + 1];
        }
    }

    @Override
    public boolean hasNext() {
        return cur < getLength();
    }

    @Override
    public byte[] next() {
        for(int i=0; i<N; i++) {
            if(cur + i < getLength()) {
                TEMP[i] = getByte(cur + i);
            } else {
                TEMP[i] = padding;
            }
        }
        cur ++;
        return TEMP;
    }

    public void printNGram() {
        for(int i=0;i<N;i++){
            System.out.print((char)TEMP[i]);
        }
        System.out.println();
    }
}

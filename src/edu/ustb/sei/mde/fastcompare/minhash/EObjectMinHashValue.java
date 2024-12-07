package edu.ustb.sei.mde.fastcompare.minhash;

public class EObjectMinHashValue {
    final public int[] nameCode;
    final public int[] attributeCode;
    final public int[] referenceCode;


    public EObjectMinHashValue(int[] nameCode, int[] attributeCode, int[] referenceCode) {
        this.nameCode = nameCode;
        this.attributeCode = attributeCode;
        this.referenceCode = referenceCode;
    }

    static public double similarity(EObjectMinHashValue l, EObjectMinHashValue r) {
        double s1 = similarity(l.nameCode, r.nameCode);
        double s2 = similarity(l.attributeCode, r.attributeCode);
        double s3 = similarity(l.referenceCode, r.referenceCode);
        return (s1 * 0.5 + s2 * 0.3 + s3 * 0.2);
    }

    static private double similarity(int[] l, int[] r) {
        int eqc = 0;
        for(int i = 0; i<l.length;i++) {
            if(l[i] == r[i]) eqc ++;
        }
        return eqc / (double) l.length;
    }
}

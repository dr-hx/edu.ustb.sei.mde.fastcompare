package edu.ustb.sei.mde.fastcompare.index;

public class TreeHashValueEx extends TreeHashValue {
    public int subtreeStructureChecksum = 0;

    public long localContentHash = 0;
    public long childContentHash = 0;
    public byte lchBits = 0;
    public byte cchBits = 0;

    public boolean isSubtreeStructuralIdentical(TreeHashValueEx value) {
        return size == value.size && height == value.height && subtreeStructureChecksum == value.subtreeStructureChecksum;
    }

    public double computeSubtreeSimilarity(TreeHashValueEx value) {
        // if(subtreeStructureChecksum != value.subtreeStructureChecksum) return 0;
        double localSim = jaccard(localContentHash, lchBits, value.localContentHash, value.lchBits);
        double childSim = jaccard(childContentHash, cchBits, value.childContentHash, value.cchBits);
        if (localSim < 0 && childSim < 0)
            return 0.0;
        else if (localSim < 0)
            return childSim;
        else if (childSim < 0)
            return localSim;
        else
            return localSim * 0.8 + childSim * 0.2;
    }

    static public byte computeBitCounts(long x) {
        return (byte) Long.bitCount(x);
    }

    static public double jaccard(long a, byte aBits, long b, byte bBits) {
    	if(a==0 && b==0) return -1.0;
    	long intersect = a & b;
    	if(intersect == 0) return 0;
    	byte iBits = computeBitCounts(intersect);
    	return iBits / (double) (aBits - iBits + bBits);
    }
}

package edu.ustb.sei.mde.fastcompare.index;

public class TreeHashValue {

	public long subtreeChecksum = 0;
	
	
	public int height = 1;
	public int size = 1;
	
	public boolean isSubtreeIdentical(TreeHashValue value) {
		return height == value.height && subtreeChecksum == value.subtreeChecksum;
	}
}
package de.x28hd.tool;

public class BranchInfo {
    public Integer branchKey;
    public String branchLabel;
    
    public static final int ROOTZETTEL = -1;
    public static final int NOZETTEL = -2;

    public BranchInfo(int branchKey, String branchLabel) {
    	this.branchKey = branchKey;
    	this.branchLabel = branchLabel;
        
        if (branchLabel == null) {
            System.err.println("Error BIn140 Couldn't find info for " + branchKey);
        }
    }
    public int getKey() {
        return branchKey;
    }
    
    public String toString() {
        return branchLabel;
    }
}
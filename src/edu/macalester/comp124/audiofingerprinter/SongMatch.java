package edu.macalester.comp124.audiofingerprinter;

/**
 * Created by sojinoh on 12/10/15.
 */
public class SongMatch implements Comparable <SongMatch>{ //SongMatch object for use in reocgnize method
    private int matchCount;
    private String songID;
    public SongMatch (int matchCount, String songID){
        this.matchCount=matchCount;
        this.songID=songID;
    }
    /**
     * Compares SongMatch objects by int MatchCount
     * @param o
     * @return int
     */
    public int compareTo(SongMatch o){
        return ((Integer) matchCount).compareTo(o.matchCount);
    }
    public int getMatchCount() {
        return matchCount;
    }
    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }
    public String getSongName() {
        return songID;
    }
    public void setSongName(String songID) {
        this.songID = songID;
    }
    /**
     * Returns standard toString with private variables
     * @return String
     */
    public String toString(){
        return "Song: " + songID + " Match Count: " + matchCount;
    }
}

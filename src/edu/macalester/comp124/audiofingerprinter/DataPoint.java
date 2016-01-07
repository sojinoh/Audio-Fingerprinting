package edu.macalester.comp124.audiofingerprinter;

/**
 * Wrapper object used to keep track of a song and time offset that correspond with a specific hash in a map.
 * Created by bjackson on 11/15/2015.
 */
public class DataPoint {

    private int time;
    private int songId;

    /**
     * Constructor to create a datapoint
     * @param songId
     * @param time
     */
    public DataPoint(int songId, int time) {
        this.songId = songId;
        this.time = time;
    }

    /**
     * Getter for time
     * @return
     */
    public int getTime() {
        return time;
    }

    /**
     * Getter for song id.
     * @return
     */
    public int getSongId() {
            return songId;
        }

}

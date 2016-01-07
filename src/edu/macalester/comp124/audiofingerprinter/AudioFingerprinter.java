package edu.macalester.comp124.audiofingerprinter;

import java.io.File;
import java.util.List;

/**
 * Created by bjackson on 11/17/2015.
 */
public interface AudioFingerprinter {

    /**
     * Returns the database of songs that this fingerprinter uses to recognize.
     * @return
     */
    SongDatabase getSongDB();

    /**
     * Given an array of bytes representing a song, this method will return a list of song names with matching fingerprints.
     * The algorithm is as follows:
     *      - Convert the audio to the frequency domain
     *      - For each slice of time, determine key points, and create a hash fingerprint
     *      - Find matching datapoints from the song database.
     *      - Calculate the number of matching points for each candidate song
     *      - return a list of song names in order of most matches to least matches.
     * @param audioData array of bytes representing a song
     * @return A list of song names with matching fingerprints, sorted in order from most likely match to least likely match.
     */
    List<String> recognize(byte[] audioData);

    /**
     * Overloaded method given a file object to recognize.
     * Hint: get the raw audio data from the file and call the other overloaded recognize method.
     * @param fileIn
     * @return
     */
    List<String> recognize(File fileIn);

    /**
     * Given a 2D array of frequency information over time, returns the keypoints.
     * @param results, an array of frequency data. The first index corresponds with a slice of time, the second with the frequency.
     *                 The data is represented as complex numbers with interleaved real and imaginary parts. For example, to get the
     *                 magnitude of a specific frequency:
     *                      double re = results[time][2*freq];
     *                      double im = results[time][2*freq+1];
    *                       double mag = Math.log(Math.sqrt(re * re + im * im) + 1);
     * @return a 2D array where the first index represents the time slice, and the second index contains the highest frequencies
     *          for the following ranges with that time slice: 30 Hz - 40 Hz, 40 Hz - 80 Hz and 80 Hz - 120 Hz for the low tones (covering bass guitar,
     *          for example), and 120 Hz - 180 Hz and 180 Hz - 300 Hz for the middle and higher tones (covering vocals and most other instruments).
     */
    long[][] determineKeyPoints(double[][] results);

    /**
     * Returns a hash combining information of several keypoints.
     * @param points array of key points for a particular slice of time. Must be at least length 4.
     * @return
     */
    long hash(long[] points);
}

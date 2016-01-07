package edu.macalester.comp124.audiofingerprinter;
import java.io.File;
import java.util.*;

/**
 * Created by sojinoh on 12/5/15.
 */

public class SongFingerprint implements AudioFingerprinter {

    private static int[] RANGE = new int[]{40, 80, 120, 180, 300};
    private static long FUZ_FACTOR = 2;
    private SongDatabase songs;

    /**
     * Constructor.
     *
     * @param songs is a database of songs.
     */
    public SongFingerprint(SongDatabase songs) {
        this.songs = songs;
    }

    /**
     * Returns a hash combining information of several keypoints.
     *
     * @param points array of key points for a particular slice of time. Must be at least length 4.
     * @return
     */
    @Override
    public long hash(long[] points) {
        if (points.length > 3)
            return (points[3] - (points[3] % FUZ_FACTOR)) * 100000000 + (points[2] - (points[2] % FUZ_FACTOR)) //JUAN BILLION
                    * 100000 + (points[1] - (points[1] % FUZ_FACTOR)) * 100
                    + (points[0] - (points[0] % FUZ_FACTOR));
        else
            return -1;
    }

    /**
     * getIndex tool for our DetermineKeyPoints method
     *
     * @param freq is an int for frequency of sound bit
     * @return i the index based on range
     */
    public int getIndex(int freq) {
        int i = 0;
        while (RANGE[i] < freq)
            i++;
        return i;
    }

    /**
     * Given an array of bytes representing a song, this method will return a list of song names with matching fingerprints.
     * The algorithm is as follows:
     * - Convert the audio to the frequency domain
     * - For each slice of time, determine key points, and create a hash fingerprint
     * - Find matching datapoints from the song database.
     * - Calculate the number of matching points for each candidate song
     * - return a list of song names in order of most matches to least matches.
     *
     * @param audioData array of bytes representing a song
     * @return A list of song names with matching fingerprints, sorted in order from most likely match to least likely match.
     */
    @Override
    public List<String> recognize(byte[] audioData) {
        long[][] keyPoints = determineKeyPoints(songs.convertToFrequencyDomain(audioData));//Converted AND determined key points on the same line #efficiencyismymiddlename
        HashMap<Integer, Map<Integer, Integer>> matches = new HashMap<>(); //Yo dawg, I heard you like hashmaps so I put a hashmap in your hashmap
        for (int t = 0; keyPoints.length > t; t++) {
            List<DataPoint> matchingPoints = songs.getMatchingPoints(hash(keyPoints[t])); //Matching Points at time t
            if (matchingPoints != null) {
                for (DataPoint d : matchingPoints) {//Use offsets to create MORE hashmaps, which we then put back into our original hashmap of matches for each datapoint
                    HashMap<Integer, Integer> offsets = new HashMap<>();
                    int offset = d.getTime() - t; //offset calculation
                    if (!matches.containsKey(d.getSongId())) {
                        offsets.put(offset, 1);
                        matches.put(d.getSongId(), offsets);
                    } else if (!matches.get(d.getSongId()).containsKey(offset))
                        matches.get(d.getSongId()).put(offset, 1);
                    else if (matches.containsKey(d.getSongId()))
                        matches.get(d.getSongId()).put(offset, matches.get(d.getSongId()).get(offset) + 1);
                }
            }
        }

        List<SongMatch> matchList = new ArrayList<SongMatch>();//Sort, organize, spit out useful data
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : matches.entrySet()) {
            Integer max = 0;
            for (Map.Entry<Integer, Integer> entries : entry.getValue().entrySet()) {
                if (max < entries.getValue())
                    max = entries.getValue();
            }
            matchList.add(new SongMatch(max, entry.getKey().toString()));
        }
///Sort sort sort
        Collections.sort(matchList);
        Collections.reverse(matchList);

        List<String> results = new ArrayList<String>(); //Turning our SongMatch objects into Strings with # of matches and Song Name: toString methods.
        for (SongMatch s : matchList) {
            results.add(songs.getSongName(Integer.parseInt(s.getSongName())) + " " + s.toString()); //Forcibly converting a String to an int
        }
        return results;
    }

    /**
     * Overloaded method given a file object to recognize.
     * Hint: get the raw audio data from the file and call the other overloaded recognize method.
     *
     * @param fileIn
     * @return
     */
    @Override
    public List<String> recognize(File fileIn) {
        return recognize(songs.getRawData(fileIn));
    } //File in, file out

    /**
     * Getter method
     *
     * @return SongDatbase of songs.
     */
    @Override
    public SongDatabase getSongDB() {
        return songs;
    }

    /**
     * Given a 2D array of frequency information over time, returns the keypoints.
     *
     * @param results, an array of frequency data. The first index corresponds with a slice of time, the second with the frequency.
     *                 The data is represented as complex numbers with interleaved real and imaginary parts. For example, to get the
     *                 magnitude of a specific frequency:
     *                 double re = results[time][2*freq];
     *                 double im = results[time][2*freq+1];
     *                 double mag = Math.log(Math.sqrt(re * re + im * im) + 1);
     * @return a 2D array where the first index represents the time slice, and the second index contains the highest frequencies
     * for the following ranges with that time slice: 30 Hz - 40 Hz, 40 Hz - 80 Hz and 80 Hz - 120 Hz for the low tones (covering bass guitar,
     * for example), and 120 Hz - 180 Hz and 180 Hz - 300 Hz for the middle and higher tones (covering vocals and most other instruments).
     */
    @Override
    public long[][] determineKeyPoints(double[][] results) {
        long[][] keyPoints = new long[results.length][5]; //2d arrays
        double[][] highScores = new double[results.length][5];
        for (int r = 0; r < results.length; r++) { //nested row column traversal, r(ow) and c(olumn). Column stands for frequency, r stands for something I accidentally forgot oops
            for (int c = 40; c < 300; c++) {
                double re = results[r][2 * c];
                double im = results[r][2 * c + 1];
                double mag = Math.log(Math.sqrt(re * re + im * im) + 1);
                int index = getIndex(c);
                if (mag > highScores[r][index]) {
                    highScores[r][index] = c;
                    keyPoints[r][index] = (long) mag;
                }
            }
        }
        return keyPoints;
    }
}
package edu.macalester.comp124.audiofingerprinter;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import org.jtransforms.fft.DoubleFFT_1D;
import org.tritonus.sampled.convert.PCM2PCMConversionProvider;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a database of songs and their associated fingerprints through time. This is used to find matches for new songs to identify them.
 * Created by bjackson on 11/15/2015.
 */
public class SongDatabase {

    private ConcurrentHashMap<Integer, String> songNames; // Maps songId to the name.
    private ConcurrentHashMap<Long, List<DataPoint>> matcherDB; // Maps a fingerprint hash to a list of datapoints (song/time offsets) where the hash was calculated.
    private int nextSongId; // Used to assign ids to songs as they are added to the database. Starts at zero and increments by one for each song.
    private AudioFingerprinter fingerprinter;

    /**
     * Constructor to initialize instance variables.
     */
    public SongDatabase(){
        songNames = new ConcurrentHashMap<>();
        matcherDB = new ConcurrentHashMap<>();
        nextSongId = 0;
        fingerprinter = null;
    }

    /**
     * Overloaded constructor to syncronously load the database at the same time the object is constructed.
     * @param directoryPath
     */
    public SongDatabase(String directoryPath){
        this();
        loadDatabase(directoryPath);
    }

    /**
     * Sets the audio fingerprinter instance.
     * @param fingerprinter
     */
    public void setFingerprinter(AudioFingerprinter fingerprinter){
        this.fingerprinter = fingerprinter;
    }

    /**
     * Given a directory, this method will find all the mp3 files inside it and create fingerprints for each one to add to the matcherDB map.
     * @param directory
     */
    public void loadDatabase(File directory){
        songNames.clear();
        matcherDB.clear();
        System.out.println("Looking for files in "+directory.getAbsolutePath());

        // Get an array of mp3 files in the directory.
        File[] audioFiles = getAudioFilesFromDirectory(directory);
        System.out.println("Found "+audioFiles.length+" files.");
        for(int i=0; i < audioFiles.length; i++){
            processFile(audioFiles[i]);
        }
    }

    /**
     * Overloaded method to take a String path rather than a File object indicating the directory to load files from.
     * @param directoryPath
     */
    public void loadDatabase(String directoryPath){
        File dir = new File(directoryPath);
        loadDatabase(dir);
    }

    /**
     * Loads the database asynchronously. I.e. this method will return, while the songs continue to get processed and added
     * to the database in a separate thread.
     * @param directory to look in for mp3 files to load
     * @param progressBar ProgressBar ui object to indicate progress
     * @param label Status label to indicate the current status in the ui
     * @param listView UI listview where we want to display the results when finished.
     */
    public void loadDatabaseAsync(File directory, ProgressBar progressBar, Label label, ListView<String> listView){
        songNames.clear();
        matcherDB.clear();
        System.out.println("Looking for files in "+directory.getAbsolutePath());

        // Create a new task object that will run in a separate thread to process each mp3 file
        // This is defining an anonymous class: https://docs.oracle.com/javase/tutorial/java/javaOO/anonymousclasses.html
        Task<ObservableList<String>> task = new Task<ObservableList<String>>() {
            @Override public ObservableList<String> call() {
                ObservableList<String> results = FXCollections.observableArrayList();
                // Get an array of mp3 files in the directory
                File[] audioFiles = getAudioFilesFromDirectory(directory);
                System.out.println("Found "+audioFiles.length+" files.");
                for(int i=0; i < audioFiles.length; i++){
                    // Check whether the task was canceled and the thread should end.
                    if (isCancelled()) {
                        break;
                    }
                    // Update the status label in the ui for which song we are processing.
                    updateMessage("Analyzing "+audioFiles[i].getName());
                    results.add(audioFiles[i].getName());
                    processFile(audioFiles[i]);
                    // Update the ui progress bar with our current progress.
                    updateProgress(i, audioFiles.length-1);
                }
                return results;
            }
        };

        // Enable the progress bar and set it to update the value based on the task properties.
        progressBar.setDisable(false);
        progressBar.progressProperty().bind(task.progressProperty());
        label.textProperty().bind(task.messageProperty());

        // Specify that the listview should update with the list of processsed songs when the task finishes processing.
        task.setOnSucceeded(e -> {
            listView.setItems(task.getValue());
            label.textProperty().unbind();
            label.setText("Ready");
            progressBar.progressProperty().unbind();
            progressBar.setDisable(true);
        });

        // Create a new thread to run the task and start it.
        Thread thread = new Thread(task);
        thread.start();
    }

    /**
     * Process a file. This gets the raw data, converts it to the frequency domain using the fft, determines the keypoints,
     * hashes the keypoints, and then adds the corresponding datapoints to the matcherDB.
     * @param file to process
     */
    public void processFile(File file){
        byte[] audioRawData = getRawData(file);
        if (audioRawData != null) {
            int songId = nextSongId;
            nextSongId++;
            songNames.put(songId, file.getName());
            //TODO: Add the song file to songNames map using the songID as the key. You can get the nume of the file using file.getName().

            //TODO: Create a fingerprint of the file by:
            double [][] frequency = convertToFrequencyDomain(audioRawData);
            // 1. converting the raw data to the frequency domain
            long [][] keyPoints = fingerprinter.determineKeyPoints(frequency);
            // 2. determining keypoints in the frequency data
            for(int time = 0; time < keyPoints.length; time++) {// 3. For each chunk of time:
                long h = fingerprinter.hash(keyPoints[time]);
                //          4.calculate the hash of the corresponding key points
                DataPoint dp = new DataPoint(songId, time);
                //          5. Create a datapoint object representing the time and song.
                if (matcherDB.containsKey(h)) {
                    List<DataPoint> matchingP = matcherDB.get(h);
                    matchingP.add(dp);
                    matcherDB.put(h, matchingP);
                } else {
                    List<DataPoint> matchingP = new ArrayList<>();
                    matchingP.add(dp);
                    matcherDB.put(h, matchingP);
                }
                //          6. Add the datapoint to list of datapoints that correspond with a specific hash in the matchedDB map (creating the list if it doesn't exist)
            }
        }
        System.out.println("Finished analyzing " + file.getName());
    }

    /**
     * Returns an array of bytes holding the raw audio data contained in the mp3 file specified by fileIn
     * @param fileIn an mp3 file
     * @return an array of the raw audio data in bytes.
     */
    public byte[] getRawData(File fileIn){
        if (!fileIn.isFile()){
            System.out.println("File does not exist or is a directory: "+fileIn.getName());
            return null;
        }

        int totalFramesRead = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            // First we need to convert the mp3 into a format we can read.
            AudioInputStream inFileAIS = AudioSystem.getAudioInputStream(fileIn);

            AudioFormat baseFormat = inFileAIS.getFormat();
            AudioFormat outDataFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16,
                    baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            AudioInputStream inStream;
            if (AudioSystem.isConversionSupported(outDataFormat, baseFormat)) {
                inStream = AudioSystem.getAudioInputStream(outDataFormat, inFileAIS);
            }
            else {
                System.out.println("Unable to convert file, continuing: "+fileIn.getName());
                return null;
            }

            PCM2PCMConversionProvider conversionProvider = new PCM2PCMConversionProvider();

            if (!conversionProvider.isConversionSupported(getFormat(), outDataFormat)) {
                System.out.println("Unable to convert file");
                return null;
            }

            AudioInputStream audioInputStream = conversionProvider.getAudioInputStream(getFormat(), inStream);

            int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
            if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                // some audio formats may have unspecified frame size
                // in that case we may read any amount of bytes
                bytesPerFrame = 1;
            }
            // Set an arbitrary buffer size of 1024 frames.
            int numBytes = 1024 * bytesPerFrame;
            byte[] audioBytes = new byte[numBytes];
            try {
                int numBytesRead = 0;
                int numFramesRead = 0;
                // Try to read numBytes bytes from the file.
                while ((numBytesRead = audioInputStream.read(audioBytes)) != -1) {
                    // Calculate the number of frames actually read.
                    numFramesRead = numBytesRead / bytesPerFrame;
                    totalFramesRead += numFramesRead;
                    // Here, do something useful with the audio data that's
                    // now in the audioBytes array...
                    outputStream.write(audioBytes, 0, numBytesRead);
                }
            } catch (Exception ex) {
                // Handle the error...
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }

        } catch (Exception e) {
            // Handle the error...
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    /**
     * Given raw audio data, this uses the fft to convert to the frequency domain by slicing the data up into time chunks
     * @param audioData raw audio
     * @return a 2D array of frequency data. The first subscript refers to a slice of time, the second contains the frequency data.
     * The frequency data are represented using complex numbers with the real and imaginary parts interleaved.
     */
    public double[][] convertToFrequencyDomain(byte[] audioData){
        int totalSize = audioData.length;
        int chunkSize = 4096; // Each chunk is 4kb.
        int sampledChunkSize = totalSize/chunkSize;

        DoubleFFT_1D fft1D = new DoubleFFT_1D(chunkSize);
        double[][] results = new double[sampledChunkSize][];

        for(int j = 0; j < sampledChunkSize; j++) {
            double[] fft = new double[chunkSize * 2];
            for (int i = 0; i < chunkSize; i++) {
                fft[2*i] = audioData[(j*chunkSize)+i];
                fft[2*i+1] = 0.0;
            }
            fft1D.complexForward(fft);
            results[j] = fft;
        }
        return results;
    }

    /**
     * Returns an array of file objects containing the mp3 files that are found in the directory
     * @param directory to search
     * @return array of mp3 files from the directory.
     */
    public File[] getAudioFilesFromDirectory(File directory){
        if (directory.isDirectory()) {

            return directory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(".mp3");
                }
            });
        }
        else {
            // if directory is not actually a directory or doesn't exist, return a zero length array.
            return new File[0];
        }

    }

    /**
     * Returns the name of the song given an id
     * @param songId
     * @return the name of the song
     */
    public String getSongName(int songId) {
        return songNames.get(songId);
    }

    /**
     * Returns a list of datapoints that match a specific hash.
     * @param hash
     * @return
     */
    public List<DataPoint> getMatchingPoints (long hash){
        return matcherDB.get(hash);
    }

    /**
     * Configure the audioformat we want to read from the mp3 file.
     * @return
     */
    private AudioFormat getFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 8;
        int channels = 1;          //mono
        boolean signed = true;     //Indicates whether the data is signed or unsigned
        boolean bigEndian = true;  //Indicates whether the audio data is stored in big-endian or little-endian order
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }



}

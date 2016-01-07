package edu.macalester.comp124.audiofingerprinter;

import javafx.concurrent.Task;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a sound recording task. It is designed to run in its own thread and will record data from
 * the computer's microphone.
 * Created by bjackson on 11/16/2015.
 */
public class SoundRecorder extends Task<ByteArrayOutputStream> {

    // this is a special type of boolean that prevents multiple threads from accessing/changing the value at concurrently.
    // This prevents race conditions that might cause program errors.
    private AtomicBoolean running;

    /**
     * Constructor initially sets running to false.
     */
    public SoundRecorder(){
        running = new AtomicBoolean(false);
    }

    /**
     * Setter method to set whether the SoundRecorder task is currently recording.
     * @param isRunning
     */
    public void setRunning(boolean isRunning){
        running.set(isRunning);
    }

    /**
     * This method is similar to the run() method in the runnable interface, but is specialized to work with
     * a javafx gui.
     * @return a bytearrayoutput stream containing the recorded audio data.
     */
    @Override
    public ByteArrayOutputStream call(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        running.set(true);

        // Start by opening a connection to the microphone.
        final AudioFormat format = getFormat(); //Fill AudioFormat with the wanted settings
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        final TargetDataLine line;
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
        }
        catch(LineUnavailableException ex){
            running.set(false);
            System.err.println("LineUnavailableException: Unable to open microphone for recording. "+ex.getMessage());
            return out;
        }
        line.start();

        try {
            updateMessage("Recording...");
            // Creates a buffer of bytes to read the data from the microphone.
            byte[] buffer = new byte[1024];
            while (running.get()) {
                // Check whether the user has canceled the running thread.
                if (isCancelled()) {
                    running.set(false);
                    break;
                }

                // Read some bytes from the microphone into the buffer.
                int count = line.read(buffer, 0, buffer.length);
                // If we read any data, save it to the outputstream.
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
            out.close();
            line.close();
        } catch (IOException e) {
            System.err.println("I/O problems: " + e);
            e.printStackTrace();
            System.exit(-1);
        }
        updateMessage("Ready");
        return out;
    }

    /**
     * Configures an audioformat object specifying in what format we want to receive data from the microphone.
     * @return formated AudioFormat.
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

package edu.macalester.comp124.audiofingerprinter;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/**
 * Creates the user interface and runs the audio fingerprinting application.
 * Created by bjackson on 11/15/2015.
 */
public class MainApp extends Application {

    private AudioFingerprinter recognizer;
    private SoundRecorder recorder;

    private ListView<String> matchResultList;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Button matchFileButton;
    private Button listenButton;

    /**
     * Creates the user interface.
     * @param primaryStage
     * @return
     */
    public Parent createContent(Stage primaryStage) {
        // Border pane allows us to layout content along the top, bottom, left, right, and center of the window.
        BorderPane borderPane = new BorderPane();

        matchResultList = new ListView<>();
        statusLabel = new Label("Ready");

        //Top content
        ToolBar toolbar = createToolbar(primaryStage);
        borderPane.setTop(toolbar);

        //Bottom content
        HBox bottomHBox = createBottomContent(primaryStage);
        borderPane.setBottom(bottomHBox);

        //Left content
        VBox leftVbox = createLeftContent(primaryStage);
        borderPane.setLeft(leftVbox);

        //Center content
        VBox centerVBox = createCenterContent(primaryStage);
        borderPane.setCenter(centerVBox);

        return borderPane;
    }

    private ToolBar createToolbar(Stage primaryStage)
    {
        listenButton = new Button("Record");
        listenButton.setDisable(true);
        Button stopButton = new Button("Stop Recording");
        stopButton.setDisable(true);
        listenButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        listenButton.setDisable(true);
                        // If the user presses the record button, create a new Sound recorder
                        // Create a new thread (we want the user interface to still be responsive while you are recording)
                        recorder = new SoundRecorder();
                        Thread thread = new Thread(recorder);
                        thread.start();
                        stopButton.setDisable(false);
                        statusLabel.setText("Recording...");
                    }
                });
        stopButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        stopButton.setDisable(true);
                        // If the user presses stop, wait for the recorder to stop running, then recognize the
                        // audio data it returned.
                        recorder.setOnSucceeded(ev -> {
                            listenButton.setDisable(false);
                            ByteArrayOutputStream stream = recorder.getValue();
                            List<String> results = recognizer.recognize(stream.toByteArray());
                            matchResultList.setItems(FXCollections.observableArrayList(results));
                            statusLabel.setText("Ready");
                        });
                        recorder.setRunning(false);
                    }
                }
        );


        Label matchLabel = new Label("or");
        matchLabel.setPrefWidth(150);
        matchLabel.setMinWidth(150);
        matchLabel.setMaxWidth(150);
        matchLabel.setAlignment(Pos.CENTER);
        matchFileButton = new Button("Open audio file to match...");
        FileChooser fileChooser = new FileChooser();
        matchFileButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        configureFileChooser(fileChooser);
                        File file = fileChooser.showOpenDialog(primaryStage);
                        if (file != null) {
                            List<String> results = recognizer.recognize(file);
                            matchResultList.setItems(FXCollections.observableArrayList(results));
                        }
                    }
                });

        matchFileButton.setDisable(true);
        ToolBar toolbar = new ToolBar();
        toolbar.setPadding(new Insets(15, 12, 15, 12));
        toolbar.getItems().addAll(listenButton, stopButton, matchLabel, matchFileButton);
        return toolbar;
    }

    /**
     * Creates the left panel content to hold the song database information.
     * @param primaryStage
     * @return
     */
    private VBox createLeftContent(Stage primaryStage){
        ListView<String> songListView = new ListView<String>();
        songListView.setPrefSize(400, 600);
        Label dbLabel = new Label("Song Database:");
        HBox fileSelectionHBox = new HBox();
        fileSelectionHBox.setSpacing(10);
        TextField fileSelectionText = new TextField();
        fileSelectionText.setEditable(false);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        Button fileSelectionButton = new Button("...");
        fileSelectionButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        configureDirectoryChooser(directoryChooser);
                        // Shows the directory chooser dialog and returns the directory that the user selected.
                        File file = directoryChooser.showDialog(primaryStage);
                        if (file != null) {
                            fileSelectionText.setText(file.toString());
                            recognizer.getSongDB().loadDatabaseAsync(file, progressBar, statusLabel, songListView);
                            // Now that we have a database, reenable the record and file selection buttons.
                            listenButton.setDisable(false);
                            matchFileButton.setDisable(false);
                        }
                    }
                });


        fileSelectionHBox.getChildren().addAll(fileSelectionText, fileSelectionButton);
        VBox leftVbox = new VBox();
        leftVbox.setPadding(new Insets(15, 12, 15, 12));
        leftVbox.setSpacing(10);
        leftVbox.getChildren().addAll(dbLabel, fileSelectionHBox, songListView);
        return leftVbox;
    }

    /**
     * Creates the progressbar and adds the status label to an hbox.
     * @param primaryStage
     * @return
     */
    private HBox createBottomContent(Stage primaryStage){
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setProgress(1.0);
        progressBar.setDisable(true);
        HBox bottomHBox = new HBox();
        bottomHBox.setSpacing(10);
        bottomHBox.setPadding(new Insets(15, 12, 5, 12));
        bottomHBox.setAlignment(Pos.CENTER_RIGHT);
        bottomHBox.getChildren().addAll(statusLabel, progressBar);
        return bottomHBox;
    }

    /**
     * Creates the center content to display match results in a listview.
     * @param primaryStage
     * @return
     */
    private VBox createCenterContent(Stage primaryStage){
        Label centerLabel = new Label("Song Matches:");
        matchResultList.setPrefHeight(660);
        VBox centerVBox = new VBox();
        centerVBox.setPadding(new Insets(15, 12, 15, 12));
        centerVBox.setSpacing(10);
        centerVBox.getChildren().addAll(centerLabel, matchResultList);
        return  centerVBox;
    }

    /**
     * Configures the directory chooser to start in the user's home directory.
     * @param directoryChooser
     */
    private void configureDirectoryChooser(DirectoryChooser directoryChooser) {
        directoryChooser.setTitle("Choose Directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
    }

    /**
     * Configures the file chooser to allow mp3 selection and to start in the user's home directory.
     * @param fileChooser
     */
    private void configureFileChooser(FileChooser fileChooser) {
        fileChooser.setTitle("Choose an mp3");
        fileChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
        );
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3", "*.mp3")
        );
    }

    /**
     * Javafx method to initialize the application. See http://docs.oracle.com/javase/8/javase-clienttechnologies.htm
     * for more details on how to create graphical user interfaces using java.
     * @param primaryStage
     */
    @Override
    public void start(Stage primaryStage) {
        SongDatabase songDB = new SongDatabase();
        //TODO: Initialize the AudioFingerPrinter with your implementing class.
        recognizer = new SongFingerprint(songDB);
        songDB.setFingerprinter(recognizer);

        Scene scene = new Scene(createContent(primaryStage), 1200, 800);

        primaryStage.setTitle("Audio Fingerprinting");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Main method that starts the application
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}

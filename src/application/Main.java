package application;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;


public class Main extends Application {
    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;
    private Thread recordingThread;
    private AudioFileFormat.Type fileType;
    private File audioFile;
    private boolean isRecording = false;
    private byte[] audioBuffer;
    private VideoCapture camera;
    private VideoWriter writer;
    private TextArea responseArea;
    private ProgressBar progressBar;
    private Task<Void> apiTask;
    private MediaPlayer mediaPlayer;  // To play the recorded audio
    private Map<String, String> languageMap;
    String finalVideoFilePath;
    private String targetlanguage="hi";
    private String sourcelagnuage ="en";
    String targetTrans;
    String base64audio="";
    File wavFile;
    HBox loaderContainer = new HBox(10);  // 10px spacing between elements
    VBox buttonContainer = new VBox(10);
    private ScheduledExecutorService scheduler;
    private int elapsedSeconds = 0;
    private Label timerLabel;
    private ProgressIndicator loader;
    final boolean[] isVoiceRecording = {false};
    final boolean[] isVideoRecording = {false};
    @Override
    public void start(Stage primaryStage) {
      
    	VBox root = new VBox(20); // 40px spacing between components
    	root.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #A8DADC;");

    	// Header Section
    	VBox header = new VBox();
    	header.setStyle("-fx-padding: 10; -fx-alignment: center; -fx-border-color: #D1D1D1; -fx-border-width: 1; -fx-background-color: #FFB6B6; -fx-spacing: 5;");
    	// Main Heading
    	Label mainHeading = new Label("Speech/Video Translation Assistant");
    	mainHeading.setFont(Font.font("Arial", 24));
    	mainHeading.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

    	// Subheading
    	Label subHeading = new Label("Developed by NIC Haryana");
    	subHeading.setFont(Font.font("Arial", 16));
    	subHeading.setStyle("-fx-text-fill: #555;");

    	// Add company logo (ImageView)
    	Image logo = new Image(getClass().getResourceAsStream("nic.jpg"));  // Path to the logo file
    	ImageView logoImageView = new ImageView(logo);
    	logoImageView.setFitHeight(50);  // Set the height of the logo
    	logoImageView.setPreserveRatio(true);

    	// Create HBox for the layout with two columns
    	HBox headerContent = new HBox(20);  // 20px spacing between the two columns
    	headerContent.setStyle("-fx-alignment: center;");  // Center the content horizontally

    	// Create the first column (Logo)
    	VBox logoColumn = new VBox();
    	logoColumn.setAlignment(Pos.CENTER);
    	logoColumn.getChildren().add(logoImageView);  // Add logo to the first column

    	// Create the second column (Heading + Subheading)
    	VBox textColumn = new VBox();
    	textColumn.setAlignment(Pos.CENTER);
    	textColumn.getChildren().addAll(mainHeading, subHeading);  // Add heading and subheading to the second column

    	// Add both columns to the HBox
    	headerContent.getChildren().addAll(logoColumn, textColumn);

    	// Add the header content to the main VBox
    	header.getChildren().add(headerContent);

    	// Add header to the root
    	root.getChildren().add(header);
    	// Circular Progress Indicator (Loader)
    	loader = new ProgressIndicator();
    	loader.setProgress(-1);    // Indeterminate progress (spinning loader)
    	loader.setMinSize(50, 50); // Set a fixed size for the loader
    	loader.setStyle("-fx-progress-color: #4caf50;"); // Set the color of the loader to green

    	Label loadingText = new Label("Loading, please wait...");  // Text to display with the loader
    	loadingText.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");  // Optional styling for the text
        loaderContainer.setAlignment(Pos.CENTER);  // Center align the content
        loaderContainer.getChildren().addAll(loader, loadingText);
        loaderContainer.setVisible(false);

     // Source and Target Language Selection
        HBox languageSelection = new HBox(10);
        languageSelection.setAlignment(Pos.CENTER);
        Label sourceLabel = new Label("Source Language:");
        ComboBox<String> sourceLangComboBox = new ComboBox<>();
        Label targetLabel = new Label("Target Language:");
        ComboBox<String> targetLangComboBox = new ComboBox<>();
        populateLanguageComboBoxes(sourceLangComboBox, targetLangComboBox);

        sourceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");
        targetLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");
        languageSelection.getChildren().addAll(sourceLabel, sourceLangComboBox, targetLabel, targetLangComboBox);


        Image voiceIcon = new Image(getClass().getResourceAsStream("voice.png"));
        Image videoIcon = new Image(getClass().getResourceAsStream("video.png"));
        Image stopIcon = new Image(getClass().getResourceAsStream("stop.png"));
        Image playicon = new Image(getClass().getResourceAsStream("play.png"));

        // Create ImageViews for buttons
        ImageView voiceImageView = new ImageView(voiceIcon);
        voiceImageView.setFitHeight(30);
        voiceImageView.setFitWidth(30);

        ImageView videoImageView = new ImageView(videoIcon);
        videoImageView.setFitHeight(30);
        videoImageView.setFitWidth(30);

        ImageView stopImageView = new ImageView(stopIcon);
        stopImageView.setFitHeight(30);
        stopImageView.setFitWidth(30);
        
        ImageView playImageView = new ImageView(playicon);
        playImageView.setFitHeight(30);
        playImageView.setFitWidth(30);

      
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setStyle("-fx-background-color: #F1F1F1; -fx-border-color: #D1D1D1; -fx-border-width: 2; -fx-padding: 10;");


        
        // Buttons for Recording
        Button startVoiceButton = new Button("Start Recording", voiceImageView);
        Button startVideoButton = new Button("Start Recording", videoImageView);
        Button playAudioButton = new Button("Play Recording",playImageView);
        Button playVideoButton = new Button("Play Recording",playImageView);
        Button fileButton = new Button("File Choose",playImageView);
        
        
        timerLabel = new Label("Elapsed Time: 00:00:00");
        timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #333;");
        timerLabel.setVisible(false);
        buttonContainer.getChildren().addAll(startVoiceButton,startVideoButton,playAudioButton,playVideoButton,fileButton, timerLabel);
        
        
        playAudioButton.setVisible(false);
        playVideoButton.setVisible(false);
        
        // Button Actions
        fileButton.setOnAction(event->{
        	FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a File");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF and DOCX Files", "*.pdf", "*.docx")
            );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                String filePath = selectedFile.getAbsolutePath();
                System.out.println("Selected File: " + filePath);

            String fileContent = "";
            if (filePath.endsWith(".pdf")) {
            	 try (PDDocument document = Loader.loadPDF(selectedFile)) {
                     PDFTextStripper pdfStripper = new PDFTextStripper();
                     
                     byte[] utf8Bytes = pdfStripper.getText(document).getBytes(StandardCharsets.UTF_16);
                     fileContent = new String(utf8Bytes, StandardCharsets.UTF_16);

                     responseArea.setText(fileContent);
                     
                     try {
						Thread.sleep(1000);
	                     sendTextToApi(sourcelagnuage,responseArea.getText().toString());

					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                 } catch (IOException e) {
                     e.printStackTrace();
                     
                 }
            } 
//            else if (filePath.endsWith(".docx")) {
//                fileContent = readDOCX(selectedFile);
//            }
            else {
                System.out.println("Unsupported file format.");
                return;
            }
        }
        	
        });
        startVoiceButton.setOnAction(event -> {
            if (isVoiceRecording[0]) {
                // Stop Voice Recording
                stopRecording();
                stopTimer();
                loaderContainer.setVisible(true);
                timerLabel.setVisible(false);
                buttonContainer.setVisible(false);
                startVoiceButton.setText("Start Recording");
                startVoiceButton.setGraphic(voiceImageView); // Change icon back to start
                sendAudioToApi(sourcelagnuage, targetlanguage);
                playAudioButton.setVisible(true); // Play the recorded audio after stopping
                playVideoButton.setVisible(false);
                startVideoButton.setVisible(true);
            } else {
                // Start Voice Recording
            	startTimer();
            	  timerLabel.setVisible(true);
            	 playAudioButton.setVisible(false);
            	 playVideoButton.setVisible(false);
            	 startVideoButton.setVisible(false);
                startRecording();
                startVoiceButton.setText("Stop Recording");
                startVoiceButton.setGraphic(stopImageView); // Change icon to stop
            }
            // Toggle the recording state
            isVoiceRecording[0] = !isVoiceRecording[0];
        });

        startVideoButton.setOnAction(event -> {
            if (isVideoRecording[0]) {
                // Stop Video Recording
                stopVideoRecording();
                stopTimer(); 
                timerLabel.setVisible(false);
                loaderContainer.setVisible(true);
                buttonContainer.setVisible(false);
                startVideoButton.setText("Start Recording");
                startVideoButton.setGraphic(videoImageView); // Change icon back to start
                sendAudioToApi(sourcelagnuage, targetlanguage);
                playVideoButton.setVisible(true);
                playAudioButton.setVisible(false);
                startVoiceButton.setVisible(true);
            } else {
                // Start Video Recording
            	timerLabel.setVisible(true);
            	startTimer();
            	 playVideoButton.setVisible(false);
            	 playAudioButton.setVisible(false);
            	 startVoiceButton.setVisible(false);
                startVideoRecording();
                startVideoButton.setText("Stop Recording");
                startVideoButton.setGraphic(stopImageView); // Change icon to stop
            }
            // Toggle the recording state
            isVideoRecording[0] = !isVideoRecording[0];
        });
        playAudioButton.setOnAction(event -> playRecordedAudio());
        playVideoButton.setOnAction(event -> {
            // Path to the video file (local or URL)
            String videoPath = finalVideoFilePath;  // Or URL like "http://example.com/video.mp4"
            
            // Create a Media object from the video file
            Media media = new Media(videoPath);
            
            // Create a MediaPlayer to control playback of the Media
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            
            // Create a MediaView to display the video
            MediaView mediaView = new MediaView(mediaPlayer);
            
            // Set up layout (you can change the layout or add more controls)
            StackPane root2 = new StackPane();
            root2.getChildren().add(mediaView);
            
            // Create a Scene and add it to the Stage
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);
            
            // Set the title and show the stage
            primaryStage.setTitle("JavaFX Video Player");
            primaryStage.show();
            
            // Play the video
            mediaPlayer.play();
        });
      

     // Response Area
        responseArea = new TextArea();
        //responseArea.setText("ਖੰਨਾ, (ਲੁਧਿਆਣਾ), 26 ਨਵੰਬਰ (ਹਰਜਿੰਦਰ ਸਿੰਘ ਲਾਲ)- ਆਮ ਆਦਮੀ ਪਾਰਟੀ ਦੇ ਪੰਜਾਬ ਦੇ ਨਵੇਂ ਪ੍ਰਧਾਨ ਅਮਨ ਅਰੋੜਾ ਸ਼ੁਕਰਾਨਾ ਯਾਤਰਾ ਕਰਦੇ ਹੋਏ ਖੰਨਾ ਪਹੁੰਚੇ, ਜਿੱਥੇ ਵਰਕਰਾਂ ਵਲੋਂ ਉਨ੍ਹਾਂ ਦਾ ਭਰਵਾਂ ਸਵਾਗਤ ਕੀਤਾ ਗਿਆ। ਇਸ ਮੌਕੇ ਕਾਰਜਕਾਰੀ ਪ੍ਰਧਾਨ ਸ਼ੈਰੀ ਕਲਸੀ ਵੀ ਉਨ੍ਹਾਂ ਦੇ ਨਾਲ ਸਨ। ਉਨ੍ਹਾਂ ਦਾ ਸਵਾਗਤ ਵੱਡੀ ਗਿਣਤੀ ਵਿਚ ਮੌਜੂਦ ਪਾਰਟੀ ਵਰਕਰਾਂ ਨੇ ਕੈਬਨਿਟ ਮੰਤਰੀ ਤਰੁਨਪ੍ਰੀਤ ਸਿੰਘ ਸੋਂਧ ਦੀ ਅਗਵਾਈ ਵਿਚ ਕੀਤਾ। ਪ੍ਰਧਾਨ ਅਰੋੜਾ ਨੇ ਕਿਹਾ ਕਿ ਪਾਰਟੀ ਵਰਕਰਾਂ ਨੂੰ 2027 ਵਾਸਤੇ ਤਕੜੇ ਹੋ ਜਾਣਾ ਚਾਹੀਦਾ ਹੈ ਕਿਉਂਕਿ ਜਿਮਨੀ ਚੋਣਾਂ ਵਿਚ ਪਾਰਟੀ ਦੀ ਵੱਡੀ ਜਿੱਤ ਹੋਈ ਹੈ। ਉਨ੍ਹਾਂ ਕਿਹਾ ਕਿ ਪਾਰਟੀ ਦੇ ਹਰ ਵਰਕਰ ਨੂੰ ਪੂਰੀ ਇੱਜ਼ਤ ਮਾਣ ਮਿਲੇਗਾ ਅਤੇ ਪਾਰਟੀ ਦੇ ਵਰਕਰਾਂ ਦੇ ਕਹੇ ਅਨੁਸਾਰ ਹੀ ਸਰਕਾਰ ਚੱਲੇਗੀ। ਉਨ੍ਹਾਂ ਨੇ ਦੱਸਿਆ ਕਿ ਆਮ ਆਦਮੀ ਪਾਰਟੀ ਦਾ ਸਥਾਪਨਾ ਦਿਵਸ ਵੀ ਅੱਜ ਹੀ ਹੈ ਤੇ ਅੱਜ ਦੇ ਦਿਨ ਹੀ ਪਾਰਟੀ ਕਨਵੀਨਰ ਕੇਜਰੀਵਾਲ ਵਲੋਂ ਪਾਰਟੀ ਦੀ ਸਥਾਪਨਾ ਕੀਤੀ ਗਈ ਸੀ। ਉਨ੍ਹਾਂ ਨੇ ਸਾਰੀ ਪਾਰਟੀ ਅਤੇ ਗੁਰੂ ਮਹਾਰਾਜ ਦਾ ਸ਼ੁਕਰਾਨਾ ਕੀਤਾ ਤੇ ਕਿਹਾ ਕਿ ਪੰਜਾਬ ਨੂੰ ਮੁੜ ਤੋਂ ਰੰਗਲਾ ਪੰਜਾਬ ਬਣਾਇਆ ਜਾਵੇਗਾ। ਉਨ੍ਹਾਂ ਅੱਗੇ ਕਿਹਾ ਕਿ ਪੂਰੀ ਲੀਡਰਸ਼ਿਪ ਦਾ ਮੈਂ ਧੰਨਵਾਦ ਕਰਦਾ ਹਾਂ ਜਿਨ੍ਹਾਂ ਨੇ ਮੈਨੂੰ ਇੰਨੀ ਵੱਡੀ ਜ਼ਿੰਮੇਵਾਰੀ ਸੌਂਪੀ ਹੈ, ਮੈਂ ਇਸ ਨੂੰ ਤਨਦੇਹੀ ਨਾਲ ਨਿਭਾਵਾਂਗਾ।");
        responseArea.setPromptText("API response will appear here...");
        responseArea.setPrefHeight(400); // Increased height
        responseArea.setPrefWidth(800);  // Increased width
        responseArea.setStyle("-fx-font-size: 16px; -fx-text-fill: #333; -fx-background-color: #F1F1F1; -fx-border-color: #87CEEB; -fx-border-width: 2; -fx-padding: 10; -fx-font-family: 'Noto Sans Devanagari', 'Mangal', 'Arial Unicode MS'; ");


        ImageView icon = new ImageView(new Image("file:D:/voice.png")); // Replace with your icon's path
        icon.setFitWidth(20); // Adjust icon size
        icon.setFitHeight(20);
        StackPane responseContainer = new StackPane();
        responseContainer.getChildren().addAll(responseArea, icon);
        StackPane.setAlignment(icon, Pos.BOTTOM_RIGHT);
        icon.setTranslateX(-10); // Adjust horizontal spacing (optional)
        icon.setTranslateY(-10); // Adjust vertical spacing (optional)

        icon.setOnMouseClicked(event->{
        	try {
        	    Media media = new Media(wavFile.toURI().toString());
        	    //MediaPlayer mediaPlayer = new MediaPlayer(media);
                AudioClip mediaPlayer = new AudioClip(media.getSource());
                mediaPlayer.play();
                
        	

        	} catch (Exception e) {
        	    e.printStackTrace();
        	    System.err.println("Error playing audio.");
        	}
        });
        // Progress Bar
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(100);   // Increased width
        progressBar.setStyle("-fx-accent: #4caf50; -fx-padding: 5;"); // Added padding for better appearance
        progressBar.setPrefHeight(20);  // Increased height

        // Add components to layout
        root.getChildren().addAll(
        		languageSelection,
        		buttonContainer,
                 responseContainer, progressBar,loaderContainer
        );

        // Create scene
        Scene scene = new Scene(root, 1100, 800, Color.LIGHTSKYBLUE); 
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        scene.setCursor(Cursor.CLOSED_HAND);
        primaryStage.setTitle("Voice Recorder");
        primaryStage.setScene(scene);
        primaryStage.setHeight(800);
        primaryStage.setWidth(1100);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Language Translator");
        //Image img = new Image("download.jpg");
       // primaryStage.getIcons().add(img);
        primaryStage.show();
    }
   
   
    private void playRecordevideo() {

	}


	// Format time in HH:mm:ss format
    private String formatTime(int seconds) {
        int hrs = seconds / 3600;
        int mins = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hrs, mins, secs);
    }
    private void startTimer() {
        elapsedSeconds = 0;
        timerLabel.setText("Elapsed Time: 00:00:00");

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            elapsedSeconds++;
            // Update the UI using Platform.runLater
            Platform.runLater(() -> timerLabel.setText("Elapsed Time: " + formatTime(elapsedSeconds)));
        }, 0, 1, TimeUnit.SECONDS);
    }
    private void stopTimer() {

            scheduler.shutdown();
        
    }

    private void startVideoRecording() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        if (isRecording) {
            return;  // If already recording, ignore the start request.
        }
        // Get the current timestamp and create filenames
        String timestamp = String.valueOf(System.currentTimeMillis()); // Or use LocalDateTime for readable format
        String videoFileName = "video_" + timestamp + ".avi";
        String audioFileName = "audio_" + timestamp + ".wav";
        String finalVideoFileName = "final_video_" + timestamp + ".avi";
        
        String projectPath = new File(System.getProperty("user.dir")).getAbsolutePath();
        String videoFilePath = projectPath  + File.separator + videoFileName;
        String audioFilePath = projectPath  + File.separator + audioFileName;
         finalVideoFilePath = projectPath  + File.separator + finalVideoFileName;
        audioFile =new File(audioFilePath);
        // Create a new thread for video recording to prevent blocking the UI
        	recordingThread = new Thread(() -> {
            camera = new VideoCapture(0);  // Access the default webcam
            if (!camera.isOpened()) {
                System.out.println("Camera not opened");
                return;
            }

            // VideoWriter to save the video
            Size frameSize = new Size(640, 480);  // Set the resolution
            writer = new VideoWriter(videoFilePath, VideoWriter.fourcc('M', 'J', 'P', 'G'), 30, frameSize);

            isRecording = true;

            // Start audio capture using FFmpeg in a separate process
            // For Windows
            String audioCommand = "ffmpeg -f dshow -i audio=\"Microphone Array (Intel® Smart Sound Technology for Digital Microphones)\" -ar 44100 -ac 2 -f wav " + audioFilePath;

            // For Linux or Mac (uncomment the next line and comment the above one if needed)
            // String audioCommand = "ffmpeg -f alsa -i default -acodec libmp3lame -ar 44100 -ac 2 -ab 192k -f wav " + audioFilePath;

            ProcessBuilder audioProcessBuilder = new ProcessBuilder(audioCommand.split(" "));
            try {
                // Start audio recording without storing the Process object
                audioProcessBuilder.start();  // Start audio recording
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Capture and save video frames
            Mat frame = new Mat();
            while (isRecording) {
                camera.read(frame);
                if (!frame.empty()) {
                    writer.write(frame);
                }
            }

            // Clean up after stopping the recording
            camera.release();  // Release the camera
            writer.release();  // Release the writer

            // Stop the audio recording process after the video recording stops
            try {
                // Force stop the audio recording process (Windows only)
                ProcessBuilder stopAudioProcessBuilder = new ProcessBuilder("taskkill", "/F", "/IM", "ffmpeg.exe");
                stopAudioProcessBuilder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // After stopping, combine audio and video into a single file
            Platform.runLater(() -> {
                // Combine audio and video into one file using FFmpeg
                combineAudioVideo(videoFilePath, audioFilePath, finalVideoFilePath);
            });
        });

        recordingThread.setDaemon(true);  // Allow JVM to exit even if thread is running
        recordingThread.start();  // Start the recording in a new thread
    }


    private void combineAudioVideo(String videoPath, String audioPath, String outputPath) {
        String[] command = {
            "ffmpeg",
            "-i", videoPath,   // Video input
            "-i", audioPath,   // Audio input
            "-c:v", "copy",     // Copy video codec (no re-encoding)
            "-c:a", "aac",      // Audio codec for the final output
            "-strict", "experimental", // Allow experimental codecs (AAC)
              // Use audio stream from the second input
            outputPath          // Output file path
        };

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);  // Merge error and output streams
            Process process = processBuilder.start();

            // Log the output of the process for debugging
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Wait for the process to finish and check the exit code
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Successfully combined audio and video into: " + outputPath);
            } else {
                System.err.println("Failed to combine audio and video. Exit code: " + exitCode);
            }
        String base64string =   convertFileToBase64(audioPath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


	

    private String convertFileToBase64(String filePath) {
        try {
            byte[] fileContent = Files.readAllBytes(new File(filePath).toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void stopVideoRecording() {
        if (!isRecording) {
            System.out.println("Not recording...");
            return;
        }

        isRecording = false;

        // Release camera and writer resources
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
        if (writer != null) {
            writer.release();
        }
    }

	private void populateLanguageComboBoxes(ComboBox<String> sourceLangComboBox, ComboBox<String> targetLangComboBox) {
        // Initialize the language map (key-value pairs)
        languageMap = new HashMap<>();
        languageMap.put("English", "en");
        languageMap.put("Punjabi", "pa");
        languageMap.put("Gujrati", "gu");
        languageMap.put("tamil", "ta");
        languageMap.put("Hindi", "hi");

        // Languages to display in ComboBox
        String[] languages = {"English", "Punjabi", "Gujrati", "tamil", "Hindi"};

        // Add items to combo boxes
        sourceLangComboBox.getItems().addAll(languages);
        targetLangComboBox.getItems().addAll(languages);

        // Set default values
        sourceLangComboBox.setValue("English");
        targetLangComboBox.setValue("Hindi");

        // Apply custom styles
        String comboBoxStyle = "-fx-background-color: #f4f4f4; " +
                                "-fx-border-color: #d1d1d1; " +
                                "-fx-border-radius: 5px; " +
                                "-fx-background-radius: 5px; " +
                                "-fx-padding: 5px; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-family: 'Arial';";

        sourceLangComboBox.setStyle(comboBoxStyle);
        targetLangComboBox.setStyle(comboBoxStyle);

        // Adding custom tooltips for better UX
        sourceLangComboBox.setTooltip(new Tooltip("Select the source language"));
        targetLangComboBox.setTooltip(new Tooltip("Select the target language"));

        // Event listener to update the language code when the user selects a language
        sourceLangComboBox.setOnAction(event -> {
            String selectedSourceLang = sourceLangComboBox.getValue();
            String sourceLangCode = languageMap.get(selectedSourceLang);  // Get language code for source language
            System.out.println("Selected source language code: " + sourceLangCode); // Update the data type or use this code
            sourcelagnuage =sourceLangCode;

            // You can now store or use `sourceLangCode` for translation API or other logic
        });

        // Similar event listener for target language if needed
        targetLangComboBox.setOnAction(event -> {
            String selectedTargetLang = targetLangComboBox.getValue();
            String targetLangCode = languageMap.get(selectedTargetLang);  // Get language code for target language
            System.out.println("Selected target language code: " + targetLangCode); // Update the data type or use this code
            targetlanguage =targetLangCode;
        });
    }

    private void startRecording() {
        if (isRecording) {
            return; // If already recording, don't start again
        }

        try {
            audioFormat = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line not supported");
                return;
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            audioFile = new File("recorded_audio_" + timestamp + ".wav");

            fileType = AudioFileFormat.Type.WAVE;
            audioBuffer = new byte[targetDataLine.getBufferSize() / 5];

            Thread recordingThread = new Thread(() -> {
                try (AudioInputStream audioStream = new AudioInputStream(targetDataLine)) {
                    AudioSystem.write(audioStream, fileType, audioFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            recordingThread.start();

            isRecording = true;
            System.out.println("Recording started...");
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (!isRecording) {
            return;
        }

        targetDataLine.stop();
        targetDataLine.close();

        isRecording = false;
        System.out.println("Recording stopped. File saved as: " + audioFile.getAbsolutePath());
    }

    private void sendAudioToApi(String sourceLanguage, String targetlanguage) {
        apiTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Read the audio file and encode it to base64
                byte[] audioData = Files.readAllBytes(audioFile.toPath());
                String base64Audio = Base64.getEncoder().encodeToString(audioData);

                // API URL for sending the request
                String apiUrl = "https://meity-auth.ulcacontrib.org/ulca/apis/v0/model/getModelsPipeline";

                // Create connection to the API
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("userID","42d8d58bc77f4a97861bdfe59b85d367");
                connection.setRequestProperty("ulcaApiKey","42c1d1d6df-e619-4b53-80c6-f1b86eb3a09a");

                // Prepare the JSON input string
                String jsonInputString = String.format(
                	    "{\"pipelineTasks\": [" +
                	        "{\"taskType\": \"asr\", \"config\": {\"language\": {\"sourceLanguage\": \"%s\"}}}," +
                	        "{\"taskType\": \"translation\", \"config\": {\"language\": {\"sourceLanguage\": \"%s\", \"targetLanguage\": \"%s\"}}} " +
                	     " ], \"pipelineRequestConfig\": {\"pipelineId\": \"64392f96daac500b55c543cd\"}}",
                	     sourceLanguage, sourceLanguage, targetlanguage
                	);


                // Write the JSON input to the output stream
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                catch (Exception e) {
				System.out.println(e.getMessage());
				}

                // Get the response code from the connection
                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response body
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        System.out.println(response);
                    }
                    String authorizationKey = "\"Authorization\"";
                    String serviceIdKey = "\"serviceId\"";

                    // Extract Authorization value
                    int authStartIndex = response.indexOf("\"value\":\"", response.indexOf(authorizationKey)) + 9;  // 8 is the length of "\"value\":\""
                    int authEndIndex = response.indexOf("\"", authStartIndex);
                    String authorizationValue = response.substring(authStartIndex, authEndIndex);
                    String serviceIdAsr=null;
                    String serviceIdTranslation=null;
                    int taskTypeStartIndexAsr = response.indexOf("\"taskType\":\"asr\""); // Find position of 'asr' task
                    if (taskTypeStartIndexAsr != -1) {
                        int configStartIndexAsr = response.indexOf("\"config\":[", taskTypeStartIndexAsr); // Find position of 'config' array
                        int serviceIdStartIndexAsr = response.indexOf("\"serviceId\":\"", configStartIndexAsr) + "\"serviceId\":\"".length();
                        int serviceIdEndIndexAsr = response.indexOf("\"", serviceIdStartIndexAsr); // Extract 'serviceId' value
                         serviceIdAsr = response.substring(serviceIdStartIndexAsr, serviceIdEndIndexAsr);
                        System.out.println("Service ID for ASR task: " + serviceIdAsr);
                    } else {
                        System.out.println("ASR task not found.");
                    }

                    // Extracting serviceId for taskType "translation"
                    int taskTypeStartIndexTrans = response.indexOf("\"taskType\":\"translation\""); // Find position of 'translation' task
                    if (taskTypeStartIndexTrans != -1) {
                        int configStartIndexTrans = response.indexOf("\"config\":[", taskTypeStartIndexTrans); // Find position of 'config' array
                        int serviceIdStartIndexTrans = response.indexOf("\"serviceId\":\"", configStartIndexTrans) + "\"serviceId\":\"".length();
                        int serviceIdEndIndexTrans = response.indexOf("\"", serviceIdStartIndexTrans); // Extract 'serviceId' value
                         serviceIdTranslation = response.substring(serviceIdStartIndexTrans, serviceIdEndIndexTrans);
                        System.out.println("Service ID for Translation task: " + serviceIdTranslation);
                    } else {
                        System.out.println("Translation task not found.");
                    }

                    // Print the extracted values
                    System.out.println("Authorization Value: " + authorizationValue);
                    System.out.println("Service ID: " + serviceIdAsr);
                    if(authorizationValue!=null && serviceIdAsr!=null && serviceIdTranslation!=null) {
                   String bhasniapidata =	callbhasiniapi(authorizationValue,serviceIdAsr,serviceIdTranslation,base64Audio,sourceLanguage,targetlanguage);
                   try {
                	    // For ASR task
                	    int taskTypeStartIndexAsr1 = bhasniapidata.indexOf("\"taskType\":\"asr\"");
                	    if (taskTypeStartIndexAsr1 != -1) {
                	        int outputStartIndexAsr = bhasniapidata.indexOf("\"output\":[", taskTypeStartIndexAsr1);
                	        int sourceStartIndexAsr = bhasniapidata.indexOf("\"source\":\"", outputStartIndexAsr) + "\"source\":\"".length();
                	        int sourceEndIndexAsr = bhasniapidata.indexOf("\"", sourceStartIndexAsr);
                	        String sourceAsr = bhasniapidata.substring(sourceStartIndexAsr, sourceEndIndexAsr);
                	        System.out.println("Source for ASR task: " + sourceAsr);
                	    } else {
                	        System.out.println("ASR task not found.");
                	    }

                	    // For Translation task
                	    int taskTypeStartIndexTrans1 = bhasniapidata.indexOf("\"taskType\":\"translation\"");
                	    if (taskTypeStartIndexTrans1 != -1) {
                	        int outputStartIndexTrans = bhasniapidata.indexOf("\"output\":[", taskTypeStartIndexTrans1);
                	        int sourceStartIndexTrans = bhasniapidata.indexOf("\"source\":\"", outputStartIndexTrans) + "\"source\":\"".length();
                	        int sourceEndIndexTrans = bhasniapidata.indexOf("\"", sourceStartIndexTrans);
                	        String sourceTrans = bhasniapidata.substring(sourceStartIndexTrans, sourceEndIndexTrans);
                	        
                	        int targetStartIndexTrans = bhasniapidata.indexOf("\"target\":\"", outputStartIndexTrans) + "\"target\":\"".length();
                	        int targetEndIndexTrans = bhasniapidata.indexOf("\"", targetStartIndexTrans);
                	         targetTrans = bhasniapidata.substring(targetStartIndexTrans, targetEndIndexTrans);
                	        
                	        System.out.println("Source for Translation task: " + sourceTrans);
                	        System.out.println("Target for Translation task: " + targetTrans);
                	    } else {
                	        System.out.println("Translation task not found.");
                	    }
                	} catch (Exception e) {
                	    e.printStackTrace();
                	}
                   // On success, update the UI to reflect the success
                    }
                    else {
                        apiTask.setOnSucceeded(event -> {
                            responseArea.setText("Authorization Value and Service ID is null");
                            progressBar.setProgress(1.0);
                        });
                    }

                   
                
                } else {
                    // If the response is not OK, capture the error stream
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }
                    // Print the error response
                    System.out.println("Error Response: " + response.toString());
                }


                return null;
            }

			private String callbhasiniapi(String authorizationValue, String serviceIdAsr,String serviceIdTranslation,String bas64data,String sourcelanguae,String targetlanguage) {
				String responsedata=null;
				System.out.println(bas64data);
				 String urlString = "https://dhruva-api.bhashini.gov.in/services/inference/pipeline";

				    // Prepare the JSON string with the variables included
				 String jsonInputString = "{\n" +
						    "    \"pipelineTasks\": [\n" +
						    "        {\n" +
						    "            \"taskType\": \"asr\",\n" +
						    "            \"config\": {\n" +
						    "                \"language\": {\n" +
						    "                    \"sourceLanguage\":\""+sourcelanguae +"\"\n" +
						    "                },\n" +
						    "                \"serviceId\": \"" + serviceIdAsr + "\",\n" + // serviceId from parameter
						    "                \"audioFormat\": \"wav\",\n" +
						    "                \"preProcessors\": [\"vad\"],\n" +
						    "                \"samplingRate\": 16000\n" +
						    "            }\n" +
						    "        },\n" +
						    "        {\n" +
						    "            \"taskType\": \"translation\",\n" +
						    "            \"config\": {\n" +
						    "                \"language\": {\n" +
						    "                    \"sourceLanguage\": \""+ sourcelanguae +"\",\n" +
						    "                    \"targetLanguage\": \""+ targetlanguage +"\"\n" +
						    "                },\n" +
						    "                \"serviceId\": \"" + serviceIdTranslation + "\"\n" + // serviceId from parameter
						    "            }\n" +
						    "        }\n" +
						    "    ],\n" +
						    "    \"inputData\": {\n" +
						    "        \"audio\": [\n" +
						    "            {\n" +
						    "                \"audioContent\": \"" + bas64data + "\"\n" + // bas64data from parameter
						    "            }\n" +
						    "        ]\n" +
						    "    }\n" +
						    "}";

				    
				    
				    try {
			            // Create a URL object from the API endpoint
			            URL url = new URL(urlString);
			            System.out.println(jsonInputString);

			            // Open a connection to the URL
			            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			            // Set request method
			            connection.setRequestMethod("POST");

			            // Set headers
			            connection.setRequestProperty("Accept", "*/*");
			         //   connection.setRequestProperty("User-Agent", "Thunder Client (https://www.thunderclient.com)");
			            connection.setRequestProperty("Authorization", authorizationValue);
			            connection.setRequestProperty("Content-Type", "application/json");

			            // Enable input/output streams
			            connection.setDoOutput(true);

			            // Write the request body
			            try (OutputStream os = connection.getOutputStream()) {
			                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
			                os.write(input, 0, input.length);
			            }

			            // Get response code and read the response
			            int responseCode = connection.getResponseCode();
			            System.out.println("Response Code: " + responseCode);

			            // Read the response
			            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			                String inputLine;
			                StringBuilder response = new StringBuilder();
			                while ((inputLine = in.readLine()) != null) {
			                    response.append(inputLine);
			                }
			                System.out.println("Response: " + response.toString());
			                responsedata = response.toString();
			                loaderContainer.setVisible(false);
			                buttonContainer.setVisible(true);
			            }

			        } catch (IOException e) {
			            e.printStackTrace();
			        }
				    
				    
				    
				return responsedata;
			}
        };

        // On success, update the UI to reflect the success
        apiTask.setOnSucceeded(event -> {
            responseArea.setText(targetTrans);
            progressBar.setProgress(1.0);
        });

        // On failure, update the UI to reflect the failure
        apiTask.setOnFailed(event -> {
            responseArea.setText("Failed to send audio for translation.");
            progressBar.setProgress(0);
        }); 

        // Start the task in a separate thread
        Thread apiThread = new Thread(apiTask);
        apiThread.start();
    }

    private void sendTextToApi(String sourceLanguage,String fileContent) {
        apiTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Read the audio file and encode it to base64
               
                // API URL for sending the request
                String apiUrl = "https://meity-auth.ulcacontrib.org/ulca/apis/v0/model/getModelsPipeline";

                // Create connection to the API
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("userID","42d8d58bc77f4a97861bdfe59b85d367");
                connection.setRequestProperty("ulcaApiKey","42c1d1d6df-e619-4b53-80c6-f1b86eb3a09a");

                // Prepare the JSON input string
                String jsonInputString = String.format(
                	    "{\"pipelineTasks\": [" +
                	        "{\"taskType\": \"tts\", \"config\": {\"language\": {\"sourceLanguage\": \"%s\"}}}" +
                	        
                	     " ], \"pipelineRequestConfig\": {\"pipelineId\": \"64392f96daac500b55c543cd\"}}",
                	     sourceLanguage
                	);
                
                System.out.println(jsonInputString);

                // Write the JSON input to the output stream
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                catch (Exception e) {
				System.out.println(e.getMessage());
				}

                // Get the response code from the connection
                int responseCode = connection.getResponseCode();
                System.out.println(responseCode);
                StringBuilder response = new StringBuilder();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response body
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        System.out.println(response);
                    }
                    String authorizationKey = "\"Authorization\"";
                    String serviceIdKey = "\"serviceId\"";

                    // Extract Authorization value
                    int authStartIndex = response.indexOf("\"value\":\"", response.indexOf(authorizationKey)) + 9;  // 8 is the length of "\"value\":\""
                    int authEndIndex = response.indexOf("\"", authStartIndex);
                    String authorizationValue = response.substring(authStartIndex, authEndIndex);
                    String serviceIdtts=null;
                    String serviceIdTranslation=null;
                    int taskTypeStartIndexAsr = response.indexOf("\"taskType\":\"tts\""); // Find position of 'asr' task
                    if (taskTypeStartIndexAsr != -1) {
                        int configStartIndexAsr = response.indexOf("\"config\":[", taskTypeStartIndexAsr); // Find position of 'config' array
                        int serviceIdStartIndexAsr = response.indexOf("\"serviceId\":\"", configStartIndexAsr) + "\"serviceId\":\"".length();
                        int serviceIdEndIndexAsr = response.indexOf("\"", serviceIdStartIndexAsr); // Extract 'serviceId' value
                        serviceIdtts = response.substring(serviceIdStartIndexAsr, serviceIdEndIndexAsr);
                        System.out.println("Service ID for ASR task: " + serviceIdtts);
                    } else {
                        System.out.println("ASR task not found.");
                    }


                    // Print the extracted values
                    System.out.println("Authorization Value: " + authorizationValue);
                    System.out.println("Service ID: " + serviceIdtts);
                    if(authorizationValue!=null && serviceIdtts!=null) {
                   String bhasniapidata =	callbhasinttsiapi(authorizationValue,serviceIdtts,fileContent,sourceLanguage);
                   try {
                	   JSONObject jsonObject = new JSONObject(bhasniapidata);

		                // Navigate to the audioContent
		                JSONArray pipelineResponse = jsonObject.getJSONArray("pipelineResponse");
		                if (pipelineResponse.length() > 0) {
		                    JSONObject firstTask = pipelineResponse.getJSONObject(0);
		                    JSONArray audioArray = firstTask.getJSONArray("audio");
		                    if (audioArray.length() > 0) {
		                        JSONObject audioObject = audioArray.getJSONObject(0);
		                        String audioContent = audioObject.getString("audioContent");
		                        System.out.println("audioContent: " + audioContent);
		                        base64audio=audioContent;
		                        
		                        
		                    		System.out.println(base64audio);
		                    		byte[] audioBytes = Base64.getMimeDecoder().decode(base64audio);
		                            wavFile = File.createTempFile("audio", ".wav");
		                            try(FileOutputStream fos = new FileOutputStream(wavFile)){
		                                fos.write(audioBytes);
		                                fos.flush();
		                            }
		                            if (wavFile.exists() && wavFile.length() > 0) {
		                                System.out.println("File size: " + wavFile.length() + " bytes");
		                                System.out.println("WAV file created at: " + wavFile.getAbsolutePath());
		                               
		                            } else {
		                                System.out.println("File creation failed or file is empty.");
		                            }
		                        
		                        
		                    } 
		                     
		                } else {
		                    System.out.println("No pipelineResponse data found.");
		                }
                	                    	
                   } catch (Exception e) {
                	    e.printStackTrace();
                	}
                   // On success, update the UI to reflect the success
                    }
                    else {
                        apiTask.setOnSucceeded(event -> {
                           // responseArea.setText("Authorization Value and Service ID is null");
                            progressBar.setProgress(1.0);
                        });
                    }

                   
                
                } else {
                    // If the response is not OK, capture the error stream
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }
                    // Print the error response
                    System.out.println("Error Response: " + response.toString());
                }


                return null;
            }

			private String callbhasinttsiapi(String authorizationValue, String serviceIdtts,String fileContent,String sourcelanguae) {
				String responsedata=null;
				System.out.println(fileContent);
				 String urlString = "https://dhruva-api.bhashini.gov.in/services/inference/pipeline";

				    // Prepare the JSON string with the variables included
				 String jsonInputString = "{\"pipelineTasks\":[{\"taskType\":\"tts\",\"config\":{\"language\":{\"sourceLanguage\":\""+sourcelanguae+"\"},\"serviceId\":\""+serviceIdtts+"\",\"gender\":\"male\",\"samplingRate\":8000}}],\"inputData\":{\"input\":[{\"source\":\""+fileContent.replaceAll("\n", "").trim()+"\"}]}}"
;

				    
				    
				    try {
			            // Create a URL object from the API endpoint
			            URL url = new URL(urlString);
			            System.out.println(jsonInputString);

			            // Open a connection to the URL
			            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			            // Set request method
			            connection.setRequestMethod("POST");

			            // Set headers
			            connection.setRequestProperty("Accept", "*/*");
			         //   connection.setRequestProperty("User-Agent", "Thunder Client (https://www.thunderclient.com)");
			            connection.setRequestProperty("Authorization", authorizationValue);
			            connection.setRequestProperty("Content-Type", "application/json");

			            // Enable input/output streams
			            connection.setDoOutput(true);

			            // Write the request body
			            try (OutputStream os = connection.getOutputStream()) {
			                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
			                os.write(input, 0, input.length);
			            }

			            // Get response code and read the response
			            int responseCode = connection.getResponseCode();
			            System.out.println("Response Code: " + responseCode);

			            // Read the response
			            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			                String inputLine;
			                StringBuilder response = new StringBuilder();
			                while ((inputLine = in.readLine()) != null) {
			                    response.append(inputLine);
			                }
			                System.out.println("Response: " + response.toString());
			                responsedata = response.toString();
			                loaderContainer.setVisible(false);
			                buttonContainer.setVisible(true);
			            }
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
				return responsedata;
			}
        };

        // On success, update the UI to reflect the success
        apiTask.setOnSucceeded(event -> {
           // responseArea.setText(targetTrans);
            progressBar.setProgress(1.0);
        });

        // On failure, update the UI to reflect the failure
        apiTask.setOnFailed(event -> {
           // responseArea.setText("Failed to send audio for translation.");
            progressBar.setProgress(0);
        }); 

        // Start the task in a separate thread
        Thread apiThread = new Thread(apiTask);
        apiThread.start();
    }


    private void playRecordedAudio() {
        try {
        	 String fileUri = "file:///" + audioFile.getAbsolutePath().replace("\\", "/");
            Media media = new Media(fileUri);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();
            System.out.println("Playing recorded audio...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

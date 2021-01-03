package application;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;

// can't be declared as a singleton
public class RootController implements Initializable {
	public static final int GRID_SPREAD = 2;
	
	private Stage stage;
	
	/*
	 * Vbox(root) 
	 * 	- MenuBar
	 * 		- Menu(menuTrack)
	 *  - ToolBar
	 *  	- Button(btnPlayOnOff)
	 *  	- Slider(musicLength)
	 *  - Pane
	 */
	@FXML private VBox root;
	@FXML private Menu menuTrack;
	@FXML private Pane mainPane;
	
	@FXML private Button btnPlayOnOff;
	@FXML private Slider musicLength;
	@FXML private Text timeDisplay;
	
	private StringBuilder sbForTimeDisplay = new StringBuilder();
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
//		System.out.println("initialized! : " + Thread.currentThread().getName());
		
		mainPane.setBackground(new Background(new BackgroundFill(Color.web("#000000"), CornerRadii.EMPTY, Insets.EMPTY)));
		
		// 호출 스레드가 생성 스레드와 다를 수 있다 
		musicLength.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> Observable, Number oldValue, Number newValue) {
//				System.out.print("changed in ");
//				System.out.println(Thread.currentThread().getName());
			}
		});
		
		Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.1), ev -> {
			if (MP3Player.getInstance().getIsRunning()) {
				mainPane.getChildren().clear();
				drawLines();
			}
//			System.out.println(Thread.currentThread().getName());
		}));
		
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
	}
	
	@FXML
	public void handleOpenFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("MP3 Files", "*.mp3"));
		fileChooser.setTitle("Open MP3 File");
		File selectedFile = fileChooser.showOpenDialog(stage);
		if (selectedFile != null) {
			String selectedFilePath = selectedFile.getPath();
			menuTrack.getItems().clear();
			
			MP3Player.getInstance().play(selectedFilePath);
		}
	}
	
	@FXML
	public void handleClose() {
		System.out.println("Close");
	}
	
	@FXML
	public void btnPlayOnOffAction() {
		MP3Player.getInstance().toggle();
	}
	
	@FXML
	public void handleMusicLengthDetected() {
//		System.out.println("detected");
		MP3Player.getInstance().setIsDragging(true);
	}
	
	@FXML
	public void handleMusicLengthDragged() {
//		System.out.println("dragged");
		MP3Player.getInstance().setIsDragging(true);
		MP3Player.getInstance().setCurrentSeconds((int)musicLength.getValue());
	}
	
	@FXML
	public void handleMusicLengthReleased() {
//		System.out.println("released");
		MP3Player.getInstance().setCurrentSeconds((int)musicLength.getValue());
		MP3Player.getInstance().setIsDragging(false);
	}
	
	public void drawLines() {
		int frameLength = MP3Player.getInstance().getMaxLengthOfFrames();
		double framePerPixelWidthRatio = frameLength / mainPane.getWidth();
		double framePerPixelHeightRatio = frameLength / mainPane.getHeight();
		byte[] allBytes = MP3Player.getInstance().getAllBytes();
		
		for (double i = 0, j = 0, k = 0; i < mainPane.getWidth(); ++j, i = framePerPixelWidthRatio * GRID_SPREAD * j, k = framePerPixelHeightRatio * GRID_SPREAD * j) {
			Line verticalLine = new Line(i, 0, i, mainPane.getHeight());
			verticalLine.setStroke(Color.LIGHTGRAY);
			Line horizontalLine = new Line(0, k, mainPane.getWidth(), k);
			horizontalLine.setStroke(Color.LIGHTGRAY);
			verticalLine.setStrokeWidth(0.1);
			horizontalLine.setStrokeWidth(0.1);
			if (j % 10 == 0) {
				Text textWidth = new Text(String.valueOf((int)i));
				textWidth.setX(i);
				textWidth.setY(20);
				textWidth.setStroke(Color.RED);
				Text textFrame = new Text(String.valueOf((int)i*(int)framePerPixelWidthRatio));
				textFrame.setX(i);
				textFrame.setY(40);
				textFrame.setStroke(Color.YELLOW);
				verticalLine.setStroke(Color.WHITE);
				verticalLine.setStrokeWidth(2);
				horizontalLine.setStrokeWidth(0.5);
				mainPane.getChildren().addAll(textWidth, textFrame);
			} 
			mainPane.getChildren().addAll(verticalLine, horizontalLine);
		}

		int frameBytesUnit = MP3Player.getInstance().getFrameBytesUnit();

		int bytesPerPixel = (int)(allBytes.length / mainPane.getWidth());
		for (int i = 0; i < mainPane.getWidth(); ++i) {
			double sum = 0.0;
			// 한 픽셀이 표현할 bytes 수 ==
			for (int byteIndex = i*bytesPerPixel, j = 0; j < bytesPerPixel; ++j, ++byteIndex) {
				sum += allBytes[byteIndex];
			}
			sum /= framePerPixelWidthRatio*frameBytesUnit/30.0;
			
			Line pcmLine = new Line(i, Math.min(mainPane.getHeight() / 2.0 + sum, mainPane.getHeight()), i, Math.max(mainPane.getHeight() / 2.0 - sum, 0));
			pcmLine.setStroke(Color.GREEN);
			pcmLine.setStrokeWidth(1.0);
			mainPane.getChildren().add(pcmLine);
		}
		
		int currentFrame = MP3Player.getInstance().calcFrameFromSeconds(MP3Player.getInstance().getCurrentSeconds());
		double currentX = currentFrame/framePerPixelWidthRatio;
		Line playingLine = new Line(currentX, 0, currentX, mainPane.getHeight());
		playingLine.setStroke(Color.RED);
		mainPane.getChildren().add(playingLine);
	}
	
	public Pane getMainPane() {
		return mainPane;
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
	public Slider getMusicLength() {
		return musicLength;
	}
	
	public void setMusicLengthValue(int seconds) {
		musicLength.setValue(seconds);
	}
	
	public void setMusicMaxLength(int seconds) {
		musicLength.setMax(seconds);
	}
	
	public void setTimeDisplay(int seconds) {
		sbForTimeDisplay.append(seconds/60);
		sbForTimeDisplay.append(":");
		sbForTimeDisplay.append(seconds%60);
		timeDisplay.setText(sbForTimeDisplay.toString());
		sbForTimeDisplay.delete(0, sbForTimeDisplay.length());
	}
}

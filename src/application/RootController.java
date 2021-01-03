package application;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

// can't be declared as a singleton
public class RootController implements Initializable {
	/*
	 * Vbox(root) 
	 * 	- MenuBar
	 * 		- Menu(menuTrack)
	 *  - ToolBar
	 *  	- Button(btnPlayOnOff)
	 *  	- Slider(musicLength)
	 *  - Pane
	 */
	private Stage stage;
	
	@FXML private VBox root;
	@FXML private Menu menuTrack;
	@FXML private Pane mainPane;
	
	@FXML private Button btnPlayOnOff;
	@FXML private Slider musicLength;
	@FXML private Text timeDisplay;
	
	private StringBuilder sbForTimeDisplay = new StringBuilder();
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
//		System.out.println("initialized!");
		
		mainPane.setBackground(new Background(new BackgroundFill(Color.web("#000000"), CornerRadii.EMPTY, Insets.EMPTY)));
		
		musicLength.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> Observable, Number oldValue, Number newValue) {
//				System.out.println("changed");
			}
		});
	}
	
	public void initMainPane(int row, int column, int trackNumber, String trackName, ArrayList<TreeMap<Integer, String>> keyRangeList) {
		mainPane.getChildren().clear();
		root.setPrefSize(Control.BASELINE_OFFSET_SAME_AS_HEIGHT, Control.BASELINE_OFFSET_SAME_AS_HEIGHT);
		mainPane.setPrefSize(root.getWidth(), root.getHeight());
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

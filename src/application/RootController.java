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
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
	 *  	- Slider(sliderLaunchpadSize)
	 */
	private Stage stage;
	
	@FXML private VBox root;
	@FXML private Menu menuTrack;
	@FXML private TilePane mainPane;
	
	@FXML private Button btnPlayOnOff;
	@FXML private Slider sliderLaunchpadSize;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
//		System.out.println("initialized!");
		
		mainPane.setBackground(new Background(new BackgroundFill(Color.web("#000000"), CornerRadii.EMPTY, Insets.EMPTY)));
		
		sliderLaunchpadSize.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> Observable, Number oldValue, Number newValue) {
				
			}
		});
	}
	
	public void initMainPane(int row, int column, int trackNumber, String trackName, ArrayList<TreeMap<Integer, String>> keyRangeList) {
		mainPane.getChildren().clear();
		root.setPrefSize(Control.BASELINE_OFFSET_SAME_AS_HEIGHT, Control.BASELINE_OFFSET_SAME_AS_HEIGHT);
		mainPane.setPrefSize(root.getWidth(), root.getHeight());
		double prefTileSize = mainPane.getPrefWidth()/column; 
		mainPane.setPrefTileWidth(prefTileSize);
		mainPane.setPrefTileHeight(prefTileSize);
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
	
	public TilePane getMainPane() {
		return mainPane;
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
}

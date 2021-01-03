package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;


public class Main extends Application {
	public static RootController rootController;
	
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../root.fxml"));
			Parent root = fxmlLoader.load();
			rootController = (RootController) fxmlLoader.getController();
			rootController.setStage(primaryStage);
			
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			
//			System.out.println(Thread.currentThread().getName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop() throws Exception {
		System.out.println(Thread.currentThread().getName()+": stop() 호출");
//		M3PPlayer.getInstance().closeSequencer();
	}
	
	public static void main(String[] args) {
//		System.out.println(Thread.currentThread().getName());
		launch(args);
	}
}

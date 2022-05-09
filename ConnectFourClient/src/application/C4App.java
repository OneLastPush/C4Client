package application;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * The starting point of the application. Prepares and displays the GUI.
 * 
 * @author Julien Comtois, Frank Birikundavyi, Marjorie Olano Morales
 * @version 11/2/2015
 */
public class C4App extends Application {

	private C4Controller controller;

	/**
	 * Launches the application.
	 * 
	 * @param args
	 *            Command line arguments. (Not used for this application)
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Loads and displays the GUI.
	 */
	@Override
	public void start(Stage primaryStage) throws IOException {
		// Constructing our scene
		URL url = getClass().getResource("C4Scene.fxml");
		FXMLLoader loader = new FXMLLoader(url);
		AnchorPane pane = (AnchorPane) loader.load();
		Scene scene = new Scene(pane);

		// Get a reference to the controller
		controller = (C4Controller) loader.getController();

		// Setting the stage
		primaryStage.setScene(scene);
		primaryStage.setTitle("Connect Four");
		primaryStage.setResizable(false);
		primaryStage.show();
	}

	/**
	 * Called when the application is closed.
	 */
	@Override
	public void stop() throws Exception {
		controller.disconnect();
		super.stop();
	}
}
package it.uniroma2.hoophub.utilities;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * NavigatorSingleton is a utility class that manages navigation between different scenes in the application.
 * It implements the singleton design pattern to ensure that only one instance of the navigator is used throughout the application.
 * <p>
 * This class is responsible for creating and displaying stages based on FXML files.
 */
public class NavigatorSingleton {
    private static NavigatorSingleton instance = null;
    protected Stage stage;

    /**
     * Protected constructor for creating a NavigatorSingleton instance.
     * This constructor is protected to prevent instantiation from outside the class.
     *
     * @param stage The primary stage of the application which will be used to display different scenes.
     */
    protected NavigatorSingleton(Stage stage) {
        this.stage = stage;
    }

    /**
     * Retrieves the single instance of NavigatorSingleton, creating it if it does not already exist.
     * This method ensures that the NavigatorSingleton is a singleton globally accessible throughout the application.
     *
     * @param stage The primary stage to be used by the navigator if the navigator is being created for the first time.
     * @return The single instance of NavigatorSingleton.
     */
    public static synchronized NavigatorSingleton getInstance(Stage stage) {
        if (NavigatorSingleton.instance == null) {
            NavigatorSingleton.instance = new NavigatorSingleton(stage);
        }
        return instance;
    }

    /**
     * Retrieves the existing single instance of NavigatorSingleton without creating a new one.
     * This method should be used only when sure that the instance has been previously created.
     *
     * @return The single instance of NavigatorSingleton.
     */
    public static synchronized NavigatorSingleton getInstance() {
        return instance;
    }


    /**
     * Navigates to a new page by loading the specified FXML file.
     * This method initializes a new scene with the contents of the FXML file and displays it on a new stage.
     *
     * @param fxmlPath the relative path to the FXML file to load, starting from the classpath root (for example, "/it/uniroma2/hoophub/fxml/Login.fxml").
     * @throws IOException if an error occurs while loading or parsing the FXML file.
     */
    public void gotoPage(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        Stage stagePage = new Stage();
        stagePage.setScene(new Scene(root));
        stagePage.setTitle("HoopHub");
        stagePage.setResizable(false);
        stagePage.show();
    }
}

package de.beinlich.markus.musicsystem.guifx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Markus
 */
public class MusicSystemFX extends Application {

    static private MusicClientFX musicClient;

    @Override
    public void start(Stage stage) throws Exception {
        musicClient = new MusicClientFX("FX-Client");

        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));

        Scene scene = new Scene(root);
        setUserAgentStylesheet(STYLESHEET_CASPIAN);
        stage.setTitle(musicClient.getMusicSystemName() + " - " + musicClient.getLocation() + " -  FX-Client");
        stage.setScene(scene);
        stage.show();
    }

    public static MusicClientFX getMusicClient() {
        return musicClient;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}

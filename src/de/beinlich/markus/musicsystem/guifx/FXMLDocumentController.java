package de.beinlich.markus.musicsystem.guifx;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Application;
import static javafx.application.Application.STYLESHEET_CASPIAN;
import static javafx.application.Application.STYLESHEET_MODENA;
import static javafx.application.Application.setUserAgentStylesheet;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.*;
import de.beinlich.markus.musicsystem.model.*;
import de.beinlich.markus.musicsystem.model.net.*;
import java.net.*;
import java.util.logging.*;
import javafx.event.*;

/**
 *
 * @author Markus
 */
public class FXMLDocumentController implements Initializable {

    private Label label;
    @FXML
    private Button buttonPlay;
    @FXML
    private Button buttonNext;
    @FXML
    private Button buttonPrevious;
    @FXML
    private Button buttonPause;
    @FXML
    private Button buttonStop;
    @FXML
    private Button buttonCSS;
    @FXML
    private Button buttonIpAdd;
    @FXML
    private Slider sliderVolume;
    @FXML
    private Slider sliderProgress;
    @FXML
    private ImageView cover;
    @FXML
    private ComboBox<String> comboBoxServer;
    @FXML
    private ComboBox<MusicPlayerInterface> comboBoxPlayer;
    @FXML
    private ComboBox<RecordInterface> comboBoxRecords;
    @FXML
    private Label labelCurrentTrack;
    @FXML
    private Label labelElapsedTime;
    @FXML
    private Label labelRemainingTime;
    @FXML
    private ScrollPane scrollPaneTrackList;
    @FXML
    private ListView<PlayListComponentInterface> listViewTrackList;

    private MusicSystemInterface musicSystem;
    private MusicCollectionInterface musicCollection;
    private MusicSystemControllerInterface musicSystemController;
    private MusicClientFX musicClient;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        musicClient = MusicSystemFX.getMusicClient();

        System.out.println(System.currentTimeMillis() + "**************MusicClient ist aktiv");
        musicSystem = musicClient;
        musicSystemController = musicClient;
        musicCollection = musicClient;

        labelElapsedTime.textProperty().bind(musicClient.getCurrentTimeTrackP().asString());
        labelRemainingTime.textProperty()
                .bind((musicClient.getCurrentTimeTrackP().subtract(musicClient.getPlayingTimeP()).asString()));

        buttonPlay.setOnAction(event -> musicSystemController.play());
        buttonStop.setOnAction(event -> musicSystemController.stop());
        buttonNext.setOnAction(event -> musicSystemController.next());
        buttonPrevious.setOnAction(event -> musicSystemController.previous());
        buttonPause.setOnAction(event -> musicSystemController.pause());
        buttonCssProperties();
        buttonIpAddProperties();

        comboBoxServerProperties();
        comboBoxRecordsProperties();
        comboBoxPlayerProperties();

        listViewTrackListProperties();

        sliderVolumeProperties();
        sliderProgressProperties();

        System.out.println(System.currentTimeMillis() + "musicSystem ist übergeben:" + musicSystem);

    }

    private void buttonCssProperties() {
        buttonCSS.setOnAction(event -> {
            if (Application.getUserAgentStylesheet().equals(STYLESHEET_CASPIAN)) {
                setUserAgentStylesheet(STYLESHEET_MODENA);
            } else {
                setUserAgentStylesheet(STYLESHEET_CASPIAN);
            }
        });
    }

    private void buttonIpAddProperties() {
        buttonIpAdd.setOnAction(event -> {
            try {
                musicClient.getServerPool().addServer("Local", new ServerAddr(50001, InetAddress.getLocalHost().getHostAddress(), "Local", true));
            } catch (UnknownHostException ex) {
                Logger.getLogger(AddLocalIpToServerPool.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private void comboBoxRecordsProperties() {
        comboBoxRecords.itemsProperty().bindBidirectional(musicClient.getMusicCollectionP());
        comboBoxRecords.getSelectionModel().select(musicSystem.getRecord());
        comboBoxRecords.setOnAction((ActionEvent event) -> {
            musicSystemController.setRecord(comboBoxRecords.getValue());
            cover.setImage(showCover());
        });
        musicClient.getRecordProp().addListener((observable, oldValue, newValue) -> {
            comboBoxRecords.getSelectionModel().select(newValue);
        });
    }

    private void comboBoxPlayerProperties() {
        comboBoxPlayer.itemsProperty().bindBidirectional(musicClient.getMusicPlayerP());
        comboBoxPlayer.getSelectionModel().select(musicSystem.getActivePlayer());
        comboBoxPlayer.setOnAction((event) -> {
            if (comboBoxPlayer != null && comboBoxPlayer.getValue() != null) {
                musicSystemController.setActivePlayer(comboBoxPlayer.getValue().getTitle());
            }
            //Werte des aktiven MusicPlayer anzeigen
            buttonPlay.setDisable(!musicSystem.hasPlay());
            buttonStop.setDisable(!musicSystem.hasStop());
            buttonNext.setDisable(!musicSystem.hasNext());
            buttonPrevious.setDisable(!musicSystem.hasPrevious());
            buttonPause.setDisable(!musicSystem.hasPause());
            sliderProgress.setDisable(!musicSystem.hasCurrentTime());
        });
        musicClient.getActivePlayerP().addListener((observable, oldValue, newValue) -> {
            comboBoxPlayer.getSelectionModel().select(newValue);
        });
    }

    private void listViewTrackListProperties() {
        listViewTrackList.itemsProperty().bindBidirectional(musicClient.getRecordP());
        listViewTrackList.setOnMouseClicked((value) -> {
            musicSystemController.setCurrentTrack(listViewTrackList.getSelectionModel().getSelectedItem());
        });
        musicClient.getPlayListComponentP().addListener((observable, oldValue, newValue) -> {
            listViewTrackList.getSelectionModel().select(newValue);
            labelCurrentTrack.setText(newValue.getTitle() + " : " + musicSystem.getCurrentTrack().getPlayingTime());
            sliderProgress.setValue(0);
            sliderProgress.setMax(newValue.getPlayingTime());
        });
    }

    private void sliderVolumeProperties() {
        sliderVolume.valueProperty().bindBidirectional(musicClient.getVolumeP());
        sliderVolume.setOnMouseReleased((event) -> {
            musicSystemController.setVolume(sliderVolume.getValue());
        });
    }

    private void sliderProgressProperties() {
        sliderProgress.valueProperty().bindBidirectional(musicClient.getCurrentTimeTrackP());
        sliderProgress.setOnMouseReleased((event) -> {
            musicSystemController.seek((int) sliderProgress.getValue());
        });
    }

    private void comboBoxServerProperties() {
        comboBoxServer.itemsProperty().bindBidirectional(musicClient.getServerPoolP());
        if (musicSystem.getServerAddr() != null) {
            comboBoxServer.getSelectionModel().select(musicSystem.getServerAddr().getName());
        }
        comboBoxServer.setOnAction((event) -> {
            if (comboBoxServer.getValue() != null
                    && !comboBoxServer.getValue().equals(musicClient.getCurrentServerAddr().getName())) {
                if (false == musicClient.switchToServer(comboBoxServer.getValue())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Server " + comboBoxServer.getValue() + " ist im Moment nicht erreichbar. Eventuell ist er nicht gestartet.");
//                    comboBoxServer.getSelectionModel().select(oldValue);
                } else {
                    System.out.println(System.currentTimeMillis() + "**************MusicClient ist aktiv");
                }
            }
        });
        musicClient.getServerAddrP().addListener((observable, oldValue, newValue) -> {
            comboBoxServer.getSelectionModel().select(newValue);
        });
    }

    private Image showCover() {
        Image img;
        img = null;
        if (hasCover()) {
            img = new Image(new ByteArrayInputStream(musicSystem.getRecord().getCover()), 150, 150, true, true);
        }
        return img;
    }

    private boolean hasCover() {
        return (musicSystem.getRecord().getCover() != null);
    }

}

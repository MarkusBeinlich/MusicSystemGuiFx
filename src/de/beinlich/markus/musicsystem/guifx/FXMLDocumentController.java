/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.beinlich.markus.musicsystem.guifx;

import de.beinlich.markus.musicsystem.lib.*;
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
        buttonCSS.setOnAction(event -> {
            if (Application.getUserAgentStylesheet().equals(STYLESHEET_CASPIAN)) {
                setUserAgentStylesheet(STYLESHEET_MODENA);
            } else {
                setUserAgentStylesheet(STYLESHEET_CASPIAN);
            }
        });

        comboBoxServer.itemsProperty().bindBidirectional(musicClient.getServerPoolP());
        comboBoxServer.getSelectionModel().select(musicSystem.getServerAddr().getName());
        comboBoxServer.setOnAction((event) -> {
            if (false == musicClient.switchToServer(comboBoxServer.getValue())) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Server " + comboBoxServer.getValue() + " ist im Moment nicht erreichbar. Eventuell ist er nicht gestartet.");
//                    comboBoxServer.getSelectionModel().select(oldValue);
            } else {
                System.out.println(System.currentTimeMillis() + "**************MusicClient ist aktiv");
            }
        });

        comboBoxRecords.itemsProperty().bindBidirectional(musicClient.getMusicCollectionP());
        comboBoxRecords.getSelectionModel().select(musicSystem.getRecord());
        comboBoxRecords.setOnAction((event) -> {
            musicSystemController.setRecord(comboBoxRecords.getValue());
            cover.setImage(showCover());
        });
        musicClient.getRecordProp().addListener((observable, oldValue, newValue) -> {
            comboBoxRecords.getSelectionModel().select(newValue);
        });

        comboBoxPlayer.itemsProperty().bindBidirectional(musicClient.getMusicPlayerP());
        comboBoxPlayer.getSelectionModel().select(musicSystem.getActivePlayer());
        comboBoxPlayer.setOnAction((event) -> {
            musicSystemController.setActivePlayer(comboBoxPlayer.getValue().getTitle());
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

        listViewTrackList.itemsProperty().bindBidirectional(musicClient.getRecordP());
        listViewTrackList.setOnMouseClicked((value) -> {
            musicSystemController.setCurrentTrack(listViewTrackList.getSelectionModel().getSelectedItem());
        });
        
        musicClient.getPlayListComponentP().addListener((observable, oldValue, newValue) -> {
//            System.out.println("getPlayListComponentP:" + newValue.getUid() + newValue + newValue.hashCode()+
//                    " - " + listViewTrackList.getItems().get(0).getUid()+ listViewTrackList.getItems().get(0).hashCode() +" - " + listViewTrackList.getItems().indexOf(newValue));
            listViewTrackList.getSelectionModel().select(newValue);
            labelCurrentTrack.setText(newValue.getTitle() + " : " + musicSystem.getCurrentTrack().getPlayingTime());
            sliderProgress.setValue(0);
            sliderProgress.setMax(newValue.getPlayingTime());
        });

        sliderVolume.valueProperty().bindBidirectional(musicClient.getVolumeP());
        sliderVolume.setOnMouseReleased((event) -> {
            musicSystemController.setVolume(sliderVolume.getValue());
        });

        sliderProgress.valueProperty().bindBidirectional(musicClient.getCurrentTimeTrackP());
        sliderProgress.setOnMouseReleased((event) -> {
            musicSystemController.seek((int) sliderProgress.getValue());
        });

        System.out.println(System.currentTimeMillis() + "musicSystem ist übergeben:" + musicSystem);


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

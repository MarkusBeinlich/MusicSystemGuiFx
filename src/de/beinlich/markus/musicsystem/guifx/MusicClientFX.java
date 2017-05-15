package de.beinlich.markus.musicsystem.guifx;

import de.beinlich.markus.musicsystem.model.net.*;
import de.beinlich.markus.musicsystem.model.*;
import static de.beinlich.markus.musicsystem.model.net.ProtokollType.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;

public class MusicClientFX implements Observer, Runnable, MusicSystemInterface, MusicSystemControllerInterface, MusicCollectionInterface {

    private final MusicClientNet musicClientNet;

    private ServerAddr currentServerAddr;
    private MusicSystemDto musicSystem;
    private RecordInterface record;
    private ServerPool serverPool;
    private MusicPlayerDto musicPlayer;
    private MusicCollectionDto musicCollection;
    private PlayListComponentDto playListComponent;
    private final ObjectProperty<PlayListComponentInterface> playListComponentP;
    private final ObjectProperty<RecordInterface> recordProp;
    private final ObjectProperty<MusicPlayerInterface> activePlayerP;
    private final ListProperty<PlayListComponentInterface> recordP;
    private final ListProperty<RecordInterface> musicCollectionP;
    private final ListProperty<MusicPlayerInterface> musicPlayerP;
    private final ListProperty<String> serverPoolP;
    private final StringProperty serverAddrP;
    private MusicSystemState musicSystemState;
    private final DoubleProperty volumeP;
    private final DoubleProperty currentTimeTrackP;
    private final DoubleProperty playingTimeP;

    public MusicClientFX(String clientName) {
        this.musicClientNet = new MusicClientNet(clientName);
        this.playingTimeP = new SimpleDoubleProperty(0.0);
        this.currentTimeTrackP = new SimpleDoubleProperty(0.0);
        this.volumeP = new SimpleDoubleProperty(0.0);
        this.serverAddrP = new SimpleStringProperty();
        this.musicPlayerP = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<>()));
        this.serverPoolP = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<>()));
        this.musicCollectionP = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<>()));
        this.recordP = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<>()));
        this.activePlayerP = new SimpleObjectProperty<>(null);
        this.recordProp = new SimpleObjectProperty<>(record);
        this.playListComponentP = new SimpleObjectProperty<>(playListComponent);

    }

    @Override
    public void run() {
        musicClientNet.addObserver(this);
        musicClientNet.netzwerkEinrichten();
        System.out.println(System.currentTimeMillis() + "netzwerk eingerichtet: ");
    }

    public boolean switchToServer(String newServer) {
        return musicClientNet.switchToServer(newServer);
    }

    @Override
    public RecordInterface getRecord() {
        return (RecordInterface) record;
    }

    @Override
    public void setRecord(RecordInterface record) {
        if (record != null && !this.record.equals(record)) {
            try {
                musicClientNet.writeObject(new Protokoll(RECORD_SELECTED, record));
            } catch (InvalidObjectException ex) {
                Logger.getLogger(MusicClientFX.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public String getMusicSystemName() {
        if (musicSystem == null) {
            return null;
        }
        return musicSystem.musicSystemName;
    }

    @Override
    public String getLocation() {
        return musicSystem.location;
    }

    @Override
    public void play() {
        try {
            musicClientNet.writeObject(new Protokoll(CLIENT_COMMAND_PLAY, musicSystemState));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void pause() {
        try {
            musicClientNet.writeObject(new Protokoll(CLIENT_COMMAND_PAUSE, musicSystemState));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void next() {
        try {
            musicClientNet.writeObject(new Protokoll(CLIENT_COMMAND_NEXT, playListComponent));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void previous() {
        try {
            musicClientNet.writeObject(new Protokoll(CLIENT_COMMAND_PREVIOUS, playListComponent));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void stop() {
        try {
            musicClientNet.writeObject(new Protokoll(CLIENT_COMMAND_STOP, musicSystemState));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setVolume(double volume) {
        try {
            musicClientNet.writeObject(new Protokoll(VOLUME, volume));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void seek(int currentTimeTrack) {
        try {
            musicClientNet.writeObject(new Protokoll(TRACK_TIME, currentTimeTrack));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setCurrentTrack(PlayListComponentInterface track) {
        try {
            //das Verändern des musicSystem/MusicSystem-Objektes muss vom Model/Server aus erfolgen. Sonst gibt es Rückkoppelungen
            //musicSystem.setCurrentTrack(listCurrentRecord.getSelectedValue());
            musicClientNet.writeObject(new Protokoll(TRACK_SELECTED, track));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean hasPause() {
        if (musicSystem != null & musicSystem.activePlayer != null) {
            return musicSystem.activePlayer.hasPause;
        }
        return false;
    }

    @Override
    public boolean hasPlay() {
        if (musicSystem != null & musicSystem.activePlayer != null) {
            return musicSystem.activePlayer.hasPlay;
        }
        return false;
    }

    @Override
    public boolean hasNext() {
        if (musicSystem != null & musicSystem.activePlayer != null) {
            return musicSystem.activePlayer.hasNext;
        }
        return false;
    }

    @Override
    public boolean hasPrevious() {
        if (musicSystem != null & musicSystem.activePlayer != null) {
            return musicSystem.activePlayer.hasPrevious;
        }
        return false;
    }

    @Override
    public boolean hasStop() {
        if (musicSystem != null & musicSystem.activePlayer != null) {
            return musicSystem.activePlayer.hasStop;
        }
        return false;
    }

    @Override
    public boolean hasTracks() {
        if (musicSystem != null & musicSystem.activePlayer != null) {
            return musicSystem.activePlayer.hasTracks;
        }
        return false;
    }

    @Override
    public boolean hasCurrentTime() {
        if (musicSystem != null & musicSystem.activePlayer != null) {
            return musicSystem.activePlayer.hasCurrentTime;
        }
        return false;
    }

    @Override
    public void setActivePlayer(String selectedPlayer
    ) {

        if (!(musicPlayer.title.equals(selectedPlayer))) {
            try {
                musicClientNet.writeObject(new Protokoll(MUSIC_PLAYER_SELECTED, selectedPlayer));

            } catch (InvalidObjectException ex) {
                Logger.getLogger(MusicClientFX.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public List<RecordInterface> getRecords() {
        return musicCollection.getRecords();
    }

    @Override
    public void update(Observable o, Object arg
    ) {
        Thread clientThread;
        Protokoll nachricht;
        MusicSystemState state;
        double volume;
        double trackTime;
        ClientInit clientInit;
        System.out.println("Observer: ");
        if (o instanceof MusicClientNet) {
            nachricht = (Protokoll) arg;
            System.out.println(System.currentTimeMillis() + "CLIENT: gelesen: " + nachricht + " - " + o.getClass());
            switch (nachricht.getProtokollType()) {
                case CLIENT_INIT:
                    clientInit = (ClientInit) nachricht.getValue();
                    musicSystem = clientInit.getMusicSystem();
                    musicCollection = clientInit.getMusicCollection();
                    serverPool = ServerPool.getInstance().addServers(clientInit.getServerPool().getServers());
                    musicSystemState = musicSystem.activePlayer.musicSystemState;
                    record = musicSystem.activePlayer.record;
                    musicPlayer = musicSystem.activePlayer;
                    playListComponent = musicSystem.activePlayer.currentTrack;
                    currentServerAddr = musicSystem.serverAddr;

                    Platform.runLater(() -> {
                        System.out.println("CLIENT_INIT 1:" );
                        musicCollectionP.set(FXCollections.observableList(new ArrayList<>()));
                        activePlayerP.set(musicSystem.activePlayer);
                         System.out.println("CLIENT_INIT 2:" );
                        recordP.set(FXCollections.observableList(record.getTracks()));
                         System.out.println("CLIENT_INIT 3:" );
                        playListComponentP.set(playListComponent);
                        musicPlayerP.set(FXCollections.observableList(musicSystem.players));
                        serverPoolP.set(FXCollections.observableList(ServerPool.getInstance().getActiveServers()));
                        getServerAddrP().set(musicSystem.serverAddr.getName());
                         System.out.println("CLIENT_INIT 4:" );
                        musicCollectionP.set(FXCollections.observableList(musicCollection.records));
                         System.out.println("CLIENT_INIT 5:" );
                        currentTimeTrackP.set(musicSystem.activePlayer.currentTimeTrack);
                        playingTimeP.set(musicSystem.activePlayer.currentTrack.playingTime);
                        volumeP.set(musicSystem.activePlayer.volume);
                        System.out.println("CLIENT_INIT vor RecordProp:" + record);
                        recordProp.set(record);
                    });
                    break;
                case SERVER_DISCONNECT:
                    musicSystem = new MusicSystemDto();
                    currentServerAddr = null;
                    musicCollection = new MusicCollectionDto();
                    ServerPool.getInstance().clear();
                    musicSystemState = null;
                    record = new RecordDto();
                    musicPlayer = new MusicPlayerDto();
                    playListComponent = new PlayListComponentDto();

                    Platform.runLater(() -> {
                        activePlayerP.set(musicSystem.activePlayer);
                        recordP.set(FXCollections.observableList(new ArrayList<>()));
                        playListComponentP.set(playListComponent);
                        musicPlayerP.set(FXCollections.observableList(new ArrayList<>()));
                        serverPoolP.set(FXCollections.observableList(ServerPool.getInstance().getActiveServers()));
                        getServerAddrP().set(null);
                        musicCollectionP.set(FXCollections.observableList(new ArrayList<>()));
                        currentTimeTrackP.set(0.0);
                        playingTimeP.set(0.0);
                        volumeP.set(0.0);
                        recordProp.set(record);

                    });
                    //Neustart des Client
                    clientThread = new Thread(this);
                    clientThread.setDaemon(true);
                    clientThread.start();
                    break;
                case MUSIC_COLLECTION_DTO:
                    this.musicCollection = (MusicCollectionDto) nachricht.getValue();
                    Platform.runLater(() -> {
                        musicCollectionP.setAll(musicCollection.getRecords());
                    });
                    break;
                case MUSIC_PLAYER_DTO:
                    musicPlayer = (MusicPlayerDto) nachricht.getValue();
                    Platform.runLater(() -> {
                        this.musicSystem.activePlayer = musicPlayer;
                        activePlayerP.set(musicPlayer);
                    });
                    break;
                case RECORD_DTO:
                    this.record = (RecordDto) nachricht.getValue();
                    Platform.runLater(() -> {
                        recordP.setAll(this.record.getTracks());
                        recordProp.set(this.record);
                    });
                    break;
                case STATE:
                    state = (MusicSystemState) nachricht.getValue();
                    System.out.println(System.currentTimeMillis() + "State");
                    musicSystemState = state;
                    break;
                case PLAY_LIST_COMPONENT_DTO:
                    playListComponent = (PlayListComponentDto) nachricht.getValue();
                    Platform.runLater(() -> {
                        playListComponentP.setValue(playListComponent);
                        playingTimeP.setValue(playListComponent.playingTime);
                    });
                    break;
                case TRACK_TIME:
                    trackTime = (int) nachricht.getValue();
                    Platform.runLater(() -> {
                        currentTimeTrackP.setValue(trackTime);
                    });
                    break;
                case VOLUME:
                    volume = (double) nachricht.getValue();
                    Platform.runLater(() -> {
                        volumeP.setValue(volume);
                    });
                    break;
                case SERVER_POOL:
                    this.serverPool.setServers((Map<String, ServerAddr>) nachricht.getValue());
                    Platform.runLater(() -> {
                        serverPoolP.setAll(this.serverPool.getActiveServers());
                    });
                    break;
                default:
                    System.out.println(System.currentTimeMillis() + "Unbekannte Nachricht:" + nachricht.getProtokollType());
            }
        }
    }

    public ServerAddr getCurrentServerAddr() {
        return currentServerAddr;
    }

    public void setCurrentServerAddr(ServerAddr currentServerAddr) {
        this.currentServerAddr = currentServerAddr;
    }

    public MusicCollectionDto getMusicCollection() {
        return musicCollection;
    }

    public ServerPool getServerPool() {
        return serverPool;
    }

    public ObjectProperty<PlayListComponentInterface> getPlayListComponentP() {
        return playListComponentP;
    }

    public ListProperty<PlayListComponentInterface> getRecordP() {
        return recordP;
    }

    public ListProperty<RecordInterface> getMusicCollectionP() {
        return musicCollectionP;
    }

    public ListProperty<MusicPlayerInterface> getMusicPlayerP() {
        return musicPlayerP;
    }

    public ListProperty<String> getServerPoolP() {
        return serverPoolP;
    }

    public DoubleProperty getVolumeP() {
        return volumeP;
    }

    public final DoubleProperty getCurrentTimeTrackP() {
        return currentTimeTrackP;
    }

    public ObjectProperty<RecordInterface> getRecordProp() {
        return recordProp;
    }

    public ObjectProperty<MusicPlayerInterface> getActivePlayerP() {
        return activePlayerP;
    }

    DoubleProperty getPlayingTimeP() {
        return playingTimeP;
    }

    public StringProperty getServerAddrP() {
        return serverAddrP;
    }

    @Override
    public ServerAddr getServerAddr() {
        if (musicSystem == null) {
            return null;
        }
        return musicSystem.serverAddr;
    }

    @Override
    public PlayListComponentInterface getCurrentTrack() {
        return (PlayListComponentInterface) playListComponent;
    }

    @Override
    public MusicPlayerInterface getActivePlayer() {
        return (MusicPlayerInterface) musicPlayer;
    }

    @Override
    public MusicSystemState getMusicSystemState() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<MusicPlayerInterface> getPlayers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getCurrentTimeTrack() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getVolume() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setActivePlayer(MusicPlayerInterface activePlayer) throws IllegalePlayerException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RecordInterface getRecord(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MusicSystemDto getDto() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MusicCollectionDto getMusicCollectionDto() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RecordInterface getRecordById(int rid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setFormat(String format) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public void registerObserver(MusicCollectionObserver o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MusicPlayerInterface getPlayer(String title) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

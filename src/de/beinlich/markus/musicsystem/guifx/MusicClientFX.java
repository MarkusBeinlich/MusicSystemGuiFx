package de.beinlich.markus.musicsystem.guifx;

import de.beinlich.markus.musicsystem.model.net.*;
import de.beinlich.markus.musicsystem.model.*;
import static de.beinlich.markus.musicsystem.model.net.ProtokollType.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;

/**
 *
 * @author Markus Beinlich
 */
public class MusicClientFX implements Runnable, MusicSystemInterface, MusicSystemControllerInterface, MusicCollectionInterface {

    // Verbindungsaufbau mit dem Server
    public Socket socket;
    private Socket newSocket;
    private ServerAddr currentServerAddr;
    private Thread readerThread;
    //
    // IO-Klassen zur Kommunikation
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private static final int MAX_RECONNECTIONS = 100;

    private MusicSystemDto musicSystem;
    private RecordInterface record;
    private ServerPool serverPool;
    private MusicPlayerDto musicPlayer;
    private MusicCollectionDto musicCollection;
    private PlayListComponentDto playListComponent;
    private ObjectProperty<PlayListComponentInterface> playListComponentP = new SimpleObjectProperty<>(playListComponent);
    private ObjectProperty<RecordInterface> recordProp = new SimpleObjectProperty<>(record);
    private ObjectProperty<MusicPlayerInterface> activePlayerP = new SimpleObjectProperty<>(null);
    private ListProperty<PlayListComponentInterface> recordP = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<PlayListComponentInterface>()));
    private ListProperty<RecordInterface> musicCollectionP = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<RecordInterface>()));
    private ListProperty<MusicPlayerInterface> musicPlayerP = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<MusicPlayerInterface>()));
    private ListProperty<String> serverPoolP = new SimpleListProperty<>(FXCollections.observableList(new ArrayList<String>()));
    private StringProperty serverAddrP = new SimpleStringProperty();
    private MusicSystemState musicSystemState;
    private DoubleProperty volumeP = new SimpleDoubleProperty(0.0);
    private DoubleProperty currentTimeTrackP = new SimpleDoubleProperty(0.0);
    private DoubleProperty playingTimeP = new SimpleDoubleProperty(0.0);
    private final String clientName;

    public MusicClientFX(String clientName) {
        this.clientName = clientName;

    }

    @Override
    public void run() {
        netzwerkEinrichten();
        musicSystemObjectRead();
        startReaderThread();
        System.out.println(System.currentTimeMillis() + "netzwerk eingerichtet: ");
    }

    private void netzwerkEinrichten() {
        serverPool = ServerPool.getInstance(clientName);
        System.out.println("Alle:" + serverPool.toString());
        while (socket == null) {
            for (Map.Entry<String, ServerAddr> poolEntry : serverPool.getServers().entrySet()) {
                ServerAddr serverAddr = poolEntry.getValue();
                System.out.println("ServerAddr: " + serverAddr);
                try {
                    // Erzeugung eines Socket-Objekts
                    //                  Rechner (Adresse / Name)
                    //                  |            Port

                    //Verbindungs-Parameter in property-file auslagern
                    NetProperties netProperties = new NetProperties();
                    System.out.println(System.currentTimeMillis() + "new Socket with " + serverAddr.getServer_ip() + serverAddr.getPort());
                    if (serverAddr.getServer_ip().equals("127.0.0.1")) {
                        throw new ConnectException();
                    }
//            socket = new Socket(InetAddress.getLocalHost(), 50001);
                    socket = new Socket(serverAddr.getServer_ip(), serverAddr.getPort());
                    // Erzeugung der Kommunikations-Objekte
                    ois = new ObjectInputStream(socket.getInputStream());
                    System.out.println(System.currentTimeMillis() + "socket.connect 2");
                    oos = new ObjectOutputStream(socket.getOutputStream());
                } catch (ConnectException e) {
                    System.out.println(System.currentTimeMillis() + "Error while connecting. " + e.getMessage());
                    continue;
                } catch (SocketTimeoutException e) {
                    System.out.println(System.currentTimeMillis() + "Connection: " + e.getMessage() + ".");
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(System.currentTimeMillis() + "socket.connect3");
            if (socket == null) {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MusicClientFX.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void startReaderThread() {
        // Thread der sich um die eingehende Kommunikation kümmert
        readerThread = new Thread(new MusicClientFX.EingehendReader(this));
        // Thread als (Hintergrund-) Service
        readerThread.setDaemon(true);
        readerThread.start();
        System.out.println(System.currentTimeMillis() + "CLIENT: Netzwerkverbindung steht jetzt");
    }

    private void musicSystemObjectRead() {
        Protokoll nachricht;
        ClientInit clientInit;
        System.out.println("musicSystemObjectRead");
        try {
            // Als erstes write den Namen des eigenen Client übergeben!
            oos.writeObject(new Protokoll(ProtokollType.CLIENT_NAME, clientName));
            oos.flush();
            try {
                // reinkommende Nachrichten vom Server. Auf diese muss gewartet werden, 
                // da ansonsten die initialisierung der GUI nicht funktioniert.
                nachricht = (Protokoll) ois.readObject(); // blockiert!
                clientInit = (ClientInit) nachricht.getValue();
                musicSystem = clientInit.getMusicSystem();
                musicCollection = clientInit.getMusicCollection();
                serverPool = ServerPool.getInstance(clientName).addServers(clientInit.getServerPool());
                musicSystemState = musicSystem.activePlayer.musicSystemState;
                record = musicSystem.activePlayer.record;
                musicPlayer = musicSystem.activePlayer;
                playListComponent = musicSystem.activePlayer.currentTrack;

                Platform.runLater(() -> {
                    activePlayerP.set(musicSystem.activePlayer);
                    recordP.set(FXCollections.observableList(record.getTracks()));
                    playListComponentP.set(playListComponent);
                    musicPlayerP.set(FXCollections.observableList(musicSystem.players));
                    serverPoolP.set(FXCollections.observableList(ServerPool.getInstance(clientName).getActiveServers()));
                    getServerAddrP().set(musicSystem.serverAddr.getName());
                    musicCollectionP.set(FXCollections.observableList(musicCollection.records));
                    currentTimeTrackP.set(musicSystem.activePlayer.currentTimeTrack);
                    playingTimeP.set(musicSystem.activePlayer.currentTrack.playingTime);
                    volumeP.set(musicSystem.activePlayer.volume);
                    recordProp.set(record);
                });
            } catch (ClassNotFoundException ex) {
                System.out.println(ex);
            }

        } catch (IOException ex) {
            System.out.println(System.currentTimeMillis() + "no connection - " + ex);
        }
    }

    public void writeObject(Protokoll protokoll) {
        try {
            System.out.println(System.currentTimeMillis() + "writeObject:" + protokoll.getProtokollType() + ": " + protokoll.getValue());
            // einen Befehl an der Server übertragen
            oos.writeObject(protokoll);
            oos.flush();
        } catch (IOException ex) {
            Logger.getLogger(Protokoll.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void tryToReconnect() {
        //        tryAllAddressesOnLan();
        try {
            System.out.println("Start sleep");
            Thread.sleep(10_000);
            System.out.println("Ende Sleep");
        } catch (InterruptedException ex) {
            Logger.getLogger(MusicClientFX.class.getName()).log(Level.SEVERE, null, ex);
        }
        netzwerkEinrichten();
    }

    private void tryAllAddressesOnLan() {
        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            Logger.getLogger(MusicClientFX.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        byte[] ip = localhost.getAddress();

        for (int i = 1; i <= 254; i++) {
            try {
                ip[3] = (byte) i;
                InetAddress address = InetAddress.getByAddress(ip);
                tryAddress(address);
            } catch (UnknownHostException e) {
                Logger.getLogger(MusicClientFX.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private void tryAddress(InetAddress address) {
        try {
            if (address.isReachable(10)) {
                System.out.println(address.toString().substring(1) + " is on the network");
                tryAllPorts(address.getHostAddress());
            }
        } catch (IOException ex) {
            Logger.getLogger(MusicClientFX.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void tryAllPorts(String hostAddress) {
        for (int j = 1; j <= 3; j++) {
            tryToConnectServer(hostAddress, 50000 + j);
        }
    }

    private void tryToConnectServer(String hostAddress, int port) {
        Socket socket;
        try {
            socket = new Socket(hostAddress, port);
            socket.close();
//            this.netzwerkEinrichten(new ServerAddr(port, hostAddress, currentServerAddr.getName(), true));
        } catch (ConnectException e) {
            System.out.println(System.currentTimeMillis() + "Error while connecting. " + e.getMessage());
        } catch (SocketTimeoutException e) {
            System.out.println(System.currentTimeMillis() + "Connection: " + e.getMessage() + ".");
        } catch (IOException e) {
            Logger.getLogger(MusicClientFX.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public boolean switchToServer(String newServer) {
        ServerAddr serverAddr;
        System.out.println("switchToServer:" + newServer);
        serverAddr = serverPool.getServers().get(newServer);
        System.out.println("switchToServer:" + serverAddr + ServerPool.getInstance(clientName));

        try {
            //wenn es geklappt hat, kann die Verbindung zum alten Server getrennt werden
            System.out.println("Old Socket:" + socket.hashCode());
            newSocket = new Socket(serverAddr.getServer_ip(), serverAddr.getPort());
            socket.close();
            System.out.println("Old Socket:" + socket.hashCode());
            System.out.println("LocalSocketAddress: " + newSocket.getLocalSocketAddress());
            System.out.println("RemoteSocketAddress: " + newSocket.getRemoteSocketAddress());

            // Erzeugung der Kommunikations-Objekte
            ois = new ObjectInputStream(newSocket.getInputStream());
            oos = new ObjectOutputStream(newSocket.getOutputStream());
            socket = newSocket;
            this.currentServerAddr = serverAddr;
            musicSystemObjectRead();
            startReaderThread();
            System.out.println(System.currentTimeMillis() + "netzwerk eingerichtet: ");
        } catch (IOException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    @Override
    public MusicPlayerInterface getActivePlayer() {
        return (MusicPlayerInterface) musicPlayer;
    }

    @Override
    public RecordInterface getRecord() {
        return (RecordInterface) record;
    }

    @Override
    public void setRecord(RecordInterface record) {
        if (!this.record.equals(record)) {
            try {
                writeObject(new Protokoll(RECORD_SELECTED, record));
            } catch (InvalidObjectException ex) {
                Logger.getLogger(MusicClientFX.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public MusicSystemState getMusicSystemState() {
        return musicSystemState;
    }

    @Override
    public List<MusicPlayerInterface> getPlayers() {
        return musicSystem.players;
    }

    @Override
    public PlayListComponentInterface getCurrentTrack() {
        return (PlayListComponentInterface) playListComponent;
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
    public int getCurrentTimeTrack() {
        return (int) getVolume();
    }

    @Override
    public double getVolume() {
        return volumeP.doubleValue();
    }

    @Override
    public void play() {
        try {
            writeObject(new Protokoll(CLIENT_COMMAND_PLAY, musicSystemState));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void pause() {
        try {
            writeObject(new Protokoll(CLIENT_COMMAND_PAUSE, musicSystemState));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void next() {
        try {
            writeObject(new Protokoll(CLIENT_COMMAND_NEXT, playListComponent));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void previous() {
        try {
            writeObject(new Protokoll(CLIENT_COMMAND_PREVIOUS, playListComponent));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void stop() {
        try {
            writeObject(new Protokoll(CLIENT_COMMAND_STOP, musicSystemState));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setVolume(double volume) {
        try {
            writeObject(new Protokoll(VOLUME, volume));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void seek(int currentTimeTrack) {
        try {
            writeObject(new Protokoll(TRACK_TIME, currentTimeTrack));

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
            writeObject(new Protokoll(TRACK_SELECTED, track));

        } catch (InvalidObjectException ex) {
            Logger.getLogger(MusicClientFX.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setActivePlayer(MusicPlayerInterface activePlayer) throws IllegalePlayerException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ServerAddr getServerAddr() {
        if (musicSystem == null) {
            return null;
        }
        return musicSystem.serverAddr;
    }

    @Override
    public boolean hasPause() {
        return musicSystem.activePlayer.hasPause;
    }

    @Override
    public boolean hasPlay() {
        return musicSystem.activePlayer.hasPlay;
    }

    @Override
    public boolean hasNext() {
        return musicSystem.activePlayer.hasNext;
    }

    @Override
    public boolean hasPrevious() {
        return musicSystem.activePlayer.hasPrevious;
    }

    @Override
    public boolean hasStop() {
        return musicSystem.activePlayer.hasStop;
    }

    @Override
    public boolean hasTracks() {
        return musicSystem.activePlayer.hasTracks;
    }

    @Override
    public boolean hasCurrentTime() {
        return musicSystem.activePlayer.hasCurrentTime;
    }

    @Override
    public void setActivePlayer(String selectedPlayer) {

        if (!(musicPlayer.title.equals(selectedPlayer))) {
            try {
                writeObject(new Protokoll(MUSIC_PLAYER_SELECTED, selectedPlayer));

            } catch (InvalidObjectException ex) {
                Logger.getLogger(MusicClientFX.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public MusicPlayerInterface getPlayer(String title) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<RecordInterface> getRecords() {
        return musicCollection.getRecords();
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

    class EingehendReader implements Runnable {

        private final MusicClientFX musicClient;
        private Protokoll nachricht;
        private MusicPlayerDto musicPlayer;
        private MusicSystemState state;
        private RecordDto record;
        double volume;
        double trackTime;

        private EingehendReader(MusicClientFX musicClient) {
            this.musicClient = musicClient;
        }

        @Override
        public void run() {

            try {
                while (true) {

                    // reinkommende Nachrichten vom Server
                    Object o = ois.readObject();
                    nachricht = (Protokoll) o; // blockiert!
                    System.out.println(System.currentTimeMillis() + "CLIENT: gelesen: " + nachricht + " - " + o.getClass());
                    switch (nachricht.getProtokollType()) {
                        case MUSIC_COLLECTION_DTO:
                            musicClient.musicCollection = (MusicCollectionDto) nachricht.getValue();
                            Platform.runLater(() -> {
                                musicCollectionP.setAll(musicCollection.getRecords());
                            });
                            break;
                        case MUSIC_PLAYER_DTO:
                            musicPlayer = (MusicPlayerDto) nachricht.getValue();
                            Platform.runLater(() -> {
                                musicClient.musicSystem.activePlayer = musicPlayer;
                                musicClient.musicPlayer = musicPlayer;
                                activePlayerP.set(musicPlayer);
                            });
                            break;
                        case RECORD_DTO:
                            musicClient.record = (RecordDto) nachricht.getValue();
                            Platform.runLater(() -> {
                                recordP.setAll(musicClient.record.getTracks());
                                recordProp.set(musicClient.record);
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
                            musicClient.serverPool = (ServerPool) nachricht.getValue();
                            Platform.runLater(() -> {
                                serverPoolP.setAll(musicClient.serverPool.getActiveServers());
                            });
                            break;
                        default:
                            System.out.println(System.currentTimeMillis() + "Unbekannte Nachricht:" + nachricht.getProtokollType());
                    }
                }

            } catch (IOException | ClassNotFoundException ex) {
                System.out.println(System.currentTimeMillis() + "CLIENT: Verbindung zum Server beendet - " + ex);
                ex.printStackTrace();
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

    public String getClientName() {
        return clientName;
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

    /**
     * @return the serverAddrP
     */
    public StringProperty getServerAddrP() {
        return serverAddrP;
    }
}

/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jfxchess.gui;

import org.asdfjkl.jfxchess.lib.*;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.lang.System;

public class GameModel {

    public static final int MAX_PV = 64;
    public static final int MAX_N_ENGINES = 10;
    public static final int MODE_ENTER_MOVES = 0;
    public static final int MODE_ANALYSIS = 1;
    public static final int MODE_PLAY_WHITE = 2;
    public static final int MODE_PLAY_BLACK = 3;
    public static final int MODE_GAME_ANALYSIS = 4;
    public static final int BOTH_PLAYERS = 5;
    public static final int MODE_PLAYOUT_POSITION = 7;
    public static final int STYLE_LIGHT = 0;
    public static final int STYLE_DARK = 1;
    public int THEME = STYLE_DARK;
    Game game;
    private final ArrayList<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private int currentMode;
    private int multiPv = 1;
    private boolean multiPvChanged = false;
    private boolean flipBoard = false;
    private boolean humanPlayerColor = CONSTANTS.WHITE;

    public boolean wasSaved = false;
    private int engineStrength = 20;
    private int engineThinkTimeSecs = 1;

    ArrayList<Engine> engines = new ArrayList<>();
    Engine activeEngine = null;

    private int gameAnalysisForPlayer = BOTH_PLAYERS;
    private double gameAnalysisThreshold = 0.5; // pawns
    private int gameAnalysisThinkTimeSecs = 3;  // seconds

    private boolean gameAnalysisJustStarted = false;

    public String currentBestPv = "";
    public int currentBestEval = 0;
    public int currentMateInMoves = -1;
    public boolean currentIsMate = false;

    public String childBestPv = "";
    public int childBestEval = 0;
    public int childMateInMoves = -1;
    public boolean childIsMate = false;

    public boolean doNotNotifyAboutResult = false;

    //public final Polyglot book;
    public final PolyglotExt extBook;

    private Preferences prefs;

    private static final int modelVersion = 430;

    private final PgnDatabase pgnDatabase;
    public int currentPgnDatabaseIdx = -1;
    public File lastOpenedDirPath = null;
    public File lastSaveDirPath = null;

    public boolean openDatabaseOnNextDialog = false;

    private String bookPathFile;

    private SearchPattern searchPattern;
    BoardStyle boardStyle;

    public GameModel() {
        this.game = new Game();
        Board b = new Board(true);

        pgnDatabase = new PgnDatabase();
        searchPattern = new SearchPattern();
        searchPattern.setSearchForHeader(true);
        boardStyle = new BoardStyle();

        this.game.getRootNode().setBoard(b);
        this.currentMode = MODE_ENTER_MOVES;

        String stockfishPath = getStockfishPath();
        // String bookPath = getBookPath();

        Engine stockfish = new Engine();
        stockfish.setName("Stockfish (Internal)");
        if(stockfishPath != null) {
            stockfish.setPath(stockfishPath);
        }
        stockfish.setInternal(true);
        engines.add(stockfish);
        activeEngine = stockfish;

        // manually add for internal stockfish up to 4 mpv
        EngineOption internalMPV = new EngineOption();
        internalMPV.name = "MultiPV";
        internalMPV.spinMin = 1;
        internalMPV.spinMax = 4;
        internalMPV.spinDefault = 1;
        internalMPV.spinValue = 1;
        internalMPV.type = EngineOption.EN_OPT_TYPE_SPIN;

        EngineOption internalElo = new EngineOption();
        internalElo.name = "UCI_Elo";
        internalElo.spinMin = 1320;
        internalElo.spinMax = 3190;
	// Previously 3190 wich surprisingly caused me to
	// beat stockfish without any problems.
        internalElo.spinDefault = 1320;
        internalElo.spinValue = 3190;
        internalElo.type = EngineOption.EN_OPT_TYPE_SPIN;

        EngineOption internalLimitStrength = new EngineOption();
        internalLimitStrength.name = "UCI_LimitStrength";
        internalLimitStrength.type = EngineOption.EN_OPT_TYPE_CHECK;
        internalLimitStrength.checkStatusDefault = false;
        internalLimitStrength.checkStatusValue = false;

        activeEngine.options.add(internalElo);
        activeEngine.options.add(internalMPV);
        activeEngine.options.add(internalLimitStrength);

        /*
        Engine stockfish_custom = new Engine();
        stockfish_custom.setName("Stockfish");
        if(stockfishPath != null) {
            stockfish_custom.setPath(stockfishPath);
        }
        engines.add(stockfish_custom);
        */

        /*
        book = new Polyglot();
        File file = null;
        if(bookPath != null) {
            file = new File(bookPath);
            book.loadBook(file);
        }*/

        File file = null;
        extBook = new PolyglotExt();
        String extBookPath = getExtBookPath();
        //DialogSimpleAlert dlg = new DialogSimpleAlert();
        //dlg.show(extBookPath, 0);
        if(extBookPath != null) {
            file = new File(extBookPath);
            extBook.loadBook(file);
        }

    }

    private String getStockfishPath() {

            String os = System.getProperty("os.name").toLowerCase();
            if(os.contains("win")) {

                String stockfishPath = "";
                String jarPath = "";
                String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                File tmp = (new File(jarPath));
                if(tmp.getParentFile().exists()) {
                    File subEngine = new File(tmp.getParentFile(), "engine");
                    stockfishPath = new File(subEngine, "stockfish.exe").getPath();
                    return stockfishPath;
                }
        }
        if(os.contains("linux")) {
                String stockfishPath = "";
                String jarPath = "";
                String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                File tmp = (new File(jarPath));
                if(tmp.getParentFile().exists()) {
                    if(tmp.getParentFile().getParentFile().exists()) {
                    File subEngine = new File(tmp.getParentFile().getParentFile(), "engine");
                    stockfishPath = new File(subEngine, "stockfish_x64").getPath();
                    return stockfishPath;
                    }
                }

        }
        return null;
    }

    private File getBaseBookPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")) {
            String jarPath = "";
            String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File tmp = (new File(jarPath));
            if(tmp.getParentFile().exists()) {
                File subBook = new File(tmp.getParentFile(), "book");
                return subBook;
                //bookPath = new File(subBook, "varied.bin").getPath();
                //return bookPath;
            }
        }
        if(os.contains("linux")) {
            String jarPath = "";
            String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File tmp = (new File(jarPath));
            if(tmp.getParentFile().exists()) {
                if(tmp.getParentFile().getParentFile().exists()) {
                    File subBook = new File(tmp.getParentFile().getParentFile(), "book");
                    return subBook;
                    //bookPath = new File(subBook, "varied.bin").getPath();
                    //return bookPath;
                }
            }
        }
        return null;
    }

    private String getBookPath() {
        File baseBook = getBaseBookPath();
        if(baseBook != null) {
            String bookPath = new File(baseBook, "varied.bin").getPath();
            return bookPath;
        }
        return null;
    }

    private String getExtBookPath() {

        //return "C:\\Users\\user\\MyFiles\\workspace\\extbook\\extbook.bin";

        File baseBook = getBaseBookPath();
        if(baseBook != null) {
            String bookPath = new File(baseBook, "extbook.bin").getPath();
            return bookPath;
        }
        return null;

    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public int getGameAnalysisForPlayer() { return gameAnalysisForPlayer; }

    public void setGameAnalysisForPlayer(int player) { gameAnalysisForPlayer = player; }

    public double getGameAnalysisThreshold() { return gameAnalysisThreshold; }

    public void setGameAnalysisThreshold(double threshold) { gameAnalysisThreshold = threshold; }

    public void setGameAnalysisThinkTimeSecs(int thinktimeSecs) { gameAnalysisThinkTimeSecs = thinktimeSecs; }

    public int getGameAnalysisThinkTimeSecs() { return gameAnalysisThinkTimeSecs; }

    public void setMode(int mode) {
        this.currentMode = mode;
    }

    public void setComputerThinkTimeSecs(int secs) {
        engineThinkTimeSecs = secs;
    }

    public int getComputerThinkTimeSecs() {
        return engineThinkTimeSecs;
    }

    public int getEngineStrength() { return engineStrength; }

    public void setEngineStrength(int strength) { engineStrength = strength; }

    public int getMode() {
        return currentMode;
    }

    public int getMultiPv() {
        return multiPv;
    }

    public void setFlipBoard(boolean flipBoard) {
        this.flipBoard = flipBoard;
    }

    public boolean getFlipBoard() {
        return flipBoard;
    }

    public void setHumanPlayerColor(boolean humanPlayerColor) {
        this.humanPlayerColor = humanPlayerColor;
    }

    public boolean getHumanPlayerColor() {
        return humanPlayerColor;
    }

    public boolean getGameAnalysisJustStarted() { return gameAnalysisJustStarted; }

    public void setGameAnalysisJustStarted(boolean val) { gameAnalysisJustStarted = val; }

    public PgnDatabase getPgnDatabase() {
        return pgnDatabase;
    }

    public SearchPattern getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(SearchPattern searchPattern) {
        this.searchPattern = searchPattern;
    }

    public void setMultiPv(int multiPv) {
        if(multiPv >= 1 && multiPv <= activeEngine.getMaxMultiPV() && multiPv <= MAX_PV) {
            this.multiPv = multiPv;
            this.multiPvChanged = true;
        }
    }

    public boolean wasMultiPvChanged() {
        return this.multiPvChanged;
    }

    public void setMultiPvChange(boolean b) {
        this.multiPvChanged = b;
    }

    public void addListener(StateChangeListener toAdd) {
        stateChangeListeners.add(toAdd);
    }

    public void triggerStateChange() {
        for (StateChangeListener sl : stateChangeListeners)
            sl.stateChange();
    }

    public void saveModel() {

        prefs = Preferences.userRoot().node(this.getClass().getName());

        prefs.putInt("modelVersion",modelVersion);

        PgnPrinter printer = new PgnPrinter();
        String pgn = printer.printGame(getGame());
        prefs.put("currentGame", pgn);
    }

    public void savePaths() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
        if(lastOpenedDirPath != null) {
            prefs.put("lastOpenDir", lastOpenedDirPath.toString());
        }
        if(lastSaveDirPath != null) {
            prefs.put("lastSaveDir", lastSaveDirPath.toString());
        }
    }

    public void saveBoardStyle() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.putInt("COLOR_STYLE", boardStyle.getColorStyle());
        prefs.putInt("PIECE_STYLE", boardStyle.getPieceStyle());
    }

    public void saveEngines() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        // Clean up preferences. Preferences for engines
        // which have been removed may still be there.
        for(int i=1;i<MAX_N_ENGINES;i++) {
            prefs.remove("ENGINE"+i);
        }
        for(int i=1;i<engines.size();i++) {
            Engine engine = engines.get(i);
            String engineString = engine.writeToString();
            prefs.put("ENGINE"+i, engineString);
        }
        prefs.putInt("ACTIVE_ENGINE_IDX", engines.indexOf(activeEngine));
    }

    public void saveBookPath() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
        if(!bookPathFile.isEmpty()) {
            prefs.put("bookPathFile", bookPathFile);
        }
    }

    public void saveGameAnalysisThresholds() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.putInt("GAME_ANALYSIS_SECS", getGameAnalysisThinkTimeSecs());
        prefs.putDouble("GAME_ANALYSIS_THRESHOLD", getGameAnalysisThreshold());
    }

    public void restoreGameAnalysisThresholds() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        if(mVersion == modelVersion) {
            int gameAnalysisSecs = prefs.getInt("GAME_ANALYSIS_SECS", 3);
            double gameAnalysisThreshold = prefs.getDouble("GAME_ANALYSIS_THRESHOLD", 0.5);
            setGameAnalysisThinkTimeSecs(gameAnalysisSecs);
            setGameAnalysisThreshold(gameAnalysisThreshold);
        }
    }

    public void saveNewGameSettings() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.putInt("COMPUTER_THINK_TIME_SECS", getComputerThinkTimeSecs());
        prefs.putDouble("COMPUTER_STRENGTH", getEngineStrength());
    }

    public void restoreNewGameSettings() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        if(mVersion == modelVersion) {
            int secs = prefs.getInt("COMPUTER_THINK_TIME_SECS", 3);
            int strength = prefs.getInt("COMPUTER_STRENGTH", 20);
            setComputerThinkTimeSecs(secs);
            setEngineStrength(strength);
        }
    }

    public void restoreBoardStyle() {

        BoardStyle style = new BoardStyle();
        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        if(mVersion == modelVersion) {
            int colorStyle = prefs.getInt("COLOR_STYLE", BoardStyle.STYLE_BLUE);
            int pieceStyle = prefs.getInt("PIECE_STYLE", BoardStyle.PIECE_STYLE_MERIDA);
            style.setPieceStyle(pieceStyle);
            style.setColorStyle(colorStyle);
        }
        boardStyle = style;
    }

    public void restoreEngines() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        if(mVersion == modelVersion) {

            for(int i=1;i<MAX_N_ENGINES;i++) {
                String engineString = prefs.get("ENGINE"+i, "");
                if(!engineString.isEmpty()) {
                    Engine engine = new Engine();
                    engine.restoreFromString(engineString);
                    // engine 0 is Stockfish internal
                    // engine 1 is Stockfish but where user
                    // is allowed to change parameters
                    // restore engine 1 to index 1
                    if(i == 1 && engines.size() > 1) {
                        engines.set(1, engine);
                    } else {
                        engines.add(engine);
                    }
                }
            }
            int activeIdx = prefs.getInt("ACTIVE_ENGINE_IDX", 0);
            if(activeIdx < engines.size()) {
                activeEngine = engines.get(activeIdx);
            } else {
                activeEngine = engines.get(0);
            }
        }
    }

    public void saveScreenGeometry(ScreenGeometry g) {

        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

        prefs.putDouble("WINDOW_POSITION_X", g.xOffset);
        prefs.putDouble("WINDOW_POSITION_Y", g.yOffset);
        prefs.putDouble("WINDOW_WIDTH", g.width);
        prefs.putDouble("WINDOW_HEIGHT", g.height);
        prefs.putDouble("MOVE_DIVIDER_RATIO", g.moveDividerRatio);
        prefs.putDouble("MAIN_DIVIDER_RATIO", g.mainDividerRatio);
    }

    public ScreenGeometry restoreScreenGeometry() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        ScreenGeometry g = new ScreenGeometry(-1.0, -1.0, -1.0,-1.0,
                ScreenGeometry.DEFAULT_MOVE_DIVIDER_RATIO,
                ScreenGeometry.DEFAULT_MAIN_DIVIDER_RATIO);

        if(mVersion == modelVersion) {

            g.xOffset = prefs.getDouble("WINDOW_POSITION_X", -1.0);
            g.yOffset = prefs.getDouble("WINDOW_POSITION_Y", -1.0);
            g.width = prefs.getDouble("WINDOW_WIDTH", -1.0);
            g.height = prefs.getDouble("WINDOW_HEIGHT", -1.0);
            g.moveDividerRatio = prefs.getDouble("MOVE_DIVIDER_RATIO",
                    ScreenGeometry.DEFAULT_MOVE_DIVIDER_RATIO);
            g.mainDividerRatio = prefs.getDouble("MAIN_DIVIDER_RATIO",
                    ScreenGeometry.DEFAULT_MAIN_DIVIDER_RATIO);

        }
        return g;

    }

    public void saveToolbarVisibility(boolean isVisible) {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.putBoolean("SHOW_TOOLBAR", isVisible);
    }

    public boolean restoreToolbarVisibility() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        return prefs.getBoolean("SHOW_TOOLBAR", true);
    }

    public void saveTheme() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.putInt("COLOR_THEME", this.THEME);
    }

    public void restoreTheme() {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        this.THEME = prefs.getInt("COLOR_THEME", STYLE_DARK);
    }

    public void restorePaths() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
        String lastOpenDir = prefs.get("lastOpenDir", "");
        String lastSaveDir = prefs.get("lastSaveDir", "");
        if (!lastOpenDir.isEmpty()) {
            lastOpenedDirPath = new File(lastOpenDir);
            if (!lastOpenedDirPath.exists()) {
                lastOpenedDirPath = null;
            }
        }
        if (!lastSaveDir.isEmpty()) {
            lastSaveDirPath = new File(lastSaveDir);
            if (!lastSaveDirPath.exists()) {
                lastSaveDirPath = null;
            }
        }
    }

    public void restoreModel() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        //intln(mVersion);
        if(mVersion == modelVersion) {
            PgnReader reader = new PgnReader();

            String pgn = prefs.get("currentGame", "");

            if(!pgn.isEmpty()) {
                Game g = reader.readGame(pgn);
                PgnPrinter p = new PgnPrinter();
                if (g.getRootNode().getBoard().isConsistent()) {
                    setGame(g);
                    g.setTreeWasChanged(true);
                    //triggerStateChange();
                }
            }
        }
    }

    // This "wrapper-method" became necessary to be able to
    // set engineID in engineOutputView from App
    public String getActiveEngineID() {
        return activeEngine.getName();
    }
    
}




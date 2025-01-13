package org.asdfjkl.jfxchess.gui;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;
import jfxtras.styles.jmetro.JMetro;
import javafx.scene.image.ImageView;
import jfxtras.styles.jmetro.Style;
import org.asdfjkl.jfxchess.lib.*;
import java.awt.*;
import java.util.ArrayList;


/**
 * JavaFX App
 */
public class App extends Application implements StateChangeListener {

    Text txtGameData;
    GameModel gameModel;
    EngineOutputView engineOutputView;

    ToggleButton tglEngineOnOff;

    SplitPane spChessboardMoves;
    SplitPane spMain;

    ModeMenuController modeMenuController;

    RadioMenuItem itmEnterMoves;

    ToolBar tbMainWindow;
    CheckMenuItem itmToggleToolbar;

    final KeyCombination keyCombinationCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationOpen = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationNextGame = new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationPreviousGame = new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationEnterPosition = new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationFlipBoard = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationAnalysis = new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationPlayWhite = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationPlayBlack = new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN);
    final KeyCombination keyCombinationEnterMoves = new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN);

    String moveBuffer = "";

    @Override
    public void start(Stage stage) {

        //TestCases tests = new TestCases();
        //tests.runPosHashTest();
        //tests.pgnScanTest();
        //tests.pgnScanSTRTest();
        //tests.testPolyglot();
        //tests.readGamesByStringTest();
        //tests.pgnReadAllMillBaseTest();

        //FooTest();

        gameModel = new GameModel();
        gameModel.restoreModel();
        gameModel.restoreBoardStyle();
        gameModel.restoreEngines();
        gameModel.restoreGameAnalysisThresholds();
        gameModel.restoreNewGameSettings();
        gameModel.restoreTheme();
        gameModel.restorePaths();
        ScreenGeometry screenGeometry = gameModel.restoreScreenGeometry();
        gameModel.getGame().setTreeWasChanged(true);
        gameModel.getGame().setHeaderWasChanged(true);

        // MENU
        MenuBar mnuBar = new MenuBar();

        Menu mnuFile = new Menu("Game");
        Menu mnuEdit = new Menu("Edit");
        Menu mnuMode = new Menu("Mode");
        Menu mnuView = new Menu("View");
        Menu mnuDatabase = new Menu("Database");
        Menu mnuHelp = new Menu("Help");

        // File Menu
        MenuItem itmNew = new MenuItem("New...");
        MenuItem itmOpenFile = new MenuItem("Open File");
        itmOpenFile.setAccelerator(keyCombinationOpen);
        MenuItem itmSaveCurrentGameAs = new MenuItem("Save Game");
        itmSaveCurrentGameAs.setAccelerator(keyCombinationSave);
        MenuItem itmPrintGame = new MenuItem("Print Game");
        MenuItem itmPrintPosition = new MenuItem("Print Position");
        MenuItem itmSavePositionAsImage = new MenuItem("Save Position As Image");
        MenuItem itmQuit = new MenuItem("Quit");

        mnuFile.getItems().addAll(itmNew, itmOpenFile, itmSaveCurrentGameAs,
                new SeparatorMenuItem(), itmPrintGame, itmPrintPosition, itmSavePositionAsImage,
                new SeparatorMenuItem(), itmQuit);

        // Edit Menu
        MenuItem itmCopyGame = new MenuItem("Copy Game");
        itmCopyGame.setAccelerator(keyCombinationCopy);
        MenuItem itmCopyPosition = new MenuItem("Copy Position");
        MenuItem itmPaste = new MenuItem("Paste Game/Position");
        itmPaste.setAccelerator(keyCombinationPaste);
        MenuItem itmEditGame = new MenuItem("Edit Game Data");
        MenuItem itmEnterPosition = new MenuItem("Enter Position");
        itmEnterPosition.setAccelerator(keyCombinationEnterPosition);
        MenuItem itmFlipBoard = new MenuItem("Flip Board");
        itmFlipBoard.setAccelerator(keyCombinationFlipBoard);
        MenuItem itmShowSearchInfo = new MenuItem("Show/Hide Search Info");

        mnuEdit.getItems().addAll(itmCopyGame, itmCopyPosition, itmPaste,
                new SeparatorMenuItem(), itmEditGame, itmEnterPosition,
                new SeparatorMenuItem(), itmFlipBoard, itmShowSearchInfo);

        // Mode Menu
        RadioMenuItem itmAnalysis = new RadioMenuItem("Analysis");
        itmAnalysis.setAccelerator(keyCombinationAnalysis);
        RadioMenuItem itmPlayAsWhite = new RadioMenuItem("Play as White");
        itmPlayAsWhite.setAccelerator(keyCombinationPlayWhite);
        RadioMenuItem itmPlayAsBlack = new RadioMenuItem("Play as Black");
        itmPlayAsBlack.setAccelerator(keyCombinationPlayBlack);
        itmEnterMoves = new RadioMenuItem("Enter Moves");
        itmEnterMoves.setAccelerator(keyCombinationEnterMoves);

        RadioMenuItem itmFullGameAnalysis = new RadioMenuItem("Full Game Analysis");
        RadioMenuItem itmPlayOutPosition = new RadioMenuItem("Play Out Position");

        ToggleGroup tglMode = new ToggleGroup();
        tglMode.getToggles().add(itmAnalysis);
        tglMode.getToggles().add(itmPlayAsWhite);
        tglMode.getToggles().add(itmPlayAsBlack);
        tglMode.getToggles().add(itmEnterMoves);
        tglMode.getToggles().add(itmFullGameAnalysis);
        tglMode.getToggles().add(itmPlayOutPosition);

        MenuItem itmEngines = new MenuItem("Engines...");

        mnuMode.getItems().addAll(itmAnalysis, itmPlayAsWhite, itmPlayAsBlack,
                itmEnterMoves, itmFullGameAnalysis, itmPlayOutPosition,
                new SeparatorMenuItem(), itmEngines);

        // View Menu
        MenuItem itmFullscreen = new MenuItem("Fullscreen");
        itmFullscreen.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        itmToggleToolbar = new CheckMenuItem("Show Toolbar");
        itmToggleToolbar.setSelected(true);
        MenuItem itmAppearance = new MenuItem("Appearance");
        MenuItem itmResetLayout = new MenuItem("Reset Layout");

        mnuView.getItems().addAll(itmFullscreen, itmToggleToolbar, itmAppearance, itmResetLayout);

        // Database Menu
        MenuItem itmBrowseDatabase = new MenuItem("Browse Database");
        MenuItem itmNextGame = new MenuItem("Next Game");
        itmNextGame.setAccelerator(keyCombinationNextGame);
        MenuItem itmPreviousGame = new MenuItem("Previous Game");
        itmPreviousGame.setAccelerator(keyCombinationPreviousGame);

        mnuDatabase.getItems().addAll(itmBrowseDatabase, itmNextGame, itmPreviousGame);

        // Help Menu
        MenuItem itmAbout = new MenuItem("About");
        MenuItem itmJerryHomepage = new MenuItem("JFXChess - Homepage");

        mnuHelp.getItems().addAll(itmAbout, itmJerryHomepage);

        mnuBar.getMenus().addAll(mnuFile, mnuEdit, mnuMode, mnuView, mnuDatabase, mnuHelp);

        // TOOLBAR
        tbMainWindow = new ToolBar();
        Button btnNew = new Button("New");
        btnNew.setGraphic(new ImageView( new Image("icons/document-new.png")));
        btnNew.setContentDisplay(ContentDisplay.TOP);

        Button btnOpen = new Button("Open File");
        btnOpen.setGraphic(new ImageView( new Image("icons/document-open.png")));
        btnOpen.setContentDisplay(ContentDisplay.TOP);

        Button btnSaveAs = new Button("Save Game");
        btnSaveAs.setGraphic(new ImageView( new Image("icons/document-save.png")));
        btnSaveAs.setContentDisplay(ContentDisplay.TOP);

        Button btnPrint = new Button("Print Game");
        btnPrint.setGraphic(new ImageView( new Image("icons/document-print.png")));
        btnPrint.setContentDisplay(ContentDisplay.TOP);

        Button btnFlipBoard = new Button("Flip Board");
        btnFlipBoard.setGraphic(new ImageView( new Image("icons/view-refresh.png")));
        btnFlipBoard.setContentDisplay(ContentDisplay.TOP);

        Button btnCopyGame = new Button("Copy Game");
        btnCopyGame.setGraphic(new ImageView( new Image("icons/edit-copy-pgn.png")));
        btnCopyGame.setContentDisplay(ContentDisplay.TOP);

        Button btnCopyPosition = new Button("Copy Position");
        btnCopyPosition.setGraphic(new ImageView( new Image("icons/edit-copy-fen.png")));
        btnCopyPosition.setContentDisplay(ContentDisplay.TOP);

        Button btnPaste = new Button("Paste");
        btnPaste.setGraphic(new ImageView( new Image("icons/edit-paste.png")));
        btnPaste.setContentDisplay(ContentDisplay.TOP);

        Button btnEnterPosition = new Button("Enter Position");
        btnEnterPosition.setGraphic(new ImageView( new Image("icons/document-enter-position.png")));
        btnEnterPosition.setContentDisplay(ContentDisplay.TOP);

        Button btnFullAnalysis = new Button("Full Analysis");
        btnFullAnalysis.setGraphic(new ImageView( new Image("icons/emblem-system.png")));
        btnFullAnalysis.setContentDisplay(ContentDisplay.TOP);

        Button btnBrowseGames = new Button("Browse Games");
        btnBrowseGames.setGraphic(new ImageView( new Image("icons/database.png")));
        btnBrowseGames.setContentDisplay(ContentDisplay.TOP);

        Button btnPrevGame = new Button("Prev. Game");
        btnPrevGame.setGraphic(new ImageView( new Image("icons/go-previous-blue.png")));
        btnPrevGame.setContentDisplay(ContentDisplay.TOP);

        Button btnNextGame = new Button("Next Game");
        btnNextGame.setGraphic(new ImageView( new Image("icons/go-next-blue.png")));
        btnNextGame.setContentDisplay(ContentDisplay.TOP);

        Button btnAbout = new Button("About");
        btnAbout.setGraphic(new ImageView( new Image("icons/help-browser.png")));
        btnAbout.setContentDisplay(ContentDisplay.TOP);

        tbMainWindow.getItems().addAll(btnNew, btnOpen, btnSaveAs, btnPrint, new Separator(),
                btnFlipBoard, new Separator(),
                btnCopyGame, btnCopyPosition, btnPaste, btnEnterPosition, new Separator(),
                btnFullAnalysis, new Separator(),
                btnBrowseGames, btnPrevGame, btnNextGame,  btnAbout);


        // Text & Edit Button for Game Data
        txtGameData = new Text("");
        txtGameData.setTextAlignment(TextAlignment.CENTER);
        Button btnEditGameData = new Button();
        btnEditGameData.setGraphic(new ImageView( new Image("icons/document_properties_small.png")));
        Region spacerGameDataLeft = new Region();
        Region spacerGameDataRight = new Region();

        HBox hbGameData = new HBox();
        hbGameData.getChildren().addAll(spacerGameDataLeft, txtGameData, spacerGameDataRight, btnEditGameData);
        hbGameData.setHgrow(spacerGameDataLeft, Priority.ALWAYS);
        hbGameData.setHgrow(spacerGameDataRight, Priority.ALWAYS);

        MoveView moveView = new MoveView(gameModel);

        // Navigation Buttons
        Button btnMoveBegin = new Button();
        btnMoveBegin.setGraphic(new ImageView( new Image("icons/go-first.png")));
        Button btnMoveEnd = new Button();
        btnMoveEnd.setGraphic(new ImageView( new Image("icons/go-last.png")));
        Button btnMovePrev = new Button();
        btnMovePrev.setGraphic(new ImageView( new Image("icons/go-previous.png")));
        Button btnMoveNext = new Button();
        btnMoveNext.setGraphic(new ImageView( new Image("icons/go-next.png")));

        HBox hbGameNavigation = new HBox();
        hbGameNavigation.setAlignment(Pos.CENTER);
        hbGameNavigation.getChildren().addAll(btnMoveBegin, btnMovePrev, btnMoveNext, btnMoveEnd);

        TabPane tabPaneMovesNotationBook = new TabPane();

        BookView bookView = new BookView(gameModel);

        Tab tabMoves = new Tab("Moves", moveView.getWebView());
        Tab tabNotation = new Tab("Score Sheet"  , new Label("score sheet"));
        //Tab tabBook = new Tab("Book" , new Label("book"));
        Tab tabBook = new Tab("Book" , bookView.bookTable);

        //tabPaneMovesNotationBook.getTabs().addAll(tabMoves, tabNotation, tabBook);
        tabPaneMovesNotationBook.getTabs().addAll(tabMoves, tabBook);
        tabPaneMovesNotationBook.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPaneMovesNotationBook.getStyleClass().add("underlined");

        VBox vbGameDataMovesNavigation = new VBox();
        vbGameDataMovesNavigation.getChildren().addAll(hbGameData, tabPaneMovesNotationBook, hbGameNavigation);
        vbGameDataMovesNavigation.setVgrow(moveView.getWebView(), Priority.ALWAYS);

        // put together  Chessboard | Game Navigation
        Chessboard chessboard = new Chessboard(gameModel);
        chessboard.boardStyle = gameModel.boardStyle;
        chessboard.resize(100,100);
        //chessboard.updateCanvas(); Already done inside resize().

        spChessboardMoves = new SplitPane();
        spChessboardMoves.getItems().addAll(chessboard, vbGameDataMovesNavigation);
        spChessboardMoves.setMaxHeight(Double.MAX_VALUE);

        // Buttons for Engine On/Off and TextFlow for Engine Output
        tglEngineOnOff = new ToggleButton("Off");
        Label lblMultiPV = new Label("Lines:");
        /*
        ComboBox<Integer> cmbMultiPV = new ComboBox<Integer>();
        cmbMultiPV.getItems().addAll(1,2,3,4);
        cmbMultiPV.setValue(1);
        */
        Button btnAddEngineLine = new Button();
        btnAddEngineLine.setText("+");
        Button btnRemoveEngineLine = new Button();
        btnRemoveEngineLine.setText("-");
        Button btnSelectEngine = new Button();
        btnSelectEngine.setGraphic(new ImageView( new Image("icons/document_properties_small.png")));
        HBox hbEngineControl = new HBox();
        Region spacerEngineControl = new Region();
        hbEngineControl.getChildren().addAll(tglEngineOnOff, lblMultiPV,
                btnAddEngineLine, btnRemoveEngineLine, spacerEngineControl, btnSelectEngine);
        hbEngineControl.setAlignment(Pos.CENTER);
        hbEngineControl.setMargin(lblMultiPV, new Insets(0,5,0,10));
        hbEngineControl.setHgrow(spacerEngineControl, Priority.ALWAYS);
        TextFlow txtEngineOut = new TextFlow();
        txtEngineOut.setPadding(new Insets(10,10,10,10));
        ScrollPane scpEngineOut = new ScrollPane(txtEngineOut);
        VBox vbBottom = new VBox();
        //vbBottom.getChildren().addAll(hbEngineControl, txtEngineOut);
        vbBottom.getChildren().addAll(hbEngineControl, scpEngineOut);
        vbBottom.setMinHeight(10);

        // put everything excl. the bottom Engine part into one VBox
        VBox vbMainUpperPart = new VBox();
        vbMainUpperPart.getChildren().addAll(mnuBar, tbMainWindow, spChessboardMoves);
        vbMainUpperPart.setVgrow(spChessboardMoves, Priority.ALWAYS);

        // add another split pane for main window part and engine output
        spMain = new SplitPane();
        spMain.setOrientation(Orientation.VERTICAL);
        spMain.getItems().addAll(vbMainUpperPart, vbBottom);

        GameMenuController gameMenuController = new GameMenuController(gameModel);

        itmOpenFile.setOnAction(actionEvent -> { gameMenuController.handleOpenGame(); } );
        btnOpen.setOnAction(actionEvent -> { gameMenuController.handleOpenGame(); } );

        itmSavePositionAsImage.setOnAction(actionEvent -> { gameMenuController.handleSaveBoardPicture(chessboard);});

        btnMoveNext.setOnAction(event -> moveView.goForward());
        btnMoveBegin.setOnAction(event -> moveView.seekToRoot());
        btnMovePrev.setOnAction(event -> moveView.goBack());
        btnMoveEnd.setOnAction(event -> moveView.seekToEnd());

        // events
        gameModel.addListener(chessboard);
        gameModel.addListener(moveView);
        gameModel.addListener(bookView);
        gameModel.addListener(this);

        // connect mode controller
        engineOutputView = new EngineOutputView(gameModel, txtEngineOut);
        // This will set the name of the restored active engine in the
        // engineOutputView. Previously it was always "Stockfish (internal)
        // at startup.
        engineOutputView.setEngineId(gameModel.getActiveEngineID());
        modeMenuController = new ModeMenuController(gameModel, engineOutputView);
        // Creation of engineController has been moved inside ModeMenuController.

        gameModel.addListener(engineOutputView);

        EditMenuController editMenuController = new EditMenuController(gameModel);

        gameModel.addListener(modeMenuController);
        modeMenuController.activateEnterMovesMode();

        itmSaveCurrentGameAs.setOnAction(e -> {
           gameMenuController.handleSaveCurrentGame();
        });

        itmPrintGame.setOnAction(actionEvent -> {
            gameMenuController.handlePrintGame(stage);
        });

        itmPrintPosition.setOnAction(actionEvent -> {
            gameMenuController.handlePrintPosition(stage);
        });

        itmPlayAsWhite.setOnAction(actionEvent -> {
            if(itmPlayAsWhite.isSelected()) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_WHITE) {
                    itmPlayAsWhite.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayWhiteMode();
                }
            }
        });

        itmPlayAsBlack.setOnAction(actionEvent -> {
            if(itmPlayAsBlack.isSelected()) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_BLACK) {
                    itmPlayAsBlack.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayBlackMode();
                }
            }
        });

        itmPlayOutPosition.setOnAction(actionEvent -> {
            if(gameModel.getMode() != GameModel.MODE_PLAYOUT_POSITION) {
                itmPlayOutPosition.setSelected(true);
                tglEngineOnOff.setSelected(true);
                tglEngineOnOff.setText("on");
                modeMenuController.activatePlayoutPositionMode();
            }
        });

        itmAnalysis.setOnAction(actionEvent -> {
            if(itmAnalysis.isSelected()) {
                if(gameModel.getMode() != GameModel.MODE_ANALYSIS) {
                    itmAnalysis.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activateAnalysisMode();
                }
            } else {
                /*
                itmEnterMoves.setSelected(true);
                tglEngineOnOff.setSelected(false);
                tglEngineOnOff.setText("Off");
                modeMenuController.activateEnterMovesMode();

                 */
            }
        });

        itmEnterMoves.setOnAction(actionEvent -> {
            tglEngineOnOff.setSelected(false);
            tglEngineOnOff.setText("Off");
            modeMenuController.activateEnterMovesMode();
        });

        itmFullGameAnalysis.setOnAction(actionEvent -> {
            handleFullGameAnalysis();
        });

        tglEngineOnOff.setOnAction(actionEvent -> {
            if(tglEngineOnOff.isSelected()) {
                itmAnalysis.setSelected(true);
                tglEngineOnOff.setText("On");
                modeMenuController.activateAnalysisMode();
            } else {
                itmEnterMoves.setSelected(true);
                tglEngineOnOff.setText("Off");
                modeMenuController.activateEnterMovesMode();
            }
        });


        btnAddEngineLine.setOnAction(actionEvent -> {
            int currentMultiPv = gameModel.getMultiPv();
            gameModel.setMultiPv(currentMultiPv+1);
            modeMenuController.engineSetOptionMultiPV(gameModel.getMultiPv());
            gameModel.triggerStateChange();
        });

        btnRemoveEngineLine.setOnAction(actionEvent -> {
            int currentMultiPv = gameModel.getMultiPv();
            gameModel.setMultiPv(currentMultiPv-1);
            modeMenuController.engineSetOptionMultiPV(gameModel.getMultiPv());
            gameModel.triggerStateChange();
        });

        itmNew.setOnAction(e -> {
            handleNewGame();
        });

        itmQuit.setOnAction(event -> {
            onExit(stage);
        });

        itmEditGame.setOnAction(e -> {
            editMenuController.editGameData();
        });

        btnEditGameData.setOnAction(e -> {
            editMenuController.editGameData();
        });

        itmEnterPosition.setOnAction(e -> {
            double height = Math.max(stage.getHeight() * 0.6, 520);
            editMenuController.enterPosition(height, chessboard.boardStyle);
        });

        itmFullscreen.setOnAction(e -> {
            stage.setFullScreen(true);
        });

        itmAppearance.setOnAction(e -> {
            DialogAppearance dlg = new DialogAppearance();
            double height = Math.max(stage.getHeight() * 0.6, 520);
            double width = height * 1.4;
            boolean accepted = dlg.show(chessboard.boardStyle, width, height, gameModel.THEME);
            if(accepted) {
                chessboard.boardStyle.setColorStyle(dlg.appearanceBoard.boardStyle.getColorStyle());
                chessboard.boardStyle.setPieceStyle(dlg.appearanceBoard.boardStyle.getPieceStyle());
                gameModel.boardStyle = chessboard.boardStyle;
                gameModel.THEME = dlg.selectedColorTheme;
                gameModel.triggerStateChange();
            }
        });

        itmToggleToolbar.setOnAction(e -> {
            if(itmToggleToolbar.isSelected()) {
                tbMainWindow.setVisible(true);
                tbMainWindow.setManaged(true);
            } else {
                tbMainWindow.setVisible(false);
                tbMainWindow.setManaged(false);
            }
        });

        itmEngines.setOnAction(e -> {
            modeMenuController.editEngines();
        });

        btnSelectEngine.setOnAction(e -> {
            modeMenuController.editEngines();
        });

        itmCopyGame.setOnAction(e -> {
            editMenuController.copyGame();
        });

        itmCopyPosition.setOnAction(e -> {
            editMenuController.copyPosition();
        });

        itmPaste.setOnAction(e -> {
            String pasteString = Clipboard.getSystemClipboard().getString().trim();
            editMenuController.paste(pasteString);
        });

        itmResetLayout.setOnAction(e -> {
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            stage.setWidth(screenBounds.getWidth() * ScreenGeometry.DEFAULT_WIDTH_RATIO);
            stage.setHeight(screenBounds.getHeight() * ScreenGeometry.DEFAULT_HEIGHT_RATIO);
            spChessboardMoves.setDividerPosition(0, ScreenGeometry.DEFAULT_MOVE_DIVIDER_RATIO);
            spMain.setDividerPosition(0, ScreenGeometry.DEFAULT_MAIN_DIVIDER_RATIO);
            stage.centerOnScreen();
            gameModel.triggerStateChange();
        });

        itmFlipBoard.setOnAction(e -> {
            gameModel.setFlipBoard(!gameModel.getFlipBoard());
            gameModel.triggerStateChange();
        });

        itmShowSearchInfo.setOnAction(e -> {
            if(engineOutputView.isEnabled()) {
                engineOutputView.disableOutput();
            } else {
                engineOutputView.enableOutput();
            }
            gameModel.triggerStateChange();
        });

        itmBrowseDatabase.setOnAction(e -> {
            gameMenuController.handleBrowseDatabase();
        });

        itmNextGame.setOnAction(e -> {
            gameMenuController.handleNextGame();
        });

        itmPreviousGame.setOnAction(e -> {
            gameMenuController.handlePrevGame();
        });

        itmAbout.setOnAction(event -> {
            DialogAbout.show(gameModel.THEME);
        });

        itmJerryHomepage.setOnAction(event -> {
            getHostServices().showDocument("https://github.com/asdfjkl/jfxchess");
        });

        stage.setOnCloseRequest(event -> {
            event.consume();
            onExit(stage);
        });

        // buttons
        btnNew.setOnAction(e -> {
            handleNewGame();
        });

        btnOpen.setOnAction(e -> {
            gameMenuController.handleOpenGame();
        });

        btnSaveAs.setOnAction(e -> {
            gameMenuController.handleSaveCurrentGame();
        });

        btnPrint.setOnAction(e -> {
            gameMenuController.handlePrintGame(stage);
        });

        btnFlipBoard.setOnAction(e -> {
            gameModel.setFlipBoard(!gameModel.getFlipBoard());
            gameModel.triggerStateChange();
        });

        btnCopyGame.setOnAction(e -> {
            editMenuController.copyGame();
        });

        btnCopyPosition.setOnAction(e -> {
            editMenuController.copyPosition();
        });

        btnPaste.setOnAction(e -> {
            String pasteString = Clipboard.getSystemClipboard().getString().trim();
            editMenuController.paste(pasteString);
        });

        btnEnterPosition.setOnAction(e -> {
            double height = Math.max(stage.getHeight() * 0.6, 520);
            editMenuController.enterPosition(height, chessboard.boardStyle);
        });

        btnFullAnalysis.setOnAction(e -> {
            handleFullGameAnalysis();
        });

        btnBrowseGames.setOnAction(e -> {
            gameMenuController.handleBrowseDatabase();
        });

        btnPrevGame.setOnAction(e -> {
            gameMenuController.handlePrevGame();
        });

        btnNextGame.setOnAction(e -> {
            gameMenuController.handleNextGame();
        });

        btnAbout.setOnAction(e -> {
            DialogAbout.show(gameModel.THEME);
        });


        Scene scene = new Scene(spMain);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            /* to enter moves via keyboard */
            if (event.getCode() == KeyCode.A ) {
                addToMoveBuffer("a");
            }
            if (event.getCode() == KeyCode.B ) {
                addToMoveBuffer("b");
            }
            if (event.getCode() == KeyCode.C ) {
                addToMoveBuffer("c");
            }
            if (event.getCode() == KeyCode.D ) {
                addToMoveBuffer("d");
            }
            if (event.getCode() == KeyCode.E ) {
                addToMoveBuffer("e");
            }
            if (event.getCode() == KeyCode.F ) {
                addToMoveBuffer("f");
            }
            if (event.getCode() == KeyCode.G ) {
                addToMoveBuffer("g");
            }
            if (event.getCode() == KeyCode.H ) {
                addToMoveBuffer("h");
            }
            if (event.getCode() == KeyCode.Q ) {
                addToMoveBuffer("q");
            }
            if (event.getCode() == KeyCode.R ) {
                addToMoveBuffer("r");
            }
            if (event.getCode() == KeyCode.N ) {
                addToMoveBuffer("n");
            }
            if (event.getCode() == KeyCode.B ) {
                addToMoveBuffer("b");
            }
            if (event.getCode() == KeyCode.DIGIT1 ) {
                addToMoveBuffer("1");
            }
            if (event.getCode() == KeyCode.DIGIT2 ) {
                addToMoveBuffer("2");
            }
            if (event.getCode() == KeyCode.DIGIT3 ) {
                addToMoveBuffer("3");
            }
            if (event.getCode() == KeyCode.DIGIT4 ) {
                addToMoveBuffer("4");
            }
            if (event.getCode() == KeyCode.DIGIT5 ) {
                addToMoveBuffer("5");
            }
            if (event.getCode() == KeyCode.DIGIT6 ) {
                addToMoveBuffer("6");
            }
            if (event.getCode() == KeyCode.DIGIT7 ) {
                addToMoveBuffer("7");
            }
            if (event.getCode() == KeyCode.DIGIT8 ) {
                addToMoveBuffer("8");
            }

            /* other gui controls */
            if (event.getCode() == KeyCode.RIGHT) {
                moveView.goForward();
            }
            if (event.getCode() == KeyCode.LEFT) {
                moveView.goBack();
            }
            if (event.getCode() == KeyCode.HOME) {
                moveView.seekToRoot();
            }
            if (event.getCode() == KeyCode.END) {
                moveView.seekToEnd();
            }
            if (keyCombinationEnterPosition.match(event)) {
                // setup position
                double height = Math.max(stage.getHeight() * 0.6, 520);
                editMenuController.enterPosition(height, chessboard.boardStyle);
            }
            if (keyCombinationFlipBoard.match(event)) {
                gameModel.setFlipBoard(!gameModel.getFlipBoard());
                gameModel.triggerStateChange();
            }
            if(keyCombinationAnalysis.match(event)) {
                // enter analysis mode
                if(gameModel.getMode() != GameModel.MODE_ANALYSIS) {
                    itmAnalysis.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activateAnalysisMode();
                }
            }
            if(keyCombinationPlayWhite.match(event)) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_WHITE) {
                    itmPlayAsWhite.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayWhiteMode();
                }
            }
            if(keyCombinationPlayBlack.match(event)) {
                if(gameModel.getMode() != GameModel.MODE_PLAY_BLACK) {
                    itmPlayAsBlack.setSelected(true);
                    tglEngineOnOff.setSelected(true);
                    tglEngineOnOff.setText("On");
                    modeMenuController.activatePlayBlackMode();
                }
            }
            if(keyCombinationEnterMoves.match(event)|| event.getCode() == KeyCode.ESCAPE) {
                // enter moves mode
                tglEngineOnOff.setSelected(false);
                tglEngineOnOff.setText("Off");
                modeMenuController.activateEnterMovesMode();
            }
            if(event.getCode() == KeyCode.F11) {
                stage.setFullScreen(true);
            }
            if(keyCombinationNextGame.match(event)) {
                gameMenuController.handleNextGame();
            }
            if(keyCombinationPreviousGame.match(event)) {
                gameMenuController.handlePrevGame();
            }
            if(keyCombinationCopy.match(event)) {
                editMenuController.copyGame();
            }
            if(keyCombinationPaste.match(event)) {
                String pasteString = Clipboard.getSystemClipboard().getString().trim();
                editMenuController.paste(pasteString);
            }
            if(keyCombinationOpen.match(event)) {
                gameMenuController.handleOpenGame();
            }
            if(keyCombinationSave.match(event)) {
                gameMenuController.handleSaveCurrentGame();
            }
            event.consume();
        });

        itmEnterMoves.setSelected(true);

        Style style = Style.DARK;
        if(gameModel.THEME == gameModel.STYLE_LIGHT) {
            style = Style.LIGHT;
        }
        JMetro jMetro = new JMetro(style);
        jMetro.setScene(scene);
        //jMetro.setAutomaticallyColorPanes(true);

        SplashScreen splash = SplashScreen.getSplashScreen();

        if (splash != null && splash.isVisible()) {
            splash.close();
        }

        stage.setScene(scene);
        // get primary screen bounds and set these to scene or
        // restore previously stet screen geometry
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        if(screenGeometry.isValid()) {
            stage.setX(screenGeometry.xOffset);
            stage.setY(screenGeometry.yOffset);
            stage.setWidth(screenGeometry.width);
            stage.setHeight(screenGeometry.height);
        } else {
            //stage.setX(screenBounds.);
            //stage.setY(screenGeometry.yOffset);
            stage.setWidth(screenBounds.getWidth() * ScreenGeometry.DEFAULT_WIDTH_RATIO);
            stage.setHeight(screenBounds.getHeight() * ScreenGeometry.DEFAULT_HEIGHT_RATIO);
        }

        boolean toolBarVisible = gameModel.restoreToolbarVisibility();
        if(toolBarVisible) {
            itmToggleToolbar.setSelected(true);
            tbMainWindow.setVisible(true);
            tbMainWindow.setManaged(true);
        } else {
            itmToggleToolbar.setSelected(false);
            tbMainWindow.setVisible(false);
            tbMainWindow.setManaged(false);
        }

        gameModel.triggerStateChange();

        // un-focus any default button etc.
        spMain.requestFocus();

        stage.getIcons().add(new Image("icons/app_icon.png"));
        stage.show();

        // recover divider ratios at the last step, as show() might trigger resizes
        spChessboardMoves.setDividerPosition(0, screenGeometry.moveDividerRatio);
        spMain.setDividerPosition(0, screenGeometry.mainDividerRatio);

    }

    @Override
    public void stateChange() {

        if(gameModel.getGame().isHeaderChanged()) {
            String white = gameModel.getGame().getHeader("White");
            String black = gameModel.getGame().getHeader("Black");
            String site = gameModel.getGame().getHeader("Site");
            String date = gameModel.getGame().getHeader("Date");

            String label = white + " - " + black + "\n" + site + ", " + date;
            txtGameData.setText(label);
        }
        if(gameModel.getMode() == GameModel.MODE_ENTER_MOVES) {
            tglEngineOnOff.setSelected(false);
            tglEngineOnOff.setText("Off");
        } else {
            tglEngineOnOff.setSelected(true);
            tglEngineOnOff.setText("On");
        }

    }


    public static void main(String[] args) {
        launch();
    }

    private void addToMoveBuffer(String s) {

        moveBuffer += s;
        if(moveBuffer.length() == 4) {
            Move move = new Move(moveBuffer);
            Board board = gameModel.getGame().getCurrentNode().getBoard();
            if (!board.isLegalAndPromotes(move)) {
                if (board.isLegal(move)) {
                    gameModel.getGame().applyMove(move);
                    gameModel.triggerStateChange();
                }
                moveBuffer = "";
            }
        }
        if(moveBuffer.length() == 5) {
            Move move = new Move(moveBuffer);
            Board board = gameModel.getGame().getCurrentNode().getBoard();
            if (board.isLegal(move)) {
                gameModel.getGame().applyMove(move);
                gameModel.triggerStateChange();
            }
            moveBuffer = "";
        }
    }

    private void onExit(Stage stage) {

        gameModel.saveModel();
        ScreenGeometry g = new ScreenGeometry(stage.getX(),
                stage.getY(), stage.getWidth(), stage.getHeight(),
                spChessboardMoves.getDividerPositions()[0],
                spMain.getDividerPositions()[0]);
        gameModel.saveScreenGeometry(g);
        gameModel.saveBoardStyle();
        gameModel.saveEngines();
        gameModel.saveGameAnalysisThresholds();
        gameModel.saveNewGameSettings();
        gameModel.saveToolbarVisibility(tbMainWindow.isVisible());
        gameModel.saveTheme();
        gameModel.savePaths();

        modeMenuController.stopEngine();
        ArrayList<Task> runningTasks = gameModel.getPgnDatabase().getRunningTasks();
        for (Task task : runningTasks) {
            task.cancel();
        }

        stage.close();
    }

    private void handleNewGame() {
        DialogNewGame dlg = new DialogNewGame();
        boolean accepted = dlg.show(gameModel.activeEngine,
                gameModel.getComputerThinkTimeSecs(),
                gameModel.THEME);
        if(accepted) {
            gameModel.wasSaved = false;
            gameModel.currentPgnDatabaseIdx = -1;
            gameModel.setComputerThinkTimeSecs(dlg.thinkTime);
            gameModel.activeEngine.setElo(dlg.strength);
            Game g = new Game();
            g.getRootNode().setBoard(new Board(true));
            gameModel.setGame(g);
            gameModel.getGame().setTreeWasChanged(true);
            gameModel.getGame().setHeaderWasChanged(true);
            if(dlg.rbWhite.isSelected()) {
                gameModel.setFlipBoard(false);
            } else {
                gameModel.setFlipBoard(true);
            }
            if(dlg.rbComputer.isSelected()) {
                if(dlg.rbWhite.isSelected()) {
                    modeMenuController.activatePlayWhiteMode();
                } else {
                    modeMenuController.activatePlayBlackMode();
                }
            } else {
                modeMenuController.activateEnterMovesMode();
            }
        }
    }

    private void handleFullGameAnalysis() {
        DialogGameAnalysis dlg = new DialogGameAnalysis();
        boolean accepted = dlg.show(gameModel.getGameAnalysisThinkTimeSecs(),
                ((double) gameModel.getGameAnalysisThreshold()),
                gameModel.THEME);
        if(accepted) {
            itmEnterMoves.setSelected(true);
            tglEngineOnOff.setSelected(true);
            tglEngineOnOff.setText("On");

            if(dlg.rbBoth.isSelected()) {
                gameModel.setGameAnalysisForPlayer(GameModel.BOTH_PLAYERS);
            }
            if(dlg.rbWhite.isSelected()) {
                gameModel.setGameAnalysisForPlayer(CONSTANTS.IWHITE);
            }
            if(dlg.rbBlack.isSelected()) {
                gameModel.setGameAnalysisForPlayer(CONSTANTS.IBLACK);
            }
            gameModel.setGameAnalysisThinkTimeSecs(dlg.sSecs.getValue());
            gameModel.setGameAnalysisThreshold(dlg.sPawnThreshold.getValue());
            gameModel.setMode(GameModel.MODE_GAME_ANALYSIS);
            modeMenuController.activateGameAnalysisMode();
        }
    }

}

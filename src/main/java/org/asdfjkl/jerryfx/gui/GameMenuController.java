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

package org.asdfjkl.jerryfx.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.print.PrinterJob;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.asdfjkl.jerryfx.lib.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class GameMenuController {

    final FileChooser fileChooser = new FileChooser();
    final PgnReader reader = new PgnReader();
    final GameModel gameModel;

    public GameMenuController(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    public void handleBrowseDatabase() {
        DialogDatabase dlg = new DialogDatabase();
        boolean accepted = false;
        if(gameModel.openDatabaseOnNextDialog) {
            accepted = dlg.show(gameModel,true);
        } else {
            accepted = dlg.show(gameModel,false);
        }
        if(accepted) {
            int gameIndex = dlg.table.getSelectionModel().getSelectedIndex();
            gameModel.currentPgnDatabaseIdx = gameIndex;
            Game g = gameModel.getPgnDatabase().loadGame(gameIndex);
            gameModel.setGame(g);
            g.setTreeWasChanged(true);
            g.setHeaderWasChanged(true);
            gameModel.triggerStateChange();
        }
    }

    public void handleOpenGame() {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        fileChooser.setTitle("Open PGN File");
        if(gameModel.lastOpenedDirPath != null && gameModel.lastOpenedDirPath.exists()) {
            fileChooser.setInitialDirectory(gameModel.lastOpenedDirPath);
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PGN", "*.pgn")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            if(file.getParentFile() != null) {
                gameModel.lastOpenedDirPath = file.getParentFile();
            }
            // for files >= 20 kb, always open in db window
            if((file.length() / 1024) < 20) {
                ArrayList<Long> indices = reader.scanPgn(file.getAbsolutePath());
                if(indices.size() == 0) {
                    return;
                }
                if(indices.size() == 1) {
                    Game g = null;
                    if(reader.isIsoLatin1(file.getAbsolutePath())) {
                        reader.setEncodingIsoLatin1();
                    } else {
                        reader.setEncodingUTF8();
                    }
                    OptimizedRandomAccessFile raf = null;
                    try {
                        raf = new OptimizedRandomAccessFile(file.getAbsolutePath(), "r");
                        g = reader.readGame(raf);
                        gameModel.currentPgnDatabaseIdx = 0;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if(raf!=null) {
                            try {
                                raf.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(g != null && (g.getRootNode().hasChild() || !g.getRootNode().getBoard().isInitialPosition())) {
                        gameModel.getPgnDatabase().filename = file.getAbsolutePath();
                        gameModel.openDatabaseOnNextDialog = true;
                        gameModel.setGame(g);
                        g.setHeaderWasChanged(true);
                        g.setTreeWasChanged(true);
                        gameModel.triggerStateChange();
                        return;
                    }
                } else {  // indices > 1, show db dialog
                    showDbDialog(file);
                }
            } else { // for larger files, we always assume there is more than one game
                // then show database dialog
                showDbDialog(file);
            }
        }
    }

    private void showDbDialog(File file) {
        DialogDatabase dlg = new DialogDatabase();
        gameModel.getPgnDatabase().filename = file.getAbsolutePath();
        //gameModel.getPgnDatabase().open(file.getAbsolutePath());
        boolean accepted = dlg.show(gameModel, true);
        if(accepted) {
            int gameIndex = dlg.table.getSelectionModel().getSelectedIndex();
            gameModel.currentPgnDatabaseIdx = gameIndex;
            Game g = gameModel.getPgnDatabase().loadGame(gameIndex);
            gameModel.setGame(g);
            g.setHeaderWasChanged(true);
            g.setTreeWasChanged(true);
            gameModel.triggerStateChange();
        }
    }

    public void handleSaveCurrentGame() {

        DialogSave dlg = new DialogSave();
        int result = dlg.show(gameModel.THEME,
                gameModel.currentPgnDatabaseIdx >= 0,
                gameModel.getPgnDatabase().filename);
        System.out.println("result of dlg: "+result);
        switch(result) {
            case DialogSave.DLG_SAVE_NEW:
                gameModel.getPgnDatabase().saveAsNewPGN(gameModel);
                break;
            case DialogSave.DLG_SAVE_APPEND_CURRENT:
                gameModel.getPgnDatabase().appendToCurrentPGN(gameModel.game);
                break;
            case DialogSave.DLG_SAVE_APPEND_OTHER:
                gameModel.getPgnDatabase().appendToOtherPGN(gameModel);
                break;
            case DialogSave.DLG_SAVE_REPLACE:
                gameModel.getPgnDatabase().replaceCurrentGame(gameModel.game, gameModel.currentPgnDatabaseIdx);
                break;
            default:
        }
    }

    public void handleSaveBoardPicture(Chessboard chessboard) {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        WritableImage image = chessboard.snapshot(new SnapshotParameters(), null);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                System.out.println(e.getStackTrace());
            }
        }
    }

    public void handlePrintGame(Stage owner) {

        PrinterJob job = PrinterJob.createPrinterJob();
        boolean ok = job.showPrintDialog(owner);

        if(ok) {
            PgnPrinter pgnPrinter = new PgnPrinter();
            String pgnGame = pgnPrinter.printGame(gameModel.getGame());
            TextFlow printArea = new TextFlow(new Text(pgnGame));
            boolean printed = job.printPage(printArea);
            if(printed) {
                job.endJob();
            }
        }
    }

    public void handleNextGame() {
        int nextIdx = gameModel.currentPgnDatabaseIdx + 1;
        if(nextIdx < gameModel.getPgnDatabase().getNrGames()) {
            Game g = gameModel.getPgnDatabase().loadGame(nextIdx);
            g.setTreeWasChanged(true);
            g.setHeaderWasChanged(true);
            gameModel.setGame(g);
            gameModel.currentPgnDatabaseIdx = nextIdx;
            gameModel.triggerStateChange();
        }
    }

    public void handlePrevGame() {
        int nextIdx = gameModel.currentPgnDatabaseIdx - 1;
        if(nextIdx >= 0) {
            Game g = gameModel.getPgnDatabase().loadGame(nextIdx);
            g.setTreeWasChanged(true);
            g.setHeaderWasChanged(true);
            gameModel.setGame(g);
            gameModel.currentPgnDatabaseIdx = nextIdx;
            gameModel.triggerStateChange();
        }
    }

    public void handlePrintPosition(Stage owner) {

        PrinterJob job = PrinterJob.createPrinterJob();
        boolean ok = job.showPrintDialog(owner);

        if(ok) {
            String fenCurrentPos = gameModel.getGame().getCurrentNode().getBoard().fen();
            TextFlow printArea = new TextFlow(new Text(fenCurrentPos));
            boolean printed = job.printPage(printArea);
            if(printed) {
                job.endJob();
            }
        }
    }

}

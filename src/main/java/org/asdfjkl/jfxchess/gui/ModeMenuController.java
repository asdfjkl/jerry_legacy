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

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import jfxtras.styles.jmetro.FlatAlert;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.asdfjkl.jfxchess.lib.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ModeMenuController implements StateChangeListener {

    final GameModel gameModel;
    final EngineOutputView engineOutputView;
    private final EngineController engineController;
    private int previousMultiPv;

    public ModeMenuController(GameModel gameModel, EngineOutputView engineOutputView) {

        this.gameModel = gameModel;
        engineController = new EngineController(this);
        this.engineOutputView = engineOutputView;
        previousMultiPv = gameModel.getMultiPv();
    }

    public void handleEngineInfo(String s) {

        if(s.startsWith("INFO")) {
            //"INFO |Stockfish 12 (Level MAX)|zobrist|145.081 kn/s||(#0)  23. Be7#||||"
            String[] sSplit = s.split("\\|");
            if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate() && sSplit.length > 1) {
                String sTemp = "|" + sSplit[1] + "||||(#0)|";
                this.engineOutputView.setText(sTemp);
            } else {
                this.engineOutputView.setText(s.substring(5));
            }
        }
        if(s.startsWith("BESTMOVE")) {
            handleBestMove(s);
        }
    }

    public void activateAnalysisMode() {
        engineController.restartEngine(gameModel.activeEngine);
        engineController.setUciLimitStrength(false);
        engineController.setMultiPV(gameModel.getMultiPv());
        gameModel.setMode(GameModel.MODE_ANALYSIS);
        gameModel.triggerStateChange();
    }

    public void activateEnterMovesMode() {
        engineController.stopEngine();
        gameModel.setMode(GameModel.MODE_ENTER_MOVES);
        gameModel.triggerStateChange();
    }

    private void handleStateChangeAnalysis() {

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        engineController.sendNewPosition(fen);
        engineController.uciGoInfinite();
    }

    private void handleStateChangeGameAnalysis() {

        boolean continueAnalysis = true;

        boolean parentIsRoot = (gameModel.getGame().getCurrentNode().getParent() == gameModel.getGame().getRootNode());
        if(!parentIsRoot) {
            // if the current position is in the opening book,
            // we stop the analysis
            long zobrist = gameModel.getGame().getCurrentNode().getBoard().getZobrist();
            if(gameModel.extBook.inBook(zobrist)) {
                gameModel.getGame().getCurrentNode().setComment("last book move");
                continueAnalysis = false;
            } else {
                // otherwise continue the analysis
                if (gameModel.getGameAnalysisJustStarted()) {
                    gameModel.setGameAnalysisJustStarted(false);
                } else {
                    gameModel.getGame().goToParent();
                }
                String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
                engineController.sendNewPosition(fen);
                engineController.uciGoMoveTime(gameModel.getGameAnalysisThinkTimeSecs() * 1000);
            }
        } else {
            continueAnalysis = false;
        }

        if(!continueAnalysis) {
            // we are at the root or found a book move
            gameModel.getGame().setTreeWasChanged(true);
            activateEnterMovesMode();
            //FlatAlert alert = new FlatAlert(Alert.AlertType.INFORMATION);

            FlatAlert alert = new FlatAlert(Alert.AlertType.INFORMATION);
            Scene scene = alert.getDialogPane().getScene();
            JMetro metro = new JMetro();
            if(gameModel.THEME == GameModel.STYLE_DARK) {
                metro.setStyle(Style.DARK);
            }
            metro.setScene(scene);
            alert.setHeaderText("Analysis Finished");
            alert.setContentText("The game analysis is finished.");
            alert.showAndWait();

            //DialogSimpleAlert dlg = new DialogSimpleAlert();
            //dlg.show("     The analysis is finished.     ", gameModel.THEME);
        }
    }

    public void activatePlayWhiteMode() {
        // restart engine and change gamestate.
        engineController.restartEngine(gameModel.activeEngine);
        if (gameModel.activeEngine.isInternal())
            engineController.setUciLimitStrength(true);
        gameModel.setMode(GameModel.MODE_PLAY_WHITE);
        gameModel.setFlipBoard(false);
        gameModel.setHumanPlayerColor(CONSTANTS.WHITE);
        gameModel.triggerStateChange();
    }

    public void activatePlayBlackMode() {
        // restart engine and change gamestate.
        engineController.restartEngine(gameModel.activeEngine);
        if (gameModel.activeEngine.isInternal())
            engineController.setUciLimitStrength(true);
        gameModel.setMode(GameModel.MODE_PLAY_BLACK);
        gameModel.setFlipBoard(true);
        gameModel.setHumanPlayerColor(CONSTANTS.BLACK);
        gameModel.triggerStateChange();
    }

    public void activatePlayoutPositionMode() {
        // first change gamestate and reset engine
        engineController.restartEngine(gameModel.activeEngine);
        engineController.setUciLimitStrength(false);    
        gameModel.setMode(GameModel.MODE_PLAYOUT_POSITION);
        gameModel.setFlipBoard(false);
        gameModel.triggerStateChange();
    }

    public void activateGameAnalysisMode() {

        gameModel.getGame().removeAllComments();
        gameModel.getGame().removeAllVariants();
        gameModel.getGame().removeAllAnnotations();
        gameModel.getGame().setTreeWasChanged(true);

        engineController.restartEngine(gameModel.activeEngine);
        engineController.setUciLimitStrength(false);
        engineController.setMultiPV(1);
        gameModel.setFlipBoard(false);
        gameModel.getGame().goToRoot();
        gameModel.getGame().goToLeaf();
        if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate()) {
            gameModel.currentIsMate = true;
            gameModel.currentMateInMoves = 0;
        }

        gameModel.setGameAnalysisJustStarted(true);
        gameModel.triggerStateChange();

    }

    public void handleStateChangePlayWhiteOrBlack() {

        // first check if we can apply a bookmove
        long zobrist = gameModel.getGame().getCurrentNode().getBoard().getZobrist();
        //ArrayList<String> uciMoves0 = gameModel.book.findMoves(zobrist);
        boolean maxDepthReached = false;
        int currentDepth = gameModel.getGame().getCurrentNode().getDepth();
        int currentElo = gameModel.activeEngine.getUciElo();
        // engine supports setting ELO
        // let's limit book knowledge according to ELO
        if(currentElo > 0) {
            if(currentElo <= 1200 && currentDepth > 10) {
                maxDepthReached = true;
            }
            if(currentElo > 1200 && currentElo < 1400 && currentDepth > 10) {
                maxDepthReached = true;
            }
            if(currentElo > 1400 && currentElo < 1600 && currentDepth > 12) {
                maxDepthReached = true;
            }
            if(currentElo > 1600 && currentElo < 1800 && currentDepth > 14) {
                maxDepthReached = true;
            }
            if(currentElo > 1800 && currentElo < 2000 && currentDepth > 16) {
                maxDepthReached = true;
            }
            if(currentElo > 2000 && currentElo < 2200 && currentDepth > 18) {
                maxDepthReached = true;
            }
        }
        String bookMove = gameModel.extBook.getRandomMove(zobrist);
        if((bookMove != null) && (!maxDepthReached)) {
            handleBestMove("BESTMOVE|"+bookMove+"|"+zobrist);
        } else {
            String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
            engineController.sendNewPosition(fen);
            engineController.uciGoMoveTime(gameModel.getComputerThinkTimeSecs()*1000);
        }
    }

    public void handleStateChangePlayoutPosition() {

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        engineController.sendNewPosition(fen);
        engineController.uciGoMoveTime(gameModel.getComputerThinkTimeSecs()*1000);
    }

    private void addBestPv(String[] uciMoves) {
        //String[] uciMoves = gameModel.currentBestPv.split(" ");
        GameNode currentNode = gameModel.getGame().getCurrentNode();

        for (String uciMove : uciMoves) {
            try {
                GameNode next = new GameNode();
                Board board = currentNode.getBoard().makeCopy();
                Move m = new Move(uciMove);
                if(!board.isLegal(m)) {
                    break;
                }
                board.apply(m);
                next.setMove(m);
                next.setBoard(board);
                next.setParent(currentNode);
                // to avoid bugs when incoherent information is
                // given/received by the engine, do not add lines that already exist
                if (currentNode.getVariations().size() > 0) {
                    String mUciChild0 = currentNode.getVariation(0).getMove().getUci();
                    if (mUciChild0.equals(uciMove)) {
                        break;
                    }
                }
                currentNode.addVariation(next);
                currentNode = next;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }


    public void editEngines() {
        // The following call stops the engine-process, set the ENTER_MOVES_MODE
        // and calls GameModel.triggerChangeState(). Previously the engine was
        // not stopped here.
        activateEnterMovesMode();
        DialogEngines dlg = new DialogEngines();
        ArrayList<Engine> enginesCopy = new ArrayList<>();
        for(Engine engine : gameModel.engines) {
            enginesCopy.add(engine);
        }
        int selectedIdx = gameModel.engines.indexOf(gameModel.activeEngine);
        if(selectedIdx < 0) {
            selectedIdx = 0;
        }
        boolean accepted = dlg.show(enginesCopy, selectedIdx, gameModel.THEME);
        if(accepted) {
            ArrayList<Engine> engineList = new ArrayList<>(dlg.engineList);
            Engine selectedEngine = dlg.engineList.get(dlg.selectedIndex);
            // Change the engine-name in the bottom panel immediately on OK
            // being pressed. Previously it didn't change until we started
            // playing.
            engineOutputView.setEngineId(selectedEngine.getName());
            gameModel.engines = engineList;
            gameModel.activeEngine = selectedEngine;
            // reset pv line to 1 for new engine
            gameModel.setMultiPv(1);
            gameModel.setMultiPvChange(true);
            gameModel.triggerStateChange();
        }
    }

    public void handleBestMove(String bestmove) {
        //System.out.println("handling bestmove: "+bestmove);
        int mode = gameModel.getMode();

        if(mode == GameModel.MODE_ENTER_MOVES) {
            return;
        }

        String[] bestmoveItems = bestmove.split("\\|");

        String zobristString = bestmoveItems[bestmoveItems.length-1];
        long zobrist = Long.parseLong(zobristString);

        if(zobrist != gameModel.getGame().getCurrentNode().getBoard().getZobrist()) {
            // if this bestmove is for a different position than the current,
            // it is a relict from thread/gui synchronisation mismatch; we just dismiss it
            return;
        }

        if (mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK  || mode == GameModel.MODE_PLAYOUT_POSITION) {

            // todo: catch Exceptions!
            String uci = bestmoveItems[1].split(" ")[0];
            Move m = new Move(uci);
            Board b = gameModel.getGame().getCurrentNode().getBoard();
            if (b.isLegal(m)) {
                if(mode == GameModel.MODE_PLAY_WHITE && b.turn == CONSTANTS.BLACK) {
                    gameModel.getGame().applyMove(m);
                    gameModel.triggerStateChange();
                }
                if(mode == GameModel.MODE_PLAY_BLACK && b.turn == CONSTANTS.WHITE) {
                    gameModel.getGame().applyMove(m);
                    gameModel.triggerStateChange();
                }
                if(mode == GameModel.MODE_PLAYOUT_POSITION) {
                    gameModel.getGame().applyMove(m);
                    gameModel.triggerStateChange();
                }
            }
        }

        if(mode == GameModel.MODE_GAME_ANALYSIS) {
            //gameModel.getGame().getCurrentNode().setComment(bestmove);

            // first update information for current node
            gameModel.childBestPv = gameModel.currentBestPv;
            gameModel.childBestEval = gameModel.currentBestEval;
            gameModel.childIsMate = gameModel.currentIsMate;
            gameModel.childMateInMoves = gameModel.currentMateInMoves;

            gameModel.currentBestPv = bestmoveItems[3];
            gameModel.currentBestEval = Integer.parseInt(bestmoveItems[2]);
            gameModel.currentIsMate = bestmoveItems[4].equals("true");
            gameModel.currentMateInMoves = Integer.parseInt(bestmoveItems[5]);

            // double check, since some engines misreport / report not quickly enough
            if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate()){
                gameModel.currentIsMate = true;
            }


            // ignore leafs (game ended here)
            if(!gameModel.getGame().getCurrentNode().isLeaf()) {
                //System.out.println("handling bestmove: "+bestmove);

                // completely skip analysis for black or white, if
                // that option was chosen
                boolean turn = gameModel.getGame().getCurrentNode().getBoard().turn;
                if ((gameModel.getGameAnalysisForPlayer() == GameModel.BOTH_PLAYERS)
                        || (gameModel.getGameAnalysisForPlayer() == CONSTANTS.IWHITE && turn == CONSTANTS.WHITE)
                        || (gameModel.getGameAnalysisForPlayer() == CONSTANTS.IBLACK && turn == CONSTANTS.BLACK)) {

                    int centiPawnThreshold = (int) (gameModel.getGameAnalysisThreshold() * 100.0);
                    //System.out.println(centiPawnThreshold);
                    // first case if there was simply a better move; i.e. no checkmate overseen or
                    // moved into a checkmate
                    if (!gameModel.currentIsMate && !gameModel.childIsMate) {
                        boolean wMistake = turn == CONSTANTS.WHITE && ((gameModel.currentBestEval - gameModel.childBestEval) >= centiPawnThreshold);
                        boolean bMistake = turn == CONSTANTS.BLACK && ((gameModel.currentBestEval - gameModel.childBestEval) <= -(centiPawnThreshold));

                        //System.out.println("threshold: "+gameModel.getGameAnalysisThreshold());
                        //System.out.println("mistake  : " + (gameModel.currentBestEval - gameModel.childBestEval));

                        if (wMistake || bMistake) {
                            String uci = bestmoveItems[1].split(" ")[0];
                            String nextMove = gameModel.getGame().getCurrentNode().getVariation(0).getMove().getUci();
                            String[] pvMoves = gameModel.currentBestPv.split(" ");
                            // if the bestmove returned by the engine is different
                            // from the best suggested pv line, it means that e.g. the
                            // engine took a book move, but did not give a pv evaluation
                            // or there was some major async error between engine and gui
                            // in such a case, do not add the best pv line, as it is probably
                            // not a valid pv line for the current node
                            // we also do not want to add the same line as the child
                            if (!uci.equals(nextMove) && pvMoves.length > 0 && pvMoves[0].equals(uci)) {

                                addBestPv(pvMoves);

                                NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                                DecimalFormat decim = (DecimalFormat) nf;
                                decim.applyPattern("0.00");
                                String sCurrentBest = decim.format(gameModel.currentBestEval / 100.0);
                                String sChildBest = decim.format(gameModel.childBestEval / 100.0);

                                ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                                if (vars != null && vars.size() > 1) {
                                    GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                                    child0.setComment(sChildBest);
                                    GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                                    child1.setComment(sCurrentBest);
                                }
                            }
                        }
                    }

                    if (gameModel.currentIsMate && !gameModel.childIsMate) {
                        // the current player missed a mate
                        String[] pvMoves = gameModel.currentBestPv.split(" ");
                        addBestPv(pvMoves);

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        DecimalFormat decim = (DecimalFormat) nf;
                        decim.applyPattern("0.00");
                        String sChildBest = decim.format(gameModel.childBestEval / 100.0);

                        String sCurrentBest = "";
                        if (turn == CONSTANTS.WHITE) {
                            sCurrentBest = "#" + (Math.abs(gameModel.currentMateInMoves));
                        } else {
                            sCurrentBest = "#-" + (Math.abs(gameModel.currentMateInMoves));
                        }

                        ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (!gameModel.currentIsMate && gameModel.childIsMate) {
                        // the current player  moved into a mate
                        String[] pvMoves = gameModel.currentBestPv.split(" ");
                        addBestPv(pvMoves);

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        DecimalFormat decim = (DecimalFormat) nf;
                        decim.applyPattern("0.00");
                        String sCurrentBest = decim.format(gameModel.currentBestEval / 100.0);

                        String sChildBest = "";
                        if (turn == CONSTANTS.WHITE) {
                            sChildBest = "#-" + (Math.abs(gameModel.childMateInMoves));
                        } else {
                            sChildBest = "#" + (Math.abs(gameModel.childMateInMoves));
                        }

                        ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (gameModel.currentIsMate && gameModel.childIsMate) {
                        // the current player had a mate, but instead of executing it, he moved into a mate,
                        // but we also want to skip the situation where the board position is checkmate
                        if ((gameModel.currentMateInMoves >= 0 && gameModel.childMateInMoves >= 0) &&
                                gameModel.childMateInMoves != 0) {

                            String[] pvMoves = gameModel.currentBestPv.split(" ");
                            addBestPv(pvMoves);

                            String sCurrentBest = "";
                            String sChildBest = "";
                            if (turn == CONSTANTS.WHITE) {
                                sCurrentBest = "#" + (Math.abs(gameModel.currentMateInMoves));
                                sChildBest = "#-" + (Math.abs(gameModel.childMateInMoves));
                            } else {
                                sCurrentBest = "#-" + (Math.abs(gameModel.currentMateInMoves));
                                sChildBest = "#" + (Math.abs(gameModel.childMateInMoves));
                            }

                            ArrayList<GameNode> vars = gameModel.getGame().getCurrentNode().getVariations();
                            if (vars != null && vars.size() > 1) {
                                GameNode child0 = gameModel.getGame().getCurrentNode().getVariation(0);
                                child0.setComment(sChildBest);
                                GameNode child1 = gameModel.getGame().getCurrentNode().getVariation(1);
                                child1.setComment(sCurrentBest);
                            }
                        }
                    }
                }
            }
            gameModel.getGame().setTreeWasChanged(true);
            gameModel.triggerStateChange();
        }

    }

    @Override
    public void stateChange() {
        int mode = gameModel.getMode();
        Board board = gameModel.getGame().getCurrentNode().getBoard();
        boolean turn = board.turn;

        boolean isCheckmate = board.isCheckmate();
        boolean isStalemate = board.isStalemate();
        boolean isThreefoldRepetition = gameModel.getGame().isThreefoldRepetition();
        boolean isInsufficientMaterial = gameModel.getGame().isInsufficientMaterial();

        boolean abort = false;

        if(gameModel.getMultiPv() != previousMultiPv) {
            engineController.setMultiPV(gameModel.getMultiPv());
            previousMultiPv = gameModel.getMultiPv();
        }

        // if we change from e.g. play white to enter moves, the state change would trigger
        // the notification again in enter moves mode after the state change. thus,
        // also check if
        if((isCheckmate || isStalemate || isThreefoldRepetition || isInsufficientMaterial)
                && !gameModel.doNotNotifyAboutResult) {
            String message = "";
            if(isCheckmate) {
                message = "     Checkmate.     ";
            }
            if(isStalemate) {
                message = "     Stalemate.     ";
            }
            if(isThreefoldRepetition) {
                message = "Draw (Threefold Repetition)";
            }
            if(isInsufficientMaterial) {
                message = "Draw (Insufficient material for checkmate)";
            }
            if(mode != GameModel.MODE_GAME_ANALYSIS) {
                FlatAlert alert = new FlatAlert(Alert.AlertType.CONFIRMATION);
                Scene scene = alert.getDialogPane().getScene();
                JMetro metro = new JMetro();
                if(gameModel.THEME == GameModel.STYLE_DARK) {
                    metro.setStyle(Style.DARK);
                }
                metro.setScene(scene);
                //alert.setTitle("Delete Game");
                alert.setHeaderText("");
                alert.setContentText(message);
                alert.showAndWait();

                //DialogSimpleAlert dlg = new DialogSimpleAlert();
                //dlg.show(message, gameModel.THEME);
            }

            if(mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK || mode == GameModel.MODE_PLAYOUT_POSITION) {
                abort = true;
            }
        }

        if(abort) {
            // if we change from e.g. play white to enter moves, the state change would trigger
            // the notification again in enter moves mode after the state change. thus
            // set a marker here, that for the next state change we ignore
            // the result notification
            gameModel.doNotNotifyAboutResult = true;
            gameModel.setMode(GameModel.MODE_ENTER_MOVES);
            gameModel.triggerStateChange();
        } else {
            gameModel.doNotNotifyAboutResult = false;
            if (mode == GameModel.MODE_ANALYSIS) {
                handleStateChangeAnalysis();
            }
            if (mode == GameModel.MODE_GAME_ANALYSIS) {
                handleStateChangeGameAnalysis();
            }
            if (mode == GameModel.MODE_PLAYOUT_POSITION) {
                handleStateChangePlayoutPosition();
            }
            if ((mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK)
                    && turn != gameModel.getHumanPlayerColor()) {
                handleStateChangePlayWhiteOrBlack();
            }
        }
    }

    // Just a few wrapper-methods, so I could remove Enginecontroller
    // completely from App.java.
    public void stopEngine() {
        engineController.stopEngine();
    }
    
    public void engineSetOptionMultiPV(int value) {
        engineController.setMultiPV(value);     
    }
}

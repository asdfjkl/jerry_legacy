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

import com.sun.webkit.Timer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class EngineController {

    EngineThread engineThread;
    final BlockingQueue<String> cmdQueue = new LinkedBlockingQueue<String>();;
    ModeMenuController modeMenuController = null;

    public EngineController(ModeMenuController modeMenuController) {

        this.modeMenuController = modeMenuController;

    }

    private void startEngineThread() {

        final AtomicReference<String> count = new AtomicReference<>();
        //cmdQueue = new LinkedBlockingQueue<String>();
        engineThread = new EngineThread(cmdQueue);
        engineThread.stringProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> observable,
                                final String oldValue, final String newValue) {
                if (count.getAndSet(newValue) == null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            String value = count.getAndSet(null);
                            modeMenuController.handleEngineInfo(value);
                        }
                    });
                }
            }
        });

        engineThread.start();
    }

    public void testStartAndGoInf() {

        //engineThread.start();
        String cmdStockfish = "C:\\Program Files (x86)\\Jerry_Chess\\engine\\stockfish.exe";
        String goInf = "go infinite";

        try {
            cmdQueue.put("start "+cmdStockfish);
            cmdQueue.put(goInf);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void sendCommand(String cmd) {
        try {
            if(cmd.startsWith("quit")) {
                cmdQueue.put(cmd);
                if(engineThread != null) {
                    Thread.sleep(100);
                    engineThread.terminate();
                    engineThread.join();
                    engineThread = null;
                }
            } else {
                if(cmd.startsWith("start ")) {
                    //System.out.println("start received, restarting engine");
                    if(engineThread == null) {
                        //System.out.println("engine thread was null, restarting");
                        startEngineThread();
                        Thread.sleep(100);
                        cmdQueue.put(cmd);
                    }
                } else {
                    cmdQueue.put(cmd);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setStockfishStrength(int level) {
        try {
            cmdQueue.put("setoption name Skill Level value "+level);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void uciGoMoveTime(int milliseconds) {
        try {
            cmdQueue.put("go movetime "+milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void restartGame(Boolean setUciLimitStrength, GameModel gameModel) {
        if(gameModel.activeEngine.supportsUciLimitStrength()) {
            gameModel.activeEngine.setUciLimitStrength(setUciLimitStrength);
        }
        sendCommand("stop");
        sendCommand("quit");
        String cmdEngine = gameModel.activeEngine.getPath();
        sendCommand("start "+cmdEngine);
        sendCommand("uci");
        sendCommand("ucinewgame");
    }

    public void activateEnterMovesMode(GameModel gameModel) {
        sendCommand("stop");
        sendCommand("quit");
        gameModel.setMode(GameModel.MODE_ENTER_MOVES);
        gameModel.triggerStateChange();
    }

    void handleStateChangeAnalysis(GameModel gameModel) {

        String fen = gameModel.getGame().getCurrentNode().getBoard().fen();
        sendCommand("stop");
        sendCommand("position fen "+fen);
        sendCommand("go infinite");

    }

    void resetEngine(GameModel gameModel) {
        restartGame(false, gameModel);
        for(EngineOption enOpt : gameModel.activeEngine.options) {
            if(enOpt.isNotDefault()) {
                sendCommand(enOpt.toUciCommand());
            }
        }
    }


}

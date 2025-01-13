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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

public class EngineThread extends Thread {

    static final Pattern REG_MOVES = Pattern.compile("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})");
    static final Pattern REG_BESTMOVE = Pattern.compile("bestmove\\s([a-z]\\d[a-z]\\d[a-z]{0,1})");
    static final Pattern REG_STRENGTH = Pattern.compile("UCI_Elo value \\d+");

    private final StringProperty stringProperty;

    private final BlockingQueue<String> cmdQueue;
    Process engineProcess;
    BufferedReader engineOutput;
    BufferedWriter engineInput;
    private volatile boolean running = true;
    private long lastInfoUpdate = 0;
    private long lastBestmoveUpdate = 0;

    private final EngineInfo engineInfo;

    private boolean readyok = false;
    private boolean uciok = false;

    public EngineThread(BlockingQueue<String> cmdQueue) {
        this.engineInfo = new EngineInfo();
        this.cmdQueue = cmdQueue;
        stringProperty = new SimpleStringProperty(this, "String", "");
        lastInfoUpdate = System.currentTimeMillis();
        lastBestmoveUpdate = System.currentTimeMillis();
        setDaemon(true);
    }

    public synchronized boolean engineIsOn() {
        return (engineProcess != null && engineProcess.isAlive());
    }
        
    // bestmove result comes from the engine via this method.
    public String getString() {
        return stringProperty.get();
    }

    public StringProperty stringProperty() {
        return stringProperty;
    }

    @Override
    public void run() {
        while (running) {
            // Set the thread to loop at about 1000 times per second.
            // It Keeps CPU-load down and is probably more than enough.
        	try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (this.isInterrupted()) {
                // here: delete process if it exists
                if(engineIsOn()) {
                    try {
                        // Try to close down the engine the normal way.
                        engineInput.write("stop\n");
                        engineInput.flush();
                        engineInput.write("quit\n");
                        engineInput.flush();
                        boolean finished = engineProcess.waitFor(500, TimeUnit.MILLISECONDS);
                        if(!finished) {
                            engineProcess.destroy();
                        }
                    } catch(IOException | InterruptedException e) {
                    e.printStackTrace(System.out);
                    }
                }
                // Stop this thread.
                running = false;
                continue;
            }
            // Process engine output
            if (engineOutput != null) {
                int linesRead = 0;
                try {
                    while (engineOutput.ready() && linesRead < 100) {
                        String line = engineOutput.readLine();
                        if(line.contains("readyok")) {
                            readyok = true;
                            continue;
                        }
                        if(line.contains("uciok")) {
                            uciok = true;
                            continue;
                        }
                        if (!line.isEmpty()) {
                            //System.out.println(line);
                            //lastString = line;
                            // todo: instead of directly setting bestmove,
                            // try updating engine info
                            if(line.startsWith("bestmove")) {
                                engineInfo.bestmove = "BESTMOVE|"
                                        + line.substring(9)
                                        +"|"+engineInfo.score.get(0)
                                        +"|"+String.join(" ", engineInfo.pvList)
                                        +"|"+engineInfo.seesMate.get(0)
                                        +"|"+engineInfo.mate.get(0)
                                        +"|"+engineInfo.zobrist;
                                linesRead++;
                                continue;
                            }
                            // Update engine info with other ouput-lines
                            engineInfo.update(line);
                        }
                        linesRead++;
                    }
                } catch (IOException e) {
                }
            }
            // send update
            long currentMs = System.currentTimeMillis();
            if((currentMs - lastInfoUpdate) > 100) {
                stringProperty.set("INFO " + engineInfo.toString());
                lastInfoUpdate = currentMs;
            }
            // we need to constantly send "bestmove". If we only send it once,
            // and the user keeps flooding the GUI with events, i.e. by frequently resizing
            // the window or other inputs, the GUI might skip to handle (the only one)
            // bestmove info. Instead, the GUI will receive bestmove frequently
            // but ignore the info, if already processed.
            if((currentMs - lastBestmoveUpdate) > 800) {
                stringProperty.set(engineInfo.bestmove);
                lastBestmoveUpdate = currentMs;
            }
            if (!engineIsOn()) { 
                // engine not running
                if(!cmdQueue.isEmpty()) {
                    try {
                        // Here we dispose of (or consume) the next command
                        // sent to a dead engine, or start a new engine process
                        // if we find a start command.
                        // This makes it OK for the engineController to
                        // always send stop and quit first, when restarting
                        // an engine, without first checking if the engine is on. 
                        String cmd = (String)cmdQueue.take();
                        if (cmd.startsWith("start")) {
                            // reset engine info if we start
                            engineInfo.clear();
                            String engineCmd = cmd.substring(6);
                            try {
                                this.engineProcess = new ProcessBuilder(engineCmd).start();
                                this.engineInput = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
                                this.engineOutput = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
                                // Reset some "state" variables.
                                readyok = false;
                                uciok = false;
                            } catch (IOException e) {
                                e.printStackTrace(System.out);
                            }
                            this.engineInfo.strength = -1;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.out);
                    }
                }
                continue;
            }
            // When we have come this far in the while-loop
            // we know that the process is alive -> engine is running.
            // The commands uci, quit, setoption and isready are
            // treated in special ways. We are not expecting any
            // other commands from the engine controller until isready
            // has been sent at least once and we have received readyok
            // from the engine.
            if(!cmdQueue.isEmpty()) {
                // Don't remove from queue until we know which command it is.
                // It could be some other command just waiting for us to
                // pass the readyok check below, now or in the next loop, maybe.
                String cmd = (String) cmdQueue.peek();
                if(cmd == null) {
                    // What to do here? 
                    // Better luck next loop!
                    continue;
                }
                
                // I noticed that even after stop, quit and waiting for
                // the process to die, the process.isAlive() method took time
                // before it answered false. So when restarting, the start
                // command was being sent to the dead engine as a normal
                // command below. The following statement prevents that. 
                if(cmd.startsWith("start")) {
                    continue;
                }
                
                // The command uci must be sent immediately after startup.
                // Some engines will not report readyok on isready directly
                // after startup (like e.g. arasan). thus we require of the
                // engine controller to always send 'uci' after starting an
                // engine process by the start command.
                if(cmd.equals("uci")) {
                    try {
                        cmdQueue.take();
                        engineInput.write(cmd + "\n");
                        engineInput.flush();
                        // Set the uciok flag to false.
                        // This thread won't send any other commands
                        // until uciok has been received.
                        uciok = false;
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace(System.out);
                    }
                    continue;
                }
                
                if(!uciok) {
                    // Go no further until we have received uciok.
                    continue;
                }
                
                // When we have reached this point in the while-loop
                // we know that the engine is ready to receive other 
                // commands than uci. We could always be ready to send
                // the quit command if it appears here, (even before isready).
                if(cmd.equals("quit")) {
                    try {
                        // reset engine info if we quit
                        engineInfo.clear();
                        cmdQueue.take();
                        engineInput.write(cmd + "\n");
                        engineInput.flush();
                        // and wait for engine process to die.
                        boolean finished = engineProcess.waitFor(500, TimeUnit.MILLISECONDS);
                        if(!finished) {
                            engineProcess.destroy();
                            // System.out.println("Engine has been destroyed."); 
                        }
                        // else
                        //    System.out.println("Engine has died a natural death.");
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace(System.out);
                    }
                    continue;
                }

                // We can (and maybe should, according to the UCI-protocol),
                // be ready to send the setoption commands directly after uciok
                // has been received.
                if(cmd.startsWith("setoption")) {
                    try {
                        // Some special actions to do before sending:
                        if(cmd.startsWith("setoption name Skill Level")) {
                            Matcher matchExpressionStrength = REG_STRENGTH.matcher(cmd);
                            if(matchExpressionStrength.find()) {
                                engineInfo.strength = Integer.parseInt(matchExpressionStrength.group().substring(18));
                            }
                        }
                        if(cmd.startsWith("setoption name MultiPV value")) {
                            engineInfo.nrPvLines = Integer.parseInt(cmd.substring(29,30));
                        }
                        cmdQueue.take();
                        engineInput.write(cmd + "\n");
                        engineInput.flush();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace(System.out);
                    }
                    continue;                        
                }
                
                if(cmd.equals("isready")) {
                    try {
                        // We wish to be able to send isready more than
                        // once during the lifetime of an engineprocess,
                        // so the next line is important.
                        readyok = false;
                        cmdQueue.take();
                        engineInput.write(cmd + "\n");
                        engineInput.flush();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace(System.out);
                    }
                    continue;
                }
                
                if(!readyok) {
                    // Wait for readyok before proceeding to
                    // the sending of other commands below.
                    continue;
                }

                try {
                    cmd = (String) cmdQueue.take();
                    // if the command is "position fen moves", first count the
                    // numbers of moves so far to generate move numbers in engine info
                    // todo: needed?
                    if(cmd.startsWith("position fen")) {
                        Matcher matchMoves = REG_MOVES.matcher(cmd);
                        int cnt = 0;
                        while(matchMoves.find()) {
                            cnt++;
                        }
                        if(cnt > 0) {
                            engineInfo.halfmoves = cnt;
                        }
                        String fen = cmd.substring(13);
                        engineInfo.setFen(fen);
                    }
                   
                    // All other commands can be sent as they are,
                    // without any action.
                    try {
                        this.engineInput.write(cmd + "\n");
                        this.engineInput.flush();
                    } catch (IOException e) {
                        e.printStackTrace(System.out);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }


    public void terminate() {
        running = false;
    }

}

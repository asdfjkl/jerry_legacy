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

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import org.asdfjkl.jerryfx.lib.Board;
import java.awt.*;

import static org.asdfjkl.jerryfx.lib.CONSTANTS.EMPTY;
import static org.asdfjkl.jerryfx.lib.CONSTANTS.FRINGE;

public class ShowAppearanceBoard extends CanvasProperties {

        final BoardStyle boardStyle;
        final double outputScaleX;
        GameModel gameModel;
        final boolean flipBoard = true;

        int innerXOffset;
        int innerYOffset;
        int squareSize;

        final PieceImageProvider pieceImageProvider;

        Point moveSource;
        GrabbedPiece grabbedPiece = new GrabbedPiece();
        final boolean drawGrabbedPiece = false;
        final Board board;

    public ShowAppearanceBoard() {

            this.boardStyle = new BoardStyle();
            this.pieceImageProvider = PieceImageProvider.getInstance();
            this.outputScaleX = Screen.getPrimary().getOutputScaleX();
            this.board = new Board(true);

        }

    @Override
    public void updateCanvas() {

            GraphicsContext gc = this.getGraphicsContext2D();

            // fill background
            gc.beginPath();
            //gc.setFill(Color.rgb(152, 152, 152));
            //gc.setFill(boardStyle.getLightSquareColor());
            gc.setFill(boardStyle.getDarkSquareColor());
            gc.rect(0, 0, this.getWidth(), this.getHeight());
            gc.fill();

            // size of real board incl. corner
            double height = this.getHeight();
            double width = this.getWidth();
            double minWidthHeight = Math.min(height, width);

            // spare 2 percent left and right
            int outerMargin = (int) (minWidthHeight * 0.05);
            int boardSize = (int) (minWidthHeight - (2*outerMargin));

            int xOffset = outerMargin;
            if(width > height) {
                int surplus = (int) (width - height);
                xOffset += surplus/2;
            }

            int borderMargin = 18; // (int) (minWidthHeight * 0.03);
            squareSize = ((boardSize - (2* borderMargin)) / 8);
            innerXOffset = (xOffset + borderMargin);
            innerYOffset = (outerMargin + borderMargin);

            // paint board inc. margin with letters & numbers
            gc.beginPath();
            gc.setFill(boardStyle.getBorderColor());
            gc.rect(xOffset, outerMargin, (squareSize*8)+(borderMargin*2), (squareSize*8)+(borderMargin*2));
            gc.fill();

            // paint squares
            Color fieldColor;
            for(int i=0;i<8;i++) {
                for(int j=0;j<8;j++) {
                    if((j%2 == 0 && i%2==1) || (j%2 == 1 && i%2==0)) {
                        fieldColor = boardStyle.getLightSquareColor();
                    } else {
                        fieldColor = boardStyle.getDarkSquareColor();
                    }
                    int x = (innerXOffset) + (i*squareSize);
                    if(flipBoard) {
                        x = innerXOffset+((7-i)*squareSize);
                    }
                    int y = (innerYOffset) + ((7-j)*squareSize);

                    gc.beginPath();
                    gc.setFill(fieldColor);
                    gc.rect(x,y,squareSize,squareSize);
                    gc.fill();
                }
            }

            // draw the board coordinates
            gc.setFill(boardStyle.getCoordinateColor());
            for(int i=0;i<8;i++) {
                if(flipBoard){
                    char ch = (char) (65 + (7 - i));
                    String idx = Character.toString(ch);
                    String num = Integer.toString(i + 1);
                    gc.beginPath();
                    gc.fillText(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                            innerYOffset + (8 * squareSize) + (borderMargin * 0.8));
                    gc.fillText(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
                } else{
                    char ch = (char) (65 + i);
                    String idx = Character.toString(ch);
                    String num = Integer.toString(8 - i);
                    gc.beginPath();
                    gc.fillText(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                            innerYOffset + (8 * squareSize) + (borderMargin * 0.8));
                    gc.fillText(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
                }
            }

            // draw pieces
            for(int i=0;i<8;i++) {
                for (int j = 0; j < 8; j++) {
                    int x;
                    if(flipBoard) {
                        x = innerXOffset+((7-i)*squareSize);
                    } else {
                        x = innerXOffset+(i*squareSize);
                    }
                    // drawing coordinates are from top left
                    // whereas chess coords are from bottom left
                    int y = innerYOffset+((7-j)*squareSize);
                    int piece = 0;
                    if(flipBoard) {
                        piece = board.getPieceAt(i, 7-j);
                    } else {
                        piece = board.getPieceAt(i, j);
                    }
                    if(piece != EMPTY && piece != FRINGE) {
                        if(!flipBoard) {
                            if (!(drawGrabbedPiece && i == moveSource.x && j == moveSource.y)) {
                                javafx.scene.image.Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                        boardStyle.getPieceStyle());
                                gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                            }
                        } else {
                            if (!(drawGrabbedPiece && i == moveSource.x && (7-j) == moveSource.y)) {
                                javafx.scene.image.Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                        boardStyle.getPieceStyle());
                                gc.drawImage(pieceImage, x, y, squareSize, squareSize);
                            }
                        }
                    }
                }
            }
        }

}

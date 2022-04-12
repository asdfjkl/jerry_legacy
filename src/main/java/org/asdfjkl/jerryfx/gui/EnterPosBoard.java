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
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import java.awt.*;
import java.util.ArrayList;

import static java.lang.Math.min;
import static org.asdfjkl.jerryfx.lib.CONSTANTS.EMPTY;
import static org.asdfjkl.jerryfx.lib.CONSTANTS.FRINGE;

public class EnterPosBoard extends CanvasProperties {

    private final ArrayList<EnterPosBoardListener> enterPosBoardListeners = new ArrayList<>();

    BoardStyle boardStyle;
    final double outputScaleX;
    Board board;
    boolean flipBoard = false;

    int innerXOffset;
    int innerYOffset;
    int squareSize;
    int xOffset;
    final int borderMargin = 18;
    int outerMargin;

    final PieceImageProvider pieceImageProvider;

    Point moveSource;
    final GrabbedPiece grabbedPiece = new GrabbedPiece();
    final boolean drawGrabbedPiece = false;

        final int[][] pickupPieces = {
                { CONSTANTS.WHITE_PAWN, CONSTANTS.BLACK_PAWN },
                { CONSTANTS.WHITE_KNIGHT, CONSTANTS.BLACK_KNIGHT},
                { CONSTANTS.WHITE_BISHOP, CONSTANTS.BLACK_BISHOP},
                { CONSTANTS.WHITE_ROOK, CONSTANTS.BLACK_ROOK},
                { CONSTANTS.WHITE_QUEEN, CONSTANTS.BLACK_QUEEN},
                { CONSTANTS.WHITE_KING, CONSTANTS.BLACK_KING}
        };

    int selectedPiece = CONSTANTS.WHITE_PAWN;



    public EnterPosBoard(Board board) {

        this.boardStyle = new BoardStyle();
        this.board = board;
        this.pieceImageProvider = PieceImageProvider.getInstance();
        this.outputScaleX = Screen.getPrimary().getOutputScaleX();
        this.grabbedPiece.setPiece(-1);

        setOnMousePressed(event -> {
            handleMousePress(event);
        });

    }


    @Override
    public double minWidth(double height) {
        return Math.max(this.getHeight() * 1.5,150D);
    }

    @Override
    public double minHeight(double width) {
        return 80D;
    }


    boolean clickedOnBoard(double x, double y) {

        if(x > innerXOffset && y > innerYOffset &&
                x < innerXOffset + (8*squareSize) &&
                y < innerYOffset + (8*squareSize)) {
            return true;
        } else {
            return false;
        }
    }

    boolean clickedOnPieceSelector(double x, double y) {
        int leftOffset = xOffset + 9*squareSize + 3*borderMargin;
        int downOffset = outerMargin + borderMargin;
        if(x > leftOffset && y > downOffset &&
                x < leftOffset + 2*squareSize &&
                y < downOffset + 6*squareSize) {
            return true;
        } else {
            return false;
        }
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
        double minWidthHeight = min(height, width);

        // spare 2 percent left and right
        outerMargin = (int) (minWidthHeight * 0.05);
        int boardSize = (int) (minWidthHeight - (2*outerMargin));

        xOffset = outerMargin;

        squareSize = ((boardSize - (2* borderMargin)) / 8);

        if(width > height) {
            int widthBoardincPieceSel = (((boardSize - (4* borderMargin)) / 8))*12;
            int surplus = (int) (widthBoardincPieceSel - height);
            xOffset += (surplus/2)+1;
        }

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
                    if(!flipBoard) {
                        fieldColor = boardStyle.getLightSquareColor();
                    } else {
                        fieldColor = boardStyle.getDarkSquareColor();
                    }
                } else {
                    if(!flipBoard) {
                        fieldColor = boardStyle.getDarkSquareColor();
                    } else {
                        fieldColor = boardStyle.getLightSquareColor();
                    }
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

        // draw rect for piece selection. reset to border color
        gc.beginPath();
        gc.setFill(boardStyle.getBorderColor());
        gc.rect((xOffset + 9*squareSize) + (borderMargin*2),
                outerMargin,
                (squareSize*2)+(borderMargin*2),
                (squareSize*6)+(borderMargin*2));
        gc.fill();

        for(int i=0;i<6;i++) {
            for(int j=0;j<2;j++) {
                // draw pickup squares
                gc.beginPath();
                if(selectedPiece == pickupPieces[i][j]) {
                    gc.setFill(boardStyle.getDarkSquareColor());
                } else {
                    gc.setFill(boardStyle.getLightSquareColor());
                }
                gc.rect((xOffset + (9+j)*squareSize) + (borderMargin*3),
                        outerMargin + (i*squareSize) + borderMargin,
                        squareSize, squareSize);
                gc.fill();

                // draw pickup piece image
                int pieceType =  pickupPieces[i][j];
                javafx.scene.image.Image pieceImage = pieceImageProvider.getImage(pieceType, (int) (squareSize * this.outputScaleX),
                        boardStyle.getPieceStyle());
                int x = (xOffset + (9+j)*squareSize) + (borderMargin*3);
                int y = outerMargin + (i*squareSize) + borderMargin;
                gc.drawImage(pieceImage, x, y, squareSize, squareSize);
            }
        }

        // draw grabbed piece
        if(drawGrabbedPiece) {
            int offset = squareSize / 2;
            Image pieceImage = pieceImageProvider.getImage(grabbedPiece.getPiece(),
                    (int) (squareSize * this.outputScaleX), boardStyle.getPieceStyle());
            gc.drawImage(pieceImage, grabbedPiece.getCurrentXLocation() - offset,
                    grabbedPiece.getCurrentYLocation() - offset,squareSize, squareSize);
        }

    }

    Point getBoardPosition(double x, double y) {

        if(x > innerXOffset && y > innerYOffset
                && x < (innerXOffset + 8*squareSize)
                && y < (innerYOffset + 8*squareSize)) {

            int i = (int) x - innerXOffset;
            int j = (int) y - innerYOffset;

            if(flipBoard) {
                i = 7 - (i / squareSize);
                j = j / squareSize;
            } else {
                i = i / squareSize;
                j = 7 - (j / squareSize);
            }
            return new Point(i,j);
        }
        return null;
    }

    int getSelectedPiece(double x, double y) {
        int leftOffset = xOffset + 9*squareSize + 3*borderMargin;
        int downOffset = outerMargin + borderMargin;

        int xIdx = (int) (x - leftOffset);
        int yIdx = (int) (y - downOffset);
        xIdx = xIdx / squareSize;
        yIdx = yIdx / squareSize;
        return pickupPieces[yIdx][xIdx];
    }

    void handleMousePress(MouseEvent e) {

        if(clickedOnPieceSelector(e.getX(),e.getY())) {
            selectedPiece = getSelectedPiece(e.getX(), e.getY());
        }
        if(clickedOnBoard(e.getX(),e.getY())) {
            Point boardPos = getBoardPosition(e.getX(), e.getY());
            int pieceAtXY = board.getPieceAt(boardPos.x, boardPos.y);
            if(pieceAtXY == selectedPiece) {
                board.setPieceAt(boardPos.x, boardPos.y, EMPTY);
            } else {
                board.setPieceAt(boardPos.x, boardPos.y, selectedPiece);
            }
            // update piece list

            notifyBoardChange();
        }
        updateCanvas();
    }

    public void addListener(EnterPosBoardListener toAdd) {
        enterPosBoardListeners.add(toAdd);
    }

    public void notifyBoardChange() {

        for (EnterPosBoardListener epbl : enterPosBoardListeners)
            epbl.boardChanged();
    }


}









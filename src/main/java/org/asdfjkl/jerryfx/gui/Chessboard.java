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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import org.asdfjkl.jerryfx.lib.*;

import java.awt.*;
import java.util.ArrayList;

import static org.asdfjkl.jerryfx.lib.CONSTANTS.*;

public class Chessboard extends CanvasProperties implements StateChangeListener {

    BoardStyle boardStyle;
    final double outputScaleX;
    final GameModel gameModel;
    boolean flipBoard = true;

    int innerXOffset;
    int innerYOffset;
    int squareSize;

    final PieceImageProvider pieceImageProvider;

    Point moveSource;
    final GrabbedPiece grabbedPiece = new GrabbedPiece();
    boolean drawGrabbedPiece = false;

    Point colorClickSource;

    Arrow grabbedArrow;
    boolean drawGrabbedArrow = false;

    Color lastMoveColor;
    Color arrowColor;
    Color arrowGrabColor;
    Color coloredFieldColor;

    public Chessboard(GameModel gameModel) {

        boardStyle = new BoardStyle();
        this.gameModel = gameModel;
        pieceImageProvider = PieceImageProvider.getInstance();
        outputScaleX = Screen.getPrimary().getOutputScaleX();
        grabbedPiece.setPiece(-1);
        moveSource = new Point(-1,-1);
        colorClickSource = new Point(-1,-1);
        grabbedArrow = new Arrow();
        grabbedArrow.xFrom = grabbedArrow.yFrom = grabbedArrow.xTo = grabbedArrow.yTo = -1;
        lastMoveColor = Color.rgb(200,200,0,0.4);
        arrowColor = Color.rgb(50,88,0,1.0);
        arrowGrabColor = Color.rgb(70,130,0, 1.0);
        coloredFieldColor = Color.rgb(200,0,0,0.4);

        setOnMousePressed(event -> {
            handleMousePress(event);
        });

        setOnMouseDragged(event -> {
            handleMouseDragged(event);
        });

        setOnMouseReleased(event -> {
            handleMouseRelease(event);
        });
    }



    @Override
    public void updateCanvas() {

        GraphicsContext graphicsContext = this.getGraphicsContext2D();

        // fill background
        graphicsContext.beginPath();
        //gc.setFill(Color.rgb(152, 152, 152));
        graphicsContext.setFill(boardStyle.getDarkSquareColor());
        graphicsContext.rect(0, 0, this.getWidth(), this.getHeight());
        graphicsContext.fill();

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
        graphicsContext.beginPath();
        graphicsContext.setFill(boardStyle.getBorderColor());
        graphicsContext.rect(xOffset, outerMargin, (squareSize*8)+(borderMargin*2), (squareSize*8)+(borderMargin*2));
        graphicsContext.fill();

        // get the from and to field of the last move
        // to highlight those squares
        Point lastMoveFrom = null;
        Point lastMoveTo = null;
        if(gameModel.getGame().getCurrentNode().getMove() != null) {
            Move m = gameModel.getGame().getCurrentNode().getMove();
            lastMoveFrom = Board.internalToXY(m.getMoveSourceSquare());
            lastMoveTo = Board.internalToXY(m.getMoveTargetSquare());

        }

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

                graphicsContext.beginPath();
                graphicsContext.setFill(fieldColor);
                graphicsContext.rect(x,y,squareSize,squareSize);
                graphicsContext.fill();

                if(lastMoveFrom != null && lastMoveTo != null) {
                    boolean markField = false;
                    if(!flipBoard) {
                        if ((lastMoveFrom.getX() == i && lastMoveFrom.getY() == j) ||
                                (lastMoveTo.getX() == i && lastMoveTo.getY() == j)) {
                            markField = true;
                        }
                    }
                    if(flipBoard) {
                        if ((lastMoveFrom.getX() == i && lastMoveFrom.getY() == 7 - j) ||
                                (lastMoveTo.getX() == i && lastMoveTo.getY() == 7 - j)) {
                            markField = true;
                        }
                    }
                    if(markField) {
                        graphicsContext.beginPath();
                        graphicsContext.setFill(lastMoveColor);
                        graphicsContext.rect(x, y, squareSize, squareSize);
                        graphicsContext.fill();
                    }
                }
            }
        }

        // paint colored fields
        for(ColoredField coloredField : gameModel.getGame().getCurrentNode().getColoredFields()) {

            int xColoredField = coloredField.x;
            int yColoredField = coloredField.y;

            int x = (innerXOffset) + (xColoredField*squareSize);
            int y = (innerYOffset) + ((7-yColoredField)*squareSize);
            if(flipBoard) {
                x = innerXOffset+((7-xColoredField)*squareSize);
                y = (innerYOffset) + (yColoredField*squareSize);
            }

            graphicsContext.beginPath();
            graphicsContext.setFill(coloredFieldColor);
            graphicsContext.rect(x,y,squareSize,squareSize);
            graphicsContext.fill();
        }

        // draw the board coordinates
        graphicsContext.setFill(boardStyle.getCoordinateColor());
        for(int i=0;i<8;i++) {
            if(flipBoard){
                char ch = (char) (65 + (7 - i));
                String idx = Character.toString(ch);
                String num = Integer.toString(i + 1);
                graphicsContext.beginPath();
                graphicsContext.fillText(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        innerYOffset + (8 * squareSize) + (borderMargin * 0.8));
                graphicsContext.fillText(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
            } else{
                char ch = (char) (65 + i);
                String idx = Character.toString(ch);
                String num = Integer.toString(8 - i);
                graphicsContext.beginPath();
                graphicsContext.fillText(idx, innerXOffset + (i * squareSize) + (squareSize / 2) - 4,
                        innerYOffset + (8 * squareSize) + (borderMargin * 0.8));
                graphicsContext.fillText(num, xOffset + 5, innerYOffset + (i * squareSize) + (squareSize / 2) + 4);
            }
        }

        // draw pieces
        Board b = gameModel.getGame().getCurrentNode().getBoard();
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
                    piece = b.getPieceAt(i, 7-j);
                } else {
                    piece = b.getPieceAt(i, j);
                }
                if(piece != EMPTY && piece != FRINGE) {
                    if(!flipBoard) {
                        if (!(drawGrabbedPiece && i == moveSource.x && j == moveSource.y)) {
                            Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                    boardStyle.getPieceStyle());
                            graphicsContext.drawImage(pieceImage, x, y, squareSize, squareSize);
                        }
                    } else {
                        if (!(drawGrabbedPiece && i == moveSource.x && (7-j) == moveSource.y)) {
                            Image pieceImage = pieceImageProvider.getImage(piece, (int) (squareSize * this.outputScaleX),
                                    boardStyle.getPieceStyle());
                            graphicsContext.drawImage(pieceImage, x, y, squareSize, squareSize);
                        }
                    }
                }
            }
        }

        // mark side to move, if we are at the root (no last move)
        if(gameModel.getGame().getRootNode() == gameModel.getGame().getCurrentNode()) {
            int x_side_to_move = innerXOffset + 8 * squareSize + 6;
            int y_side_to_move = innerYOffset + 8 * squareSize + 6;
            if(b.turn == WHITE) {
                if(gameModel.getFlipBoard()) {
                    x_side_to_move = innerXOffset - 11;
                    y_side_to_move = innerYOffset - 11;
                }
            }
            if(b.turn == BLACK) {
                if(!gameModel.getFlipBoard()) {
                    x_side_to_move = innerXOffset - 11;
                    y_side_to_move = innerYOffset - 11;
                }
            }
            graphicsContext.beginPath();
            graphicsContext.setFill(boardStyle.getLightSquareColor());
            graphicsContext.rect(x_side_to_move, y_side_to_move, 4,4);
            graphicsContext.fill();
        }

        // draw grabbed piece
        if(drawGrabbedPiece) {
            int offset = squareSize / 2;
            Image pieceImage = pieceImageProvider.getImage(grabbedPiece.getPiece(),
                    (int) (squareSize * this.outputScaleX), boardStyle.getPieceStyle());
            graphicsContext.drawImage(pieceImage, grabbedPiece.getCurrentXLocation() - offset,
                    grabbedPiece.getCurrentYLocation() - offset,squareSize, squareSize);
        }

        // draw arrows
        ArrayList<Arrow> arrows = gameModel.getGame().getCurrentNode().getArrows();
        if(arrows != null) {
            for (Arrow ai : gameModel.getGame().getCurrentNode().getArrows()) {
                drawArrow(ai, arrowColor, innerXOffset, innerYOffset);
            }
        }

        // draw currently grabbed arrow
        if(drawGrabbedArrow
                && grabbedArrow.xFrom != -1 && grabbedArrow.yFrom != -1
                && grabbedArrow.xTo != -1 && grabbedArrow.yTo != -1
                && ((grabbedArrow.xFrom != grabbedArrow.xTo) || (grabbedArrow.yFrom != grabbedArrow.yTo))) {
            drawArrow(grabbedArrow, arrowGrabColor, innerXOffset, innerYOffset);
        }

    }

    private void drawArrow(Arrow arrow, Color color, int boardOffsetX, int boardOffsetY) {

        GraphicsContext gc = this.getGraphicsContext2D();

        int xFrom = 0;
        int xTo = 0;
        int yFrom = 0;
        int yTo = 0;
        if(this.flipBoard) {
            xFrom = boardOffsetX+((7-arrow.xFrom)*squareSize) + (squareSize/2);
            xTo = boardOffsetX+((7-arrow.xTo)*squareSize) + (squareSize/2);
            yFrom = boardOffsetY+(arrow.yFrom*squareSize)+ (squareSize/2);
            yTo = boardOffsetY+(arrow.yTo*squareSize)+ (squareSize/2);
        } else {
            xFrom = boardOffsetX+(arrow.xFrom*squareSize)+ (squareSize/2);
            xTo = boardOffsetX+(arrow.xTo*squareSize)+ (squareSize/2);
            yFrom = boardOffsetY+((7-arrow.yFrom)*squareSize)+ (squareSize/2);
            yTo = boardOffsetY+((7-arrow.yTo)*squareSize)+ (squareSize/2);
        }

        // incredible annoying calculation to get arrow head
        Point fromPoint = new Point(xFrom, yFrom);
        Point toPoint = new Point(xTo, yTo);

        // added to toPoint to place arrow head
        // somewhere in the center
        double vx = -toPoint.getX() + fromPoint.getX();
        double vy = -toPoint.getY() + fromPoint.getY();

        // vectors correspond to the arrows
        double dx = toPoint.getX() - fromPoint.getX();
        double dy = toPoint.getY() - fromPoint.getY();

        double length = Math.sqrt(dx * dx + dy * dy);

        double unitDx = dx / length;
        double unitDy = dy / length;

        // adjusted according to arrow length
        vx = vx * (squareSize/6 /length);
        vy = vy * (squareSize/6 /length);

        toPoint = new Point((int) (toPoint.getX() - vx), (int) (toPoint.getY() - vy));

        int arrowHeadBoxSize = squareSize/4;
        Point arrowPoint1 = new Point(
                (int) (toPoint.getX() - unitDx * arrowHeadBoxSize - unitDy * arrowHeadBoxSize),
                (int) (toPoint.getY() - unitDy * arrowHeadBoxSize + unitDx * arrowHeadBoxSize));

        Point arrowPoint2 = new Point(
                (int) (toPoint.getX() - unitDx * arrowHeadBoxSize + unitDy * arrowHeadBoxSize),
                (int) (toPoint.getY() - unitDy * arrowHeadBoxSize - unitDx * arrowHeadBoxSize));

        gc.setFill(color);
        gc.fillPolygon( new double[] { toPoint.getX(), arrowPoint1.getX(), arrowPoint2.getX() },
                    new double[] { toPoint.getY(), arrowPoint1.getY(), arrowPoint2.getY() },
                    3);

        // take the old center coord to draw the
        // line to, so that the line doesn not
        // cover the arrow head due to the line's thickness
        Point to = new Point(xTo, yTo);
        double currentLineWidth = gc.getLineWidth();
        javafx.scene.paint.Paint currentPaint = gc.getStroke();
        gc.setLineWidth(squareSize/6);
        gc.setStroke(color);
        gc.strokeLine(fromPoint.getX(), fromPoint.getY(),to.getX(), to.getY());
        gc.setLineWidth(currentLineWidth);
        gc.setStroke(currentPaint);
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

    void touchPiece(int boardX, int boardY, double currentXLocation, double currentYLocation) {

        moveSource = new Point();
        moveSource.x = boardX;
        moveSource.y = boardY;

        grabbedPiece.setCurrentXLocation(currentXLocation);
        grabbedPiece.setCurrentYLocation(currentYLocation);
        grabbedPiece.setPiece(gameModel.getGame().getCurrentNode().getBoard().getPieceAt(boardX,boardY));
        drawGrabbedPiece = true;
    }

    void handleMousePress(MouseEvent e) {

        MouseButton mouseButton = e.getButton();
        if(mouseButton == MouseButton.PRIMARY) {
            Board b = this.gameModel.getGame().getCurrentNode().getBoard();
            Point boardPos = getBoardPosition(e.getX(), e.getY());
            // case a) 1) user clicks source field, then 2) clicks destination
            // case b) user clicks and drags piece
            if (boardPos != null) {
                if (grabbedPiece.getPiece() != -1) {
                    // case a) 2)
                    Move m = new Move(moveSource.x, moveSource.y, boardPos.x, boardPos.y);
                    if (b.isLegalAndPromotes(m)) {
                        int promotionPiece = DialogPromotion.show(b.turn,
                                boardStyle.getPieceStyle(),
                                gameModel.THEME);
                        if (promotionPiece != EMPTY) {
                            m.setPromotionPiece(promotionPiece);
                            applyMove(m);
                        }
                        resetMove();
                    } else if (b.isLegal(m)) {
                        applyMove(m);
                        resetMove();
                    } else {
                        resetMove();
                        if (b.isPieceAt(boardPos.x, boardPos.y)) {
                            touchPiece(boardPos.x, boardPos.y, e.getX(), e.getY());
                        }
                    }
                } else { // case a) 1) or b)
                    if (b.isPieceAt(boardPos.x, boardPos.y)) {
                        touchPiece(boardPos.x, boardPos.y, e.getX(), e.getY());
                    }
                }
            }
        }
        if(mouseButton == MouseButton.SECONDARY) {
            Point boardCoordinate = getBoardPosition(e.getX(), e.getY());
            handleRightClick(boardCoordinate);

        }
    }

    private void handleRightClick(Point boardCoordinate) {
        // user clicked and is going to draw arrow or mark a field
        if(boardCoordinate != null) {
            /*
            if(flipBoard) {
                boardCoordinate.x = (int) (7-boardCoordinate.x);
                boardCoordinate.y = (int) (7-boardCoordinate.y);
            }*/
            colorClickSource.x = boardCoordinate.x;
            colorClickSource.y = boardCoordinate.y;

            grabbedArrow.xFrom = boardCoordinate.x;
            grabbedArrow.yFrom = boardCoordinate.y;
            drawGrabbedArrow = true;
        }
    }

    void handleMouseDragged(MouseEvent e) {

        if(drawGrabbedPiece && grabbedPiece.getPiece() != -1) {
            grabbedPiece.setCurrentXLocation(e.getX());
            grabbedPiece.setCurrentYLocation(e.getY());
            updateCanvas();
        }
        if(drawGrabbedArrow) {
            Point xy = getBoardPosition(e.getX(), e.getY());
            /*
            if(flipBoard) {
                xy.x = 7-xy.x;
                xy.y = 7-xy.y;
            }*/
            if(xy != null) {
                grabbedArrow.xTo = xy.x;
                grabbedArrow.yTo = xy.y;
                updateCanvas();
            }
        }
    }

    void handleMouseRelease(MouseEvent e) {

        MouseButton mouseButton = e.getButton();
        if(mouseButton == MouseButton.PRIMARY) {
            drawGrabbedPiece = false;
            Point boardPos = getBoardPosition(e.getX(), e.getY());
            Board b = gameModel.getGame().getCurrentNode().getBoard();
            if (boardPos != null && grabbedPiece.getPiece() != -1) {
                if (!(boardPos.x == moveSource.x && boardPos.y == moveSource.y)) {
                    Move m = new Move(moveSource.x, moveSource.y, boardPos.x, boardPos.y);
                    if (b.isLegalAndPromotes(m)) {
                        int promotionPiece = DialogPromotion.show(b.turn,
                                boardStyle.getPieceStyle(),
                                gameModel.THEME);
                        if (promotionPiece != EMPTY) {
                            m.setPromotionPiece(promotionPiece);
                            applyMove(m);
                        }
                        resetMove();
                    } else if (b.isLegal(m)) {
                        applyMove(m);
                    } else {
                        resetMove();
                    }

                }
            }
        }
        if(mouseButton == MouseButton.SECONDARY) {
            Point boardCoordinate = getBoardPosition(e.getX(), e.getY());
            handleRightClickRelease(boardCoordinate);
        }
        updateCanvas();
    }

    private void handleRightClickRelease(Point boardCoordinate) {
        // user clicked and is going to draw arrow
        if(boardCoordinate != null) {
            /*
            if (flipBoard) {
                boardCoordinate.x = (7 - boardCoordinate.x);
                boardCoordinate.y = (7 - boardCoordinate.y);
            }*/
            // arrow case
            if (boardCoordinate.x != colorClickSource.x || boardCoordinate.y != colorClickSource.y) {
                Arrow a = new Arrow();
                a.xFrom = colorClickSource.x;
                a.yFrom = colorClickSource.y;
                a.xTo = boardCoordinate.x;
                a.yTo = boardCoordinate.y;
                gameModel.getGame().getCurrentNode().addOrRemoveArrow(a);
            } else { // just marking a field
                ColoredField c = new ColoredField();
                c.x = boardCoordinate.x;
                c.y = boardCoordinate.y;
                gameModel.getGame().getCurrentNode().addOrRemoveColoredField(c);
            }
            drawGrabbedArrow = false;
        }
    }

    void applyMove(Move m) {

        this.gameModel.getGame().applyMove(m);
        // trigger statechange, would trigger updateCanvas itself
        this.gameModel.triggerStateChange();
        //this.updateCanvas();
    }

    void resetMove() {

        moveSource.x = -1;
        moveSource.y = -1;
        grabbedPiece.setPiece(-1);
        drawGrabbedPiece = false;
    }

    @Override
    public void stateChange() {

        if(gameModel.getFlipBoard() != flipBoard) {
            flipBoard = gameModel.getFlipBoard();
        }
        updateCanvas();
    }

}

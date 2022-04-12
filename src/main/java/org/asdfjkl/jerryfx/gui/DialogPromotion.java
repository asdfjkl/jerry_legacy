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

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;
import org.asdfjkl.jerryfx.lib.CONSTANTS;

public class DialogPromotion {

    static Stage stage;
    static int choice;

    public static int show(boolean playerColor, int pieceStyle, int colorTheme) {

        PieceImageProvider provider = PieceImageProvider.getInstance();

        choice = CONSTANTS.EMPTY;

        Button btnQueen = new Button();
        Button btnRook = new Button();
        Button btnBishop = new Button();
        Button btnKnight = new Button();

        int squareSize = 64;
        if(playerColor == CONSTANTS.WHITE) {

            Image imgQueen = provider.getImage(CONSTANTS.WHITE_QUEEN, squareSize, pieceStyle);
            btnQueen.setGraphic(new ImageView(imgQueen));

            Image imgRook = provider.getImage(CONSTANTS.WHITE_ROOK, squareSize, pieceStyle);
            btnRook.setGraphic(new ImageView(imgRook));

            Image imgBishop = provider.getImage(CONSTANTS.WHITE_BISHOP, squareSize, pieceStyle);
            btnBishop.setGraphic(new ImageView(imgBishop));

            Image imgKnight = provider.getImage(CONSTANTS.WHITE_KNIGHT, squareSize, pieceStyle);
            btnKnight.setGraphic(new ImageView(imgKnight));

        } else {

            Image imgQueen = provider.getImage(CONSTANTS.BLACK_QUEEN, squareSize, pieceStyle);
            btnQueen.setGraphic(new ImageView(imgQueen));

            Image imgRook = provider.getImage(CONSTANTS.BLACK_ROOK, squareSize, pieceStyle);
            btnRook.setGraphic(new ImageView(imgRook));

            Image imgBishop = provider.getImage(CONSTANTS.BLACK_BISHOP, squareSize, pieceStyle);
            btnBishop.setGraphic(new ImageView(imgBishop));

            Image imgKnight = provider.getImage(CONSTANTS.BLACK_KNIGHT, squareSize, pieceStyle);
            btnKnight.setGraphic(new ImageView(imgKnight));

        }

        btnQueen.setOnAction(event -> handleQueenClick());
        btnRook.setOnAction(event -> handleRookClick());
        btnBishop.setOnAction(event -> handleBishopClick());
        btnKnight.setOnAction(event -> handleKnightClick());

        stage = new Stage();

        HBox pane = new HBox();
        pane.getChildren().addAll(btnQueen, btnRook, btnBishop, btnKnight);

        Scene scene = new Scene(pane);
        pane.getStyleClass().add(JMetroStyleClass.BACKGROUND);

        JMetro jMetro;
        if(colorTheme == GameModel.STYLE_LIGHT) {
            jMetro = new JMetro();
        } else {
            jMetro = new JMetro(Style.DARK);
        }
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        // stage.initStyle(StageStyle.UTILITY); will result on window not having focus/keyboard
        //                                      input not working on Linux
        stage.getIcons().add(new Image("icons/app_icon.png")); // To add an icon
        stage.showAndWait();

        return choice;

    }

    private static void handleQueenClick() {
        choice = CONSTANTS.QUEEN;
        stage.close();
    }

    private static void handleRookClick() {
        choice = CONSTANTS.ROOK;
        stage.close();
    }

    private static void handleBishopClick() {
        choice = CONSTANTS.BISHOP;
        stage.close();
    }

    private static void handleKnightClick() {
        choice = CONSTANTS.KNIGHT;
        stage.close();
    }

}
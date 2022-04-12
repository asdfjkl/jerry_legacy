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

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import javafx.scene.image.Image;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import javafx.embed.swing.SwingFXUtils;

import static org.asdfjkl.jerryfx.gui.BoardStyle.*;

public class PieceImageProvider {

    private static PieceImageProvider pieceImageProvider = null;

    private PieceImageProvider() {}
    public static PieceImageProvider getInstance() {
        if(pieceImageProvider == null) {
            pieceImageProvider = new PieceImageProvider();
        }
        return pieceImageProvider;
    }

    final HashMap<Integer, Image> whitePawnsMerida = new HashMap<>();
    final HashMap<Integer, Image> blackPawnsMerida = new HashMap<>();

    final HashMap<Integer, Image> whiteRooksMerida = new HashMap<>();
    final HashMap<Integer, Image> blackRooksMerida = new HashMap<>();

    final HashMap<Integer, Image> whiteKnightsMerida = new HashMap<>();
    final HashMap<Integer, Image> blackKnightsMerida = new HashMap<>();

    final HashMap<Integer, Image> whiteBishopsMerida = new HashMap<>();
    final HashMap<Integer, Image> blackBishopsMerida = new HashMap<>();

    final HashMap<Integer, Image> whiteQueensMerida = new HashMap<>();
    final HashMap<Integer, Image> blackQueensMerida = new HashMap<>();

    final HashMap<Integer, Image> whiteKingsMerida = new HashMap<>();
    final HashMap<Integer, Image> blackKingsMerida = new HashMap<>();

    final HashMap<Integer, Image> whitePawnsOld = new HashMap<>();
    final HashMap<Integer, Image> blackPawnsOld = new HashMap<>();

    final HashMap<Integer, Image> whiteRooksOld = new HashMap<>();
    final HashMap<Integer, Image> blackRooksOld = new HashMap<>();

    final HashMap<Integer, Image> whiteKnightsOld = new HashMap<>();
    final HashMap<Integer, Image> blackKnightsOld = new HashMap<>();

    final HashMap<Integer, Image> whiteBishopsOld = new HashMap<>();
    final HashMap<Integer, Image> blackBishopsOld = new HashMap<>();

    final HashMap<Integer, Image> whiteQueensOld = new HashMap<>();
    final HashMap<Integer, Image> blackQueensOld = new HashMap<>();

    final HashMap<Integer, Image> whiteKingsOld = new HashMap<>();
    final HashMap<Integer, Image> blackKingsOld = new HashMap<>();

    final HashMap<Integer, Image> whitePawnsUscf = new HashMap<>();
    final HashMap<Integer, Image> blackPawnsUscf = new HashMap<>();

    final HashMap<Integer, Image> whiteRooksUscf = new HashMap<>();
    final HashMap<Integer, Image> blackRooksUscf = new HashMap<>();

    final HashMap<Integer, Image> whiteKnightsUscf = new HashMap<>();
    final HashMap<Integer, Image> blackKnightsUscf = new HashMap<>();

    final HashMap<Integer, Image> whiteBishopsUscf = new HashMap<>();
    final HashMap<Integer, Image> blackBishopsUscf = new HashMap<>();

    final HashMap<Integer, Image> whiteQueensUscf = new HashMap<>();
    final HashMap<Integer, Image> blackQueensUscf = new HashMap<>();

    final HashMap<Integer, Image> whiteKingsUscf = new HashMap<>();
    final HashMap<Integer, Image> blackKingsUscf = new HashMap<>();

    private BufferedImage renderSVG(URL urlPieceSVG, int width, int height) {

        SVGUniverse svgUniverse = new SVGUniverse();
        SVGDiagram diagram = null;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        try {
            diagram = svgUniverse.getDiagram(svgUniverse.loadSVG(urlPieceSVG));
            diagram.setIgnoringClipHeuristic(true);
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.setToScale(width/diagram.getWidth(), height/diagram.getWidth());
            Graphics2D graphics2DImg = bufferedImage.createGraphics();
            graphics2DImg.transform(affineTransform);
            graphics2DImg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            diagram.render(graphics2DImg);
        } catch (SVGException e) {
            e.printStackTrace();
        }
        return bufferedImage;
    }

    private HashMap<Integer, Image> getPieceStyleHashMap(int pieceType, int pieceStyle) {
        if(pieceType == CONSTANTS.WHITE_PAWN) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return whitePawnsMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return whitePawnsOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return whitePawnsUscf;
            }
        }
        if(pieceType == CONSTANTS.BLACK_PAWN) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return blackPawnsMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return blackPawnsOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return blackPawnsUscf;
            }
        }
        if(pieceType == CONSTANTS.WHITE_ROOK) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return whiteRooksMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return whiteRooksOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return whiteRooksUscf;
            }
        }
        if(pieceType == CONSTANTS.BLACK_ROOK) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return blackRooksMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return blackRooksOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return blackRooksUscf;
            }
        }
        if(pieceType == CONSTANTS.WHITE_KNIGHT) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return whiteKnightsMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return whiteKnightsOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return whiteKnightsUscf;
            }
        }
        if(pieceType == CONSTANTS.BLACK_KNIGHT) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return blackKnightsMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return blackKnightsOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return blackKnightsUscf;
            }
        }
        if(pieceType == CONSTANTS.WHITE_BISHOP) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return whiteBishopsMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return whiteBishopsOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return whiteBishopsUscf;
            }
        }
        if(pieceType == CONSTANTS.BLACK_BISHOP) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return blackBishopsMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return blackBishopsOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return blackBishopsUscf;
            }
        }
        if(pieceType == CONSTANTS.WHITE_QUEEN) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return whiteQueensMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return whiteQueensOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return whiteQueensUscf;
            }
        }
        if(pieceType == CONSTANTS.BLACK_QUEEN) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return blackQueensMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return blackQueensOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return blackQueensUscf;
            }
        }
        if(pieceType == CONSTANTS.WHITE_KING) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return whiteKingsMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return whiteKingsOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return whiteKingsUscf;
            }
        }
        if(pieceType == CONSTANTS.BLACK_KING) {
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                return blackKingsMerida;
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                return blackKingsOld;
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                return blackKingsUscf;
            }
        }
        throw  new IllegalArgumentException("called with, but there is no such piece: "+pieceType);
    }

    private String getFilename(int pieceType) {

        if(pieceType == CONSTANTS.WHITE_PAWN) {
            return "wp.svg";
        }
        if(pieceType == CONSTANTS.BLACK_PAWN) {
            return "bp.svg";
        }
        if(pieceType == CONSTANTS.WHITE_ROOK) {
            return "wr.svg";
        }
        if(pieceType == CONSTANTS.BLACK_ROOK) {
            return "br.svg";
        }
        if(pieceType == CONSTANTS.WHITE_KNIGHT) {
            return "wn.svg";
        }
        if(pieceType == CONSTANTS.BLACK_KNIGHT) {
            return "bn.svg";
        }
        if(pieceType == CONSTANTS.WHITE_BISHOP) {
            return "wb.svg";
        }
        if(pieceType == CONSTANTS.BLACK_BISHOP) {
            return "bb.svg";
        }
        if(pieceType == CONSTANTS.WHITE_QUEEN) {
            return "wq.svg";
        }
        if(pieceType == CONSTANTS.BLACK_QUEEN) {
            return "bq.svg";
        }
        if(pieceType == CONSTANTS.WHITE_KING) {
            return "wk.svg";
        }
        if(pieceType == CONSTANTS.BLACK_KING) {
            return "bk.svg";
        }
        throw  new IllegalArgumentException("called with, but there is no such piece: "+pieceType);
    }


    public Image getImage(int piece, int squareSize, int pieceStyle) {

        HashMap<Integer, Image> pieceHashMap = getPieceStyleHashMap(piece, pieceStyle);
        String svgPieceName = getFilename(piece);

        if(pieceHashMap.containsKey(squareSize)) {
            return pieceHashMap.get(squareSize);
        } else {
            String fileName = "pieces/";
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                fileName += "merida/";
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                fileName += "old/";
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                fileName += "uscf/";
            }
            fileName += svgPieceName;
            URL urlSVG = getClass().getClassLoader().getResource(fileName);
            BufferedImage bufferedImagePiece = renderSVG(urlSVG, squareSize, squareSize);
            Image img = SwingFXUtils.toFXImage(bufferedImagePiece, null);
            pieceHashMap.put(squareSize, img);
            return img;
        }
    }
}

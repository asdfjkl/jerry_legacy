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

package org.asdfjkl.jerryfx.lib;

import java.awt.Point;
import java.util.ArrayList;
import java.lang.Math;


public class Board implements Cloneable {

    public boolean turn;
    public int halfmoveClock;
    public int fullmoveNumber;

    private int[] board;
    private int[] oldBoard;
    private int[][][] pieceList;

    private long zobristHash;
    private long positionHash;

    private boolean undoAvailable;
    private boolean zobristInitialized;
    private boolean posHashInitialized;
    private boolean lastMoveWasNull;

    private boolean castleWkingOk;
    private boolean castleWqueenOk;
    private boolean castleBkingOk;
    private boolean castleBqueenOk;

    private boolean prevCastleWkingOk;
    private boolean prevCastleWqueenOk;
    private boolean prevCastleBkingOk;
    private boolean prevCastleBqueenOk;

    private int enPassentTarget;
    private int prevEnPassentTarget;

    private int prevHalfmoveClock;

    public static int alphaToPos(char alpha) {
        if (alpha == 'A') {
            return 0;
        } else if (alpha == 'B') {
            return 1;
        } else if (alpha == 'C') {
            return 2;
        } else if (alpha == 'D') {
            return 3;
        } else if (alpha == 'E') {
            return 4;
        } else if (alpha == 'F') {
            return 5;
        } else if (alpha == 'G') {
            return 6;
        } else if (alpha == 'H') {
            return 7;
        }
        throw new IllegalArgumentException("alpha to pos called with: " + alpha);
    }

    public static Point internalToXY(int internalCoordinate) {
        if(internalCoordinate < 21 || internalCoordinate > 98) {
            throw new IllegalArgumentException("internalToXY; arg out of range: "+internalCoordinate);
        }
        int col = internalCoordinate % 10 - 1;
        int row = (internalCoordinate / 10) - 2;
        return new Point(col, row);
    }

    public static int xyToInternal(int x, int y) {
        if(x < 0 || x > 7 || y < 0 || y > 7) {
            throw new IllegalArgumentException("xyToInternal; arg out of range: "+ x + " " + y);
        } else {
            return (x+1) + ((y*10)+20);
        }
    }

    // create empty board
    public Board() {
        this(false);
    }

    public Board(boolean startingPosition) {

        if(startingPosition) {
            this.board = new int[120];
            this.oldBoard = new int[120];
            this.pieceList = new int[2][7][10];

            this.turn = CONSTANTS.WHITE;
            for (int i = 0; i < 120; i++) {
                this.board[i] = CONSTANTS.INIT_POS[i];
                this.oldBoard[i] = 0xFF;
            }
            this.initPieceList();
            this.castleWkingOk = true;
            this.castleWqueenOk = true;
            this.castleBkingOk = true;
            this.castleBqueenOk = true;
            this.prevCastleWkingOk = false;
            this.prevCastleWqueenOk = false;
            this.prevCastleBkingOk = false;
            this.prevCastleBqueenOk = false;
            this.enPassentTarget = 0;
            this.halfmoveClock = 0;
            this.fullmoveNumber = 1;
            this.undoAvailable = false;
            this.lastMoveWasNull = false;
            this.prevHalfmoveClock = 0;
            this.zobristInitialized = false;
            this.posHashInitialized = false;
        } else {  // initialize empty board
            this.board = new int[120];
            this.oldBoard = new int[120];
            this.pieceList = new int[2][7][10];

            this.turn = CONSTANTS.WHITE;
            for(int i=0;i<120;i++) {
                this.board[i] = CONSTANTS.EMPTY_POS[i];
                this.oldBoard[i] = 0xFF;
            }
            this.initPieceList();
            this.castleWkingOk = false;
            this.castleWqueenOk = false;
            this.castleBkingOk = false;
            this.castleBqueenOk = false;
            this.prevCastleWkingOk = false;
            this.prevCastleWqueenOk = false;
            this.prevCastleBkingOk = false;
            this.prevCastleBqueenOk = false;
            this.enPassentTarget = 0;
            this.halfmoveClock = 0;
            this.fullmoveNumber = 1;
            this.undoAvailable = false;
            this.lastMoveWasNull = false;
            this.prevHalfmoveClock = 0;
            this.zobristInitialized = false;
            this.posHashInitialized = false;
        }
    }

    public Board(String fen) {

        this.board = new int[120];
        this.oldBoard = new int[120];
        this.pieceList = new int[2][7][10];

        for(int i=0;i<120;i++) {
            this.board[i] = CONSTANTS.EMPTY_POS[i];
            this.oldBoard[i] = 0xFF;
        }

        // check that we have six parts in fen, each separated by space
        // if last two parts are missing (fullmove no. + halfmove clock)
        // try to still parse the game
        String[] fenParts = fen.split(" ");
        if(fenParts.length < 4) {
            throw new IllegalArgumentException("fen: parts missing in "+fen);
        }
        // check that the first part consists of 8 rows, each sep. by /
        String[] rows = fenParts[0].split("/");
        if(rows.length != 8) {
            throw new IllegalArgumentException("fen: not 8 rows in 0th part in "+fen);
        }
        // check that in each row, there are no two consecutive digits
        for(int i=0;i<rows.length;i++) {
            String row = rows[i];
            int field_sum = 0;
            boolean previous_was_digit = false;
            for(int j=0;j<row.length();j++) {
                char rj = row.charAt(j);
                char rjl = Character.toLowerCase(rj);
                if(rj == '1' || rj == '2' || rj == '3' || rj == '4' ||
                        rj == '5' || rj == '6' || rj == '7' || rj == '8')
                {
                    if(previous_was_digit) {
                        throw new IllegalArgumentException("fen: two consecutive digits in rows in " + fen);
                    } else {
                        field_sum += Character.getNumericValue(rj);
                        previous_was_digit = true;
                    }
                } else if(rjl == 'p' || rjl == 'n' || rjl == 'b' || rjl == 'r' ||
                        rjl == 'q' || rjl == 'k')
                {
                    field_sum += 1;
                    previous_was_digit = false;
                } else {
                    throw new IllegalArgumentException("fen: two consecutive chars in rows in " + fen);
                }
            }
            // validate that there are 8 alphanums in each row
            if(field_sum != 8) {
                throw new IllegalArgumentException("fen: field sum is not 8 in "+fen);
            }
        }
        // check that turn part is valid
        if(!(fenParts[1].equals("w") || fenParts[1].equals("b"))) {
            throw new IllegalArgumentException("fen: turn part is invalid in "+fen);
        }
        // check that castles part in correctly encoded using regex
        boolean castlesMatch = fenParts[2].matches("^-|[KQABCDEFGH]{0,2}[kqabcdefgh]{0,2}$");
        if(!castlesMatch) {
            throw new IllegalArgumentException("fen: castles encoding is invalid in "+fen);
        }
        // check correct encoding of en passent squares
        if(!fenParts[3].equals("-")) {
            if(fenParts[1].equals("w")) {
                // should be something like "e6" etc. if white is to move
                // check that int value part is sixth rank
                if(fenParts[3].length() != 2 || fenParts[3].charAt(1) != '6') {
                    throw new IllegalArgumentException("fen: invalid e.p. encoding (white to move) in " + fen);
                }
            } else {
                if(fenParts[3].length() != 2 || fenParts[3].charAt(1) != '3') {
                    throw new IllegalArgumentException("fen: invalid e.p. encoding (black to move) in "+fen);
                }
            }
        }
        // half-move counter validity (if half-move is present)
        if((fenParts.length >= 5) && Integer.parseInt(fenParts[4]) < 0) {
            throw new IllegalArgumentException("fen: negative half move clock or not a number in "+fen);
        }
        // full move number validity (if full move number is present)
        if((fenParts.length >= 6) && Integer.parseInt(fenParts[5]) < 0) {
            throw new IllegalArgumentException("fen: fullmove number not positive");
        }
        // set pieces
        for(int i=0;i<rows.length;i++) {
            int square_index = 91 - (i*10);
            String row = rows[i];
            for(int j=0;j<row.length();j++) {
                char rj = row.charAt(j);
                char rjl = Character.toLowerCase(rj);
                if(rj == '1' || rj == '2' || rj == '3' || rj == '4' || rj == '5' ||
                        rj == '6' || rj == '7' || rj == '8')
                {
                    square_index += Character.getNumericValue(rj);
                } else if(rjl == 'p' || rjl == 'n' || rjl == 'b' ||
                        rjl == 'r' || rjl == 'q' || rjl == 'k')
                {
                    int piece = this.pieceFromSymbol(rj);
                    this.board[square_index] = piece;
                    square_index += 1;
                }
            }
        }
        // set turn
        if(fenParts[1].equals("w")) {
            this.turn = CONSTANTS.WHITE;
        }
        if(fenParts[1].equals("b")) {
            this.turn = CONSTANTS.BLACK;
        }
        this.castleWkingOk = false;
        this.castleWqueenOk = false;
        this.castleBkingOk = false;
        this.castleBqueenOk = false;
        for(int i=0;i<fenParts[2].length();i++) {
            char ci = fenParts[2].charAt(i);
            if(ci == 'K') {
                this.castleWkingOk = true;
            }
            if(ci == 'Q') {
                this.castleWqueenOk = true;
            }
            if(ci == 'k') {
                this.castleBkingOk = true;
            }
            if(ci == 'q') {
                this.castleBqueenOk = true;
            }
        }
        // set en passent square
        if(fenParts[3].equals("-")) {
            this.enPassentTarget = 0;
        } else {
            int row = 10 + Character.getNumericValue(fenParts[3].charAt(1)) * 10;
            int col = 0;
            char c = Character.toLowerCase(fenParts[3].charAt(0));
            if(c == 'a') {
                col = 1;
            }
            if(c == 'b') {
                col = 2;
            }
            if(c == 'c') {
                col = 3;
            }
            if(c == 'd') {
                col = 4;
            }
            if(c == 'e') {
                col = 5;
            }
            if(c == 'f') {
                col = 6;
            }
            if(c == 'g') {
                col = 7;
            }
            if(c == 'h') {
                col = 8;
            }
            this.enPassentTarget = row + col;
        }
        if(fenParts.length >= 5) {
            this.halfmoveClock = Integer.parseInt(fenParts[4]);
        } else {
            this.halfmoveClock = 0;
        }
        if(fenParts.length >= 6) {
            int fullMoveNumber = Integer.parseInt(fenParts[5]);
            if(fullMoveNumber > 0) {
                this.fullmoveNumber = fullMoveNumber;
            } else {
                this.fullmoveNumber = 1;
            }
        } else {
            this.fullmoveNumber = 1;
        }
        this.undoAvailable = false;
        this.lastMoveWasNull = false;
        if(!this.isConsistent()) {
            throw new IllegalArgumentException("fen: board position from supplied fen is inconsistent in "+fen);
        }
        this.initPieceList();

        this.zobristInitialized = false;
        this.posHashInitialized = false;

    }

    // reset to starting position
    public void resetToStartingPosition() {

        this.board = new int[120];
        this.oldBoard = new int[120];
        this.pieceList = new int[2][7][10];

        this.turn = CONSTANTS.WHITE;
        for(int i=0;i<120;i++) {
            this.board[i] = CONSTANTS.INIT_POS[i];
            this.oldBoard[i] = 0xFF;
        }
        this.initPieceList();
        this.castleWkingOk = true;
        this.castleWqueenOk = true;
        this.castleBkingOk = true;
        this.castleBqueenOk = true;
        this.prevCastleWkingOk = false;
        this.prevCastleWqueenOk = false;
        this.prevCastleBkingOk = false;
        this.prevCastleBqueenOk = false;
        this.enPassentTarget = 0;
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
        this.undoAvailable = false;
        this.lastMoveWasNull = false;
        this.prevHalfmoveClock = 0;
        this.zobristInitialized = false;
        this.posHashInitialized = false;
    }

    public Board makeCopy() {
        Board cloneBoard = null;
        try {
            cloneBoard = (Board) this.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return cloneBoard;
    }

   /* public Board makeCopy() {

        Board b = new Board();

        for(int i=0;i<120;i++) {
            b.board[i] = this.board[i];
            b.oldBoard[i] = this.oldBoard[i];
        }
        for(int i=0;i<2;i++) {
            for(int j=0;j<7;j++) {
                for(int k=0;k<10;k++) {
                    b.pieceList[i][j][k] = this.pieceList[i][j][k];
                }
            }
        }

        b.turn = this.turn;
        b.halfmoveClock = this.halfmoveClock;
        b.fullmoveNumber = this.fullmoveNumber;

        b.zobristHash = this.zobristHash;
        b.positionHash = this.positionHash;

        b.undoAvailable = this.undoAvailable;
        b.zobristInitialized = this.zobristInitialized;
        b.posHashInitialized = this.posHashInitialized;
        b.lastMoveWasNull = this.lastMoveWasNull;

        b.castleWkingOk = this.castleWkingOk;
        b.castleWqueenOk = this.castleWqueenOk;
        b.castleBkingOk = this.castleBkingOk;
        b.castleBqueenOk = this.castleBqueenOk;

        b.prevCastleWkingOk = this.prevCastleWkingOk;
        b.prevCastleWqueenOk = this.prevCastleWqueenOk;
        b.prevCastleBkingOk = this.prevCastleBkingOk;
        b.prevCastleBqueenOk = this.prevCastleBqueenOk;

        b.enPassentTarget = this.enPassentTarget;
        b.prevEnPassentTarget = this.prevEnPassentTarget;

        b.prevHalfmoveClock = this.prevHalfmoveClock;

        return b;

    }*/

    public String fen() {
        String fenString = "";
        // first build board
        for(int i=90;i>=20;i-=10) {
            int square_counter = 0;
            for(int j=1;j<9;j++) {
                if(this.board[i+j] != CONSTANTS.EMPTY) {
                    int piece = this.board[i+j];
                    fenString += this.pieceToSymbol(piece);
                    square_counter = 0;
                } else {
                    square_counter += 1;
                    if(j==8) {
                        char c = (char) (48 + square_counter);
                        fenString += c;
                    } else {
                        if(this.board[i+j+1] != CONSTANTS.EMPTY) {
                            char c = (char) (48 + square_counter);
                            fenString += c;
                        }
                    }
                }
            }
            if(i!=20) {
                fenString += '/';
            }
        }
        // write turn
        if(this.turn == CONSTANTS.WHITE) {
            fenString += " w";
        } else {
            fenString += " b";
        }
        // write castling rights
        if(this.canCastleWhiteKing() || this.canCastleWhiteQueen() || this.canCastleBlackKing() || this.canCastleBlackQueen()) {
            fenString += " ";
            if(this.canCastleWhiteKing()) {
                fenString += "K";
            }
            if(this.canCastleWhiteQueen()) {
                fenString += "Q";
            }
            if(this.canCastleBlackKing()) {
                fenString += "k";
            }
            if(this.canCastleBlackQueen()) {
                fenString += "q";
            }

        } else {
            fenString += " -";
        }
        // write ep target if exists
        if(this.enPassentTarget != 0) {
            fenString += " " + this.internalIdxToString(this.enPassentTarget);
        } else {
            fenString += " -";
        }
        // add halfmove clock and fullmove counter
        fenString += " " + this.halfmoveClock;
        fenString += " " + this.fullmoveNumber;

        return fenString;
    }

    private void removeFromPieceList(boolean color, int piece_type, int idx) {

        int intColor = 0;
        if(color) {
            intColor = 1;
        }

        int j = -1;
        for(int i=0;i<10;i++) {
            if(this.pieceList[intColor][piece_type][i] == idx) {
                j = i;
                break;
            }
        }
        if(j>=0) {
            // move all other one step further
            for(int i=j+1;i<10;i++) {
                this.pieceList[intColor][piece_type][i-1] = this.pieceList[intColor][piece_type][i];
            }
            // empty last one in list
            this.pieceList[intColor][piece_type][9] = CONSTANTS.EMPTY;
        }
    }

    private void addToPieceList(boolean color, int piece_type, int idx) {

        int intColor = 0;
        if(color) {
            intColor = 1;
        }

        for(int i=0;i<10;i++) {
            if(this.pieceList[intColor][piece_type][i] == CONSTANTS.EMPTY) {
                this.pieceList[intColor][piece_type][i] = idx;
                break;
            }
        }
    }


    // doesn't check legality
    public void apply(Move m) {

        if(m.isNullMove) {
            this.turn = !this.turn;
            this.prevEnPassentTarget = this.enPassentTarget;
            this.enPassentTarget = 0;
            this.lastMoveWasNull = true;
            this.undoAvailable = true;
            if(this.turn == CONSTANTS.WHITE) {
                this.fullmoveNumber++;
            }
        } else {
            this.lastMoveWasNull = false;
            this.turn = !this.turn;
            this.prevEnPassentTarget = this.enPassentTarget;
            this.prevCastleWkingOk = this.castleWkingOk;
            this.prevCastleWqueenOk = this.castleWqueenOk;
            this.prevCastleBkingOk = this.castleBkingOk;
            this.prevCastleBqueenOk = this.castleBqueenOk;
            this.enPassentTarget = 0;
            if(this.turn == CONSTANTS.WHITE) {
                this.fullmoveNumber++;
            }
            for(int i=0;i<120;i++) {
                this.oldBoard[i] = this.board[i];
            }
            int oldPieceType = this.getPieceTypeAt(m.from);
            boolean color = this.getPieceColorAt(m.from);
            // if target field is not empty, remove from piece list
            // this must be of oppsite color than the currently moving piece
            if(this.board[m.to] != CONSTANTS.EMPTY) {
                int currentTargetPiece = this.getPieceTypeAt(m.to);
                this.removeFromPieceList(!color, currentTargetPiece, m.to);
            }
            // also remove the currently moving piece from the list
            this.removeFromPieceList(color, oldPieceType, m.from);
            // increase halfmove clock only if no capture or pawn advance
            // happend
            this.prevHalfmoveClock = this.halfmoveClock;
            if(oldPieceType == CONSTANTS.PAWN || this.board[m.to] != CONSTANTS.EMPTY) {
                this.halfmoveClock = 0;
            } else {
                this.halfmoveClock++;
            }
            // if we move a pawn two steps up, set the en_passent field
            if(oldPieceType == CONSTANTS.PAWN) {
                // white pawn moved two steps up
                if((m.to - m.from) == 20) {
                    this.enPassentTarget = m.from + 10;
                }
                // black pawn moved two steps up (down)
                if((m.to - m.from == -20)) {
                    this.enPassentTarget = m.from - 10;
                }
            }
            // if the move is an en-passent capture,
            // remove the (non-target) corresponding pawn
            // move is an en passent move, if
            // a) color is white, piece type is pawn, target
            // is up left or upright and empty
            // b) color is black, piece type is pawn, target
            // is down right or down left and empty
            // also set last_move_was_ep to true
            if(oldPieceType == CONSTANTS.PAWN) {
                if(this.board[m.to] == CONSTANTS.EMPTY) {
                    if(color == CONSTANTS.WHITE && ((m.to-m.from == 9) || (m.to-m.from)==11)) {
                        // remove captured pawn
                        this.board[m.to-10] = CONSTANTS.EMPTY;
                        // also remove from piece list
                        this.removeFromPieceList(!color, CONSTANTS.PAWN, m.to-10);
                    }
                    if(color == CONSTANTS.BLACK && ((m.from -m.to == 9) || (m.from - m.to)==11)) {
                        // remove captured pawn
                        this.board[m.to+10] = CONSTANTS.EMPTY;
                        // also remove from piece list
                        this.removeFromPieceList(!color, CONSTANTS.PAWN, m.to+10);
                    }
                }
            }
            // if the move is a promotion, the target
            // field becomes the promotion choice
            if(m.promotionPiece != CONSTANTS.EMPTY) {
                // true means black
                if(color == CONSTANTS.BLACK) {
                    // +128 sets 7th bit to true (means black)
                    this.board[m.to] = m.promotionPiece + 128;
                    // add to piece list
                    this.addToPieceList(CONSTANTS.BLACK, m.promotionPiece, m.to);
                }
                else {
                    this.board[m.to] = m.promotionPiece;
                    this.addToPieceList(CONSTANTS.WHITE, m.promotionPiece, m.to);
                }
            } else {
                // otherwise the target is the piece on the from field
                this.board[m.to] = this.board[m.from];
                this.addToPieceList(color, oldPieceType, m.to);
            }
            this.board[m.from] = CONSTANTS.EMPTY;
            // check if the move is castles, i.e. 0-0 or 0-0-0
            // then we also need to move the rook
            // white kingside
            if(oldPieceType == CONSTANTS.KING) {
                if(color==CONSTANTS.WHITE) {
                    if(m.from == CONSTANTS.E1 && m.to == CONSTANTS.G1) {
                        this.board[CONSTANTS.F1] = this.board[CONSTANTS.H1];
                        this.board[CONSTANTS.H1] = CONSTANTS.EMPTY;
                        this.setCastleWKing(false);
                        this.removeFromPieceList(CONSTANTS.WHITE, CONSTANTS.ROOK, CONSTANTS.H1);
                        this.addToPieceList(CONSTANTS.WHITE, CONSTANTS.ROOK, CONSTANTS.F1);
                    }
                    // white queenside
                    if(m.from == CONSTANTS.E1 && m.to == CONSTANTS.C1) {
                        this.board[CONSTANTS.D1] = this.board[CONSTANTS.A1];
                        this.board[CONSTANTS.A1] = CONSTANTS.EMPTY;
                        this.setCastleWQueen(false);
                        this.removeFromPieceList(CONSTANTS.WHITE, CONSTANTS.ROOK, CONSTANTS.A1);
                        this.addToPieceList(CONSTANTS.WHITE, CONSTANTS.ROOK, CONSTANTS.D1);
                    } }
                else if(color==CONSTANTS.BLACK) {
                    // black kingside
                    if(m.from == CONSTANTS.E8 && m.to == CONSTANTS.G8) {
                        this.board[CONSTANTS.F8] = this.board[CONSTANTS.H8];
                        this.board[CONSTANTS.H8] = CONSTANTS.EMPTY;
                        this.setCastleBKing(false);
                        this.removeFromPieceList(CONSTANTS.BLACK, CONSTANTS.ROOK, CONSTANTS.H8);
                        this.addToPieceList(CONSTANTS.BLACK, CONSTANTS.ROOK, CONSTANTS.F8);
                    }
                    // black queenside
                    if(m.from == CONSTANTS.E8 && m.to == CONSTANTS.C8) {
                        this.board[CONSTANTS.D8] = this.board[CONSTANTS.A8];
                        this.board[CONSTANTS.A8] = CONSTANTS.EMPTY;
                        this.setCastleBQueen(false);
                        this.removeFromPieceList(CONSTANTS.BLACK, CONSTANTS.ROOK, CONSTANTS.A8);
                        this.addToPieceList(CONSTANTS.BLACK, CONSTANTS.ROOK, CONSTANTS.D8);
                    }
                }
            }
            // check if someone loses castling rights
            // by moving king or by moving rook
            // or if one of the rooks is captured by the
            // opposite side
            if(color == CONSTANTS.WHITE) {
                if(oldPieceType == CONSTANTS.KING) {
                    if(m.from == CONSTANTS.E1 && m.to != CONSTANTS.G1) {
                        this.setCastleWKing(false);
                    }
                    if(m.from == CONSTANTS.E1 && m.to != CONSTANTS.C1) {
                        this.setCastleWQueen(false);
                    }
                }
                if(oldPieceType == CONSTANTS.ROOK) {
                    if(m.from == CONSTANTS.A1) {
                        this.setCastleWQueen(false);
                    }
                    if(m.from == CONSTANTS.H1) {
                        this.setCastleWKing(false);
                    }
                }
                // white moves a piece to H8 or A8
                // means either white captures rook
                // or black has moved rook prev.
                // [even though: in the latter case, should be already
                // done by check above in prev. moves]
                if(m.to == CONSTANTS.H8) {
                    this.setCastleBKing(false);
                }
                if(m.to == CONSTANTS.A8) {
                    this.setCastleBQueen(false);
                }
            }
            // same for black
            if(color == CONSTANTS.BLACK) {
                if(oldPieceType == CONSTANTS.KING) {
                    if(m.from == CONSTANTS.E8 && m.to != CONSTANTS.G8) {
                        this.setCastleBKing(false);
                    }
                    if(m.from == CONSTANTS.E8 && m.to != CONSTANTS.C8) {
                        this.setCastleBQueen(false);
                    }
                }
                if(oldPieceType == CONSTANTS.ROOK) {
                    if(m.from == CONSTANTS.A8) {
                        this.setCastleBQueen(false);
                    }
                    if(m.from == CONSTANTS.H8) {
                        this.setCastleBKing(false);
                    }
                }
                // black moves piece to A1 or H1
                if(m.to == CONSTANTS.H1) {
                    this.setCastleWKing(false);
                }
                if(m.to == CONSTANTS.A1) {
                    this.setCastleWQueen(false);
                }
            }
            // after move is applied, can revert to the previous position
            this.undoAvailable = true;
        }
    }

    public void undo() {
        if(!this.undoAvailable) {
            throw new IllegalArgumentException("must call board.apply(move) each time before calling undo() ");
        } else {
            if(this.lastMoveWasNull) {
                this.turn = !this.turn;
                this.enPassentTarget = this.prevEnPassentTarget;
                this.prevEnPassentTarget = 0;
                this.lastMoveWasNull = false;
                this.undoAvailable = true;
            } else {
                for(int i=0;i<120;i++) {
                    this.board[i] = this.oldBoard[i];
                }
                this.undoAvailable = false;
                this.enPassentTarget = this.prevEnPassentTarget;
                this.prevEnPassentTarget = 0;
                this.castleWkingOk = this.prevCastleWkingOk;
                this.castleWqueenOk = this.prevCastleWqueenOk;
                this.castleBkingOk = this.prevCastleBkingOk;
                this.castleBqueenOk = this.prevCastleBqueenOk;
                this.turn = !this.turn;
                this.halfmoveClock = this.prevHalfmoveClock;
                this.prevHalfmoveClock = 0;
                if(this.turn == CONSTANTS.BLACK) {
                    this.fullmoveNumber--;
                }
            }
        }
        this.initPieceList();
    }

    private String internalIdxToString(int idx) {
        if(idx<21 || idx>98) {
            throw new IllegalArgumentException("called idx_to_str but idx is in fringe: "+idx);
        } else {
            char row = (char) ((idx / 10) + 47);
            char col = (char) ((idx % 10) + 96);
            String s = "";
            s += col;
            s += row;
            return s;
        }
    }

    //private boolean isOffside(int internalCoordinate) {
    //    return (this.board[internalCoordinate] == 0xFF);
    //}

    //private boolean isEmpty(int internalCoordinate) {
    //    return (this.board[internalCoordinate] == 0);
    //}

    public ArrayList<Move> pseudoLegalMoves() {
        return this.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, CONSTANTS.ANY_SQUARE, CONSTANTS.ANY_PIECE,true, this.turn);
    }

    public ArrayList<Move> pseudoLegalMoves(int internalFromSquare, int internalToSquare, int pieceType,
                                            boolean genCastleMoves, boolean color) {

        int intColor = 0;
        if(color) {
            intColor = 1;
        }

        ArrayList<Move> moves = new ArrayList<Move>();
        // pawn moves
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.PAWN) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[intColor][CONSTANTS.PAWN][i];
                if(from == CONSTANTS.EMPTY) { // we reached the end of the piece list
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                //assert(this->board[from] != 0xff);
                int piece_idx = CONSTANTS.IDX_WPAWN;
                if(color == CONSTANTS.BLACK) {
                    piece_idx = CONSTANTS.IDX_BPAWN;
                }
                // take up right, or up left
                for(int j=3;j<=4;j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[piece_idx][j];
                    if((internalToSquare == CONSTANTS.ANY_SQUARE || idx == internalToSquare) && this.board[idx] != 0xFF) {
                        if(( this.board[idx] != 0 && color==CONSTANTS.BLACK && (getPieceColorAt(idx) == CONSTANTS.WHITE)) ||
                                ( this.board[idx] != 0 && color==CONSTANTS.WHITE && (getPieceColorAt(idx) == CONSTANTS.BLACK)))
                        {
                            // if it's a promotion square, add four moves
                            if((color==CONSTANTS.WHITE && (idx / 10 == 9)) || (color==CONSTANTS.BLACK && (idx / 10 == 2)))
                            {
                                //assert(this->board[from] != 0xff);
                                moves.add(new Move(from,idx,CONSTANTS.QUEEN));
                                moves.add(new Move(from,idx,CONSTANTS.ROOK));
                                moves.add(new Move(from,idx,CONSTANTS.BISHOP));
                                moves.add(new Move(from,idx,CONSTANTS.KNIGHT));
                            } else {
                                //assert(this->board[from] != 0xff);
                                moves.add(new Move(from,idx));
                            }
                        }
                    }
                }
                // move one (j=1) or two (j=2) up (or down in the case of black)
                int idx_1up = from + CONSTANTS.DIR_TABLE[piece_idx][1];
                int idx_2up = from + CONSTANTS.DIR_TABLE[piece_idx][2];
                if((internalToSquare == CONSTANTS.ANY_SQUARE || idx_2up == internalToSquare) && this.board[idx_2up] != 0xFF) {
                    if((color == CONSTANTS.WHITE && (from/10==3)) || (color==CONSTANTS.BLACK && (from/10==8))) {
                        // means we have a white/black pawn in inital position, direct square
                        // in front is empty => allow to move two forward
                        if( this.board[idx_1up] == 0 && this.board[idx_2up] == 0) {
                            //assert(this->board[from] != 0xff);
                            moves.add(new Move(from,idx_2up));
                        }
                    }
                }
                if((internalToSquare == CONSTANTS.ANY_SQUARE || idx_1up == internalToSquare)
                        && this.board[idx_1up] == 0) {
                    // if it's a promotion square, add four moves
                    if((color==CONSTANTS.WHITE && (idx_1up / 10 == 9)) || (color==CONSTANTS.BLACK && (idx_1up / 10 == 2))) {
                        //assert(this->board[from] != 0xff);
                        moves.add(new Move(from,idx_1up,CONSTANTS.QUEEN));
                        moves.add(new Move(from,idx_1up,CONSTANTS.ROOK));
                        moves.add(new Move(from,idx_1up,CONSTANTS.BISHOP));
                        moves.add(new Move(from,idx_1up,CONSTANTS.KNIGHT));
                    } else {
                        //assert(this->board[from] != 0xff);
                        moves.add(new Move(from,idx_1up));
                    }
                }
                // finally, potential en-passent capture is handled
                // left up
                if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == this.enPassentTarget) {
                    if (color == CONSTANTS.WHITE && (this.enPassentTarget - from) == 9) {
                        //assert(this.board[from] != 0xff);
                        Move m = new Move(from, this.enPassentTarget);
                        moves.add(m);
                    }
                    // right up
                    if (color == CONSTANTS.WHITE && (this.enPassentTarget - from) == 11) {
                        //assert(this->board[from] != 0xff);
                        Move m = new Move(from, this.enPassentTarget);
                        moves.add(m);
                    }
                    // left down
                    if (color == CONSTANTS.BLACK && (this.enPassentTarget - from) == -9) {
                        //assert(this->board[from] != 0xff);
                        Move m = new Move(from, this.enPassentTarget);
                        moves.add(m);
                    }
                    if (color == CONSTANTS.BLACK && (this.enPassentTarget - from) == -11) {
                        //assert(this->board[from] != 0xff);
                        Move m = new Move(from, this.enPassentTarget);
                        moves.add(m);
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.KNIGHT) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[intColor][CONSTANTS.KNIGHT][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                //assert(this->board[from] != 0xff);
                int lookup_idx = CONSTANTS.IDX_KNIGHT;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0];j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    if((internalToSquare == CONSTANTS.ANY_SQUARE || idx == internalToSquare) && this.board[idx] != 0xFF) {
                        if(  this.board[idx] == 0 || (this.getPieceColorAt(idx) != color)) {
                            moves.add(new Move(from,idx));
                        }
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.KING) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[intColor][CONSTANTS.KING][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                //assert(this->board[from] != 0xff);
                int lookup_idx = CONSTANTS.IDX_KING;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0];j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    if((internalToSquare == CONSTANTS.ANY_SQUARE || idx == internalToSquare) && this.board[idx] != 0xFF) {
                        if( this.board[idx] == 0 || (this.getPieceColorAt(idx) != color)) {
                            moves.add(new Move(from,idx));
                        }
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.ROOK) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[intColor][CONSTANTS.ROOK][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                //assert(this->board[from] != 0xff);
                int lookup_idx = CONSTANTS.IDX_ROOK;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0] ;j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    boolean stop = false;
                    while(!stop) {
                        if(this.board[idx] != 0xFF) {
                            if(this.board[idx]==0) {
                                if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                    moves.add(new Move(from,idx));
                                }
                            } else {
                                stop = true;
                                if(this.getPieceColorAt(idx) != color) {
                                    if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                        moves.add(new Move(from,idx));
                                    }
                                }
                            }
                            idx = idx + CONSTANTS.DIR_TABLE[lookup_idx][j];
                        } else {
                            stop = true;
                        }
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.BISHOP) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[intColor][CONSTANTS.BISHOP][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                int lookup_idx = CONSTANTS.IDX_BISHOP;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0] ;j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    boolean stop = false;
                    while(!stop) {
                        if(this.board[idx]!=0xFF) {
                            if(this.board[idx] == 0) {
                                if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                    moves.add(new Move(from,idx));
                                }
                            } else {
                                stop = true;
                                if(this.getPieceColorAt(idx) != color) {
                                    if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                        moves.add(new Move(from,idx));
                                    }
                                }
                            }
                            idx = idx + CONSTANTS.DIR_TABLE[lookup_idx][j];
                        } else {
                            stop = true;
                        }
                    }
                }
            }
        }
        if(pieceType == CONSTANTS.ANY_PIECE || pieceType == CONSTANTS.QUEEN) {
            for(int i=0;i<10;i++) {
                int from = this.pieceList[intColor][CONSTANTS.QUEEN][i];
                if(from == CONSTANTS.EMPTY) {
                    break;
                }
                // skip if we generate only moves from a certain square
                if(internalFromSquare != CONSTANTS.ANY_SQUARE && internalFromSquare != from) {
                    continue;
                }
                int lookup_idx = CONSTANTS.IDX_QUEEN;
                for(int j=1;j<=CONSTANTS.DIR_TABLE[lookup_idx][0] ;j++) {
                    int idx = from + CONSTANTS.DIR_TABLE[lookup_idx][j];
                    boolean stop = false;
                    while(!stop) {
                        if(this.board[idx]!=0xFF) {
                            if(this.board[idx] == 0) {
                                if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                    moves.add(new Move(from,idx));
                                }
                            } else {
                                stop = true;
                                if(this.getPieceColorAt(idx) != color) {
                                    if(internalToSquare == CONSTANTS.ANY_SQUARE || internalToSquare == idx) {
                                        moves.add(new Move(from,idx));
                                    }
                                }
                            }
                            idx = idx + CONSTANTS.DIR_TABLE[lookup_idx][j];
                        } else {
                            stop = true;
                        }
                    }
                }
            }
        }
        if(genCastleMoves) {
            if(color == CONSTANTS.WHITE) {
                // check for castling
                // white kingside
                if(  this.board[CONSTANTS.E1] != 0 && this.canCastleWhiteKing() && this.board[CONSTANTS.H1] != 0 &&
                        this.getPieceColorAt(CONSTANTS.E1) == CONSTANTS.WHITE &&
                        this.getPieceColorAt(CONSTANTS.H1) == CONSTANTS.WHITE &&
                        this.getPieceTypeAt(CONSTANTS.E1) == CONSTANTS.KING &&
                        this.getPieceTypeAt(CONSTANTS.H1) == CONSTANTS.ROOK &&
                        this.board[CONSTANTS.F1] == 0 && this.board[CONSTANTS.G1] == 0) {
                    moves.add(new Move(CONSTANTS.E1,CONSTANTS.G1));
                }
                // white queenside
                if( this.board[CONSTANTS.E1] != 0 && this.canCastleWhiteQueen() && this.board[CONSTANTS.A1] != 0 &&
                        this.getPieceColorAt(CONSTANTS.E1) == CONSTANTS.WHITE &&
                        this.getPieceColorAt(CONSTANTS.A1) == CONSTANTS.WHITE &&
                        this.getPieceTypeAt(CONSTANTS.E1) == CONSTANTS.KING &&
                        this.getPieceTypeAt(CONSTANTS.A1) == CONSTANTS.ROOK
                        && this.board[CONSTANTS.D1]==0 && this.board[CONSTANTS.C1]==0 && this.board[CONSTANTS.B1]==0) {
                    moves.add(new Move(CONSTANTS.E1, CONSTANTS.C1));
                }
            }
            if(color == CONSTANTS.BLACK) {
                // black kingside
                if(this.board[CONSTANTS.E8] !=0 && this.canCastleBlackKing() && this.board[CONSTANTS.H8]!=0 &&
                        this.getPieceColorAt(CONSTANTS.E8) == CONSTANTS.BLACK &&
                        this.getPieceColorAt(CONSTANTS.H8) == CONSTANTS.BLACK &&
                        this.getPieceTypeAt(CONSTANTS.E8) == CONSTANTS.KING &&
                        this.getPieceTypeAt(CONSTANTS.H8) == CONSTANTS.ROOK &&
                        this.board[CONSTANTS.F8]==0 && this.board[CONSTANTS.G8]==0) {
                    moves.add(new Move(CONSTANTS.E8, CONSTANTS.G8));
                }
                // black queenside
                if(this.board[CONSTANTS.E8]!=0 && this.canCastleBlackQueen() && board[CONSTANTS.A8] !=0 &&
                        this.getPieceColorAt(CONSTANTS.E8) == CONSTANTS.BLACK &&
                        this.getPieceColorAt(CONSTANTS.A8) == CONSTANTS.BLACK &&
                        this.getPieceTypeAt(CONSTANTS.E8) == CONSTANTS.KING &&
                        this.getPieceTypeAt(CONSTANTS.A8) == CONSTANTS.ROOK &&
                        this.board[CONSTANTS.D8]==0 && this.board[CONSTANTS.C8]==0 && this.board[CONSTANTS.B8]==0) {
                    moves.add(new Move(CONSTANTS.E8, CONSTANTS.C8));
                }
            }
        }
        return moves;
    }

    // doesn't account for attacks via en-passent
    private boolean isAttacked(int idx, boolean attacker_color) {
        // first check for potential pawn attackers
        // attacker color white, pawn must be white.
        // lower left
        if(attacker_color == CONSTANTS.WHITE && (this.board[idx-9]!=0xFF && this.board[idx-9] != 0)
            && (this.getPieceColorAt(idx-9)==CONSTANTS.WHITE)
                && (this.getPieceTypeAt(idx-9)==CONSTANTS.PAWN)) {
            return true;
        }
        // lower right
        if(attacker_color == CONSTANTS.WHITE && (this.board[idx-11]!=0xFF && this.board[idx-11] != 0)
            && (this.getPieceColorAt(idx-11)==CONSTANTS.WHITE)
                && (this.getPieceTypeAt(idx-11)==CONSTANTS.PAWN)) {
            return true;
        }
        // check black, upper right
        if(attacker_color == CONSTANTS.BLACK && (this.board[idx+11]!=0xFF && this.board[idx+11] != 0)
            && (this.getPieceColorAt(idx+11)==CONSTANTS.BLACK)
                && (this.getPieceTypeAt(idx+11)==CONSTANTS.PAWN)) {
            return true;
        }
        // check black, upper left
        if(attacker_color == CONSTANTS.BLACK && (this.board[idx+9]!=0xFF && this.board[idx+9] != 0)
            && (this.getPieceColorAt(idx+9)==CONSTANTS.BLACK)
                && (this.getPieceTypeAt(idx+9)==CONSTANTS.PAWN)) {
            return true;
        }
        // check all squares (except idx itself)
        // for potential attackers
        for(int i=21;i<99;i++) {
            // skip empty squares
            if(i!=idx && this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                // can't attack yourself
                if(this.getPieceColorAt(i) == attacker_color) {
                    int piece = this.getPieceTypeAt(i);
                    int distance = idx - i;
                    if(distance < 0) {
                        distance = -distance;
                    }
                    int pot_attackers = CONSTANTS.ATTACK_TABLE[distance];
                    if((piece == CONSTANTS.KNIGHT && isKthBitSet(pot_attackers,0)) ||
                            (piece == CONSTANTS.BISHOP && isKthBitSet(pot_attackers,1)) ||
                            (piece == CONSTANTS.ROOK && isKthBitSet(pot_attackers,2)) ||
                            (piece == CONSTANTS.QUEEN && isKthBitSet(pot_attackers,3)) ||
                            (piece == CONSTANTS.KING && isKthBitSet(pot_attackers,4)))
                    {
                        // the target could be a potential attacker
                        // now just get all pseudo legal moves from i,
                        // excluding castling. If a move contains
                        // target idx, then we have an attacker
                        ArrayList<Move> targets = this.pseudoLegalMoves(i, CONSTANTS.ANY_SQUARE, CONSTANTS.ANY_PIECE, false, attacker_color);
                        for(int j=0;j<targets.size();j++) {
                            if(targets.get(j).to == idx) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isCastlesWking(Move m) {
        if(this.getPieceTypeAt(m.from) == CONSTANTS.KING
                && this.getPieceColorAt(m.from) == CONSTANTS.WHITE
                && m.from == CONSTANTS.E1 && m.to == CONSTANTS.G1) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCastlesWQueen(Move m) {
        if(this.getPieceTypeAt(m.from) == CONSTANTS.KING
                && this.getPieceColorAt(m.from) == CONSTANTS.WHITE
                && m.from == CONSTANTS.E1 && m.to == CONSTANTS.C1) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCastlesBking(Move m) {
        if(this.getPieceTypeAt(m.from) == CONSTANTS.KING
                && this.getPieceColorAt(m.from) == CONSTANTS.BLACK
                && m.from == CONSTANTS.E8 && m.to == CONSTANTS.G8) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCastlesBqueen(Move m) {
        if(this.getPieceTypeAt(m.from) == CONSTANTS.KING
                && this.getPieceColorAt(m.from) == CONSTANTS.BLACK
                && m.from == CONSTANTS.E8 && m.to == CONSTANTS.C8) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isPseudoALegal(Move m) {
        // a pseudo legal move is a legal move if
        // a) doesn't put king in check
        // b) if castle, must ensure that 1) king is not currently in check
        //                                2) castle over squares are not in check
        //                                3) doesn't castle into check
        // first find color of mover
        boolean color = this.getPieceColorAt(m.from);
        // find king with that color
        for(int i= 21;i<99;i++) {
            if(!(this.board[i] == CONSTANTS.EMPTY) && !(this.board[i] == CONSTANTS.FRINGE)) {
                if (this.getPieceTypeAt(i) == CONSTANTS.KING && this.getPieceColorAt(i) == color) {
                    // if the move is not by the king
                    if (i != m.from) {
                        // apply the move, check if king is attacked, and decide
                        //boolean legal = false;
                        //Board b_temp = this.makeCopy();
                        this.apply(m);      // note: if we do not copy here (performance) then legals() destorys our undo!
                        boolean legal = !this.isAttacked(i, !color);
                        this.undo();
                        return legal;
                    } else {
                        // means we move the king
                        // first check castle cases
                        if (this.isCastlesWking(m)) {
                            if (!this.isAttacked(CONSTANTS.E1, CONSTANTS.BLACK)
                                    && !this.isAttacked(CONSTANTS.F1, CONSTANTS.BLACK)
                                    && !this.isAttacked(CONSTANTS.G1, CONSTANTS.BLACK)) {
                                this.apply(m);
                                boolean legal = !this.isAttacked(CONSTANTS.G1, CONSTANTS.BLACK);
                                this.undo();
                                return legal;
                            } else {
                                return false;
                            }
                        }
                        if (this.isCastlesBking(m)) {
                            if (!this.isAttacked(CONSTANTS.E8, CONSTANTS.WHITE)
                                    && !this.isAttacked(CONSTANTS.F8, CONSTANTS.WHITE)
                                    && !this.isAttacked(CONSTANTS.G8, CONSTANTS.WHITE)) {
                                this.apply(m);
                                boolean legal = !this.isAttacked(CONSTANTS.G8, CONSTANTS.WHITE);
                                this.undo();
                                return legal;
                            } else {
                                return false;
                            }
                        }
                        if (this.isCastlesWQueen(m)) {
                            if (!this.isAttacked(CONSTANTS.E1, CONSTANTS.BLACK)
                                    && !this.isAttacked(CONSTANTS.D1, CONSTANTS.BLACK)
                                    && !this.isAttacked(CONSTANTS.C1, CONSTANTS.BLACK)) {
                                this.apply(m);
                                boolean legal = !this.isAttacked(CONSTANTS.C1, CONSTANTS.BLACK);
                                this.undo();
                                return legal;
                            } else {
                                return false;
                            }
                        }
                        if (this.isCastlesBqueen(m)) {
                            if (!this.isAttacked(CONSTANTS.E8, CONSTANTS.WHITE)
                                    && !this.isAttacked(CONSTANTS.D8, CONSTANTS.WHITE)
                                    && !this.isAttacked(CONSTANTS.C8, CONSTANTS.WHITE)) {
                                ArrayList<Move> targets = this.pseudoLegalMoves(CONSTANTS.F7, CONSTANTS.ANY_SQUARE, CONSTANTS.ANY_PIECE, false, CONSTANTS.WHITE);
                                this.apply(m);
                                boolean legal = !this.isAttacked(CONSTANTS.C8, CONSTANTS.WHITE);
                                this.undo();
                                return legal;
                            } else {
                                return false;
                            }
                        }
                        // if none of the castles cases triggered, we have a standard king move
                        // just check if king isn't attacked after applying the move
                        //boolean legal = false;
                        this.apply(m);
                        //Board b_temp = this.makeCopy();
                        boolean legal = !this.isAttacked(m.to, !color);
                        this.undo();
                        return legal;
                    }
                }
            }
        }
        return false;
    }


    public boolean isLegal(Move m) {
        ArrayList<Move> pseudoLegals = this.pseudoLegalMoves(m.from, m.to, CONSTANTS.ANY_PIECE, true, this.turn);
        for(Move mi : pseudoLegals) {
            if(mi.from == m.from && mi.to == m.to && mi.promotionPiece == m.promotionPiece) {
                if(isPseudoALegal(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isLegalAndPromotes(Move m) {
        ArrayList<Move> pseudoLegals = this.pseudoLegalMoves(m.from, m.to, CONSTANTS.ANY_PIECE, true, this.turn);
        for(Move mi : pseudoLegals) {
            if(mi.from == m.from && mi.to == m.to && mi.promotionPiece != 0) {
                if(isPseudoALegal(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Move> legalMoves() {

        ArrayList<Move> pseudoLegals = this.pseudoLegalMoves();
        // System.out.println("pseudoLegalSize: "+pseudoLegals.size());
        ArrayList<Move> legals = new ArrayList<Move>();
        for(Move mi : pseudoLegals) {
            try {
                if (this.isPseudoALegal(mi)) {
                    legals.add(mi);
                }
            } catch(IllegalArgumentException e) {
                int cnt_i = pseudoLegals.size();
                this.initPieceList();
                int cnt_i1 = this.pseudoLegalMoves().size();
                throw new IllegalArgumentException("err: before init pcl: "+ cnt_i + ", after: "+cnt_i1);
            }
        }
        return legals;
    }

    public ArrayList<Move> legaMovesTo(int internalToSquare, int pieceType) {

        ArrayList<Move> pseudoLegals = this.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, internalToSquare, pieceType, true, this.turn);
        ArrayList<Move> legals = new ArrayList<Move>();
        for(Move mi : pseudoLegals) {
            if(this.isPseudoALegal(mi)) {
                legals.add(mi);
            }
        }
        return legals;
    }

    public ArrayList<Move> legalMovesFrom(int internalFromSquare) {
        ArrayList<Move> pseudoLegals =
                this.pseudoLegalMoves(
                        internalFromSquare,
                        CONSTANTS.ANY_SQUARE,
                        CONSTANTS.ANY_PIECE,
                        true,
                        this.turn
                );
        ArrayList<Move> legals = new ArrayList<Move>();
        for(Move mi : pseudoLegals) {
            if(this.isPseudoALegal(mi)) {
                legals.add(mi);
            }
        }
        return legals;
    }

    public ArrayList<Move> legalsFromPseudos(ArrayList<Move> pseudos) {
        ArrayList<Move> legals = new ArrayList<Move>();
        for(Move mi : pseudos) {
            if(this.isPseudoALegal(mi)) {
                legals.add(mi);
            }
        }
        return legals;
    }

    public boolean isCheckmate() {
        // search for king of player with current turn
        // check whether king is attacked
        // check if player has no moves
        for(int i=21;i<99;i++) {
            if(this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                if(this.getPieceTypeAt(i) == CONSTANTS.KING && this.getPieceColorAt(i) == this.turn) {
                    if(this.isAttacked(i, !this.turn)) {
                        ArrayList<Move> legals = this.legalMoves();
                        return legals.size() == 0;
                    } else{
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public boolean isStalemate() {
        // search for king of player with current turn
        // check whether king is not attacked
        // check if player has no moves
        for(int i=21;i<99;i++) {
            if(this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                if(this.getPieceTypeAt(i) == CONSTANTS.KING && this.getPieceColorAt(i) == this.turn) {
                    if(!this.isAttacked(i, !this.turn)) {
                        ArrayList<Move> legals = this.legalMoves();
                        return legals.size() == 0;
                    } else{
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public boolean isCheck() {
        for(int i=21;i<99;i++) {
            if(this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                if (this.getPieceTypeAt(i) == CONSTANTS.KING && this.getPieceColorAt(i) == this.turn) {
                    return this.isAttacked(i, !this.turn);
                }
            }
        }
        return false;
    }

    public String san(Move m) {
        // first check for null move
        StringBuilder san = new StringBuilder();
        if(m.isNullMove) {
            return "--";
        }
        // first test for checkmate and check (to be appended later)
        // create temp board, since appyling move and
        // testing for checkmate (which again needs
        // application of a move) makes it impossible
        // to undo (undo can only be done once, not twice in a row)
        Board b_temp = this.makeCopy();
        b_temp.apply(m);
        boolean is_check = b_temp.isCheck();
        boolean is_checkmate = b_temp.isCheckmate();

        if(this.isCastlesWking(m) || this.isCastlesBking(m)) {
            san.append("O-O");
            if(is_checkmate) {
                san.append("#");
            }
            if(is_check) {
                san.append("+");
            }
            return san.toString();
        } else if(this.isCastlesWQueen(m) || this.isCastlesBqueen(m)) {
            san.append("O-O-O");
            if(is_checkmate) {
                san.append("#");
            } else if(is_check) {
                san.append("+");
            }
            return san.toString();
        } else {
            int pieceType = this.getPieceTypeAt(m.from);
            int piece = this.getPieceAt(m.from);
            if(pieceType == CONSTANTS.KNIGHT) {
                san.append("N");
            }
            if(pieceType == CONSTANTS.BISHOP) {
                san.append("B");
            }
            if(pieceType == CONSTANTS.ROOK) {
                san.append("R");
            }
            if(pieceType == CONSTANTS.QUEEN) {
                san.append("Q");
            }
            if(pieceType == CONSTANTS.KING) {
                san.append("K");
            }
            //QVector<Move> col_disambig;
            //QVector<Move> row_disambig;
            int thisRow = (m.from / 10) - 1;
            int thisCol = m.from % 10;

            ArrayList<Move> colDisAmbig = new ArrayList<>();
            ArrayList<Move> rowDisAmbig = new ArrayList<>();
            // find amibguous moves (except for pawns)
            if(pieceType != CONSTANTS.PAWN) {
                // if piece list contains only one piece, skip move generation
                // for testing disambiguity
                int color = 0;
                if(this.turn) {
                    color = 1;
                }
                if(this.pieceList[color][pieceType][1] != CONSTANTS.EMPTY) {
                    // otherwise we are finished as there is only one piece
                    ArrayList<Move> pseudos = this.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, m.to,
                            pieceType, false, this.turn);
                    if(pseudos.size() != 1) { // otherwise we are finished, as there is only one pseudo-legal move
                        ArrayList<Move> legals = this.legalsFromPseudos(pseudos);
                        if(legals.size() != 1) { // really need to resolve disambiguity
                            for(Move mi : legals) {
                                if(mi.from != m.from) { // skip the actual move to render
                                    if (mi.from % 10 != thisCol) {
                                        colDisAmbig.add(mi);
                                    } else {
                                        rowDisAmbig.add(mi);
                                    }
                                }
                            }
                        }
                    }
                }
                int cntColDisambig = colDisAmbig.size();
                int cntRowDisambig = rowDisAmbig.size();
                // if there is an ambiguity
                if(cntColDisambig != 0 || cntRowDisambig != 0) {
                    // preferred way: resolve via column
                    if(cntColDisambig>0 && cntRowDisambig==0) {
                        san.append((char) (thisCol + 96));
                        // if not try to resolve via row
                    } else if(cntRowDisambig>0 && cntColDisambig==0) {
                        san.append((char) (thisRow + 48));
                    } else {
                        // if that also fails (think three queens)
                        // resolve via full coordinate
                        san.append((char) (thisCol + 96));
                        san.append((char) (thisRow + 48));
                    }
                }
            }
            // handle a capture, i.e. if destination field
            // is not empty
            // in case of an en-passent capture, the destiation field
            // is empty. But then the destination field is the e.p. square
            if(this.board[m.to] != CONSTANTS.EMPTY || m.to == this.enPassentTarget) {
                if(pieceType == CONSTANTS.PAWN) {
                    san.append((char) (thisCol + 96));
                }
                san.append("x");
            }
            san.append(this.internalIdxToString(m.to));
            if(m.promotionPiece == CONSTANTS.KNIGHT) {
                san.append("=N");
            }
            if(m.promotionPiece == CONSTANTS.BISHOP) {
                san.append("=B");
            }
            if(m.promotionPiece == CONSTANTS.ROOK) {
                san.append("=R");
            }
            if(m.promotionPiece == CONSTANTS.QUEEN) {
                san.append("=Q");
            }
        }
        if(is_checkmate) {
            san.append("#");
        } else if(is_check) {
            san.append("+");
        }
        return san.toString();
    }

    public boolean isPromoting(Move m) {
        return m.promotionPiece > 0;
    }

    public boolean isInitialPosition() {
        if(!this.turn == CONSTANTS.WHITE) {
            return false;
        }
        for(int i=0;i<120;i++) {
            if(this.board[i] != CONSTANTS.INIT_POS[i]) {
                return false;
            }
        }
        if(!(this.canCastleWhiteKing() && this.canCastleBlackKing()
                && this.canCastleWhiteQueen() && this.canCastleBlackQueen())) {
            return false;
        }

        if(this.enPassentTarget != 0) {
            return false;
        }
        if(this.halfmoveClock != 0) {
            return false;
        }
        if(this.fullmoveNumber != 1) {
            return false;
        }
        if(this.undoAvailable) {
            return false;
        }
        return true;
    }

    public boolean canCastleWhiteKing() {
        return this.castleWkingOk;
    }

    public boolean canCastleBlackKing() {
        return this.castleBkingOk;
    }

    public boolean canCastleWhiteQueen() {
        return this.castleWqueenOk;
    }

    public boolean canCastleBlackQueen() {
        return this.castleBqueenOk;
    }

    public boolean isUndoAvailable() {
        return this.undoAvailable;
    }

    public void setCastleWKing(boolean canDo) {
        this.castleWkingOk = canDo;
    }

    public void setCastleBKing(boolean canDo) {
        this.castleBkingOk = canDo;
    }

    public void setCastleWQueen(boolean canDo) {
        this.castleWqueenOk = canDo;
    }

    public void setCastleBQueen(boolean canDo) {
        this.castleBqueenOk = canDo;
    }

    public void setPieceAt(int x, int y, int piece) {
        // check wether x,y is a valid location on chess board
        // and wether piece is a valid piece
        if(x>=0 && x<8 && y>=0 && y <8 &&
                ((piece >= 0x01 && piece <= 0x07) ||  // white piece
                 (piece >= 0x81 && piece <= 0x87) || (piece == 0x00))) // black piece or empty
        {
            int idx = this.xyToInternal(x,y);
            this.board[idx] = piece;
            // we need to recalculate the piece list, if the board
            // was manually modified
            this.initPieceList();
        } else {
            throw new IllegalArgumentException("called setPieceAt with invalid paramters, (x,y,piece): "+x+","+y+","+piece);
        }
    }

    public int getPieceAt(int x, int y) {
        if(x>=0 && x<8 && y>=0 && y <8) {
            int idx = this.xyToInternal(x,y);
            return this.board[idx];
        } else {
            throw new IllegalArgumentException("called getPieceAt with invalid paramters, (x,y): "+x+","+y);
        }
    }

    public boolean isPieceAt(int x, int y) {
        if (x >= 0 && x < 8 && y >= 0 && y < 8) {
            int idx = this.xyToInternal(x, y);
            if ((this.board[idx] >= CONSTANTS.WHITE_PAWN && this.board[idx] <= CONSTANTS.WHITE_KING) ||
                    (this.board[idx] >= CONSTANTS.BLACK_PAWN && this.board[idx] <= CONSTANTS.BLACK_KING)) {
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("called isPieceAt with invalid paramters, (x,y): "+x+","+y);
        }
    }

    public int getPieceAt(int internalCoordinate) {
        if(internalCoordinate <21 || internalCoordinate >98) {
            throw new IllegalArgumentException("out of range: " + internalCoordinate);
        }
        return this.board[internalCoordinate];
    }

    public int getPieceTypeAt(int x, int y) {
        if(x>=0 && x<8 && y>=0 && y <8) {
            int idx = this.xyToInternal(x,y);
            int piece = this.board[idx];
            if(piece >= 0x80) {
                return piece - 0x80;
            } else {
                return piece;
            }
        } else {
            throw new IllegalArgumentException("called get_piece_type_at with invalid paramters, (x,y): "+x + ","+y);
        }
    }

    public int getPieceTypeAt(int internalCoordinate) {
        if(internalCoordinate <21 || internalCoordinate >98) {
            throw new IllegalArgumentException("out of range: " + internalCoordinate);
        }
        int piece = this.board[internalCoordinate];
        if(piece == CONSTANTS.EMPTY) {
            throw new IllegalArgumentException("no piece: field is empty!");
        }
        if(piece == CONSTANTS.FRINGE) {
            throw new IllegalArgumentException("no piece: field in fringe!");
        }
        if(piece > 0x80) {
            return piece - 0x80;
        } else {
            return piece;
        }
    }

    public boolean getPieceColorAt(int x, int y) {
        int internalCoordinate = this.xyToInternal(x,y);
        return this.getPieceColorAt(internalCoordinate);
    }

    public boolean getPieceColorAt(int internalCoordinate) {
        if(this.board[internalCoordinate] > 0x80) {
            return CONSTANTS.BLACK;
        } else {
            return CONSTANTS.WHITE;
        }
    }

    public int getKingPos(boolean player) {
        for(int i=21;i<99;i++) {
            if(player == CONSTANTS.WHITE) {
                if(this.board[i] == CONSTANTS.WHITE_KING) {
                    return i;
                }
            } else {
                if(this.board[i] == CONSTANTS.BLACK_KING) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("there is no king on the board for supplied player!");
    }

    public boolean isConsistent() {
        int whiteKingPos = -1;
        int blackKingPos = -1;

        int cntWhiteKing = 0;
        int cntBlackKing = 0;

        int cntWhiteQueens = 0;
        int cntWhiteRooks = 0;
        int cntWhiteBishops = 0;
        int cntWhiteKnights = 0;
        int cntWhitePawns = 0;

        int cntBlackQueens = 0;
        int cntBlackRooks = 0;
        int cntBlackBishops = 0;
        int cntBlackKnights = 0;
        int cntBlackPawns = 0;

        for(int i=21;i<99;i++) {
            if(this.board[i] != CONSTANTS.EMPTY && this.board[i] != CONSTANTS.FRINGE) {
                int pieceType = this.getPieceTypeAt(i);
                boolean pieceColor = this.getPieceColorAt(i);
                if (pieceType == CONSTANTS.KING) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        whiteKingPos = i;
                        cntWhiteKing++;
                    } else {
                        blackKingPos = i;
                        cntBlackKing++;
                    }
                } else if (pieceType == CONSTANTS.QUEEN) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        cntWhiteQueens++;
                    } else {
                        cntBlackQueens++;
                    }
                } else if (pieceType == CONSTANTS.ROOK) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        cntWhiteRooks++;
                    } else {
                        cntBlackRooks++;
                    }
                } else if (pieceType == CONSTANTS.BISHOP) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        cntWhiteBishops++;
                    } else {
                        cntBlackBishops++;
                    }
                } else if (pieceType == CONSTANTS.KNIGHT) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        cntWhiteKnights++;
                    } else {
                        cntBlackKnights++;
                    }
                } else if (pieceType == CONSTANTS.PAWN) {
                    if (pieceColor == CONSTANTS.WHITE) {
                        if ((i / 10) == 2) { // white pawn in first rank
                            return false;
                        } else {
                            cntWhitePawns++;
                        }
                    } else {
                        if ((i / 10) == 9) { // black pawn in 8th rank
                            return false;
                        } else {
                            cntBlackPawns++;
                        }
                    }
                }
            }
        }
        // exactly one white and black king exist on board
        if(cntWhiteKing != 1 || cntBlackKing != 1) {
            return false;
        }
        // white and black king at least on field apart
        int larger = whiteKingPos;
        int smaller = blackKingPos;
        if(blackKingPos > whiteKingPos) {
            larger = blackKingPos;
            smaller = whiteKingPos;
        }
        int diff = larger - smaller;
        if(diff == 10 || diff == 1 || diff == 11 || diff == 9) {
            return false;
        }
        // side not to move must not be in check
        boolean notToMove = !this.turn;
        boolean toMove = this.turn;
        int idx_king_not_to_move = whiteKingPos;
        if(notToMove == CONSTANTS.BLACK) {
            idx_king_not_to_move = blackKingPos;
        }
        if(this.isAttacked(idx_king_not_to_move, toMove)) {
            return false;
        }
        // each side has 8 pawns or less
        if(cntWhitePawns > 8 || cntBlackPawns > 8) {
            return false;
        }
        // check whether no. of promotions and pawn count fits for white
        int whiteExtraPieces = Math.max(0, cntWhiteQueens-1) + Math.max(0, cntWhiteRooks-2)
        + Math.max(0, cntWhiteBishops - 2) + Math.max(0, cntWhiteKnights - 2);
        if(whiteExtraPieces > (8-cntWhitePawns)) {
            return false;
        }
        // ... for black
        int blackExtraPieces = Math.max(0, cntBlackQueens-1) + Math.max(0, cntBlackRooks-2)
        + Math.max(0, cntBlackBishops - 2) + Math.max(0, cntBlackKnights - 2);
        if(blackExtraPieces > (8-cntBlackPawns)) {
            return false;
        }
        // compare encoded castling rights of this board w/ actual
        // position of king and rook
        if(this.canCastleWhiteKing() && this.isWhiteKingCastleLost()) {
            return false;
        }
        if(this.canCastleWhiteQueen() && this.isWhiteQueenCastleLost()) {
            return false;
        }
        if(this.canCastleBlackKing() && this.isBlackKingCastleLost()) {
            return false;
        }
        if(this.canCastleBlackQueen() && this.isBlackQueenCastleLost()) {
            return false;
        }
        return true;
    }

    public boolean isBlackKingCastleLost() {
        if(this.board[CONSTANTS.E8] == CONSTANTS.BLACK_KING &&
        this.board[CONSTANTS.H8] == CONSTANTS.BLACK_ROOK) {
            return false;
        }
        return true;
    }

    public boolean isBlackQueenCastleLost() {
        if(this.board[CONSTANTS.E8] == CONSTANTS.BLACK_KING &&
                this.board[CONSTANTS.A8] == CONSTANTS.BLACK_ROOK) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isWhiteKingCastleLost() {
        if(this.board[CONSTANTS.E1] == CONSTANTS.WHITE_KING &&
                this.board[CONSTANTS.H1] == CONSTANTS.WHITE_ROOK) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isWhiteQueenCastleLost() {
        if(this.board[CONSTANTS.E1] == CONSTANTS.WHITE_KING &&
                this.board[CONSTANTS.A1] == CONSTANTS.WHITE_ROOK) {
            return false;
        } else {
            return true;
        }
    }

    public int getEpTarget() {
        return this.enPassentTarget;
    }

    boolean canClaimFiftyMoves() {
        return this.halfmoveClock >= 100;
    }

    private int zobristPieceType(int piece) {
        switch (piece)
        {
            case CONSTANTS.BLACK_PAWN:
                return 0;
            case CONSTANTS.WHITE_PAWN:
                return 1;
            case CONSTANTS.BLACK_KNIGHT:
                return 2;
            case CONSTANTS.WHITE_KNIGHT:
                return 3;
            case CONSTANTS.BLACK_BISHOP:
                return 4;
            case CONSTANTS.WHITE_BISHOP:
                return 5;
            case CONSTANTS.BLACK_ROOK:
                return 6;
            case CONSTANTS.WHITE_ROOK:
                return 7;
            case CONSTANTS.BLACK_QUEEN:
                return 8;
            case CONSTANTS.WHITE_QUEEN:
                return 9;
            case CONSTANTS.BLACK_KING:
                return 10;
            case CONSTANTS.WHITE_KING:
                return 11;
        }
        throw new IllegalArgumentException("piece type out of range in ZobristHash:kind_of_piece");
    }


    public long getZobrist() {
        if (this.zobristInitialized) {
            return this.zobristHash;
        } else {
            long piece = 0L;
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    int internalCoordinate = this.xyToInternal(i, j);
                    if (this.board[internalCoordinate] != CONSTANTS.EMPTY) {
                        int pieceAt_ij = this.getPieceAt(internalCoordinate);
                        int kind_of_piece = this.zobristPieceType(pieceAt_ij);
                        int offset_piece = 64 * kind_of_piece + 8 * j + i;
                        piece = piece ^ CONSTANTS.POLYGLOT_RANDOM_64[offset_piece];
                    }
                }
            }
            long enPassent = 0L;
            int epTarget = this.getEpTarget();
            if (epTarget != 0) {
                int file = (epTarget % 10) - 1;
                // check if left or right is a pawn from player to move
                if (this.turn == CONSTANTS.WHITE) {
                    int left = this.getPieceAt(epTarget - 11);
                    int right = this.getPieceAt(epTarget - 9);
                    if (left == CONSTANTS.WHITE_PAWN || right == CONSTANTS.WHITE_PAWN) {
                        enPassent = CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_EN_PASSENT + file];
                    }
                } else {
                    int left = this.getPieceAt(epTarget + 11);
                    int right = this.getPieceAt(epTarget + 9);
                    if (left == CONSTANTS.BLACK_PAWN || right == CONSTANTS.BLACK_PAWN) {
                        enPassent = CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_EN_PASSENT + file];
                    }
                }
            }
            long castle = 0L;
            if (this.canCastleWhiteKing()) {
                castle = castle ^ CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_CASTLE];
            }
            if (this.canCastleWhiteQueen()) {
                castle = castle ^ CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_CASTLE + 1];
            }
            if (this.canCastleBlackKing()) {
                castle = castle ^ CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_CASTLE + 2];
            }
            if (this.canCastleBlackKing()) {
                castle = castle ^ CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_CASTLE + 3];
            }

            long turn = 0L;
            if (this.turn == CONSTANTS.WHITE) {
                turn = CONSTANTS.POLYGLOT_RANDOM_64[CONSTANTS.RANDOM_TURN];
            }

            this.zobristHash = piece ^ castle ^ enPassent ^ turn;
            return this.zobristHash;
        }
    }

    public long getPositionHash() {
        if(this.posHashInitialized) {
            return this.positionHash;
        } else {
            long piece = 0L;
            for(int i=0;i<8;i++) {
                for(int j=0;j<8;j++) {
                    int pieceAt_ij = this.getPieceAt(i,j);
                    if(pieceAt_ij != CONSTANTS.EMPTY) {
                        int kindOfPiece = this.zobristPieceType(pieceAt_ij);
                        int offset_piece = 64 * kindOfPiece + 8 * j + i;
                        piece = piece^CONSTANTS.POLYGLOT_RANDOM_64[offset_piece];
                    }
                }
            }
            this.positionHash = piece;
            return this.positionHash;
        }
    }

    int pieceFromSymbol(char c) {
        if(c == 'K') {
            return 0x06;
        }
        if(c == 'Q') {
            return 0x05;
        }
        if(c == 'R') {
            return 0x04;
        }
        if(c == 'B') {
            return 0x03;
        }
        if(c == 'N') {
            return 0x02;
        }
        if(c == 'P') {
            return 0x01;
        }
        if(c == 'k') {
            return 0x86;
        }
        if(c == 'q') {
            return 0x85;
        }
        if(c == 'r') {
            return 0x84;
        }
        if(c == 'b') {
            return 0x83;
        }
        if(c == 'n') {
            return 0x82;
        }
        if(c == 'p') {
            return 0x81;
        }
        throw new IllegalArgumentException("piece to symbol: unknown input: "+c);
    }

    public boolean isKthBitSet(int n, int k) {
        // 1.) shift k positions to the right ( n >> k ) into tempVal
        // 2.) do bitwise AND with 000000....1 (int 1), i.e. res = tempVal & 1
        // 3.) if rightmost bit was set (LSB), result should be 1
        return ((n >> k) & 1) == 1;
    }

    private char pieceToSymbol(int piece) {
        if(piece == CONSTANTS.WHITE_KING) {
            return 'K';
        }
        if(piece == CONSTANTS.WHITE_QUEEN) {
            return 'Q';
        }
        if(piece == CONSTANTS.WHITE_ROOK) {
            return 'R';
        }
        if(piece == CONSTANTS.WHITE_BISHOP) {
            return 'B';
        }
        if(piece == CONSTANTS.WHITE_KNIGHT) {
            return 'N';
        }
        if(piece == CONSTANTS.WHITE_PAWN) {
            return 'P';
        }
        if(piece == CONSTANTS.BLACK_KING) {
            return 'k';
        }
        if(piece == CONSTANTS.BLACK_QUEEN) {
            return 'q';
        }
        if(piece == CONSTANTS.BLACK_ROOK) {
            return 'r';
        }
        if(piece == CONSTANTS.BLACK_BISHOP) {
            return 'b';
        }
        if(piece == CONSTANTS.BLACK_KNIGHT) {
            return 'n';
        }
        if(piece == CONSTANTS.BLACK_PAWN) {
            return  'p';
        }
        throw new IllegalArgumentException("called piece to symbol with unkown value: "+piece);
    }

    @Override
    public String toString() {
        String s = "";
        for(int i=90;i>=20;i-=10) {
            for(int j=1;j<9;j++) {
                int piece = this.board[i+j];
                if(piece != CONSTANTS.EMPTY) {
                    s += this.pieceToSymbol(piece);
                } else {
                    if(this.enPassentTarget == (i+j)) {
                        s += ":";
                    } else {
                        s += ".";
                    }
                }
            }
            s += "\n";
        }
        return s;
    }

    private void initPieceList() {

        for(int i=0;i<7;i++) {
            for(int j=0;j<10;j++) {
                this.pieceList[CONSTANTS.IWHITE][i][j] = CONSTANTS.EMPTY;
                this.pieceList[CONSTANTS.IBLACK][i][j] = CONSTANTS.EMPTY;
            }
        }
        for(int i=21;i<99;i++) {
            int piece = this.board[i];
            if(!(piece == CONSTANTS.EMPTY) && !(piece == 0xFF)) {
                int color = CONSTANTS.IWHITE;
                if(piece > 0x80) {
                    piece = piece - 0x80;
                    color = CONSTANTS.IBLACK;
                }
                // piece contains now the piece type
                for(int j=0;j<10;j++) {
                    if(this.pieceList[color][piece][j] == CONSTANTS.EMPTY) {
                        this.pieceList[color][piece][j] = i;
                        break;
                    }
                }
            }
        }
    }
    
    public int getPieceType(int piece)
    {
      return piece & 0x0000000F;
    }

    public int getSquareColorAt(int x, int y)
    {
      // optional TODO: Check that x and y values are within range 0..7.
      if ((x+y)%2 == 0) {
          return CONSTANTS.IBLACK;
      }
      return CONSTANTS.IWHITE;
    }
    
    public boolean isInsufficientMaterial() {
        int pCounter = 0;
        int lastPiece = CONSTANTS.EMPTY;
        int lastPieceSquareColor = CONSTANTS.IWHITE; // just default
        
        // Scan the board:
        for(int x=0;x<8;x++) {
            for(int y=0;y<8;y++) {
                int piece = getPieceAt(x,y);
                if (piece == CONSTANTS.EMPTY)
                    continue;
                
                switch (getPieceType(piece)) {
                    case CONSTANTS.KING:
                        continue;

                    case CONSTANTS.PAWN:
                    case CONSTANTS.QUEEN:
                    case CONSTANTS.ROOK:
                        // Possible to force mate.
                        return false;

                    case CONSTANTS.KNIGHT:
                        if(++pCounter > 1) {
                            // More than a knight:
                            // Theoretically possible to mate or to force mate,
                            // depending on the color of the pieces.
                            return false;
                        }
                        // This is the first knight that we find:
                        // Save it. (not necessary)
                        lastPiece = piece;
                        break;

                    case CONSTANTS.BISHOP:
                        if(++pCounter > 2) {
                            // More than two bishops):
                            // Theroetically possible to mate or to force mate.
                            return false;
                        }
                        else if (pCounter > 1)
                        {
                            // We hsve found another piece (knight or bishop) 
                            // before this one:
                            if (getPieceType(lastPiece) != CONSTANTS.BISHOP)
                                // A knight and a bishop:
                                // Theoretically possible to mate
                                // or to force mate.
                                return false;
                            
                            // Two bishops:
                            if (getPieceAt(x,y) == lastPiece)
                                // Two Bishops with the same "pieceColor":
                                // Possible to force checkmate.
                                return false;
                            
                            // One bishop each:
                            if (getSquareColorAt(x,y) != lastPieceSquareColor)
                                // One bishop each on squares of different
                                // colors: Theoretically Possible to mate.
                                return false;
                        }
                        else
                        {
                            // This is the first piece (bishop) that we find: Save it.
                            lastPieceSquareColor = getSquareColorAt(x,y);
                            lastPiece = piece;
                        }
                        break;
                }
            }
        }
        return true;
    }
}

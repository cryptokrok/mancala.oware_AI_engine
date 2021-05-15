package com.joansala.engine.base;

/*
 * Aalina oware engine.
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import com.joansala.engine.Game;


/**
 * Abstract game logic implementation.
 */
public abstract class BaseGame implements Game {

    /** Maximum score to which states are evaluated */
    public static final int MAX_SCORE = 1000;

    /** Recommended score to evaluate draws */
    public static final int CONTEMPT_SCORE = 0;

    /** Default capacity of this object */
    public static final int DEFAULT_CAPACITY = 254;

    /** Number of moves this game can store */
    protected int capacity;

    /** Current state index */
    protected int index;

    /** Player to move on current state */
    protected int turn;

    /** Performed move to reach current state */
    protected int move;

    /** Performed moves array */
    protected int[] moves;

    /** Current state hash code */
    protected long hash;


    /**
     * Instantiate a new game on the start state.
     */
    public BaseGame() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Instantiate a new game on the start state.
     *
     * @param capacity      Initial capacity
     */
    public BaseGame(int capacity) {
        this.index = -1;
        this.turn = Game.SOUTH;
        this.move = Game.NULL_MOVE;
        this.moves = new int[capacity];
        this.capacity = capacity;
        this.setStart(startPosition(), Game.SOUTH);
        this.hash = computeHash();
    }


    /**
     * Computes a unique hash for the current state.
     *
     * @return          Unique hash code
     */
    protected abstract long computeHash();


    /**
     * Sets a new initial position and turn for this game.
     *
     * @param position  Start position
     * @param turn      Start turn
     */
    protected abstract void resetState(Object position, int turn);


    /**
     * Obtain the initial position of the game.
     *
     * @return          New position object
     */
    protected abstract Object startPosition();


    /**
     * Check if an object is a valid position for this game.
     *
     * @param position  Position instance
     * @return          If position is valid
     */
    protected abstract boolean isPosition(Object position);


    /**
     * {@inheritDoc}
     */
    public abstract Object position();


    /**
     * {@inheritDoc}
     */
    public abstract boolean hasEnded();


    /**
     * {@inheritDoc}
     */
    public abstract int winner();


    /**
     * {@inheritDoc}
     */
    public abstract int outcome();


    /**
     * {@inheritDoc}
     */
    public abstract int score();


    /**
     * {@inheritDoc}
     */
    public abstract int getCursor();


    /**
     * {@inheritDoc}
     */
    public abstract void setCursor(int cursor);


    /**
     * {@inheritDoc}
     */
    public abstract void resetCursor();


    /**
     * {@inheritDoc}
     */
    public abstract void makeMove(int move);


    /**
     * {@inheritDoc}
     */
    public abstract void unmakeMove();


    /**
     * {@inheritDoc}
     */
    public abstract int nextMove();


    /**
     * {@inheritDoc}
     */
    public abstract void ensureCapacity(int size);


    /**
     * {@inheritDoc}
     */
    public void endMatch() {
        // Does nothing
    }


    /**
     * {@inheritDoc}
     */
    public int length() {
        return 1 + index;
    }


    /**
     * {@inheritDoc}
     */
    public int turn() {
        return turn;
    }


    /**
     * {@inheritDoc}
     */
    public long hash() {
        return hash;
    }


    /**
     * {@inheritDoc}
     */
    public int contempt() {
        return CONTEMPT_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    public int infinity() {
        return MAX_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    public void setStart(Object position, int turn) {
        validateTurn(turn);
        validatePosition(position);
        resetState(position, turn);
    }


    /**
     * {@inheritDoc}
     */
    public int[] moves() {
        int[] moves = new int[length()];

        if (index >= 0) {
            System.arraycopy(this.moves, 1, moves, 0, index);
            moves[index] = this.move;
        }

        return moves;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLegal(int move) {
        for (int house : legalMoves()) {
            if (house == move) {
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public int[] legalMoves() {
        int length = 0;
        int move = NULL_MOVE;

        final int cursor = getCursor();
        final int[] moves = new int[6];

        resetCursor();

        while ((move = nextMove()) != NULL_MOVE) {
            moves[length++] = move;
        }

        setCursor(cursor);

        return Arrays.copyOf(moves, length);
    }


    /**
     * {@inheritDoc}
     */
    public int toCentiPawns(int score) {
        return score;
    }


    /**
     * {@inheritDoc}
     */
    public Game cast() {
        return this;
    }


    /**
     * Asserts a value represents a valid turn for this game.
     *
     * @throws IllegalArgumentException If not valid
     */
    protected void validateTurn(int turn) {
        if (turn != SOUTH && turn != NORTH) {
            throw new IllegalArgumentException(
                "Game turn is not a valid");
        }
    }


    /**
     * Asserts a value represents a valid position for this game.
     *
     * @throws IllegalArgumentException If not valid
     */
    protected void validatePosition(Object position) {
        if (isPosition(position) == false) {
            throw new IllegalArgumentException(
                "Game position is not valid");
        }
    }
}

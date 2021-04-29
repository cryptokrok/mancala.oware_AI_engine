package com.joansala.engine.negamax;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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

import com.google.inject.Inject;
import java.util.function.Consumer;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.joansala.engine.*;


/**
 * Implements a game engine using a negamax framework.
 *
 * @author    Joan Sala Soler
 * @version   1.1.0
 */
public class Negamax implements Engine {

    /** The maximum depth allowed for a search */
    public static final int MAX_DEPTH = 254;

    /** The minimum depth allowed for a search */
    public static final int MIN_DEPTH = 2;

    /** An exact score was returned */
    public static final int EXACT = 0;

    /** An heuristic score may have been returned */
    public static final int FUZZY = 1;

    /** Search timer */
    private final Timer timer;

    /** References the {@code Game} to search */
    private Game game = null;

    /** The transpositions table */
    private Cache cache = null;

    /** Endgame database */
    private Leaves leaves = null;

    /** Consumer of best moves */
    private Set<Consumer<Report>> consumers = new HashSet<>();

    /** The maximum depth allowed for the current search */
    private int maxDepth = MAX_DEPTH;

    /** The maximum time allowed for the current search */
    private long moveTime = DEFAULT_MOVETIME;

    /** The maximum possible score value */
    private int maxScore = Integer.MAX_VALUE;

    /** The minimum possible score value */
    private int minScore = -Integer.MAX_VALUE;

    /** Contempt factor used to evaluaty draws */
    private int contempt = Game.DRAW_SCORE;

    /** Holds the best score found so far */
    private int bestScore = Integer.MAX_VALUE;

    /** Depth of the last completed search */
    private int scoreDepth = 0;

    /** This flag is set to true to abort a computation */
    private volatile boolean aborted = false;


    /**
     * Initializes a new {@code Negamax} object.
     */
    public Negamax() {
        this.timer = new Timer(true);
        this.cache = dummyCache;
        this.leaves = dummyLeaves;
    }


    /**
     * Returns the maximum depth allowed for the search
     *
     * @return   The depth value
     */
    public int getDepth() {
        return maxDepth;
    }


    /**
     * Returns the maximum time allowed for a move computation
     * in milliseconds
     *
     * @return   The new search time in milliseconds
     */
    public long getMoveTime() {
        return moveTime;
    }


    /**
     * Returns current the comptempt factor of the engine.
     */
    public int getContempt() {
        return contempt;
    }


    /**
     * Returns the current infinity score of the engine.
     */
    public int getInfinity() {
        return maxScore;
    }


    /**
     * Depth of the last completed search iteration.
     */
    public int getScoreDepth() {
        return scoreDepth;
    }


    /**
     * {@inheritDoc}
     */
    public int getPonderMove(Game game) {
        int move = Game.NULL_MOVE;

        if (cache != null && cache.find(game)) {
            if (cache.getFlag() == Flag.EXACT) {
                move = cache.getMove();
            }
        }

        return move;
    }


    /**
     * Sets the maximum depth for subsequent computations
     *
     * @param depth  The new depth value as an even number lower
     *               or equal to {@code Negamax.MAX_DEPTH}
     */
    public synchronized void setDepth(int depth) {
        // Set new depth as an even value

        if (depth > MAX_DEPTH) {
            this.maxDepth = MAX_DEPTH;
        } else if (depth < MIN_DEPTH) {
            this.maxDepth = MIN_DEPTH;
        } else {
            this.maxDepth = depth + depth % 2;
        }
    }


    /**
     * Sets the maximum time allowed for subsequent computations
     *
     * @param delay    The new time value in milliseconds as a
     *                 positive number greater than zero
     */
    public synchronized void setMoveTime(long delay) {
        if (delay > 0) {
            this.moveTime = delay;
        } else {
            throw new IllegalArgumentException(
                "Move time must be a positive number");
        }
    }


    /**
     * Sets the contempt factor. That is the score to which end game
     * positions that are draw will be evaluated.
     *
     * @param score     Score for draw positions
     */
    public synchronized void setContempt(int score) {
        this.contempt = score;
    }


    /**
     * Sets the infinity score. Setting this value to the maximum score
     * a game object can possibly be evaluated will improve the engine
     * performance by producing more cut-offs.
     *
     * @param score     Infinite value as apositive integer
     */
    public synchronized void setInfinity(int score) {
        if (score > 0) {
            this.maxScore = score;
            this.minScore = -score;
        } else {
            throw new IllegalArgumentException(
                "Infinity must be a positive number");
        }
    }


    /**
     * Sets the transposition table to use.
     *
     * @param cache     A cache object or {@code null} to disable
     *                  the transposition table
     */
    @Inject(optional=true)
    public synchronized void setCache(Cache cache) {
        this.cache = (cache != null) ? cache : dummyCache;
    }


    /**
     * Sets the endgames database to use.
     *
     * @param leaves    A leaves object or {@code null} to disable
     *                  the use of precomputed endgames
     */
    @Inject(optional=true)
    public synchronized void setLeaves(Leaves leaves) {
        this.leaves = (leaves != null) ? leaves : dummyLeaves;
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void attachConsumer(Consumer<Report> consumer) {
        consumers.add(consumer);
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void detachConsumer(Consumer<Report> consumer) {
        consumers.remove(consumer);
    }


    /**
     * Tells the engine that the next positions are going to be from
     * a different match.
     */
    public synchronized void newMatch() {
        cache.clear();
        timer.purge();
    }


    /**
     * Aborts the current search
     */
    public void abortComputation() {
        aborted = true;
        synchronized (this) {
            aborted = false;
        }
    }


    /**
     * Computes a best move for the current position of a game and
     * returns its score. A positive score means an advantage for the
     * player to move.
     *
     * @param game  The game for which a score must be computed
     * @return      The best score found for the current game position
     */
    public synchronized int computeBestScore(Game game) {
        computeBestMove(game);
        return -bestScore;
    }


    /**
     * Computes a best move for the current position of a game.
     *
     * <p>Note that the search is performed on the provided game object,
     * thus, the game object will change during the computation and its
     * capacity may be increased. The provided game object must not be
     * manipulated while a computation is ongoing.</p>
     *
     * @param game  The game for which a best move must be computed
     * @return      The best move found for the current game position
     *              or {@code Game.NULL_MOVE} if the game already ended
     */
    public synchronized int computeBestMove(Game game) {
        this.game = game;

        // If the game ended on that position return a null move
        // and set the best score acordingly

        if (game.hasEnded()) {
            bestScore = -(game.outcome() * game.turn());
            return Game.NULL_MOVE;
        }

        // Start countdown

        final TimerTask countDown = new TimerTask() {
            public void run() {
                aborted = true;
            }
        };

        timer.schedule(countDown, moveTime);

        // Get ready for the move computation

        game.ensureCapacity(MAX_DEPTH + game.length());
        cache.discharge();

        // Compute all legal moves for the game

        int[] rootMoves = game.legalMoves();

        // Check for a hash move and reorder moves accordingly

        if (cache.find(game) && cache.getMove() != Game.NULL_MOVE) {
            final int hashMove = cache.getMove();

            for (int index = 0; index < 6; index++) {
                if (rootMoves[index] == hashMove) {
                    System.arraycopy(rootMoves, 0, rootMoves, 1, index);
                    rootMoves[0] = hashMove;
                    break;
                }
            }
        }

        // Iterative deepening search for a best move

        int score;
        int beta = maxScore;
        int depth = MIN_DEPTH;
        int lastScore = maxScore;
        int lastMove = Game.NULL_MOVE;
        int bestMove = rootMoves[0];

        bestScore = Game.DRAW_SCORE;
        scoreDepth = 0;

        while (!aborted || depth == MIN_DEPTH) {
            for (int move : rootMoves) {
                game.makeMove(move);
                score = search(minScore, beta, depth);
                game.unmakeMove();

                if (aborted && depth > MIN_DEPTH) {
                    bestMove = lastMove;
                    bestScore = lastScore;
                    break;
                }

                if (score < beta) {
                    bestMove = move;
                    bestScore = score;
                    beta = score;
                } else if (score == beta) {
                    bestScore = score;
                }
            }

            // Stop if an exact score was found

            if (!aborted || depth == MIN_DEPTH) {
                scoreDepth = depth;
            }

            if (Math.abs(bestScore) == maxScore) {
                break;
            }

            // Stop on timeout elaspe or maximum recursion

            if (aborted || depth >= maxDepth) {
                break;
            }

            // Create a report of the current search results

            if (depth > MIN_DEPTH) {
                if (bestMove != lastMove || bestScore != lastScore) {
                    invokeConsumers(game, bestMove);
                }
            }

            // She's heading for the disco…

            beta = maxScore;
            lastMove = bestMove;
            lastScore = bestScore;
            depth += 2;
        }

        invokeConsumers(game, bestMove);
        countDown.cancel();
        aborted = false;

        return bestMove;
    }


    /**
     * Performs a recursive search for a best move
     *
     * @param alpha  The propagated alpha value
     * @param beta   The propagated beta value
     * @param depth  Search depth of the node. Defines the maximum number
     *               of recursive calls that could be made for the node
     */
    private int search(int alpha, int beta, int depth) {
        if (aborted && depth > MIN_DEPTH) {
            return minScore;
        }

        // Return the utility score of the node

        if (game.hasEnded()) {
            final int score = game.outcome();

            return (score == Game.DRAW_SCORE) ?
                contempt * game.turn() : score * game.turn();
        }

        // Return an endgame score if possible

        if (leaves.find(game)) {
            final int score = leaves.getScore();

            return (score == Game.DRAW_SCORE) ?
                contempt * game.turn() : score * game.turn();
        }

        // Return the heuristic score of the node

        if (depth == 0) {
            return game.score() * game.turn();
        }

        // Hash table lookup

        int hashMove = Game.NULL_MOVE;

        if (depth > 2 && cache.find(game)) {
            // Check for a possible cut-off

            if (cache.getDepth() >= depth) {
                switch (cache.getFlag()) {
                    case Flag.UPPER:
                        if (cache.getScore() >= beta)
                            return beta;
                        break;
                    case Flag.LOWER:
                        if (cache.getScore() <= alpha)
                            return alpha;
                        break;
                    case Flag.EXACT:
                        return cache.getScore();
                }
            }

            // Get the hash move

            hashMove = cache.getMove();
        }

        // Initialize score and flags

        int score = minScore;
        int flag = Flag.LOWER;

        // Try the hash move first

        if (hashMove != Game.NULL_MOVE) {
            game.makeMove(hashMove);
            score = -search(-beta, -alpha, depth - 1);
            game.unmakeMove();

            if (score >= beta && aborted == false) {
                cache.store(game, score, hashMove, depth, Flag.UPPER);
                return beta;
            }

            if (score > alpha) {
                alpha = score;
                flag = Flag.EXACT;
            }
        }

        // Iterate through generated moves

        int cmove;

        while ((cmove = game.nextMove()) != Game.NULL_MOVE) {
            if (cmove == hashMove)
                continue;

            game.makeMove(cmove);
            score = -search(-beta, -alpha, depth - 1);
            game.unmakeMove();

            if (score >= beta) {
                alpha = beta;
                hashMove = cmove;
                flag = Flag.UPPER;
                break;
            }

            if (score > alpha) {
                alpha = score;
                hashMove = cmove;
                flag = Flag.EXACT;
            }
        }

        // Store the transposition ignoring pre-frontier subtrees

        if (depth > 2 && aborted == false) {
            cache.store(game, alpha, hashMove, depth, flag);
        }

        // Return the best score found

        return alpha;
    }


    /**
     * Notifies registered consumers of a state change.
     *
     * @param game          Game state before a search
     * @param bestMove      Best move found so far
     */
    protected void invokeConsumers(Game game, int bestMove) {
        Report report = new CacheReport(game, cache, bestMove);

        for (Consumer<Report> consumer : consumers) {
            consumer.accept(report);
        }
    }


    /**
     * Implements a dummy cache that does not store or return any entries.
     * This is implemented for efficiency to allow games without a cache
     * to use this engine.
     */
    private final Cache dummyCache = new Cache() {
        public long size() { return 0L; }
        public int getScore() { return 0; }
        public int getMove() { return Game.NULL_MOVE; }
        public int getDepth() { return 0; }
        public int getFlag() { return Flag.EMPTY; }
        public boolean find(Game g) { return false; }
        public void store(Game g, int s, int m, int d, int f) { }
        public void discharge() { }
        public void resize(long m) { }
        public void clear() { }
    };


    /**
     * Implements a dummy endgames database that does not contain entries.
     */
    private final Leaves dummyLeaves = new Leaves() {
        public int getScore() { return 0; }
        public int getFlag() { return Flag.EMPTY; }
        public boolean find(Game g) { return false; }
    };
}

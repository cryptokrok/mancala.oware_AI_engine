package com.joansala.engine.base;

/*
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

import com.joansala.engine.Flag;
import com.joansala.engine.Game;
import com.joansala.engine.Cache;


/**
 * A cache that does not store or return any entries.
 */
public class BaseCache implements Cache {

    /** {@inheritDoc} */
    public long size() { return 0L; }

    /** {@inheritDoc} */
    public int getScore() { return 0; }

    /** {@inheritDoc} */
    public int getMove() { return Game.NULL_MOVE; }

    /** {@inheritDoc} */
    public int getDepth() { return 0; }

    /** {@inheritDoc} */
    public int getFlag() { return Flag.EMPTY; }

    /** {@inheritDoc} */
    public boolean find(Game g) { return false; }

    /** {@inheritDoc} */
    public void store(Game game, int score, int move, int depth, int flag) {}

    /** {@inheritDoc} */
    public void resize(long memory) {}

    /** {@inheritDoc} */
    public void discharge() {}

    /** {@inheritDoc} */
    public void clear() {}
}

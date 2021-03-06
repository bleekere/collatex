/*
 * Copyright (c) 2015 The Interedition Development Group.
 *
 * This file is part of CollateX.
 *
 * CollateX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CollateX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.collatex.medite;

import eu.interedition.collatex.AbstractTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 */
public class SuffixTreeTest extends AbstractTest {

    @Test
    public void suffixTree() {
        final SuffixTree<String> st = SuffixTree.build(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        }, "S", "P", "O", "a", "s", "p", "o");

        LOG.fine(() -> st.toString());
        LOG.fine(() -> StreamSupport.stream(st.match(Arrays.asList("s", "p", "o", "a")).spliterator(), false).map(Object::toString).collect(Collectors.joining(", ")));
    }

}

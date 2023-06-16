/*
 * Copyright 2006 and onwards Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ziplet.filter.compression;

import com.github.ziplet.filter.compression.statistics.CompressingFilterStats;
import com.github.ziplet.filter.compression.statistics.CompressingFilterStatsImpl;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Tests {@link StatsInputStream}.
 *
 * @author Sean Owen
 * @since 1.6
 */
public final class StatsInputStreamTest extends TestCase {

    private ByteArrayInputStream bais;
    private MockStatsInputStream statsIn;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        bais = new ByteArrayInputStream(new byte[100]);
        statsIn = new MockStatsInputStream(bais, new CompressingFilterStatsImpl(),
                StatsField.REQUEST_INPUT_BYTES);
    }

    public void testStats() throws Exception {
        assertEquals(0, statsIn.read());
        assertEquals(1, statsIn.getTotalBytesRead());
        assertEquals(10, statsIn.read(new byte[10]));
        assertEquals(11, statsIn.getTotalBytesRead());
        assertEquals(5, statsIn.read(new byte[10], 0, 5));
        assertEquals(16, statsIn.getTotalBytesRead());
        statsIn.close();
    }

    private static final class MockStatsInputStream extends StatsInputStream {

        public MockStatsInputStream(InputStream inputStream, CompressingFilterStats stats,
                                    StatsField field) {
            super(inputStream, stats, field);
        }

        public long getTotalBytesRead() {
            return ((CompressingFilterStatsImpl) this.stats).getRequestInputBytes();
        }
    }
}

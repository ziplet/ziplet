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

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} that decorates another {@link InputStream} and notes when bytes are read
 * from the stream. Callers create an instance of {@link StatsInputStream} with an instance of
 * {@link InputStream}, which receives notification of reads. This information might be used to
 * tally the number of bytes read from a stream.
 *
 * @author Sean Owen
 * @since 1.6
 */
public class StatsInputStream extends InputStream {

    protected final InputStream inputStream;
    protected final CompressingFilterStats stats;
    protected final StatsField field;

    public StatsInputStream(InputStream inputStream, CompressingFilterStats stats,
                            StatsField field) {
        assert inputStream != null && stats != null;
        this.inputStream = inputStream;
        this.stats = stats;
        this.field = field;
    }

    @Override
    public int read() throws IOException {
        int result = inputStream.read();
        notifyRequestBytesRead(1);
        return result;
    }

    private void notifyRequestBytesRead(long result) {
        notifyBytesRead(result);
    }

    private void notifyBytesRead(long result) {
        if (result >= 0) {
            switch (this.field) {
                case REQUEST_INPUT_BYTES:
                    stats.notifyRequestBytesRead(result);
                    break;
                case REQUEST_COMPRESSED_BYTES:
                    stats.notifyCompressedRequestBytesRead(result);
                    break;
                default:
                    throw new IllegalStateException();
            }
            // here, result is the byte read, or -1 if EOF
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = inputStream.read(b);
        this.notifyBytesRead(result);
        return result;
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        int result = inputStream.read(b, offset, length);
        this.notifyBytesRead(result);
        return result;
    }

    // Leave implementation of readLine() in superclass alone, even if it's not so efficient
    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    @Override
    public String toString() {
        return "StatsInputStream[" + inputStream + ']';
    }

}

/*
 * Copyright 2004 and onwards Sean Owen
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
import java.io.OutputStream;

/**
 * An {@link OutputStream} that decorates another {@link OutputStream} and notes when bytes are
 * written to the stream. Callers create an instance of {@link StatsOutputStream} with an instance
 * of {@link OutputStream}, which receives notification of writes. This information might be used to
 * tally the number of bytes written to a stream.
 *
 * @author Sean Owen
 */
public class StatsOutputStream extends OutputStream {

    private final OutputStream outputStream;
    protected final CompressingFilterStats stats;
    private final StatsField field;

    StatsOutputStream(OutputStream outputStream, CompressingFilterStats stats, StatsField field) {
        assert outputStream != null && stats != null;
        this.outputStream = outputStream;
        this.stats = stats;
        this.field = field;
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
        notifyBytesWritten(1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        outputStream.write(b);
        if (b != null && b.length > 0) {
            notifyBytesWritten(b.length);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
        if (len > 0) {
            notifyBytesWritten(len);
        }
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    @Override
    public String toString() {
        return "StatsOutputStream[" + outputStream + ']';
    }

    private void notifyBytesWritten(long result) {
        switch (this.field) {
            case RESPONSE_INPUT_BYTES:
                stats.notifyResponseBytesWritten(result);
                break;
            case RESPONSE_COMPRESSED_BYTES:
                stats.notifyCompressedResponseBytesWritten(result);
                break;
            default:
                throw new IllegalStateException();
        }
    }

}
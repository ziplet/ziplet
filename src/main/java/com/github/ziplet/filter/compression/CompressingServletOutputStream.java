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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Implementation of {@link ServletOutputStream} which will optionally compress data written to it.
 *
 * @author Sean Owen
 */
final class CompressingServletOutputStream extends ServletOutputStream {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CompressingServletOutputStream.class);
    private final OutputStream rawStream;
    private final CompressingStreamFactory compressingStreamFactory;
    private final CompressingHttpServletResponse compressingResponse;
    private final CompressingFilterContext context;
    private ThresholdOutputStream thresholdOutputStream;
    private boolean closed;
    private boolean aborted;

    CompressingServletOutputStream(OutputStream rawStream,
                                   CompressingStreamFactory compressingStreamFactory,
                                   CompressingHttpServletResponse compressingResponse,
                                   CompressingFilterContext context) {
        this.rawStream = rawStream;
        this.compressingStreamFactory = compressingStreamFactory;
        this.compressingResponse = compressingResponse;
        this.context = context;
        closed = false;
        aborted = false;
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkClosed();
        checkWriteState();
        assert thresholdOutputStream != null;
        thresholdOutputStream.write(b);
    }

    @Override
    public void write(byte[] b, int offset, int length) throws IOException {
        checkClosed();
        checkWriteState();
        assert thresholdOutputStream != null;
        thresholdOutputStream.write(b, offset, length);
    }

    @Override
    public void write(int b) throws IOException {
        checkClosed();
        checkWriteState();
        assert thresholdOutputStream != null;
        thresholdOutputStream.write(b);
    }

    @Override
    public void flush() {
        // do nothing actually
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            compressingResponse.flushBuffer();
            closed = true;
            if (thresholdOutputStream == null) {
                // Nothing written, so, signal that effectively the 'raw' output stream was used and close it
                compressingResponse.rawStreamCommitted();
                rawStream.close();
            } else {
                thresholdOutputStream.close();
            }
        }
    }

    @Override
    public String toString() {
        return "CompressingServletOutputStream";
    }

    boolean isClosed() {
        return closed;
    }

    void reset() {
        // can't reset rawStream, so do nothing if compressionDisabled, else:
        if (thresholdOutputStream != null) {
            thresholdOutputStream.reset();
        }
    }

    void engageCompression() throws IOException {
        checkWriteState();
        thresholdOutputStream.switchToOutputStream2();
    }

    void abortCompression() throws IOException {
        assert thresholdOutputStream == null;
        // remember that this was called, in case thresholdOutputStream has not been set up yet,
        // so that when it is we can invoke forceOutputStream1()
        checkWriteState();
        thresholdOutputStream.forceOutputStream1();
        aborted = true;
    }

    boolean isAborted() {
        return aborted;
    }

    private void checkWriteState() {
        if (thresholdOutputStream == null) {
            thresholdOutputStream =
                    new ThresholdOutputStream(rawStream,
                            compressingStreamFactory,
                            context,
                            new ResponseBufferCommitmentCallback(compressingResponse));
        }
    }

    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("Stream is already closed");
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
    }

    private static final class ResponseBufferCommitmentCallback
            implements ThresholdOutputStream.BufferCommitmentCallback {

        private final CompressingHttpServletResponse response;

        private ResponseBufferCommitmentCallback(CompressingHttpServletResponse response) {
            assert response != null;
            this.response = response;
        }

        public void rawStreamCommitted() {
            response.rawStreamCommitted();
        }

        public void compressingStreamCommitted() {
            response.switchToCompression();
        }

        @Override
        public String toString() {
            return "ResponseBufferCommitmentCallback";
        }
    }
}

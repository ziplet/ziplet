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
package com.github.ziplet.filter.compression.statistics;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>This class provides runtime statistics on the performance of {@link
 * com.github.ziplet.filter.compression.CompressingFilter}. If stats are enabled, then an instance
 * of this object will be available in the servlet context under the key {@link #STATS_KEY}. It can
 * be retrieved and used like so:</p>
 * <pre>
 * ServletContext ctx = ...;
 * // in a JSP, "ctx" is already available as the "application" variable
 * CompressingFilterStatsImpl stats = (CompressingFilterStatsImpl) ctx.getAttribute(CompressingFilterStatsImpl.STATS_KEY);
 * double ratio = stats.getAverageCompressionRatio();
 * ...
 * </pre>
 *
 * @author Sean Owen
 * @since 1.1
 */
public class CompressingFilterStatsImpl implements Serializable,
        com.github.ziplet.filter.compression.statistics.CompressingFilterStats {

    private static final long serialVersionUID = -2246829834191152845L;
    /**
     * Key under which a {@link CompressingFilterStatsImpl} object can be found in the servlet
     * context.
     */
    private static final String STATS_KEY = "com.github.ziplet.filter.compression.statistics.CompressingFilterStatsImpl";
    /**
     * @serial
     */
    private AtomicInteger numResponsesCompressed = new AtomicInteger();
    /**
     * @serial
     */
    private AtomicInteger totalResponsesNotCompressed = new AtomicInteger();
    /**
     * @serial
     */
    private AtomicLong responseInputBytes = new AtomicLong();
    /**
     * @serial
     */
    private AtomicLong responseCompressedBytes = new AtomicLong();
    /**
     * @serial
     */
    private AtomicInteger numRequestsCompressed = new AtomicInteger();
    /**
     * @serial
     */
    private AtomicInteger totalRequestsNotCompressed = new AtomicInteger();
    /**
     * @serial
     */
    private AtomicLong requestInputBytes = new AtomicLong();
    /**
     * @serial
     */
    private AtomicLong requestCompressedBytes = new AtomicLong();

    /**
     * @return the number of responses which {@link com.github.ziplet.filter.compression.CompressingFilter}
     * has compressed.
     */
    public int getNumResponsesCompressed() {
        return numResponsesCompressed.get();
    }

    @Override
    public void incrementNumResponsesCompressed() {
        numResponsesCompressed.incrementAndGet();
    }

    /**
     * @return the number of responses which {@link com.github.ziplet.filter.compression.CompressingFilter}
     * has processed but <em>not</em> compressed for some reason (compression not supported by the
     * browser, for example).
     */
    public int getTotalResponsesNotCompressed() {
        return totalResponsesNotCompressed.get();
    }

    @Override
    public void incrementTotalResponsesNotCompressed() {
        totalResponsesNotCompressed.incrementAndGet();
    }

    /**
     * @return total number of bytes written to the {@link com.github.ziplet.filter.compression.CompressingFilter}
     * in responses.
     * @deprecated use {@link #getResponseInputBytes()}
     */
    @Deprecated
    public long getInputBytes() {
        return getResponseInputBytes();
    }

    /**
     * @return total number of bytes written to the {@link com.github.ziplet.filter.compression.CompressingFilter}
     * in responses.
     */
    public long getResponseInputBytes() {
        return responseInputBytes.get();
    }

    /**
     * @return total number of compressed bytes written by the {@link com.github.ziplet.filter.compression.CompressingFilter}
     * to the client in responses.
     * @deprecated use {@link #getResponseCompressedBytes()}
     */
    @Deprecated
    public long getCompressedBytes() {
        return getResponseCompressedBytes();
    }

    /**
     * @return total number of compressed bytes written by the {@link com.github.ziplet.filter.compression.CompressingFilter}
     * to the client in responses.
     */
    public long getResponseCompressedBytes() {
        return responseCompressedBytes.get();
    }

    /**
     * @deprecated use {@link #getResponseAverageCompressionRatio()}
     */
    @Deprecated
    public double getAverageCompressionRatio() {
        return getResponseAverageCompressionRatio();
    }

    /**
     * @return average compression ratio (input bytes / compressed bytes) in responses, or 0 if
     * nothing has yet been compressed. Note that this is (typically) greater than 1, not less than
     * 1.
     */
    public double getResponseAverageCompressionRatio() {
        return getResponseCompressedBytes() == 0L ? 0.0 :
                (double) getResponseInputBytes() / (double) getResponseCompressedBytes();
    }

    /**
     * @return the number of requests which {@link com.github.ziplet.filter.compression.CompressingFilter}
     * has compressed.
     * @since 1.6
     */
    public int getNumRequestsCompressed() {
        return numRequestsCompressed.get();
    }

    @Override
    public void incrementNumRequestsCompressed() {
        numRequestsCompressed.incrementAndGet();
    }

    /**
     * @return the number of requests which {@link com.github.ziplet.filter.compression.CompressingFilter}
     * has processed but <em>not</em> compressed for some reason (no compression requested, for
     * example).
     * @since 1.6
     */
    public int getTotalRequestsNotCompressed() {
        return totalRequestsNotCompressed.get();
    }

    @Override
    public void incrementTotalRequestsNotCompressed() {
        totalRequestsNotCompressed.incrementAndGet();
    }

    /**
     * @return total number of bytes written to the {@link com.github.ziplet.filter.compression.CompressingFilter}
     * in requests.
     * @since 1.6
     */
    public long getRequestInputBytes() {
        return requestInputBytes.get();
    }

    /**
     * @return total number of compressed bytes written by the {@link com.github.ziplet.filter.compression.CompressingFilter}
     * to the client in requests.
     * @since 1.6
     */
    public long getRequestCompressedBytes() {
        return requestCompressedBytes.get();
    }

    /**
     * @return average compression ratio (input bytes / compressed bytes) in requests, or 0 if
     * nothing has yet been compressed. Note that this is (typically) greater than 1, not less than
     * 1.
     * @since 1.6
     */
    public double getRequestAverageCompressionRatio() {
        return requestCompressedBytes.get() == 0L ? 0.0 :
                (double) requestInputBytes.get() / (double) requestCompressedBytes.get();
    }

    /**
     * @return a summary of the stats in String form
     */
    @Override
    public String toString() {
        return "CompressingFilterStatsImpl[responses compressed: " + numResponsesCompressed
                + ", avg. response compression ratio: " + getResponseAverageCompressionRatio()
                + ", requests compressed: " + numRequestsCompressed
                + ", avg. request compression ratio: " + getRequestAverageCompressionRatio() + ']';
    }

    @Override
    public void notifyRequestBytesRead(long read) {
        requestInputBytes.addAndGet(read);
    }

    @Override
    public void notifyCompressedRequestBytesRead(long read) {
        this.requestCompressedBytes.addAndGet(read);
    }

    @Override
    public void notifyResponseBytesWritten(long written) {
        this.responseInputBytes.addAndGet(written);
    }

    @Override
    public void notifyCompressedResponseBytesWritten(long written) {
        this.responseCompressedBytes.addAndGet(written);
    }

    @Override
    public String getStatsKey() {
        return STATS_KEY;
    }
}



package com.github.ziplet.filter.compression;

import com.github.ziplet.filter.compression.statistics.CompressingFilterStats;

import java.io.Serializable;

/**
 * Created by fdonnarumma on 3/11/14.
 */
public class OutputStatsCallback implements Serializable {

    private static final long serialVersionUID = -4483355731273629325L;
    /**
     * @serial
     */
    private final StatsField field;

    private final CompressingFilterStats stats;

    public OutputStatsCallback(StatsField field, CompressingFilterStats stats) {
        this.field = field;
        this.stats = stats;
    }

    public void bytesWritten(int numBytes) {
        assert numBytes >= 0;
        switch (field) {
            case RESPONSE_INPUT_BYTES:
                stats.notifyResponseBytesWritten(numBytes);
                break;
            case RESPONSE_COMPRESSED_BYTES:
                stats.notifyCompressedResponseBytesWritten(numBytes);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public String toString() {
        return "OutputStatsCallback[field: " + field + ']';
    }
}



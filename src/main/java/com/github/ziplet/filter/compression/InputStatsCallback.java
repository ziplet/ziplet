package com.github.ziplet.filter.compression;

import com.github.ziplet.filter.compression.statistics.CompressingFilterStats;

import java.io.Serializable;

/**
 * Created by fdonnarumma on 3/11/14.
 */
public class InputStatsCallback implements Serializable {

    private static final long serialVersionUID = 8205059279453932247L;
    /**
     * @serial
     */
    private final StatsField field;

    private final CompressingFilterStats stats;

    public InputStatsCallback(StatsField field, CompressingFilterStats stats) {
        this.field = field;
        this.stats = stats;
    }

    public void bytesRead(int numBytes) {
        assert numBytes >= 0;
        switch (field) {
            case REQUEST_INPUT_BYTES:
                stats.notifyRequestBytesRead(numBytes);
                break;
            case REQUEST_COMPRESSED_BYTES:
                stats.notifyCompressedRequestBytesRead(numBytes);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public String toString() {
        return "InputStatsCallback[field: " + field + ']';
    }
}
package com.github.ziplet.filter.compression.statistics;

/**
 * Created by fdonnarumma on 3/10/14.
 */
public interface CompressingFilterStats {

    public void incrementNumResponsesCompressed();

    public void incrementTotalResponsesNotCompressed();

    public void incrementNumRequestsCompressed();

    public void incrementTotalRequestsNotCompressed();

    public void notifyRequestBytesRead(long read);

    public void notifyCompressedRequestBytesRead(long read);

    public void notifyResponseBytesWritten(long written);

    public void notifyCompressedResponseBytesWritten(long written);

    public String getStatsKey();

}

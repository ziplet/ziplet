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

import com.github.ziplet.filter.compression.statistics.CompressingFilterStatsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import junit.framework.TestCase;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tests {@link CompressingFilter} compressed requests.
 *
 * @author Sean Owen
 * @since 1.6
 */
public final class CompressingFilterRequestTest extends TestCase {

    private static final byte[] BIG_DOCUMENT;

    static {
        // Make up a random, but repeatable String
        Random r = new Random(0xDEADBEEFL);
        BIG_DOCUMENT = new byte[10000];
        r.nextBytes(BIG_DOCUMENT);
    }

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    MockFilterConfig config;
    CompressingFilter compressingFilter;


    private static byte[] getCompressedOutput(byte[] output) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream gzipOut = new GZIPOutputStream(baos);
        gzipOut.write(output);
        gzipOut.finish();
        gzipOut.close();
        baos.close();
        return baos.toByteArray();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        config = new MockFilterConfig();
        config.addInitParameter("debug", "true");
        config.addInitParameter("statsEnabled", "true");
        compressingFilter = new CompressingFilter();
        compressingFilter.init(config);
        request = new MockHttpServletRequest();
        request.setMethod(HttpMethod.GET.name());
        response =  new MockHttpServletResponse();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBigOutput() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
        HttpTestServlet servlet = new HttpTestServlet() {

            @Override
            public void doGet(HttpServletRequest request, HttpServletResponse resp) throws IOException {
                InputStream sis = request.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = sis.read(buffer)) > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
                baos.close();
            }
        };
        servlet.init(new MockServletConfig());

        request.addHeader("Content-Encoding", "gzip");
        byte[] compressedBigDoc = getCompressedOutput(BIG_DOCUMENT);
        request.setContent(compressedBigDoc);

        new MockFilterChain(servlet, compressingFilter).doFilter(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null);
        assertTrue(response.getErrorMessage() == null);
        assertTrue(Arrays.equals(BIG_DOCUMENT, baos.toByteArray()));
        CompressingFilterStatsImpl stats = (CompressingFilterStatsImpl) config.getServletContext().getAttribute("com.github.ziplet.filter.compression.statistics.CompressingFilterStatsImpl");
        assertNotNull(stats);

        assertEquals(1, stats.getNumRequestsCompressed());
        assertEquals(0, stats.getTotalRequestsNotCompressed());
        assertEquals((double) BIG_DOCUMENT.length / (double) compressedBigDoc.length,
                stats.getRequestAverageCompressionRatio());
        assertEquals((long) compressedBigDoc.length, stats.getRequestCompressedBytes());
        assertEquals((long) BIG_DOCUMENT.length, stats.getRequestInputBytes());

        assertEquals(0, stats.getNumResponsesCompressed());
        assertEquals(1, stats.getTotalResponsesNotCompressed());
        assertEquals(0.0, stats.getResponseAverageCompressionRatio());
        assertEquals(0L, stats.getResponseCompressedBytes());
        assertEquals(0L, stats.getResponseInputBytes());
    }

}

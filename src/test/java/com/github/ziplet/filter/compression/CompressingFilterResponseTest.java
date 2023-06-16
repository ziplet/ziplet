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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import junit.framework.TestCase;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.function.ThrowingConsumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Tests {@link CompressingFilter} compressing responses.
 *
 * @author Sean Owen
 */
public final class CompressingFilterResponseTest extends TestCase {

    static final String SMALL_DOCUMENT = "Test";
    static final String BIG_DOCUMENT;
    static final String BIG_TEXT_DOCUMENT;
    private static final String TEST_ENCODING = "ISO-8859-1";
    private static final String EMPTY = "";

    static {
        // Make up a random, but repeatable String
        Random r = new Random(0xDEADBEEFL);
        byte[] bytes = new byte[10000];
        r.nextBytes(bytes);
        String temp = null;
        try {
            temp = new String(bytes, TEST_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            // can't happen
        }
        BIG_DOCUMENT = temp;
    }

    static {
        // Make up a random, but repeatable long ASCII-7 String that is non-trivially compressible
        Random r = new Random(0xDEADBEEFL);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            int c = r.nextInt(Character.MIN_HIGH_SURROGATE);
            sb.append(String.valueOf(c)).append(":").append(Character.getName(c)).append("\n");
        }
        BIG_TEXT_DOCUMENT = sb.toString();
    }

    HttpTestServlet servlet;


    private static byte[] uncompressGzip(byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(input);
        byte[] buffer = new byte[1024];
        GZIPInputStream gzipIn = new GZIPInputStream(bais);
        int len;
        while ((len = gzipIn.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    private static byte[] uncompressDeflate(byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(input);
        byte[] buffer = new byte[1024];
        InflaterInputStream deflateIn = new InflaterInputStream(bais);
        int len;
        while ((len = deflateIn.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    private static byte[] getCompressedOutput(byte[] output) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream gzipOut = new GZIPOutputStream(baos);
        gzipOut.write(output);
        gzipOut.finish();
        gzipOut.close();
        return baos.toByteArray();
    }

    MockFilterConfig config;
    CompressingFilter filter ;

    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        config = new MockFilterConfig();
        config.addInitParameter("debug", "true");
        config.addInitParameter("statsEnabled", "true");
        config.addInitParameter("excludePathPatterns", ".*badpath.*,whocares");
        config.addInitParameter("excludeContentTypes", "text/badtype,whatever");
        config.addInitParameter("excludeUserAgentPatterns", "Nokia.*");
        config.addInitParameter("noVaryHeaderPatterns", ".*MSIE 8.*");
        filter = new CompressingFilter();
        filter.init(config);
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        response = new MockHttpServletResponse();
        response.setCharacterEncoding(TEST_ENCODING);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        servlet = null;
    }

    public void testSmallOutput() throws Exception {
        verifyOutput(SMALL_DOCUMENT, false);
    }

    public void testBigOutput() throws Exception {
        verifyOutput(BIG_DOCUMENT, true);

        CompressingFilterStatsImpl stats = (CompressingFilterStatsImpl) config.getServletContext().getAttribute(
                "com.github.ziplet.filter.compression.statistics.CompressingFilterStatsImpl");
        assertNotNull(stats);

        assertEquals(0, stats.getNumRequestsCompressed());
        assertEquals(1, stats.getTotalRequestsNotCompressed());
        assertEquals(0.0, stats.getRequestAverageCompressionRatio());
        assertEquals(0L, stats.getRequestCompressedBytes());
        assertEquals(0L, stats.getRequestInputBytes());

        assertEquals(1, stats.getNumResponsesCompressed());
        assertEquals(0, stats.getTotalResponsesNotCompressed());
        assertEquals(0.9977, stats.getResponseAverageCompressionRatio(), 0.0001);
        assertEquals(10023L, stats.getResponseCompressedBytes());
        assertEquals(10000L, stats.getResponseInputBytes());
    }

    public void testAlreadyApplied() throws Exception {
        // add the filter again
//        module.addFilter(new CompressingFilter(), true); // TODO
        verifyOutput(BIG_DOCUMENT, true);
    }

    public void testForceEncoding() throws Exception {
        // force no-compression compression for a big response
        request.setAttribute(CompressingFilter.FORCE_ENCODING_KEY, "identity");
        verifyOutput(BIG_DOCUMENT, false);
    }

    public void testNoTransform() throws Exception {
        servlet = createServlet(response -> {
            response.setHeader("Cache-Control", "no-transform");
            response.getWriter().print(BIG_DOCUMENT);
        });
        verifyOutput(BIG_DOCUMENT, false);
    }

    public void testExcludePathPatterns1() throws Exception {
        request.setRequestURI("/some/goodpath/index.html");
        verifyOutput(BIG_DOCUMENT, true);
    }

    public void testExcludePathPatterns2() throws Exception {
        request.setRequestURI("/some/badpath/index.html");
        verifyOutput(BIG_DOCUMENT, false);
    }

    public void testExcludeUserAgentPatterns1() throws Exception {
        request.addHeader("User-Agent", "MSIE5");
        verifyOutput(BIG_DOCUMENT, true);
    }

    public void testExcludeUserAgentPatterns2() throws Exception {
        request.addHeader("User-Agent", "Nokia6820");
        verifyOutput(BIG_DOCUMENT, false);
    }

    public void testExcludeContentTypes1() throws Exception {
        servlet = createServlet(response -> {
            response.setContentType("text/badtype; otherstuff");
            response.getWriter().print(BIG_DOCUMENT);

        });
        verifyOutput(BIG_DOCUMENT, false);
    }

    public void testExcludeContentTypes2() throws Exception {
        servlet = createServlet(response -> {
            response.setContentType("text/goodtype; otherstuff");
            response.getWriter().print(BIG_DOCUMENT);

        });
        verifyOutput(BIG_DOCUMENT, true);
    }

    public void testDontSendVaryHeader() throws IOException, ServletException {
        request.addHeader("User-Agent", "bla MSIE 8.0 blub");
        verifyOutput(BIG_DOCUMENT, true, true);
    }

    public void testRedirect() throws Exception {
        servlet = createServlet(response -> {
            response.sendRedirect("http://www.google.com/");
        });

        MockFilterChain mockFilterChain = new MockFilterChain(servlet, filter);
        mockFilterChain.doFilter(request, response);

        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl() != null);
        assertTrue(response.getErrorMessage() == null);
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    public void testFlush() throws ServletException, IOException {
        HttpTestServlet servlet = createServlet(response -> {
            response.getWriter().print(SMALL_DOCUMENT);
            response.flushBuffer();
            response.getWriter().print(SMALL_DOCUMENT);
        });
        MockFilterChain mockFilterChain = new MockFilterChain(servlet, filter);
        mockFilterChain.doFilter(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null);
        assertTrue(response.getErrorMessage() == null);
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    public void testClose() throws ServletException, IOException {
        servlet = createServlet(response -> {
            response.getWriter().print(SMALL_DOCUMENT);
            response.getWriter().close();
        });

        MockFilterChain mockFilterChain = new MockFilterChain(servlet, filter);
        mockFilterChain.doFilter(request, response);


        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null);
        assertTrue(response.getErrorMessage() == null);
        assertEquals(SMALL_DOCUMENT, response.getContentAsString());
        assertNull(request.getAttribute(CompressingFilter.COMPRESSED_KEY));
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    public void testSpuriousFlushClose() throws ServletException, IOException {
        servlet = createServlet(response -> {
            response.getWriter().print(SMALL_DOCUMENT);
            response.getWriter().close();
            response.getWriter().flush();
            response.getWriter().close();
        });

        MockFilterChain mockFilterChain = new MockFilterChain(servlet, filter);
        mockFilterChain.doFilter(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null);
        assertTrue(response.getErrorMessage() == null);
        assertEquals(SMALL_DOCUMENT, response.getContentAsString());
        assertNull(request.getAttribute(CompressingFilter.COMPRESSED_KEY));
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    public void testNoGzipOutput() throws ServletException, IOException {
        doTestNoOutput();
    }

    public void testNoZipOutput() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Content-Encoding", "compress");
        doTestNoOutput();
    }

    public void testCompressionLevel() throws IOException, ServletException {
        int[] compressionLevels = new int[]{
                Deflater.BEST_SPEED, // 1
                Deflater.DEFAULT_COMPRESSION, // -1 -> 6
                Deflater.BEST_COMPRESSION, // 9
        };

        String[] acceptEncodings = new String[]{
                "deflate",
                "gzip",
        };

        for (String acceptEncoding : acceptEncodings) {
            long[] responseLengths = new long[compressionLevels.length];

            for (int i = 0; i < compressionLevels.length; i++) {
                // override module to set different config
                MockFilterConfig config = new MockFilterConfig();
                config.addInitParameter("debug", "true");
                config.addInitParameter("statsEnabled", "true");
                config.addInitParameter("compressionThreshold", String.valueOf(0));
                config.addInitParameter("compressionLevel", String.valueOf(compressionLevels[i]));
                CompressingFilter filter = new CompressingFilter();

                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();
                response.setCharacterEncoding(TEST_ENCODING);

                request.addHeader("Accept-Encoding", acceptEncoding);
                request.setMethod("GET");
                HttpTestServlet servlet = createServlet(resp -> {
                    resp.getWriter().print(BIG_TEXT_DOCUMENT);

                });
                filter.init(config);
                MockFilterChain mockFilterChain = new MockFilterChain(servlet, filter);
                mockFilterChain.doFilter(request, response);
                // response should be OK
                assertEquals(HttpServletResponse.SC_OK, response.getStatus());
                assertTrue(response.getRedirectedUrl() == null);
                assertTrue(response.getErrorMessage() == null);

                // response should be compressed
                assertTrue(response.containsHeader("Content-Encoding"));
                assertEquals(acceptEncoding, response.getHeader("Content-Encoding"));
                assertEquals(Boolean.TRUE,
                        request.getAttribute(CompressingFilter.COMPRESSED_KEY));
                String moduleOutput = response.getContentAsString();
                assertFalse("Response body not compressed", BIG_TEXT_DOCUMENT.equals(moduleOutput));

                // uncompressed response body should match expected
                final byte[] uncompressedBytes;
                if ("gzip".equals(acceptEncoding)) {
                    uncompressedBytes = uncompressGzip(moduleOutput.getBytes(TEST_ENCODING));
                } else if ("deflate".equals(acceptEncoding)) {
                    uncompressedBytes = uncompressDeflate(moduleOutput.getBytes(TEST_ENCODING));
                } else {
                    throw new IllegalStateException("Unhandled encoding: " + acceptEncoding);
                }
                assertEquals("Response body uncompression mismatch", BIG_TEXT_DOCUMENT,
                        new String(uncompressedBytes, TEST_ENCODING));

                // compression ratio should be sane
                CompressingFilterStatsImpl stats = (CompressingFilterStatsImpl) config.getServletContext()
                        .getAttribute(
                                "com.github.ziplet.filter.compression.statistics.CompressingFilterStatsImpl");
                assertNotNull(stats);

                assertEquals(1, stats.getNumResponsesCompressed());
                assertEquals(0, stats.getTotalResponsesNotCompressed());
                assertTrue("response length did not shrink by compression",
                        BIG_TEXT_DOCUMENT.length() > stats.getResponseCompressedBytes());

                // compression ratio should monotonically increase with increasing compressionLevels
                responseLengths[i] = stats.getResponseCompressedBytes();
                // compare to previous, if any
                if (i > 0) {
                    assertTrue(
                            "Compression ratio did not improve from " + acceptEncoding + " level "
                                    + compressionLevels[i - 1] + " to " + compressionLevels[i],
                            responseLengths[i - 1] > responseLengths[i]);
                }
            }
        }
    }

    private void doTestNoOutput() throws ServletException, IOException {
        servlet = createServlet((response) -> {
            response.getWriter().close();
        });

        MockFilterChain mockFilterChain = new MockFilterChain(servlet, filter);
        mockFilterChain.doFilter(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null);
        assertTrue(response.getErrorMessage() == null);
        assertEquals(EMPTY, response.getContentAsString());
        assertNull(request.getAttribute(CompressingFilter.COMPRESSED_KEY));

        assertFalse(response.containsHeader("Content-Encoding"));
        assertFalse(response.containsHeader("X-Compressed-By"));
        assertTrue(response.containsHeader(CompressingFilter.VARY_HEADER));
    }

    private void verifyOutput(final String output, boolean shouldCompress) throws IOException, ServletException {
        verifyOutput(output, shouldCompress, false);
    }

    private void verifyOutput(final String output, boolean shouldCompress, boolean noVaryHeader)
            throws IOException, ServletException {
        final String originalEtag = "\"" + output.hashCode() + "\""; // Fake ETag
        final String compressedEtag = "\"" + output.hashCode() + "-gzip\"";
        if (servlet == null) {
            servlet = createServlet((response) -> {
                response.setHeader("ETag", originalEtag);
                response.getWriter().print(output);
            });
        }
        request.addHeader("Accept-Encoding", "deflate,gzip");
        MockFilterChain filterChain2 = new MockFilterChain(servlet, filter);
        filterChain2.doFilter(request, response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getRedirectedUrl() == null);
        assertTrue(response.getErrorMessage() == null);

        if (shouldCompress) {
            assertEquals("Check vary header", !noVaryHeader, response.containsHeader("Vary"));
            byte[] expectedBytes = getCompressedOutput(output.getBytes(TEST_ENCODING));
            // Since ServletTestModule makes a String out of the output according to ISO-8859-1 encoding,
            // do the same for expected bytes and then compare. Don't use assertEquals(); you'll just see
            // a bunch of binary garbage if the results differ
            String moduleOutput = response.getContentAsString();
            assertFalse(output.equals(moduleOutput));
            String expectedString = new String(expectedBytes, TEST_ENCODING);
            assertEquals(expectedString, moduleOutput);
            assertEquals(Boolean.TRUE,
                    request.getAttribute(CompressingFilter.COMPRESSED_KEY));

            assertTrue(response.containsHeader("Content-Encoding"));
            assertTrue(response.containsHeader("X-Compressed-By"));
            assertTrue(
                    !response.containsHeader("ETag") || response.getHeader("ETag").equals(compressedEtag));
        } else {
            assertEquals(output, response.getContentAsString());
            assertNull(request.getAttribute(CompressingFilter.COMPRESSED_KEY));
            assertTrue(!response.containsHeader("ETag") || response.getHeader("ETag").equals(originalEtag));
        }
    }

    private HttpTestServlet createServlet(ThrowingConsumer<HttpServletResponse> doGet) throws IOException {
        return new HttpTestServlet() {
            @Override
            public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                doGet.accept(resp);
            }
        };
    }
}

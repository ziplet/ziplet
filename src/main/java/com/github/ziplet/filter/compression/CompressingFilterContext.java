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

import com.github.ziplet.filter.compression.statistics.CompressingFilterEmptyStats;
import com.github.ziplet.filter.compression.statistics.CompressingFilterStats;
import com.github.ziplet.filter.compression.statistics.CompressingFilterStatsImpl;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

/**
 * Encapsulates the {@link CompressingFilter} environment, including configuration and runtime
 * statistics. This object may be conveniently passed around in the code to make this information
 * available.
 *
 * @author Sean Owen
 * @author Peter Bryant
 */
final class CompressingFilterContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressingFilterContext.class);
    private static final int DEFAULT_COMPRESSION_THRESHOLD = 1024;
    private static final int DEFAULT_COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;
    private static final Pattern COMMA = Pattern.compile(",");
    private final boolean debug;
    private final int compressionThreshold;
    private final int compressionLevel;
    private final ServletContext servletContext;
    private final boolean includeContentTypes;
    private final Collection<String> contentTypes;
    // Thanks to Peter Bryant for suggesting this functionality:
    private final boolean includePathPatterns;
    private final Collection<Pattern> pathPatterns;
    // Thanks to reimerl for proposing this + sample code
    private final boolean includeUserAgentPatterns;
    private final Collection<Pattern> userAgentPatterns;
    private final Collection<Pattern> noVaryHeaderPatterns;
    private CompressingFilterStats stats;

    CompressingFilterContext(FilterConfig filterConfig, CompressingFilterStats stats)
            throws ServletException {
        this(filterConfig);
        this.setCompressingFilterStats(stats);
    }

    CompressingFilterContext(FilterConfig filterConfig) throws ServletException {

        assert filterConfig != null;

        debug = readBooleanValue(filterConfig, "debug");

        LOGGER.debug("Debug logging statements are enabled");

        compressionThreshold = readCompressionThresholdValue(filterConfig);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Using compressing threshold: " + compressionThreshold);
        }

        compressionLevel = readCompressionLevelValue(filterConfig);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Using compression level: " + compressionLevel);
        }

        servletContext = filterConfig.getServletContext();
        assert this.servletContext != null;

        if (readBooleanValue(filterConfig, "statsEnabled")) {
            stats = new CompressingFilterStatsImpl();
            ensureStatsInContext();
            LOGGER.debug("Stats are enabled");
        } else {
            stats = new CompressingFilterEmptyStats();
            LOGGER.debug("Stats are disabled");
        }

        String noVaryHeaderString = filterConfig.getInitParameter("noVaryHeaderPatterns");
        if (noVaryHeaderString != null) {
            noVaryHeaderPatterns = parsePatterns(noVaryHeaderString);
        } else {
            noVaryHeaderPatterns = Collections.emptyList();
        }

        String includeContentTypesString = filterConfig.getInitParameter("includeContentTypes");
        String excludeContentTypesString = filterConfig.getInitParameter("excludeContentTypes");
        if (includeContentTypesString != null && excludeContentTypesString != null) {
            throw new IllegalArgumentException(
                    "Can't specify both includeContentTypes and excludeContentTypes");
        }

        if (includeContentTypesString == null) {
            includeContentTypes = false;
            contentTypes = parseContentTypes(excludeContentTypesString);
        } else {
            includeContentTypes = true;
            contentTypes = parseContentTypes(includeContentTypesString);
        }

        if (!contentTypes.isEmpty()) {
            LOGGER.debug("Filter will " + (includeContentTypes ? "include" : "exclude")
                    + " only these content types: " + contentTypes);
        }

        String includePathPatternsString = filterConfig.getInitParameter("includePathPatterns");
        String excludePathPatternsString = filterConfig.getInitParameter("excludePathPatterns");
        if (includePathPatternsString != null && excludePathPatternsString != null) {
            throw new IllegalArgumentException(
                    "Can't specify both includePathPatterns and excludePathPatterns");
        }

        if (includePathPatternsString == null) {
            includePathPatterns = false;
            pathPatterns = parsePatterns(excludePathPatternsString);
        } else {
            includePathPatterns = true;
            pathPatterns = parsePatterns(includePathPatternsString);
        }

        if (!pathPatterns.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Filter will " + (includePathPatterns ? "include" : "exclude")
                    + " only these file patterns: " + pathPatterns);
        }

        String includeUserAgentPatternsString = filterConfig
                .getInitParameter("includeUserAgentPatterns");
        String excludeUserAgentPatternsString = filterConfig
                .getInitParameter("excludeUserAgentPatterns");
        if (includeUserAgentPatternsString != null && excludeUserAgentPatternsString != null) {
            throw new IllegalArgumentException(
                    "Can't specify both includeUserAgentPatterns and excludeUserAgentPatterns");
        }

        if (includeUserAgentPatternsString == null) {
            includeUserAgentPatterns = false;
            userAgentPatterns = parsePatterns(excludeUserAgentPatternsString);
        } else {
            includeUserAgentPatterns = true;
            userAgentPatterns = parsePatterns(includeUserAgentPatternsString);
        }

        if (!userAgentPatterns.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Filter will " + (includeUserAgentPatterns ? "include" : "exclude")
                    + " only these User-Agent patterns: " + userAgentPatterns);
        }

    }

    private static boolean readBooleanValue(FilterConfig filterConfig, String parameter) {
        return Boolean.valueOf(filterConfig.getInitParameter(parameter));
    }

    private static int readCompressionThresholdValue(FilterConfig filterConfig)
            throws ServletException {
        String compressionThresholdString = filterConfig.getInitParameter("compressionThreshold");
        int value;
        if (compressionThresholdString != null) {
            try {
                value = Integer.parseInt(compressionThresholdString);
            } catch (NumberFormatException nfe) {
                throw new ServletException(
                        "Invalid compression threshold: " + compressionThresholdString,
                        nfe);
            }
            if (value < 0) {
                throw new ServletException("Compression threshold cannot be negative");
            }
        } else {
            value = DEFAULT_COMPRESSION_THRESHOLD;
        }
        return value;
    }

    private static int readCompressionLevelValue(FilterConfig filterConfig)
            throws ServletException {
        String compressionLevelString = filterConfig.getInitParameter("compressionLevel");
        int value;
        if (compressionLevelString != null) {
            try {
                value = Integer.parseInt(compressionLevelString);
            } catch (NumberFormatException nfe) {
                throw new ServletException("Invalid compression level: " + compressionLevelString,
                        nfe);
            }
            if (value == -1) {
                value = DEFAULT_COMPRESSION_LEVEL;
            } else if (value < 0) {
                throw new ServletException("Compression level cannot be negative, unless it is -1");
            } else if (value > Deflater.BEST_COMPRESSION) {
                throw new ServletException(
                        "Compression level cannot be greater than " + Deflater.BEST_COMPRESSION);
            }
        } else {
            value = DEFAULT_COMPRESSION_LEVEL;
        }
        return value;
    }

    private static Collection<String> parseContentTypes(String contentTypesString) {
        if (contentTypesString == null) {
            return Collections.emptyList();
        }
        List<String> contentTypes = new ArrayList<String>(5);
        for (String contentType : COMMA.split(contentTypesString)) {
            if (contentType.length() > 0) {
                contentTypes.add(contentType);
            }
        }
        return Collections.unmodifiableList(contentTypes);
    }

    private static Collection<Pattern> parsePatterns(String patternsString) {
        if (patternsString == null) {
            return Collections.emptyList();
        }
        List<Pattern> patterns = new ArrayList<Pattern>(5);
        for (String pattern : COMMA.split(patternsString)) {
            if (pattern.length() > 0) {
                patterns.add(Pattern.compile(pattern));
            }
        }
        return Collections.unmodifiableList(patterns);
    }

    public void setCompressingFilterStats(CompressingFilterStats stats) {
        this.stats = stats;
    }

    boolean isDebug() {
        return debug;
    }

    int getCompressionThreshold() {
        return compressionThreshold;
    }

    int getCompressionLevel() {
        return compressionLevel;
    }

    public CompressingFilterStats getStats() {
        if (stats == null) {
            throw new IllegalStateException("Stats are not enabled");
        }
        ensureStatsInContext();
        return stats;
    }

    boolean isIncludeContentTypes() {
        return includeContentTypes;
    }

    Collection<String> getContentTypes() {
        return contentTypes;
    }

    boolean isIncludePathPatterns() {
        return includePathPatterns;
    }

    Iterable<Pattern> getPathPatterns() {
        return pathPatterns;
    }

    boolean isIncludeUserAgentPatterns() {
        return includeUserAgentPatterns;
    }

    Iterable<Pattern> getUserAgentPatterns() {
        return userAgentPatterns;
    }

    Iterable<Pattern> getNoVaryHeaderPatterns() {
        return noVaryHeaderPatterns;
    }

    @Override
    public String toString() {
        return "CompressingFilterContext";
    }

    private void ensureStatsInContext() {
        assert servletContext != null;
        if (servletContext.getAttribute(stats.getStatsKey()) == null) {
            servletContext.setAttribute(stats.getStatsKey(), stats);
        }
    }
}

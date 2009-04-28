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

package com.planetj.servlet.filter.compression;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * <p>This filter can, based on HTTP headers in a {@link HttpServletRequest}, compress data written to the {@link
 * HttpServletResponse}, or decompress data read from the request.
 * When supported by the client browser, this can potentially greatly reduce the number of bytes
 * written across the network from and to the client. As a {@link Filter}, this class can also be easily added to
 * any J2EE 1.3+ web application.</p>
 *
 * <h3>Installation</h3>
 *
 * <ol>
 * <li>Add the .jar file containing CompressingFilter to your web application's
 *  <code>WEB-INF/lib</code> directory.</li>
 * <li>Add the following entries to your <code>web.xml</code> deployment descriptor:<br/>
 *
 * <pre>
 * &lt;filter&gt;
 *  &lt;filter-name>CompressingFilter&lt;/filter-name&gt;
 *  &lt;filter-class>com.planetj.servlet.filter.compression.CompressingFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * ...
 * &lt;filter-mapping&gt;
 *  &lt;filter-name>CompressingFilter&lt;/filter-name&gt;
 *  &lt;url-pattern>/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * </li>
 * </ol>
 *
 * <h3>Configuration</h3>
 *
 * <p>{@link CompressingFilter} supports the following parameters:</p>
 * <ul>
 *
 * <li><strong>debug</strong> (optional): if
 * set to "true", additional debug information will be written to the servlet log. Defaults to false.</li>
 *
 * <li><strong>compressionThreshold</strong> (optional): sets the size of the smallest response that will be compressed,
 * in bytes. That is, if less than <code>compressionThreshold</code> bytes are written to the response, it will not be
 * compressed and the response will go to the client unmodified. If 0, compression always begins immediately.
 * Defaults to 1024.</li>
 *
 * <li><strong>statsEnabled</strong> (optional): enables collection of statistics. See {@link CompressingFilterStats}.
 * Defaults to false.</li>
 *
 * <li><strong>includeContentTypes</strong> (optional): if specified, this is treated as a
 * comma-separated list of content types (e.g. <code>text/html,text/xml</code>). The filter will attempt to only
 * compress responses which specify one of these values as its content type, for example via {@link
 * HttpServletResponse#setContentType(String)}. Note that the filter does not know the response content type at the time
 * it is applied, and so must apply itself and later attempt to disable compression when content type has been set. This
 * will fail if the response has already been committed. Also note that this parameter cannot be specified if
 * <code>excludeContentTypes</code> is too.</li>
 *
 * <li><strong>excludeContentTypes</strong> (optional): same as above, but
 * specifies a list of content types to <strong>not</strong> compress. Everything else will be compressed.</li>
 *
 * <li><strong>includePathPatterns</strong> (optional): if specified, this is treated as a
 * comma-separated list of regular expressions (of the type accepted by {@link java.util.regex.Pattern}) which
 * match exactly those paths which should be compressed by this filter. Anything else will not be compressed. One
 * can also merely apply the filter to a subset of all URIs served by the web application using standard
 * <code>filter-mapping</code> elements in <code>web.xml</code>; this element provides more fine-grained control
 * for when that mechanism is insufficient. "Paths" here means values returned by
 * {@link javax.servlet.http.HttpServletRequest#getRequestURI()}. Note that
 * the regex must match the filename exactly; pattern "static" does <strong>not</strong> match everything containing
 * the string "static. Use ".*static.*" for that, for example. This cannot be specified if <code>excludeFileTypes</code>
 * is too.</li>
 *
 * <li><strong>excludePathPatterns</strong> (optional): same as above, but specifies a list of patterns which match
 * paths that should <strong>not</strong> be compressed. Everything else will be compressed.</li>
 *
 * <li><strong>includeUserAgentPatterns</strong> (optional): Like <code>includePathPatterns</code>. Only requests
 * with <code>User-Agent</code> headers whose value matches one of these regular expressions will be compressed.
 * Can't be specified if <code>excludeUserAgentPatterns</code> is too.</li>
 *
 * <li><strong>excludeUserAgentPatterns</strong> (optional): as above, requests whose <code>User-Agent</code> header
 * matches one of these patterns will not be compressed.</li>
 *
 * <li><strong>javaUtilLogger</strong> (optional): if specified, the named <code>java.util.logging.Logger</code>
 * will also receive log messages from this filter.</li>
 *
 * <li><strong>jakartaCommonsLogger</strong> (optional): if specified the named Jakarta Commons Log will
 * also receive log messages from this filter.</li>
 *
 * </ul>
 *
 * <p>These values are configured in <code>web.xml</code> as well with init-param elements:<br/>
 * <pre>
 * &lt;filter&gt;
 *  &lt;filter-name>CompressingFilter&lt;/filter-name&gt;
 *  &lt;filter-class>com.planetj.servlet.filter.compression.CompressingFilter&lt;/filter-class&gt;
 *  &lt;init-param&gt;
 *   &lt;param-name&gt;debug&lt;/param-name&gt;
 *   &lt;param-value&gt;true&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * &lt;/filter&gt;
 * </pre>
 *
 * <h3>Supported compression algorithms</h3>
 *
 * <h4>Response</h4>
 *
 * <p>This filter supports the following compression algorithms when compressing data to the repsonse,
 * as specified in the "Accept-Encoding" HTTP request header:</p>
 *
 * <ul>
 * <li>gzip</li>
 * <li>x-gzip</li>
 * <li>compress</li>
 * <li>x-compress</li>
 * <li>deflate</li>
 * <li>identity (that is, no compression)</li>
 * </ul>
 *
 * <h4>Request</h4>
 *
 * <p>This filter supports the following compression algorithms when decompressing data from the request body,
 * as specified in the "Content-Encoding" HTTP request header:</p>
 *
 * <ul>
 * <li>gzip</li>
 * <li>x-gzip</li>
 * <li>compress</li>
 * <li>x-compress</li>
 * <li>deflate</li>
 * <li>identity</li>
 * </ul>
 *
 * <h3>Controlling runtime behavior</h3>
 *
 * <p>An application may force the encoding / compression used by setting an "Accept-Encoding" value into the request as
 * an attribute under the key {@link #FORCE_ENCODING_KEY}. Obviously this has to be set upstream from the filter, not
 * downstream.</p>
 *
 * <h3>Caveats and Notes</h3>
 *
 * <p>The filter requires Java 5 and J2EE 1.4 or better.</p>
 *
 * <p>Note that if this filter decides that it should try to compress the response, it <em>will</em> close the response
 * (whether or not it ends up compressing the response). No more can be written to the response after this filter has
 * been applied; this should never be necessary anyway. Put this filter ahead of any filters that might try to write to
 * the repsonse, since presumably you want this content compressed too anyway.</p>
 *
 * <p>If a {@link java.io.OutputStream#flush()} occurs before the filter has decided whether to compress or not,
 * it will be forced into compression mode.</p>
 *
 * <p>The filter will not compress if the response sets <code>Cache-Control: no-transform</code> header in the
 * response.</p>
 *
 * <p>The filter attempts to modify the <code>ETag</code> response header, if present, when compressing. This
 * is because the compressed response must be considered a separate entity by caches. It simply appends, for
 * example, "-gzip" to the ETag header value when compressing with gzip. This is not guaranteed to work
 * in all containers, in the sense that some containers may not properly associated this ETag with the
 * compressed content and simply return the response again.</p>
 *
 * <p>The filter always sets the <code>Vary</code> response header to indicate that a different response may be
 * returned based on the <code>Accept-Encoding</code> header of the request.</p>
 *
 * @author Sean Owen
 * @since 1.0
 */
public final class CompressingFilter implements Filter {

	private static final String ALREADY_APPLIED_KEY = "com.planetj.servlet.filter.compression.AlreadyApplied";

	/**
	 * One may force the filter to use a particular encoding by setting its value as an attribute of the {@link
	 * ServletRequest} passed to this filter, under this key. The value should be a valid "Accept-Encoding" header value,
	 * like "gzip". Specify "identity" to force no compression.
	 *
	 * @since 1.2
	 */
	public static final String FORCE_ENCODING_KEY = "com.planet.servlet.filter.compression.ForceEncoding";

	/**
	 * A request attribute is set under this key with a non-null value if this filter has applied compression to the
	 * response. Upstream filters may check for this flag. Note that if the response has been compressed, then it will be
	 * closed by the time this filter finishes as well.
	 *
	 * @since 1.2
	 */
	public static final String COMPRESSED_KEY = "com.planetj.servlet.filter.compression.Compressed";

	static final String VERSION = "1.7";
	static final String VERSION_STRING = CompressingFilter.class.getName() + '/' + VERSION;

	private CompressingFilterContext context;
	private CompressingFilterLoggerImpl logger;

	public void init(FilterConfig config) throws ServletException {
		assert config != null;
		context = new CompressingFilterContext(config);
		logger = context.getLogger();
		logger.log("CompressingFilter has initialized");
	}

	public void doFilter(ServletRequest request,
	                     ServletResponse response,
	                     FilterChain chain) throws IOException, ServletException {

		ServletRequest chainRequest = getRequest(request);
		ServletResponse chainResponse = getResponse(request, response);

		boolean attemptingToDecompressRequest = chainRequest != null;
		boolean attemptingToCompressResponse = chainResponse != null;

		if (chainRequest == null) {
			chainRequest = request;
		}
		if (chainResponse == null) {
			chainResponse = response;
		}

		boolean statsEnabled = context.isStatsEnabled();

		if (statsEnabled) {
			if (attemptingToDecompressRequest) {
				context.getStats().incrementNumRequestsCompressed();						
			} else {
				context.getStats().incrementTotalRequestsNotCompressed();
			}
		}

    // Is this the right place? also done in CompressingHttpServletResponse
    if (chainResponse instanceof HttpServletResponse) {
      ((HttpServletResponse) chainResponse).setHeader(
          CompressingHttpServletResponse.VARY_HEADER, CompressingHttpServletResponse.ACCEPT_ENCODING_HEADER);
    }

		request.setAttribute(ALREADY_APPLIED_KEY, Boolean.TRUE);
		chain.doFilter(chainRequest, chainResponse);

		if (attemptingToCompressResponse) {

			CompressingHttpServletResponse compressingResponse = (CompressingHttpServletResponse) chainResponse;

			// We close the response in all cases since much of the logic in this filter depends upon
			// close() getting called at some point; it seems that some containers do not explicitly
			// call close() in all situations, so we do here. We also close it because if compressed data
			// has been written to the stream, it's almost certainly not valid or even possible to write more
			// to the stream after this filter anyway.
			logger.logDebug("Closing the response (if not already closed)...");
			try {
				// This will also flush
				compressingResponse.close();
			} catch (IOException ioe) {
				// underlying stream might have been closed -- ignore IOException here
				logger.logDebug("Error while flushing buffer", ioe);
			}

			if (compressingResponse.isCompressing()) {
				chainRequest.setAttribute(COMPRESSED_KEY, Boolean.TRUE);
			}

			if (statsEnabled) {
				context.getStats().incrementNumResponsesCompressed();
			}

		} else {
			if (statsEnabled) {
				context.getStats().incrementTotalResponsesNotCompressed();
			}
		}

	}

	private ServletRequest getRequest(ServletRequest request) {
		if (!(request instanceof HttpServletRequest)) {
			logger.logDebug("Can't compress non-HTTP request");
			return null;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String contentEncoding = httpRequest.getHeader(CompressingHttpServletResponse.CONTENT_ENCODING_HEADER);
		if (contentEncoding == null) {
			logger.logDebug("Request is not compressed, so not decompressing");
			return null;
		}

		if (!CompressingStreamFactory.isSupportedRequestContentEncoding(contentEncoding)) {
			logger.logDebug("Can't decompress request with encoding: " + contentEncoding);
			return null;
		}

		return new CompressedHttpServletRequest(httpRequest,
				                                CompressingStreamFactory.getFactoryForContentEncoding(contentEncoding),
				                                context);
	}

	private ServletResponse getResponse(ServletRequest request,
	                                    ServletResponse response) {
		if (response.isCommitted() || request.getAttribute(ALREADY_APPLIED_KEY) != null) {
			logger.logDebug("Response committed or filter has already been applied");
			return null;
		}

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			logger.logDebug("Can't compress non-HTTP request, response");
			return null;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		if (logger.isDebug()) {
			logger.logDebug("Request for: '" + httpRequest.getRequestURI() + '\'');
		}

		String contentEncoding = CompressingStreamFactory.getBestContentEncoding(httpRequest);
		assert contentEncoding != null;

		if (CompressingStreamFactory.NO_ENCODING.equals(contentEncoding)) {
			logger.logDebug("Compression not supported or declined by request");
			return null;
		}

		String requestURI = httpRequest.getRequestURI();
		if (!isCompressablePath(requestURI)) {
			logger.logDebug("Compression disabled for path: " + requestURI);
			return null;
		}

		String userAgent = httpRequest.getHeader("User-Agent");
		if (!isCompressableUserAgent(userAgent)) {
			logger.logDebug("Compression disabled for User-Agent: " + userAgent);
			return null;
		}

		if (logger.isDebug()) {
			logger.logDebug("Compression supported; using content encoding '" + contentEncoding + '\'');
		}

		CompressingStreamFactory compressingStreamFactory =
		    CompressingStreamFactory.getFactoryForContentEncoding(contentEncoding);

		return new CompressingHttpServletResponse(httpResponse,
		                                          compressingStreamFactory,
		                                          contentEncoding,
		                                          context);
	}

	public void destroy() {
		logger.log("CompressingFilter is being destroyed...");
	}

	/**
	 * Checks to see if the given path should be compressed. This checks against the <code>includePathPatterns</code>
	 * and <code>excludePathPatterns</code> filter init parameters; if the former is set and the given path matches
	 * a regular expression in that parameter's list, or if the latter is set and the path does not match, then
	 * this method returns <code>true</code>.
	 *
	 * @param path request path
	 * @return true if and only if the path should be compressed
	 */
	private boolean isCompressablePath(String path) {
		if (path != null) {
			for (Pattern pattern : context.getPathPatterns()) {
				if (pattern.matcher(path).matches()) {
					return context.isIncludePathPatterns();
				}
			}
		}
		return !context.isIncludePathPatterns();
	}

	private boolean isCompressableUserAgent(String userAgent) {
		if (userAgent != null) {
			for (Pattern pattern : context.getUserAgentPatterns()) {
				if (pattern.matcher(userAgent).matches()) {
					return context.isIncludeUserAgentPatterns();
				}
			}
		}
		return !context.isIncludeUserAgentPatterns();
	}

	@Override
	public String toString() {
		return VERSION_STRING;
	}

}

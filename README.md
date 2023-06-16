ziplet
===============
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.ziplet/ziplet/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.ziplet/ziplet)
[![Build Status](https://travis-ci.org/ziplet/ziplet.svg?branch=master)](https://travis-ci.org/ziplet/ziplet)

This filter can, based on HTTP headers in a HttpServletRequest, compress data written to the HttpServletResponse, or
decompress data read from the request. When supported by the client browser, this can potentially greatly reduce the
number of bytes written across the network from and to the client. As a Filter, this class can also be easily added to
any JakartaEE web application.


Features
--------

Why might you want to use this solution compared to others?

* Little in-memory buffering
* Handles compressed requests too
* Selective compression based on content type, size, or user agent
* Exposes compression statistics
* Easy Logging integration (SL4J)
* Installation

Add the ziplet-XX.jar file containing CompressingFilter to your web application's WEB-INF/lib directory:

```xml
    <dependency>
        <groupId>com.github.ziplet</groupId>
        <artifactId>ziplet</artifactId>
        <version>2.4.1</version>
    </dependency>
```

Add the following entries to your web.xml deployment descriptor:

```xml
    <filter>
        <filter-name>CompressingFilter</filter-name>
        <filter-class>com.github.ziplet.filter.compression.CompressingFilter</filter-class>
    </filter>
```

```xml
    <filter-mapping>
        <filter-name>CompressingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
```

Configuration
-------------

CompressingFilter supports the following parameters:

**debug** (optional): if set to "true", additional debug information will be written to the servlet log. Defaults to
false.

**compressionThreshold** (optional): sets the size of the smallest response that will be compressed, in bytes. That is,
if less than compressionThreshold bytes are written to the response, it will not be compressed and the response will go
to the client unmodified. If 0, compression always begins immediately. Defaults to 1024.

**compressionLevel** (optional): sets the compression level used for response gzip/deflate compression, from 1 (fastest
compression, less CPU) to 9 (best compression, more CPU), or -1 (platform default, currently equivalent to 6). Defaults
to -1.

**statsEnabled** (optional): enables collection of statistics. See CompressingFilterStats. Defaults to false. Don't use
this in high traffic environments!

**includeContentTypes** (optional): if specified, this is treated as a comma-separated list of content types (e.g.
text/html,text/xml). The filter will attempt to only compress responses which specify one of these values as its content
type, for example via ServletResponse.setContentType(String). Note that the filter does not know the response content
type at the time it is applied, and so must apply itself and later attempt to disable compression when content type has
been set. This will fail if the response has already been committed. Also note that this parameter cannot be specified
if excludeContentTypes is too.

**excludeContentTypes** (optional): same as above, but specifies a list of content types to not compress. Everything
else will be compressed. However note that any content type that indicates a compressed format (e.g. application/gzip,
application/x-compress) will not be compressed in any event.

**includePathPatterns** (optional): if specified, this is treated as a comma-separated list of regular expressions (of
the type accepted by Pattern) which match exactly those paths which should be compressed by this filter. Anything else
will not be compressed. One can also merely apply the filter to a subset of all URIs served by the web application using
standard filter-mapping elements in web.xml; this element provides more fine-grained control for when that mechanism is
insufficient. "Paths" here means values returned by HttpServletRequest.getRequestURI(). Note that the regex must match
the filename exactly; pattern "static" does not match everything containing the string "static. Use ".*static.*" for
that, for example. This cannot be specified if excludeFileTypes is too.

**excludePathPatterns** (optional): same as above, but specifies a list of patterns which match paths that should not be
compressed. Everything else will be compressed.

**includeUserAgentPatterns** (optional): Like includePathPatterns. Only requests with User-Agent headers whose value
matches one of these regular expressions will be compressed. Can't be specified if excludeUserAgentPatterns is too.

**excludeUserAgentPatterns** (optional): as above, requests whose User-Agent header matches one of these patterns will
not be compressed.

**noVaryHeaderPatterns** (optional): Like includeUserAgentPatterns. Requests with User-Agent headers whose value matches
one of these regular expressions result in a response that does not contain the Vary-header Since version 1.8

These values are configured in web.xml as well with init-param elements:

```xml
    <filter>
        <filter-name>CompressingFilter</filter-name>
        <filter-class>com.github.ziplet.filter.compression.CompressingFilter</filter-class>
        <init-param>
            <param-name>debug</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>
```

Supported compression algorithms
--------------------------------

Response

This filter supports the following compression algorithms when compressing data to the repsonse, as specified in the "
Accept-Encoding" HTTP request header:

*gzip
*x-gzip
*compress
*x-compress
*deflate
*identity (that is, no compression)
*Request

This filter supports the following compression algorithms when decompressing data from the request body, as specified in
the "Content-Encoding" HTTP request header:

*gzip
*x-gzip
*compress
*x-compress
*deflate
*identity
*Controlling runtime behavior

An application may force the encoding / compression used by setting an "Accept-Encoding" value into the request as an
attribute under the key FORCE_ENCODING_KEY. Obviously this has to be set upstream from the filter, not downstream.

Caveats and Notes
-----------------

The filter requires Java 8 and JakartaEE.

Note that if this filter decides that it should try to compress the response, it will close the response (whether or not
it ends up compressing the response). No more can be written to the response after this filter has been applied; this
should never be necessary anyway. Put this filter ahead of any filters that might try to write to the response, since
presumably you want this content compressed too anyway.

If a OutputStream.flush() occurs before the filter has decided whether to compress or not, it will be forced into
compression mode.

The filter will not compress if the response sets Cache-Control: no-transform header in the response.

The filter attempts to modify the ETag response header, if present, when compressing. This is because the compressed
response must be considered a separate entity by caches. It simply appends, for example, "-gzip" to the ETag header
value when compressing with gzip. This is not guaranteed to work in all containers, in the sense that some containers
may not properly associated this ETag with the compressed content and simply return the response again.

The filter normally sets the Vary response header to indicate that a different response may be returned based on the
Accept-Encoding header of the request. This can be configured in the web.xml.

License
-------

This project is published under the Apache License, Version 2.0. For details see files LICENSE and NOTICE.

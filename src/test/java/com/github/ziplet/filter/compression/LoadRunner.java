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


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Can be used to generate load on {@link CompressingFilter}.
 *
 * @author Sean Owen
 */
public final class LoadRunner {

    private LoadRunner() {
        // do nothing
    }

    public static void main(String... args) throws ServletException, IOException {

        MockFilterConfig context = new MockFilterConfig();
        context.addInitParameter("debug", "true");
        context.addInitParameter("statsEnabled", "true");
        CompressingFilter filter = new CompressingFilter();
        filter.init(context);

        Random r = new Random(0xDEADBEEFL);
        final String[] data = new String[200];
        for (int i = 0; i < data.length; i++) {
            byte[] bytes = new byte[50];
            r.nextBytes(bytes);
            data[i] = new String(bytes);
        }

        HttpTestServlet servlet = new HttpTestServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
                PrintWriter writer = response.getWriter();
                for (String string : data) {
                    writer.print(string);
                }
            }
        };
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Encoding", "gzip");

        long start = System.currentTimeMillis();
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            new MockFilterChain(servlet, filter).doFilter(request, new MockHttpServletResponse());
        }
        long end = System.currentTimeMillis();
        long time = end - start;
        System.out
                .println(
                        "Completed in " + time + "ms (" + (double) time / iterations + " per request)");

    }
}

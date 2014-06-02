/*
 * JSmart5 - Java Web Development Framework
 * Copyright (c) 2014, Jeferson Albino da Silva, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
*/

package com.jsmart5.framework.manager;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import static com.jsmart5.framework.manager.SmartConstants.*;
import static com.jsmart5.framework.manager.SmartExpression.*;

public final class SmartOutputFilter implements Filter {

	private static final Pattern OUTPUT_PATTERN = Pattern.compile(EL_PATTERN);

	@Override
	public void init(FilterConfig config) throws ServletException {
		// DO NOTHING
	}

	@Override
	public void destroy() {
		// DO NOTHING
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		final HttpServletResponseWrapper responseWrapper = (HttpServletResponseWrapper) response;

        filterChain.doFilter(request, responseWrapper);

        boolean foundRegex = false;
        String html = responseWrapper.toString();

        Matcher outputMatcher = OUTPUT_PATTERN.matcher(html);
	    while (outputMatcher.find()) {
	    	foundRegex = true;
	    	String expression = outputMatcher.group();
	    	Object value = EXPRESSIONS.getExpressionValue(expression);
	    	html = html.replace(expression, value != null ? value.toString() : "");
	    }

        // Write our modified text to the real response
	    if (foundRegex) {
		    responseWrapper.reset();
		    responseWrapper.setContentLength(html.getBytes().length);
	        PrintWriter out = responseWrapper.getWriter();
	        out.write(html);
	        // out.close();
	    }
	}

}

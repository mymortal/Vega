/*******************************************************************************
 * Copyright (c) 2011 Subgraph.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Subgraph - initial API and implementation
 ******************************************************************************/
package com.subgraph.vega.internal.http.proxy;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HTTP;

import com.subgraph.vega.http.requests.custom.HttpEntityEnclosingMutableRequest;
import com.subgraph.vega.http.requests.custom.HttpMutableRequest;

public class UriRequestCreator {
	private final static String HTTP_SCHEME = "http://";
	private final static String HTTPS_SCHEME = "https://";

	private final boolean preferHostHeader;

	public UriRequestCreator(boolean preferHostHeader) {
		this.preferHostHeader = preferHostHeader;
	}

	public HttpUriRequest createUriRequest(HttpRequest request, boolean isSSL) throws HttpException {
		final URI uri = getUriForRequest(request, isSSL);
		final HttpUriRequest uriRequest;
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingMutableRequest tmp = new HttpEntityEnclosingMutableRequest(request.getRequestLine().getMethod(), uri);
			tmp.setEntity(((HttpEntityEnclosingRequest) request).getEntity());
			uriRequest = tmp;
		} else {
			uriRequest = new HttpMutableRequest(request.getRequestLine().getMethod(), uri);
		}
		uriRequest.setParams(request.getParams());
		uriRequest.setHeaders(request.getAllHeaders());
		return uriRequest;
	}

	public URI getUriForRequest(HttpRequest request, boolean isSSL) throws HttpException {
		final String scheme = (isSSL) ? HTTPS_SCHEME : HTTP_SCHEME;
		final String hostname = getHostname(request);
		final String pathAndQuery = extractPathAndQuery(request.getRequestLine().getUri());

		if(hostname == null) {
			throw new HttpException("Cannot create URI from request because no hostname is specified in either Host header or HTTP request line");
		}

		try {
			return new URI(scheme + hostname + pathAndQuery);
		} catch (URISyntaxException e) {
			throw new HttpException("Cannot create URI from request because URI format is incorrect.", e);
		}
	}

	private String getHostname(HttpRequest request) {
		final String headerHostname = requestHostHeaderValue(request);
		final String pathHostname = maybeExtractHostname(request.getRequestLine().getUri());
		if(headerHostname != null && pathHostname != null) {
			return (preferHostHeader) ? (headerHostname) : (pathHostname);
		}
		return (headerHostname != null) ? (headerHostname) : (pathHostname);	
	}

	private String requestHostHeaderValue(HttpRequest request) {
		final Header hostHeader = request.getFirstHeader(HTTP.TARGET_HOST);
		if(hostHeader == null) {
			return null;
		}

		final String hostValue = hostHeader.getValue();
		return (hostValue == null || hostValue.isEmpty()) ? (null) : (hostValue);
	}

	private String maybeExtractHostname(String requestUri) {
		final int hostnameIndex = indexOfHostname(requestUri);
		if(hostnameIndex == -1)
			return null;
		final int pathIndex = indexOfPath(requestUri);
		return requestUri.substring(hostnameIndex, pathIndex);
	}

	private int indexOfHostname(String requestUri) {
		if(requestUri.startsWith(HTTP_SCHEME)) {
			return HTTP_SCHEME.length();
		} else if (requestUri.startsWith(HTTPS_SCHEME)) {
			return HTTPS_SCHEME.length();
		} else {
			return -1;
		}
	}

	private String extractPathAndQuery(String requestUri) {
		final int hostnameIndex = indexOfHostname(requestUri);
		if(hostnameIndex == -1) {
			return maybeCreateRootPath(requestUri);
		} 
		final int pathIndex = indexOfPath(requestUri);
		return maybeCreateRootPath( requestUri.substring(pathIndex) );
	}

	private String maybeCreateRootPath(String path) {
		return (path.isEmpty()) ? ("/") : (path);
	}

	/*
	 * 
	 * Returns 0 if not an absolute URI, otherwise searches for the first '/' character
	 * after the hostname and if not found returns the length of the String.
	 * 
	 * If result is 0 then the whole string is the path otherwise,  the path can 
	 * be extracted with requestUri.substring(pathIndex) and the hostname can
	 * be extracted with requestUri.substring(hostnameIndex, pathIndex)
	 * 
	 */
	private int indexOfPath(String requestUri) {
		final int hostnameIndex = indexOfHostname(requestUri);
		if(hostnameIndex == -1)
			return 0;
		final int idx = requestUri.indexOf("/", hostnameIndex);

		return (idx == -1) ? (requestUri.length()) : (idx);			
	}
}

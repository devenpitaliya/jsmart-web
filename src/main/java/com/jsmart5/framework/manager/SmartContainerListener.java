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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;

import org.springframework.web.context.ContextLoader;

import static com.jsmart5.framework.manager.SmartImage.*;
import static com.jsmart5.framework.manager.SmartConfig.*;
import static com.jsmart5.framework.manager.SmartHandler.*;
import static com.jsmart5.framework.manager.SmartText.*;

@WebListener
public final class SmartContainerListener implements ServletContextListener {

	private static final List<String> METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE");

	private static final ContextLoader CONTEXT_LOADER = new ContextLoader();

	@Override
	@SuppressWarnings("unchecked")
	public void contextInitialized(ServletContextEvent sce)  {
		try {
			ServletContext servletContext = sce.getServletContext();

			CONFIG.init(servletContext);
	        if (CONFIG.getContent() == null) {
	        	throw new RuntimeException("Configuration file jsmart5.xml was not found in WEB-INF resources folder!");
	        }

	        // Configure the necessary parameters in the Servlet context to get Spring to configure the application without needing an XML file
	        servletContext.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");

	        String contextConfigLocation = "com.jsmart5.framework.manager";
	        if (CONFIG.getContent().getSmartScan() != null) {
	        	contextConfigLocation += "," + CONFIG.getContent().getSmartScan();
	        }

	        // Tell Spring where to scan for annotations
	        servletContext.setInitParameter("contextConfigLocation", contextConfigLocation);

	        CONTEXT_LOADER.initWebApplicationContext(servletContext);

	        IMAGES.init(servletContext);
	        TEXTS.init(CONFIG.getContent().getMessageFiles(), CONFIG.getContent().getDefaultLocale());
	        HANDLER.init(servletContext);

	        // SmartServlet -> @MultipartConfig @WebServlet(name = "SmartServlet", displayName = "SmartServlet", loadOnStartup = 1)
	        Servlet smartServlet = servletContext.createServlet((Class<? extends Servlet>) Class.forName("com.jsmart5.framework.manager.SmartServlet"));
	        ServletRegistration.Dynamic servletReg = (ServletRegistration.Dynamic) servletContext.addServlet("SmartServlet", smartServlet);
	        servletReg.setLoadOnStartup(1);

	        // SmartServlet Initial Parameters
	        SmartInitParam[] initParams = CONFIG.getContent().getInitParams();
	        if (initParams != null) {
	        	for (SmartInitParam initParam : initParams) {
	        		servletReg.setInitParameter(initParam.getName(), initParam.getValue());
	        	}
	        }

	        // MultiPart to allow file upload on SmartServlet 
	        MultipartConfigElement multipartElement = getServletMultipartElement();
	        if (multipartElement != null) {
	        	servletReg.setMultipartConfig(multipartElement);
	        }

	        // Security constraint to SmartServlet
	        ServletSecurityElement servletSecurityElement = getServletSecurityElement(servletContext);
	        if (servletSecurityElement != null) {
	        	servletReg.setServletSecurity(servletSecurityElement);
	        }

	        // TODO: Fix problem related to authentication by container to use SSL dynamically (Maybe create more than one servlet for secure and non-secure patterns)
	        // Check also the use of request.login(user, pswd)
	        // Check the HttpServletRequest.BASIC_AUTH, CLIENT_CERT_AUTH, FORM_AUTH, DIGEST_AUTH
	        // servletReg.setRunAsRole("admin");
	        // servletContext.declareRoles("admin");

	        // SmartServlet URL mapping
	        String[] servletMapping = getServletMapping();
	        servletReg.addMapping(servletMapping);

	        // Add custom filters defined by client
	        if (CONFIG.getContent().getCustomFilters() != null) {
	        	for (String filterClass : CONFIG.getContent().getCustomFilters()) {

	        		Filter customFilter = servletContext.createFilter((Class<? extends Filter>) Class.forName(filterClass));
	     	        FilterRegistration.Dynamic customFilterReg = (FilterRegistration.Dynamic) servletContext.addFilter(filterClass, customFilter);

	     	       customFilterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR, 
	     	        		DispatcherType.INCLUDE, DispatcherType.ASYNC), true, "/*");
	        	}
	        }

			// SmartErrorFilter -> @WebFilter(urlPatterns = {"/*"})
	        Filter errorFilter = servletContext.createFilter((Class<? extends Filter>) Class.forName("com.jsmart5.framework.manager.SmartErrorFilter"));
	        FilterRegistration.Dynamic errorFilterReg = (FilterRegistration.Dynamic) servletContext.addFilter("SmartErrorFilter", errorFilter);

	        errorFilterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR, 
	        		DispatcherType.INCLUDE, DispatcherType.ASYNC), true, "/*");

	        // SmartCacheFilter -> @WebFilter(urlPatterns = {"/*"})
	        Filter cacheFilter = servletContext.createFilter((Class<? extends Filter>) Class.forName("com.jsmart5.framework.manager.SmartCacheFilter"));
	        FilterRegistration.Dynamic cacheFilterReg = (FilterRegistration.Dynamic) servletContext.addFilter("SmartCacheFilter", cacheFilter);

	        cacheFilterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR, 
	        		DispatcherType.INCLUDE, DispatcherType.ASYNC), true, "/*");

	        // SmartWebFilter -> @WebFilter(servletNames = {"SmartServlet"})
	        Filter webFilter = servletContext.createFilter((Class<? extends Filter>) Class.forName("com.jsmart5.framework.manager.SmartWebFilter"));
	        FilterRegistration.Dynamic webFilterReg = (FilterRegistration.Dynamic) servletContext.addFilter("SmartWebFilter", webFilter);

	        webFilterReg.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR, 
	        		DispatcherType.INCLUDE, DispatcherType.ASYNC), true, "SmartServlet");

	        // SmartOutputFilter -> @WebFilter(servletNames = {"SmartServlet"})
	        Filter outputFilter = servletContext.createFilter((Class<? extends Filter>) Class.forName("com.jsmart5.framework.manager.SmartOutputFilter"));
	        FilterRegistration.Dynamic outputFilterReg = (FilterRegistration.Dynamic) servletContext.addFilter("SmartOutputFilter", outputFilter);

	        outputFilterReg.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR, 
	        		DispatcherType.INCLUDE, DispatcherType.ASYNC), true, "SmartServlet");

	        // SmartSessionControl - > @WebListener
	        EventListener smartSessionListener = servletContext.createListener((Class<? extends EventListener>) Class.forName("com.jsmart5.framework.manager.SmartSessionControl"));
	        servletContext.addListener(smartSessionListener);


	        // Custom SmartServlet -> Custom Servlets created by application
	        for (String servletName : HANDLER.smartServlets.keySet()) {
	        	Servlet customServlet = servletContext.createServlet((Class<? extends Servlet>) HANDLER.smartServlets.get(servletName));
	        	HANDLER.executeInjection(customServlet);

	        	com.jsmart5.framework.annotation.SmartServlet customSmartServlet = customServlet.getClass().getAnnotation(com.jsmart5.framework.annotation.SmartServlet.class);
	        	ServletRegistration.Dynamic customReg = (ServletRegistration.Dynamic) servletContext.addServlet(servletName, customServlet);

	        	customReg.setLoadOnStartup(customSmartServlet.loadOnStartup());
	        	customReg.setAsyncSupported(customSmartServlet.asyncSupported());

	        	WebInitParam[] customInitParams = customSmartServlet.initParams();
	        	if (customInitParams != null) {
	        		for (WebInitParam customInitParam : customInitParams) {
	        			customReg.setInitParameter(customInitParam.name(), customInitParam.value());
	        		}
	        	}

		        // Add mapping url for custom servlet
		        customReg.addMapping(customSmartServlet.urlPatterns());

		        if (customServlet.getClass().isAnnotationPresent(MultipartConfig.class)) {
		        	customReg.setMultipartConfig(new MultipartConfigElement(customServlet.getClass().getAnnotation(MultipartConfig.class)));
		        }
	        }

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		HANDLER.destroy(sce.getServletContext());
		CONTEXT_LOADER.closeWebApplicationContext(sce.getServletContext());
	}

	private MultipartConfigElement getServletMultipartElement() {
		SmartUploadConfig uploadConfig = null;
		MultipartConfigElement multipartElement = new MultipartConfigElement("");

        if ((uploadConfig = CONFIG.getContent().getUploadConfig()) != null) {
        	multipartElement = new MultipartConfigElement(uploadConfig.getLocation(), uploadConfig.getMaxFileSize(), uploadConfig.getMaxRequestSize(), uploadConfig.getFileSizeThreshold());
        }

        return multipartElement;
	}

	private ServletSecurityElement getServletSecurityElement(ServletContext servletContext) {
		SmartSecureMethod[] smartMethods = CONFIG.getContent().getSecureMethods();

        if (smartMethods != null && smartMethods.length > 0) {

        	HttpConstraintElement constraint = new HttpConstraintElement();

        	SmartSecureMethod allMethods = CONFIG.getContent().getSecureMethod("*");
        	Set<HttpMethodConstraintElement> methodConstraints = new HashSet<HttpMethodConstraintElement>();

        	if (allMethods != null) {
        		for (String method : METHODS) {
        			HttpConstraintElement constraintElement = getHttpConstraintElement(allMethods);
        			if (constraintElement != null) {
        				methodConstraints.add(new HttpMethodConstraintElement(method, constraintElement));
        			}
        		}

        	} else {
        		for (SmartSecureMethod method : smartMethods) {
        			HttpConstraintElement constraintElement = getHttpConstraintElement(method);
        			if (constraintElement != null) {

        				if (method.getMethod() == null || !METHODS.contains(method.getMethod().toUpperCase())) {
        					throw new RuntimeException("Method name declared in <secure-method> tag is unsupported! Supported values are HTTP methods.");
        				}
        				methodConstraints.add(new HttpMethodConstraintElement(method.getMethod().toUpperCase(), constraintElement));
        			}
        		}
        	}

            return new ServletSecurityElement(constraint, methodConstraints);
        }

        return null;
	}

	private HttpConstraintElement getHttpConstraintElement(SmartSecureMethod smartMethod) {
		HttpConstraintElement constraintElement = null;

		if (smartMethod.getEmptyRole() != null && smartMethod.getTransport() != null) {

			EmptyRoleSemantic emptyRole = getEmptyRoleSemantic(smartMethod.getEmptyRole());

			TransportGuarantee transport = getTransportGuarantee(smartMethod.getTransport());

			if (transport == null || emptyRole == null) {
				throw new RuntimeException("Invalid transport or emptyRole attribute for <secure-method> tag! Values allowed are confidential or none.");
			}
			constraintElement = new HttpConstraintElement(emptyRole, transport, smartMethod.getRoles() != null ? smartMethod.getRoles() : new String[]{});

		} else if (smartMethod.getTransport() != null) {

			TransportGuarantee transport = getTransportGuarantee(smartMethod.getTransport());

			if (transport == null) {
				throw new RuntimeException("Invalid transport attribute for <secure-method> tag! Values allowed are confidential or none.");
			}
			constraintElement = new HttpConstraintElement(transport, smartMethod.getRoles() != null ? smartMethod.getRoles() : new String[]{});

		} else if (smartMethod.getEmptyRole() != null) {

			EmptyRoleSemantic emptyRole = getEmptyRoleSemantic(smartMethod.getEmptyRole());

			if (emptyRole == null) {
				throw new RuntimeException("Invalid emptyRole attribute for <secure-method> tag! Values allowed are deny or permit.");
			}
			constraintElement = new HttpConstraintElement(emptyRole);
		}

		return constraintElement;
	}

	private TransportGuarantee getTransportGuarantee(String transport) {
		return transport.equalsIgnoreCase("confidential") ? TransportGuarantee.CONFIDENTIAL : transport.equalsIgnoreCase("none") ? TransportGuarantee.NONE : null;
	}

	private EmptyRoleSemantic getEmptyRoleSemantic(String emptyRole) {
		return emptyRole.equalsIgnoreCase("deny") ? EmptyRoleSemantic.DENY : emptyRole.equalsIgnoreCase("permit") ? EmptyRoleSemantic.PERMIT : null;
	}

	private String[] getServletMapping() {
		List<String> mapping = new ArrayList<String>();

		if (CONFIG.getContent().getUrlPatterns() == null) {
        	throw new RuntimeException("None <url-pattern> tags were found in configuration file jsmart5.xml for url mapping! At lease one URL pattern must be informed.");
        }

    	for (SmartUrlPattern urlPattern : CONFIG.getContent().getUrlPatterns()) {
    		mapping.add(urlPattern.getUrl());
    	}

        CONFIG.addMappedUrls(mapping);

        return mapping.toArray(new String[mapping.size()]);
	}

}

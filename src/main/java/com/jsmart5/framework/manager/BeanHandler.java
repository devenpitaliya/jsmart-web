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
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import com.jsmart5.framework.annotation.AuthenticateBean;
import com.jsmart5.framework.annotation.AuthenticateField;
import com.jsmart5.framework.annotation.AuthorizeAccess;
import com.jsmart5.framework.annotation.ExecuteAccess;
import com.jsmart5.framework.annotation.PostPreset;
import com.jsmart5.framework.annotation.PostSubmit;
import com.jsmart5.framework.annotation.PreSubmit;
import com.jsmart5.framework.annotation.ScopeType;
import com.jsmart5.framework.annotation.SmartBean;
import com.jsmart5.framework.annotation.SmartFilter;
import com.jsmart5.framework.annotation.SmartListener;
import com.jsmart5.framework.annotation.Unescape;
import com.jsmart5.framework.config.UrlPattern;
import com.jsmart5.framework.listener.SmartContextListener;
import com.jsmart5.framework.listener.SmartSessionListener;
import com.jsmart5.framework.util.SmartUtils;

import static com.jsmart5.framework.config.Config.*;
import static com.jsmart5.framework.config.Constants.*;
import static com.jsmart5.framework.manager.ExpressionHandler.*;

public enum BeanHandler {

	HANDLER();

	private static final Logger LOGGER = Logger.getLogger(BeanHandler.class.getPackage().getName());

	private static final Pattern HANDLER_EL_PATTERN = Pattern.compile(EL_PATTERN.pattern() + "|" + URL_PARAM_PATTERN.pattern() + "|" + INCLUDE_JSPF_PATTERN.pattern());
	
	private static final Pattern SPRING_VALUE_PATTERN = Pattern.compile("[\\$,\\{,\\}]*");

	Map<String, Class<?>> smartBeans;

	Map<String, Class<?>> authBeans;

	Map<String, Class<?>> smartServlets;

	Map<String, Class<?>> smartFilters;

	Set<SmartContextListener> contextListeners;
	
	Set<SmartSessionListener> sessionListeners;

	private Map<String, String> forwardPaths;

	private InitialContext initialContext;

	private ApplicationContext springContext;

    private Map<Class<?>, String> jndiMapping = new HashMap<Class<?>, String>();

	private Map<Class<?>, Field[]> mappedBeanFields = new HashMap<Class<?>, Field[]>();

	private Map<Class<?>, Method[]> mappedBeanMethods = new HashMap<Class<?>, Method[]>();

	private Map<String, JspPageBean> jspPageBeans = new HashMap<String, JspPageBean>();

	void init(ServletContext context) {
		checkWebXmlPath(context);
		initJndiMapping();
		initAnnotatedBeans(context);
		initForwardPaths(context);
		initJspPageBeans(context);
	}

	void destroy(ServletContext context) {
        try {
        	finalizeBeans(context);
        	authBeans.clear();
        	smartBeans.clear();
        	smartServlets.clear();
        	smartFilters.clear();
        	contextListeners.clear();
        	sessionListeners.clear();
        	forwardPaths.clear();
        	jspPageBeans.clear();
        	jndiMapping.clear();
        	initialContext = null;
        	springContext = null;
        } catch (Exception ex) {
        	LOGGER.log(Level.INFO, "Failure to destroy SmartHandler: " + ex.getMessage());
        }
	}

	void setSpringContext(ApplicationContext springContext) {
		this.springContext = springContext;
	}

	@SuppressWarnings("all")
	void executePreSubmit(Object bean) {
		for (Method method : getBeanMethods(bean.getClass())) {
			if (method.isAnnotationPresent(PreSubmit.class)) {
				try {
					method.invoke(bean, null);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return;
			}
		}
	}

	@SuppressWarnings("all")
	void executePostSubmit(Object bean) {
		for (Method method : getBeanMethods(bean.getClass())) {
			if (method.isAnnotationPresent(PostSubmit.class)) {
				try {
					method.invoke(bean, null);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return;
			}
		}
	}

	@SuppressWarnings("all")
	void executePreDestroy(Object bean) {
		for (Method method : getBeanMethods(bean.getClass())) {
			if (method.isAnnotationPresent(PreDestroy.class)) {
				try {
					method.invoke(bean, null);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return;
			}
		}
	}

	@SuppressWarnings("all")
	void executePostConstruct(Object bean) {
		for (Method method : getBeanMethods(bean.getClass())) {
			if (method.isAnnotationPresent(PostConstruct.class)) {
				try {
					method.invoke(bean, null);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return;
			}
		}
	}

	void executeUrlParams(String name, Set<String> urlParams) {
		if (urlParams != null && !urlParams.isEmpty()) {
			Set<String> tempPresets = new HashSet<String>();

			for (String urlParam : urlParams) {

				if (urlParam.contains(name) && !tempPresets.contains(urlParam)) {
					tempPresets.add(urlParam);

					String attrName = urlParam.substring(urlParam.indexOf(URL_PARAM_NAME_ATTR) + URL_PARAM_NAME_ATTR.length());
					String attrParam = urlParam.substring(urlParam.indexOf(URL_PARAM_PARAM_ATTR) + URL_PARAM_PARAM_ATTR.length());

					EXPRESSIONS.setExpressionValue(attrName.substring(0, attrName.indexOf("\"")), attrParam.substring(0, attrParam.indexOf("\"")), true);
				}
			}
		}
	}

	void executePostPreset(String name, Object bean, Map<String, String> expressions) {
		try {
			if (expressions != null) {
				for (Field field : getBeanFields(bean.getClass())) {

					if (field.isAnnotationPresent(PostPreset.class)) {
						for (Entry<String, String> expr : expressions.entrySet()) {

							if (expr.getValue().contains(START_EL + name + "." + field.getName() + END_EL)) {
								EXPRESSIONS.handleRequestExpression(expr.getKey(), expr.getValue());
								expressions.remove(expr.getKey());
								break;
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.log(Level.INFO, "Execute PostPreset on smart bean " + bean + " failure: " + ex.getMessage());
		}
	}

	boolean containsUnescapeMethod(String[] names) {
		if (names != null && names.length > 1) {
			Class<?> clazz = smartBeans.get(names[0]);
			if (clazz != null) {
				for (Method method : getBeanMethods(clazz)) {
					if (method.getName().startsWith("set") && method.getName().endsWith(names[1].substring(1))) {
						return method.isAnnotationPresent(Unescape.class);
					}
				}
			}
		}
		return false;
	}

	Map<String, String> getRequestExpressions() {
		return EXPRESSIONS.getRequestExpressions();
	}

	String handleRequestExpressions(Map<String, String> expressions) throws ServletException, IOException {
		String submitExpression = null;
		for (Entry<String, String> expr : expressions.entrySet()) {
			String expression = EXPRESSIONS.handleRequestExpression(expr.getKey(), expr.getValue());
			if (expression != null) {
				submitExpression = expression;
			}
		}
		return submitExpression;
	}

	void instantiateBeans(String path, Map<String, String> expressions) throws Exception {
		JspPageBean jspPageBean = jspPageBeans.get(path);
		if (jspPageBean != null) {

			PageScope pageScope = new PageScope(path);

			for (String name : jspPageBean.getBeanNames()) {
				instantiateBean(name, expressions, jspPageBean.getUrlParams(), pageScope);
			}

			// Include into session the path with respective page scoped bean names
			if (!pageScope.getNames().isEmpty()) {
				HttpSession session = SmartContext.getSession();
				synchronized (session) {
					session.setAttribute(path, pageScope);
				}
			}
		}
	}

	private Object instantiateBean(String name, Map<String, String> expressions, Set<String> urlParams, PageScope pageScope) throws Exception {
		Object bean = null;
		ServletContext context = SmartContext.getApplication();
		HttpSession session = SmartContext.getSession();
		HttpServletRequest request = SmartContext.getRequest();

		if (request.getAttribute(name) != null) {
			bean = request.getAttribute(name);
			executeInjection(bean, pageScope);
			return bean;
		}

		synchronized (session) {
			if (session.getAttribute(name) != null) {
				bean = session.getAttribute(name);
				executeInjection(bean, pageScope);
				return bean;
			}
		}

		if (context.getAttribute(name) != null) {
			bean = context.getAttribute(name);
			executeInjection(bean, pageScope);
			return bean;
		}

		if (smartBeans.containsKey(name)) {
			Class<?> clazz = smartBeans.get(name);
			bean = clazz.newInstance();

			SmartBean servletBean = clazz.getAnnotation(SmartBean.class);
			if (servletBean.scope().equals(ScopeType.REQUEST_SCOPE)) {
				request.setAttribute(name, bean);

			} else if (servletBean.scope().equals(ScopeType.PAGE_SCOPE)) {
				synchronized (session) {
					pageScope.addName(name);
					session.setAttribute(name, bean);
				}

			} else if (servletBean.scope().equals(ScopeType.SESSION_SCOPE)) {
				synchronized (session) {
					session.setAttribute(name, bean);
				}

			} else if (servletBean.scope().equals(ScopeType.APPLICATION_SCOPE)) {
				context.setAttribute(name, bean);

			} else {
				return null;
			}

			executeInjection(bean, pageScope);
			executeUrlParams(name, urlParams);
			executePostPreset(name, bean, expressions);
			executePostConstruct(bean);
		}
		return bean;
	}

	void executeInjection(Object bean) {
		executeInjection(bean, null);
	}

	private String getClassName(SmartBean smartBean, Class<?> beanClass) {
		if (smartBean.name().isEmpty()) {
			String beanName = beanClass.getSimpleName();
			return beanName.replaceFirst(beanName.substring(0, 1), beanName.substring(0, 1).toLowerCase());
		}
		return smartBean.name();
	}

	private String getClassName(AuthenticateBean authBean, Class<?> authClass) {
		if (authBean.name().isEmpty()) {
			String beanName = authClass.getSimpleName();
			return beanName.replaceFirst(beanName.substring(0, 1), beanName.substring(0, 1).toLowerCase());
		}
		return authBean.name();
	}

	private String getClassName(com.jsmart5.framework.annotation.SmartServlet servlet, Class<?> servletClass) {
		if (servlet.name() == null || servlet.name().isEmpty()) {
			String servletName = servletClass.getSimpleName();
			return servletName.replaceFirst(servletName.substring(0, 1), servletName.substring(0, 1).toLowerCase());
		}
		return servlet.name();
	}

	private String getClassName(SmartFilter filter, Class<?> filterClass) {
		if (filter.name() == null || filter.name().isEmpty()) {
			String filterName = filterClass.getSimpleName();
			return filterName.replaceFirst(filterName.substring(0, 1), filterName.substring(0, 1).toLowerCase());
		}
		return filter.name();
	}

	private void executeInjection(Object bean, PageScope pageScope) {
		try {
			for (Field field : getBeanFields(bean.getClass())) {
				if (field.isAnnotationPresent(Inject.class)) {

					SmartBean sb = field.getType().getAnnotation(SmartBean.class);
					if (sb != null) {
						field.setAccessible(true);
						field.set(bean, instantiateBean(getClassName(sb, field.getType()), null, null, pageScope));
						continue;
					}

					AuthenticateBean ab = field.getType().getAnnotation(AuthenticateBean.class);
					if (ab != null) {
						field.setAccessible(true);
						field.set(bean, instantiateAuthBean(getClassName(ab, field.getType()), SmartContext.getSession()));
						continue;
					}
				}

				// Inject dependencies
				if (field.getAnnotations().length > 0) {

					if (initialContext != null && jndiMapping.containsKey(field.getType())) {
						field.setAccessible(true);
						field.set(bean, initialContext.lookup(jndiMapping.get(field.getType())));
						continue;
					}

					if (springContext != null) {
						if (springContext.containsBean(field.getName())) {
							field.setAccessible(true);
							field.set(bean, springContext.getBean(field.getType()));

						} else if (field.isAnnotationPresent(Value.class)) {
							String propertyName = field.getAnnotation(Value.class).value();
							propertyName = SPRING_VALUE_PATTERN.matcher(propertyName).replaceAll("");
							field.setAccessible(true);
							field.set(bean, springContext.getEnvironment().getProperty(propertyName, field.getType()));
						}
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.log(Level.INFO, "Injection on smart bean " + bean + " failure: " + ex.getMessage());
		}
	}

	void finalizeBeans(ServletContext servletContext) {
		List<String> names = Collections.list(servletContext.getAttributeNames());
		for (String name : names) {
			Object bean = servletContext.getAttribute(name);
			if (bean != null) {

				if (bean.getClass().isAnnotationPresent(SmartBean.class)) {
					finalizeBean(bean, servletContext);
				}
			}
		}
	}

	private void finalizeBean(Object bean, ServletContext servletContext) {
		if (bean != null) {
			executePreDestroy(bean);
			finalizeInjection(bean, servletContext);
	
			SmartBean smartBean = bean.getClass().getAnnotation(SmartBean.class);
			servletContext.removeAttribute(getClassName(smartBean, bean.getClass()));
			bean = null;
		}
	}

	void finalizeBeans(HttpSession session) {
		synchronized (session) {
			List<String> names = Collections.list(session.getAttributeNames());
			for (String name : names) {
				Object bean = session.getAttribute(name);
				if (bean != null) {

					if (bean.getClass().isAnnotationPresent(SmartBean.class)) {
						finalizeBean(bean, session);

					} else if (bean.getClass().isAnnotationPresent(AuthenticateBean.class)) {
						finalizeAuthBean(bean, session);
					}
				}
			}
		}
	}

	void finalizeBeans(String path, HttpSession session) {
		synchronized (session) {
			List<String> names = Collections.list(session.getAttributeNames());
			for (String attrname : names) {
				Object object = session.getAttribute(attrname);

				if (!attrname.equals(path) && object instanceof PageScope) {
					
					for (String name : ((PageScope) object).getNames()) {
						finalizeBean(session.getAttribute(name), session);
					}
					session.removeAttribute(attrname);
				}
			}
		}
	}

	void finalizeBean(String path, HttpSession session) {
		synchronized (session) {
			Object pageScope = session.getAttribute(path);
			if (pageScope instanceof PageScope) {

				for (String name : ((PageScope) pageScope).getNames()) {
					finalizeBean(session.getAttribute(name), session);
				}
			}
			session.removeAttribute(path);
		}
	}

	private void finalizeBean(Object bean, HttpSession session) {
		if (bean != null) {
			executePreDestroy(bean);
			finalizeInjection(bean, session);
	
			SmartBean smartBean = bean.getClass().getAnnotation(SmartBean.class);
			session.removeAttribute(getClassName(smartBean, bean.getClass()));
			bean = null;
		}
	}

	public void finalizeBeans(HttpServletRequest request) {
		List<String> names = Collections.list(request.getAttributeNames());
		for (String name : names) {

			if (smartBeans.containsKey(name)) {
				Object bean = request.getAttribute(name);

				if (bean != null && bean.getClass().isAnnotationPresent(SmartBean.class)) {
					finalizeBean(bean, request);
				}
			}
		}
	}

	private void finalizeBean(Object bean, HttpServletRequest request) {
		if (bean != null) {
			executePreDestroy(bean);
			finalizeInjection(bean, request);

			SmartBean smartBean = bean.getClass().getAnnotation(SmartBean.class);
			request.removeAttribute(getClassName(smartBean, bean.getClass()));
			bean = null;
		}
	}

	private void finalizeInjection(Object bean, Object servletObject) {
		try {
			for (Field field : getBeanFields(bean.getClass())) {
				if (field.isAnnotationPresent(Inject.class)) {

					if (field.getType().isAnnotationPresent(SmartBean.class)) {
						field.setAccessible(true);

						if (servletObject instanceof HttpServletRequest) {
							finalizeBean(field.get(bean), (HttpServletRequest) servletObject);

						} else if (servletObject instanceof HttpSession) {
							finalizeBean(field.get(bean), (HttpSession) servletObject);

						} else if (servletObject instanceof ServletContext) {
							finalizeBean(field.get(bean), (ServletContext) servletObject);
						}

						field.set(bean, null);
						continue;
					}

					if (field.getType().isAnnotationPresent(AuthenticateBean.class)) {
						field.setAccessible(true);
						field.set(bean, null);
						continue;
					}
				}

				if (field.getAnnotations().length > 0) {
					field.setAccessible(true);
					field.set(bean, null);
				}
			}
		} catch (Exception ex) {
			LOGGER.log(Level.INFO, "Finalize injection on smart bean " + bean + " failure: " + ex.getMessage());
		}
	}

	void instantiateAuthBean(HttpSession session) {
		for (String name : authBeans.keySet()) {
			instantiateAuthBean(name, session);

			// We must have only one authentication bean mapped
			break;
		}
	}

	private Object instantiateAuthBean(String name, HttpSession session) {
		synchronized (session) {
			Object bean = session.getAttribute(name);

			if (bean == null) {
				try {
					bean = authBeans.get(name).newInstance();
					for (Field field : getBeanFields(bean.getClass())) {

						if (field.getAnnotations().length > 0) {

							if (initialContext != null && jndiMapping.containsKey(field.getType())) {
								field.setAccessible(true);
								field.set(bean, initialContext.lookup(jndiMapping.get(field.getType())));
								continue;
							}

							if (springContext != null) {
								if (springContext.containsBean(field.getName())) {
									field.setAccessible(true);
									field.set(bean, springContext.getBean(field.getType()));

								} else if (field.isAnnotationPresent(Value.class)) {
									String propertyName = field.getAnnotation(Value.class).value();
									propertyName = SPRING_VALUE_PATTERN.matcher(propertyName).replaceAll("");
									field.setAccessible(true);
									field.set(bean, springContext.getEnvironment().getProperty(propertyName, field.getType()));
								}
							}
						}
					}

					executePostConstruct(bean);
					session.setAttribute(name, bean);
				} catch (Exception ex) {
					LOGGER.log(Level.INFO, "Injection on authentication smart bean " + bean + " failure: " + ex.getMessage());
				}
			}
			return bean;
		}
	}

	private void finalizeAuthBean(Object bean, HttpSession session) {
		executePreDestroy(bean);

		try {
			for (Field field : getBeanFields(bean.getClass())) {
				if (field.getAnnotations().length > 0) {
					field.setAccessible(true);
					field.set(bean, null);
				}
			}
		} catch (Exception ex) {
			LOGGER.log(Level.INFO, "Finalize injection on authentication bean " + bean + " failure: " + ex.getMessage());
		}

		AuthenticateBean authBean = bean.getClass().getAnnotation(AuthenticateBean.class);
		session.removeAttribute(getClassName(authBean, bean.getClass()));
		bean = null;
	}

	String checkAuthentication(String path) throws ServletException {

		if (authBeans.isEmpty() && !CONFIG.getContent().getSecureUrls().isEmpty()) {
			throw new ServletException("Not found authentication bean mapped in your system. Once your system has secure urls, please use @AuthenticateBean!");
		}

		boolean authenticated = true;
		AuthenticateBean authBean = null;

		HttpSession session = SmartContext.getSession();
		synchronized (session) {

			for (String name : authBeans.keySet()) {
				authBean = authBeans.get(name).getAnnotation(AuthenticateBean.class);
				Object bean = session.getAttribute(name);

				if (bean != null) {
					boolean foundField = false;

					for (Field field : getBeanFields(bean.getClass())) {
		
						if (field.isAnnotationPresent(AuthenticateField.class)) {
							try {
								foundField = true;
								field.setAccessible(true);
								if (field.get(bean) == null) {
									authenticated = false;
									break;
								}
							} catch (Exception ex) {
								throw new ServletException("Authentication field not accessible: " + ex.getMessage(), ex);
							}
						}
					}

					if (!foundField) {
						throw new ServletException("None authenticateField found in authenticateBean!");
					}
				}

				// We must have only one authentication bean mapped
				break;
			}
		}

		// Access secure url
		//  - User authenticated ===>>> ok redirect to path
		//  - User not authenticated ===>>> redirect to login
		if (CONFIG.getContent().containsSecureUrl(path)) {
			if (authenticated) {
				return path;
			} else {
				return SmartUtils.decodePath(authBean.loginPath());
			}
		}

		// Access non secure url
		//   - User authenticated
		//         - access login page or except page ===>>> redirect to home
	    //         - other cases ===>>> ok redirect to path
		//   - User not authenticated ===>>> ok redirect to path
		else {
			if (authenticated) {
				if (authBean != null && (path.equals(SmartUtils.decodePath(authBean.loginPath())) 
						|| CONFIG.getContent().containsNonSecureUrlOnly(path))) {
					return SmartUtils.decodePath(authBean.homePath());
				} else {
					return path;
				}
			} else {
				return path;
			}
		}
	}

	@SuppressWarnings("all")
	Integer checkAuthorization(String path) {
		if (CONFIG.getContent().containsSecureUrl(path)) {

			Collection<String> userAccess = getUserAuthorizationAccess();

			AuthenticateBean authBean = null;
			for (String name : authBeans.keySet()) {
				authBean = authBeans.get(name).getAnnotation(AuthenticateBean.class);

				// We must have only one authentication bean mapped
				break;
			}

			// Check mapped urls
			UrlPattern urlPattern = CONFIG.getContent().getUrlPattern(path);
			if (urlPattern != null && urlPattern.getAccess() != null) {

				for (String access : urlPattern.getAccess()) {
					if (userAccess.contains(access) || access.equals("*")) {
						return null; // It means, authorized user
					}
				}

				return HttpServletResponse.SC_FORBIDDEN;
			}
		}
		return null; // It means, authorized user
	}

	@SuppressWarnings("unchecked")
	Collection<String> getUserAuthorizationAccess() {
		HttpServletRequest request = SmartContext.getRequest();

		if (request.getAttribute(REQUEST_USER_ACCESS) == null) {

			Collection<String> userAccess = new HashSet<String>();

			HttpSession session = SmartContext.getSession();
			synchronized (session) {

				for (String name : authBeans.keySet()) {
					Object bean = session.getAttribute(name);

					if (bean != null) {
						for (Field field : getBeanFields(bean.getClass())) {

							if (field.isAnnotationPresent(AuthorizeAccess.class)) {
								try {
									field.setAccessible(true);
									Object object = field.get(bean);
									if (object != null) {
										userAccess.addAll((Collection<String>) object);
									}
								} catch (Exception ex) {
									LOGGER.log(Level.INFO, "Authorize access mapped on smart bean [" + bean + "] could not be cast to Collection<String>: " + ex.getMessage());
								}
								break;
							}
						}
					}

					// We must have only one authentication bean mapped
					break;
				}
			}
			request.setAttribute(REQUEST_USER_ACCESS, userAccess);
		}
		return (Collection<String>) request.getAttribute(REQUEST_USER_ACCESS);
	}

	boolean checkExecuteAuthorization(Object bean, String expression) {

		for (Method method : getBeanMethods(bean.getClass())) {

			ExecuteAccess execAccess = method.getAnnotation(ExecuteAccess.class);
			if (execAccess != null && execAccess.access().length > 0 && expression.contains(method.getName())) {

				Collection<String> userAccess = getUserAuthorizationAccess();
				if (!userAccess.isEmpty()) {
					
					for (String access : execAccess.access()) {
						if (userAccess.contains(access)) {
							return true;
						}
					}
					return false;
				}

				break;
			}
		}
		return true;
	}

    private void initAnnotatedBeans(ServletContext context) {

		smartBeans = new HashMap<String, Class<?>>();
		authBeans = new HashMap<String, Class<?>>();
		smartServlets = new HashMap<String, Class<?>>();
		smartFilters = new HashMap<String, Class<?>>();
		contextListeners = new HashSet<SmartContextListener>();
		sessionListeners = new HashSet<SmartSessionListener>();

		if (CONFIG.getContent().getPackageScan() == null) {
			LOGGER.log(Level.SEVERE, "None [package-scan] tag was found on jsmart5.xml file! Skipping package scanning.");
			return;
		}

		Object[] packages = CONFIG.getContent().getPackageScan().split(",");
		Reflections reflections = new Reflections(packages);

		Set<Class<?>> annotations = reflections.getTypesAnnotatedWith(SmartBean.class);
		for (Class<?> clazz : annotations) {
			SmartBean bean = clazz.getAnnotation(SmartBean.class);
			LOGGER.log(Level.INFO, "Mapping SmartBean class: " + clazz);

			if ((bean.scope() == ScopeType.PAGE_SCOPE || bean.scope() == ScopeType.SESSION_SCOPE)
					&& !Serializable.class.isAssignableFrom(clazz)) {
				throw new RuntimeException("Mapped SmartBean class [" + clazz + "] with scope [" + bean.scope() + "] must implement java.io.Serializable interface");
			}

			setBeanFields(clazz);
			setBeanMethods(clazz);
			smartBeans.put(getClassName(bean, clazz), clazz);
		}

		annotations = reflections.getTypesAnnotatedWith(AuthenticateBean.class);
		for (Class<?> clazz : annotations) {
			AuthenticateBean authBean = clazz.getAnnotation(AuthenticateBean.class);
			if (authBeans.isEmpty()) {
				LOGGER.log(Level.INFO, "Mapping AuthenticateBean class: " + clazz);

				if (!Serializable.class.isAssignableFrom(clazz)) {
					throw new RuntimeException("Mapped AuthenticateBean class [" + clazz + "] must implement java.io.Serializable interface");
				}

				setBeanFields(clazz);
				setBeanMethods(clazz);
				authBeans.put(getClassName(authBean, clazz), clazz);
				continue;
			} else {
				LOGGER.log(Level.SEVERE, "Only one AuthenticationBean must be declared! Skipping remained ones.");
			}
		}

		annotations = reflections.getTypesAnnotatedWith(com.jsmart5.framework.annotation.SmartServlet.class);
		for (Class<?> clazz : annotations) {
			com.jsmart5.framework.annotation.SmartServlet servlet = clazz.getAnnotation(com.jsmart5.framework.annotation.SmartServlet.class);
			LOGGER.log(Level.INFO, "Mapping SmartServlet class: " + clazz);
			setBeanFields(clazz);
			setBeanMethods(clazz);
			smartServlets.put(getClassName(servlet, clazz), clazz);
		}
		
		annotations = reflections.getTypesAnnotatedWith(SmartFilter.class);
		for (Class<?> clazz : annotations) {
			SmartFilter filter = clazz.getAnnotation(SmartFilter.class);
			LOGGER.log(Level.INFO, "Mapping SmartFilter class: " + clazz);
			setBeanFields(clazz);
			setBeanMethods(clazz);
			smartFilters.put(getClassName(filter, clazz), clazz);
		}

		annotations = reflections.getTypesAnnotatedWith(SmartListener.class);
		for (Class<?> clazz : annotations) {
			try {
				Object listenerObj = clazz.newInstance();
				if (SmartContextListener.class.isInstance(listenerObj)) {
					LOGGER.log(Level.INFO, "Mapping SmartListener class [" + clazz + "]");
					setBeanFields(clazz);
					setBeanMethods(clazz);
					contextListeners.add((SmartContextListener) listenerObj);

				} else if (SmartSessionListener.class.isInstance(listenerObj)) {
					LOGGER.log(Level.INFO, "Mapping SmartListener class [" + clazz + "]");
					setBeanFields(clazz);
					setBeanMethods(clazz);
					sessionListeners.add((SmartSessionListener) listenerObj);
				}
			} catch (Exception ex) {
				LOGGER.log(Level.INFO, "SmartListener class [" + clazz.getName() + "] could not be instantiated!");
			}
		}

		if (smartBeans.isEmpty()) {
			LOGGER.log(Level.INFO, "SmartBeans were not mapped!");
		}
		if (authBeans.isEmpty()) {
			LOGGER.log(Level.INFO, "AuthenticateBean was not mapped!");
		}
		if (smartServlets.isEmpty()) {
			LOGGER.log(Level.INFO, "SmartServlets were not mapped!");
		}
		if (smartFilters.isEmpty()) {
			LOGGER.log(Level.INFO, "SmartFilters were not mapped!");
		}
		if (contextListeners.isEmpty() && sessionListeners.isEmpty()) {
			LOGGER.log(Level.INFO, "SmartListeners were not mapped!");
		}
    }

	public String getForwardPath(String path) {
    	if (path != null) {
    		return forwardPaths.get(path);
    	}
    	return path;
    }

    private void initForwardPaths(ServletContext servletContext) {
    	forwardPaths = new HashMap<String, String>();
    	lookupInResourcePath(servletContext, SEPARATOR);
    	overrideForwardPaths();
    }

    private void overrideForwardPaths() {
    	for (UrlPattern urlPattern : CONFIG.getContent().getUrlPatterns()) {

    		if (urlPattern.getJsp() != null && !urlPattern.getJsp().trim().isEmpty()) {
    			String prevJsp = forwardPaths.put(urlPattern.getUrl(), urlPattern.getJsp());

    			if (prevJsp != null) {
    				LOGGER.log(Level.INFO, "Overriding path mapping [" + urlPattern.getUrl() + "] from [" + prevJsp + "] to [" + urlPattern.getJsp() + "]");
    			} else {
    				LOGGER.log(Level.INFO, "Mapping path  [" + urlPattern.getUrl() + "] to [" + urlPattern.getJsp() + "]");
    			}
    		}
    	}
    }

    private void lookupInResourcePath(ServletContext servletContext, String path) {
    	Set<String> resources = servletContext.getResourcePaths(path);
    	if (resources != null) {
	    	for (String res : resources) {
	    		if (res.endsWith(".jsp") || res.endsWith(".jspf") || res.endsWith(".html")) {
	    			String[] bars = res.split(SEPARATOR);
	    			if (res.endsWith(".jspf")) {
	    				forwardPaths.put(SEPARATOR + bars[bars.length -1], res);
	    			} else {
	    				forwardPaths.put(SEPARATOR + bars[bars.length -1].replace(".jsp", "").replace(".html", ""), res);
	    			}
	    		} else {
	    			lookupInResourcePath(servletContext, res);
	    		}
	    	}
    	}
    }

    private void checkWebXmlPath(ServletContext servletContext) {
    	try {
	    	URL webXml = servletContext.getResource("/WEB-INF/web.xml");
	    	if (webXml != null) {
	    		throw new RuntimeException("JSmart5 framework is not compatible with [/WEB-INF/web.xml] file. Please remove the web.xml and compile your project with [failOnMissingWebXml=false]");
	    	}
    	} catch (MalformedURLException ex) {
    		LOGGER.log(Level.WARNING, "/WEB-INF/web.xml malformed Url: " + ex.getMessage());
    	}
    }

    private void initJndiMapping() {
		try {
			String lookupName = CONFIG.getContent().getEjbLookup();
			initialContext = new InitialContext();

			// For glassfish implementation
			NamingEnumeration<Binding> bindList = initialContext.listBindings("");
			while (bindList.hasMore()) {
				Binding bind = bindList.next();
				if (bind != null && ("java:" + lookupName).equals(bind.getName()) && bind.getObject() instanceof Context) {
					lookupInContext((Context) bind.getObject(), "java:" + lookupName);
				}
			}

			// For Jboss implementation
			if (jndiMapping.isEmpty()) {
				lookupInContext((Context) initialContext.lookup("java:" + lookupName), "java:" + lookupName);
			}
		} catch (Exception ex) {
			LOGGER.log(Level.WARNING, "JNDI for EJB mapping could not be initialized: " + ex.getMessage());
		}
	}

	private void lookupInContext(Context context, String prefix) {
		try {
			prefix += "/";
			NamingEnumeration<Binding> bindList = context.listBindings("");
			while (bindList.hasMore()) {
				Binding bind = bindList.next();
				if (bind != null) {
					if (bind.getObject() instanceof Context) {
						lookupInContext((Context) bind.getObject(), prefix + bind.getName());
					}
					String[] binds = bind.getName().split("!");
					if (binds.length > 1) {
						try {
							jndiMapping.put(Class.forName(binds[1]), prefix + binds[0]);
						} catch (Throwable ex) {
							LOGGER.log(Level.WARNING, "Class could not be found for EJB mapping: " + ex.getMessage());
						}
					}
				}
			}
		} catch (Throwable ex) {
			LOGGER.log(Level.WARNING, "Bindings could not be found for EJB context: " + ex.getMessage());
		}
	}

    private Field[] getBeanFields(Class<?> clazz) {
    	if (!mappedBeanFields.containsKey(clazz)) {
    		mappedBeanFields.put(clazz, clazz.getDeclaredFields());
    	}
    	return mappedBeanFields.get(clazz);
    }
   
    private void setBeanFields(Class<?> clazz) {
    	if (!mappedBeanFields.containsKey(clazz)) {
    		mappedBeanFields.put(clazz, clazz.getDeclaredFields());
    	}
    }

    private Method[] getBeanMethods(Class<?> clazz) {
    	if (!mappedBeanMethods.containsKey(clazz)) {
    		mappedBeanMethods.put(clazz, clazz.getMethods());
    	}
    	return mappedBeanMethods.get(clazz);
    }

    private void setBeanMethods(Class<?> clazz) {
    	if (!mappedBeanMethods.containsKey(clazz)) {
    		mappedBeanMethods.put(clazz, clazz.getMethods());
    	}
    }

    private void initJspPageBeans(ServletContext context) {
    	for (UrlPattern urlPattern : CONFIG.getContent().getUrlPatterns()) {
    		JspPageBean jspPageBean = new JspPageBean();
    		readJspPageResource(context, urlPattern.getUrl(), jspPageBean);
    		jspPageBeans.put(urlPattern.getUrl(), jspPageBean);
    	}
    }

    private void readJspPageResource(ServletContext context, String path, JspPageBean jspPageBean) {
    	InputStream is = context.getResourceAsStream(getForwardPath(path));

		if (is != null) {
			Scanner fileScanner = new Scanner(is);
			Set<String> includes = new LinkedHashSet<String>();

			try {
				String match = null;
				while ((match = fileScanner.findWithinHorizon(HANDLER_EL_PATTERN, 0)) != null) {

					if (match.contains(URL_PARAM_TAG)) {
						jspPageBean.addUrlParam(match);

		            	// It gets an attribute name
		            	match = match.substring(match.indexOf(URL_PARAM_NAME_ATTR) + URL_PARAM_NAME_ATTR.length());

		            	for (String name : match.replace(START_EL, "").replace(END_EL, "").split(EL_SEPARATOR)) {
		            		if (smartBeans.containsKey(name)) {
		            			jspPageBean.addBeanName(name);
		            		}
		            	}
		            	continue;
		            }

					if (match.contains(INCLUDE_TAG)) {
						match = match.replace(INCLUDE_TAG, "").replace(INCLUDE_FILE_ATTR, "").replace("\"", "")
										.replace(START_JSP_TAG, "").replace(END_JSP_TAG, "").trim();
						includes.add(match.contains("/") ? match.substring(match.lastIndexOf("/")) : match);
						continue;
					}

	            	for (String name : match.replace(START_EL, "").replace(END_EL, "").split(EL_SEPARATOR)) {
	            		if (smartBeans.containsKey(name)) {
	            			jspPageBean.addBeanName(name);
	            		}
	            	}
				}
			} finally {
				fileScanner.close();
			}

			// Read include page resources
			for (String include : includes) {
				readJspPageResource(context, include, jspPageBean);
			}
		}
    }

    private class JspPageBean {

    	private Set<String> urlParams;

    	private Set<String> beanNames;

    	public JspPageBean() {
    		this.urlParams = new LinkedHashSet<String>();
    		this.beanNames = new LinkedHashSet<String>();
    	}

		public Set<String> getUrlParams() {
			return urlParams;
		}

		public void addUrlParam(String urlParam) {
			this.urlParams.add(urlParam);
		}

		public Set<String> getBeanNames() {
			return beanNames;
		}

		public void addBeanName(String beanName) {
			this.beanNames.add(beanName);
		}
    }

}

package com.conbon.atg.extensions.dynamo.admin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import atg.nucleus.Nucleus;
import atg.nucleus.ServiceAdminServlet;

/**
 * <p>Implementation of {@link ServiceAdminServlet} to allow calling of methods with
 * parameters.</p>
 * 
 * <p>Class can be extended to provide a custom admin servlet UI specific to the function
 * whilst providing a way to pass params back from this UI eg. a form.</p>
 * 
 * <p>Implementation of a form or ajax request to this servlet must provide specific parameters:
 * <ul>
 *   <li><b>invokeMethod</b> - the method name you want to invoke</li>
 *   <li><b>methodSig</b> - a delimited list of parameter types (see below)</li>
 * 	 <li/><b>fieldValue</b> - a delimited list of parameters</li>
 * </ul>
 * </p>
 * <p> Parameter types should be provided in integer form where:
 * <ul>
 * 	 <li><b>1</b> - {@link Integer}
 * 	 <li><b>2</b> - {@link Double}
 * 	 <li><b>3</b> - {@link Float}
 * 	 <li><b>4</b> - {@link Long}
 * 	 <li><b>5</b> - {@link String}
 * </ul>
 * A semi-colon ; deimiter may be used to implement a list of parameters - (eg. 2;1;4).
 * </p>
 * 
 * @author conal.mclaughlin
 *
 */
@SuppressWarnings("rawtypes")
public class ParameterisedServiceAdminServlet extends ServiceAdminServlet {

	public ParameterisedServiceAdminServlet(Object pService, Nucleus pNucleus) {
		super(pService, pNucleus);
	}

	private static final long serialVersionUID = -8407374716219944474L;

	private static final String TOKEN_DELIMITER = ";";
	private static final String STRING_CLASS = "java.lang.String";
	private static final String INT_CLASS = "java.lang.Integer";
	private static final String DOUBLE_CLASS = "java.lang.Double";
	private static final String FLOAT_CLASS = "java.lang.Float";
	private static final String LONG_CLASS = "java.lang.Long";

	@Override
	public void service(HttpServletRequest pRequest, HttpServletResponse pResponse)
			throws ServletException, IOException {
		if ((pRequest.getRequestURI() != null) && (!pRequest.getRequestURI().endsWith("/"))) {
			StringBuffer url = pRequest.getRequestURL();
			url.append('/');
			String queryString = pRequest.getQueryString();
			if ((queryString != null) && (queryString.length() > 0)) {
				url.append('?');
				url.append(queryString);
			}
			pResponse.sendRedirect(pResponse.encodeURL(url.toString()));
			return;
		}

		pResponse.setContentType("text/html");
		if (mCharEncoding != null) {
			pResponse.setCharacterEncoding(mCharEncoding);
		}

		pResponse.setHeader("Pragma", "no-cache");

		String value = pRequest.getParameter("cancelMethodInvocation");
		if (value != null) {
			printService(pRequest, pResponse);
			return;
		}

		value = pRequest.getParameter("propertyName");
		if (value != null) {
			printProperty(pRequest, pResponse, value);
			return;
		}

		value = pRequest.getParameter("eventSetName");
		if (value != null) {
			printEventSet(pRequest, pResponse, value);
			return;
		}

		value = pRequest.getParameter("invokeMethod");
		String methodSig = pRequest.getParameter("methodSig");
		String fieldValue = pRequest.getParameter("fieldValue");
		if (value != null && methodSig != null && fieldValue != null) {
			printMethodInvocation(pRequest, pResponse, value, methodSig, fieldValue);
			return;
		} else if (value != null) {
			printMethodInvocation(pRequest, pResponse, value, null, null);
			return;
		}

		value = pRequest.getParameter("shouldInvokeMethod");
		if (value != null) {
			printMethodInvocationVerification(pRequest, pResponse, value);
			return;
		}

		value = pRequest.getParameter("reloadComponent");
		if (value != null) {
			reloadComponent(pRequest, pResponse);
			return;
		}

		printService(pRequest, pResponse);
	}

	public void printMethodInvocation(HttpServletRequest pRequest, HttpServletResponse pResponse, String pMethodName,
			String methodSignature, String parameters) throws ServletException, IOException {
		ServletOutputStream out = getResponseStream(pResponse);
		out.println("<html><title>");
		printHeaderTitle(pRequest, pResponse, out);
		out.println("</title>");
		insertStyle(pRequest, pResponse, out);
		printBodyTag(pRequest, pResponse, out);

		printTitle(pRequest, pResponse, out);

		Object[] args = { pMethodName };
		out.println(getResourceString("serviceAdminServletMethodInvokedHdr", args));

		Object val = null;
		try {
			Object target = mService;
			if (mResolvedService != null)
				target = mResolvedService;
			
			Class<?>[] parameterTypes = methodSignature != null ? parseMethodSignature(methodSignature) : new Class<?>[] {};
			Method method = target.getClass().getMethod(pMethodName, parameterTypes);
			Object[] paramArray = parameters != null ? parseParams(parameters) : new Object[] {};
			val = method.invoke(target, paramArray);
		} catch (InvocationTargetException exc) {
			handleInvocationException(pRequest, pResponse, pMethodName, exc);
			return;
		} catch (Throwable exc) {
			handleInvocationException(pRequest, pResponse, pMethodName, exc);
			return;
		}

		reloadComponent(pRequest, pResponse);
	}

	/**
	 * Tokenize param based on semi-colon delimiter
	 * 
	 * @param methodSignature
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Class<?>[] parseMethodSignature(String methodSignature) throws ClassNotFoundException {
		List<Class<?>> typeList = new ArrayList<Class<?>>();
		StringTokenizer tokenizer = new StringTokenizer(methodSignature, TOKEN_DELIMITER);
		while (tokenizer.hasMoreTokens()) {
			TypeToken typeToken = TypeToken.valueOfCodeString(tokenizer.nextToken());
			Class<?> typeClass = null;
			switch (typeToken) {
			case INTEGER:
				typeClass = Class.forName(INT_CLASS);
				break;
			case DOUBLE:
				typeClass = Class.forName(DOUBLE_CLASS);
				break;
			case FLOAT:
				typeClass = Class.forName(FLOAT_CLASS);
				break;
			case LONG:
				typeClass = Class.forName(LONG_CLASS);
				break;
			case STRING:
				typeClass = Class.forName(STRING_CLASS);
				break;
			default:
				break;
			}
			typeList.add(typeClass);
		}
		Class<?>[] classArray = new Class<?>[typeList.size()];
		for (int i = 0; i < classArray.length; i++) {
			classArray[i] = typeList.get(i);
		}
		return classArray;
	}
	
	private Object[] parseParams(String param) {
		List<Object> paramList = new ArrayList<Object>();
		StringTokenizer tokenizer = new StringTokenizer(param, TOKEN_DELIMITER);
		while(tokenizer.hasMoreTokens()) {
			paramList.add(tokenizer.nextToken());
		}
		Object[] paramArray = new Object[paramList.size()];
		for(int i=0; i<paramArray.length; i++) {
			paramArray[i] = paramList.get(i);
		}
		
		return paramArray;
	}

	@Override
	public void reloadComponent(HttpServletRequest pRequest, HttpServletResponse pResponse)
			throws ServletException, IOException {

		if(pRequest.getRequestURL() != null) {
			String url = pRequest.getRequestURL().toString();
			pResponse.sendRedirect(url);
			return;
		}
	}
}

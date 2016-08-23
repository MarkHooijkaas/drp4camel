package org.kisst.drp4camel;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.xml.DefaultNamespaceContext;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.Iterator;

public class SoapActionLocator implements Processor {
	private final XPathExpression wsaAction;
	private final XPathExpression soapBodyName;
	private final XPathExpression soapBodyNamespace;

	public SoapActionLocator() {
		try {
			DefaultNamespaceContext ns=new DefaultNamespaceContext();
			ns.add("SOAP","http://schemas.xmlsoap/soap/envelope/");
			ns.add("wsa","http://www.w3.org/2005/08/addressing");
			final XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(ns);

			this.wsaAction = xpath.compile("/SOAP:Envelope/SOAP:Header/wsa:Action/text()");
			this.soapBodyName = xpath.compile("local-name(/SOAP:Envelope/SOAP:Body/*)");
			this.soapBodyNamespace = xpath.compile("namespace-uri(/SOAP:Envelope/SOAP:Body/*)");
		}
		catch (XPathExpressionException e) { throw new RuntimeException(e);}
	}

	@Handler
	public void process(Exchange exchange) {
		Message in=exchange.getIn();
		String action=in.getHeader("SOAPAction",String.class);
		if (action==null || action.trim().length()==0) {
			try {
				Document doc = in.getBody(Document.class);
				action = wsaAction.evaluate(doc);
				if (action==null || action.trim().length()==0) {
					String name = soapBodyName.evaluate(doc);
					String namespace = soapBodyNamespace.evaluate(doc);
					if (name == null || name.trim().length() == 0)
						throw new RuntimeException("Could not determine action");
					if (namespace.endsWith("/"))
						action = namespace + name;
					else
						action = namespace + "/" + name;
				}
			}
			catch (XPathExpressionException e) { throw new RuntimeException(e);}
		}
		int pos=action.lastIndexOf('/');
		String serviceName = action.substring(0, pos);
		String operationName = action.substring(pos+1);
		String serviceShortName = serviceName.substring(serviceName.lastIndexOf('/')+1);
		exchange.setProperty("soapAction", action);
		exchange.setProperty("soapServiceName", serviceName);
		exchange.setProperty("soapServiceShortName", serviceShortName);
		exchange.setProperty("soapOperationName", operationName);

	}
}

package com.emc.mongoose.storage.mock.impl.http.request;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface XmlShortcuts {

	static void appendElement(
		final Document xml, final Element parentElement, final String elementName,
		final String innerText
	) {
		final Element element = xml.createElement(elementName);
		element.appendChild(xml.createTextNode(innerText));
		parentElement.appendChild(element);
	}

	static void appendElement(
		final Document xml, final Element parentElement, final String elementName
	) {
		final Element element = xml.createElement(elementName);
		parentElement.appendChild(element);
	}

	static void appendElement(
		final Element parentElement, final Element element
	) {
		parentElement.appendChild(element);
	}
}

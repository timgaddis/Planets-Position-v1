package planets.position;

/*
 * Copyright (C) 2010 Tim Gaddis
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLDataHandler extends DefaultHandler {

	private boolean gmtOffsetTag = false;
	private boolean elevationTag = false;
	private boolean temperatureTag = false;
	private boolean pressureTag = false;
	private StringBuffer buff = null;
	private ParsedLocationData locationDataSet;

	public XMLDataHandler(ParsedLocationData pld) {
		locationDataSet = pld;
	}

	public ParsedLocationData getParsedData() {
		return this.locationDataSet;
	}

	@Override
	public void startDocument() throws SAXException {
		// this.locationDataSet = new ParsedLocationData();
	}

	@Override
	public void endDocument() throws SAXException {
		// Nothing to do
	}

	/**
	 * Gets be called on opening tags like: <tag> Can provide attribute(s), when
	 * xml was like: <tag attribute="attributeValue">
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		if (localName.equals("rawOffset")) {
			this.gmtOffsetTag = true;
			buff = new StringBuffer("");
		} else if (localName.equals("elevation")) {
			this.elevationTag = true;
			buff = new StringBuffer("");
		} else if (localName.equals("temperature")) {
			this.temperatureTag = true;
			buff = new StringBuffer("");
		} else if (localName.equals("seaLevelPressure")) {
			this.pressureTag = true;
			buff = new StringBuffer("");
		} else if (localName.equals("hectoPascAltimeter")) {
			this.pressureTag = true;
			buff = new StringBuffer("");
		} else if (localName.equals("status")) {
			// error message returned
			String attrValue = atts.getValue("value");
			int i = Integer.parseInt(attrValue);
			String attrMsg = atts.getValue("message");
			locationDataSet.setErr(attrMsg);
			locationDataSet.setErrCode(i);
		}
	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>
	 */
	@Override
	public void characters(char ch[], int start, int length) {

		if (gmtOffsetTag | elevationTag | temperatureTag | pressureTag) {
			buff.append(ch, start, length);
		}

		// if (this.gmtOffsetTag) {
		// String value = new String(ch, start, length);
		//
		// } else if (this.elevationTag) {
		// String value = new String(ch, start, length);
		//
		// } else if (this.temperatureTag) {
		// String value = new String(ch, start, length);
		//
		// } else if (this.pressureTag) {
		// String value = new String(ch, start, length);
		//
		// }
	}

	/**
	 * Gets be called on closing tags like: </tag>
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		if (localName.equals("rawOffset")) {
			String content = buff.toString();
			locationDataSet.setOffset(Double.parseDouble(content));
			this.gmtOffsetTag = false;
		} else if (localName.equals("elevation")) {
			String content = buff.toString();
			locationDataSet.setElevation(Double.parseDouble(content));
			this.elevationTag = false;
		} else if (localName.equals("temperature")) {
			String content = buff.toString();
			locationDataSet.setTemp(Double.parseDouble(content));
			this.temperatureTag = false;
		} else if (localName.equals("seaLevelPressure")) {
			String content = buff.toString();
			locationDataSet.setPressure(Double.parseDouble(content));
			this.pressureTag = false;
		} else if (localName.equals("hectoPascAltimeter")) {
			String content = buff.toString();
			locationDataSet.setPressure(Double.parseDouble(content));
			this.pressureTag = false;
		}
	}

}

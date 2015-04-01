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

package com.jsmart5.framework.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;


public enum Config {

	CONFIG();

	private static final Logger LOGGER = Logger.getLogger(Config.class.getPackage().getName());

	private ConfigContent content;

	private List<String> mappedUrls = new ArrayList<String>();

	public void init(ServletContext context) {
		try (InputStream is = context.getResourceAsStream(Constants.JSMART5_XML_PATH)) {
			JAXBContext jaxbContext = JAXBContext.newInstance(ConfigContent.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			content = (ConfigContent) unmarshaller.unmarshal(is);
		} catch (Exception ex) {
			LOGGER.log(Level.INFO, "Failure to parse jsmart5.xml: " + ex.getMessage());
		}
	}

	public ConfigContent getContent() {
		return content;
	}

	public void addMappedUrls(List<String> urls) {
		mappedUrls.addAll(urls);
	}

	public boolean containsMappedUrl(String url) {
		return mappedUrls.contains(url);
	}

}
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

package com.jsmart5.framework.tag;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;

import com.jsmart5.framework.manager.SmartTagHandler;
import static com.jsmart5.framework.tag.JSConstants.*;

public final class SelectTagHandler extends SmartTagHandler {

	private String value;

	private boolean ajax;

	private boolean multiple;

	private boolean disabled;

	private Integer tabIndex;

	private boolean async = false;

	private List<OptionTagHandler> options;

	public SelectTagHandler() {
		options = new ArrayList<OptionTagHandler>();
	}

	@Override
	public void validateTag() throws JspException {
		// DO NOTHING
	}

	@Override
	public void executeTag() throws JspException, IOException {

		// Just to call nested tags
		JspFragment body = getJspBody();
		if (body != null) {
			body.invoke(null);
		}
		
		StringBuilder builder = new StringBuilder(HtmlConstants.OPEN_SELECT_TAG);

		builder.append("id=\"" + id + "\" ");

		if (style != null) {
			builder.append("style=\"" + style + "\" ");
		}

		StringBuilder scriptBuilder = new StringBuilder();

		if (styleClass != null) {
			builder.append("class=\"" + styleClass + "\" ");

		} else {
			if (multiple) {
				builder.append(CssConstants.CSS_SELECT_MULTI);
			} else {
				builder.append(CssConstants.CSS_SELECT);
			}
		}

		appendScriptBuilder(scriptBuilder);

		if (disabled || isEditRowTagEnabled()) {
			builder.append("disabled=\"disabled\" ");
		}
		if (multiple) {
			builder.append("multiple=\"multiple\" ");
		}
		if (tabIndex != null) {
			builder.append("tabindex=\"" + tabIndex + "\" ");
		}

		String name = getTagName((multiple ? J_ARRAY : J_TAG), value);
		if (name != null) {
			builder.append("name=\"" + name + "\" ");
		}

		String command = ajaxCommand;

		if (ajax) {
			if (command != null) {
				if (command.startsWith(ON_CHANGE)) {
					if (!command.contains(JSMART_AJAX.toString())) {
						command = command.replace(ON_CHANGE, ON_CHANGE + JSMART_SELECT.format(async, id));
					}
				} else {
					command += ON_CHANGE + JSMART_SELECT.format(async, id) + "\" ";
				}
			} else {
				command = ON_CHANGE + JSMART_SELECT.format(async, id) + "\" ";
			}
		}

		if (command != null) {
			builder.append(command);
		}

		appendEventBuilder(builder);

		appendFormValidator(builder);
		
		appendRestBuilder(builder);

		builder.append(">");

		if (!options.isEmpty()) {
 			for (OptionTagHandler option : options) {
				StringWriter sw = new StringWriter();
				option.setName(value);
				option.setOutputWriter(sw);
				option.executeTag();
				builder.append(sw.toString()); 
			}
		}

		printOutput(builder.append(HtmlConstants.CLOSE_SELECT_TAG));
	}

	/*package*/ void addOption(OptionTagHandler option) {
		this.options.add(option);
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setAjax(boolean ajax) {
		this.ajax = ajax;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public void setTabIndex(Integer tabIndex) {
		this.tabIndex = tabIndex;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

}
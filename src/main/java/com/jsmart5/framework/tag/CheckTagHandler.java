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

import java.util.Collection;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspTag;

import com.jsmart5.framework.json.JsonAjax;
import com.jsmart5.framework.manager.SmartTagHandler;
import com.jsmart5.framework.manager.SmartValidateTagHandler;
import com.jsmart5.framework.tag.css3.Bootstrap;
import com.jsmart5.framework.tag.html.Input;
import com.jsmart5.framework.tag.html.Label;

import static com.jsmart5.framework.tag.js.JsConstants.*;

public final class CheckTagHandler extends SmartTagHandler {

	static final String CHECKBOX = "checkbox";

	static final String RADIO = "radio";

	private Object value;

	private String label;

	private String type;

	private String name;

	private boolean ajax;

	private boolean async;
	
	private boolean inline;

	@Override
	public boolean beforeTag() throws JspException, IOException {
		JspTag parent = getParent();

		if (parent instanceof RadioGroupTagHandler) {
			((RadioGroupTagHandler) parent).addCheck(this);
			return false;

		} else if (parent instanceof CheckGroupTagHandler) {
			((CheckGroupTagHandler) parent).addCheck(this);
			return false;
		}
		return true;
	}

	@Override
	public void validateTag() throws JspException {
		// DO NOTHING - type is internal
	}

	@Override
	public void executeTag() throws JspException, IOException {

		Label lb = new Label();
		lb.addAttribute("style", style);
		
		if (inline) {
			if (CHECKBOX.equals(type)) {
				lb.addAttribute("class", Bootstrap.CHECKBOX_INLINE);
			} else {
				lb.addAttribute("class", Bootstrap.RADION_INLINE);
			}
		}
		
		lb.addAttribute("class", styleClass);

		Input input = new Input();
		input.addAttribute("id", id)
			.addAttribute("type", type);
		
		if (CHECKBOX.equals(type)) {
			input.addAttribute("checkgroup", "checkgroup");
		} else if (RADIO.equals(type)) {
			input.addAttribute("radiogroup", "radiogroup");
		}

		String name = getTagName((type == null || type.equals(RADIO) ? J_TAG : J_ARRAY), 
				this.name != null ? this.name : (value != null ? value.toString() : null));
		if (name != null) {
			input.addAttribute("name", name);
		}

		appendFormValidator(input);
		appendRest(input);
		appendEvent(input);

		Object object = getTagValue(value);
		input.addAttribute("value", object)
			.addAttribute("checked", verifyCheck(object) ? "checked" : null);
		
		lb.addTag(input).addText((String) getTagValue(label));

		if (!ajaxTags.isEmpty()) {
			for (AjaxTagHandler ajax : ajaxTags) {
				appendScript(ajax.getFunction(id));
			}
		}

		if (ajax) {
			appendScript(getFunction());
		}

		printOutput(lb.getHtml());
	}

	private StringBuilder getFunction() {
		StringBuilder builder = new StringBuilder();
		builder.append("$('#").append(id).append("').bind('").append(EVENT_CLICK).append("', function(){");

		JsonAjax jsonAjax = new JsonAjax();
		jsonAjax.setId(id);
		jsonAjax.setAsync(async);
		jsonAjax.setMethod("post");

		builder.append(JSMART_CHECK.format(getJsonValue(jsonAjax)));

		builder.append("});");
		return builder;
	}

	@SuppressWarnings("rawtypes")
	private boolean verifyCheck(Object value) {
		// Get selected values
		Object values = getTagValue(name);

		if (values != null && value != null) {
			if (values instanceof Collection) {
				for (Object obj : (Collection) values) {
					if (obj != null && obj.toString().equals(value.toString())) {
						return true;
					}
				}
			} else {
				return values.equals(value);
			}
		}
		return false;
	}

	void setType(String type) {
		this.type = type;
	}

	void setName(String name) {
		this.name = name;
	}
	
	void setInline(boolean inline) {
		this.inline = inline;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	void setAjax(boolean ajax) {
		this.ajax = ajax;
	}

	void setAsync(boolean async) {
		this.async = async;
	}

	void setValidator(SmartValidateTagHandler validator) {
		this.validator = validator;
	}

}
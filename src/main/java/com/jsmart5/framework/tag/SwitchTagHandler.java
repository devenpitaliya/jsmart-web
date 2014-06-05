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

import javax.servlet.jsp.JspException;

import com.jsmart5.framework.manager.SmartTagHandler;

public final class SwitchTagHandler extends SmartTagHandler {

	private static final String SWITCH_INPUT = "_switch_input";

	private static final String SWITCH_BUTTON = "_switch_button";

	private static final String SWITCH_SPAN_ON = "_switch_span_on";

	private static final String SWITCH_SPAN_OFF = "_switch_span_off";

	private String value;

	private String labelOn;

	private String labelOff;

	private boolean ajax;

	private boolean disabled;

	private boolean async = true;

	@Override
	public void validateTag() throws JspException {
		// DO NOTHING
	}

	@Override
	public void executeTag() throws JspException, IOException {

		// Container to hold switch
		StringBuilder builder = new StringBuilder(HtmlConstants.OPEN_DIV_TAG);

		builder.append("id=\"" + id + "\" ");

		builder.append("switch=\"switch\" ");

		if (style != null) {
			builder.append("style=\"" + style + "\" ");
		}
		if (styleClass != null) {
			builder.append("class=\"" + styleClass + "\" ");
		} else {
			builder.append(CssConstants.CSS_SWITCH_CONTAINER);
		}

		appendEventBuilder(builder);

		builder.append(">");

		// Hidden input to send value to server
		builder.append(HtmlConstants.INPUT_TAG);

		builder.append("id=\"" + id + SWITCH_INPUT + "\" ");

		String name = getTagName(J_TAG, value);
		if (name != null) {
			builder.append("name=\"" + name + "\" ");
		}

		Boolean object = (Boolean) getTagValue(value);
		if (object != null) {
			builder.append("value=\"" + object + "\" ");
		}

		if (disabled || isEditRowTagEnabled()) {
			builder.append("disabled=\"disabled\" ");
		}

		builder.append("type=\"hidden\" ");

		appendFormValidator(builder);
		
		appendRestBuilder(builder);

		builder.append(" />");

		// Span tag to represent On state
		builder.append(HtmlConstants.OPEN_SPAN_TAG);

		builder.append("id=\"" + id + SWITCH_SPAN_ON + "\" ");

		if (disabled) {
			builder.append(CssConstants.CSS_SWITCH_SPAN_ON_DISABLED);
		} else {
			builder.append(CssConstants.CSS_SWITCH_SPAN_ON);
		}

		builder.append(">");

		builder.append(getTagValue(labelOn));

		builder.append(HtmlConstants.CLOSE_SPAN_TAG);

		// Span tag to represent Off state
		builder.append(HtmlConstants.OPEN_SPAN_TAG);

		builder.append("id=\"" + id + SWITCH_SPAN_OFF + "\" ");

		if (disabled) {
			builder.append(CssConstants.CSS_SWITCH_SPAN_OFF_DISABLED);
		} else {
			builder.append(CssConstants.CSS_SWITCH_SPAN_OFF);
		}

		builder.append(">");

		builder.append(getTagValue(labelOff));

		builder.append(HtmlConstants.CLOSE_SPAN_TAG);

		// Switch button
		builder.append(HtmlConstants.OPEN_DIV_TAG);

		builder.append("id=\"" + id + SWITCH_BUTTON + "\" ");

		if (disabled) {
			builder.append(CssConstants.CSS_SWITCH_BUTTON_DISABLED);
		} else {
			builder.append(CssConstants.CSS_SWITCH_BUTTON);

			builder.append(ON_CLICK + JSConstants.JSMART_SWITCH.format(id, async, ajax) + "\" ");

			appendEventBuilder(builder);
		}

		builder.append(">" + HtmlConstants.CLOSE_DIV_TAG);

		printOutput(builder.append(HtmlConstants.CLOSE_DIV_TAG));

		appendScriptBuilder(new StringBuilder(JSConstants.JSMART_SWITCH_RESET.format(id)));
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setLabelOn(String labelOn) {
		this.labelOn = labelOn;
	}

	public void setLabelOff(String labelOff) {
		this.labelOff = labelOff;
	}

	public void setAjax(boolean ajax) {
		this.ajax = ajax;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

}
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

import static com.jsmart5.framework.tag.HtmlConstants.*;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;

import com.jsmart5.framework.manager.SmartTagHandler;

public class ButtonGroupTagHandler extends SmartTagHandler {

	private boolean inline = true;

	@Override
	public void validateTag() throws JspException {
		// DO NOTHING
	}

	@Override
	public void executeTag() throws JspException, IOException {
		
		StringWriter sw = new StringWriter();
		JspFragment body = getJspBody();
		if (body != null) {
			body.invoke(sw);
		}

		StringBuilder builder = new StringBuilder(OPEN_DIV_TAG);

		builder.append("id=\"" + id + "\" ");

		if (inline) {
			appendClass(builder, CssConstants.CSS_BUTTON_GROUP_HORIZONTAL);
		} else {
			appendClass(builder, CssConstants.CSS_BUTTON_GROUP_VERTICAL);
		}

		if (style != null) {
			builder.append("style=\"" + style + "\" ");
		}

		builder.append(">");
		builder.append(sw);
		builder.append(CLOSE_DIV_TAG);

		printOutput(builder);
	}

	public void setInline(boolean inline) {
		this.inline = inline;
	}

}
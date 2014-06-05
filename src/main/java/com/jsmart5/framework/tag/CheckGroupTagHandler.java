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


public final class CheckGroupTagHandler extends SmartTagHandler {

	protected final List<GroupItemTagHandler> items;

	private String align;

	private String value;

	private boolean inline;

	private boolean ajax;

	private boolean async = false;

	public CheckGroupTagHandler() {
		items = new ArrayList<GroupItemTagHandler>();
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

		StringBuilder builder = new StringBuilder(HtmlConstants.OPEN_DIV_TAG);

		builder.append("id=\"" + id + "\" ");

	 	if (align != null) {
	 		builder.append("align=\"" + align + "\" ");
		}

		builder.append(CssConstants.CSS_CHECKGROUP + ">");

		builder.append(HtmlConstants.OPEN_TABLE_TAG + CssConstants.CSS_CHECKGROUP_TABLE + ">");

		if (!items.isEmpty()) {

 	 		String columnStyle = CssConstants.CSS_CHECKGROUP_TABLE_COLUMN;
 	 		builder.append(HtmlConstants.OPEN_TABLE_ROW_TAG + CssConstants.CSS_CHECKGROUP_TABLE_ROW + ">");

 	 		int index = 0;
 			for (GroupItemTagHandler item : items) {
 				
 				if (!inline && items.indexOf(item) != 0) {
 		 	 		builder.append(HtmlConstants.OPEN_TABLE_ROW_TAG + CssConstants.CSS_CHECKGROUP_TABLE_ROW + ">");
 				}

				StringWriter sw = new StringWriter();
				item.setItemId(id + "_" + index++);
				item.setStyle(style);
				item.setStyleClass(styleClass);
				item.setValidator(validator);
				item.setRest(rest);
				item.setName(value);
				item.setAjax(ajax);
				item.setAsync(async);
				item.setAjaxCommand(ajaxCommand);
				item.setType(GroupItemTagHandler.CHECKBOX);
				setEvents(item);
				item.setOutputWriter(sw);
				item.executeTag();

				builder.append(HtmlConstants.OPEN_TABLE_COLUMN_TAG + columnStyle + ">" + sw.toString() + HtmlConstants.CLOSE_TABLE_COLUMN_TAG);

				if (!inline) {
					builder.append(HtmlConstants.CLOSE_TABLE_ROW_TAG);
				}
			}

 			if (inline) {
 				builder.append(HtmlConstants.CLOSE_TABLE_ROW_TAG);
 			}
		}

		builder.append(HtmlConstants.CLOSE_TABLE_TAG);

		printOutput(builder.append(HtmlConstants.CLOSE_DIV_TAG));
	}

	/*package*/ void addItem(GroupItemTagHandler item) {
		this.items.add(item);
	}

	private void setEvents(GroupItemTagHandler item) {
		item.setOnClick(onClick);
		item.setOnDblClick(onDblClick);
		item.setOnMouseDown(onMouseDown);
		item.setOnMouseMove(onMouseMove);
		item.setOnMouseOver(onMouseOver);
		item.setOnMouseOut(onMouseOut);
		item.setOnMouseUp(onMouseUp);
		item.setOnKeyDown(onKeyDown);
		item.setOnKeyPress(onKeyPress);
		item.setOnKeyUp(onKeyUp);
		item.setOnBlur(onBlur);
		item.setOnChange(onChange);
		item.setOnFocus(onFocus);
		item.setOnSelect(onSelect);
	}

	public void setAlign(String align) {
		this.align = align;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setInline(boolean inline) {
		this.inline = inline;
	}

	public void setAjax(boolean ajax) {
		this.ajax = ajax;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

}
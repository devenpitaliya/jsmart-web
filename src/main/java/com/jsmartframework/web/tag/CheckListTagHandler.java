/*
 * JSmart Framework - Java Web Development Framework
 * Copyright (c) 2015, Jeferson Albino da Silva, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
*/

package com.jsmartframework.web.tag;

import com.jsmartframework.web.manager.TagHandler;
import com.jsmartframework.web.tag.html.Tag;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspTag;


public final class CheckListTagHandler extends TagHandler {

    private String values;

    @Override
    public void validateTag() throws JspException {
        // DO NOTHING
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean beforeTag() throws JspException, IOException {
        JspTag parent = getParent();
        Object object = getTagValue(values);

        if (object instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) object;

            for (Entry<Object, Object> entry : map.entrySet()) {

                CheckTagHandler check = new CheckTagHandler();
                check.setValue(entry.getKey());
                check.setLabel(entry.getValue() != null ? entry.getValue().toString() : null);

                if (parent instanceof RadioGroupTagHandler) {
                    ((RadioGroupTagHandler) parent).addCheck(check);

                } else if (parent instanceof CheckGroupTagHandler) {
                    ((CheckGroupTagHandler) parent).addCheck(check);
                }
            }
        }
        return false;
    }

    @Override
    public Tag executeTag() throws JspException, IOException {
        // DO NOTHING
        return null;
    }

    public void setValues(String values) {
        this.values = values;
    }

}

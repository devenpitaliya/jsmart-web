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

import com.jsmartframework.web.exception.InvalidAttributeException;
import com.jsmartframework.web.manager.TagHandler;
import com.jsmartframework.web.tag.html.Tag;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspTag;

public final class ArgTagHandler extends TagHandler {

    private String name;

    private Object value;

    private String bindTo;

    @Override
    public void validateTag() throws JspException {
        if (!(getParent() instanceof FunctionTagHandler) && value == null && StringUtils.isBlank(bindTo)) {
            throw InvalidAttributeException.fromConflict("arg", "value", "Attribute [value] must be specified");
        }
    }

    @Override
    public boolean beforeTag() throws JspException, IOException {
        char argName = 'a';
        TagHandler parent = (TagHandler) getParent();

        String nameVal = (String) getTagValue(name);
        if (StringUtils.isBlank(nameVal)) {
            nameVal = "_" + String.valueOf(argName + parent.getArgs().size());
        }

        if (parent instanceof FunctionTagHandler && value == null && StringUtils.isBlank(bindTo)) {
            FunctionTagHandler function = ((FunctionTagHandler) parent);
            function.addArg(nameVal, null);
            function.appendFunctionArg(nameVal);

        } else if (StringUtils.isNotBlank(bindTo)) {
            parent.addArg(nameVal, (String) getTagValue(bindTo));

        } else {
            parent.addArg(getTagValue(value), (String) getTagValue(bindTo));
        }
        return false;
    }

    @Override
    public Tag executeTag() throws JspException, IOException {
        // DO NOTHING
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setBindTo(String bindTo) {
        this.bindTo = bindTo;
    }
}

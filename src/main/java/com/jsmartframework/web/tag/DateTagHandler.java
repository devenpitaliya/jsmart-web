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

import static com.jsmartframework.web.tag.js.JsConstants.JSMART_DATE;

import com.jsmartframework.web.exception.InvalidAttributeException;
import com.jsmartframework.web.json.Date;
import com.jsmartframework.web.manager.TagHandler;
import com.jsmartframework.web.tag.css.Bootstrap;
import com.jsmartframework.web.tag.css.JSmart;
import com.jsmartframework.web.tag.html.Div;
import com.jsmartframework.web.tag.html.Input;
import com.jsmartframework.web.tag.html.Label;
import com.jsmartframework.web.tag.html.Set;
import com.jsmartframework.web.tag.html.Tag;
import com.jsmartframework.web.tag.type.Mode;
import com.jsmartframework.web.tag.type.Size;
import com.jsmartframework.web.tag.type.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.JspTag;

public final class DateTagHandler extends TagHandler {

    private String dateMode;

    private boolean hideIcon;

    private String defaultDate;

    private boolean showWeeks;

    private String locale;

    private String linkWith;

    private String size;

    private String value;

    private boolean readOnly;

    private boolean autoFocus;

    private Integer tabIndex;

    private String placeholder;

    private String label;

    private String leftAddOn;

    private String rightAddOn;

    private List<TagHandler> childAddOns;

    private FormatTagHandler format;

    private String mask;

    public DateTagHandler() {
        childAddOns = new ArrayList<TagHandler>(2);
    }

    @Override
    public void validateTag() throws JspException {
        if (size != null && !Size.validateSmallLarge(size)) {
            throw InvalidAttributeException.fromPossibleValues("date", "size", Size.getSmallLargeValues());
        }
        if (dateMode != null && !Mode.validate(dateMode)) {
            throw InvalidAttributeException.fromPossibleValues("date", "dateMode", Mode.getValues());
        }
    }

    @Override
    public Tag executeTag() throws JspException, IOException {

        // Just to call nested tags
        JspFragment body = getJspBody();
        if (body != null) {
            body.invoke(null);
        }

        setRandomId("date");

        JspTag parent = getParent();

        Div inputGroup = null;

        Div formGroup = new Div();
        formGroup.addAttribute("class", Bootstrap.FORM_GROUP)
            .addAttribute("class", JSmart.DATE_FORM_GROUP);

        String size = null;
        if (parent instanceof FormTagHandler) {
            size = ((FormTagHandler) parent).getSize();
        } else if (parent instanceof RestTagHandler) {
            size = ((RestTagHandler) parent).getSize();
        }
        if (Size.LARGE.equalsIgnoreCase(size)) {
            formGroup.addAttribute("class", Bootstrap.FORM_GROUP_LARGE);
        } else if (Size.SMALL.equalsIgnoreCase(size)) {
            formGroup.addAttribute("class", Bootstrap.FORM_GROUP_SMALL);
        }

        if (label != null) {
            Label labelTag = new Label();
            labelTag.addAttribute("for", id)
                    .addAttribute("class", Bootstrap.LABEL_CONTROL)
                    .addText(getTagValue(label));
            formGroup.addTag(labelTag);
        }

        if (leftAddOn != null || rightAddOn != null || !hideIcon) {
            inputGroup = new Div();

            // Need to pass the wrap id to avoid opening date when addOn button is clicked
            inputGroup.addAttribute("id", id + "-wrap")
                .addAttribute("class", Bootstrap.INPUT_GROUP)
                .addAttribute("class", Bootstrap.DATE);

            if (Size.SMALL.equalsIgnoreCase(size)) {
                inputGroup.addAttribute("class", Bootstrap.INPUT_GROUP_SMALL);
            } else if (Size.LARGE.equalsIgnoreCase(size)) {
                inputGroup.addAttribute("class", Bootstrap.INPUT_GROUP_LARGE);
            }

            if (formGroup != null) {
                formGroup.addTag(inputGroup);
            }
        }

        if (leftAddOn != null) {
            boolean foundAddOn = false;

            for (int i = 0; i < childAddOns.size(); i++) {
                if (leftAddOn.equalsIgnoreCase(childAddOns.get(i).getId())) {
                    inputGroup.addTag(childAddOns.get(i).executeTag());
                    foundAddOn = true;
                    break;
                }
            }
            if (!foundAddOn) {
                Div div = new Div();
                div.addAttribute("class", Bootstrap.INPUT_GROUP_ADDON)
                    .addText(getTagValue(leftAddOn));
                inputGroup.addTag(div);
            }
        }

        String name = getTagName(J_DATE, value);

        Input hidden = new Input();
        hidden.addAttribute("id", id + (inputGroup != null ? "-wrap-date" : "-date"))
            .addAttribute("name", name + (readOnly ? EL_PARAM_READ_ONLY : ""))
            .addAttribute("type", Type.HIDDEN.name().toLowerCase());

        Input input = new Input();
        input.addAttribute("type", Type.TEXT.name().toLowerCase())
             .addAttribute("date", "date")
             .addAttribute("class", Bootstrap.FORM_CONTROL)
             .addAttribute("tabindex", tabIndex)
             .addAttribute("readonly", readOnly ? readOnly : null)
             .addAttribute("disabled", isDisabled() ? "disabled" : null)
             .addAttribute("placeholder", getTagValue(placeholder))
             .addAttribute("datatype", Type.TEXT.name().toLowerCase())
             .addAttribute("autofocus", autoFocus ? autoFocus : null)
             .addAttribute("data-mask", mask);

        appendRefId(input, id);

        if (Size.SMALL.equalsIgnoreCase(size)) {
            input.addAttribute("class", Bootstrap.INPUT_SMALL);
        } else if (Size.LARGE.equalsIgnoreCase(size)) {
            input.addAttribute("class", Bootstrap.INPUT_LARGE);
        }

        // Add the style class at last
        if (inputGroup != null) {
            inputGroup.addAttribute("style", getTagValue(style))
                .addAttribute("class", getTagValue(styleClass));
        } else {
            input.addAttribute("style", getTagValue(style))
                .addAttribute("class", getTagValue(styleClass));
        }

        Object dateValue = getTagValue(value);
        if (dateValue == null) {
            dateValue = getTagValue(defaultDate);
        }
        input.addAttribute("value", dateValue);
        hidden.addAttribute("value", dateValue);

        appendValidator(input);
        appendRest(input, name);
        appendEvent(input);

        if (inputGroup != null) {
            inputGroup.addTag(input);
        } else if (formGroup != null) {
            formGroup.addTag(input);
        }

        if (!hideIcon) {
            Div div = new Div();
            div.addAttribute("class", Bootstrap.INPUT_GROUP_ADDON);

            IconTagHandler icon = new IconTagHandler();
            if (Mode.TIMEONLY.equalsIgnoreCase(dateMode)) {
                icon.setName("glyphicon-time");
            } else {
                icon.setName("glyphicon-calendar");
            }
            div.addTag(icon.executeTag());

            inputGroup.addTag(div);
        }

        if (rightAddOn != null) {
            boolean foundAddOn = false;

            for (int i = 0; i < childAddOns.size(); i++) {
                if (rightAddOn.equalsIgnoreCase(childAddOns.get(i).getId())) {
                    inputGroup.addTag(childAddOns.get(i).executeTag());
                    foundAddOn = true;
                    break;
                }
            }
            if (!foundAddOn) {
                Div div = new Div();
                div.addAttribute("class", Bootstrap.INPUT_GROUP_ADDON)
                    .addText(getTagValue(rightAddOn));
                inputGroup.addTag(div);
            }
        }

        appendAjax(id);
        appendBind(id);

        // Need to pass the wrap id to avoid opening date when addOn button is clicked
        appendDateScript(inputGroup != null ? id + "-wrap" : id);

        appendTooltip(formGroup);
        appendPopOver(formGroup);

        Set set = new Set();
        set.addTag(formGroup).addTag(hidden);
        return set;
    }

    private void appendDateScript(String id) {
        Date jsonDate = new Date();
        jsonDate.setId(id);
        jsonDate.setLinkDate(linkWith);
        jsonDate.setLocale(locale);
        jsonDate.setShowWeeks(showWeeks);

        if (format != null) {
            jsonDate.setFormat(format.getRegex());
        }
        if (dateMode != null) {
            if (Mode.TIMEONLY.equalsIgnoreCase(dateMode)) {
                jsonDate.setFormat("LT");
            } else {
                jsonDate.setViewMode(dateMode.toLowerCase());
            }
        }
        StringBuilder script = new StringBuilder(JSMART_DATE.format(getJsonValue(jsonDate)));
        appendDocScript(script);
    }

    void addChildAddOn(TagHandler childAddOn) {
        this.childAddOns.add(childAddOn);
    }

    void setFormat(FormatTagHandler format) {
        this.format = format;
    }

    public void setDateMode(String dateMode) {
        this.dateMode = dateMode;
    }

    public void setShowWeeks(boolean showWeeks) {
        this.showWeeks = showWeeks;
    }

    public void setHideIcon(boolean hideIcon) {
        this.hideIcon = hideIcon;
    }

    public void setDefaultDate(String defaultDate) {
        this.defaultDate = defaultDate;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setLinkWith(String linkWith) {
        this.linkWith = linkWith;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setAutoFocus(boolean autoFocus) {
        this.autoFocus = autoFocus;
    }

    public void setTabIndex(Integer tabIndex) {
        this.tabIndex = tabIndex;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setLeftAddOn(String leftAddOn) {
        this.leftAddOn = leftAddOn;
    }

    public void setRightAddOn(String rightAddOn) {
        this.rightAddOn = rightAddOn;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }
}

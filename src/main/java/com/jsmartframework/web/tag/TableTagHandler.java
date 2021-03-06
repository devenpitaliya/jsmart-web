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

import static com.jsmartframework.web.tag.js.JsConstants.JSMART_TABLE;
import static com.jsmartframework.web.tag.js.JsConstants.JSMART_TABLEHEADER;
import static com.jsmartframework.web.tag.js.JsConstants.JSMART_TABLESCROLL;

import com.jsmartframework.web.adapter.TableAdapter;
import com.jsmartframework.web.exception.InvalidAttributeException;
import com.jsmartframework.web.json.Ajax;
import com.jsmartframework.web.json.Param;
import com.jsmartframework.web.json.Scroll;
import com.jsmartframework.web.manager.TagHandler;
import com.jsmartframework.web.tag.css.Bootstrap;
import com.jsmartframework.web.tag.css.JSmart;
import com.jsmartframework.web.tag.html.Caption;
import com.jsmartframework.web.tag.html.Div;
import com.jsmartframework.web.tag.html.Input;
import com.jsmartframework.web.tag.html.TBody;
import com.jsmartframework.web.tag.html.THead;
import com.jsmartframework.web.tag.html.Table;
import com.jsmartframework.web.tag.html.Tag;
import com.jsmartframework.web.tag.html.Td;
import com.jsmartframework.web.tag.html.Th;
import com.jsmartframework.web.tag.html.Tr;
import com.jsmartframework.web.tag.type.Event;
import com.jsmartframework.web.tag.type.RowMode;
import com.jsmartframework.web.tag.type.Size;
import com.jsmartframework.web.tag.type.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspFragment;

public final class TableTagHandler extends TagHandler {

    private String var;

    private String values;

    private String selectValue;

    private String caption;

    private Integer scrollSize;

    private String scrollOffset;

    private String maxHeight;

    private String size;

    private boolean bordered;

    private boolean striped;

    private String rowMode;

    private String update;

    private String beforeSend;

    private String onError;

    private String onSuccess;

    private String onComplete;

    private String onFilter;

    private final List<ColumnTagHandler> columns;

    public TableTagHandler() {
        columns = new ArrayList<ColumnTagHandler>();
    }

    @Override
    public void validateTag() throws JspException {
        if (scrollSize != null && scrollSize <= 0) {
            throw InvalidAttributeException.fromConstraint("table", "scrollSize", "greater than zero");
        }
        if (scrollSize != null && maxHeight == null) {
            throw InvalidAttributeException.fromConflict("table", "maxHeight", "Attribute [maxHeight] must be specified");
        }
        if (size != null && !Size.validateSmallLarge(size)) {
            throw InvalidAttributeException.fromPossibleValues("table", "size", Size.getSmallLargeValues());
        }
        if (values != null && var == null) {
            throw InvalidAttributeException.fromConflict("table", "var", "Attribute [var] must be specified case [values] is specified");
        }
        if (rowMode != null && !RowMode.validate(rowMode)) {
            throw InvalidAttributeException.fromPossibleValues("table", "rowMode", RowMode.getValues());
        }
    }

    @Override
    protected boolean checkTagExecution() {
        return shallExecuteTag();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Tag executeTag() throws JspException, IOException {

        // Need to indicate that it is a table parent tag for deep inner tags
        // so the ajax and bind actions can be set by this class
        pushDelegateTagParent();

        // Just to call nested tags
        JspFragment body = getJspBody();
        if (body != null) {
            body.invoke(null);
        }

        HttpServletRequest request = getRequest();

        Div div = new Div();
        div.addAttribute("class", Bootstrap.TABLE_RESPONSIVE)
            .addAttribute("style", getTagValue(style))
            .addAttribute("class", getTagValue(styleClass));

        Table table = new Table();
        table.addAttribute("id", id)
            .addAttribute("class", Bootstrap.TABLE)
            .addAttribute("class", bordered ? Bootstrap.TABLE_BORDERED : null)
            .addAttribute("class", striped ? Bootstrap.TABLE_STRIPED : null)
            .addAttribute("class", selectValue != null ? Bootstrap.TABLE_HOVER : null)
            .addAttribute("class", scrollSize != null ? JSmart.TABLE_SCROLL : null)
            .addAttribute("row-mode", rowMode);

        if (Size.SMALL.equalsIgnoreCase(size)) {
            table.addAttribute("class", Bootstrap.TABLE_CONDENSED);
        }

        if (caption != null) {
            Caption captionTag = new Caption();
            captionTag.addText(getTagValue(caption));
            table.addTag(captionTag);
        }

        THead thead = new THead();
        thead.addAttribute("class", JSmart.TABLE_HEAD);

        TBody tbody = new TBody();
        tbody.addAttribute("scroll-size", scrollSize);

        if (maxHeight != null) {
            tbody.addAttribute("style", "height: " + maxHeight + ";")
                .addAttribute("style", "max-height: " + maxHeight + ";");
            table.addAttribute("style", "margin-bottom: " + maxHeight + ";");
        }

        if (loadTag != null) {
            Tr tr = new Tr();
            tr.addAttribute("style", "border: 1px solid transparent;")
                .addAttribute("role-load", "true");

            Td td = new Td();
            td.addAttribute("style", "display: none;")
                .addAttribute("style", "text-align: center;")
                .addAttribute("colspan", columns.size())
                .addTag(loadTag.executeTag());

            tr.addTag(td);
            tbody.addTag(tr);
        }

        if (emptyTag != null) {
            Tr tr = new Tr();
            tr.addAttribute("role-empty", "true")
                .addAttribute("style", "border: 1px solid transparent;");

            Td td = new Td();
            td.addAttribute("id", emptyTag.id)
                .addAttribute("style", "display: none; border: none; cursor: default;")
                .addAttribute("style", getTagValue(emptyTag.style))
                .addAttribute("class", getTagValue(emptyTag.styleClass))
                .addAttribute("colspan", columns.size())
                .addText(emptyTag.getContent());

            tr.addTag(td);
            tbody.addTag(tr);
        }

        appendColumnTemplate(tbody);

        // Get the scroll parameters case requested by scroll table
        Scroll scroll = null;
        boolean hasFilterOrSort = hasFilterOrSort();

        // It means that a scroll maybe happened
        String scrollParam = request.getParameter(getTagName(J_SCROLL, fakeTagName(id)));
        if (scrollParam != null) {
            scroll = GSON.fromJson(scrollParam, Scroll.class);

        } else {
            // It means that the select on table was performed
            scrollParam = request.getParameter(getTagName(J_SCROLL, selectValue));

            if (scrollParam != null) {
                scroll = GSON.fromJson(scrollParam, Scroll.class);
            }
        }

        Object object = getTableContent(getTagValue(values), scroll, hasFilterOrSort);

        if (object instanceof List<?>) {
            Iterator<Object> iterator = ((List<Object>) object).iterator();

            int scrollIndex = scroll != null && scroll.getIndex() != null ? scroll.getIndex() : 0;
            int selectIndex = scrollIndex;

            while (iterator.hasNext()) {
                Object obj = iterator.next();
                if (obj == null) {
                    continue;
                }
                request.setAttribute(var, obj);

                Tr tr = new Tr();
                tr.addAttribute("scroll-index", scrollIndex)
                    .addAttribute("table-index", selectIndex);

                if (selectValue != null) {
                    tr.addAttribute("style", "cursor: pointer;");
                }

                // Only needs to set the role-delegate case bind or ajax is included inside table,
                // otherwise the table row does not need an id
                appendRefId(tr, id, true);

                Object scrollOffsetVal = getTagValue(scrollOffset);
                if (scrollOffsetVal != null) {
                    tr.addAttribute("scroll-offset", scrollOffsetVal);
                }

                appendEvent(tr);

                for (ColumnTagHandler column : columns) {
                    tr.addTag(column.executeTag());
                }

                tbody.addTag(tr);
                selectIndex++;
                request.removeAttribute(var);
            }
        }

        // Must be outside the looping case the initial table has no items
        appendAjax(id);
        appendBind(id);

        // Call after reading the column inner tag headers
        thead.addTag(addHeaderColumns());

        // Needs to pop the iterator action so this class set the
        // ajax and bind actions carried via RefAction
        popDelegateTagParent();

        if (selectValue != null) {
            appendDocScript(getAjaxFunction());
        }
        if (scrollSize != null) {
            appendDocScript(getScrollFunction());
        }
        if (hasFilterOrSort) {
            appendDocScript(getHeaderFunction());
        }

        div.addTag(table);
        table.addTag(thead)
            .addTag(tbody);

        return div;
    }

    private void appendColumnTemplate(Tag tbody) throws JspException, IOException {
        Tr tr = new Tr();
        tr.addAttribute("style", "display: none;")
                .addAttribute("role-template", "0");
        appendRefId(tr, id, true);
        appendEvent(tr);

        for (ColumnTagHandler column : columns) {
            tr.addTag(column.executeTag());
        }
        tbody.addTag(tr);
    }

    private Tag addHeaderColumns() throws JspException, IOException {
        Tr tr = new Tr();

        for (ColumnTagHandler column : columns) {
            Th th = new Th();
            th.addAttribute("style", getTagValue(column.style))
                .addAttribute("class", getTagValue(column.styleClass));

            Object label = getTagValue(column.getLabel());

            if (column.getFilterBy() != null) {
                Div div = new Div();
                div.addAttribute("class", JSmart.TABLE_HEADER_FILTER_BY);

                Input input = new Input();
                input.addAttribute("class", Bootstrap.FORM_CONTROL)
                    .addAttribute("class", Bootstrap.INPUT_SMALL)
                    .addAttribute("placeholder", label)
                    .addAttribute("type", Type.TEXT.name().toLowerCase())
                    .addAttribute("datatype", Type.TEXT.name().toLowerCase())
                    .addAttribute("filter-by", column.getFilterBy())
                    .addAttribute("data-mask", column.getFilterMask());
                div.addTag(input);
                th.addTag(div);

            } else if (column.getSortBy() != null) {
                Div div = new Div();
                div.addAttribute("class", JSmart.TABLE_HEADER_SORT_BY).addText(label);
                th.addTag(div);

            } else {
                th.addText(label);
            }

            if (column.getSortBy() != null) {
                Div div = new Div();
                div.addAttribute("class", column.getFilterBy() != null ?
                        JSmart.TABLE_FILTER_SORT_BY : JSmart.TABLE_SORT_BY);

                IconTagHandler topIcon = new IconTagHandler();
                topIcon.setName("glyphicon-triangle-top");
                Tag topTag = topIcon.executeTag();
                topTag.addAttribute("sort-by", column.getSortBy())
                    .addAttribute("sort-order", "1");

                IconTagHandler bottomIcon = new IconTagHandler();
                bottomIcon.setName("glyphicon-triangle-bottom");
                Tag bottomTag = bottomIcon.executeTag();
                bottomTag.addAttribute("sort-by", column.getSortBy())
                    .addAttribute("sort-order", "-1");

                div.addTag(topTag);
                div.addTag(bottomTag);

                th.addTag(div);

                if (column.getFilterBy() == null) {
                    th.addAttribute("sortable", "true")
                        .addAttribute("style", "cursor: pointer;");
                }
            }
            tr.addTag(th);
        }
        return tr;
    }

    @SuppressWarnings("unchecked")
    private List<?> getTableContent(Object object, Scroll scroll, Boolean hasFilterOrSort) throws JspException {
        int index = scroll != null  && scroll.getIndex() != null ? scroll.getIndex() : 0;
        Object offset = scroll != null ? scroll.getOffset() : null;

        if (object instanceof TableAdapter) {
            if (scrollSize == null) {
                throw InvalidAttributeException.fromConflict("table", "scrollSize",
                        "Attribute [scrollSize] must be specified to use TableAdapter");
            }

            String sort = null;
            Integer order = 0;
            Map<String, String> filters = null;

            if (scroll != null) {
                sort = scroll.getSort();
                order = scroll.getOrder();
                filters = scroll.getFilters();
            }

            TableAdapter<Object> tableAdapter = (TableAdapter<Object>) object;
            return tableAdapter.load(index, offset, scrollSize, sort, order, filters);

        } else if (object instanceof List) {
            if (hasFilterOrSort) {
                throw InvalidAttributeException.fromConflict("table", "values",
                        "Attribute [values] with static List cannot be used along with columns " +
                        "using [filterBy] or [sortBy] attributes. Please use TableAdapter instead.");
            }

            List<Object> list = (List<Object>) object;
            Object[] array = list.toArray();

            List<Object> retList = new ArrayList<Object>();

            int size = list.size();
            if (scrollSize != null) {
                size = index + scrollSize >= list.size() ? list.size() : (int) (index + scrollSize);
            }

            for (int i = index; i < size; i++) {
                  retList.add(array[i]);
              }
              return retList;
        }
        return Collections.EMPTY_LIST;
    }

    private StringBuilder getAjaxFunction() {
        Ajax jsonAjax = new Ajax();

        jsonAjax.setId(id);
        jsonAjax.setMethod("post");
        jsonAjax.setTag("table");

        jsonAjax.addParam(new Param(getTagName(J_SEL, selectValue), getTagName(J_VALUES, values)));
        jsonAjax.addParam(new Param(getTagName(J_SEL_VAL, selectValue), ""));

        if (scrollSize != null) {
            jsonAjax.addParam(new Param(getTagName(J_SCROLL, selectValue), ""));
        }
        if (update != null) {
            jsonAjax.setUpdate((String) getTagValue(update.trim()));
        }
        if (beforeSend != null) {
            jsonAjax.setBefore((String) getTagValue(beforeSend.trim()));
        }
        if (onError != null) {
            jsonAjax.setError((String) getTagValue(onError.trim()));
        }
        if (onSuccess != null) {
            jsonAjax.setSuccess((String) getTagValue(onSuccess.trim()));
        }
        if (onComplete != null) {
            jsonAjax.setComplete((String) getTagValue(onComplete.trim()));
        }

        StringBuilder builder = new StringBuilder();
        builder.append(JSMART_TABLE.format(getJsonValue(jsonAjax)));
        return getDelegateFunction(id, ">tr", Event.CLICK.name(), builder);
    }

    private StringBuilder getScrollFunction() {
        Ajax jsonAjax = new Ajax();
        jsonAjax.setId(id);
        jsonAjax.setMethod("post");
        jsonAjax.setTag("tablescroll");

        jsonAjax.addParam(new Param(getTagName(J_SCROLL, fakeTagName(id)), ""));

        StringBuilder builder = new StringBuilder();
        builder.append(JSMART_TABLESCROLL.format(getJsonValue(jsonAjax)));
        return builder;
    }

    private StringBuilder getHeaderFunction() {
        Ajax jsonAjax = new Ajax();
        jsonAjax.setId(id);
        jsonAjax.setMethod("post");
        jsonAjax.setTag("tableheader");

        jsonAjax.addParam(new Param(getTagName(J_SCROLL, fakeTagName(id)), ""));

        if (onFilter != null) {
            jsonAjax.setFilter(onFilter);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(JSMART_TABLEHEADER.format(getJsonValue(jsonAjax)));
        return builder;
    }

    private boolean hasFilterOrSort() {
        boolean headerScript = false;

        for (ColumnTagHandler column : columns) {
            if (column.getFilterBy() != null || column.getSortBy() != null) {
                headerScript = true;
                break;
            }
        }
        return headerScript;
    }

    void addColumn(ColumnTagHandler column) {
        this.columns.add(column);
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setValues(String values) {
        this.values = values;
    }

    public void setSelectValue(String selectValue) {
        this.selectValue = selectValue;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setScrollSize(Integer scrollSize) {
        this.scrollSize = scrollSize;
    }

    public void setScrollOffset(String scrollOffset) {
        this.scrollOffset = scrollOffset;
    }

    public void setMaxHeight(String maxHeight) {
        this.maxHeight = maxHeight;
    }

    public void setRowMode(String rowMode) {
        this.rowMode = rowMode;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setBordered(boolean bordered) {
        this.bordered = bordered;
    }

    public void setStriped(boolean striped) {
        this.striped = striped;
    }

    public void setUpdate(String update) {
        this.update = update;
    }

    public void setBeforeSend(String beforeSend) {
        this.beforeSend = beforeSend;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }

    public void setOnSuccess(String onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setOnComplete(String onComplete) {
        this.onComplete = onComplete;
    }

    public void setOnFilter(String onFilter) {
        this.onFilter = onFilter;
    }
}

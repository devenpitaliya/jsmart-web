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

package com.jsmartframework.web.tag.type;

public enum Method {

    POST,
    GET,
    PUT,
    DELETE,
    PATCH,
    OPTIONS,
    HEAD;

    public static boolean validate(String method) {
        try {
            Method.valueOf(method.toUpperCase());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validatePostGet(String method) {
        return POST.equalsIgnoreCase(method) || GET.equalsIgnoreCase(method);
    }

    public static String[] getValues() {
        int index = 0;
        Method[] methods = values();
        String[] values = new String[methods.length];

        for (Method method : methods) {
            values[index++] = method.name().toLowerCase();
        }
        return values;
    }

    public static String[] getPostGetValues() {
        String[] values = new String[2];
        values[0] = POST.name().toLowerCase();
        values[1] = GET.name().toLowerCase();
        return values;
    }

    public boolean equalsIgnoreCase(String string) {
        return this.name().equalsIgnoreCase(string);
    }
}

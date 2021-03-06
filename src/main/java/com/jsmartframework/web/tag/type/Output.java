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

public enum Output {

    DIV,
    SPAN,
    LEGEND,
    STRONG,
    MARK,
    EM,
    SMALL,
    LABEL,
    OUTPUT,
    DEL,
    S,
    INS,
    U,
    P,
    H1,
    H2,
    H3,
    H4,
    H5,
    H6,
    NONE,
    SCRIPT;

    public static boolean validate(String output) {
        try {
            Output.valueOf(output.toUpperCase());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validateHeader(String header) {
        return H1.equalsIgnoreCase(header) || H2.equalsIgnoreCase(header) || H3.equalsIgnoreCase(header)
                || H4.equalsIgnoreCase(header) || H5.equalsIgnoreCase(header) || H6.equalsIgnoreCase(header);
    }

    public static String[] getValues() {
        int index = 0;
        Output[] outputs = values();
        String[] values = new String[outputs.length];

        for (Output output : outputs) {
            values[index++] = output.name().toLowerCase();
        }
        return values;
    }

    public static String[] getHeaderValues() {
        String[] values = new String[6];
        values[0] = H1.name().toLowerCase();
        values[1] = H2.name().toLowerCase();
        values[2] = H3.name().toLowerCase();
        values[3] = H4.name().toLowerCase();
        values[4] = H5.name().toLowerCase();
        values[5] = H6.name().toLowerCase();
        return values;
    }

    public boolean equalsIgnoreCase(String string) {
        return this.name().equalsIgnoreCase(string);
    }
}

/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.datamodel;

import static com.codenvy.analytics.datamodel.ValueDataUtil.treatAsLong;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class LongValueData extends NumericValueData {

    public static final LongValueData DEFAULT = new LongValueData(0);

    private long value;

    /** For serialization one. */
    public LongValueData() {
    }

    public LongValueData(long value) {
        this.value = value;
    }

    public static LongValueData valueOf(long value) {
        return new LongValueData(value);
    }

    @Override
    public String getAsString() {
        return Long.toString(value);
    }

    @Override
    public String getType() {
        return ValueDataTypes.LONG.toString();
    }

    @Override
    protected boolean doEquals(ValueData valueData) {
        return value == ((LongValueData)valueData).value;
    }

    @Override
    protected int doHashCode() {
        return (int)value;
    }

    @Override
    protected ValueData doAdd(ValueData valueData) {
        return new LongValueData(value + treatAsLong(valueData));
    }

    @Override
    protected ValueData doSubtract(ValueData valueData) {
        return new LongValueData(value - treatAsLong(valueData));
    }

    public long getAsLong() {
        return value;
    }

    public double getAsDouble() {
        return value;
    }
}

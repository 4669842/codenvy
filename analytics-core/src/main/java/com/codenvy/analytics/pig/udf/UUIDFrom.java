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
package com.codenvy.analytics.pig.udf;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import java.io.IOException;

/** @author Anatoliy Bazko */
public class UUIDFrom extends EvalFunc<String> {

    /** {@inheritDoc} */
    @Override
    public String exec(Tuple input) throws IOException {
        if (input.size() == 0 || (input.size() > 0 && input.get(0) == null)) {
            return java.util.UUID.randomUUID().toString();
        } else {
            return exec(input.get(0).toString());
        }
    }

    public static String exec(String input) {
        byte[] bytes = input.getBytes();
        return java.util.UUID.nameUUIDFromBytes(bytes).toString();
    }

    /** {@inheritDoc} */
    @Override
    public Schema outputSchema(Schema input) {
        return new Schema(new Schema.FieldSchema(getSchemaName(this.getClass().getName().toLowerCase(), input), DataType.CHARARRAY));
    }
}

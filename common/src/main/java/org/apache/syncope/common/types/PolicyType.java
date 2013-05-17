/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.types;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum PolicyType {

    /**
     * Account policy like:
     * password expire time, change password at first access, ...
     */
    ACCOUNT("Account Policy"),
    GLOBAL_ACCOUNT("Account Global Policy"),

    /**
     * Password policy regarding password syntax.
     */
    PASSWORD("Password Policy"),
    GLOBAL_PASSWORD("Password Global Policy"),

    /**
     * SYNC policy regarding account conflicts resolution.
     */
    SYNC("Synchronization Policy"),
    GLOBAL_SYNC("Synchronization Global Policy");

    private String description;

    PolicyType(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PolicyType fromString(final String value) {
        return PolicyType.valueOf(value.toUpperCase());
    }
}

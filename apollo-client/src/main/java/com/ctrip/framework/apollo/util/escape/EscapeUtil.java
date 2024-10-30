/*
 * Copyright 2022 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.util.escape;

/**
 * @author dyx1234
 */
public class EscapeUtil {

    private static final String SINGLE_UNDERSCORE = "_";
    private static final String DOUBLE_UNDERSCORE = "__";
    private static final String TRIPLE_UNDERSCORE = "___";

    // Escapes a single underscore in a namespace
    public static String escapeNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        }
        return namespace.replace(SINGLE_UNDERSCORE, DOUBLE_UNDERSCORE);
    }

    // Concatenate the cluster and the escaped namespace, using three underscores as delimiters
    public static String createConfigMapKey(String cluster, String namespace) {
        String escapedNamespace = escapeNamespace(namespace);
        return String.join(TRIPLE_UNDERSCORE, cluster, escapedNamespace);
    }

}

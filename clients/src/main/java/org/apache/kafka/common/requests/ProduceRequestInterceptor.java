/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.common.requests;

import org.apache.kafka.common.message.ProduceRequestData;

/**
 * Intercepts produce requests after parsing but before records are appended to the log.
 * Implementations can transform records and/or signal per-partition errors that will be
 * returned to the producer without writing those partitions.
 */
public interface ProduceRequestInterceptor {
    /**
     * Intercept a produce request before it is written to the log.
     *
     * @param data The parsed produce request data
     * @param version The API version of the request
     * @return An InterceptResult containing the (possibly transformed) data and any partition errors
     */
    InterceptResult intercept(ProduceRequestData data, short version);
}

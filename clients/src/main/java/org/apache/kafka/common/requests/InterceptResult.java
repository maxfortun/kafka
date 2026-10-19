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

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.protocol.Errors;

/**
 * Result of intercepting a produce request.
 * Contains the (possibly transformed) request data and any per-partition errors.
 * Partitions with errors will not be written to the log; instead, the error
 * will be returned directly to the producer in the response.
 */
public class InterceptResult {
    private final ProduceRequestData data;
    private final Map<TopicPartition, Errors> partitionErrors;

    public InterceptResult(ProduceRequestData data) {
        this(data, Collections.emptyMap());
    }

    public InterceptResult(ProduceRequestData data, Map<TopicPartition, Errors> partitionErrors) {
        this.data = data;
        this.partitionErrors = partitionErrors != null ? partitionErrors : Collections.emptyMap();
    }

    public ProduceRequestData data() {
        return data;
    }

    public Map<TopicPartition, Errors> partitionErrors() {
        return partitionErrors;
    }

    public boolean hasErrors() {
        return !partitionErrors.isEmpty();
    }
}

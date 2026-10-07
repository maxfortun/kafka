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

import org.apache.kafka.common.errors.InvalidConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProduceResponseListenerFactory {
    public static final Logger log = LoggerFactory.getLogger(ProduceResponseListenerFactory.class);

    public static final String PRODUCE_RESPONSE_LISTENER_PROPERTY = "org.apache.kafka.common.requests.ProduceResponseListener";
    public static final String PRODUCE_RESPONSE_LISTENER_ENV = "KAFKA_PRODUCE_RESPONSE_LISTENER";
    public static final String PRODUCE_RESPONSE_LISTENER_DEFAULT = "org.apache.kafka.common.requests.DefaultProduceResponseListener";

    private static String getProduceResponseListenerClassName() {
        String produceResponseListenerClassName = System.getProperty(PRODUCE_RESPONSE_LISTENER_PROPERTY);
        if (null != produceResponseListenerClassName) {
            log.info("ProduceResponseListener class {} from system property {}", produceResponseListenerClassName, PRODUCE_RESPONSE_LISTENER_PROPERTY);
            return produceResponseListenerClassName;
        }

        produceResponseListenerClassName = System.getenv(PRODUCE_RESPONSE_LISTENER_ENV);
        if (null != produceResponseListenerClassName) {
            log.info("ProduceResponseListener class {} from env {}", produceResponseListenerClassName, PRODUCE_RESPONSE_LISTENER_ENV);
            return produceResponseListenerClassName;
        }

        produceResponseListenerClassName = PRODUCE_RESPONSE_LISTENER_DEFAULT;
        log.debug("ProduceResponseListener class {} default {}", produceResponseListenerClassName, PRODUCE_RESPONSE_LISTENER_DEFAULT);
        return produceResponseListenerClassName;
    }

    public static ProduceResponseListener getProduceResponseListener() {
        try {
            String produceResponseListenerClassName = getProduceResponseListenerClassName();
            return (ProduceResponseListener) Class.forName(produceResponseListenerClassName).getConstructor().newInstance();
        } catch (Exception e) {
            String message = "Failed to initialize";
            log.error(message, e);
            throw new InvalidConfigurationException(message, e);
        }
    }
}

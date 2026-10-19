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

public class ProduceRequestInterceptorFactory {
    public static final Logger log = LoggerFactory.getLogger(ProduceRequestInterceptorFactory.class);

    public static final String PRODUCE_REQUEST_INTERCEPTOR_PROPERTY = "org.apache.kafka.common.requests.ProduceRequestInterceptor";
    public static final String PRODUCE_REQUEST_INTERCEPTOR_ENV = "KAFKA_PRODUCE_REQUEST_INTERCEPTOR";
    public static final String PRODUCE_REQUEST_INTERCEPTOR_DEFAULT = "org.apache.kafka.common.requests.DefaultProduceRequestInterceptor";

    private static String getProduceRequestInterceptorClassName() {
        String interceptorClassName = System.getProperty(PRODUCE_REQUEST_INTERCEPTOR_PROPERTY);
        if (null != interceptorClassName) {
            log.debug("ProduceRequestInterceptor class {} from system property {}", interceptorClassName, PRODUCE_REQUEST_INTERCEPTOR_PROPERTY);
            return interceptorClassName;
        }

        interceptorClassName = System.getenv(PRODUCE_REQUEST_INTERCEPTOR_ENV);
        if (null != interceptorClassName) {
            log.debug("ProduceRequestInterceptor class {} from env {}", interceptorClassName, PRODUCE_REQUEST_INTERCEPTOR_ENV);
            return interceptorClassName;
        }

        interceptorClassName = PRODUCE_REQUEST_INTERCEPTOR_DEFAULT;
        log.debug("ProduceRequestInterceptor class {} default {}", interceptorClassName, PRODUCE_REQUEST_INTERCEPTOR_DEFAULT);
        return interceptorClassName;
    }

    public static ProduceRequestInterceptor getProduceRequestInterceptor() {
        try {
            String interceptorClassName = getProduceRequestInterceptorClassName();
            return (ProduceRequestInterceptor) Class.forName(interceptorClassName).getConstructor().newInstance();
        } catch (Exception e) {
            String message = "Failed to initialize ProduceRequestInterceptor";
            log.error(message, e);
            throw new InvalidConfigurationException(message, e);
        }
    }
}

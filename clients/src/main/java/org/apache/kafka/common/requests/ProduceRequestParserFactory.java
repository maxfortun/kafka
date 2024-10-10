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

public class ProduceRequestParserFactory {
    public static final Logger log = LoggerFactory.getLogger(ProduceRequestParserFactory.class);

    public static final String PRODUCE_REQUEST_PARSER_PROPERTY = "org.apache.kafka.common.requests.ProduceRequestParser";
    public static final String PRODUCE_REQUEST_PARSER_ENV = "KAFKA_PRODUCE_REQUEST_PARSER";
    public static final String PRODUCE_REQUEST_PARSER_DEFAULT = "org.apache.kafka.common.requests.DefaultProduceRequestParser";

    private static String getProduceRequestParserClassName() {
        String produceRequestParserClassName = System.getProperty(PRODUCE_REQUEST_PARSER_PROPERTY);
        if (null != produceRequestParserClassName) {
            log.debug("ProduceRequestParser class {} from system property {}", produceRequestParserClassName, PRODUCE_REQUEST_PARSER_PROPERTY);
            return produceRequestParserClassName;
        }

        produceRequestParserClassName = System.getenv(PRODUCE_REQUEST_PARSER_ENV);
        if (null != produceRequestParserClassName) {
            log.debug("ProduceRequestParser class {} from env {}", produceRequestParserClassName, PRODUCE_REQUEST_PARSER_ENV);
            return produceRequestParserClassName;
        }

        produceRequestParserClassName = PRODUCE_REQUEST_PARSER_DEFAULT;
        log.debug("ProduceRequestParser class {} default {}", produceRequestParserClassName, PRODUCE_REQUEST_PARSER_DEFAULT);
        return produceRequestParserClassName;
    }

    public static ProduceRequestParser getProduceRequestParser() {
        try {
            String produceRequestParserClassName = getProduceRequestParserClassName();
            return (ProduceRequestParser) Class.forName(produceRequestParserClassName).getConstructor().newInstance();
        } catch (Exception e) {
            String message = "Failed to initialize";
            log.error(message, e);
            throw new InvalidConfigurationException(message, e);
        }
    }
}


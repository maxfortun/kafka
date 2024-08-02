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
package org.apache.kafka.common.requests.transform;

import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;

import java.nio.ByteBuffer;

import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.requests.ProduceRequest;
import org.apache.kafka.common.requests.ProduceRequestParser;
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.common.message.ProduceRequestData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformingProduceRequestParser implements ProduceRequestParser {
    public static final Logger log = LoggerFactory.getLogger(TransformingProduceRequestParser.class);

    private static Class[] transformerConstructorParameterTypes = new Class[] {ResourceBundle.class, String.class};

    private ResourceBundle resources = ResourceBundle.getBundle("TransformingProduceRequestParser");
    private Collection<ByteBufferTransformer> byteBufferTransformers = new ArrayList<>();
    private Collection<ProduceRequestDataTransformer> produceRequestDataTransformers = new ArrayList<>();

    public TransformingProduceRequestParser() {
        try {
            String[] byteBufferTransformerNames = resources.getString("byteBufferTransformers").split("[\\s,;]*");
            for (String byteBufferTransformerName : byteBufferTransformerNames) {
                byteBufferTransformers.add((ByteBufferTransformer) getTransformer("byteBufferTransformer." + byteBufferTransformerName));
            }

            String[] produceRequestDataTransformerNames = resources.getString("produceRequestDataTransformers").split("[\\s,;]*");
            for (String produceRequestDataTransformerName : produceRequestDataTransformerNames) {
                produceRequestDataTransformers.add((ProduceRequestDataTransformer) getTransformer("produceRequestDataTransformer." + produceRequestDataTransformerName));
            }
        } catch (Exception e) {
            String message = "Failed to initialize";
            log.error(message, e);
            throw new InvalidConfigurationException(message, e);
        }
    }

    private Object getTransformer(String transformerName) throws Exception {
        String transformerClassName = resources.getString(transformerName + ".class");

        Class<?> transformerClass = Class.forName(transformerClassName);
        Constructor<?> transformerConstructor = transformerClass.getConstructor(transformerConstructorParameterTypes);

        if (null != transformerConstructor) {
            return transformerConstructor.newInstance(new Object[] {resources, transformerName});
        }

        transformerConstructor = transformerClass.getConstructor();
        return transformerConstructor.newInstance();
    }

    public ProduceRequest parse(ByteBuffer byteBuffer, short version) {
        for (ByteBufferTransformer byteBufferTransformer : byteBufferTransformers) {
            byteBuffer = byteBufferTransformer.transform(byteBuffer, version);
        }

        ProduceRequestData produceRequestData = new ProduceRequestData(new ByteBufferAccessor(byteBuffer), version);
        for (ProduceRequestDataTransformer produceRequestDataTransformer : produceRequestDataTransformers) {
            produceRequestData = produceRequestDataTransformer.transform(produceRequestData, version);
        }

        return new ProduceRequest(produceRequestData, version);
    }
}

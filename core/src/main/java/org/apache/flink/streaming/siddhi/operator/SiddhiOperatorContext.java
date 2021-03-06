/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.siddhi.operator;

import io.siddhi.core.SiddhiManager;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.siddhi.exception.UndefinedStreamException;
import org.apache.flink.streaming.siddhi.schema.SiddhiStreamSchema;
import org.apache.flink.streaming.siddhi.schema.StreamSchema;
import org.apache.flink.streaming.siddhi.utils.SiddhiExecutionPlanner;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SiddhiCEP Operator Context Metadata including input/output stream (streamId, TypeInformation) as well execution plan query,
 * and execution environment context like TimeCharacteristic and ExecutionConfig.
 */
public class SiddhiOperatorContext implements Serializable {
    private ExecutionConfig executionConfig;
    private Map<String, SiddhiStreamSchema<?>> inputStreamSchemas;
    private final Map<String, Class<?>> siddhiExtensions;
    private Map<String, TypeInformation> outputStreamTypes = new HashMap<>();
    private TimeCharacteristic timeCharacteristic;
    private String name;
    private String uuid = UUID.randomUUID().toString();

    /**
     * UUID -- Execution Plan.
     */
    private final Map<String, String> executionPlanMap = new ConcurrentHashMap<>();

    public SiddhiOperatorContext() {
        inputStreamSchemas = new HashMap<>();
        siddhiExtensions = new HashMap<>();
    }

    /**
     * @param extensions siddhi extensions to register
     */
    public void setExtensions(Map<String, Class<?>> extensions) {
        Preconditions.checkNotNull(extensions,"extensions");
        siddhiExtensions.putAll(extensions);
    }

    /**
     * @return registered siddhi extensions
     */
    public Map<String, Class<?>> getExtensions() {
        return siddhiExtensions;
    }

    /**
     * @return Siddhi Stream Operator Name in format of "Siddhi: execution query ... (query length)"
     */
    public String getName() {
        if (this.name == null) {
            return "CEP: Unnamed (" + uuid + ")";
        } else {
            return "CEP: " + this.name + " (" + uuid + ")";
        }
    }

    /**
     * @return Source siddhi stream IDs
     */
    public List<String> getInputStreams() {
        Object[] keys = this.inputStreamSchemas.keySet().toArray();
        List<String> result = new ArrayList<>(keys.length);
        for (Object key : keys) {
            result.add((String) key);
        }
        return result;
    }

    /**
     * @return Siddhi CEP cql-like execution plan
     */
    public Map<String, String> getExecutionPlanMap() {
        return executionPlanMap;
    }

    /**
     * Stream definition + execution expression
     */
    public String getAllEnrichedExecutionPlan() {
        Preconditions.checkNotNull(executionPlanMap, "Execution plan is not set");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, SiddhiStreamSchema<?>> entry : inputStreamSchemas.entrySet()) {
            sb.append(entry.getValue().getStreamDefinitionExpression(entry.getKey()));
        }
        for (Map.Entry<String, String> entry : this.getExecutionPlanMap().entrySet()) {
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Stream definition + execution expression
     */
    public String getEnrichedExecutionPlan(String id) {
        Preconditions.checkNotNull(executionPlanMap, "Execution plan is not set");
        return SiddhiExecutionPlanner.of(inputStreamSchemas, this.getExecutionPlanMap().get(id)).getEnrichedExecutionPlan();
    }

    /**
     * @return Siddhi Stream Operator output type information
     */
    public TypeInformation getOutputStreamType(String outputStreamId) {
        return outputStreamTypes.get(outputStreamId);
    }

    public Map<String, TypeInformation> getOutputStreamTypes() {
        return outputStreamTypes;
    }

    /**
     * @param inputStreamId Siddhi streamId
     * @return StreamSchema for given siddhi streamId
     *
     * @throws UndefinedStreamException throws if stream is not defined
     */
    @SuppressWarnings("unchecked")
    public <IN> StreamSchema<IN> getInputStreamSchema(String inputStreamId) {
        Preconditions.checkNotNull(inputStreamId,"inputStreamId");

        if (!inputStreamSchemas.containsKey(inputStreamId)) {
            throw new UndefinedStreamException("Input stream: " + inputStreamId + " is not found");
        }
        return (StreamSchema<IN>) inputStreamSchemas.get(inputStreamId);
    }

    /**
     * @param outputStreamType Output stream TypeInformation
     */
    public void setOutputStreamType(String outputStreamId, TypeInformation outputStreamType) {
        Preconditions.checkNotNull(outputStreamId,"outputStreamId");
        Preconditions.checkNotNull(outputStreamType,"outputStreamType");
        this.outputStreamTypes.put(outputStreamId, outputStreamType);
    }

    /**
     * @return Returns execution environment TimeCharacteristic
     */
    public TimeCharacteristic getTimeCharacteristic() {
        return timeCharacteristic;
    }

    public void setTimeCharacteristic(TimeCharacteristic timeCharacteristic) {
        Preconditions.checkNotNull(timeCharacteristic,"timeCharacteristic");
        this.timeCharacteristic = timeCharacteristic;
    }

    /**
     * @param executionPlan Siddhi SQL-Like exeuction plan query
     */
    public String addExecutionPlan(String executionPlan) {
        Preconditions.checkNotNull(executionPlan,"executionPlan");
        String id = UUID.randomUUID().toString();
        addExecutionPlan(id, executionPlan);
        return id;
    }

    public void addExecutionPlan(String id, String executionPlan) {
        this.executionPlanMap.put(id, executionPlan);
    }

    /**
     * @return Returns input stream ID and  schema mapping
     */
    public Map<String, SiddhiStreamSchema<?>> getInputStreamSchemas() {
        return inputStreamSchemas;
    }

    /**
     * @param inputStreamSchemas input stream ID and  schema mapping
     */
    public void setInputStreamSchemas(Map<String, SiddhiStreamSchema<?>> inputStreamSchemas) {
        Preconditions.checkNotNull(inputStreamSchemas,"inputStreamSchemas");
        this.inputStreamSchemas = inputStreamSchemas;
    }

    public void setName(String name) {
        Preconditions.checkNotNull(name,"name");
        this.name = name;
    }

    /**
     * @return Created new SiddhiManager instance with registered siddhi extensions
     */
    public SiddhiManager createSiddhiManager() {
        SiddhiManager siddhiManager = new SiddhiManager();
        for (Map.Entry<String, Class<?>> entry : getExtensions().entrySet()) {
            siddhiManager.setExtension(entry.getKey(), entry.getValue());
        }
        return siddhiManager;
    }

    /**
     * @return StreamExecutionEnvironment ExecutionConfig
     */
    public ExecutionConfig getExecutionConfig() {
        return executionConfig;
    }

    /**
     * @param executionConfig StreamExecutionEnvironment ExecutionConfig
     */
    public void setExecutionConfig(ExecutionConfig executionConfig) {
        Preconditions.checkNotNull(executionConfig,"executionConfig");
        this.executionConfig = executionConfig;
    }

    public boolean removeExecutionPlan(String id) {
        return this.executionPlanMap.remove(id) != null;
    }

    public void updateExecutionPlan(String id, String executionPlan) {
        addExecutionPlan(id, executionPlan);
    }
}
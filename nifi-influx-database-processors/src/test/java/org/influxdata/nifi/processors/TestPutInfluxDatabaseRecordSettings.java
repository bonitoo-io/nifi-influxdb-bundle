/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.influxdata.nifi.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.influxdata.nifi.processors.WriteOptions.ComplexFieldBehaviour;
import org.influxdata.nifi.processors.WriteOptions.MissingItemsBehaviour;
import org.influxdata.nifi.processors.WriteOptions.NullValueBehaviour;

import avro.shaded.com.google.common.collect.Maps;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.serialization.record.RecordFieldType;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TestPutInfluxDatabaseRecordSettings extends AbstractTestPutInfluxDatabaseRecord {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void defaultSettingsValid() {

        testRunner.assertValid();
    }

    @Test
    public void defaultSettings() {

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        // GZIP
        Mockito.verify(influxDB, Mockito.times(1)).disableGzip();
        Mockito.verify(influxDB, Mockito.times(0)).enableGzip();

        // LogLevel
        Mockito.verify(influxDB, Mockito.times(1)).setLogLevel(InfluxDB.LogLevel.NONE);

        // Consistency level
        Mockito.verify(influxDB, Mockito.times(1)).setConsistency(InfluxDB.ConsistencyLevel.ONE);

        // Batch
        Mockito.verify(influxDB, Mockito.times(1)).disableBatch();
        Mockito.verify(influxDB, Mockito.times(0)).enableBatch();
        Mockito.verify(influxDB, Mockito.times(0)).enableBatch(Mockito.any());
    }

    @Test
    public void influxDBServiceNotDefined() {

        testRunner.removeProperty(PutInfluxDatabaseRecord.INFLUX_DB_SERVICE);

        testRunner.assertNotValid();
    }

    @Test
    public void databaseName() {

        recordReader.addSchemaField("nifi-field", RecordFieldType.STRING);
        recordReader.addRecord("nifi-field-value");

        testRunner.enqueue("");
        testRunner.run();

        Assert.assertEquals("nifi-database", pointCapture.getValue().getDatabase());
    }

    @Test
    public void databaseNameExpression() {

        testRunner.setProperty(PutInfluxDatabaseRecord.DB_NAME, "${databaseProperty}");

        Map<String, String> attributes = Maps.newHashMap();
        attributes.put("databaseProperty", "dynamic-database-name");

        recordReader.addSchemaField("nifi-field", RecordFieldType.STRING);
        recordReader.addRecord("nifi-field-value");

        testRunner.enqueue("", attributes);
        testRunner.run();

        Assert.assertEquals("dynamic-database-name", pointCapture.getValue().getDatabase());
    }

    @Test
    public void retentionPolicy() {

        testRunner.setProperty(PutInfluxDatabaseRecord.RETENTION_POLICY, "custom-retention");

        recordReader.addSchemaField("nifi-field", RecordFieldType.STRING);
        recordReader.addRecord("nifi-field-value");

        testRunner.enqueue("");
        testRunner.run();

        Assert.assertEquals("custom-retention", pointCapture.getValue().getRetentionPolicy());
    }

    @Test
    public void retentionPolicyNotDefined() {

        testRunner.setProperty(PutInfluxDatabaseRecord.RETENTION_POLICY, "${retentionNotDefinedProperty}");

        recordReader.addSchemaField("nifi-field", RecordFieldType.STRING);
        recordReader.addRecord("nifi-field-value");

        testRunner.enqueue("");
        testRunner.run();

        Assert.assertEquals(WriteOptions.DEFAULT_RETENTION_POLICY, pointCapture.getValue().getRetentionPolicy());
    }

    @Test
    public void enableGZIP() {

        testRunner.setProperty(PutInfluxDatabaseRecord.ENABLE_GZIP, "true");

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        Mockito.verify(influxDB, Mockito.times(0)).disableGzip();
        Mockito.verify(influxDB, Mockito.times(1)).enableGzip();
    }

    @Test
    public void logLevel() {

        testRunner.setProperty(PutInfluxDatabaseRecord.LOG_LEVEL, InfluxDB.LogLevel.FULL.toString());

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        Mockito.verify(influxDB, Mockito.times(1)).setLogLevel(Mockito.eq(InfluxDB.LogLevel.FULL));
    }

    @Test
    public void logLevelNotDefined() {

        testRunner.setProperty(PutInfluxDatabaseRecord.LOG_LEVEL, "");

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        Mockito.verify(influxDB, Mockito.times(1)).setLogLevel(Mockito.eq(InfluxDB.LogLevel.NONE));
    }

    @Test
    public void logLevelUnsupportedName() {

        testRunner.setProperty(PutInfluxDatabaseRecord.LOG_LEVEL, "wrong_name");

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        Mockito.verify(influxDB, Mockito.times(1)).setLogLevel(Mockito.eq(InfluxDB.LogLevel.NONE));
    }

    @Test
    public void consistencyLevel() {

        testRunner.setProperty(PutInfluxDatabaseRecord.CONSISTENCY_LEVEL, InfluxDB.ConsistencyLevel.QUORUM.toString());

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        Mockito.verify(influxDB, Mockito.times(1)).setConsistency(Mockito.eq(InfluxDB.ConsistencyLevel.QUORUM));
    }

    @Test
    public void consistencyLevelNotDefined() {

        testRunner.setProperty(PutInfluxDatabaseRecord.CONSISTENCY_LEVEL, "");

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        Mockito.verify(influxDB, Mockito.times(1)).setConsistency(Mockito.eq(InfluxDB.ConsistencyLevel.ONE));
    }

    @Test
    public void consistencyLevelUnsupportedName() {

        testRunner.setProperty(PutInfluxDatabaseRecord.CONSISTENCY_LEVEL, "wrong_name");

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        Mockito.verify(influxDB, Mockito.times(1)).setConsistency(Mockito.eq(InfluxDB.ConsistencyLevel.ONE));
    }

    @Test
    public void enableBatching() {

        testRunner.setProperty(PutInfluxDatabaseRecord.ENABLE_BATCHING, Boolean.TRUE.toString());

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        ArgumentCaptor<BatchOptions> captor = ArgumentCaptor.forClass(BatchOptions.class);

        Mockito.verify(influxDB, Mockito.times(0)).disableBatch();
        Mockito.verify(influxDB, Mockito.times(0)).enableBatch();
        Mockito.verify(influxDB, Mockito.times(1)).enableBatch(captor.capture());

        BatchOptions batchOptions = captor.getValue();

        // default Batch Options settings
        Assert.assertEquals(1000, batchOptions.getFlushDuration());
        Assert.assertEquals(1000, batchOptions.getActions());
        Assert.assertEquals(0, batchOptions.getJitterDuration());
        Assert.assertEquals(10000, batchOptions.getBufferLimit());
        Assert.assertEquals(InfluxDB.ConsistencyLevel.ONE, batchOptions.getConsistency());
    }

    @Test
    public void configureBatching() {

        testRunner.setProperty(PutInfluxDatabaseRecord.ENABLE_BATCHING, Boolean.TRUE.toString());
        testRunner.setProperty(PutInfluxDatabaseRecord.CONSISTENCY_LEVEL, InfluxDB.ConsistencyLevel.QUORUM.toString());
        testRunner.setProperty(PutInfluxDatabaseRecord.BATCH_FLUSH_DURATION, "10 sec");
        testRunner.setProperty(PutInfluxDatabaseRecord.BATCH_ACTIONS, "2000");
        testRunner.setProperty(PutInfluxDatabaseRecord.BATCH_JITTER_DURATION, "3 sec");
        testRunner.setProperty(PutInfluxDatabaseRecord.BATCH_BUFFER_LIMIT, "40000");

        InfluxDB influxDB = processor.getInfluxDB(testRunner.getProcessContext());

        ArgumentCaptor<BatchOptions> captor = ArgumentCaptor.forClass(BatchOptions.class);

        Mockito.verify(influxDB, Mockito.times(0)).disableBatch();
        Mockito.verify(influxDB, Mockito.times(0)).enableBatch();
        Mockito.verify(influxDB, Mockito.times(1)).enableBatch(captor.capture());

        BatchOptions batchOptions = captor.getValue();

        // Batch Options settings
        Assert.assertEquals(10000, batchOptions.getFlushDuration());
        Assert.assertEquals(2000, batchOptions.getActions());
        Assert.assertEquals(3000, batchOptions.getJitterDuration());
        Assert.assertEquals(40000, batchOptions.getBufferLimit());
        Assert.assertEquals(InfluxDB.ConsistencyLevel.QUORUM, batchOptions.getConsistency());
    }

    @Test
    public void writeOptions() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        // Write Options
        Assert.assertNotNull(writeOptions);

        // Database
        Assert.assertEquals("nifi-database", writeOptions.getDatabase());

        // Retention Policy
        Assert.assertEquals("autogen", writeOptions.getRetentionPolicy());

        // Timestamp field
        Assert.assertEquals("timestamp", writeOptions.getTimestamp());

        // Timestamp precision
        Assert.assertEquals(TimeUnit.NANOSECONDS, writeOptions.getPrecision());

        // Measurement
        Assert.assertEquals("nifi-measurement", writeOptions.getMeasurement());

        // Fields
        Assert.assertEquals(1, writeOptions.getFields().size());
        Assert.assertEquals("nifi-field", writeOptions.getFields().get(0));

        // Missing Fields
        Assert.assertEquals(MissingItemsBehaviour.IGNORE, writeOptions.getMissingFields());

        // Tags
        Assert.assertEquals(1, writeOptions.getTags().size());
        Assert.assertEquals("tags", writeOptions.getTags().get(0));

        // Missing Tags
        Assert.assertEquals(MissingItemsBehaviour.IGNORE, writeOptions.getMissingTags());

        // Complex fields behaviour
        Assert.assertEquals(ComplexFieldBehaviour.TEXT, writeOptions.getComplexFieldBehaviour());

        // Null Field Behavior
        Assert.assertEquals(NullValueBehaviour.IGNORE, writeOptions.getNullValueBehaviour());
    }

    @Test
    public void timestamp() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.TIMESTAMP_FIELD, "createdAt");

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals("createdAt", writeOptions.getTimestamp());
    }

    @Test
    public void timestampOverFlowFileAttributes() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        ProcessSession processSession = testRunner.getProcessSessionFactory().createSession();

        FlowFile flowFile = processSession.create();

        Map<String, String> props = new HashMap<>();
        props.put("createdProperty", "createdTimestamp");

        flowFile = processSession.putAllAttributes(flowFile, props);

        testRunner.setProperty(PutInfluxDatabaseRecord.TIMESTAMP_FIELD, "${createdProperty}");

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), flowFile);

        Assert.assertEquals("createdTimestamp", writeOptions.getTimestamp());
    }

    @Test
    public void timestampPrecision() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.TIMESTAMP_PRECISION, TimeUnit.MINUTES.name());

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(TimeUnit.MINUTES, writeOptions.getPrecision());
    }

    @Test
    public void measurement() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.MEASUREMENT, "another-measurement");

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals("another-measurement", writeOptions.getMeasurement());
    }

    @Test
    public void measurementEmpty() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.MEASUREMENT, "");

        expectedException.expect(new TypeOfExceptionMatcher<>(PutInfluxDatabaseRecord.IllegalConfigurationException.class));

        processor.writeOptions(testRunner.getProcessContext(), null);
    }

    @Test
    public void fields() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.FIELDS, "user-id, user-screen-name ");

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(2, writeOptions.getFields().size());
        Assert.assertEquals("user-id", writeOptions.getFields().get(0));
        Assert.assertEquals("user-screen-name", writeOptions.getFields().get(1));
    }

    @Test
    public void fieldsTrailingComma() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.FIELDS, "user-id, ");

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(1, writeOptions.getFields().size());
        Assert.assertEquals("user-id", writeOptions.getFields().get(0));
    }

    @Test
    public void fieldsEmpty() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.FIELDS, " ");

        expectedException.expect(new TypeOfExceptionMatcher<>(PutInfluxDatabaseRecord.IllegalConfigurationException.class));

        processor.writeOptions(testRunner.getProcessContext(), null);
    }

    @Test
    public void missingFields() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.MISSING_FIELD_BEHAVIOR, MissingItemsBehaviour.FAIL.name());

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(MissingItemsBehaviour.FAIL, writeOptions.getMissingFields());
    }

    @Test
    public void missingFieldsUnsupported() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.MISSING_FIELD_BEHAVIOR, "wrong_name");

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(MissingItemsBehaviour.IGNORE, writeOptions.getMissingFields());
    }

    @Test
    public void tags() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.TAGS, "lang,keyword");

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(2, writeOptions.getTags().size());
        Assert.assertEquals("lang", writeOptions.getTags().get(0));
        Assert.assertEquals("keyword", writeOptions.getTags().get(1));
    }

    @Test
    public void missingTags() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.MISSING_TAG_BEHAVIOR, MissingItemsBehaviour.FAIL.name());

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(MissingItemsBehaviour.FAIL, writeOptions.getMissingTags());
    }

    @Test
    public void complexFieldBehaviour() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.COMPLEX_FIELD_BEHAVIOR, ComplexFieldBehaviour.IGNORE.name());

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(ComplexFieldBehaviour.IGNORE, writeOptions.getComplexFieldBehaviour());
    }

    @Test
    public void complexFieldBehaviourUnsupported() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.COMPLEX_FIELD_BEHAVIOR, "wrong_name");

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(ComplexFieldBehaviour.TEXT, writeOptions.getComplexFieldBehaviour());
    }

    @Test
    public void nullValueBehavior() throws PutInfluxDatabaseRecord.IllegalConfigurationException {

        testRunner.setProperty(PutInfluxDatabaseRecord.NULL_VALUE_BEHAVIOR, NullValueBehaviour.FAIL.name());

        WriteOptions writeOptions = processor.writeOptions(testRunner.getProcessContext(), null);

        Assert.assertEquals(NullValueBehaviour.FAIL, writeOptions.getNullValueBehaviour());
    }
}

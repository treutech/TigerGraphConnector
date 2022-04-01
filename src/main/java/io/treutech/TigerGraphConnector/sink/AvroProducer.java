//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.treutech.TigerGraphConnector.sink;

import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

public class AvroProducer {
  private final KafkaProducer<String, Schema> producer;

  public AvroProducer() {
    Properties config = new Properties();
    final String registry = "http://broker1:8081";
    config.put(ProducerConfig.CLIENT_ID_CONFIG, "TigerGraphSink");
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
    config.put("schema.registry.url", registry);

    //todo
    final String schemaPath = "src/main/java/avro/bigavro.asvc";

    this.producer = new KafkaProducer<>(config);
  }

  public void send(final String topic, final Schema avroKeySchema, final Schema avroValueSchema) {
    for (int k = 0; k < 1000; k++) {
      new GenericData.Record(avroKeySchema);
      new GenericData.Record(avroValueSchema);
    }
    this.producer.flush();
    this.producer.close();
  }

  public static void main(String[] args) throws Exception {
    final String TOPIC = "tigergraph";
    final String keySchemaString = "{\"type\": \"record\",\"name\": \"key\",\"fields\":[{\"type\": \"string\",\"name\": \"key\"}]}}";
    final String valueSchemaString = "{\"v_id\": \"3\", \"number\": 314222, \"timestamp_\": \"2020-05-30 21:40:20\", \"name\": \"Bar_0\", \"v_type\": \"bar\"}";
    final Schema avroKeySchema = (new Schema.Parser()).parse(keySchemaString);
    final Schema avroValueSchema = (new Schema.Parser()).parse(valueSchemaString);
    final AvroProducer avroProducer = new AvroProducer();
    avroProducer.send(TOPIC, avroKeySchema, avroValueSchema);
    Thread.sleep(2000L);
  }
}

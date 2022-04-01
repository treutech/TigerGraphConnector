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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;

public class JsonProducer {
  private final KafkaProducer<String, JsonNode> producer;

  public JsonProducer() {
    final Properties config = new Properties();
    config.put(ProducerConfig.CLIENT_ID_CONFIG, "TigerGraphSink");
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonSerializer");
    this.producer = new KafkaProducer<>(config);
  }

  public void send(final String topic, final JsonNode msg) {
    final ProducerRecord<String, JsonNode> record =
        new ProducerRecord<>(topic, String.valueOf((new Random()).nextInt() % 1000000), msg);
    this.producer.send(record, (metadata, e) -> e.printStackTrace());
    this.producer.flush();
    this.producer.close();
  }

  public static void main(final String[] args) throws Exception {
    final String TOPIC = "tigergraph";
    final JsonProducer jsonProducer = new JsonProducer();
    final String jsonString =
        "{\"v_id\": \"3\", \"number\": 314222, \"timestamp_\": \"2020-05-30 21:40:20\", \"name\": \"Bar_0\", \"v_type\": \"bar\"}";
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode jsonObject = mapper.readTree(jsonString);
    jsonProducer.send(TOPIC, jsonObject);
    Thread.sleep(2000L);
  }
}

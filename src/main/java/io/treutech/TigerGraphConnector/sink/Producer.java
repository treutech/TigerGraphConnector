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
import org.apache.kafka.clients.producer.*;

public class Producer {
  private final KafkaProducer<String, String> producer;

  public Producer() {
    Properties config = new Properties();
    config.put(ProducerConfig.CLIENT_ID_CONFIG, "TigerGraphSink");
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(ProducerConfig.ACKS_CONFIG, "all");
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    this.producer = new KafkaProducer<>(config);
  }

  public void send(final String topic, final String msg) {
    ProducerRecord<String, String> record =
        new ProducerRecord<>(topic, String.valueOf((int) (Math.random() % 1000000.0)), msg);
    this.producer.send(record, (metadata, e) -> {});
    this.producer.flush();
    this.producer.close();
  }

  public static void main(final String[] args) throws Exception {
    final String TOPIC = "tigergraph";
    final Producer p = new Producer();
    p.send(TOPIC, "{\"v_id\": \"0\",\"v_type\": \"foo\",\"attributes\": {\"name\": \"Foo_0\",\"number\": 477176}}");
    Thread.sleep(2000L);
  }
}

package com.github.liyibo1110.openfeign.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import feign.codec.EncodeException;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 该类为 Spring Sort 对象提供了序列化和反序列化支持
 * @author liyibo
 * @date 2026-05-12 13:40
 */
public class SortJsonComponent {

    public static class SortSerializer extends JsonSerializer<Sort> {
        @Override
        public void serialize(Sort value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();
            value.iterator().forEachRemaining(v -> {
                try {
                    gen.writeObject(v);
                } catch (IOException e) {
                    throw new EncodeException("Couldn't serialize object " + v);
                }
            });
            gen.writeEndArray();
        }

        @Override
        public Class<Sort> handledType() {
            return Sort.class;
        }
    }

    public static class SortDeserializer extends JsonDeserializer<Sort> {

        @Override
        public Sort deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);
            if (treeNode.isArray()) {
                ArrayNode arrayNode = (ArrayNode) treeNode;
                return toSort(arrayNode);
            } else if (treeNode.get("orders") != null && treeNode.get("orders").isArray()) {
                ArrayNode arrayNode = (ArrayNode) treeNode.get("orders");
                return toSort(arrayNode);
            }
            return null;
        }

        @Override
        public Class<Sort> handledType() {
            return Sort.class;
        }

        private static Sort toSort(ArrayNode arrayNode) {
            List<Sort.Order> orders = new ArrayList<>();
            for (JsonNode jsonNode : arrayNode) {
                Sort.Order order;
                // there is no way to construct without null handling
                if ((jsonNode.has("ignoreCase") && jsonNode.get("ignoreCase").isBoolean())
                        && jsonNode.has("nullHandling") && jsonNode.get("nullHandling").isTextual()) {

                    boolean ignoreCase = jsonNode.get("ignoreCase").asBoolean();
                    String nullHandlingValue = jsonNode.get("nullHandling").textValue();

                    order = new Sort.Order(Sort.Direction.valueOf(jsonNode.get("direction").textValue()),
                            jsonNode.get("property").textValue(), ignoreCase,
                            Sort.NullHandling.valueOf(nullHandlingValue));
                } else {
                    // backward compatibility
                    order = new Sort.Order(Sort.Direction.valueOf(jsonNode.get("direction").textValue()), jsonNode.get("property").textValue());
                }
                orders.add(order);
            }
            return Sort.by(orders);
        }
    }
}

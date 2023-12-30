package loom.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.experimental.SuperBuilder;
import loom.common.collections.IteratorUtils;
import loom.common.exceptions.LookupError;
import loom.common.json.HasToJsonString;
import loom.common.json.JsonUtil;
import loom.common.json.MapValueListUtil;
import loom.validation.ListValidationIssueCollector;
import loom.validation.ValidationIssue;
import loom.validation.ValidationIssueCollector;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/** A Loom Graph document. */
@Getter
@Setter
@ToString(of = {"id", "nodes"})
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LoomGraph implements Iterable<LoomGraph.Node<?, ?>>, HasToJsonString {

  /**
   * Base class for a node in the graph.
   *
   * @param <NodeType> the node subclass.
   * @param <BodyType> the node body class.
   */
  @Getter
  @Setter
  @SuperBuilder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonSerialize(using = Node.Serialization.NodeSerializer.class)
  public abstract static class Node<NodeType extends Node<NodeType, BodyType>, BodyType>
      implements HasToJsonString {

    @SuppressWarnings("unused")
    public abstract static class NodeBuilder<
        NodeType extends Node<NodeType, BodyType>,
        BodyType,
        C extends Node<NodeType, BodyType>,
        B extends NodeBuilder<NodeType, BodyType, C, B>> {

      public NodeType buildOn(LoomGraph graph) {
        return graph.addNode(this);
      }
    }

    /**
     * Build a {@link ValidationIssue.Context} for this node.
     *
     * @param name the name of the context.
     * @return the context.
     */
    public ValidationIssue.Context asContext(String name) {
      return asContext(name, null);
    }

    /**
     * Build a {@link ValidationIssue.Context} for this node.
     *
     * @param name the name of the context.
     * @param message the message of the context.
     * @return the context.
     */
    public ValidationIssue.Context asContext(String name, @Nullable String message) {
      var builder = ValidationIssue.Context.builder().name(name).jsonpath(getJsonPath()).data(this);

      if (message != null) {
        builder.message(message);
      }

      return builder.build();
    }

    @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public static final class Serialization {
      /**
       * A Jackson serializer for Node. We use a custom serializer because {@code @Delegate} applied
       * to a method in subclasses to delegate the type methods of {@code body} does not honor
       * {@code @JsonIgnore}, and we otherwise generate data fields for every getter in the body.
       *
       * @param <B> the type of the node body.
       */
      public static final class NodeSerializer<N extends Node<N, B>, B>
          extends JsonSerializer<Node<N, B>> {
        @Override
        public void serialize(Node<N, B> value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
          gen.writeStartObject();
          gen.writeStringField("id", value.getId().toString());
          gen.writeStringField("type", value.getType());

          var label = value.getLabel();
          if (label != null) {
            gen.writeStringField("label", label);
          }

          gen.writeObjectField("body", value.getBody());
          gen.writeEndObject();
        }
      }
    }

    @Nonnull private final UUID id;
    @Nonnull private final String type;
    @JsonIgnore @Nullable private LoomGraph graph;
    @Nullable private String label;

    @JsonIgnore
    public final String getJsonPath() {
      return "$.nodes[@.id=='%s']".formatted(getId());
    }

    @Override
    public final String toString() {
      return "%s%s".formatted(getClass().getSimpleName(), toJsonString());
    }

    /**
     * Get the graph that this node belongs to.
     *
     * @return the graph.
     */
    public final LoomGraph assertGraph() {
      if (graph == null) {
        throw new IllegalStateException("Node does not belong to a graph: " + id);
      }
      return graph;
    }

    /**
     * Get the node body as a JSON string.
     *
     * @return the JSON string.
     */
    public final String getBodyAsJson() {
      return JsonUtil.toPrettyJson(getBody());
    }

    @Nonnull
    public abstract BodyType getBody();

    public abstract void setBody(@Nonnull BodyType body);

    /**
     * Get the node body as a JSON tree.
     *
     * @return the JSON tree.
     */
    public final JsonNode getBodyAsJsonNode() {
      return JsonUtil.valueToJsonNodeTree(getBody());
    }

    /**
     * Set the node body from a JSON string.
     *
     * @param json the JSON string.
     */
    public final void setBodyFromJson(String json) {
      setBody(JsonUtil.fromJson(json, getBodyClass()));
    }

    /** Get the class type of the node body. */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public final Class<BodyType> getBodyClass() {
      return getBodyClass(getClass());
    }

    /**
     * Get the class type of the node body.
     *
     * <p>Introspects the node type class parameters to get the body type class.
     *
     * @param nodeTypeClass the node type class.
     * @return the body class.
     * @param <N> the node type.
     * @param <B> the body type.
     */
    public static <N extends Node<N, B>, B> Class<B> getBodyClass(Class<N> nodeTypeClass) {
      var cls = (ParameterizedType) nodeTypeClass.getGenericSuperclass();
      @SuppressWarnings("unchecked")
      var bodyClass = (Class<B>) cls.getActualTypeArguments()[1];
      return bodyClass;
    }

    /**
     * Set the node body from a JSON tree.
     *
     * @param tree the JSON tree; either a {@code JsonNode} or a {@code Map<String, Object>}.
     */
    public final void setBodyFromValue(Object tree) {
      setBody(JsonUtil.convertValue(tree, getBodyClass()));
    }

    /**
     * Subclass type helper.
     *
     * @return this, cast to the subclass {@code NodeType} type.
     */
    @SuppressWarnings("unchecked")
    public final NodeType self() {
      return (NodeType) this;
    }
  }

  /** Support classes for Jackson serialization. */
  @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  public static final class Serialization {
    /** Jackson deserializer for {@link LoomGraph#nodes}. */
    public static final class NodeListToMapDeserializer
        extends MapValueListUtil.MapDeserializer<UUID, Node<?, ?>> {
      @SuppressWarnings("unchecked")
      public NodeListToMapDeserializer() {
        super((Class<Node<?, ?>>) (Class<?>) Node.class, Node::getId, HashMap::new);
      }
    }
  }

  @JsonIgnore @Nonnull private final LoomEnvironment env;

  @Nullable private UUID id;

  @JsonSerialize(using = MapValueListUtil.MapSerializer.class)
  @JsonDeserialize(using = Serialization.NodeListToMapDeserializer.class)
  private final Map<UUID, Node<?, ?>> nodes = new HashMap<>();

  /**
   * Validate the graph.
   *
   * <p>Validates the graph against the environment.
   *
   * @throws loom.validation.LoomValidationError if the graph is invalid.
   */
  public void validate() {
    var collector = new ListValidationIssueCollector();
    env.validateGraph(this, collector);
    collector.check();
  }

  public void validate(ValidationIssueCollector issueCollector) {
    env.validateGraph(this, issueCollector);
  }

  /**
   * Add a node to the graph.
   *
   * <p>If the node builder does not have an ID, a new ID will be generated.
   *
   * @param builder a builder for the node to add.
   * @return the ID of the node.
   */
  @Nonnull
  public <N extends Node<N, B>, B> N addNode(Node.NodeBuilder<N, B, ?, ?> builder) {
    if (builder.id == null) {
      builder.id = newUnusedNodeId();
    }

    @SuppressWarnings("unchecked")
    var node = (N) builder.build();

    return addNode(node);
  }

  /**
   * Create a new, unused node ID.
   *
   * @return the new ID.
   */
  public UUID newUnusedNodeId() {
    UUID id;
    do {
      id = UUID.randomUUID();
    } while (hasNode(id));
    return id;
  }

  /**
   * Add a node to the graph.
   *
   * @param node the node to add.
   * @return the added Node.
   */
  @Nonnull
  @SuppressWarnings("unchecked")
  public <T extends Node<?, ?>> T addNode(T node) {
    if (hasNode(node.getId())) {
      throw new IllegalArgumentException("Graph already has node with id: " + node.getId());
    }

    if (node.getGraph() != null) {
      throw new IllegalArgumentException("Node already belongs to a graph: " + node.getId());
    }
    node.setGraph(this);

    env.assertNodeTypeClass(node.getType(), (Class<T>) node.getClass());

    nodes.put(node.getId(), node);

    return node;
  }

  /**
   * Add a node to the graph.
   *
   * <p>If the node does not have an ID, a new ID will be added to the tree.
   *
   * @param jsonNode the json node tree to build from.
   * @return the added Node.
   */
  @Nonnull
  public <T extends Node<?, ?>> T addNode(@Nonnull JsonNode jsonNode) {
    ObjectNode obj = (ObjectNode) jsonNode;
    String type = obj.get("type").asText();
    var nodeClass = env.assertClassForType(type);
    if (obj.get("id") == null) {
      obj.put("id", newUnusedNodeId().toString());
    }
    @SuppressWarnings("unchecked")
    var node = (T) JsonUtil.convertValue(obj, nodeClass);
    return addNode(node);
  }

  /**
   * Does this graph contain a node with the given ID?
   *
   * @param id the ID to check.
   * @return true if the graph contains a node with the given ID.
   */
  public boolean hasNode(UUID id) {
    return nodes.containsKey(id);
  }

  @Nonnull
  public Node<?, ?> buildNode(@Nonnull String type, @Nonnull Object body) {
    return buildNode(type, null, body);
  }

  @Nonnull
  public Node<?, ?> buildNode(@Nonnull String type, @Nullable String label, @Nonnull Object body) {
    var nodeTree = JsonNodeFactory.instance.objectNode();
    nodeTree.put("type", type);
    if (label != null) {
      nodeTree.put("label", label);
    }
    if (body instanceof String) {
      nodeTree.set("body", JsonUtil.parseToJsonNodeTree((String) body));
    } else if (body instanceof ObjectNode bodyNode) {
      nodeTree.set("body", bodyNode);
    } else {
      nodeTree.set("body", JsonUtil.valueToJsonNodeTree(body));
    }
    return addNode(nodeTree);
  }

  /**
   * Does this graph contain a node with the given ID?
   *
   * @param id the ID to check.
   * @return true if the graph contains a node with the given ID.
   */
  public boolean hasNode(String id) {
    return hasNode(UUID.fromString(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node, or null if not found.
   */
  @Nullable public Node<?, ?> getNode(UUID id) {
    return nodes.get(id);
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node, or null if not found.
   */
  @Nullable public Node<?, ?> getNode(String id) {
    return getNode(UUID.fromString(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node.
   * @throws LookupError if the node does not exist.
   */
  @Nonnull
  public Node<?, ?> assertNode(String id) {
    return assertNode(UUID.fromString(id));
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @return the node.
   * @throws LookupError if the node does not exist.
   */
  @Nonnull
  public Node<?, ?> assertNode(UUID id) {
    var node = nodes.get(id);
    if (node == null) {
      throw new LookupError("Node not found: " + id);
    }
    return node;
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @param type the type of the node to get; null to skip type check.
   * @param nodeClass the class of the node to get.
   * @return the cast node.
   * @param <T> the type of the node to get.
   * @throws LookupError if the node does not exist, or is not of the given type.
   */
  @Nonnull
  public <T extends Node<?, ?>> T assertNode(String id, String type, Class<T> nodeClass) {
    return assertNode(UUID.fromString(id), type, nodeClass);
  }

  /**
   * Get the node with the given ID.
   *
   * @param id the ID of the node to get.
   * @param type the type of the node to get; null to skip type check.
   * @param nodeClass the class of the node to get.
   * @return the cast node.
   * @param <T> the type of the node to get.
   * @throws LookupError if the node does not exist, or is not of the given type.
   */
  @Nonnull
  public <T extends Node<?, ?>> T assertNode(UUID id, @Nullable String type, Class<T> nodeClass) {
    var node = assertNode(id);
    if (type != null && !node.getType().equals(type)) {
      throw new LookupError("Node is not of type " + type + ": " + id);
    }
    if (!nodeClass.isInstance(node)) {
      throw new LookupError("Node is not of type " + nodeClass.getSimpleName() + ": " + id);
    }
    return nodeClass.cast(node);
  }

  @Override
  @Nonnull
  public Iterator<Node<?, ?>> iterator() {
    return iterableNodes().iterator();
  }

  /**
   * Get an iterable of all nodes in the graph.
   *
   * @return the iterable.
   */
  @Nonnull
  public Iterable<Node<?, ?>> iterableNodes() {
    return nodes.values();
  }

  /**
   * Get an iterable of all nodes of class {@code T} the graph, optionally filtered by type.
   *
   * @param type the type to filter by; null to skip type filter.
   * @param nodeClass the class of the nodes to get.
   * @return the iterable.
   * @param <NodeType> the type of the nodes to get.
   */
  @Nonnull
  public <NodeType extends Node<?, ?>> Iterable<NodeType> iterableNodes(
      @Nullable String type, Class<NodeType> nodeClass) {
    return IteratorUtils.supplierToIterable(() -> stream(type, nodeClass).iterator());
  }

  /**
   * Get a stream of all nodes of class {@code T} the graph, optionally filtered by type.
   *
   * @param type the type to filter by; null to skip type filter.
   * @param nodeClass the class of the nodes to get.
   * @return the stream.
   * @param <NodeType> the type of the nodes to get.
   */
  @CheckReturnValue
  @Nonnull
  public <NodeType> Stream<NodeType> stream(@Nullable String type, Class<NodeType> nodeClass) {
    var s = stream();
    if (type != null) {
      s = s.filter(node -> node.getType().equals(type));
    }

    return s.filter(nodeClass::isInstance).map(nodeClass::cast);
  }

  /**
   * Get a stream of all nodes in the graph.
   *
   * @return the stream.
   */
  @CheckReturnValue
  @Nonnull
  public Stream<Node<?, ?>> stream() {
    return nodes.values().stream();
  }
}

package loom.graph;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.NoArgsConstructor;
import loom.graph.nodes.OperationSignatureNode;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TraversalUtils {
  /**
   * Find all simple cycles of Tensors and Operations in the graph.
   *
   * @param graph the graph to search
   * @return a list of cycles, where each cycle is a list of nodes in the cycle.
   */
  public static List<List<LoomNode<?, ?>>> findOperationSimpleCycles(LoomGraph graph) {
    Graph<LoomNode<?, ?>, DefaultEdge> linkGraph = buildOpeartionLinkGraph(graph);

    List<List<LoomNode<?, ?>>> simpleCycles = new ArrayList<>();
    new TarjanSimpleCycles<>(linkGraph).findSimpleCycles(simpleCycles::add);
    // Tarjan will place all non-cycle nodes in their own cycles, so filter those out.
    return simpleCycles.stream().filter(cycle -> cycle.size() > 1).toList();
  }

  /**
   * Build a JGraphT graph of the data flow of Operation and Tensor nodes in the graph.
   *
   * <p>This is a directed graph where the nodes are Operation and Tensor nodes, and the edges
   * represent data flow from Tensor inputs to Operation nodes; and from Operation nodes to Tensor
   * outputs.
   *
   * @param graph the graph to traverse.
   * @return a JGraphT graph of the data flow.
   */
  @Nonnull
  public static Graph<LoomNode<?, ?>, DefaultEdge> buildOpeartionLinkGraph(LoomGraph graph) {
    Graph<LoomNode<?, ?>, DefaultEdge> linkGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    for (var node :
        graph
            .nodeScan()
            .type(OperationSignatureNode.TYPE)
            .nodeClass(OperationSignatureNode.class)
            .asList()) {
      linkGraph.addVertex(node);

      for (var entry : node.getInputs().entrySet()) {
        for (var tensorSelection : entry.getValue()) {
          var refNode = graph.assertNode(tensorSelection.getTensorId());
          linkGraph.addVertex(refNode);
          linkGraph.addEdge(refNode, node);
        }
      }
      for (var entry : node.getOutputs().entrySet()) {
        for (var tensorSelection : entry.getValue()) {
          var refNode = graph.assertNode(tensorSelection.getTensorId());
          linkGraph.addVertex(refNode);
          linkGraph.addEdge(node, refNode);
        }
      }
    }
    return linkGraph;
  }
}

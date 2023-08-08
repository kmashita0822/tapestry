package loom.alt.xgraph;

import com.google.common.base.Joiner;
import lombok.Synchronized;
import loom.common.w3c.NodeListList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public final class XGraphUtils {
  private XGraphUtils() {}

  public static final String EG_SCHEMA_URI =
      "http://loom-project.org/schemas/v0.1/ExpressionGraph.core.xsd";
  public static final String EG_EXT_SCHEMA_URI =
      "http://loom-project.org/schemas/v0.1/ExpressionGraph.ext.xsd";

  public record SchemaResource(String prefix, String namespace, String resourcePath) {}

  public static final List<SchemaResource> SCHEMA_RESOURCES =
      List.of(
          new SchemaResource("loom", EG_SCHEMA_URI, "loom/alt/xgraph/ExpressionGraph.core.xsd"),
          new SchemaResource("ext", EG_EXT_SCHEMA_URI, "loom/alt/xgraph/ExpressionGraph.ext.xsd"));

  public static final String XFORM_RESOURCE = "loom/alt/xgraph/Validate.core.xsl";

  public static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

  static {
    dbFactory.setNamespaceAware(true);
    dbFactory.setIgnoringComments(true);
    dbFactory.setIgnoringElementContentWhitespace(true);
  }

  public static final DocumentBuilder documentBuilder;

  static {
    try {
      documentBuilder = dbFactory.newDocumentBuilder();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Document parse(InputStream is) {
    try {
      return documentBuilder.parse(is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static final SchemaFactory schemaFactory =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
  // public static final SchemaFactory schemaFactory =
  //     SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");

  // static {
  //   try {
  //     schemaFactory.setProperty("http://saxon.sf.net/feature/xsd-version", "1.1");
  //   } catch (Exception e) {
  //     throw new RuntimeException(e);
  //   }
  // }

  public static final XPathFactory xpathFactory = XPathFactory.newInstance();
  public static final XPath xpath = xpathFactory.newXPath();

  static {
    xpath.setNamespaceContext(
        new NamespaceContext() {
          @Override
          public String getNamespaceURI(String prefix) {
            if (prefix == null) {
              throw new NullPointerException("Null prefix");
            } else if ("xml".equals(prefix)) {
              return XMLConstants.XML_NS_URI;
            } else if ("xs".equals(prefix)) {
              return XMLConstants.W3C_XML_SCHEMA_NS_URI;
            } else if ("xsi".equals(prefix)) {
              return XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
            } else {
              for (var sr : SCHEMA_RESOURCES) {
                if (sr.prefix().equals(prefix)) {
                  return sr.namespace();
                }
              }
            }
            return XMLConstants.NULL_NS_URI;
          }

          @Override
          public String getPrefix(String uri) {
            // Not needed in this context
            return null;
          }

          @Override
          public Iterator<String> getPrefixes(String uri) {
            // Not needed in this context
            return null;
          }
        });
  }

  public static InputStream resourceAsStream(String path) {
    return XGraphUtils.class.getClassLoader().getResourceAsStream(path);
  }

  public static Node parseNestedSchema(InputStream is) {
    var d = parse(is);
    return d.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "schema").item(0);
  }

  public static Source parseNestedSchemaSource(InputStream is) {
    return new DOMSource(parseNestedSchema(is));
  }

  public static Schema getLoomSchema(Document doc) {
    return getLoomSchema(documentNamespaceMap(doc).values());
  }

  public static Node loadSchemaNode(String namespace) {
    for (var r : SCHEMA_RESOURCES) {
      if (r.namespace().equals(namespace)) {
        return parseNestedSchema(resourceAsStream(r.resourcePath()));
      }
    }
    throw new RuntimeException("No schema resource found for namespace: " + namespace);
  }

  public static List<Node> loadSchemaNodes(Document doc) {
    return loadSchemaNodes(documentNamespaceMap(doc).values());
  }

  public static List<Node> loadSchemaNodes(Collection<String> ns) {
    return ns.stream().map(XGraphUtils::loadSchemaNode).collect(Collectors.toList());
  }

  private static final Map<String, Schema> SCHEMA_CACHE = new HashMap<>();

  @Synchronized
  public static Schema getLoomSchema(Collection<String> ns) {
    ns = ns.stream().sorted().toList();
    var key = String.join(";", ns);
    if (SCHEMA_CACHE.containsKey(key)) {
      return SCHEMA_CACHE.get(key);
    }

    try {
      var schema = schemaFactory.newSchema(
          loadSchemaNodes(ns).stream().map(DOMSource::new).toArray(Source[]::new));
      SCHEMA_CACHE.put(key, schema);
      return schema;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static final TransformerFactory factory = TransformerFactory.newInstance();

  private static final Document XSLT_DOC = parse(resourceAsStream(XFORM_RESOURCE));

  private static final Transformer XSLT_TRANSFORMER;
  static {
    try {
      XSLT_TRANSFORMER =
              factory.newTransformer(new DOMSource(XSLT_DOC));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Synchronized
  public static void xsltCheck(Document loomDoc) {
    List<String> errors = new ArrayList<>();
    XSLT_TRANSFORMER.setErrorListener(
            new javax.xml.transform.ErrorListener() {

              @Override
              public void warning(TransformerException e) throws TransformerException {
                errors.add(e.getMessageAndLocation());
                throw e;
              }

              @Override
              public void error(TransformerException e) throws TransformerException {
                errors.add(e.getMessageAndLocation());
                throw e;
              }

              @Override
              public void fatalError(TransformerException e) throws TransformerException {
                errors.add(e.getMessageAndLocation());
                throw e;
              }
            }
    );


    var result = new DOMResult();
    try {
      XSLT_TRANSFORMER.transform(new DOMSource(loomDoc), result);
    } catch (TransformerException e) {
      throw new IllegalStateException(Joiner.on("\n").join(errors));
    }
  }

  public static Map<String, String> documentNamespaceMap(Document doc) {
    return NodeListList.of(doc.getDocumentElement().getAttributes()).stream()
        .filter(n -> n.getNodeName().startsWith("xmlns:"))
        .collect(Collectors.toMap(Node::getLocalName, Node::getNodeValue));
  }

  public static void validateLoomGraph(Document doc) {
    try {
      var val = XGraphUtils.getLoomSchema(doc).newValidator();
      val.validate(new DOMSource(doc));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    xsltCheck(doc);

  }
}

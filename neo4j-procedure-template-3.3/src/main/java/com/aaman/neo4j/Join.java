package com.aaman.neo4j;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Iterator;
import javax.swing.JFrame;

import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.logging.Log;

import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import com.aaman.neo4j.MovieVertex;
import com.aaman.neo4j.NodeInfo;
import com.aaman.neo4j.PersonVertex;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

/**
 * This is an example how you can create a simple user-defined function for Neo4j.
 */
public class Join
{
	
	// This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;


    
    @UserFunction
    @Description("com.aaman.neo4j.join(['s1','s2',...], delimiter) - join the given strings with the given delimiter.")
    public String join(
            @Name("strings") List<String> strings,
            @Name(value = "delimiter", defaultValue = ",") String delimiter) {
        if (strings == null || delimiter == null) {
            return null;
        }
        return String.join(delimiter, strings);
    }
    
    @UserFunction
    @Description("com.aaman.neo4j.getJungJSON(query) - return JUNG-rendered JSON of query results")
    public String getJungJSON(
            @Name("query") String query) {
    		  String JSONResultStr="";
			try {
				JSONResultStr = generateJSONGraph(query);
			} catch (IOException | SQLException e) {
				return e.getMessage();
			} 

       return JSONResultStr;
    }
    
    
    @UserFunction
    @Description("com.aaman.neo4j.getJungJSONFromPaths(nodes,rels) - return JUNG-rendered JSON of query results")
    public String getJungJSONFromPaths(
            @Name("nodes") List<Node> nodes,
            @Name("rels") List<Relationship> rels) {
    		  String JSONResultStr="";
			try {
				JSONResultStr = generateJSONGraphFromPaths(nodes,rels);
			} catch (IOException e) {
				return e.getMessage();
			} 

       return JSONResultStr;
    }
    
    private String generateJSONGraphFromPaths(List<Node> nodes, List<Relationship> rels) throws IOException {
    		
    		// convert Nodes to RootNodes
    		Iterator<Node> itNode = nodes.iterator();
    		List<RootNode> rtNodeList = new ArrayList<>();
    		while(itNode.hasNext()) {
    			rtNodeList.add(new RootNode( itNode.next()));
    		}
    		
    		return renderJSONGraphFromPaths(loadJungGraphFromPaths(rtNodeList,rels));
    	}

	private String renderJSONGraphFromPaths(DirectedSparseGraph<RootNode, Relationship> g) throws JsonProcessingException {
		String JSONGraph="";
		 ObjectMapper objectMapper = new ObjectMapper();
			     objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

				ISOMLayout<RootNode,Relationship> layout = new ISOMLayout<>(g);
				Dimension viewerDim = new Dimension(800,800);
				Rectangle viewerRect = new Rectangle(viewerDim);
			    VisualizationViewer<RootNode,Relationship> vv =
			      new VisualizationViewer<>(layout, viewerDim);
			    GraphElementAccessor<RootNode,Relationship> pickSupport = 
			            vv.getPickSupport();
			        Collection<RootNode> vertices = 
			            pickSupport.getVertices(layout, viewerRect);
			       // vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
			        for (RootNode vertex: vertices) {
		        			//print JSON version of vertex
		            		JSONGraph += objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vertex.nNode);
			        }
			        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
			        // The following code adds capability for mouse picking of vertices/edges. Vertices can even be moved!
				    final DefaultModalGraphMouse<String,Number> graphMouse = new DefaultModalGraphMouse<>();
				    vv.setGraphMouse(graphMouse);
				    graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
				 	      
					  JFrame frame = new JFrame();
					  frame.getContentPane().add(vv);
					  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					  frame.pack();
					  frame.setVisible(true);
					
			        //print vertices collection as JSON array
			       // String verticesJSON = objectMapper.writeValueAsString(vertices);
			      //  JSONGraph += verticesJSON;
			        return JSONGraph;
	}

	private DirectedSparseGraph<RootNode, Relationship> loadJungGraphFromPaths(List<RootNode> nodes,
			List<Relationship> rels) {
		DirectedSparseGraph<RootNode,Relationship> graph2 = new DirectedSparseGraph<>();
		
		// iterate through Relationships and load graph
		Iterator<Relationship> itrel = rels.iterator();
		while(itrel.hasNext()) {
			Relationship rel = itrel.next();
			//get startNode and endNode and add to graph
			RootNode startNode = new RootNode(rel.getStartNode());
			RootNode endNode = new RootNode(rel.getEndNode());
	
			graph2.addVertex(startNode);
			graph2.addVertex(endNode);
			//add relationship to graph as edge

			graph2.addEdge(rel,startNode,endNode,EdgeType.DIRECTED);
		}
		
		// then iterate through nodes and it should add only 'orphan' nodes
		Iterator<RootNode> itnode = nodes.iterator();
		while(itnode.hasNext()) {
			RootNode node = itnode.next();
			graph2.addVertex(node);
		}
 
		return graph2;
	}

	@UserFunction
    @Description("com.aaman.neo4j.getJungSVG(query) - return JUNG-rendered SVG of query results")
    public String getJungSVG(
            @Name("query") String query) {
    		  String SVGResultStr="";
    		  
			try {
				SVGResultStr = generateSVGGraph(query);
			} catch (IOException | SQLException e) {
				return e.getMessage();
			} 
    		  
        return SVGResultStr;
    }
    


	public  String generateJSONGraph(final String cql)
			throws IOException, SQLException {
		
		return renderJSONGraph(loadJungGraph(cql));
	}

	public  String generateSVGGraph(final String cql) 
			throws IOException,SQLException {
		
		return renderSVGGraph(loadJungGraph(cql));
	}

	public Relationship getRelationshipBetween(Node start, Node end, Direction direction, RelationshipType type) {
	    for (Relationship r: start.getRelationships(direction,type)) {
	       if (r.getOtherNode(start).equals(end)) return r;
	    }
	    return null;
	}
	
	/**
	 * Function to load a JUNG graph object with values from Neo4J
	 * @param cql
	 * @param uri
	 * @param user
	 * @param password
	 * 
	 * @throws SQLException
	 */
	private DirectedSparseGraph<NodeInfo,String> loadJungGraph(final String cql) throws SQLException {
		
		DirectedSparseGraph<NodeInfo,String> graph2 = new DirectedSparseGraph<>();
		Result rs = db.execute(cql);
	
		while (rs.hasNext()) {
			Map<String, Object> row = rs.next();
			Node personNode =(Node)row.get("person");
			Node movieNode = (Node) row.get("movie");
			/*Relationship rel = getRelationshipBetween
					(personNode,movieNode,Direction.OUTGOING,RelationshipType.withName("ACTED_IN"));*/
			 String rel = personNode.getProperty("name") + "-ACTED_IN-"+ movieNode.getProperty("title"); 
			 String sourceNode =personNode.getProperty("name").toString();
			String targetNode = movieNode.getProperty("title").toString();
		/*	graph2.addVertex(targetNode);
			graph2.addVertex(sourceNode);
		
			graph2.addEdge(rel,targetNode,sourceNode);*/

			String released = movieNode.getProperty("released").toString();
			 String tagline =  "tagline";
			 int born = ((Long)personNode.getProperty("born")).intValue();;

		  	MovieVertex mv = new MovieVertex(targetNode,tagline,released,"Movie");
        	  	PersonVertex pv = new PersonVertex(sourceNode,born,"Person");
        	  	
        	  	graph2.addVertex(pv);
        	  
        	  	graph2.addVertex(mv);
        	  	graph2.addEdge(rel, pv, mv);	         
		}  
		return graph2;
		//return graph;
	}


	private String renderJSONGraph(DirectedSparseGraph<NodeInfo, String> g) throws IOException {
	
		String JSONGraph="";
 ObjectMapper objectMapper = new ObjectMapper();
	     objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		ISOMLayout<NodeInfo,String> layout = new ISOMLayout<>(g);
		Dimension viewerDim = new Dimension(800,800);
		Rectangle viewerRect = new Rectangle(viewerDim);
	    VisualizationViewer<NodeInfo,String> vv =
	      new VisualizationViewer<>(layout, viewerDim);
	    GraphElementAccessor<NodeInfo,String> pickSupport = 
	            vv.getPickSupport();
	        Collection<NodeInfo> vertices = 
	            pickSupport.getVertices(layout, viewerRect);
	       // vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
	        for (NodeInfo vertex: vertices) {
        			//print JSON version of vertex
            		JSONGraph += objectMapper.writeValueAsString(vertex);
	        }
        		
	        //print vertices collection as JSON array
	       // String verticesJSON = objectMapper.writeValueAsString(vertices);
	      //  JSONGraph += verticesJSON;
	        return JSONGraph;
	}
	
	private String renderSVGGraph(DirectedSparseGraph<NodeInfo,String> g) throws IOException {
		String svgResult="";
		ISOMLayout<NodeInfo,String> layout = new ISOMLayout<>(g);
		Dimension viewerDim = new Dimension(800,800);
	
	    VisualizationViewer<NodeInfo,String> vv =
	      new VisualizationViewer<>(layout, viewerDim);
	       
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        // The following code adds capability for mouse picking of vertices/edges. Vertices can even be moved!
	    final DefaultModalGraphMouse<String,Number> graphMouse = new DefaultModalGraphMouse<>();
	    vv.setGraphMouse(graphMouse);
	    graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
	 	      
		  JFrame frame = new JFrame();
		  frame.getContentPane().add(vv);
		  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		  frame.pack();
		  frame.setVisible(true);
		  
	
		  
		  // create svg from Visualization
		  Properties p = new Properties(); 
		  p.setProperty("PageSize","A5"); 
		  String svgURI = "/Users/aamanlamba/Downloads/Output.svg";
		  File svgOutput = new File(svgURI);
		  if(svgOutput.exists())
		  		svgOutput.delete();
		  VectorGraphics vg = new SVGGraphics2D(svgOutput,
		  			viewerDim);
		  vg.setProperties(p); 
		  vg.startExport(); 
		  vv.print(vg); 
		  vg.endExport();
		  //ugly way of getting the svg into a string - from the file
		  FileInputStream fis = new FileInputStream(svgOutput);
		  try( BufferedReader br =
		          new BufferedReader( new InputStreamReader(fis, "UTF-8" )))
		  {
		     StringBuilder sb = new StringBuilder();
		     String line;
		     while(( line = br.readLine()) != null ) {
		        sb.append( line );
		        sb.append( '\n' );
		     	}
	     		svgResult= sb.toString();
	  		}
	    		return svgResult;
		}

}
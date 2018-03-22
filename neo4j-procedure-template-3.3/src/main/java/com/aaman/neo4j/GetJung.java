package com.aaman.neo4j;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.Iterator;
import javax.swing.JFrame;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Result;
import org.neo4j.logging.Log;

import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import com.aaman.neo4j.MovieVertex;
import com.aaman.neo4j.NodeInfo;
import com.aaman.neo4j.PersonVertex;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

/**
 * JUNGGraph Neo4J Functions
 */
public class GetJung
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

	/**
	 *  Neo4J UserFunction to get JUNG Graph in JSON format with x,y vertices of ISOMLayout
	 * @param nodes
	 * @param rels
	 * @return String of JUNG Graph in JSON Format
	 * with [] as nodesColl, [] as relsColl
	 * match p=(person:Person {name:"Keanu Reeves"} )-[:ACTED_IN]->(movie:Movie)
	 * with nodes(p) as pNodes, rels(p) as rNodes,nodesColl,relsColl
	* unwind pNodes as pN
	* unwind rNodes as rN
* with nodesColl + collect(distinct pN) as nodesColl, relsColl + collect(distinct rN) as relsColl

* return com.aaman.neo4j.getJungJSONFromPaths(nodesColl,relsColl,"/Users/aamanlamba/Downloads/Output.svg")
	 */
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

	
	/**
	 * 
	 * @param nodes
	 * @param rels
	 * @param svgPath
	 * @return String of SVG Graph 
	 */
	@UserFunction
	@Description("com.aaman.neo4j.getJungSVGFromPaths(nodes,rels,svgPath) - return JUNG-rendered SVG of query results")
	public String getJungSVGFromPaths(
			@Name("nodes") List<Node> nodes,
			@Name("rels") List<Relationship> rels,
			@Name("svgPath") String svgPath) {
		String SVGResultStr="";
		try {
			SVGResultStr = generateSVGGraphFromPaths(nodes,rels,svgPath);
		} catch (IOException e) {
			return e.getMessage();
		} 

		return SVGResultStr;
	}

	/**
	 * 
	 * @param nodes
	 * @param rels
	 * @return
	 * @throws IOException
	 */
	private String generateJSONGraphFromPaths(List<Node> nodes, List<Relationship> rels) throws IOException {

		// convert Nodes to RootNodes
		Iterator<Node> itNode = nodes.iterator();
		List<RootNode> rtNodeList = new ArrayList<>();
		while(itNode.hasNext()) {
			rtNodeList.add(new RootNode( itNode.next()));
		}
	
		return renderJSONGraphFromPaths(loadJungGraphFromPaths(rtNodeList,rels));
	}

	/**
	 * 
	 * @param nodes
	 * @param rels
	 * @param svgPath
	 * @return
	 * @throws IOException
	 */
	private String generateSVGGraphFromPaths(List<Node> nodes, List<Relationship> rels, String svgPath) throws IOException {

		// convert Nodes to RootNodes
		Iterator<Node> itNode = nodes.iterator();
		List<RootNode> rtNodeList = new ArrayList<>();
		while(itNode.hasNext()) {
			rtNodeList.add(new RootNode( itNode.next()));
		}
		
		
		return renderSVGGraphFromPaths(loadJungGraphFromPaths(rtNodeList,rels),svgPath);
	}

	/**
	 * 
	 * @param nodes
	 * @param rels
	 * @param svgPath
	 * @return String of SVG Graph 
	 */
	@UserFunction
	@Description("com.aaman.neo4j.getJungSVGFromPathsTest(nodes,rels,svgPath) - return JUNG-rendered SVG of query results")
	public String getJungSVGFromPathsTest(
			@Name("nodes") List<Node> nodes,
			@Name("rels") List<Relationship> rels,
			@Name("svgPath") String svgPath) {
		String SVGResultStr="";
		try {
			SVGResultStr = renderSVGGraphFromPathsTest(svgPath);
		} catch (IOException e) {
			return e.getMessage();
		} 

		return SVGResultStr;
	}
	/**
	 * Test version of renderSVGGraph
	 * 
	 */
	/**
	 *  Function to return SVG as a String from JUNG graph
	 * @param DirectedSparseGraph<RootNode, RootRelationship> g
	 * @return String
	 * @throws IOException
	 */
	private String renderSVGGraphFromPathsTest( String svgPath) throws IOException {
		DirectedSparseGraph<String,String> g = new DirectedSparseGraph<>();
		String svgResult="";
		 g.addVertex("Vertex1");
		    g.addVertex("Vertex2");
		    g.addVertex("Vertex3");
		    g.addEdge("Edge1", "Vertex1", "Vertex2");
		    g.addEdge("Edge2", "Vertex1", "Vertex3");
		    g.addEdge("Edge3", "Vertex3", "Vertex1");
		
		Dimension viewerDim = new Dimension(800,800);
		Rectangle viewerRect = new Rectangle(viewerDim);
		//VisualizationViewer<String,String> vv =
		//		new VisualizationViewer<>(layout, viewerDim);
	    VisualizationImageServer vs =
	    	      new VisualizationImageServer(
	    	        new ISOMLayout(g), viewerDim);
		GraphElementAccessor<String,String> pickSupport = 
				vs.getPickSupport();
		vs.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vs.setBackground(Color.LIGHT_GRAY);
		// create svg from Visualization
		Properties p = new Properties(); 
		p.setProperty("PageSize","A5"); 
		//svgResult= vv.toString();
		String svgURI = svgPath;
		File svgOutput = new File(svgURI);
		if(svgOutput.exists())
			svgOutput.delete();
		//write to the file for reference for now
		VectorGraphics vg = new SVGGraphics2D(svgOutput, viewerDim);
		vg.setProperties(p); 
		vg.setBackground(Color.YELLOW);

		vg.startExport(); 
		vs.printAll(vg);
		//vv.print(vg); 
		vg.endExport();
	 	     //Display visualization for reference for now
		  JFrame frame = new JFrame();
		  frame.getContentPane().add(vs);
		  frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		  frame.pack();
		  frame.setVisible(true);
	/*	
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
	*/
		
		//write the svg to a ByteOutputStream and then to the String
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		VectorGraphics vg2 = new SVGGraphics2D(bos,viewerDim);
		vg2.setProperties(p);
		vg2.setBackground(Color.YELLOW);
		vg2.startExport();
		vs.print(vg2);
		vg2.endExport();
		svgResult = new String(bos.toByteArray(), "UTF-8");
	
	
		return svgResult;   
	}
	
	/**
	 *  Function to create a JUNG DirectedSparseGraph from a collection of Neo4j Nodes & Relationships
	 * @param nodes
	 * @param rels
	 * @return DirectedSparseGraph<RootNode, RootRelationship
	 */
	private DirectedSparseGraph<RootNode, RootRelationship> loadJungGraphFromPaths(List<RootNode> nodes,
			List<Relationship> rels) {
		DirectedSparseGraph<RootNode,RootRelationship> graph2 = new DirectedSparseGraph<>();
		// convert Relationships to RootRelationships

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
			// convert Relationships to RootRelationships
	
			RootRelationship rtRel = new RootRelationship( rel) ;
			graph2.addEdge(rtRel,startNode,endNode);
		}

		// then iterate through nodes and it should add only 'orphan' nodes
		Iterator<RootNode> itnode = nodes.iterator();
		while(itnode.hasNext()) {
			RootNode node = itnode.next();
			graph2.addVertex(node);
		}

		return graph2;
	}

	/**
	 *  Function to return a JSON-formatted string of nodes with their X,Y ISOMLayout vertices
	 * @param DirectedSparseGraph<RootNode, RootRelationship> g
	 * @return String
	 * @throws JsonProcessingException
	 */
	private String renderJSONGraphFromPaths(DirectedSparseGraph<RootNode, RootRelationship> g) throws JsonProcessingException {
		String JSONGraph="";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

		ISOMLayout<RootNode,RootRelationship> layout = new ISOMLayout<>(g);
		Dimension viewerDim = new Dimension(800,800);
		Rectangle viewerRect = new Rectangle(viewerDim);
		VisualizationViewer<RootNode,RootRelationship> vv =
				new VisualizationViewer<>(layout, viewerDim);
		GraphElementAccessor<RootNode,RootRelationship> pickSupport = 
				vv.getPickSupport();
		Collection<RootNode> vertices = 
				pickSupport.getVertices(layout, viewerRect);
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		//convert vertices to JSON
		vertices  = getDedupVertices(vertices);
		for (RootNode vertex: vertices) {
			//print JSON version of vertex
			
			vertex.posX = layout.getX(vertex);
			vertex.posY = layout.getY(vertex);
			JSONGraph += objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vertex);
		}
		return JSONGraph;
	}
	
	private Collection<RootNode> getDedupVertices(Collection<RootNode> vertices) {
		Collection<RootNode> dedupVertices = Collections.emptyList();
		dedupVertices = vertices.stream().distinct().collect(Collectors.toList());;		
		return dedupVertices;
	}

	/**
	 *  Function to return SVG as a String from JUNG graph
	 * @param DirectedSparseGraph<RootNode, RootRelationship> g
	 * @return String
	 * @throws IOException
	 */
	private String renderSVGGraphFromPaths(DirectedSparseGraph<RootNode, RootRelationship> g, String svgPath) throws IOException {

		String svgResult="";

		ISOMLayout<RootNode,RootRelationship> layout = new ISOMLayout<>(g);
		Dimension viewerDim = new Dimension(800,800);
		Rectangle viewerRect = new Rectangle(viewerDim);
	    VisualizationImageServer vs =
	    	      new VisualizationImageServer(
	    	        new ISOMLayout(g), viewerDim);
		GraphElementAccessor<String,String> pickSupport = 
				vs.getPickSupport();
		vs.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vs.setBackground(Color.LIGHT_GRAY);
		// create svg from Visualization
		Properties p = new Properties(); 
		p.setProperty("PageSize","A5"); 
		String svgURI = svgPath;
		File svgOutput = new File(svgURI);
		if(svgOutput.exists())
			svgOutput.delete();
		//write to the file for reference for now
		VectorGraphics vg = new SVGGraphics2D(svgOutput, viewerDim);
		vg.setProperties(p); 
		vg.setBackground(Color.YELLOW);

		vg.startExport(); 
		vs.print(vg); 
		vg.endExport();
		
		ByteArrayInputStream bis =  new ByteArrayInputStream(FileUtils.readFileToByteArray(svgOutput));
		svgResult  = IOUtils.toString(bis,StandardCharsets.UTF_8);
		/*//ugly way of getting the svg into a string - from the file - this introduces newlines
		FileInputStream fis = new FileInputStream(svgOutput);
		try( BufferedReader br =
				new BufferedReader( new InputStreamReader(fis, "UTF-8" )))
		{
			StringBuilder sb = new StringBuilder();
			String line;
			while(( line = br.readLine()) != null ) {
				sb.append( line );
				
			}
			svgResult= sb.toString();
		}
		//write the svg to a ByteOutputStream and then to the String
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		VectorGraphics vg2 = new SVGGraphics2D(bos,viewerDim);
		vg2.setProperties(p);
		vg2.setBackground(Color.YELLOW);
		vg2.startExport();
		vs.print(vg2);
		vg2.endExport();
		svgResult = new String(bos.toByteArray(), "UTF-8");
		
*/
	  
	/*
		// The following code adds capability for mouse picking of vertices/edges. Vertices can even be moved!
	    final DefaultModalGraphMouse<String,Number> graphMouse = new DefaultModalGraphMouse<>();
	    vv.setGraphMouse(graphMouse);
	    graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
	 	     //Display visualization for reference for now
		  JFrame frame = new JFrame();
		  frame.getContentPane().add(vv);
		  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		  frame.pack();
		  frame.setVisible(true);
		
		//write the svg to a ByteOutputStream and then to the String
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		VectorGraphics vg2 = new SVGGraphics2D(bos,viewerDim);
		vg2.setProperties(p);
		vg2.setBackground(Color.YELLOW);
		vg2.startExport();
		vv.print(vg2);
		vg2.endExport();
		svgResult = new String(bos.toByteArray(), "UTF-8");
	*/
	
		return svgResult;   
	}



}
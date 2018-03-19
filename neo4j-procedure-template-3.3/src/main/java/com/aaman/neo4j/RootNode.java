package com.aaman.neo4j;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class RootNode implements NodeInfo{
	protected String name;
	protected String id;
	protected String type;
	protected Map<String,Object> nNode;
	protected RootNode() {
		name="";
		id="";
		type="";
		nNode=null;
	}
	protected RootNode(Node n) {
		
		
		if(n.hasProperty("name"))
			this.name = n.getProperty("name").toString();
		else {
			Iterable<Label> itlab = n.getLabels();
			for(Label lab: itlab) {
				//take the first label
				this.name = lab.name();
				break;
			}
		}
		Iterable<Label> itlab = n.getLabels();
		for(Label lab: itlab) {
			//take the first label
			this.type = lab.name();
			break;
		}
		this.id = ((Long)n.getId()).toString();
		nNode = n.getAllProperties();
	}
	
	protected RootNode(String name,String id,String type) {
		this.name=name;
		this.id=id;
		this.type = type;
		this.nNode=null;
	}
	protected String getName() {
		return name;
	}
	
	
	
	protected void setName(String name) {
		this.name = name;
	}
	protected String getID() {
		return id;
	}
	protected void setID(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	protected void setType(String type) {
		this.type = type;
	}
	
	
	@Override
	public String toString() {
		return name;
		
	}
	
	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof RootNode)) {
            return false;
        }

        RootNode rt = (RootNode) o;

        return new EqualsBuilder()
                .append(id, rt.id)
                .isEquals();
    }
  
  @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }
}
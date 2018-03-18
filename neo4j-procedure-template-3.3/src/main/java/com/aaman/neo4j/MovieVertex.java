package com.aaman.neo4j;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class MovieVertex extends RootNode implements NodeInfo {
	protected String title;
	protected String tagline;
	protected String released;
	
	MovieVertex(){
		super();

		title="";
		tagline="";
		released="";
	}

	public MovieVertex(String targetNode, String tagline2, String released2,String type) {
		super(targetNode,targetNode,type);
		title = targetNode;
		tagline = tagline2;
		released = released2;
	}
	@Override 
	public String getType() {
		return super.getType();
	}
	@Override
	public String toString() {
		return this.title;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTagline() {
		return tagline;
	}

	public void setTagline(String tagline) {
		this.tagline = tagline;
	}

	public String getReleased() {
		return released;
	}

	public void setReleased(String released) {
		this.released = released;
	}
	
	  @Override
	    public boolean equals(Object o) {

	        if (o == this) return true;
	        if (!(o instanceof MovieVertex)) {
	            return false;
	        }

	        MovieVertex mv = (MovieVertex) o;

	        return new EqualsBuilder()
	                .append(title, mv.title)
	                .append(tagline, mv.tagline)
	                .append(released, mv.released)
	                .isEquals();
	    }
	  
	  @Override
	    public int hashCode() {
	        return new HashCodeBuilder(17, 37)
	                .append(title)
	                .append(tagline)
	                .append(released)
	                .toHashCode();
	    }
}

package com.aaman.neo4j;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PersonVertex extends RootNode implements NodeInfo {

	protected String personName;
	protected int born;
	
	@Override
	public String getName() {
		return personName;
	}
	@Override
	public void setName(String name) {
		this.personName = name;
	}

	public int getBorn() {
		return born;
	}

	public void setBorn(int born) {
		this.born = born;
	}

	public PersonVertex() {
		super();
		personName="";
		born=0;
	}
	public PersonVertex(String sourceNode, int born,String type) {
		super(sourceNode,sourceNode,type);
		this.personName = sourceNode;
		this.born = born;
	}
	@Override 
	public String getType() {
		return super.getType();
	}
	
	@Override
	public String toString() {
		return this.personName;
	}

	  @Override
	    public boolean equals(Object o) {

	        if (o == this) return true;
	        if (!(o instanceof PersonVertex)) {
	            return false;
	        }

	        PersonVertex mv = (PersonVertex) o;

	        return new EqualsBuilder()
	                .append(personName, mv.personName)
	                .append(born, mv.born)
	                .isEquals();
	    }
	  
	  @Override
	    public int hashCode() {
	        return new HashCodeBuilder(17, 37)
	                .append(personName)
	                .append(born)
	                .toHashCode();
	    }
}

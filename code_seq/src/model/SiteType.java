package model;

import java.awt.Color;
import java.io.Serializable;


/**
 * 
 * @author Gisler Christophe
 *
 */
public class SiteType implements Serializable {

    protected final String name;
    protected final Color color;
    
    public SiteType(String name, Color color) {
		this.name = name;
		this.color = color;
    }

    public String getName() {
    	return name;
    }
    
    public Color getColor() {
    	return color;
    }
    
    public boolean equals(SiteType s) {
    	return this.getName().equals(s.getName());
    }
    
}

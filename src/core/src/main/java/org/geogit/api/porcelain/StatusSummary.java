package org.geogit.api.porcelain;

import java.awt.Color;
import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;

public class StatusSummary {
  private List<Conflict> conflicts;
  private Iterator<DiffEntry> staged, unstaged;
  private String message;
  
  
  
	public String getMessage() {
	return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public List<Conflict> getConflicts() {
		return conflicts;
	}
	
	public void setConflicts(List<Conflict> conflicts) {
		this.conflicts = conflicts;
	}
	
	public Iterator<DiffEntry> getStaged() {
		return staged;
	}
	
	public void setStaged(Iterator<DiffEntry> staged) {
		this.staged = staged;
	}
	
	public Iterator<DiffEntry> getUnstaged() {
		return unstaged;
	}
	
	public void setUnstaged(Iterator<DiffEntry> unstaged) {
		this.unstaged = unstaged;
	}

	
  public StatusSummary() {
    this.conflicts = new ArrayList<Conflict>();
    this.staged = null;
    this.unstaged = null;
    
  }
  
}

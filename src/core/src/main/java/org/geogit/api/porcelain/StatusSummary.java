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
  
  public StatusSummary() {
    this.conflicts = new ArrayList<Conflict>();
    this.staged = null;
    this.message = "";
  }
  
  public String getMessage() {
    return this.message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  public void print(Console console, GeoGIT geogit, Color color) {
    System.out.println("TODO");
    return;
  }
  
  public void printUnmerged(final ConsoleReader console,
                            final List<Conflict> conflicts,
                            final Color color,
                            final int total) throws IOException {
    
    
  }
}

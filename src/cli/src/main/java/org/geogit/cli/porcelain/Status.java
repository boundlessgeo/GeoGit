/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli.porcelain;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.porcelain.StatusOp;
import org.geogit.api.porcelain.StatusSummary;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Displays features that have differences between the index and the current HEAD commit and
 * features that have differences between the working tree and the index file. The first are what
 * you would commit by running geogit commit; the second are what you could commit by running geogit
 * add before running geogit commit.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit status [<options>]}
 * </ul>
 * 
 * @see Commit
 * @see Add
 */
@Parameters(commandNames = "status", commandDescription = "Show the working tree status")
public class Status extends AbstractCommand implements CLICommand {

    @Parameter(names = "--limit", description = "Limit number of displayed changes. Must be >= 0.")
    private Integer limit = 50;

    @Parameter(names = "--all", description = "Force listing all changes (overrides limit).")
    private boolean all = false;

    /**
     * Executes the status command using the provided options.
     */
    @Override
    public void runInternal(GeogitCLI cli) throws IOException {
        checkParameter(limit >= 0, "Limit must be 0 or greater.");

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        final StagingArea index = geogit.getRepository().getIndex();

        final WorkingTree workTree = geogit.getRepository().getWorkingTree();

//        final long countStaged = index.countStaged(null).getCount();
//        final int countConflicted = index.countConflicted(null);
//        final long countUnstaged = workTree.countUnstaged(null).getCount();
        
        StatusOp op = new StatusOp(index,workTree,geogit);
        StatusSummary summary = op.call();
        
        
        final Optional<Ref> currHead = geogit.command(RefParse.class).setName(Ref.HEAD).call();
        checkParameter(currHead.isPresent(), "Repository has no HEAD.");
        
        if (currHead.get() instanceof SymRef) {
            final SymRef headRef = (SymRef) currHead.get();
            console.println("# On branch " + Ref.localName(headRef.getTarget()));
        } else {
            console.println("# Not currently on any branch.");
        }
        
        print(console, summary);
       
    }
    
    private void print(ConsoleReader console,StatusSummary summary) throws IOException{
    	long countStaged = summary.getCountStaged();
    	long countUnstaged = summary.getCountUnstaged();
    	int countConflicted = summary.getCountConflicts();
    	
      if (summary.getCountStaged() + countUnstaged + countConflicted == 0) {
    	  print(console,summary.getStaged(),Color.GREEN, countStaged + countUnstaged + countConflicted);
	  }
	
	  if (countStaged > 0) {
		  print(console,summary.getStaged(),Color.GREEN, countStaged + countUnstaged + countConflicted);
	  }
	
	  if (countConflicted > 0) {
		  printUnmerged(console,summary.getConflicts(),Color.RED, (int) (countStaged + countUnstaged + countConflicted));
	  }
	
	  if (countUnstaged > 0) {
		  print(console,summary.getUnstaged(),Color.RED, countStaged + countUnstaged + countConflicted);
	  }
    	
    }

    /**
     * Prints the list of changes using the specified options
     * 
     * @param console the output console
     * @param changes an iterator of differences to print
     * @param color the color to use for the changes if color use is enabled
     * @param total the total number of changes
     * @throws IOException
     * @see DiffEntry
     */
    private void print(final ConsoleReader console, final Iterator<DiffEntry> changes,
            final Color color, final long total) throws IOException {

        final int limit = all || this.limit == null ? Integer.MAX_VALUE : this.limit.intValue();

        StringBuilder sb = new StringBuilder();

        Ansi ansi = newAnsi(console.getTerminal(), sb);

        DiffEntry entry;
        ChangeType type;
        String path;
        int cnt = 0;
        if (limit > 0) {
            Iterator<DiffEntry> changesIterator = changes;
            while (changesIterator.hasNext() && cnt < limit) {
                ++cnt;

                entry = changesIterator.next();
                type = entry.changeType();
                path = formatPath(entry);

                sb.setLength(0);
                ansi.a("#      ").fg(color).a(type.toString().toLowerCase()).a("  ").a(path)
                        .reset();
                console.println(ansi.toString());
            }
        }
        sb.setLength(0);
        ansi.a("# ").a(total).reset().a(" total.");
        console.println(ansi.toString());
    }

    private void printUnmerged(final ConsoleReader console, final List<Conflict> conflicts,
            final Color color, final int total) throws IOException {

        final int limit = all || this.limit == null ? Integer.MAX_VALUE : this.limit.intValue();

        StringBuilder sb = new StringBuilder();

        Ansi ansi = newAnsi(console.getTerminal(), sb);

        String path;
        for (int i = 0; i < conflicts.size() && i < limit; i++) {
            path = conflicts.get(i).getPath();
            sb.setLength(0);
            ansi.a("#      ").fg(color).a("unmerged").a("  ").a(path).reset();
            console.println(ansi.toString());
        }

        sb.setLength(0);
        ansi.a("# ").a(total).reset().a(" total.");
        console.println(ansi.toString());
    }

    /**
     * Formats a DiffEntry for display
     * 
     * @param entry the DiffEntry to format
     * @return the formatted display string
     * @see DiffEntry
     */
    private String formatPath(DiffEntry entry) {
        String path;
        NodeRef oldObject = entry.getOldObject();
        NodeRef newObject = entry.getNewObject();
        if (oldObject == null) {
            path = newObject.path();
        } else if (newObject == null) {
            path = oldObject.path();
        } else {
            if (oldObject.path().equals(newObject.path())) {
                path = oldObject.path();
            } else {
                path = oldObject.path() + " -> " + newObject.path();
            }
        }
        return path;
    }
}

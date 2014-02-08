package org.geogit.api.porcelain;

import java.util.Iterator;
import java.util.List;

import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsReadOp;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

import com.google.common.base.Optional;

public class StatusOp extends AbstractGeoGitOp<StatusSummary> {
  private long numStaged;
  private int numConflicts;
  private long numUnstaged;
  
  public StatusOp(StagingArea index, WorkingTree workTree) {
    this.numStaged = index.countStaged(null).getCount();
    this.numConflicts = index.countConflicted(null);
    this.numUnstaged = workTree.countUnstaged(null).getCount();
  }

  @Override
  public StatusSummary call() {
    StatusSummary summary = new StatusSummary();
    if (numStaged + numUnstaged + numConflicts == 0) {
        summary.setMessage("nothing to commit (working directory clean)");
    }

    if (numStaged > 0) {
        String msg = "# Changes to be committed:\n" +
                     "#   (use \"geogit reset HEAD <path/to/fid>...\" to unstage)\n" +
                     "#";
        summary.setMessage(msg);
    }

    if (numConflicts > 0) {
        List<Conflict> conflicts = geogit.command(ConflictsReadOp.class).call();
        String msg = "# Unmerged paths:\n" +
                     "#   (use \"geogit add/rm <path/to/fid>...\" as appropriate to mark resolution\n" +
                     "#";
        

        printUnmerged(console, conflicts, Color.RED, numConflicts);
    }

    if (numUnstaged > 0) {
        Iterator<DiffEntry> unstaged = geogit.command(DiffWorkTree.class).setReportTrees(true)
                .call();
        console.println("# Changes not staged for commit:");
        console.println("#   (use \"geogit add <path/to/fid>...\" to update what will be committed");
        console.println("#   (use \"geogit checkout -- <path/to/fid>...\" to discard changes in working directory");
        console.println("#");
        print(console, unstaged, Color.RED, numUnstaged);
    }

  }
}

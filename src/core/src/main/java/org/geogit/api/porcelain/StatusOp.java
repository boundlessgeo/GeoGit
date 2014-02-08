package org.geogit.api.porcelain;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsReadOp;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

public class StatusOp extends AbstractGeoGitOp<StatusSummary> {
  private GeoGIT geogit;
  
  private long countStaged;
  private int countConflicted;
  private long countUnstaged;
  
  public StatusOp(StagingArea index, WorkingTree workTree, GeoGIT geogit) {
    this.countStaged = index.countStaged(null).getCount();
    this.countConflicted = index.countConflicted(null);
    this.countUnstaged = workTree.countUnstaged(null).getCount();
    this.geogit = geogit;
  }

  @Override
  public StatusSummary call() {
	  
	  StatusSummary summary = new StatusSummary();
      
    if (countStaged + countUnstaged + countConflicted == 0) {
      summary.setMessage("nothing to commit (working directory clean)");
    }

      if (countStaged > 0) {
          Iterator<DiffEntry> staged = geogit.command(DiffIndex.class).setReportTrees(true)
                  .call();
//          console.println("# Changes to be committed:");
//          console.println("#   (use \"geogit reset HEAD <path/to/fid>...\" to unstage)");
//          console.println("#");
          
          // print(console, staged, Color.GREEN, countStaged);
          String msg = "# Changes to be committed:" +
        		  	   "#   (use \"geogit reset HEAD <path/to/fid>...\" to unstage)" + 
        		  	   "#";
          summary.setMessage(msg);
          summary.setStaged(staged);
          
      }

      if (countConflicted > 0) {
          List<Conflict> conflicts = geogit.command(ConflictsReadOp.class).call();
//          console.println("# Unmerged paths:");
//          console.println("#   (use \"geogit add/rm <path/to/fid>...\" as appropriate to mark resolution");
//          console.println("#");
          // printUnmerged(console, conflicts, Color.RED, countConflicted);
          String msg = "# Unmerged paths:\n" +
	        		   "#   (use \"geogit add/rm <path/to/fid>...\" as appropriate to mark resolution\n"+
	        		   "#";
          summary.setMessage(msg);
          summary.setConflicts(conflicts);
      }

      if (countUnstaged > 0) {
          Iterator<DiffEntry> unstaged = geogit.command(DiffWorkTree.class).setReportTrees(true)
                  .call();
//          console.println("# Changes not staged for commit:");
//          console.println("#   (use \"geogit add <path/to/fid>...\" to update what will be committed");
//          console.println("#   (use \"geogit checkout -- <path/to/fid>...\" to discard changes in working directory");
//          console.println("#");
          // print(console, unstaged, Color.RED, countUnstaged);
          String msg = "# Changes not staged for commit:\n" +
	        		   "#   (use \"geogit add <path/to/fid>...\" to update what will be committed\n" +
	        		   "#   (use \"geogit checkout -- <path/to/fid>...\" to discard changes in working directory" + 
	        		   "#";
          summary.setMessage(msg);
          summary.setUnstaged(unstaged);
      }
      
      summary.setCountStaged(this.countStaged);
      summary.setCountUnstaged(this.countUnstaged);
      summary.setCountConflicts(this.countConflicted);
      
      return summary;
  }
}


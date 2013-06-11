package org.geogit.web.api.commands;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.CommandLocator;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.LogOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Interface for the Log operation in GeoGit.
 * 
 * Web interface for {@link LogOp}
 */
public class Log extends AbstractWebAPICommand {

    Integer skip;

    Integer limit;

    String since;

    String until;

    List<String> paths;

    private int page;

    private int elementsPerPage;

    boolean firstParentOnly;

    /**
     * Mutator for the limit variable
     * 
     * @param limit - the number of commits to print
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * Mutator for the offset variable
     * 
     * @param offset - the offset to start listing at
     */
    public void setOffset(Integer offset) {
        this.skip = offset;
    }

    /**
     * Mutator for the since variable
     * 
     * @param since - the start place to list commits
     */
    public void setSince(String since) {
        this.since = since;
    }

    /**
     * Mutator for the until variable
     * 
     * @param until - the end place for listing commits
     */
    public void setUntil(String until) {
        this.until = until;
    }

    /**
     * Mutator for the paths variable
     * 
     * @param paths - list of paths to filter commits by
     */
    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    /**
     * Mutator for the page variable
     * 
     * @param page - the page number to build the response
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Mutator for the elementsPerPage variable
     * 
     * @param elementsPerPage - the number of elements to display in the response per page
     */
    public void setElementsPerPage(int elementsPerPage) {
        this.elementsPerPage = elementsPerPage;
    }

    /**
     * Mutator for the firstParentOnly variable
     * 
     * @param firstParentOnly - true to only show the first parent of a commit
     */
    public void setFirstParentOnly(boolean firstParentOnly) {
        this.firstParentOnly = firstParentOnly;
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     * 
     * @throws IllegalArgumentException
     */
    @Override
    public void run(CommandContext context) {
        final CommandLocator geogit = this.getCommandLocator(context);

        LogOp op = geogit.command(LogOp.class).setFirstParentOnly(firstParentOnly);

        if (skip != null) {
            op.setSkip(skip.intValue());
        }
        if (limit != null) {
            op.setLimit(limit.intValue());
        }

        if (this.since != null) {
            Optional<ObjectId> since;
            since = geogit.command(RevParse.class).setRefSpec(this.since).call();
            Preconditions.checkArgument(since.isPresent(), "Object not found '%s'", this.since);
            op.setSince(since.get());
        }
        if (this.until != null) {
            Optional<ObjectId> until;
            until = geogit.command(RevParse.class).setRefSpec(this.until).call();
            Preconditions.checkArgument(until.isPresent(), "Object not found '%s'", this.until);
            op.setUntil(until.get());
        }
        if (paths != null && !paths.isEmpty()) {
            for (String path : paths) {
                op.addPath(path);
            }
        }

        final Iterator<RevCommit> log = op.call();
        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeCommits(log, page, elementsPerPage);
                out.finish();
            }
        });

    }
}

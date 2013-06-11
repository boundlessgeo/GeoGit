package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.CommandLocator;
import org.geogit.api.Remote;
import org.geogit.api.porcelain.RemoteListOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;

/**
 * Interface for the Remote operations in GeoGit. Currently only supports listing of remotes.
 * 
 * Web interface for {@link RemoteListOp}
 */

public class RemoteWebOp extends AbstractWebAPICommand {

    private boolean list;

    /**
     * Mutator for the list variable
     * 
     * @param list - true to list the names of your remotes
     */
    public void setList(boolean list) {
        this.list = list;
    }

    /**
     * Runs the command and builds the appropriate response.
     * 
     * @param context - the context to use for this command
     */
    @Override
    public void run(CommandContext context) {
        if (list) {
            final CommandLocator geogit = this.getCommandLocator(context);
            final List<Remote> remotes = geogit.command(RemoteListOp.class).call();

            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeRemoteListResponse(remotes);
                    out.finish();
                }
            });
        }
    }

}

package org.geogit.gui.cli;

import java.io.IOException;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.RequiresRepository;
import org.geogit.gui.internal.MapPane;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;

@RequiresRepository(true)
@Parameters(commandNames = "map", commandDescription = "Opens a map")
public class Map extends AbstractCommand {

    @Parameter(description = "<layer names>,...")
    private List<String> layerNames = Lists.newArrayList();

    @Override
    protected void runInternal(GeogitCLI cli) {
        GeoGIT geogit = cli.getGeogit();
        MapPane mapPane;
        try {
            mapPane = new MapPane(geogit, layerNames);
        } catch (IOException e) {
            throw new CommandFailedException(e);
        }
        mapPane.show();
    }

}

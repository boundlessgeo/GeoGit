/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jline.Terminal;
import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.RevPerson;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.LogOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Shows the commit logs.
 * <p>
 * CLI proxy for {@link org.geogit.api.porcelain.LogOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit log [<options>]}
 * </ul>
 * 
 * @see org.geogit.api.porcelain.LogOp
 */
@Parameters(commandNames = "log", commandDescription = "Show commit logs")
public class Log extends AbstractCommand implements CLICommand {

    @Parameter(names = { "--max-count", "-n" }, description = "Maximum number of commits to log.")
    private Integer limit;

    @Parameter(names = "--skip", description = "Skip number commits before starting to show the commit output.")
    private Integer skip;

    @Parameter(names = "--since", description = "Maximum number of commits to log")
    private String since;

    @Parameter(names = "--until", description = "Maximum number of commits to log")
    private String until;

    @Parameter(names = "--oneline", description = "Print only commit id and message on a sinlge line per commit")
    private boolean oneline;

    @Parameter(description = "[[<until>]|[<since>..<until>]] [<path>]...]")
    private List<String> sinceUntilPaths = Lists.newArrayList();

    @Parameter(names = "--color", description = "Whether to apply colored output. Possible values are auto|never|always.", converter = ColorArg.Converter.class)
    private ColorArg color = ColorArg.auto;

    /**
     * Executes the log command using the provided options.
     * 
     * @param cli
     * @throws IOException
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        final Platform platform = cli.getPlatform();
        Preconditions.checkState(cli.getGeogit() != null, "Not a geogit repository: "
                + platform.pwd().getAbsolutePath());

        final GeoGIT geogit = cli.getGeogit();

        LogOp op = geogit.command(LogOp.class);

        if (skip != null) {
            op.setSkip(skip.intValue());
        }
        if (limit != null) {
            op.setLimit(limit.intValue());
        }
        if (!sinceUntilPaths.isEmpty()) {
            List<String> sinceUntil = ImmutableList.copyOf((Splitter.on("..").split(sinceUntilPaths
                    .get(0))));
            Preconditions.checkArgument(sinceUntil.size() == 1 || sinceUntil.size() == 2,
                    "Invalid refSpec format, expected [<until>]|[<since>..<until>]: %s",
                    sinceUntilPaths.get(0));

            String sinceRefSpec;
            String untilRefSpec;
            if (sinceUntil.size() == 1) {
                // just until was given
                sinceRefSpec = null;
                untilRefSpec = sinceUntil.get(0);
            } else {
                sinceRefSpec = sinceUntil.get(0);
                untilRefSpec = sinceUntil.get(1);
            }
            if (sinceRefSpec != null) {
                Optional<ObjectId> since;
                since = geogit.command(RevParse.class).setRefSpec(sinceRefSpec).call();
                Preconditions.checkArgument(since.isPresent(), "Object not found '%s'",
                        sinceRefSpec);
                op.setSince(since.get());
            }
            if (untilRefSpec != null) {
                Optional<ObjectId> until;
                until = geogit.command(RevParse.class).setRefSpec(untilRefSpec).call();
                Preconditions.checkArgument(until.isPresent(), "Object not found '%s'",
                        sinceRefSpec);
                op.setUntil(until.get());
            }
        }
        Iterator<RevCommit> log = op.call();
        ConsoleReader console = cli.getConsole();
        Terminal terminal = console.getTerminal();
        final boolean useColor;
        switch (color) {
        case never:
            useColor = false;
            break;
        case always:
            useColor = true;
            break;
        default:
            useColor = terminal.isANSISupported();
        }

        if (!log.hasNext()) {
            console.println("No commits to show");
            console.flush();
            return;
        }

        Function<RevCommit, CharSequence> printFunction;
        if (oneline) {
            printFunction = oneLineConverter(useColor);
        } else {
            printFunction = standardConverter(useColor, geogit.getPlatform());
        }

        Iterator<CharSequence> formattedLog = Iterators.transform(log, printFunction);
        while (formattedLog.hasNext()) {
            CharSequence formattedCommit = formattedLog.next();
            console.println(formattedCommit);
            console.flush();
        }
    }

    /**
     * @param useColor
     * @return
     */
    private Function<RevCommit, CharSequence> oneLineConverter(final boolean useColor) {
        return new Function<RevCommit, CharSequence>() {

            @Override
            public CharSequence apply(RevCommit commit) {
                Ansi ansi = AnsiDecorator.newAnsi(useColor);
                ansi.fg(Color.YELLOW).a(commit.getId().toString()).reset();
                String message = Strings.nullToEmpty(commit.getMessage());
                String title = Splitter.on('\n').split(message).iterator().next();
                ansi.a(" ").a(title);
                return ansi.toString();
            }
        };
    }

    /**
     * @param useColor
     * @return
     */
    private Function<RevCommit, CharSequence> standardConverter(final boolean useColor,
            final Platform platform) {
        return new Function<RevCommit, CharSequence>() {

            @Override
            public CharSequence apply(RevCommit commit) {
                Ansi ansi = AnsiDecorator.newAnsi(useColor);

                ansi.a("Commit:  ").fg(Color.YELLOW).a(commit.getId().toString()).reset().newline();
                ansi.a("Author:  ").fg(Color.GREEN).a(formatPerson(commit.getAuthor())).reset()
                        .newline();
                ansi.a("Date:    (").fg(Color.RED)
                        .a(estimateSince(platform, commit.getTimestamp())).reset().a(") ")
                        .a(new Date(commit.getTimestamp())).newline();
                ansi.a("Subject: ").a(commit.getMessage()).newline();
                return ansi.toString();
            }
        };
    }

    /**
     * Converts a RevPersion for into a readable string.
     * 
     * @param person the person to format.
     * @return the formatted string
     * @see RevPerson
     */
    private String formatPerson(RevPerson person) {
        StringBuilder sb = new StringBuilder();
        if (person.getName() == null) {
            sb.append("<name not set>");
        } else {
            sb.append(person.getName());
        }
        if (person.getEmail() != null) {
            sb.append(" <").append(person.getEmail()).append(">");
        }
        return sb.toString();
    }

    /**
     * Converts a timestamp into a readable string that represents the rough time since that
     * timestamp.
     * 
     * @param platform
     * @param timestamp
     * @return
     */
    private String estimateSince(Platform platform, long timestamp) {
        long now = platform.currentTimeMillis();
        long diff = now - timestamp;
        final long seconds = 1000;
        final long minutes = seconds * 60;
        final long hours = minutes * 60;
        final long days = hours * 24;
        final long weeks = days * 7;
        final long months = days * 30;
        final long years = days * 365;

        if (diff > years) {
            return diff / years + " years ago";
        }
        if (diff > months) {
            return diff / months + " months ago";
        }
        if (diff > weeks) {
            return diff / weeks + " weeks ago";
        }
        if (diff > days) {
            return diff / days + " days ago";
        }
        if (diff > hours) {
            return diff / hours + " hours ago";
        }
        if (diff > minutes) {
            return diff / minutes + " minutes ago";
        }
        if (diff > seconds) {
            return diff / seconds + " seconds ago";
        }
        return "just now";
    }
}

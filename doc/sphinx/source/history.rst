Exploring you history
======================

We have already seen how to explore the content of a a GeoGit repository. It is also interesting to explore its history, since it will show us what has happened since it was created. Exploring the history can answer questions such as "who has edited this feature?", "since when does this feature exist?" or "how many edits has a given person made during the last month?", just to name a few.

The log command, which we have already seen, is used to browse the history of a GeoGit repository. In its most basic form, as we know, it just shows a list of all commits, ordered by its commit date.

::

	$geogit log


We can change two things in the output of the log command: the amount of information displayed for each commit, and the commits that are printed out. In the fist case, we can tell GeoGit to just show one line for each commit or, if we want the full information, to display all the changes introduced by each commit. To select which commits which commits should be shown, we can request a restricted list of them by setting time constraints, the commits of a single author, or just the history of the current branch without merged branches, among other available options.

Setting how commits are displayed
----------------------------------

One of the most useful options of the ``log`` command is ``--oneline``. If the usual description of a commit is too verbose for your needs, you can reduce it to just one single line, so more commits can be shown in your screen.

.. note:: ``git`` uses the ``less`` command by default to page results. GeoGit just prints the commits history without paging, and it is up to you to pipe it into a console utility to page it.

Here you can see an example of the output that the ``log`` command produces when the ``--oneline`` option used.

::

	$geogit log --oneline
	6f250c0c74d9cb852405818f4a5e50aa971d543c Merged branch1
	7aa528d3d3437ca8015f1dcfb8a61e197aa09dbe Changed unit in area field
	da1534a4aae8a1b29974c040c9fb44be426c58f8 Added missing feature
	159b517523e05083a18f5626439017663610deeb Minor changes
	6cda554ac1af6c0fad1841b5b36018bd107d926d First import

Sometimes it is useful not to summarize commits into a single line, but instead to display more information about the changes introduced in each one. Two commands are available for this: "--stats" and ``--summary``.

The ``--stats`` option prints an additional line with the number of objects that have been modified, added or removed, as it can be seen below.

::

	$geogit log --stats
	Commit:  6f250c0c74d9cb852405818f4a5e50aa971d543c
	Merge: 7aa528dda1534a
	Author:  volaya <volaya@opengeo.org>
	Date:    (1 day ago) 2013-03-14 10:05:06 +0100
	Subject: Merge branch1

	Commit:  7aa528d3d3437ca8015f1dcfb8a61e197aa09dbe
	Author:  volaya <volaya@opengeo.org>
	Date:    (1 day ago) 2013-03-14 10:05:02 +0100
	Subject: Changed unit in area field
	Changes: 0 features added, 5 changed, 0 deleted.

	Commit:  da1534a4aae8a1b29974c040c9fb44be426c58f8
	Author:  volaya <volaya@opengeo.org>
	Date:    (1 day ago) 2013-03-14 10:04:49 +0100
	Subject: Added missing feature
	Changes: 1 features added, 0 changed, 0 deleted.

	Commit:  159b517523e05083a18f5626439017663610deeb
	Author:  volaya <volaya@opengeo.org>
	Date:    (1 day ago) 2013-03-14 10:04:36 +0100
	Subject: Minor changes
	Changes:1 features added, 4 changed, 0 deleted.

	Commit:  6cda554ac1af6c0fad1841b5b36018bd107d926d
	Author:  volaya <volaya@opengeo.org>
	Date:    (1 day ago) 2013-03-14 10:04:27 +0100
	Subject: First import

The first commit to appear (the last one that was made) has no change information, since it is a merge commit. We will discuss merges a bit later.

If you actually want to know which elements have changed, the ``--names--only`` option will add that information to the description of each commit. Only one log entry is shown, for the sake of space.

::

	$geogit log --names-only
	Commit:  159b517523e05083a18f5626439017663610deeb
	Author:  volaya <volaya@opengeo.org>
	Date:    (1 day ago) 2013-03-14 10:04:36 +0100
	Subject: Minor changes
	Changes:
		parks/parks.2
		parks/parks.3
		parks/parks.4
		parks/parks.5
		parks/parks.1

A full description of the differences introduced by the commit for each affected feature can be obtained using the ``--summary`` option.

::

	$geogit log --summary
	Commit:  159b517523e05083a18f5626439017663610deeb
	Author:  volaya <volaya@opengeo.org>
	Date:    (1 day ago) 2013-03-14 10:04:36 +0100
	Subject: Minor changes

	49852c... 49852c... 84150c... bfd1d4...   M  parks/parks.2
	the_geom: 0 point(s) deleted, 3 new point(s) added, 8 point(s) moved
	perimeter: 572.1249849079909 -> 441.0000642357465
	area: 17345.00048828125 -> 9945.093872070312
	name: "Mc Loughlin Junior School" -> "Roosevelt School"

	49852c... 49852c... 5347d1... 84150c...   M  parks/parks.3
	the_geom: 327 point(s) deleted, 0 new point(s) added, 8 point(s) moved
	perimeter: 1020.6679216977345 -> 572.1249849079909
	area: 54662.815673828125 -> 17345.00048828125
	parktype: "Park" -> "School Field"
	owner: "City Of Medford" -> "Medford School District"
	name: "Hawthorne Park / Pool" -> "Mc Loughlin Junior School"
	agency: "City Of Medford" -> "Medford School District"
	.
	.
	.



Setting which commits are displayed (history simplification)
-------------------------------------------------------------

Another way of limiting the history that is displayed is by just printing out commits from the current branch. The history displayed corresponds to the current branch, but if other branches have been merged into it, their history will also be displayed. Using the ``--first-parent`` option will cause the ``log`` command to just follow the first parent when traversing the history tree, so commits in branches other than the main one are discarded.

Let's say we have the following repository, and we are on the ``master`` branch.

.. figure:: log_branches.png

The default log output would be like the one shown next:

::

	$geogit log --oneline
	86562df2e4f357b05abd2f61c5db2013185f478a Merge commit 'be697a1404f1ead7d92b3e9c094627b3e047aae0'.
	7844674611811fe888852ca9418028dd900f4a8e Changed unit in area field
	be697a1404f1ead7d92b3e9c094627b3e047aae0 Added missing feature
	d0bb10b4aee5fe9961d361031d548df3136cada2 Minor changes
	a87bc4f1725115d5cbdcf22a1b9914cf37911e18 First import


Using the ``--first-parent`` option, the output would be like this:

::

	$geogit log --oneline --first-parent
	6f250c0c74d9cb852405818f4a5e50aa971d543c Merge branch1
	7aa528d3d3437ca8015f1dcfb8a61e197aa09dbe Changed unit in area field
	159b517523e05083a18f5626439017663610deeb Minor changes
	6cda554ac1af6c0fad1841b5b36018bd107d926d First import

You can see that the commit in the branch that was merged onto the current branch is not displayed.

If, instead, you want to show the full history of the repository, including all branches and not just the current one, the ``--all`` option can be used.

::

	$geogit log --oneline -all
	adf66fe8b06bd810767de4222b387c7b2a18233f Edited wrong geometries
	6f250c0c74d9cb852405818f4a5e50aa971d543c Merge branch1
	7aa528d3d3437ca8015f1dcfb8a61e197aa09dbe Changed unit in area field
	da1534a4aae8a1b29974c040c9fb44be426c58f8 Added missing feature
	159b517523e05083a18f5626439017663610deeb Minor changes
	6cda554ac1af6c0fad1841b5b36018bd107d926d First import


Some of the commits in that history correspond to the tips of its branches, so it would be a good idea to show that. The ``--decorate`` option adds the reference name when a commit correspond to a reference.

::

	$geogit log --oneline -all --decoration
	adf66fe8b06bd810767de4222b387c7b2a18233f (refs/heads/branch2) Edited wrong geometries
	6f250c0c74d9cb852405818f4a5e50aa971d543c (HEAD, refs/heads/master) Merge branch1
	7aa528d3d3437ca8015f1dcfb8a61e197aa09dbe Changed unit in area field
	da1534a4aae8a1b29974c040c9fb44be426c58f8 (refs/heads/branch1) Added missing feature
	159b517523e05083a18f5626439017663610deeb Minor changes
	6cda554ac1af6c0fad1841b5b36018bd107d926d First import

If you prefer to show shortened Ids, use the ``abbrev-commit`` option 

::

	$geogit log --oneline -all --decoration --abbrev-commit
	adf66fe (refs/heads/branch2) Edited wrong geometries
	6f250c0 (HEAD, refs/heads/master) Merge branch1
	7aa528d Changed unit in area field
	da1534a (refs/heads/branch1) Added missing feature
	159b517 Minor changes
	6cda554 First import

You can see that, in both cases, the history is displayed in chronological order, so commits from several branches are mixed. If you want all commits from a branch to be displayed one after another, without mixing with commits from other branches (but also without respecting the chronological order), use the ``--topo-order`` option.

::
	$geogit log --oneline --topo-order --abbrev-commit



You can limit the number of commits by setting a fixed number of them or entering a date range. In the first case, just use the ``-n`` option followed by the number of commits, as in the following example:

::

	$geogit log -n 2 --oneline
	86562df2e4f357b05abd2f61c5db2013185f478a Merge commit 'be697a1404f1ead7d92b3e9c094627b3e047aae0'.
	7844674611811fe888852ca9418028dd900f4a8e Changed unit in area field

To set a date range, use the ``--since`` and ``--until`` commands, followed by the corresponding dates, as in the examples below:

::

	$geogit log --since yesterday --oneline
	$geogit log --since 2.months.ago --until 1.week.ago --oneline


As you can see, there is no need to use both of them, you can just use one of them and a single limit date.

If instead of dates you want to use commit references (any commit that resolves to a commit using any of the available syntaxes) as limits of the history to display, then just enter the references separated by two points (``..``), with no additional command option needed. For instance:

::

	$geogit log 6f250c0...159b517


Commits can also be filtered by author and committer, using the ``--author`` and ``--committer`` options respectively. The value after them is a regular expression that is used to filter the commits. For instance, to show just the commits made by a user named "geogituser", use the following line

::

	$geogit log --author geogituser

All the above options can be combined to filter the resulting list of commits according to several different criteria.	

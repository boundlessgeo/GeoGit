Using GeoGit with OpenStreetMap data
=====================================

GeoGit can be used to version OSM data, and also to contribute changes back once the work is done. GeoGit aims to provide all the tools needed for the versioning part of a normal OSM editing workflow, adding some of its powerful tools to give additional possibilites.

This section describes the GeoGit commands that interact with OSM and their usage.

Importing OSM data
--------------------

Just like you can import data form a shapefile or a PostGIS database into a GeoGit repository, you can also import OSM data from a file in one of the supported OSm formats (OSM XML or pbf). The ``osm import`` is the command to use for that.

The ``osm import`` command has the following syntax:

::

	geogit osm import <path_to_file>

Both OSM XML and pbf formats are supported.

Data in the specified file is imported into GeoGit and put into two trees: ``way`` and ``node``, with default feature types in both cases. The feature type keeps the data in a way that makes it possible to later recreate the OSM objects and export back to an OSM XML or pbf file, as we will see.

You can see the definition of those feature types by using the ``show`` command on both trees.

::

	$ geogit show WORK_HEAD:way
	TREE ID:  fb04b79726d7a969393308a3e40fdd47a6c7be4b
	SIZE:  5254
	NUMBER Of SUBTREES:  0
	DEFAULT FEATURE TYPE ID:  03585dd9f1ccd1555372782e6f18bf44ec5d9693

	DEFAULT FEATURE TYPE ATTRIBUTES
	--------------------------------
	changeset: <LONG>
	nodes: <STRING>
	tags: <STRING>
	timestamp: <LONG>
	user: <STRING>
	version: <INTEGER>
	visible: <BOOLEAN>
	way: <LINESTRING>


	$geogit show WORK_HEAD:node
	TREE ID:  98d0b69bab10307921b939aa8ee975e6eb669d17
	SIZE:  153503
	NUMBER Of SUBTREES:  0
	DEFAULT FEATURE TYPE ID:  f63c0dd9d76623e2af985553c94d5219a9c0e2b7

	DEFAULT FEATURE TYPE ATTRIBUTES
	--------------------------------
	changeset: <LONG>
	location: <POINT>
	tags: <STRING>
	timestamp: <LONG>
	user: <STRING>
	version: <INTEGER>
	visible: <BOOLEAN>

Here is an example of a way imported into the ``way`` tree, as described by the ```show`` command:

::

	$ geogit show WORK_HEAD:way/31347480

	ID:  d81271b7346586c95166c43feb6e91ffe7adb9d5

	ATTRIBUTES
	----------
	changeset: 14220478
	nodes: 269237860;2059114068;269237861;278905850;269237862;269237863;278904103;1300224351;269237865;345117527
	tags: highway:residential|lit:yes|name:Gielgenstra▀e
	timestamp: 1355097350000
	user: adjuva:92274
	version: 5
	visible: true
	way: LINESTRING (7.1960069 50.7399033, 7.195868 50.7399081, 7.1950788 50.739912, 7.1949262 50.7399053, 7.1942463 50.7398686, 7.1935778 50.7398262, 7.1931011 50.7398018, 7.1929987 50.7398009, 7.1925978, 50.7397889, 7.1924199 50.7397781)

As in the case of importing from a shapefile of database, the tree where data is imported is deleted before importing, so the imported data replaces the previous one. In this case, both ``way`` and ``node`` trees are deleted, even if the imported data does not contain entities of both types. To keep existing data, use the ``--add`` switch. Notice that, although existing data will not be deleted, it will be overwritten if the imported data contains entities with the same OSM id.

Downloading data from an OSM Server
------------------------------------

A different way of putting OSM data into a GeoGit repository is by connecting to a OSM endpoint that supports the OSM Overpass API. In this case, the ``osm download`` command has to be used instead of ``osm import``

The syntax of the commands is as follows:

::

	geogit osm download [<server_URL>] [--filter <filter_file>] [--bbox <S> <W> <N> <E>]

You can specify the server from which you want to get your OSM data, just entering its URL after the ``osm download`` command. By default, if no URL is provided, the ``download`` command uses ``http://overpass-api.de/api/interpreter`` as endpoint. 

To avoid downloading the full OSM planet, a filter can be used. You should write your filter in a separate text file using the Overpass Query Language. Check the `language documentation <http://wiki.openstreetmap.org/wiki/Overpass_API/Language_Guide>`_ to know more about it.

A few considerations to take into account:

- Version information is needed to parse the downloaded data. Use the verbose mode (``out meta;``) to get the version information added.

- If your filter downloads ways, it should also download the corresponding nodes. For instance, this filter will add no data to your GeoGit repo:

	::

		way
			["name"="Gielgenstraße"]
			(50.7,7.1,50.8,7.25);
		out meta;

	The following one, however, will work:

	::

		(
		  way
		    ["name"="Gielgenstraße"]
		    (50.7,7.1,50.8,7.25);
		  >;
		);
		out meta;


If the filter you want to set is just a bounding box filter, you can use the ``--bbox`` option as a practical alternative, as in the next example:

::

	$ geogit osm download --bbox 50.7 7.1 50.8 7.25

Values after the ``--bbox`` option represent South, West, North and East limits, respectively.


Unlike the case of importing from a file, which works similar to the case of importing from a shapefile or database, downloading from OSM has to be performed with a clean index and working tree, and the imported data is not just imported into the working tree, but also staged and commited. This is done to ensure that the commit actually correspond to an OSM changeset, with no further modification, so it can be later identified and used as a reference when performing other tasks agains the OSM planet, such as updating.

Updating OSM data
-----------------

If you have downloaded OSM data into your GeoGit repository using the ``download`` command, you can easily update it to get the new changes that might have been added in the central OSM planet. To do so, just run the ``osm download`` command with the ``--update`` switch and without specifying any filter file.

::

	$ geogit osm download --update

As in the case of importing, you can select a URL different to the default one, just entering it after the command.

::

	$ geogit osm download http://overpass.osm.rambler.ru/ --update

The filter that you used for the latest import will be used. In case you want to get the most recent OSM data with a different filter, you should run the ``download`` command instead as explained before, which will replace the current OSM data in the geogit repository.

The ``download`` command with the ``--update`` switch is similar to the ``pull`` command in a normal repository. It will get the latest version of the OSM data and put it in new temporary branch. That branch starts at the commit where you made your last update. From that point GeoGit will try to merge that branch with your current branch, doing it the usual way. If you have edited your OSM and your changes are not compatible with the changes introduced in the latest snapshot that you you have just downloaded, conflicts will be signaled, and you should resolve them.

As in the case of the ``pull`` command, you can tell GeoGit to perform a rebase instead of a merge, by using the ``--rebase`` switch.

Exporting to OSM formats
-------------------------

The content of a GeoGit repository can be exported in OSM XML format, much in the same way as it works for other formats such as shapefiles. The OSM export command has the following format:

::

	geogit osm export <file> [commitish]

If the file has the ``pbf`` extension, the created file will be a pbf file. Otherwise, it will be an OSM XML file.

The area to export can be restricted by using the ``--b`` option, which works just as it does in the case of the ``download`` command. Use it to define a bounding box, and only those elements intersecting the selected area will be exported.

Data exported is taken from the "way" and "node" trees, and assumed to use the corresponding default feature types. In other words, it assumes OSM data in your repository has been imported either by using the ``osm import`` or ``osm download`` commands. Data in other trees in the repository will not be imported, even if it originated from OSM data and even uses the same feature type, since there is no way for GeoGit to know about it. You will notice that, for this reason, there is no path option in the syntax of the command, since the paths from which to export data are not configurable, and GeoGit uses the default OSM paths.

By default, the data at HEAD is exported. You can export from a different snapshot by entering the commit reference after the export file path.

For instance:

::

	$ geogit export myexportedfile.pbf HEAD~3	

OSM formats should be used as a part of a normal OSM workflow, both for importing and exporting. If you plan to edit your data and create new versions in your GeoGit repository that you can later contribute back to the OSM planet, either the OSM XML format or the pbf format have to be used. Other formats will not guarantee that the relation between nodes and ways is kept, and the result of a workflow might result in a new snapshot in the GeoGit repository that cannot be later exported and contributed back to the OSM planet.

The geometry of ways is not used to export, and it is assumed to match the set of nodes that are kept in the ``nodes`` attribute. That's the reason why the OSM formats should be used instead of other formats when exporting OSM data. Using other formats can lead to unconsistent relations between nodes and ways.

In short, you should use ``osm export`` to export your OSM data, and not commands such as ``pg export`` or ``shp export``.

To be able to use a shapefile or a PostGIS database for working with OSM data, GeoGit provides additional export commands and data mapping functionalities that will be explained later in this chapter. For now, just remember that the usual export commands are not a good idea in case you want to edit and reimport your OSM data. 




Exporting differences as changesets
------------------------------------

The differences between two commits in a repository can be exported as an OSM changeset that can be used to contribute those changes to the OSM planet. To export differences as changesets, the following command has to be used:

::

	geogit osm create-changeset [commit [commit]] -f <changesets_file>

The syntax is similar to the ``diff`` command, but the output will be saved to the specified file instead of printed on the console. The two commits are optional, and allow to select the snapshots to compare, with the same meaning as the equivalent parameters in the ``diff`` command.

To export the differences between the working tree and the current HEAD, this would be the command line to use:

::

	$ geogit osm create-changeset -f changeset.xml

Only the ``node`` and ``way`` trees are compared to find the differences between the specified commits. Changes in other trees will be ignored, and no changeset entries will be created based on them.

Data mapping
-------------

Apart from importing the data in the default "node" and "way" trees, OSM data can also be imported in any given tree, and a custom schema can be used for the corresponding features. This is done using a data mapping. A data mapping is a set of rules, each of them defines the data to map into a given tree. Each mapping rule contains the following elements.

- A destination tree.
- A set of characteristics of the entities to import onto that tree, which are used as a filter over the whole OSM dataset in the Geogit repository
- A set of attributes for the feature type to use. Value of those attributes will be taken from the tags of the same name, if present.

Mappings are defined in a mapping file, using JSON syntax, as in the following example:

::

	{"rules":[{"name":"onewaystreets","filter":{"oneway":["yes"]},"fields":{"lit":"STRING","geom":"LINESTRING"}}]}

A mapping description is an array of mapping rules, each of them with the following fields:
 
 - ``name`` defines the name of the mapping, which is used as the destination tree.
 - ``filter`` is a set of tags and values, which define the entities to use for the tree. All entities which have any of the specified values for any of the given tags will be used. And empty filter will cause all entities to be used.

 The following mapping will cause all ways to be be mapped, to a feature type that just contains the geometry of the way:

 ::

 	{"rules":[{"filter":{},"fields":{"geom":"LINESTRING"},"name":"all_ways"}]}

 To get all entities that have a given tag, no matter which value the tag gas, just use an empty list for the accepted values. For instance, to get all the nodes with the tag ``power`` (can be ``power=tower``, ``power=pole``, etc.), use the following mapping:


::

 	{"rules":[{"filter":{"power":[]},"fields":{"geom":"POINT"},"name":"power"}]}



 - ``fields`` describes the attributes for the feature type, as ``field_name:field_type`` values. Valid types for the ``field_type`` are ``INTEGER, FLOAT, DOUBLE, LONG SHORT, POINT LINE, POLYGON, STRING, DATE``. Only one of the geometry types can be used for a field in a mapping rule. This defines the type of entities that will be used, so it acts as a filter as well. So, if you add a field "coordinate:POINT", it will use only those entities represented as a points. That is, it will use only nodes. LINESTRING  and POLYGON will cause only ways to be used. In both cases, all ways are used, even if they are not closed (they will be automatically closed to create the polygon). It is up to you to define the criteria for a way to be suitable for creating a polygon, such as, for instance, requiring the ``area=yes`` or "building=yes" tag/value pair.

 Apart from the fields that you add to the feature type in your mapping definition, GeoGit will always add an ``id`` field with the OSM Id of the entity. This is used to track the Id and allow for unmapping, as we will later see. In the case of ways, another field is added, ``nodes``, which contains the Id's of nodes that belong to the way. You should avoid using ``id`` or ``nodes`` as names of your fields, as that might cause problems.

.. note:: [Explain this better and in more in detail]

A mapping file can be used in three different cases.

- When importing OSM data using the ``osm import`` or ``osm download`` commands. In both cases, the ``--mapping`` option has to be used, followed by the name of the file where the mapping is found, as in the following example.

::

	$ geogit osm import fiji-latest.osm.pbf --mapping mymapping.txt

Data will be imported in the usual ``way`` and ``node`` trees with the corresponding default feature types, but also in the trees defined by the mapping, and according to the filter and feature types that it defines. 

If you do not want the imported data to be added in *raw* format in the default trees, you can use the ``--no-raw`` switch. 

::

	$ geogit osm import fiji-latest.osm.pbf --mapping mymapping.txt --no-raw

This option is only available for the ``osm import`` command, but not for the ``osm download`` command, since the *raw* data is needed to later be able to perform operations such as update.

Be aware that, when you import using the ``--no-raw`` switch, you will not be able to use OSM operations on the imported data, since GeoGit will not consider it as OSM data. When using a mapping, the mapped data is an additional version of the data that is imported in a different tree to give a more practical alternative to the *raw* one, but that data is not guaranteed to have the necessary information to be able to reconstruct OSM entities. In short, GeoGit will not track data other than the data stored in the ``way`` and ``node`` trees as OSM data, so you should not to use the ``--no-raw`` switch if you plan to do OSM-like work on the imported data.

- With already imported OSM data. If you imported OSM data without a mapping, you can apply it afterwards by using the ``osm map`` command followed by the mapping file, as in the example below.

::

	$ geogit osm map mymapping.txt

- When exporting OSM data. OSM data can be exported to OSM formats using the ``osm export`` command, and also to other formats using commands such as ``shp export`` or ``pg export``. In these two last cases, the feature type created in the destination file or database is the same one used it the ``way`` or ``node`` tree. That is, the default one used for storing the *raw* OSM data in GeoGit. Additional commands are available to export a mapped set of features.

	- `osm export-shp``. Export to a shapefile
	- `osm export-pg``. Export to a PostGIS database
	- `osm export-sl``. Export to a Spatialite database.

 .. note:: only shp and pg export currently implemented

These commands all have a syntax similar to the equivalent export commands such as ``shp export`` or ``pg export``, but without the ``--alter``, ``--defaulttype`` and ``--featuretype`` options. Instead, the ``--mapping`` option must be used to specify the file that contains the mapping to use. Also, a path cannot be specified, since the operation will always take the OSM data from the default *raw* locations at the ``way`` and ``node`` trees.

Below you can see some examples:

::

	$ geogit osm export-shp ./myexportfile.shp --mapping ./mymappingfile.json


The mapping file should contain a single rule. If the mapping contains more than one mapping rule, only the first one will be used. 

In the case of a shapefile, the destination file has to be entered. In the case of a database export, the name of the mapping is used as the name of the table to create. In both cases, the ``--overwrite`` switch has to be used if the destination file/table already exists.

Since features in a shapefiles must have a geometry, the mapping used when exporting to a shapefile must contain one, and only one, field of type ``POLYGON, LINESTRING`` or ``POINT``. In the case of exporting to a database, the rule can contain no geometry attribute at all. 

In all cases, exporting is done from the working tree.

.. note:: Maybe add an option to select a commitish to export from?

Data unmapping
--------------

Mapped OSM data can also be used to modify the original OSM data that is kept in the default ``node`` and ``way`` trees. This way, you can export your data using a mapping, modifiy that mapped data, reimport it, and then tell GeoGit to reflect those changes back in the original data, which is the one used for all OSM tasks such as generating changesets, remapping to a different feature type, etc.

To unmap the data in a tree in your repository, the ``osm unmap`` command should be used, with the following syntax:

::

	geogit osm unmap <tree_path>


If you add new entities, they will just be added to the corresponding ``way`` or ``node`` trees. In case the entity already existed, the modified version from you mapped data is merged with the information that is stored in the default location and was not mapped. Those tags that are defined for an entity (and, as such, stored in the ``way`` or ``node`` trees) but are not used to create attributes in the mapped feature type, are reused when unmapping. Let's see it with an example.

For instance, imagine that you have an OSM entity with the following tags

::

    amenity:fire_station
    name:Unnamed fire station
    phone:5555986154
    

Let's say that you have run the ``export-pg`` command to export your nodes to a postGIS database, with the following mapping

::

	 {"rules":[{"filter":{"amenity":["fire_station"]},"fields":{"geom":"POINT", "name":"STRING"},"name":"firestations"}]}

Basically, you are mapping all fire stations to a new feature type which just contains the station name and its location.


Now, in your exported data, you modified the name of the above firestation from "Unnamed fire station" to "Central fire station". After that, you imported the data to a ``fire_stations`` tree using the ``pg import`` command.

The ``firestations`` tree contains your changes, but the corresponding feature in the ``node`` tree is not updated. You can tell GeoGit to update it, by running the unmap command, as shown below.

::

	$ geogit unmap fire_stations

The corresponding feature will be updated, and will have the following tags.

::

    amenity:fire_station
    name:Central fire station
    phone:5555986154

Although the ``phone`` tag was not present in the mapped data, it will continue to appear here, since it is taken from the previous version of the feature that was stored in the ``node`` tree.

All the work done by the unmap command takes place on the working tree. That is, the mapped path ``firestations`` refers to ``WORK_HEAD:firestations``, and the unmapped data is added/replaced in ``WORK_HEAD:node`` and ``WORK_HEAD:way``.

In the case of ways, the ``nodes`` field will be recomputed based on the geometry. If the geometry has changed and new points have been added to the corresponding line of polygon, new nodes will be added accordingly.

An OSM workflow using GeoGit
-----------------------------

The following is a short exercise demostrating how GeoGit can be used as part of a workflow involving OSM data.

First, let's initialize the repository.

::

	$ geogit init

For this example, we will be working on a small area define by a bounding box. The first step is to get the data corresponding to that area. We will be using a bounding box filtering, which will retrieve all the data within the area, including both ways and nodes.

Run the following command:

::

	$ geogit osm download --bbox 40 0 40.01 0.01  


Your OSM data should now be in your GeoGit repository, and a new commit should have been made.

::

	$ geogit log
	Commit:  d972aa12d9fdf9ac4192fb81da131e77c3867acf
	Author:  volaya <volaya@opengeo.org>
	Date:    (4 minutes ago) 2013-06-03 14:37:21 +0300
	Subject: Updated OSM data

If you want to edit that data and work on it, you can export it using the ``osm export`` command.

::

	$ geogit osm export exported.xml

You can open the ``exported.xml`` file in a software such as JOSM and edit it. Once it is edited, export it back to an OSM file.

To create a new snapshot in the geogit repository with the edited data, just import the new OSM file.

::

	$ geogit osm import editedWithJosm.xml

and then add and commit it

::

	$ geogit add
	$ geogit commit -m "Edited OSM data"
	[...]
	$ geogit log
	Commit: a465736fdabc6d6b5a3289499bba695328a6b43c 	        
	Author:  volaya <volaya@opengeo.org>
	Date:    (15 seconds ago) 2013-05-21 12:37:33 +0300
	Subject: Edited OSM data

	Commit:  58b84cee8f4817b96804324e83d10c31174da695
	Author:  volaya <volaya@opengeo.org>
	Date:    (3 minutes ago) 2013-05-21 12:34:30 +0300
	Subject: Update OSM to changeset 16215593


Another way of editing your data is to export it using a mapping. Let's see how to do it.

Create a file named ``mapping.json`` in your GeoGit repository folder, with the following content:

::
	
	{"rules":[{"filter":{"power":["tower", "pole"]},"fields":{"coord":"POINT", "power":"STRING"},"name":"power"}]}

Now export the OSM data that you downloaded, using the above mapping. 

::

	$ geogit osm export-shp exported.shp --mapping mapping.json

The resulting database file can be imported into a desktop GIS such as QGIS. Here's how the attributes table of the imported layer would look like:

.. figure:: ../img/qgis_osm.png


Let's edit one of the features in the layer (don't worry, we are not going to commit the changes back to OSM, so we can modify it even if the new data is not real). Take the feature with the Id ``1399057662``, move its corresponding point to a different place and change the value of the ``power`` attribute from ``tower`` to ``pole``.

Save it to the same ``export.shp`` file and then import it back into the GeoGit repository using the following command:

::

	$ geogit shp import export.shp -d power

The imported data is now in the ``power`` tree.

::

	$ geogit show WORK_HEAD:power
	TREE ID:  cd6d05d0fe0c527a78e56ef4ec7439a494a6229c
	SIZE:  130
	NUMBER Of SUBTREES:  0
	DEFAULT FEATURE TYPE ID:  e1833b12c4fc867f10b3558b1b32c33abdd88afa

	DEFAULT FEATURE TYPE ATTRIBUTES
	--------------------------------
	id: <LONG>
	power: <STRING>
	the_geom: <POINT>

The node we have edited is not updated in the ``node`` tree, as you can see by running the following command:

::

	$ geogit show WORK_HEAD:node/1399057662
	ID:  9877ef1ed87f5e9e85a00416e681f3a0238725b9

	ATTRIBUTES
	----------
	changeset: 9020582
	location: POINT (0.0033643 40.0084599)
	tags: power:tower
	timestamp: 1313352916000
	user: Antonio Eugenio Burriel:24070
	version: 1
	visible: true
	


To update the data in the "node" tree, we can run the ``osm unmap`` command:


::

	$ geogit osm unmap power

Now the node should have been updated.

::

	$ geogit show WORK_HEAD:node/1399057662
	ID:  ff6663ccec292fb2c06dcea5ec8b539be9cb50fb

	ATTRIBUTES
	----------
	changeset: Optional.absent()
	location: POINT (0.0033307887896529 40.00889554573451)
	tags: power:pole
	timestamp: 1370271076015
	user: Optional.absent()
	version: Optional.absent()
	visible: true
	

You can now add and commit your changes.

To merge those changes (no matter which one of the above method you have used to edit the OSM data in your GeoGit repository) with the current data in the OSM planet, in case there have been changes, use the ``update`` switch.

::

	$ geogit download --update

If there are conflicts, the operation will be stopped and you should resolve them as usual. If not, the, changes will merged with the changes you just added when importing the xml file. If there are no changes since the last time you fetched data from the OSM server, no commit will be made, and the repository will not be changed by the update operation.



Finally, you can export the new changes that you have introduced, as a changeset, ready to be contributed to the OSM planet. The commits to compare depend on the workflow that you have followed. In the case above, you can get them by comparing the current HEAD with its second parent, which corresponds to the branch that was created with the changes downloaded in the update operation, in case there were changes (otherwise, there would be no merge operation, since it was not necessary).

::
	
	$ geogit create-changeset HEAD^2 HEAD -f changeset.xml

Or you can just compare your current HEAD to what you had after your first import.

::

	$ geogit create-changeset 58b84cee8f4 HEAD -f changeset.xml



IDEAS FOR FUTURE DEVELOPMENT. ISSUES
=======================================

Problems to solve / Things to consider  about OSM commands in GeoGit
---------------------------------------------------------------------

1) Updating. The current update command just re-downloads with the last filter, but is not a smart download, and downloads everything. The overpass API allows to fetch only features newer than a given data, but that cannot be used if there are deletions, since it does not report deleted elements.

The OSM API allows to download history, including deletions, but does not support filters.

Ideally, a mix of both functionalities would be needed for geogit to work optimally

2) Unmapping of ways. When a way is unmapped, its geometry is used to re-create the list of nodes. The best way would be to take the coords of the nodes and check if a node exist in each coordinate, and if so, take the node id, otherwise, add a new node. This is, however, not possible now, since it would not be efficient. GeoGit has no spatial indexing, and searching a feature by its coordinates is not an available operation.

The current implementation just retrieves the nodes that belonged to the way in the last version, and check the current geometry against them. This is fine if all new nodes are actually new, but if the way uses a node that it did not use before but that exists already, that node will not be used (since there is no way of retrieving the id of the node in that coord), and a new one in that same position is added.

3)Updating new entities. When a new node is added (whether by the user, who created it in something like JOSM, or by an unmap operation), new entities get a negative ID, as it seems customary in OSM before commiting them. Once submitted, they get a valid ID, and when later updating, the Id's will not match, so GeoGit will not replace them, leaving both versions.

This is, in fact, not a problem now, since the update operation just deletes and updates everything (see (1)), but once we get a more efficient update strategy, this problem will surface.


OSM paths
----------

..  note:: [This is just an idea, not implemented yet. Is it a good idea??]

The default paths for OSM data are ``way`` and ``node``. they should contain just OSM data imported using the corresponding GeoGit commands. To use those paths for different data and avoid problem with OSM commands, the default paths can be changed using the ``config`` command. Default paths are kept in the ``osm.nodepath`` and ``osm.waypath`` config parameters, which can be configured as shown in the example below.

::

	$ geogit config osm.nodepath osmnode
	$ geogit config osm.waypath osmway
Importing and exporting data
=============================

Unlike git or other VCS's, the working tree of GeoGit is not the filesystem. That means that, to make data available to GeoGit so it can be added to the index and commited , you cannot just copy the file to be versioned into a repository folder or create it there. It's necessary to import your data into the working tree, so it gets stored in the native format used by GeoGit. This adds an extra step if compared to VCS's oriented to work with source code file, but allows GeoGit to have a more powerful handling of spatial data.

Importing data into the working tree of a GeoGit repository is done using GeoGit commands which read a given data source and add all or part of its content to the repository. Data can also be exported from the repository into common formats that can be then used in other applications such as a desktop GIS

Currently, data can be imported/exported from/to shapefiles, PostGIS databases and SpatiaLite databases. Depending on the type of data source, a different command should be used. For this supported data sources, the general syntax is as follows:

::

	$geogit <shp|pg|spatialite> <import|export> <specific_parameters>

Importing data
---------------

In the case	of importing data, the following syntax is used

::

	$geogit <shp|pg|spatialite> import <source>[-d <destination_path>] [--no-overwrite] <specific_parameters>

Data can be imported in a given path defined using the ``-d`` option. If it is not used, the destination path will be defined automatically based on the data source. For instance, in the case of using a shapefile, the destination path is defined using the name of the shapefile.

The ``source`` argument is the filepath of the file to import, in case of using shapefiles, or the name of the table to import in case of importing from a PostGIS database.

The following command line will import all the features in a shapefile named ``roads.shp`` into the ``roads`` tree in the working tree of the GeoGit repository.

::

	$geogit shp import /home/shapefiles/roads.shp

To import into a tree named "myroads", the following command should be used:

::

	$geogit shp import /home/shapefiles/roads.shp -d myroads

If the working tree already contains a feature with the same Id under the selected destination path, it will be overwritten. To perform a safe import and only add new features without overwritting, the ``--no-overwite`` option can be used.

Features under the same path do not have to necessarily share the same feature type. In the case of shapefiles, several shapefiles containing features with different feature types can be imported to the same path. In the case of importing from a database, several tables can be imported into the same path in the GeoGit repository.

When importing from a database, additional parameters can be supplied to configure the database connection. In the case of importing from a PostGIS database, the following options are available.


* ``--host``: Machine name or IP address to connect to. Default: localhost
* ``--port``: Port number to connect to.  Default: 5432
* ``--schema``: The database schema to access.  Default: public
* ``--database``: The database to connect to.  Default: database
* ``--user``: User name.  Default: postgres
* ``--password``: Password.  Default: <no password>

When importing from a database, all tables can be imported with one single command. To do so, do not enter the name of a table as data source, but use the ``--all`` option instead, as in the following example:

::

	$geogit pg import --all 

If a destination path is supplied and the ``--all`` option used, all tables will be imported into the same path.

A listing of all available tables for a given database connection can be obtained using the ``list`` command, as shown below.

::

	$geogit pg list


Exporting data
---------------

Data can also be exported from the GeoGit repository, allowing full synchronization with external applications that cannot use the native format of the GeoGit working tree.

To export from a GeoGit repository, the following syntax is used

::

	$geogit <shp|pg|spatialite> export <path_to_export> <destination>


The ``destination`` option is the filepath in the case of exporting to a shapefile, or the table name in case of exporting to a database. In both cases, the element designated by the ``destination`` parameter should not exist. If it exists, GeoGit will not perform the export operation. If you want GeoGit to overwrite, you must explicitly tell it to do so, by using the ``--overwrite`` option.

The path to export option refers by default to the working tree. Thus, the path ``roads`` refers to the full refspec ``WORK_HEAD:roads``. Data con be exported from a given commit or a different reference, by using a full refspec instead of just a path. For instance, the following line will export the ``roads`` path from the current HEAD of the repository, to a shapefile.

::

	$geogit shp export HEAD:roads exported.shp

When exporting to a database, the same options used to configure the database connection that are available for the import operation are also available for exporting.

Notice that, as it was stated before, features with different feature types can coexist under the same path. When exporting, this will cause an exception to be thrown, since this is not allowed to happen in a shapefile or a PostGIS table. Only paths with all features sharing the same feature type can be safely imported using the corresponding export commands.








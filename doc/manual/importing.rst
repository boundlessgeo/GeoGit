Importing and exporting data
=============================

Importing data into the working tree of a GeoGit repository is done using GeoGit commands which read a given data source and add all or part of its content to the repository.

Data can also be exported from the repository into common formats that can be then used in other applications such as a desktop GIS

Currently, data can be imported/exported from/to shapefiles, PostGIS databases and SpatiaLite databases. Depending on the type of data source, a different command should be used. For this supported data sources, and the general syntax is as follows:

::

	$geogit <shp|pg|spatialite> <import|export> <specific_parameters>

Importing data
---------------

In the case	of importing data, the following syntax is used

$geogit <shp|pg|spatialite> import [-d <destination_path>] [--no-overwrite] <specific_parameters>

Data can be imported in a given path defined using the ``-d`` option. If it is not used, the destination path will be defined automatically based on the data store. For instance, in the case of using a shapefile, the destination path is defined using the name of the shapefile.

The following comand will import all the features in a shapefile named ``roads.shp`` into the ``roads`` tree in the working tree of the GeoGit repository.

::

	$goegit shp import /home/shapefiles/roads.shp

To import into a tree named "myroads", the following command should be used:

::

	$geogit shp import /home/shapefiles/roads.shp -d myroads

If the working tree already contains a feature with the same Id under the selected destination path, it will be overwritten. To perform a safe import and only add new feature without overwritting, the ``--no-overwite`` option can be used.

Features under the same path do not have to necessarily share the same feature type. In the case of shapefiles, several shapefiles containing features with different feature types can be imported to the same path. In the case of importing from a database, several tables can be imported into the same path in the GeoGit repository.

Exporting data
---------------








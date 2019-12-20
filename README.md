# compound-evolver

This web application presents a new genetic algorithm (GA) 
that aims to find the best 'druglike' compounds within a 
large combinatorial space. A genetic algorithm is an iterative, 
population based technique to decrease the amount of sampling 
necessary for finding a good solution. In this GA, the 
population consists of candidate solutions, that are comprised 
of reactants (building blocks), which make up a compound. 

## Requirements

```
Java 8 (https://www.java.com/en/download/)
Apache Tomcat 8.5 (https://tomcat.apache.org/download-80.cgi)

At least one of the following:
smina (https://sourceforge.net/projects/smina/files/), 
    AutoDockTools 1.5.6 (http://mgltools.scripps.edu/downloads) 
    and Python 2.7.16 (https://www.python.org/downloads/release/python-2716/)
moloc (http://www.moloc.ch/)

Optional:
Viper 3.5 (http://www.desertsci.com/products/viper/)
LePro (http://www.lephar.com/download.htm)
```

## Development

This is a gradle project that should be imported via the build.gradle
file. In addition, the project requires a `gradle.properties` file that
is used for the ChemAxon repository. [The ChemAxon website](https://docs.chemaxon.com/display/docs/Public+Repository)
provides a description on how to obtain such an API key.

The following setup of the `gradle.properties` file has proven to work well.

```
artifactory_user = email@email.com
artifactory_password = API-key
artifactory_contextUrl = https://hub.chemaxon.com/artifactory/libs-release
```

## Deployment

The web application requires a Tomcat server instance.
The software was developed with Tomcat version 8.5 downloadable
from [their website](https://tomcat.apache.org/download-80.cgi).
The installation process is well described in the various readme files
the software provides.

To deploy the web application build a .war file using gradle.
A good development environment is advices for this step.
By making the file `<TOMCAT_HOME>/conf/Catalina/localhost/ROOT.xml`
with the following content:

```
<Context
  docBase=".../compound_evolver.war"
  path=""
  reloadable="true"
/>
```

where `.../compound_evolver.war` is the location of the war file within
the filesystem. Starting the server is done with the `startup.sh`
or `startup.bat` scripts in the `bin` folder. After tomcat is started the
web application should be available via `<host>:<port>/app`. The readme files
tomcat provides gives a more detailed explanation on the running procedure.

It also needs some environment variables to be set, being the following:

`MOL3D_EXE`: Specifies the location of the mol3d executable, which comes with moloc

`ESPRNTO_EXE`: Specifies the location of the esprnto executable, which comes with moloc

`MCNF_EXE`: Specifies the location of the mcnf executable, which comes with moloc

`MSMAB_EXE`: Specifies the location of the msmab executable, which comes with moloc

`PL_TARGET_DIR`: Specifies the location of the pipeline output steps

`POOL_SIZE`: Specifies the number of threads to use for the energy minimization step

`SMINA_EXE`: Specifies the location of the smina executable

`MGL_PYTHON`: Specifies the python executable

`PRPR_REC_EXE`: Specifies the location of the prepare_receptor4.py script, which is included with AutoDockTools

`FINDPATHS3_EXE`: Specifies the location of the viewpaths3.py script, which is included with Scorpion/Viper

`LEPRO_EXE`: Specifies the location of the lepro executable for receptor preparation

`FIXER_EXE`: Specifies the location of the bundled get_scorp_for_conformers.py script

`PYTHON_EXE`: Specifies the location or name of the python executable
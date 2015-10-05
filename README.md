

Apache Apex (incubating)
========================

Apache Apex is a unified platform for big data stream and batch processing. Use cases include ingestion, ETL, real-time analytics, alerts and real-time actions. Apex is a Hadoop-native YARN implementation and uses HDFS by default. It simplifies development and productization of Hadoop applications by reducing time to market. Key features include Enterprise Grade Operability with Fault Tolerance,  State Management, Event Processing Guarantees, No Data Loss, In-memory Performance & Scalability and Native Window Support.

##Documentation

Please visit the [documentation section](http://apex.incubator.apache.org/docs.html). 

[Malhar](https://github.com/apache/incubator-apex-malhar) is a library of application building blocks and examples that will help you build out your first Apex application quickly.

##Contributing

This project welcomes new contributors.  If you would like to help by adding new features, enhancements or fixing bugs, here is how to do it.

You acknowledge that your submissions to this repository are made pursuant the terms of the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html) and constitute "Contributions," as defined therein, and you represent and warrant that you have the right and authority to do so.

  * Fork your own GitHub repository
  * Create a topic branch with an appropriate name
  * Write code, comments, tests in your repository
  * Create a GitHub pull request from your repository, providing as many details about your changes as possible
  * After review and acceptance one of the committers will merge the pull request.

Apex code needs to follow a certain coding style so that it is consistent with the existing code that is already in the repository. We provide code style configurations for some of the most popular IDEs to make it easy for the contributors of Apex.

  * To configure **Eclipse** follow instructions in [Eclipse README](misc/ide-templates/eclipse/README.md)
  * To configure **IntelliJ** follow instructions in [IntelliJ README](misc/ide-templates/intellij/README.md)
  * To configure **NetBeans** follow instructions in [NetBeans README](misc/ide-templates/netbeans/README.md)

When adding **new files**, please include the Apache v2.0 license header. From the top level directory:

Run `mvn license:check -Dlicense.skip=false` to check correct header formatting.
Run `mvn license:format -Dlicense.skip=false` to automatically add the header when missing.

Thanks for contributing!
 
##Building Apex

The project uses Maven for the build. Run 
```
mvn install
``` 
at the top level. You can then use the command line interface (CLI) from the build directory:
```
./engine/src/main/scripts/dtcli
```
Type help to list available commands. 

Pre-built distributions (see [README](https://www.datatorrent.com/docs/README.html)) are available from
https://www.datatorrent.com/download/

##Issue tracking

(Note that we will be moving to the Apache JIRA system soon.)

[Apex JIRA](https://malhar.atlassian.net/projects/APEX) issue tracking system is used for this project.
You can submit new issues and track the progress of existing issues at https://malhar.atlassian.net/projects/APEX.

When working with JIRA to submit pull requests, please use [smart commits](https://confluence.atlassian.com/display/AOD/Processing+JIRA+issues+with+commit+messages) feature by specifying APEX-XXXX in the commit messages.
It helps us link commits with issues being tracked for easy reference.  And example commit might look like this:

    git commit -am "APEX-1234 #comment Task completed ahead of schedule #resolve"

##License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

##Contact

Please visit http://apex.incubator.apache.org and [subscribe](http://apex.incubator.apache.org/community.html) to the mailing lists.


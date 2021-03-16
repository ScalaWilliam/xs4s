
Publishing
======
``` bash
# update version in build.sbt
# commit
# git tag v*
# git push
# cat <<EOF > ~/.sbt/sonatype_credentials
realm=Sonatype Nexus Repository Manager
host=oss.sonatype.org
user=fill in
password=fill in
EOF
$ nano ~/.sbt/sonatype_credentials
$ cat <<EOF > ~/.sbt/1.1/sonatype.sbt
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
EOF

$ export GPG_TTY=$(tty)
$ sbt ++test +publishSigned
```

Then in https://oss.sonatype.org/ log in, go to 'Staging Repositories', sort by date descending, select the latest package, click 'Close' and then 'Release'.

https://central.sonatype.org/pages/releasing-the-deployment.html

To initially set up, go to Sonatype JIRA https://issues.sonatype.org/secure/Dashboard.jspa and make something like this https://issues.sonatype.org/browse/OSSRH-13569

ScalaWilliam <https://www.scalawilliam.com/>

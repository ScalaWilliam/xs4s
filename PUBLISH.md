
Publishing
======
``` bash
$ cat <<EOF > ~/.sbt/1.0/sonatype.sbt
credentials +=
  Credentials("Sonatype Nexus Repository Manager",
              "oss.sonatype.org",
              "USERNAME",
              "PASSWORD")
EOF

$ export GPG_TTY=$(tty)
$ sbt +core/publishSigned
```

Then in https://oss.sonatype.org/ log in, go to 'Staging Repositories', sort by date descending, select the latest package, click 'Close' and then 'Release'.

Then go to Sonatype JIRA https://issues.sonatype.org/secure/Dashboard.jspa

ScalaWilliam <https://www.scalawilliam.com/>

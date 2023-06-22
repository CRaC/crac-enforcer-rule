# Custom CRaC rule for Maven Enforcer

Some libraries require modifications in order to work well with [OpenJDK CRaC](https://wiki.openjdk.org/display/crac).
Until these are integrated to the main project, or when these are not available for older version we release artifacts
fixed for CRaC under `io.github.crac.<project.group.id>`.

The Maven Enforcer rule helps you identify which artifacts should be replaced by CRaC'ed versions.

## Usage

Please make sure you're using Maven Enforcer version 3.3.0 or higher.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.3.0</version>
    <dependencies>
        <dependency>
            <groupId>io.github.crac</groupId>
            <artifactId>crac-enforcer-rule</artifactId>
            <version><!-- VERSION --></version>
        </dependency>
    </dependencies>
    <configuration>
        <rules>
            <cracDependencies>
                <!-- Some artifacts that have been tested in our app can be allow-listed -->
                <allowedArtifacts>
                    <artifact>com.example:some-artifact:1.2.3</artifact>
                </allowedArtifacts>
            </cracDependencies>
        </rules>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>enforce</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

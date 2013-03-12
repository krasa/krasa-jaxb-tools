Actual version:
----------------
```xml
<dependency>
    <groupId>com.github.krasa</groupId>
    <artifactId>krasa-jaxb-tools</artifactId>
    <version>0.3</version>
</dependency>
```


Contains:
----------------
* plugin for replacing primitives **-XReplacePrimitives**
* plugin for generation of Bean Validation Annotations (JSR-303) **-XJsr303Annotations**

---- 

XJsr303Annotations
----------------
Generates:
* @Valid annotation for objects defined in schema: -XJsr303Annotations:targetNamespace=http://www.foo.com/bar
* @NotNull annotation for objects that has a MinOccur value >= 1 or for attributes with required use
* @Size for lists that have minOccurs > 1
* @Size if there is a maxLength or minLength restriction
* @DecimalMax for maxInclusive restriction
* @DecimalMin for minInclusive restriction
* @DecimalMax for maxExclusive restriction, enable new parameter (inclusive=false) with: -XJsr303Annotations:JSR_349=true
* @DecimalMin for minExclusive restriction, enable new parameter (inclusive=false) with: -XJsr303Annotations:JSR_349=true
* @Digits if there is a totalDigits or fractionDigits restriction.
* @Pattern if there is a Pattern restriction


XReplacePrimitives
----------------
replaces following types of fields, setters and getters:
* int to Integer
* long to Long
* boolean to Boolean

---- 

Usage:
----------------

```java
<plugin>
    <groupId>org.apache.cxf</groupId>
    <artifactId>cxf-codegen-plugin</artifactId>
    <version>${cxf-codegen-plugin.version}</version>
    <executions>
        <execution>
            <id>wsdl2java</id>
            <phase>generate-sources</phase>
            <configuration>
                <wsdlOptions>
                    <wsdlOption>
                        <wsdl>src/main/resources/wsdl/...</wsdl>
                        <extraargs>
                            ...
                            <extraarg>-xjc-XJsr303Annotations</extraarg>
                            <extraarg>-xjc-XJsr303Annotations:targetNamespace=http://www.foo.com/bar</extraarg>
                        </extraargs>
                    </wsdlOption>
                </wsdlOptions>
            </configuration>
            <goals>
                <goal>wsdl2java</goal>
            </goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>com.github.krasa</groupId>
            <artifactId>krasa-jaxb-tools</artifactId>
            <version>0.1.0-SNAPSHOT</version>
        </dependency>
        ...
    </dependencies>
</plugin>
```

```java
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <forceRegenerate>true</forceRegenerate>
                <schemas>
                    <schema>
                        <fileset>
                            <directory>${basedir}/src/main/resources/wsdl</directory>
                            <includes>
                                <include>*.*</include>
                            </includes>
                            <excludes>
                                <exclude>*.xs</exclude>
                            </excludes>
                        </fileset>
                    </schema>
                </schemas>
                <extension>true</extension>
                <args>
                    <arg>-XJsr303Annotations</arg>
                    <arg>-XJsr303Annotations:targetNamespace=http://www.foo.com/bar</arg>
                </args>
                <plugins>
                    <plugin>
                        <groupId>com.github.krasa</groupId>
                        <artifactId>krasa-jaxb-tools</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    </plugin>
                </plugins>
            </configuration>
        </execution>
    </executions>
</plugin>
```

```java
<plugin>
    <groupId>org.apache.cxf</groupId>
    <artifactId>cxf-xjc-plugin</artifactId>
    <version>2.6.0</version>
    <configuration>
        <sourceRoot>${basedir}/src/generated/</sourceRoot>
        <xsdOptions>
            <xsdOption>
                <extension>true</extension>
                <xsd>src/main/resources/a.xsd</xsd>
                <packagename>foo</packagename>
                <extensionArgs>
                    <extensionArg>-XJsr303Annotations</extensionArg>
                    <extensionArg>-XJsr303Annotations:targetNamespace=http://www.foo.com/bar</extensionArg>
                </extensionArgs>
            </xsdOption>
        </xsdOptions>
        <extensions>
            <extension>com.github.krasa:krasa-jaxb-tools:0.1.0-SNAPSHOT</extension>
        </extensions>
    </configuration>
    <executions>
        <execution>
            <id>generate-sources</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>xsdtojava</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

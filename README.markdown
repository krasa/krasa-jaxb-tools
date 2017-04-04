>NOTE: I no longer use this plugin so I won't make any fixes, but if you make a pull request, I will merge it and release it.

Contains:
----------------
* plugin for replacing primitives **-XReplacePrimitives** (e.g. int -> Integer)
* plugin for generation of Bean Validation Annotations (JSR-303) **-XJsr303Annotations**

---- 

Actual Release:
----------------
```xml
<dependency>
    <groupId>com.github.krasa</groupId>
    <artifactId>krasa-jaxb-tools</artifactId>
    <version>1.5</version>
</dependency>
```
Snapshot:
----------------
```xml
<dependency>
    <groupId>com.github.krasa</groupId>
    <artifactId>krasa-jaxb-tools</artifactId>
    <version>1.3-SNAPSHOT</version>
</dependency>

<repository>
    <id>snapshots-repo</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

---- 
XJsr303Annotations
----------------
Generates:
* @Valid annotation for all complex types, can be further restricted to generate only for types from defined schema: -XJsr303Annotations:targetNamespace=http://www.foo.com/bar
* @NotNull annotation for objects that has a MinOccur value >= 1 or for attributes with required use
* @Size for lists that have minOccurs > 1
* @Size if there is a maxLength or minLength or length restriction
* @DecimalMax for maxInclusive restriction
* @DecimalMin for minInclusive restriction
* @DecimalMax for maxExclusive restriction, enable new parameter (inclusive=false) with: -XJsr303Annotations:JSR_349=true
* @DecimalMin for minExclusive restriction, enable new parameter (inclusive=false) with: -XJsr303Annotations:JSR_349=true
* @Digits if there is a totalDigits or fractionDigits restriction.
* @Pattern if there is a Pattern restriction


----------------

@NotNull's default validation message is not always helpful, so it can be customized with **-XJsr303Annotations:notNullAnnotationsCustomMessages=OPTION** where **OPTION** is one of the following:
* `false` (default: no custom message -- not useful)
* `true` (message is present but equivalent to the default: **"{javax.validation.constraints.NotNull.message}"** -- not useful)
* `FieldName` (field name is prefixed to the default message: **"field {javax....message}"**)
* `ClassName` (class and field name are prefixed to the default message: **"Class.field {javax....message}"**)
* `other-non-empty-text` (arbitrary message, with substitutable, case-sensitive parameters `{ClassName}` and `{FieldName}`: **"Class {ClassName} field {FieldName} non-null"**)

---- 
XReplacePrimitives
----------------
* replaces primitive types by Objects
* WARNING: must be defined before XhashCode or Xequals

---- 
Example project:
----------------
https://github.com/krasa/krasa-jaxb-tools-example

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
							<!--optional-->
                            <extraarg>-xjc-XJsr303Annotations:targetNamespace=http://www.foo.com/bar</extraarg>
                         	<!--optional, this is default values-->
                            <extraarg>-xjc-XJsr303Annotations:generateNotNullAnnotations=true</extraarg>
                         	<!--optional, default is false, possible values are true, FieldName, ClassName, or an actual message -->
                            <extraarg>-xjc-XJsr303Annotations:notNullAnnotationsCustomMessages=false</extraarg>
                            <extraarg>-xjc-XJsr303Annotations:JSR_349=false</extraarg>
                            <extraarg>-xjc-XJsr303Annotations:verbose=false</extraarg>
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
            <version>${krasa-jaxb-tools.version}</version>
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
                </args>
                <plugins>
                    <plugin>
                        <groupId>com.github.krasa</groupId>
                        <artifactId>krasa-jaxb-tools</artifactId>
                        <version>${krasa-jaxb-tools.version}</version>
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
            <extension>com.github.krasa:krasa-jaxb-tools:${krasa-jaxb-tools.version}</extension>
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

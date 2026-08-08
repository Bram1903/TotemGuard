package totemguard.java

plugins {
    id("totemguard.java.standard")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.addAll(
        listOf("-parameters", "-g", "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:removal")
    )
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
}

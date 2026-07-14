package totemguard.java

plugins {
    id("totemguard.java.standard")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

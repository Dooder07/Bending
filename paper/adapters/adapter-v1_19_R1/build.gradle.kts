dependencies {
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")
}
tasks.shadowJar {
    dependencies {
        relocate("me.moros.bending.common.adapter", "me.moros.bending.paper.adapter.v1_19_R1")
    }
}

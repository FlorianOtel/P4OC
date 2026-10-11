package dev.blazelight.p4oc.domain.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceAliasGeneratorTest {
    @Test
    fun `unique issue examples use their final component`() {
        val weather = path("C:\\Users\\dev\\Projects\\apps\\weather-dashboard")
        val analyzer = path("C:\\Users\\dev\\Projects\\tools\\log-analyzer")

        assertEquals(
            mapOf(weather.compareKey to "weather-dashboard", analyzer.compareKey to "log-analyzer"),
            WorkspaceAliasGenerator.generate(listOf(weather, analyzer)),
        )
    }

    @Test
    fun `only colliding storefront directories expand`() {
        val storefront = path("C:\\Projects\\Storefront\\frontend")
        val admin = path("C:\\Projects\\AdminPortal\\frontend")
        val backend = path("C:\\Projects\\AdminPortal\\backend")

        assertEquals(
            mapOf(
                storefront.compareKey to "Storefront / frontend",
                admin.compareKey to "AdminPortal / frontend",
                backend.compareKey to "backend",
            ),
            WorkspaceAliasGenerator.generate(listOf(storefront, admin, backend)),
        )
    }

    @Test
    fun `deeper issue collisions expand through every shared suffix component`() {
        val clientA = path("C:\\Projects\\ClientA\\web\\frontend")
        val clientB = path("C:\\Projects\\ClientB\\web\\frontend")

        assertEquals(
            mapOf(
                clientA.compareKey to "ClientA / web / frontend",
                clientB.compareKey to "ClientB / web / frontend",
            ),
            WorkspaceAliasGenerator.generate(listOf(clientA, clientB)),
        )
    }

    @Test
    fun `shorter path runs out of components while longer collision expands`() {
        val longPath = path("/a/b")
        val shortPath = path("/b")

        assertEquals(
            mapOf(longPath.compareKey to "a / b", shortPath.compareKey to "b"),
            WorkspaceAliasGenerator.generate(listOf(longPath, shortPath)),
        )
    }

    @Test
    fun `colliding paths expand only the longer suffix when the shorter is unique`() {
        val longer = path("/x/a/b")
        val shorter = path("/a/b")

        assertEquals(
            mapOf(longer.compareKey to "x / a / b", shorter.compareKey to "a / b"),
            WorkspaceAliasGenerator.generate(listOf(longer, shorter)),
        )
    }

    @Test
    fun `root-only paths use their canonical root`() {
        val posixRoot = path("/")
        val driveRoot = path("C:\\")

        assertEquals(
            mapOf(posixRoot.compareKey to "/", driveRoot.compareKey to "C:\\"),
            WorkspaceAliasGenerator.generate(listOf(posixRoot, driveRoot)),
        )
    }

    @Test
    fun `posix final components remain case sensitive`() {
        val upper = path("/x/App")
        val lower = path("/y/app")

        assertEquals(
            mapOf(upper.compareKey to "App", lower.compareKey to "app"),
            WorkspaceAliasGenerator.generate(listOf(upper, lower)),
        )
    }

    @Test
    fun `windows collisions compare components without losing original case`() {
        val upper = path("C:\\x\\App")
        val lower = path("C:\\y\\app")

        assertEquals(
            mapOf(upper.compareKey to "x / App", lower.compareKey to "y / app"),
            WorkspaceAliasGenerator.generate(listOf(upper, lower)),
        )
    }

    @Test
    fun `identical component lists on different drives use full paths`() {
        val driveC = path("C:\\x")
        val driveD = path("D:\\x")

        assertEquals(
            mapOf(driveC.compareKey to "C:\\x", driveD.compareKey to "D:\\x"),
            WorkspaceAliasGenerator.generate(listOf(driveC, driveD)),
        )
    }

    @Test
    fun `paths with equal compare keys collapse to one result`() {
        val first = path("C:\\x\\App")
        val sameDirectory = path("c:/X/app")
        val generated = WorkspaceAliasGenerator.generate(listOf(first, sameDirectory))

        assertEquals(1, generated.size)
        assertEquals("App", generated[first.compareKey])
    }

    @Test
    fun `generated aliases do not depend on input order`() {
        val paths = listOf(
            path("C:\\Projects\\Storefront\\frontend"),
            path("C:\\Projects\\AdminPortal\\frontend"),
            path("C:\\Projects\\AdminPortal\\backend"),
            path("C:\\Projects\\ClientA\\web\\frontend"),
            path("C:\\Projects\\ClientB\\web\\frontend"),
        )

        assertEquals(
            WorkspaceAliasGenerator.generate(paths),
            WorkspaceAliasGenerator.generate(paths.reversed()),
        )
    }

    private fun path(input: String): ServerPath =
        (ServerPath.parseUserInput(input) as ServerPathParse.Valid).path
}

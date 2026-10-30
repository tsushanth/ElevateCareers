package com.kreativekoala.elevatecareers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decodes a trimmed copy of a real https://elevate-careers-api.fly.dev/jobs payload
 * (src/test/resources/jobs_fly_sample.json) through the same code path the app uses.
 */
class JobsParserTest {

    private fun fixture(): String =
        javaClass.classLoader!!.getResource("jobs_fly_sample.json")!!.readText()

    @Test
    fun decodesRealFlyPayload() {
        val jobs = JobsParser.parseList(fixture())
        assertEquals(3, jobs.size)

        val first = jobs[0]
        assertEquals("568730", first.id)
        assertEquals("Hard Rock Digital", first.companyName)
        assertEquals("full_time", first.employmentType)
        assertEquals(listOf("Hollywood"), first.cities)
        assertEquals(listOf("United States"), first.countries)
        assertTrue(first.locations.isEmpty())
        assertEquals("Hollywood, United States", first.getLocationDisplay())
    }

    @Test
    fun salaryStringsAreParsedAsNumbers() {
        val job = JobsParser.parseList(fixture())[1]
        assertEquals(190900, job.salaryMin)
        assertEquals("USD", job.salaryCurrency)
        assertEquals("\$190K - \$334K/yr", job.getSalaryRange())
    }

    @Test
    fun missingCitiesFallBackToCountryAndNeverCrash() {
        val job = JobsParser.parseList(fixture())[2]
        assertNull(job.cities)
        assertNull(job.employmentType)
        assertNull(job.salaryMin)
        assertEquals("United States", job.getLocationDisplay())
    }

    @Test
    fun multiCityStringShowsFirstCityOnly() {
        val body = """{"jobs":[{"id":"1","title":"t","company_name":"c","apply_url":"u",
            "cities":["Austin; New York City; San Francisco"],"countries":["United States"]}]}"""
        assertEquals("Austin, United States", JobsParser.parseList(body).single().getLocationDisplay())
    }

    @Test
    fun remoteJobWithNoLocationShowsRemote() {
        val body = """{"jobs":[{"id":"1","title":"t","company_name":"c","apply_url":"u","remote":true,
            "cities":null,"countries":null}]}"""
        assertEquals("Remote", JobsParser.parseList(body).single().getLocationDisplay())
    }

    @Test
    fun oneMalformedJobDoesNotBreakTheList() {
        val body = """{"jobs":[
            {"id":"1","title":"ok","company_name":"c","apply_url":"u"},
            {"id":"2","title":"no company or url"},
            {"id":"3","title":"also ok","company_name":"c","apply_url":"u","salary_min":"not a number","skills":null,"locations":null}
        ],"count":3}"""
        val jobs = JobsParser.parseList(body)
        assertEquals(listOf("1", "3"), jobs.map { it.id })
        assertNull(jobs[1].salaryMin)
        assertTrue(jobs[1].skills.isEmpty())
    }

    @Test
    fun emptyAndUnexpectedBodiesGiveEmptyList() {
        assertTrue(JobsParser.parseList("""{"jobs":[],"count":0}""").isEmpty())
        assertTrue(JobsParser.parseList("""{"error":"nope"}""").isEmpty())
    }

    @Test(expected = Exception::class)
    fun nonJsonBodyIsAnError() {
        // e.g. an HTML 404/502 page: surfaces as a failed request, which the UI turns into the retry state.
        JobsParser.parseList("<html>Not Found</html>")
    }

    @Test
    fun jobSurvivesTheNavigationRoundTrip() {
        // MainActivity passes the selected job to the next screen as JSON using the default Json.
        val original = JobsParser.parseList(fixture())[1]
        val restored = Json.decodeFromString<Job>(Json.encodeToString(original))
        assertEquals(original, restored)
    }

    @Test
    fun jobDetailPayloadWithNullSkillsDecodes() {
        // /jobs/{id} returns skills:null and locations with an extra "remote" key.
        val body = """{"id":"568730","title":"t","company_name":"c","apply_url":"u","salary_min":null,
            "skills":null,"locations":[{"city":"Hollywood","region":"Florida","country":"United States","remote":false}]}"""
        val job = JobsParser.parseJob(body)!!
        assertEquals("Hollywood, Florida, United States", job.getLocationDisplay())
        assertTrue(job.skills.isEmpty())
    }

    @Test
    fun jobsApiPointsAtFly() {
        assertEquals("https://elevate-careers-api.fly.dev", ApiConfig.JOBS_API_BASE_URL)
    }
}

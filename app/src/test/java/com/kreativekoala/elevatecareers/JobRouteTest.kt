package com.kreativekoala.elevatecareers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

class JobRouteTest {
    private fun job(title: String, company: String = "Acme") = Job(
        id = "42", title = title, companyName = company, applyUrl = "https://example.com/apply?a=1&b=2%203",
        descriptionExcerpt = "100% remote, C++ and A+ welcome. {braces} \"quotes\" / slash # hash ? query",
    )

    @Test fun roundTripsTextThatBrokeTheOldDoubleDecode() {
        // "%+o" is the exact fragment from the production crash report (URLDecoder: Illegal hex characters in escape (%) pattern : %+o)
        for (title in listOf("100% remote engineer", "Engineer %+o level", "C++ / C# developer", "A+ player", "Sr. Dev (L5) \u2013 caf\u00e9 \u65e5\u672c\u8a9e", "50%", "%", "+", "a b+c%20d")) {
            val j = job(title)
            assertEquals(title, j, JobRoute.decode(JobRoute.encode(j)))
        }
    }

    @Test fun encodedRouteArgumentIsOnlyLowercaseHex() {
        val arg = JobRoute.encode(job("100% {x} + \u00e9 \u65e5\u672c"))
        assertTrue(arg, arg.matches(Regex("[0-9a-f]+")))
    }

    @Test fun unreadableArgumentsReturnNullInsteadOfThrowing() {
        for (bad in listOf(null, "", "z", "zz", "abc", "6162", "7b7d", "%7B%22id%22", "{\"id\":\"1\"}")) assertNull(bad, JobRoute.decode(bad))
    }

    @Test fun anOldStyleUrlEncodedArgumentIsRejectedNotCrashed() {
        val old = URLEncoder.encode(Json.encodeToString(job("100% remote")), "UTF-8")
        assertNull(JobRoute.decode(old))
    }

    @Test fun decodedJobKeepsEveryField() {
        val j = job("Hello").copy(remote = true, skills = listOf("kotlin", "c++"), promoted = true)
        val back = JobRoute.decode(JobRoute.encode(j))
        assertNotNull(back)
        assertEquals(j, back)
    }
}

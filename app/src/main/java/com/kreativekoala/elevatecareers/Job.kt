package com.kreativekoala.elevatecareers

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.Duration

@Parcelize
@Serializable
data class Job(
    val id: String,
    val title: String,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("apply_url")
    val applyUrl: String,
    val remote: Boolean = false,
    @SerialName("employment_type")
    val employmentType: String? = null,
    @SerialName("salary_min")
    val salaryMin: Int? = null,
    @SerialName("salary_max")
    val salaryMax: Int? = null,
    @SerialName("salary_currency")
    val salaryCurrency: String? = null,
    @SerialName("posted_at")
    val postedAt: String? = null,
    @SerialName("description_excerpt")
    val descriptionExcerpt: String? = null,
    val locations: List<Location> = emptyList(),
    val skills: List<String> = emptyList(),
    val promoted: Boolean = false
) : Parcelable

@Parcelize
@Serializable
data class Location(
    val city: String? = null,
    val region: String? = null,
    val country: String? = null
) : Parcelable {
    fun toDisplayString(): String {
        return listOfNotNull(city, region, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }
}

@Serializable
data class JobsResponse(
    val jobs: List<Job>,
    val count: Int,
    val page: Int? = null,
    val limit: Int? = null
)

fun Job.getSalaryRange(): String? {
    if (salaryMin != null && salaryMax != null) {
        val currency = if (salaryCurrency == "USD") "$" else salaryCurrency ?: ""
        val min = "${currency}${salaryMin / 1000}K"
        val max = "${currency}${salaryMax / 1000}K"
        return "$min - $max/yr"
    }
    return null
}

fun Job.getLocationDisplay(): String {
    return when {
        locations.isNotEmpty() -> locations.first().toDisplayString()
        remote -> "Remote"
        else -> "Location not specified"
    }
}

fun Job.getTimeAgo(): String {
    if (postedAt == null) return "Recently posted"

    return try {
        val posted = Instant.parse(postedAt)
        val now = Instant.now()
        val days = Duration.between(posted, now).toDays()

        when {
            days < 1 -> "Posted today"
            days == 1L -> "Posted 1 day ago"
            days < 7 -> "Posted $days days ago"
            days < 14 -> "Posted 1 week ago"
            days < 30 -> "Posted ${days / 7} weeks ago"
            else -> "Posted ${days / 30} months ago"
        }
    } catch (e: Exception) {
        "Recently posted"
    }
}

fun Job.getCompanyLogoUrl(): String {
    return "https://ui-avatars.com/api/?name=${companyName}&size=200&background=random&bold=true"
}
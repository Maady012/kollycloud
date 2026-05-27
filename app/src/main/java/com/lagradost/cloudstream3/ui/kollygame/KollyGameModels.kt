package com.lagradost.cloudstream3.ui.kollygame

import org.json.JSONObject
import java.util.Locale

data class TmdbMovie(
    val id: Long,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double?,
    val genreIds: List<Long>? = null
) {
    val fullPosterUrl: String?
        get() = when {
            posterPath.isNullOrBlank() -> null
            posterPath.startsWith("http") -> posterPath
            else -> "https://image.tmdb.org/t/p/w500$posterPath"
        }

    val fullBackdropUrl: String?
        get() = when {
            backdropPath.isNullOrBlank() -> null
            backdropPath.startsWith("http") -> backdropPath
            else -> "https://image.tmdb.org/t/p/w780$backdropPath"
        }

    companion object {
        fun fromJson(obj: JSONObject): TmdbMovie {
            val genreIdsList = mutableListOf<Long>()
            val genreIdsArray = obj.optJSONArray("genre_ids")
            if (genreIdsArray != null) {
                for (i in 0 until genreIdsArray.length()) {
                    genreIdsList.add(genreIdsArray.optLong(i))
                }
            }

            return TmdbMovie(
                id = obj.optLong("id"),
                title = obj.optString("title").ifBlank { obj.optString("original_title") },
                originalTitle = obj.optString("original_title").takeIf { it.isNotBlank() },
                overview = obj.optString("overview").takeIf { it.isNotBlank() },
                posterPath = obj.optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
                backdropPath = obj.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" },
                releaseDate = obj.optString("release_date").takeIf { it.isNotBlank() },
                voteAverage = obj.optDouble("vote_average", 0.0),
                genreIds = genreIdsList
            )
        }
    }
}

data class TmdbCreditsResponse(
    val id: Long,
    val cast: List<TmdbCastMember>,
    val crew: List<TmdbCrewMember>
) {
    companion object {
        fun fromJson(obj: JSONObject): TmdbCreditsResponse {
            val castArray = obj.optJSONArray("cast")
            val castList = mutableListOf<TmdbCastMember>()
            if (castArray != null) {
                for (i in 0 until castArray.length()) {
                    castList.add(TmdbCastMember.fromJson(castArray.getJSONObject(i)))
                }
            }

            val crewArray = obj.optJSONArray("crew")
            val crewList = mutableListOf<TmdbCrewMember>()
            if (crewArray != null) {
                for (i in 0 until crewArray.length()) {
                    crewList.add(TmdbCrewMember.fromJson(crewArray.getJSONObject(i)))
                }
            }

            return TmdbCreditsResponse(
                id = obj.optLong("id"),
                cast = castList,
                crew = crewList
            )
        }
    }
}

data class TmdbCastMember(
    val id: Long,
    val name: String,
    val character: String?,
    val profilePath: String?
) {
    val fullProfileUrl: String?
        get() = when {
            profilePath.isNullOrBlank() -> null
            profilePath.startsWith("http") -> profilePath
            else -> "https://image.tmdb.org/t/p/w185$profilePath"
        }

    companion object {
        fun fromJson(obj: JSONObject): TmdbCastMember {
            return TmdbCastMember(
                id = obj.optLong("id"),
                name = obj.optString("name"),
                character = obj.optString("character").takeIf { it.isNotBlank() },
                profilePath = obj.optString("profile_path").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }
}

data class TmdbCrewMember(
    val id: Long,
    val name: String,
    val job: String,
    val department: String?,
    val profilePath: String?
) {
    val fullProfileUrl: String?
        get() = when {
            profilePath.isNullOrBlank() -> null
            profilePath.startsWith("http") -> profilePath
            else -> "https://image.tmdb.org/t/p/w185$profilePath"
        }

    companion object {
        fun fromJson(obj: JSONObject): TmdbCrewMember {
            return TmdbCrewMember(
                id = obj.optLong("id"),
                name = obj.optString("name"),
                job = obj.optString("job"),
                department = obj.optString("department").takeIf { it.isNotBlank() },
                profilePath = obj.optString("profile_path").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }
}

data class TmdbPersonResponse(
    val id: Long,
    val name: String,
    val biography: String?,
    val profilePath: String?,
    val birthday: String?,
    val placeOfBirth: String?,
    val knownForDepartment: String?
) {
    val fullProfileUrl: String?
        get() = when {
            profilePath.isNullOrBlank() -> null
            profilePath.startsWith("http") -> profilePath
            else -> "https://image.tmdb.org/t/p/w185$profilePath"
        }

    companion object {
        fun fromJson(obj: JSONObject): TmdbPersonResponse {
            return TmdbPersonResponse(
                id = obj.optLong("id"),
                name = obj.optString("name"),
                biography = obj.optString("biography").takeIf { it.isNotBlank() },
                profilePath = obj.optString("profile_path").takeIf { it.isNotBlank() && it != "null" },
                birthday = obj.optString("birthday").takeIf { it.isNotBlank() },
                placeOfBirth = obj.optString("place_of_birth").takeIf { it.isNotBlank() },
                knownForDepartment = obj.optString("known_for_department").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class TmdbPersonMovieCreditsResponse(
    val cast: List<TmdbPersonCastCredit>,
    val crew: List<TmdbPersonCrewCredit>
) {
    companion object {
        fun fromJson(obj: JSONObject): TmdbPersonMovieCreditsResponse {
            val castArray = obj.optJSONArray("cast")
            val castList = mutableListOf<TmdbPersonCastCredit>()
            if (castArray != null) {
                for (i in 0 until castArray.length()) {
                    castList.add(TmdbPersonCastCredit.fromJson(castArray.getJSONObject(i)))
                }
            }

            val crewArray = obj.optJSONArray("crew")
            val crewList = mutableListOf<TmdbPersonCrewCredit>()
            if (crewArray != null) {
                for (i in 0 until crewArray.length()) {
                    crewList.add(TmdbPersonCrewCredit.fromJson(crewArray.getJSONObject(i)))
                }
            }

            return TmdbPersonMovieCreditsResponse(
                cast = castList,
                crew = crewList
            )
        }
    }
}

data class TmdbPersonCastCredit(
    val id: Long,
    val title: String,
    val character: String?,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double?
) {
    val fullPosterUrl: String?
        get() = when {
            posterPath.isNullOrBlank() -> null
            posterPath.startsWith("http") -> posterPath
            else -> "https://image.tmdb.org/t/p/w185$posterPath"
        }

    companion object {
        fun fromJson(obj: JSONObject): TmdbPersonCastCredit {
            return TmdbPersonCastCredit(
                id = obj.optLong("id"),
                title = obj.optString("title").ifBlank { obj.optString("original_title") },
                character = obj.optString("character").takeIf { it.isNotBlank() },
                posterPath = obj.optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
                releaseDate = obj.optString("release_date").takeIf { it.isNotBlank() },
                voteAverage = obj.optDouble("vote_average", 0.0)
            )
        }
    }
}

data class TmdbPersonCrewCredit(
    val id: Long,
    val title: String,
    val job: String,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double?
) {
    val fullPosterUrl: String?
        get() = when {
            posterPath.isNullOrBlank() -> null
            posterPath.startsWith("http") -> posterPath
            else -> "https://image.tmdb.org/t/p/w185$posterPath"
        }

    companion object {
        fun fromJson(obj: JSONObject): TmdbPersonCrewCredit {
            return TmdbPersonCrewCredit(
                id = obj.optLong("id"),
                title = obj.optString("title").ifBlank { obj.optString("original_title") },
                job = obj.optString("job"),
                posterPath = obj.optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
                releaseDate = obj.optString("release_date").takeIf { it.isNotBlank() },
                voteAverage = obj.optDouble("vote_average", 0.0)
            )
        }
    }
}

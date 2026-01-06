package ru.yarsu

object AuthConstants {
    const val MIN_USERNAME_LENGTH = 3
    const val MIN_PASSWORD_LENGTH = 6
}

object SessionConstants {
    const val SESSION_MAX_AGE_SECONDS = 604800L // 7 days (7 * 24 * 60 * 60)
}

object DefaultValues {
    const val DEFAULT_USER_ID = "12345"
}

object DatabaseConstants {
    const val FIRST_PARAMETER_INDEX = 1
    const val SECOND_PARAMETER_INDEX = 2
    const val THIRD_PARAMETER_INDEX = 3
    const val FOURTH_PARAMETER_INDEX = 4
}

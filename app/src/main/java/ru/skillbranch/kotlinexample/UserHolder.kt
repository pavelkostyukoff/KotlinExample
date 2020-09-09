package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName, email = email, password = password).also { user ->
        if (map.containsKey(user.login.trim()))
            throw IllegalArgumentException("A user with this email already exists")
        map[user.login.trim()] = user
    }

    fun loginUser(login: String, password: String): String? =
        map[login.trim()]?.let {
            if (it.checkPassword(password)) it.userInfo
            else null
        }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        return User.makeUser(fullName, phone = rawPhone)
            .also {
                val pattern = "^[+]*[0-9]?[ ]?[(]?[0-9]{1,4}[)]?[ ]?[0-9]{3}[- ]?[0-9]{2}[- ]?[0-9]{2}\$".toRegex()
                if (pattern.matches(rawPhone)) {
                    if (map.containsKey(it.login.trim())) {
                        throw java.lang.IllegalArgumentException("a user with phone already exist")
                    } else {
                        throw IllegalArgumentException("Enter a valid phone number , starting with a + and containing digits")
                    }
                }
            }
    }

    fun requestAccessCode(login: String) {
        val user = map[login.trim()] ?: map[login.replace("[^+\\d]".toRegex(), "")]
        user?.newAuthoriationCode()
    }
}
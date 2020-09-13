package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
        //todo test
    private var salt: String? = null
    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    var userInfo: String
        get() {
            TODO()
        }

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private var phone: String? = null
        set(value) {
            field = value?.replace("""[^+\d]""".toRegex(), "")
        }

    private var _login: String? = null

    internal var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    //for Email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ) :
            this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("secondary mail constructor")
        passwordHash = encrypt(password)
    }

    //for Phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String?
    ) :
            this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        println("Phone passwordHash is $passwordHash")
        accessCode = code
        //todo отправвка смс для входа 4 символа
        sendAccessCodeToUser(rawPhone, code)
    }

    //todo блок инициаизации
    init {
        println("First init block, primary constructor was called")
        check(firstName.isNotBlank()) { "Firstname must not be blank" }//todo имя не пустое
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) { "Firstname must not be blank" } //todo проверка emil rawPhone
        phone = rawPhone
        login = email ?: phone!! //todo или mail или телефон

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    //todo функция отправляет смс челу своими сервисами
    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println("..... sending access code: $code on $phone")
    }

    //todo верификация паспорта
    fun checkPassword(pass: String) = encrypt(pass) == passwordHash.also {
        println("Check pass encrypt")
    }

    fun changePassword(oldPassword: String, newPass: String) {
        if (checkPassword(oldPassword)) {
            passwordHash = encrypt(newPass)
            if (!accessCode.isNullOrEmpty()) accessCode = newPass
            println("Chenge pass")

        } else throw IllegalArgumentException("Test")
    }

    private fun encrypt(password: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also {
                SecureRandom().nextBytes(it)
            }.toString()
        }
        println("")
        return salt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()
            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    phone,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    //todo constructor for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        _salt: String,
        hash: String,
        phone: String?
    ) : this(firstName, lastName, email = email, rawPhone = phone, meta = mapOf("src" to "csv")) {
        println("Secondary csv constructor")
        salt = _salt
        passwordHash = hash
    }

    fun parseCSV(csv: String): User {
        val user = csv.split(";", ":")
        val (firstName, lastName) = user[0].trim().fullNameToPair()
        return User(
            firstName,
            lastName,
            user[1].ifBlank { null },
            user[2],
            user[3],
            user[4].ifBlank { null }
        )
    }

    fun newAuthoriationCode() {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
    }
}

private fun String.fullNameToPair(): Pair<String, String?> {
    return this.split(" ")
        .filter { it.isNotBlank() }
        .run {
            when
                (size) {
                1 -> first() to null
                2 -> first() to last()
                else -> throw IllegalArgumentException("FullName must contain only first name and last name, current split result ${this@fullNameToPair}")
            }
        }
}

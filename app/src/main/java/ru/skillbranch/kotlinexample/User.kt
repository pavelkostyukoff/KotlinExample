package ru.skillbranch.kotlinexample

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
    val userInfo: String
        get() {
            TODO()
        }

    private val firstName: String
        get() = listOfNotNull(firstName, lastName)
            .map {
                it.first().toUpperCase()
            }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("""[^+\d]""".toRegex(), "")
        }

    private var _login: String? = null
    var login: String?
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login


    private var salt: String? = null

    //for Email
    constructor(firstName: String,
    lastName: String?,
    email: String?,
    password: String):
            this(firstName,lastName,email = email,meta = mapOf("auth" to "password")){
        println("secondary constructor")
        passwordHash = encrypt(password)
    }

    //for Phone
    constructor(
        firstName: String,
    lastName: String?,
    rawPhone: String?
    ):
            this(firstName,lastName,rawPhone = rawPhone,meta = mapOf("auth" to "sms")) {
        println("secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        println("Phone passwordHash is $passwordHash")
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

        init {
            println("First init block, primary constructor was called")
            check(firstName.isNotBlank()) {"Firstname must not be blank"}
            check(!email.isNullOrBlank()|| !rawPhone.isNullOrBlank() ) {"Firstname must not be blank"}
            phone = rawPhone
            login = email ?: phone!!
        }


    fun checkPassword(pass: String)  = encrypt(pass) == passwordHash.also {
        println(" Check")
    }

    fun chengePassword(oldPassword: String, newPass: String){
        if(checkPassword(oldPassword)) {
            passwordHash = encrypt(newPass)
            if(!accessCode.isNullOrEmpty()) accessCode = newPass
            println("Ch")

        }
        else throw IllegalArgumentException("Test")
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
        val digest =  md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6){
                (possible.indices).random().also {
                    index - > append(possible[index])
                }
            }
        }.toString()
    }

    fun sentAccessCodeToUser(phone: String, code: String) {
        println("")
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String?  = null,
            phone: String? = null
        ) :User {
            val (firstName, lastName ) = fullName.fullNameToPair()
            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    phone,
                    password)
                        else -> throw IllegalArgumentException("Test")

            }
        }
    }
    private fun String.fullNameToPair(): Pair<String, String?> =
        this.split(" ")
            .filter {  it.isNullOrBlank() }
            .run {
                when (size) {
                    1 -> first() to null
                    2 -> first() to last()
                    else -> throw IllegalArgumentException("Test2")
                }
            }
}
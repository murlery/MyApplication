package com.lera.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import okhttp3.MediaType.Companion.toMediaType

// ----------- Модели данных -----------
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

@Serializable
data class UserResponse(val data: UserData)

@Serializable
data class UserData(val id: Int, val email: String, val first_name: String)


// ----------- API интерфейс -----------
interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): UserResponse
}


class MainActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var loginButton: Button
    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText

    private lateinit var apiService: ApiService
    private var loginJob: Job? = null

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        loginButton = findViewById(R.id.login_button)
        nameInput = findViewById(R.id.name_input)
        passwordInput = findViewById(R.id.password_input)

        setupApiService()

        loginButton.setOnClickListener {
            loginUser()
        }

        findViewById<View>(android.R.id.content).setOnClickListener {
            hideKeyboard()
        }
    }

    private fun setupApiService() {
        val cache = Cache(File(applicationContext.cacheDir, "http_cache"), 10 * 1024 * 1024)
        val contentType = "application/json".toMediaType()

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cache(cache)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://reqres.in/") // тестовый API
            .client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun loginUser() {
        loginJob?.cancel()

        val email = nameInput.text.toString()
        val password = passwordInput.text.toString()

        loginJob = launch {
            try {
                val loginResponse = withContext(Dispatchers.IO) {
                    apiService.login(LoginRequest(email, password))
                }

                val userResponse = withContext(Dispatchers.IO) {
                    apiService.getUser(2) // получаем произвольного пользователя (например, id = 2)
                }

                Toast.makeText(this@MainActivity, "Вход выполнен: ${userResponse.data.first_name}", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@MainActivity, ChildrenActivity::class.java)
                intent.putExtra("user_name", userResponse.data.first_name)
                startActivity(intent)

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loginJob?.cancel() // отмена запроса
        coroutineContext.cancel() // отмена всех корутин
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}

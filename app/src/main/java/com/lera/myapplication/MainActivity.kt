package com.lera.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

//class MainActivity : AppCompatActivity() {
//    private lateinit var loginButton: Button
//    private lateinit var nameInput: EditText
//    private lateinit var passwordInput: EditText
//    private lateinit var userDao: UserDao
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.main_activity)
//        loginButton = findViewById(R.id.login_button)
//        nameInput = findViewById(R.id.name_input)
//        passwordInput = findViewById(R.id.password_input)
//
//        //экземпляр UserDao для работы с таблицей пользователей
//        userDao = AppDatabase.getDatabase(this).userDao()
//
//
//        loginButton.setOnClickListener {
//            Thread {
//                try {
//                    // Получаем учетные данные пользователя из базы данных по введенному логину и паролю
//                    val user = userDao.getByLoginAndPass(
//                        nameInput.text.toString(),//логин
//                        passwordInput.text.toString()//пароль
//                    )
//                    if (user == null){
//                        throw Exception("Неверные учетные данные")
//                    }
//                    // Если пользователь найден, выполняем код на главном (UI) потоке
//                    runOnUiThread {
//                        Toast.makeText(applicationContext, "Вход выполнен", Toast.LENGTH_SHORT).show()
//                        val intent = Intent(applicationContext, ChildrenActivity::class.java)
//                        intent.putExtra("user", user as Serializable)
//                        intent.putExtra("login", user.login)
//                        startActivity(intent)
//                    }
//                } catch(e : Exception){
//                    runOnUiThread {
//                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }.start()
//
//
//
//        }
//
//        // Устанавливаем обработчик для скрытия клавиатуры при нажатии на экран
//        findViewById<View>(android.R.id.content).setOnClickListener {
//            hideKeyboard()
//        }
//    }
//
//    // Функция для скрытия клавиатуры
//    private fun hideKeyboard() {
//        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
//    }
//
//
//}
class MainActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var userDao: UserDao

    // Поток для работы с базой данных
    private var loginThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        loginButton = findViewById(R.id.login_button)
        nameInput = findViewById(R.id.name_input)
        passwordInput = findViewById(R.id.password_input)

        userDao = AppDatabase.getDatabase(this).userDao()

        loginButton.setOnClickListener {
            loginUser()
        }

        findViewById<View>(android.R.id.content).setOnClickListener {
            hideKeyboard()
        }
    }

    private fun loginUser() {
        // Если поток уже выполняется – прерываем его
        loginThread?.interrupt()

        loginThread = Thread {
            try {
                val login = nameInput.text.toString()
                val password = passwordInput.text.toString()

                // Получаем пользователя из БД (фоновый поток)
                val user = userDao.getByLoginAndPass(login, password)
                    ?: throw Exception("Неверные учетные данные")

                // Обновляем UI (главный поток)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Вход выполнен", Toast.LENGTH_SHORT).show()
                    val intent = Intent(applicationContext, ChildrenActivity::class.java)
                    intent.putExtra("user", user as Serializable)
                    intent.putExtra("login", user.login)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        loginThread?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        loginThread?.interrupt() // Отменяем поток при выходе
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}
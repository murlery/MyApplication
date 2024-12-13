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
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {
    private lateinit var loginButton: Button
    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
        loginButton = findViewById(R.id.login_button)
        nameInput = findViewById(R.id.name_input)
        passwordInput = findViewById(R.id.password_input)

        loginButton.setOnClickListener {
            // Проверка учетных данных
            if (nameInput.text.toString() == "lera" && passwordInput.text.toString() == "a") {
                Toast.makeText(this, "Вход выполнен", Toast.LENGTH_SHORT).show()

                // Получаем введенный логин
                val login = nameInput.text.toString()

                // Переходим к следующей активности, передавая логин
                val intent = Intent(this, ChildrenActivity::class.java)
                intent.putExtra("login", login)
                startActivity(intent)

                // Очищаем поля ввода
                nameInput.setText("")
                passwordInput.setText("")


            } else {
                Toast.makeText(this, "Неверные учетные данные", Toast.LENGTH_SHORT).show()
            }
        }

        // Устанавливаем обработчик для скрытия клавиатуры при нажатии на экран
        findViewById<View>(android.R.id.content).setOnClickListener {
            hideKeyboard()
        }
    }
    // Функция для скрытия клавиатуры
    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }






}

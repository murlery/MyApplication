package com.lera.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChildrenActivity : AppCompatActivity() {
    private lateinit var nameChildEditText: EditText;
    private lateinit var birthdayChildEditText: EditText;
    private lateinit var addChildCheckbox: CheckBox;
    private lateinit var addChildButton: Button;
    private lateinit var nextChildButton: Button;
    private var childrenList = mutableListOf<Child>() // Список детей
    private lateinit var login: String;


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_children)

        // Получаем логин из Intent
        login = intent.getStringExtra("login") ?: "" //  Используем пустую строку, если login не найден
        // Получаем список детей из Intent
        childrenList = intent.getSerializableExtra("children") as? MutableList<Child> ?: mutableListOf()
        // Находим TextView и устанавливаем текст приветствия
        val welcomeTextView = findViewById<TextView>(R.id.welcomeTextView)
        welcomeTextView.text = "Привет, $login!"
        nameChildEditText = findViewById(R.id.nameChildEditText)
        birthdayChildEditText = findViewById(R.id.birthdayChildEditText)
        addChildCheckbox = findViewById(R.id.childCheckbox)
        addChildButton = findViewById(R.id.addChildButton)

        // Добавляем слушателя для изменения состояния CheckBox
        addChildCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                addChildButton.isEnabled = true
            } else {
                addChildButton.isEnabled = false
            }
        }

        addChildButton.setOnClickListener {

                  addChild()
                  }


    }
    // Функция для добавления ребенка в список
    private fun addChild() {
        if (addChildCheckbox.isChecked) {
            val nameChild = nameChildEditText.text.toString()

            val birthdayChild = birthdayChildEditText.text.toString()

            // Проверяем, что поля заполнены
            if (nameChild.isNotEmpty() && birthdayChild.isNotEmpty()) {
                // Преобразуем дату рождения в Date объект
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                try {
                    val date = dateFormat.parse(birthdayChild)
                    // Проверяем, что дата не больше текущей
                    val currentDate = Calendar.getInstance().time
                    if (date.after(currentDate)) {
                        Toast.makeText(this, "Некорректная дата рождения", Toast.LENGTH_SHORT).show()
                        return
                    }
                    // Проверяем, что дата корректна
                    val calendar = Calendar.getInstance()
                    calendar.time = date

                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    val month = calendar.get(Calendar.MONTH) + 1 // Месяцы начинаются с 0
                    val year = calendar.get(Calendar.YEAR)

                    if (day != dateFormat.format(date).split(".")[0].toInt() ||
                        month != dateFormat.format(date).split(".")[1].toInt() ||
                        year != dateFormat.format(date).split(".")[2].toInt()) {
                        Toast.makeText(this, "Некорректная дата", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Проверяем, что год рождения не выше текущего года
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    if (year > currentYear) {
                        Toast.makeText(this, "Год рождения не может быть выше текущего года", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Создаем объект Child и добавляем его в список
                    val child = Child(nameChild,  date)
                    childrenList.add(child)
                    nameChildEditText.setText("")
                    birthdayChildEditText.setText("")
                    addChildCheckbox.isChecked = false

                } catch (e: Exception) {
                    Toast.makeText(this, "Неверный формат даты рождения", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Выберите галочку", Toast.LENGTH_SHORT).show()
        }
       // supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_main -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_list_children -> {
                val intent = Intent(this, ListChildrenActivity::class.java)
                intent.putExtra("login", login) // Передаем логин
                intent.putExtra("children", childrenList as Serializable)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    data class Child (var name: String, var birthday: Date): Serializable{
        companion object {
            private var nextId = 1
        }

        val id: Int = nextId++
        override fun toString(): String {
            return "$name, Дата рождения: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(birthday)}"
        }
    }}


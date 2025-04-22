package com.lera.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var childDao: ChildDao
    private lateinit var user: User
    private var job: Job? = null  // Для управления корутинами
    private var ioJob: Job? = null // Job для работы с IO
    private val computationJob = Job() // Job для вычислений
    private val computationScope = CoroutineScope(Dispatchers.Default + computationJob)


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingInflatedId")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_children)

        childDao = AppDatabase.getDatabase(this).childDao()

        // Получаем логин из Intent
        user = intent.getSerializableExtra("user") as User

        // Получаем список детей из Intent
        childrenList = intent.getSerializableExtra("children") as? MutableList<Child> ?: mutableListOf()
        // Находим TextView и устанавливаем текст приветствия
        val welcomeTextView = findViewById<TextView>(R.id.welcomeTextView)
        welcomeTextView.text = "Привет, ${user.login}!"
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
    // Функция для добавления ребенка
    private fun addChild() {
        if (!addChildCheckbox.isChecked) {
            Toast.makeText(this, "Выберите галочку", Toast.LENGTH_SHORT).show()
            return
        }

        val nameChild = nameChildEditText.text.toString().trim()
        val birthdayChild = birthdayChildEditText.text.toString().trim()

        if (nameChild.isEmpty() || birthdayChild.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        // Используем Dispatchers.Default для обработки даты
        computationScope.launch {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            val date: Date? = try {
                dateFormat.parse(birthdayChild)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChildrenActivity, "Неверный формат даты", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            if (date == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChildrenActivity, "Ошибка обработки даты", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val currentDate = Calendar.getInstance().time
            if (date.after(currentDate)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChildrenActivity, "Некорректная дата рождения", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val calendar = Calendar.getInstance().apply { time = date }
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            if (calendar.get(Calendar.YEAR) > currentYear) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChildrenActivity, "Год рождения не может быть выше текущего года", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val child = Child(user.id, nameChild, date)

            // Переключаемся на IO-контекст для сохранения в БД
            ioJob?.cancel()
            ioJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    childDao.insert(child)
                    sendChildToServer(child)
                    withContext(Dispatchers.Main) {
                        childrenList.add(child)
                        Toast.makeText(this@ChildrenActivity, "Ребенок добавлен!", Toast.LENGTH_SHORT).show()
                        clearFields()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChildrenActivity, "Ошибка при добавлении ребенка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun sendChildToServer(child: Child) {
        val apiService = NetworkModule.provideApiService(this)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val childDTO = ChildDTO(0, child.user, child.name, formatter.format(child.birthday))

        lifecycleScope.launch {
            try {
                val response = apiService.createChild(childDTO)
                Toast.makeText(this@ChildrenActivity, "Добавлен на сервер: ${response.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ChildrenActivity, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Функция очистки полей ввода
    private fun clearFields() {
        nameChildEditText.text.clear()
        birthdayChildEditText.text.clear()
        addChildCheckbox.isChecked = false
        hideKeyboard()
    }

    override fun onDestroy() {
        super.onDestroy()
        ioJob?.cancel() // Отменяем IO-задачу
        computationJob.cancel() // Отменяем вычислительный контекст
    }

    // Универсальная функция для скрытия клавиатуры
    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
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
                intent.putExtra("user", user as Serializable) // Передаем user
                intent.putExtra("children", childrenList as Serializable)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}


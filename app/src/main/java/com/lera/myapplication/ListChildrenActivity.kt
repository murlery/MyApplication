package com.lera.myapplication
import ChildListAdapter
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException


class ListChildrenActivity: AppCompatActivity() {
//    private lateinit var login: String
    private lateinit var user: User
    private lateinit var yourChildrenTextView: TextView;
    private var children = mutableListOf<Child>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChildListAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var childDao: ChildDao
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())//для задач с БД и файлами
    private val computationScope = CoroutineScope(Dispatchers.Default + Job())//для обработки данных (преобразования строк в объекты Child)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_children_activity)

        childDao = AppDatabase.getDatabase(this).childDao()

        user = intent.getSerializableExtra("user") as User

        // Находим TextView для отображения приветствия
        yourChildrenTextView = findViewById(R.id.yourChildrenTextView)
        yourChildrenTextView.text = "Ваши детки, ${user.login}!" // Отображаем логин

        // Находим RecyclerView
        recyclerView = findViewById(R.id.childrenRecyclerView)

        // Создаем адаптер для RecyclerView
        adapter = ChildListAdapter(children)
        recyclerView.adapter = adapter

        // Устанавливаем горизонтальный LinearLayoutManager
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Регистрируем RecyclerView для контекстного меню
        registerForContextMenu(recyclerView)
        loadChildrenFromDbAndShow()
        // Настраиваем SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            showAddChildDialog()
            swipeRefreshLayout.isRefreshing = false // Останавливаем индикатор загрузки
        }
        var initialY: Float = 0f
        var isSwipingUp: Boolean = false
        // Инициализация кнопок
        val saveTxtButton: Button = findViewById(R.id.save_txt_button)
        val saveBinaryButton: Button = findViewById(R.id.save_binary_button)
        val savePdfButton: Button = findViewById(R.id.save_pdf_button)
        val loadTxtButton: Button = findViewById(R.id.load_txt_button)
        val loadBinaryButton: Button = findViewById(R.id.load_binary_button)
        val loadPdfButton: Button = findViewById(R.id.load_pdf_button)

        // Обработчики нажатий для сохранения данных
        saveTxtButton.setOnClickListener { saveChildrenToTxtFile() }
        saveBinaryButton.setOnClickListener { saveChildrenToBinaryFile2() }
        savePdfButton.setOnClickListener{ saveToPDF(this) }

        // Обработчики нажатий для загрузки данных
        loadTxtButton.setOnClickListener {
            for (child in children) {
                deleteChildFromDb(child)
            }
            children.clear()
            loadChildrenFromTxtFile()
            for (child in children) {
                saveChildToDb(child)
            }
            adapter.notifyDataSetChanged()
        }
        loadBinaryButton.setOnClickListener {
            loadChildrenFromBinaryFile()
        }
        loadPdfButton.setOnClickListener {
            openPdfFile(this, "data")
        }
    }
    private fun loadChildrenFromDbAndShow() {
        ioScope.launch {
            val childrenList = childDao.getAll(user.id)

            withContext(Dispatchers.Main) {

                children.addAll(childrenList)
                adapter.notifyDataSetChanged() // Уведомляем адаптер об изменениях
            }
        }
    }
    private fun saveChildToDb(child: Child){
        ioScope.launch {
            childDao.insert(child)
        }
    }
    private fun updateChildInDb(child: Child){
        ioScope.launch {
            childDao.update(child)
        }
    }

    private fun deleteChildFromDb(child: Child){
        ioScope.launch {
            childDao.delete(child)
        }
    }
    private fun saveChildrenToTxtFile() {
        ioScope.launch {
            val filename = "children.txt"
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            try {
                withTimeout(5000) { // Тайм-аут 5 секунд
                    openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                        for (child in children) {
                            val line = "${child.id},${child.user},${child.name},${dateFormat.format(child.birthday)}\n"
                            output.write(line.toByteArray()) // Запись строки
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ListChildrenActivity, "Файл сохранен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val message = if (e is CancellationException) "Операция прервана (тайм-аут)" else "Ошибка: ${e.message}"
                    Toast.makeText(this@ListChildrenActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun loadChildrenFromTxtFile() {
        ioScope.launch {
            val filename = "children.txt"

            try {
                val childrenList = withTimeout(5000) {
                    val fileContent = openFileInput(filename).bufferedReader().use { it.readText() }
                    // переключаем на dispatchers.default для парсинга содержимого файла
                    computationScope.async {
                        fileContent
                            .lineSequence()// разбиваем текст на строки
                            .mapNotNull { line ->
                                val parts = line.split(",")
                                if (parts.size == 4) {
                                    val id = parts[0].toInt()
                                    val loadedUser = parts[1].toInt()
                                    if (loadedUser == user.id) {
                                        val name = parts[2]
                                        val birthday = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(parts[3])
                                        // Если дата распознана, создаем объект `Child`, иначе пропускаем строку
                                        if (birthday != null) Child(loadedUser, name, birthday).apply { this.id = id } else null
                                    } else null
                                } else null
                            }.toList()
                    }.await() // дожидаемся завершения асинхронной обработки данных
                }

                withContext(Dispatchers.Main) {
                    children.clear()
                    children.addAll(childrenList)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@ListChildrenActivity, "Файл загружен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val message = if (e is CancellationException) "Операция прервана (тайм-аут)" else "Ошибка: ${e.message}"
                    Toast.makeText(this@ListChildrenActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun saveChildrenToBinaryFile2() {
        ioScope.launch {
        val filename = "children.dat"

        // Получаем путь к директории приложения
        val directory = getExternalFilesDir(null)

        // Проверяем, что директория существует
        if (directory != null && !directory.exists()) {
            directory.mkdirs() // Создаем директорию, если она не существует
        }

        // Создаем файл в указанной директории
        val file = File(directory, filename)

        try {
            // Записываем данные в файл
            FileOutputStream(file).use { output ->
                ObjectOutputStream(output).use { oos ->
                    oos.writeObject(children)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ListChildrenActivity, "Файл сохранен", Toast.LENGTH_SHORT).show()
            }} catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ListChildrenActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }}
    }




    private fun loadChildrenFromBinaryFile() {
        ioScope.launch {
            val filename = "children.dat"
            val file = File(getExternalFilesDir(null), filename)

            try {
                val childrenList = withTimeout(5000) { // Тайм-аут 5 секунд
                    ObjectInputStream(FileInputStream(file)).use { ois ->
                        ois.readObject() as List<Child>
                    }
                }
                withContext(Dispatchers.Main) {
                    children.clear()
                    children.addAll(childrenList)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@ListChildrenActivity, "Файл загружен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val message = if (e is CancellationException) "Операция прервана (тайм-аут)" else "Ошибка: ${e.message}"
                    Toast.makeText(this@ListChildrenActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPdfFile(context: Context, fileName: String) {

        //Массив строк, указывающий, какие колонки из базы данных медиафайлов мы хотим получить
        // запрашиваем только идентификатор файла
        val projection = arrayOf(MediaStore.MediaColumns._ID)

        // Строка, представляющая условие выборки
        // ищем файл с именем, соответствующим заданному имени (используется параметр ? для безопасной подстановки)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"

        //Массив аргументов для условия выборки
        // добавляем расширение .pdf к имени файла
        val selectionArgs = arrayOf("$fileName.pdf")


        //Cursor — это интерфейс, который предоставляет доступ к данным, полученным из базы данных или другого источника данных.
        // Он представляет собой набор строк (записей), которые могут быть обработаны по одной за раз.
        // Выполняем запрос к контент-провайдеру для получения курсора с результатами
        val cursor = context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            null
        )
        // Проверяем, успешно ли выполнен запрос и есть ли результаты
        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            val uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())

            // интент для открытия PDF-файла

            val intent = Intent(Intent.ACTION_VIEW)//Это предопределенное действие, которое указывает, что вы хотите просмотреть данные
            intent.setDataAndType(uri, "application/pdf")// тип содержимого как PDF
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION// Запрашиваем разрешение на чтение URI
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)// Добавляем флаг для запуска новой задачи

            try {
                // Пытаемся запустить активность для открытия PDF-файла
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Нет приложения для открытия PDF", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
        }
        // Закрываем курсор, чтобы избежать утечек памяти
        cursor?.close()
    }

    fun saveToPDF(context: Context) {
        try {
            savePdfToSharedStorage(context, "data")
        } catch (e: IOException) {
            Toast.makeText(context, "Ошибка при сохранении PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Throws(IOException::class)
    private fun savePdfToSharedStorage(context: Context, fileName: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        paint.textSize = 12f

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 50f
        for ((index, child) in children.withIndex()) {
            canvas.drawText("Имя: ${child.name}", 50f, y, paint)
            y += 20f
            canvas.drawText("Дата рождения: ${SimpleDateFormat("dd.MM.yyyy").format(child.birthday)}", 50f, y, paint)
            y += 40f
        }
        pdfDocument.finishPage(page)

        // Создаем ContentValues для добавления файла в MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS) // Путь к директории документов
        }

        // Получаем URI для записи файла
        val uri: Uri? = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        // Записываем PDF в OutputStream
        uri?.let { pdfUri ->
            context.contentResolver.openOutputStream(pdfUri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                Toast.makeText(context, "PDF сохранен в $pdfUri", Toast.LENGTH_LONG).show()
            }
        } ?: throw IOException("Не удалось создать файл в Shared Storage")

        pdfDocument.close()
    }

    private fun showEditChildDialog(position: Int) {
        // Получаем данные ребенка по позиции
        val child = adapter.getChildAtPosition(position) // Предполагается, что у вас есть метод для получения ребенка по позиции
        val currentName = child.name // Предполагается, что у объекта ребенка есть поле name
        val currentBirthday = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(child.birthday) // Форматируем дату

        // Создаем EditText для редактирования имени ребенка и даты рождения
        val editTextName = EditText(this)
        editTextName.hint = "Введите имя ребенка"
        editTextName.setText(currentName) // Устанавливаем текущее имя

        val editTextBirthday = EditText(this)
        editTextBirthday.hint = "Введите дату рождения (дд.мм.гггг)"
        editTextBirthday.setText(currentBirthday) // Устанавливаем текущую дату рождения

        // Создаем вертикальный LinearLayout для размещения EditText
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(editTextName)
        layout.addView(editTextBirthday)

        // Создаем диалоговое окно
        val dialog = AlertDialog.Builder(this)
            .setTitle("Редактировать ребенка")
            .setMessage("Измените данные о ребенке:")
            .setView(layout)
            .setPositiveButton("Сохранить") { dialogInterface, which ->
                val newChildName = editTextName.text.toString()
                val newBirthdayString = editTextBirthday.text.toString()

                if (newChildName.isNotBlank() && newBirthdayString.isNotBlank()) {
                    try {
                        val newBirthday = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(newBirthdayString)
                        if (newBirthday != null) {
                            val updatedChild = children[position]
                            child.name = newChildName
                            child.birthday = newBirthday
                            updateChildInDb(updatedChild)
                            adapter.updateChild(position, newChildName, newBirthday) // Обновляем данные ребенка в адаптере
                        } else {
                            Toast.makeText(this, "Некорректная дата", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: ParseException) {
                        Toast.makeText(this, "Некорректный формат даты", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Имя и дата не могут быть пустыми", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }


    private fun showAddChildDialog() {
        // Создаем EditText для ввода имени ребенка и даты рождения
        val editTextName = EditText(this)
        editTextName.hint = "Введите имя ребенка"

        val editTextBirthday = EditText(this)
        editTextBirthday.hint = "Введите дату рождения (дд.мм.гггг)"

        // Создаем вертикальный LinearLayout для размещения EditText
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(editTextName)
        layout.addView(editTextBirthday)

        // Создаем диалоговое окно
        val dialog = AlertDialog.Builder(this)
            .setTitle("Добавить ребенка")
            .setMessage("Введите данные о ребенке:")
            .setView(layout)
            .setPositiveButton("Добавить") { dialogInterface, which ->
                val childName = editTextName.text.toString()
                val birthdayString = editTextBirthday.text.toString()

                if (childName.isNotBlank() && birthdayString.isNotBlank()) {
                    try {
                        val birthday = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(birthdayString)
                        if (birthday != null) {
                            addChild(childName, birthday) // Добавляем ребенка в список
                        } else {
                            Toast.makeText(this, "Некорректная дата", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: ParseException) {
                        Toast.makeText(this, "Некорректный формат даты", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Имя и дата не могут быть пустыми", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }



    private fun addChild(childName: String, birthday: Date) {
        // Создаем нового ребенка и добавляем его в список
        val newChild = Child(user.id, childName, birthday)
        children.add(newChild) // Добавляем нового ребенка в список
        saveChildToDb(newChild)
        adapter.notifyItemInserted(children.size - 1) // Уведомляем адаптер о добавлении нового элемента
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle("Действие")
        menu.add(Menu.NONE, ChildListAdapter.DELETE_CHILD, Menu.NONE, "Удалить")
        menu.add(Menu.NONE, ChildListAdapter.EDIT_CHILD, Menu.NONE, "Редактировать")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("delete_position", -1)
        when (item.itemId) {
            ChildListAdapter.DELETE_CHILD -> {
                if (position != -1) {
                    deleteChildFromDb(children[position])
                    adapter.deleteChild(position)
                }
                return true
            }
            ChildListAdapter.EDIT_CHILD -> { // Обработка редактирования
                if (position != -1) {
                    showEditChildDialog(position) // Открываем диалог редактирования
                }
                return true
            }
        }
        return super.onContextItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu) // Загружаем меню из файла
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_main -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_children -> {
                val intent = Intent(this, ChildrenActivity::class.java)
                intent.putExtra("user", user as Serializable)
                intent.putExtra("children", children as Serializable)// Передаем список детей
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
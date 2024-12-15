package com.lera.myapplication
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import ChildListAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.text.font.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lera.myapplication.ChildrenActivity.Child
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
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

class ListChildrenActivity: AppCompatActivity() {
    private lateinit var login: String;
    private lateinit var yourChildrenTextView: TextView;
    private var children = mutableListOf<Child>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChildListAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_children_activity)

        // Получаем список детей из Intent
        children = (intent.getSerializableExtra("children") as ArrayList<ChildrenActivity.Child>?)!!
        // Получаем логин из Intent
        login = intent.getStringExtra("login") ?: "Пусто"


        // Находим TextView для отображения приветствия
        yourChildrenTextView = findViewById(R.id.yourChildrenTextView)
        yourChildrenTextView.text = "Ваши детки, $login!" // Отображаем логин

        // Находим RecyclerView
        recyclerView = findViewById(R.id.childrenRecyclerView)
        // Устанавливаем горизонтальный LinearLayoutManager
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Регистрируем RecyclerView для контекстного меню
        registerForContextMenu(recyclerView)

        // Создаем адаптер для RecyclerView
        adapter = ChildListAdapter(children)
        recyclerView.adapter = adapter

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
        saveBinaryButton.setOnClickListener { saveChildrenToBinaryFile() }
        //savePdfButton.setOnClickListener { saveChildrenToPdfFile() }
        // Обработчики нажатий для загрузки данных
        loadTxtButton.setOnClickListener {
            children.clear()
            children.addAll(loadChildrenFromTxtFile())
            adapter.notifyDataSetChanged()
        }
        loadBinaryButton.setOnClickListener {
            children.clear()
            children.addAll(loadChildrenFromBinaryFile())
            adapter.notifyDataSetChanged()
        }
        savePdfButton.setOnClickListener{
            saveToPDF(this)

        }
        loadPdfButton.setOnClickListener {
            openPdfFile(this, "data")

        }

    }
    private fun saveChildrenToTxtFile() {
        val filename = "children.txt"
        openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
            for (child in children) {
                val line = "${child.id},${child.name},${SimpleDateFormat("dd.MM.yyyy").format(child.birthday)}\n"
                output.write(line.toByteArray())
            }
        }
    }

    private fun loadChildrenFromTxtFile(): List<Child> {
        val filename = "children.txt"
        val childrenList = mutableListOf<Child>()

        try {
            openFileInput(filename).use { input ->
                input.bufferedReader().forEachLine { line ->
                    val parts = line.split(",")
                    if (parts.size == 3) {
                        val id = parts[0]
                        val name = parts[1]
                        val birthday = SimpleDateFormat("dd.MM.yyyy").parse(parts[2])
                        if (birthday != null) {
                            childrenList.add(Child(name, birthday))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return childrenList
    }

    private fun saveChildrenToBinaryFile() {
        val filename = "children.dat"

        File(getExternalFilesDir(null), filename).outputStream().use { output ->
            ObjectOutputStream(output).use { oos ->
                oos.writeObject(children)
            }
        }
    }

    private fun loadChildrenFromBinaryFile(): List<Child> {
        val filename = "children.dat"

        return try {
            ObjectInputStream(FileInputStream(File(getExternalFilesDir(null), filename))).use { ois ->
                ois.readObject() as List<Child>
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    fun saveToPDF(context: Context) {
        try {
            savePdfToInternalStorage(context, "data")
        } catch (e: IOException) {
            Toast.makeText(context, "Ошибка при сохранении PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    fun saveListToPDF() {


        // Создаем новый PDF-документ
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 размер
        val page = pdfDocument.startPage(pageInfo)

        // Получаем объект Canvas для рисования на странице
        val canvas = page.canvas
        val paint = Paint()
        paint.textSize = 12f

        // Записываем данные о детях на страницу
        var yPosition = 25
        for (child in children) {
            canvas.drawText("ID: ${child.id}, Имя: ${child.name}, Дата рождения: ${SimpleDateFormat("dd.MM.yyyy").format(child.birthday)}", 10f, yPosition.toFloat(), paint)
            yPosition += 20 // Увеличиваем позицию по Y для следующей строки
        }

        // Завершаем страницу и добавляем ее в документ
        pdfDocument.finishPage(page)

        // Сохраняем документ в файл
        val pdfFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "children_list.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            Toast.makeText(this, "PDF сохранен: ${pdfFile.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при сохранении PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }

        val pdfFile1 = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "children_list.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(pdfFile1))
            if (pdfFile1.exists()) {
                Toast.makeText(this, "PDF сохранен: ${pdfFile1.absolutePath}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ошибка: файл не создан", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при сохранении PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    @Throws(IOException::class)
    private fun savePdfToInternalStorage(context: Context, fileName: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        paint.textSize = 12f

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 50f
        for ((index, child) in children.withIndex()) {
            canvas.drawText("ID: ${child.id}", 50f, y, paint)
            y += 20f
            canvas.drawText("Имя: ${child.name}", 50f, y, paint)
            y += 20f
            canvas.drawText("Дата рождения: ${SimpleDateFormat("dd.MM.yyyy").format(child.birthday)}", 50f, y, paint)
            y += 40f
        }
        pdfDocument.finishPage(page)

        val file = File(context.filesDir, "$fileName.pdf")
        if (file.exists()) file.delete()
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(context, "PDF сохранен в ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }

    private fun openPdfFile(context: Context, fileName: String) {
        val file = File(context.filesDir, "$fileName.pdf")
        if (!file.exists()) {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) //Requires adding FileProvider to manifest

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Нет приложения для открытия PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun readChildrenFromPDF(context: Context, pdfFilePath: String): List<Child> {
        val childrenList = mutableListOf<Child>()
        try {
            val pdfFile = File(pdfFilePath)
            if (!pdfFile.exists()) {
                Toast.makeText(this, "нет такого", Toast.LENGTH_SHORT).show()
                return childrenList //возвращает пустой список, если файл не существует
            }
            val document = PDDocument.load(pdfFile)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()

            val lines = text.lines()
            val dateFormat = SimpleDateFormat("ID: \\d+, Имя: \\w+, Дата рождения: (\\d{2}\\.\\d{2}\\.\\d{4})", Locale.getDefault())
            for (line in lines) {
                val matcher = dateFormat.toPattern().toRegex().find(line)

                matcher?.let {
                    val dateString = it.groupValues[1]
                    try {
                        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateString)
                        val id = line.substringAfter("ID: ").substringBefore(",")
                        val name = line.substringAfter("Имя: ").substringBefore(", Дата рождения")

                        childrenList.add(Child(name, date))
                    } catch (e: ParseException) {
                        //Обработка исключений
                        e.printStackTrace()
                    }
                }

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return childrenList
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
        val newChild = Child(childName, birthday)
        children.add(newChild) // Добавляем нового ребенка в список
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
                intent.putExtra("login", login)
                intent.putExtra("children", children as Serializable)// Передаем список детей
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
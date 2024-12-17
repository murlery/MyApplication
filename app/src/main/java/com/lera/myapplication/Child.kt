package com.lera.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Класс-конвертер для преобразования объектов Date в строки и обратно для Room Database.
class DateConverter {
    // Аннотация @TypeConverter указывает, что этот метод используется для преобразования типов данных.
    @TypeConverter
    fun fromBirthday(birthday: Date): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(birthday)// Преобразует Date в String для хранения в базе данных.
    }

    @TypeConverter
    fun toBirthday(data: String): Date {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.parse(data)// Преобразует String в Date при чтении из базы данных.
    }
}
// Аннотация @Entity указывает, что этот класс представляет сущность (таблицу) в базе данных Room.
@Entity(
    tableName = "childs_table",
    foreignKeys = [
        ForeignKey(
            onDelete = CASCADE,
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user"]
        )]

)

//определяется структура таблицы
// Аннотация @TypeConverters указывает, какой класс конвертеров использовать для этого класса.
@TypeConverters(DateConverter::class)
class Child (
    @ColumnInfo(name = "user") var user: Int,// Аннотация @ColumnInfo задает имя колонки в таблице, Внешний ключ
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "birthday") var birthday: Date,
): Serializable {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    // Переопределение метода toString для удобного вывода информации об объекте.
    override fun toString(): String {
        return "$name, Дата рождения: ${birthday}"
    }
    @TypeConverter
    fun fromBirthday(birthday: Date): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(birthday)
    }

    @TypeConverter
    fun toBirthday(data: String): Date {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.parse(data)
    }
}


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


class DateConverter {
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
@TypeConverters(DateConverter::class)
class Child (
    @ColumnInfo(name = "user") var user: Int,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "birthday") var birthday: Date,
): Serializable {
    @PrimaryKey(autoGenerate = true)
    var id = 0
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


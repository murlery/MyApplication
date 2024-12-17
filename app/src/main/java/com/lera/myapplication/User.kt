package com.lera.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
//определяется структура таблицы
// Аннотация @Entity указывает, что этот класс представляет сущность (таблицу) в базе данных Room.
@Entity(tableName = "users_table")
class User (
    @ColumnInfo(name = "login") var login: String,// Аннотация @ColumnInfo задает имя колонки в таблице
    @ColumnInfo(name = "pass") var pass: String,
): Serializable {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    override fun toString(): String {
        return login
    }
}

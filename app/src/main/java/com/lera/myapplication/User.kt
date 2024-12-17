package com.lera.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users_table")
class User (
    @ColumnInfo(name = "login") var login: String,
    @ColumnInfo(name = "pass") var pass: String,
): Serializable {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    override fun toString(): String {
        return login
    }
}

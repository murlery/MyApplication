package com.lera.myapplication

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
//предоставляет методы для добавления, обновления, удаления и получения данных о детях из базы данных,
// абстрагируя  детали SQL-запросов
@Dao
interface ChildDao {
    // Аннотация @Insert указывает, что этот метод используется для вставки данных.
    @Insert
    fun insertList(data: List<Child>)
    //1 объект
    @Insert
    suspend fun insert(child: Child)

    @Update
    suspend fun update(child: Child)

    @Delete
    suspend fun delete(child: Child)

    @Query("Select * from childs_table WHERE user=:user order by id ASC")// Запрос для получения всех детей, принадлежащих указанному пользователю, отсортированных по id.
    suspend fun getAll(user: Int): List<Child>
}
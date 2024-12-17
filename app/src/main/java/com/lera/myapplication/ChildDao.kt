package com.lera.myapplication

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ChildDao {
    @Insert
    fun insertList(data: List<Child>)

    @Insert
    suspend fun insert(child: Child)

    @Update
    suspend fun update(child: Child)

    @Delete
    suspend fun delete(child: Child)

    @Query("Select * from childs_table WHERE user=:user order by id ASC")
    fun getAll(user: Int): List<Child>
}
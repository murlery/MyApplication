package com.lera.myapplication


import retrofit2.http.*

interface ApiService {

    @GET("children")
    suspend fun getChildren(): List<ChildDTO>

    @POST("children")
    suspend fun createChild(@Body newChild: ChildDTO): ChildDTO

    @PUT("children/{id}")
    suspend fun updateChild(@Path("id") id: Int, @Body updatedChild: ChildDTO): ChildDTO

    @DELETE("children/{id}")
    suspend fun deleteChild(@Path("id") id: Int): Unit
}

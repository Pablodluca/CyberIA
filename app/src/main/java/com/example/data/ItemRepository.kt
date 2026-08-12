package com.example.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    val allItems: Flow<List<Item>> = itemDao.getAllItems()

    suspend fun insert(item: Item) = itemDao.insertItem(item)

    suspend fun deleteById(id: Int) = itemDao.deleteItemById(id)
}

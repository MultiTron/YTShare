package com.example.ytshare.helpers

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.ytshare.models.LinkModel

class DBHelper (context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

        // below is the method for creating a database by a sqlite query
        override fun onCreate(db: SQLiteDatabase) {
            // below is a sqlite query, where column names
            // along with their data types is given
            val query = ("""CREATE TABLE $TABLE_NAME (
                    $ID_COL INTEGER PRIMARY KEY AUTOINCREMENT,
                    $TITLE_COL TEXT,
                    $LINK_COL TEXT,
                    $THUMB_COL TEXT,
                    $DATE_COL DATETIME DEFAULT CURRENT_TIMESTAMP)""")

            // we are calling sqlite
            // method for executing our query
            db.execSQL(query)
        }

        override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
            // this method is to check if table already exists
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }

        // This method is for adding data in our database
        fun addLink(title : String, link : String, thumbnail : String){

            // below we are creating
            // a content values variable
            val values = ContentValues()

            // we are inserting our values
            // in the form of key-value pair
            values.put(TITLE_COL, title)
            values.put(LINK_COL, link)
            values.put(THUMB_COL, thumbnail)

            // here we are creating a
            // writable variable of
            // our database as we want to
            // insert value in our database
            val db = this.writableDatabase

            // all values are inserted into database
            db.insert(TABLE_NAME, null, values)

            // at last we are
            // closing our database
            db.close()
        }

    fun deleteAll(){
        val db = this.writableDatabase
        val truncateQuery = "DELETE FROM $TABLE_NAME"
        db.execSQL(truncateQuery)
        db.close()
    }

    fun getAllLinks(): List<LinkModel> {

        val linkList = ArrayList<LinkModel>()
        val selectQuery = "SELECT $TITLE_COL, $LINK_COL, $THUMB_COL, $DATE_COL FROM $TABLE_NAME"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                val title = cursor.getString(cursor.getColumnIndexOrThrow(TITLE_COL))
                val link = cursor.getString(cursor.getColumnIndexOrThrow(LINK_COL))
                val thumb = cursor.getString(cursor.getColumnIndexOrThrow(THUMB_COL))
                val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DATE_COL))
                linkList.add(LinkModel(title, link, thumb, timestamp))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return linkList
    }

    companion object{
        // here we have defined variables for our database

        // below is variable for database name
        private const val DATABASE_NAME = "YTSHARE"

        // below is the variable for database version
        private const val DATABASE_VERSION = 3

        // below is the variable for table name
        const val TABLE_NAME = "link_history"

        // below is the variable for id column
        const val ID_COL = "id"

        const val TITLE_COL = "title"

        // below is the variable for name column
        const val LINK_COL = "link"

        const val THUMB_COL = "thumbnail"

        // below is the variable for age column
        const val DATE_COL = "date"
    }
}
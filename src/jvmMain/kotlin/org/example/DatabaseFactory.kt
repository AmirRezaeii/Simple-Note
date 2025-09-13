package org.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(dbFilePath: String = "./simplenote.db") {
        val jdbcUrl = "jdbc:sqlite:$dbFilePath"
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_SERIALIZABLE"
        }
        val ds = HikariDataSource(config)
        Database.connect(ds)

        // Create tables if not exist
        transaction {
            SchemaUtils.create(NotesTable)
        }
    }
}

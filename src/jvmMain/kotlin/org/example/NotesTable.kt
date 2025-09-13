import org.jetbrains.exposed.sql.Table

object Notes : Table("notes") {
    val localId = integer("local_id").autoIncrement()
    val id = integer("id").nullable() // remote ID from API (nullable if offline only)
    val title = varchar("title", 255)
    val content = text("content")

    override val primaryKey = PrimaryKey(localId)
}

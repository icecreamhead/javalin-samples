package javalinstagram.util

import javalinstagram.Database

// database-setup/reset
fun main() {
    println("Setting up database...")
    Database.useHandle<Exception> { handle ->
        handle.execute("DROP TABLE IF EXISTS \"user\"")
        handle.execute("CREATE TABLE \"user\" (id VARCHAR, password VARCHAR, created TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
        handle.execute("DROP TABLE IF EXISTS photo")
        handle.execute("CREATE TABLE photo (id VARCHAR, ownerid VARCHAR, created TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
        handle.execute("DROP TABLE IF EXISTS \"like\"")
        handle.execute("CREATE TABLE \"like\" (photoid VARCHAR, ownerid VARCHAR, UNIQUE(photoid, ownerid) )")
    }
    println("Database setup complete!")
}

object DbSetupUtil {
    fun bootstrap() {
        main();
    }
}

package javalinstagram

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.util.JavalinLogger
import io.javalin.vue.VueComponent
import javalinstagram.Role.ANYONE
import javalinstagram.Role.LOGGED_IN
import javalinstagram.account.AccountController
import javalinstagram.like.LikeController
import javalinstagram.photo.PhotoController
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import java.net.URI
import java.net.URISyntaxException
import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource
import kotlin.system.exitProcess

val Database: Jdbi = Jdbi.create(getConnection())
val DATA_DIR: String = resolveDataDir()

fun resolveDataDir(): String {
    return with (System.getenv("DATA_DIR")) {
        if (isNullOrBlank()) {
            JavalinLogger.error("No data dir configured")
            exitProcess(42)
        }
        "${this}/user-uploads/static/p"
    }
}

//@Throws(URISyntaxException::class, SQLException::class)
private fun getConnection(): DataSource {
    return with(URI(System.getenv("DATABASE_URL"))) {
        val ds = PGSimpleDataSource()
        ds.serverNames = arrayOf(host);
        ds.databaseName = path.substring(1);
        ds.user = userInfo.split(":")[0];
        ds.password = userInfo.split(":")[1];
        ds
    }
}

fun main() {
//    DbSetupUtil.bootstrap()
    val app = Javalin.create {
        it.staticFiles.add("src/main/resources/public", Location.EXTERNAL)
        it.staticFiles.enableWebjars()
        it.jetty.sessionHandler { Session.fileSessionHandler() }
        it.accessManager { handler, ctx, permitted ->
            when {
                ANYONE in permitted -> handler.handle(ctx)
                ctx.currentUser != null && permitted.contains(LOGGED_IN) -> handler.handle(ctx)
                ctx.header(Header.ACCEPT)
                    ?.contains("html") == true -> ctx.redirect("/signin") // redirect browser to signin
                else -> ctx.status(401)
            }
        }
        it.compression.brotliAndGzip()
        it.vue.enableCspAndNonces = true
        it.vue.stateFunction = { ctx -> mapOf("currentUser" to ctx.currentUser) }
        it.vue.rootDirectory(
            "src/main/resources/vue",
            Location.EXTERNAL
        )
        it.requestLogger.http { ctx, ms ->
            with(ctx) {
                JavalinLogger.info("Req: ${Instant.now()} ${method()} ${path()} ${status().code} $ms")
            }
        }

    }.start(7070)

    app.routes {
        get("/signin", VueComponent("signin-view"), ANYONE)
        get("/", VueComponent("feed-view"), LOGGED_IN)
        get("/my-photos", VueComponent("my-photos-view"), LOGGED_IN)
    }

    app.error(404, "html", VueComponent("not-found-view"))

    app.routes {
        path("api") {
            path("photos") {
                get(PhotoController::getForQuery, LOGGED_IN)
                post(PhotoController::upload, LOGGED_IN)
                delete(PhotoController::deleteById, LOGGED_IN)
                path("{id}") {
                    get(PhotoController::getById, LOGGED_IN)
                }
            }
            path("likes") {
                post(LikeController::create, LOGGED_IN)
                delete(LikeController::delete, LOGGED_IN)
            }
            path("account") {
                post("sign-up", AccountController::signUp, ANYONE)
                post("sign-in", AccountController::signIn, ANYONE)
                post("sign-out", AccountController::signOut, ANYONE)
            }
        }
    }

}

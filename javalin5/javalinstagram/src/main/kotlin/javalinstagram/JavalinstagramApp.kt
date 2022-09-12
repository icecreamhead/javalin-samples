package javalinstagram

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Header
import io.javalin.http.RequestLogger
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.bundled.requestDevLogger
import io.javalin.routing.PathMatcher
import io.javalin.util.JavalinLogger
import io.javalin.vue.VueComponent
import javalinstagram.Role.ANYONE
import javalinstagram.Role.LOGGED_IN
import javalinstagram.account.AccountController
import javalinstagram.like.LikeController
import javalinstagram.photo.PhotoController
import javalinstagram.util.DbSetupUtil
import org.jdbi.v3.core.Jdbi
import java.net.URI
import java.net.URISyntaxException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant
import java.time.ZonedDateTime


//val Database: Jdbi = Jdbi.create("jdbc:sqlite:javalinstagram.db")
val Database: Jdbi = Jdbi.create(getConnection())

val basePath = "" // change this to "" if you are opening the project standalone

@Throws(URISyntaxException::class, SQLException::class)
private fun getConnection(): Connection? {
    val dbUri = URI(System.getenv("DATABASE_URL"))
    val username: String = dbUri.getUserInfo().split(":").get(0)
    val password: String = dbUri.getUserInfo().split(":").get(1)
    val dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath()
    println(dbUrl)
    return DriverManager.getConnection(dbUrl, username, password)
}

fun main() {
//    DbSetupUtil.bootstrap()
    val app = Javalin.create {
        it.staticFiles.add("${basePath}user-uploads", Location.EXTERNAL)
        it.staticFiles.add("${basePath}src/main/resources/public", Location.EXTERNAL)
        it.staticFiles.enableWebjars()
        it.jetty.sessionHandler { Session.fileSessionHandler() }
        it.accessManager { handler, ctx, permitted ->
            when {
                ANYONE in permitted -> handler.handle(ctx)
                ctx.currentUser != null && permitted.contains(LOGGED_IN) -> handler.handle(ctx)
                ctx.header(Header.ACCEPT)?.contains("html") == true -> ctx.redirect("/signin") // redirect browser to signin
                else -> ctx.status(401)
            }
        }
        it.compression.brotliAndGzip()
        it.vue.enableCspAndNonces = true
        it.vue.stateFunction = { ctx -> mapOf("currentUser" to ctx.currentUser) }
        it.vue.rootDirectory("${basePath}src/main/resources/vue", Location.EXTERNAL) // comment out this line if you are opening the project standalone
        it.requestLogger.http { ctx, ms -> with(ctx) {
            JavalinLogger.info("Req: ${Instant.now()} ${method()} ${path()} ${status().code}")
        }}

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

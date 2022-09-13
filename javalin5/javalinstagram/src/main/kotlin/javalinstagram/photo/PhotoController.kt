package javalinstagram.photo

import io.javalin.http.Context
import io.javalin.util.JavalinLogger
import javalinstagram.DATA_DIR
import javalinstagram.currentUser
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object PhotoController {

    fun upload(ctx: Context) {
        Files.createDirectories(Paths.get(DATA_DIR)) // create folder if not exists
        val id = UUID.randomUUID().toString().replace("-", "")
        ctx.uploadedFile("photo")?.let { photoFile ->

            File("${DATA_DIR}/${id}.jpg").apply {
                photoFile.contentAndClose { it.copyTo(this.outputStream()) }
            }

//            val photo = File.createTempFile("temp", "upload").apply {
//                photoFile.contentAndClose { it.copyTo(this.outputStream()) }
//            }

//            Thumbnails.of(photo)
//                .crop(Positions.CENTER)
//                .size(800, 800)
//                .outputFormat("jpg")
//                .toFile(File("${DATA_DIR}/${id}.jpg"))
            PhotoDao.add(photoId = "${id}.jpg", ownerId = ctx.currentUser!!)
//            photo.delete()
            ctx.status(201)
        } ?: ctx.status(400).json("No photo found")
    }

    fun getForQuery(ctx: Context) {
        val numberToTake = ctx.queryParam("take") ?: "all"
        val ownerId = ctx.queryParam("owner-id")
        when {
            numberToTake == "all" && ownerId?.isNotEmpty() == true -> {
                ctx.json(PhotoDao.findByOwnerId(ownerId))
            }
            numberToTake == "all" -> ctx.json(PhotoDao.all(ctx.currentUser!!))
            else -> ctx.json(PhotoDao.all(ctx.currentUser!!).take(numberToTake.toInt()))
        }
    }

    fun getById(ctx: Context) {
        JavalinLogger.info(ctx.pathParam("id"))
        val id = ctx.pathParam("id")
        ctx.outputStream().write(File("${DATA_DIR}/${id}").readBytes())
    }

}
